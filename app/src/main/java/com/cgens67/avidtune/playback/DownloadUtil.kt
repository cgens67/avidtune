package com.cgens67.avidtune.playback

import android.content.Context
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.cgens67.innertube.YouTube
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.AudioQualityKey
import com.cgens67.avidtune.constants.PlayerClientOrderKey
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.FormatEntity
import com.cgens67.avidtune.di.DownloadCache
import com.cgens67.avidtune.di.PlayerCache
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.enumPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: SimpleCache,
    @PlayerCache val playerCache: SimpleCache,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val songUrlCache = HashMap<String, Pair<String, Long>>()
    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setCacheWriteDataSinkFactory(null) // Prevent writing to playerCache during downloads
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        OkHttpClient
                            .Builder()
                            .proxy(YouTube.proxy)
                            .build(),
                    ).setContentTypePredicate { contentType ->
                        contentType == null || !contentType.contains("text/html")
                    },
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1

            val reqLength = if (dataSpec.length != androidx.media3.common.C.LENGTH_UNSET.toLong()) dataSpec.length else Long.MAX_VALUE
            val cachedLength = playerCache.getCachedLength(mediaId, dataSpec.position, reqLength)

            if (cachedLength > 0 || playerCache.isCached(mediaId, dataSpec.position, length)) {
                val newLength = if (cachedLength > 0) cachedLength else {
                    if (dataSpec.length == androidx.media3.common.C.LENGTH_UNSET.toLong()) MusicService.CHUNK_LENGTH else kotlin.math.min(dataSpec.length, MusicService.CHUNK_LENGTH)
                }
                return@Factory dataSpec.subrange(0, newLength)
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                val newLength = if (dataSpec.length == androidx.media3.common.C.LENGTH_UNSET.toLong()) MusicService.CHUNK_LENGTH else kotlin.math.min(dataSpec.length, MusicService.CHUNK_LENGTH)
                return@Factory dataSpec.withUri(it.first.toUri())
                    .subrange(0, newLength)
            }

            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
            val clientOrder = runBlocking(Dispatchers.IO) {
                context.dataStore.data.map { it[PlayerClientOrderKey] }.first()
            }
            val playbackData = runBlocking(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    mediaId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                    clientOrder = clientOrder
                )
            }.getOrThrow()
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        playbackUrl = playbackData.streamUrl
                    ),
                )
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache[mediaId] =
                streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
            
            val newLength = if (dataSpec.length == androidx.media3.common.C.LENGTH_UNSET.toLong()) MusicService.CHUNK_LENGTH else kotlin.math.min(dataSpec.length, MusicService.CHUNK_LENGTH)
            dataSpec.withUri(streamUrl.toUri()).subrange(0, newLength)
        }
    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)
    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 3
            addListener(
                ExoDownloadService.TerminalStateNotificationHelper(
                    context = context,
                    notificationHelper = downloadNotificationHelper,
                    nextNotificationId = ExoDownloadService.NOTIFICATION_ID + 1,
                ),
            )
        }
    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    init {
        val result = mutableMapOf<String, Download>()
        val cursor = downloadManager.downloadIndex.getDownloads()
        while (cursor.moveToNext()) {
            result[cursor.download.request.id] = cursor.download
        }
        downloads.value = result
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    downloads.update { map ->
                        map.toMutableMap().apply {
                            set(download.request.id, download)
                        }
                    }
                }
            },
        )
    }
}
