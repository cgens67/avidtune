package com.cgens67.avidtune.ui.player

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.view.TextureView
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.cgens67.avidtune.R
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.playback.AlternativeStreamClient
import com.cgens67.avidtune.playback.ChunkedDataSource
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.potoken.PoTokenGenerator
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.roundToInt

/** Quality UI state for the playing music video. */
data class MusicVideoQualityUi(
    val heights: List<Int> = emptyList(),
    val held: Int? = null,
    val activeHeight: Int? = null,
)

internal fun videoQualityLabel(height: Int): String = when {
    height >= 4320 -> "8K"
    height >= 2160 -> "4K"
    height >= 1440 -> "2K"
    height >= 1080 -> "FullHD"
    height >= 720 -> "HD"
    else -> "${height}p"
}

/**
 * Plays a music video's video-only stream in a second muted ExoPlayer kept
 * tightly synchronized with MusicService's audio ExoPlayer.
 */
@OptIn(UnstableApi::class)
object MusicVideoEngine {

    private const val TAG = "MusicVideoEngine"

    private const val SEEK_DRIFT_MS = 600L
    private const val IGNORE_DRIFT_MS = 25L
    private const val SYNC_EVERY_MS = 250L
    private const val SEEK_COOLDOWN_MS = 2_500L
    private const val RELEASE_GRACE_MS = 4_000L
    private const val SKIP_DEBOUNCE_MS = 180L
    private const val CHUNK_BYTES = 1L * 1024 * 1024
    private const val STREAM_TTL_MS = 60 * 60 * 1000L
    private const val MIN_VIDEO_ASPECT = 1.2f
    private const val AUTO_HEIGHT = 720
    private const val METERED_HEIGHT = 480
    private const val PROBE_TIMEOUT_SECONDS = 3L
    private const val AUTH_BOUNDARY_BYTES = 1024L * 1024
    private const val PROBE_READ_BYTES = 16L * 1024
    private const val MAX_REFUSED_CLIENTS = 5
    private const val PREFS_NAME = "music_video"
    private const val KEY_QUALITY = "quality"
    private const val KEY_KNOWN_VIDEOS = "known_video_ids"

    private val REFUSAL_CODES = setOf(403, 404, 410)

    private val VIDEO_CLIENT_CHAIN = listOf(
        "ANDROID_VR_1_65_10",
        "ANDROID_VR_1_43_32",
        "IOS",
        "VISIONOS",
        "ANDROID_NO_SDK",
        "IPADOS",
        "ANDROID_VR_1_61_48",
        "ANDROID_CREATOR",
        "MOBILE",
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        "TVHTML5",
        "WEB_REMIX",
        "MWEB",
        "WEB",
    )

    data class VideoStream(
        val videoId: String,
        val url: String,
        val width: Int,
        val height: Int,
        val mimeType: String,
        val headers: Map<String, String>,
        val heights: List<Int>,
        val profileId: String,
    ) {
        val aspect: Float get() = if (height > 0) width.toFloat() / height else 0f
    }

    private sealed interface VideoResolution {
        data class Found(val stream: VideoStream) : VideoResolution
        data object NoVideoTrack : VideoResolution
        data object TransientFailure : VideoResolution
    }

    private class Surface(val view: TextureView, val priority: Int)

    private val scope = MainScope()
    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var controller: Player? = null
    private var player: ExoPlayer? = null
    private val surfaces = ArrayList<Surface>()

    private var lastControllerMediaId: String? = null
    private var loadedId: String? = null
    private var loadedProfile: String? = null
    private var loadedUrl: String? = null
    private var loadedCap = 0
    private var videoCache: SimpleCache? = null
    private var loadJob: Job? = null
    private var prefetchJob: Job? = null
    private var syncJob: Job? = null
    private var releaseJob: Job? = null
    private var lastSeekAt = 0L
    private var seekStartedAt = 0L
    private var seekLeadMs = 200L

    private val streams = LinkedHashMap<String, Triple<VideoStream, Int, Long>>()
    private val refused = HashMap<String, MutableSet<String>>()
    private val stepDown = HashSet<String>()

    private val _noPictureIds = MutableStateFlow<Set<String>>(emptySet())
    val noPictureIds: StateFlow<Set<String>> = _noPictureIds.asStateFlow()

    private val _failedVideoIds = MutableStateFlow<Set<String>>(emptySet())
    val failedVideoIds: StateFlow<Set<String>> = _failedVideoIds.asStateFlow()

    private val _detectedVideoIds = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val detectedVideoIds: StateFlow<Map<String, Boolean>> = _detectedVideoIds.asStateFlow()

    private val _audioOnlyTracks = MutableStateFlow<Set<String>>(emptySet())
    val audioOnlyTracks: StateFlow<Set<String>> = _audioOnlyTracks.asStateFlow()

    private val _videoPlaybackEnabled = MutableStateFlow(true)
    val videoPlaybackEnabled: StateFlow<Boolean> = _videoPlaybackEnabled.asStateFlow()

    private val _shownVideoId = MutableStateFlow<String?>(null)
    val shownVideoId: StateFlow<String?> = _shownVideoId.asStateFlow()

    private val _frameOn = MutableStateFlow<TextureView?>(null)
    internal val frameOn: StateFlow<TextureView?> = _frameOn.asStateFlow()

    private val _quality = MutableStateFlow(MusicVideoQualityUi())
    val quality: StateFlow<MusicVideoQualityUi> = _quality.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _aspect = MutableStateFlow(16f / 9f)
    val aspect: StateFlow<Float> = _aspect.asStateFlow()

    @Volatile
    private var cachedStreamClientKey: Pair<Proxy?, String?>? = null
    @Volatile
    private var cachedStreamClient: OkHttpClient? = null
    @Volatile
    private var cachedProbeClient: OkHttpClient? = null

    private val streamHttpClient: OkHttpClient
        get() {
            val key = YouTube.proxy to YouTube.proxyAuth
            val existing = cachedStreamClient
            if (existing != null && cachedStreamClientKey == key) return existing
            return synchronized(this) {
                val again = cachedStreamClient
                if (again != null && cachedStreamClientKey == key) {
                    again
                } else {
                    OkHttpClient.Builder()
                        .proxy(key.first)
                        .apply {
                            key.second?.let { auth ->
                                proxyAuthenticator { _, response ->
                                    response.request.newBuilder()
                                        .header("Proxy-Authorization", auth)
                                        .build()
                                }
                            }
                        }
                        .retryOnConnectionFailure(true)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .build()
                        .also { built ->
                            cachedStreamClientKey = key
                            cachedStreamClient = built
                            cachedProbeClient = built.newBuilder()
                                .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                .build()
                        }
                }
            }
        }

    private val probeClient: OkHttpClient
        get() {
            streamHttpClient
            return cachedProbeClient!!
        }

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        if (prefs == null) {
            prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { stored ->
                val held = stored.getInt(KEY_QUALITY, 0).takeIf { it > 0 }
                _quality.value = _quality.value.copy(held = held)
                val savedVideos = stored.getStringSet(KEY_KNOWN_VIDEOS, emptySet()).orEmpty()
                if (savedVideos.isNotEmpty()) {
                    _detectedVideoIds.update { map ->
                        savedVideos.associateWith { true } + map
                    }
                }
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (_videoPlaybackEnabled.value == enabled) return
        _videoPlaybackEnabled.value = enabled
        refresh()
    }

    fun setAudioOnlyForTrack(videoId: String, audioOnly: Boolean) {
        _audioOnlyTracks.update { current ->
            if (audioOnly) current + videoId else current - videoId
        }
        if (!audioOnly) {
            clearTransientFailure(videoId)
        }
        refresh()
    }

    fun isAudioOnlyForTrack(videoId: String): Boolean = videoId in _audioOnlyTracks.value

    fun onVideoTypeResolved(videoId: String, musicVideoType: String?) {
        if (videoId.isBlank() || musicVideoType.isNullOrBlank()) return
        val isVideo = musicVideoType != MUSIC_VIDEO_TYPE_ATV
        rememberVideoId(videoId, isVideo)
    }

    fun rememberVideoId(videoId: String, isVideo: Boolean) {
        if (videoId.isBlank()) return
        val prev = _detectedVideoIds.value[videoId]
        if (prev == isVideo) return
        _detectedVideoIds.update { it + (videoId to isVideo) }
        if (isVideo) {
            prefs?.let { stored ->
                val existing = stored.getStringSet(KEY_KNOWN_VIDEOS, emptySet()).orEmpty().toMutableSet()
                if (existing.size > 500) {
                    val trimmed = existing.toList().takeLast(250).toMutableSet()
                    existing.clear()
                    existing.addAll(trimmed)
                }
                if (existing.add(videoId)) {
                    stored.edit().putStringSet(KEY_KNOWN_VIDEOS, existing).apply()
                }
            }
        }
        scope.launch {
            if (controller?.currentMediaItem?.mediaId == videoId) {
                refresh()
            }
        }
    }

    fun isVideoTrack(metadata: MediaMetadata?): Boolean {
        metadata ?: return false
        return metadata.isVideo || _detectedVideoIds.value[metadata.id] == true
    }

    fun showsMusicVideo(metadata: MediaMetadata?): Boolean {
        metadata ?: return false
        return _videoPlaybackEnabled.value &&
            isVideoTrack(metadata) &&
            metadata.id !in _noPictureIds.value &&
            metadata.id !in _audioOnlyTracks.value
    }

    fun onSessionChanged() {
        _noPictureIds.value = emptySet()
        _failedVideoIds.value = emptySet()
        streams.clear()
        refused.clear()
        stepDown.clear()
        refresh()
    }

    fun attach(controller: Player?) {
        if (this.controller === controller) return
        this.controller?.removeListener(controllerListener)
        this.controller = controller
        controller?.addListener(controllerListener)
        lastControllerMediaId = controller?.currentMediaItem?.mediaId?.takeIf { it.isNotBlank() }
        _playing.value = controller?.isPlaying == true
        if (controller == null) {
            _failedVideoIds.value = emptySet()
            refused.clear()
            stepDown.clear()
            unload()
        } else {
            refresh()
        }
    }

    fun setQuality(height: Int?) {
        prefs?.edit()?.apply {
            if (height == null) remove(KEY_QUALITY) else putInt(KEY_QUALITY, height)
        }?.apply()
        _quality.value = _quality.value.copy(held = height)
        val id = loadedId ?: wantedVideoId() ?: return
        clearTransientFailure(id)
        streams.remove(id)
        load(id)
    }

    fun retry(videoId: String? = null) {
        val targetId = videoId ?: wantedVideoId() ?: return
        clearTransientFailure(targetId)
        _noPictureIds.update { it - targetId }
        streams.remove(targetId)
        if (surfaces.isNotEmpty() && wantedVideoId() == targetId) {
            load(targetId)
        }
    }

    private fun clearTransientFailure(videoId: String) {
        _failedVideoIds.update { it - videoId }
        refused.remove(videoId)
        stepDown.remove(videoId)
    }

    private fun clearAllTransientFailures() {
        _failedVideoIds.value = emptySet()
        refused.clear()
        stepDown.clear()
    }

    internal fun claim(view: TextureView, priority: Int): () -> Unit {
        releaseJob?.cancel()
        val prevTop = top()
        surfaces.removeAll { it.view === view }
        surfaces += Surface(view, priority)
        val app = view.context.applicationContext
        init(app)
        val exo = ensurePlayer(app)
        if (top() === view) {
            _frameOn.value = null
            if (prevTop != null && prevTop !== view) {
                exo.clearVideoTextureView(prevTop)
            }
            exo.setVideoTextureView(view)
            repaint()
        }
        refresh()
        return release@{
            val wasTop = top() === view
            surfaces.removeAll { it.view === view }
            if (!wasTop) return@release
            if (_frameOn.value === view) _frameOn.value = null
            exo.clearVideoTextureView(view)
            val next = top()
            if (next != null) {
                exo.setVideoTextureView(next)
                repaint()
            } else {
                releaseJob = scope.launch {
                    delay(RELEASE_GRACE_MS)
                    if (surfaces.isEmpty()) {
                        clearAllTransientFailures()
                        unload()
                    }
                }
            }
        }
    }

    private fun top(): TextureView? =
        surfaces.lastOrNull { s -> surfaces.none { it.priority > s.priority } }?.view

    private fun ensureVideoCache(context: Context): SimpleCache = videoCache ?: synchronized(this) {
        videoCache ?: SimpleCache(
            context.applicationContext.cacheDir.resolve("music_video_cache"),
            LeastRecentlyUsedCacheEvictor(128L * 1024 * 1024),
            StandaloneDatabaseProvider(context.applicationContext),
        ).also { videoCache = it }
    }

    private fun ensurePlayer(context: Context): ExoPlayer = player ?: ExoPlayer.Builder(context)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(15_000, 30_000, 500, 1_000)
                .build()
        )
        .build()
        .apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            addListener(playerListener)
        }
        .also { player = it }

    private fun repaint() {
        val exo = player ?: return
        if (!exo.playWhenReady && exo.playbackState != Player.STATE_IDLE) {
            exo.seekTo(exo.currentPosition)
        }
    }

    private fun wantedVideoId(): String? {
        val item = controller?.currentMediaItem ?: return null
        val id = item.mediaId.takeIf { it.isNotBlank() } ?: return null
        val meta = item.metadata
        val isVideo = meta?.isVideo == true || _detectedVideoIds.value[id] == true
        if (!_videoPlaybackEnabled.value || !isVideo) return null
        if (id in _noPictureIds.value || id in _audioOnlyTracks.value) return null
        return id
    }

    private fun refresh() {
        val wantedId = if (surfaces.isEmpty()) null else wantedVideoId()
        if (wantedId == null) {
            if (surfaces.isNotEmpty() || wantedVideoId() != loadedId) unload()
            return
        }
        if (wantedId in _failedVideoIds.value) {
            return
        }
        if (wantedId == loadedId) {
            startSync()
            return
        }
        load(wantedId)
    }

    private fun capFor(videoId: String): Int {
        val metered = appContext?.getSystemService<ConnectivityManager>()?.isActiveNetworkMetered == true
        val auto = if (metered) METERED_HEIGHT else AUTO_HEIGHT
        val held = _quality.value.held ?: return auto
        return if (videoId in stepDown) minOf(held, AUTO_HEIGHT) else held
    }

    private fun isRefusal(error: PlaybackException): Boolean =
        generateSequence<Throwable>(error) { it.cause }
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .any { it.responseCode in REFUSAL_CODES }

    private fun onChunkRefused(url: String, code: Int) {
        if (code !in REFUSAL_CODES) return
        val id = loadedId ?: return
        if (url != loadedUrl) return
        streams.remove(id)
        val profile = loadedProfile ?: return
        refused.getOrPut(id) { HashSet() }.add(profile)
    }

    private fun load(videoId: String, debounce: Boolean = true) {
        unload()
        _failedVideoIds.update { it - videoId }
        loadedId = videoId
        val cap = capFor(videoId)
        loadedCap = cap
        loadJob = scope.launch {
            val cached = cachedStream(videoId, cap)
            if (cached == null && debounce) {
                delay(SKIP_DEBOUNCE_MS)
                if (loadedId != videoId) return@launch
            }
            val resolution = try {
                if (cached != null) {
                    VideoResolution.Found(cached)
                } else {
                    withContext(Dispatchers.IO) {
                        resolveVideoStream(videoId, cap, refused[videoId].orEmpty())
                    }.also { res ->
                        if (res is VideoResolution.Found) {
                            streams[videoId] = Triple(res.stream, cap, System.currentTimeMillis())
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to resolve video stream for $videoId")
                if (loadedId == videoId) {
                    loadedId = null
                    _failedVideoIds.update { it + videoId }
                }
                return@launch
            }
            if (loadedId != videoId) return@launch

            val stream = when (resolution) {
                is VideoResolution.Found -> resolution.stream
                VideoResolution.NoVideoTrack -> {
                    _noPictureIds.update { it + videoId }
                    loadedId = null
                    return@launch
                }
                VideoResolution.TransientFailure -> {
                    _failedVideoIds.update { it + videoId }
                    loadedId = null
                    return@launch
                }
            }

            if (stream.aspect < MIN_VIDEO_ASPECT) {
                _noPictureIds.update { it + videoId }
                loadedId = null
                return@launch
            }

            val exo = player ?: return@launch
            val upstream = OkHttpDataSource.Factory(streamHttpClient)
                .setDefaultRequestProperties(stream.headers)
            val chunkedFactory = ChunkedDataSource.Factory(upstream, CHUNK_BYTES, ::onChunkRefused)
            val mediaFactory = appContext?.let { ctx ->
                CacheDataSource.Factory()
                    .setCache(ensureVideoCache(ctx))
                    .setUpstreamDataSourceFactory(chunkedFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } ?: chunkedFactory
            val cacheKey = "${stream.videoId}_${stream.height}_${stream.mimeType.substringBefore(";")}"
            val mediaItem = MediaItem.Builder()
                .setUri(stream.url)
                .setCustomCacheKey(cacheKey)
                .build()
            val source = ProgressiveMediaSource.Factory(mediaFactory)
                .createMediaSource(mediaItem)

            if (stream.aspect > 0f) _aspect.value = stream.aspect
            _quality.value = _quality.value.copy(heights = stream.heights, activeHeight = stream.height)
            loadedProfile = stream.profileId
            loadedUrl = stream.url
            val start = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
            exo.setMediaSource(source, start + seekLeadMs)
            exo.playWhenReady = controller?.isPlaying == true
            exo.prepare()
            lastSeekAt = System.currentTimeMillis()
            seekStartedAt = 0L
            startSync()
        }
    }

    private fun prefetchNext() {
        val audio = controller ?: return
        val nextIdx = audio.nextMediaItemIndex
        if (nextIdx == C.INDEX_UNSET || nextIdx !in 0 until audio.mediaItemCount) return
        val nextItem = audio.getMediaItemAt(nextIdx)
        val nextId = nextItem.mediaId.takeIf { it.isNotBlank() } ?: return
        if (nextId == loadedId || nextId in _noPictureIds.value || nextId in _audioOnlyTracks.value) return
        val meta = nextItem.metadata
        val isVideo = meta?.isVideo == true || _detectedVideoIds.value[nextId] == true
        if (!isVideo) return
        val cap = capFor(nextId)
        if (cachedStream(nextId, cap) != null) return

        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            delay(400L)
            val res = try {
                withContext(Dispatchers.IO) {
                    resolveVideoStream(nextId, cap, refused[nextId].orEmpty())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@launch
            }
            if (res is VideoResolution.Found && res.stream.aspect >= MIN_VIDEO_ASPECT) {
                streams[nextId] = Triple(res.stream, cap, System.currentTimeMillis())
            } else if (res is VideoResolution.NoVideoTrack) {
                _noPictureIds.update { it + nextId }
            }
        }
    }

    private fun cachedStream(videoId: String, cap: Int): VideoStream? {
        val (stream, askedFor, at) = streams[videoId] ?: return null
        if (askedFor != cap || System.currentTimeMillis() - at > STREAM_TTL_MS) {
            streams.remove(videoId)
            return null
        }
        return stream
    }

    private fun unload() {
        loadJob?.cancel()
        loadJob = null
        prefetchJob?.cancel()
        prefetchJob = null
        syncJob?.cancel()
        syncJob = null
        loadedId = null
        loadedProfile = null
        loadedUrl = null
        _shownVideoId.value = null
        _frameOn.value = null
        _quality.value = _quality.value.copy(heights = emptyList(), activeHeight = null)
        player?.let {
            it.stop()
            it.clearMediaItems()
        }
        while (streams.size > 8) streams.remove(streams.keys.first())
    }

    private fun startSync() {
        if (syncJob?.isActive == true) return
        syncJob = scope.launch {
            while (isActive) {
                sync()
                delay(SYNC_EVERY_MS)
            }
        }
    }

    private fun sync() {
        val audio = controller ?: return
        val exo = player ?: return
        if (loadedId == null || exo.playbackState == Player.STATE_IDLE || exo.playbackState == Player.STATE_ENDED) return
        val speed = audio.playbackParameters.speed
        val audioPos = audio.currentPosition
        val now = System.currentTimeMillis()
        if (!audio.isPlaying) {
            exo.playWhenReady = false
            setSpeed(exo, speed)
            if (abs(exo.currentPosition - audioPos) > 40L && now - lastSeekAt > 120L) seek(exo, audioPos)
            return
        }
        exo.playWhenReady = true
        if (exo.playbackState == Player.STATE_BUFFERING) return
        val drift = exo.currentPosition - audioPos
        if (abs(drift) > SEEK_DRIFT_MS) {
            if (now - lastSeekAt > SEEK_COOLDOWN_MS) seek(exo, audioPos + seekLeadMs)
            return
        }
        val nudge = if (abs(drift) > IGNORE_DRIFT_MS) (-drift / 1000f).coerceIn(-0.1f, 0.1f) else 0f
        setSpeed(exo, speed * (1f + nudge))
    }

    private fun seek(exo: ExoPlayer, positionMs: Long) {
        lastSeekAt = System.currentTimeMillis()
        seekStartedAt = lastSeekAt
        exo.seekTo(positionMs.coerceAtLeast(0L))
    }

    private fun setSpeed(exo: ExoPlayer, speed: Float) {
        if (abs(exo.playbackParameters.speed - speed) > 0.005f) {
            exo.playbackParameters = PlaybackParameters(speed)
        }
    }

    private suspend fun resolveVideoStream(
        videoId: String,
        maxHeight: Int,
        skipClients: Set<String>,
    ): VideoResolution {
        val isLoggedIn = YouTube.cookie != null
        val signatureTimestamp = NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
        var cachedPoToken: String? = null
        var poTokenFetched = false

        for (clientKey in VIDEO_CLIENT_CHAIN) {
            if (clientKey in skipClients) continue
            val client = YTPlayerUtils.AVAILABLE_CLIENTS[clientKey] ?: continue
            if (client.loginRequired && !isLoggedIn) continue

            val clientPoToken = if (client.useWebPoTokens) {
                if (!poTokenFetched) {
                    poTokenFetched = true
                    val sessionId = (if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData).orEmpty()
                    cachedPoToken = PoTokenGenerator.getWebClientPoToken(videoId, sessionId)?.playerRequestPoToken
                }
                cachedPoToken
            } else {
                null
            }

            val playerRes = YouTube.player(
                videoId = videoId,
                playlistId = null,
                client = client,
                signatureTimestamp = signatureTimestamp,
                poToken = clientPoToken,
            ).getOrNull() ?: continue

            if (playerRes.playabilityStatus.status != "OK") continue

            // Catalogue Art Tracks (ATV) only carry a still square cover video.
            if (playerRes.videoDetails?.musicVideoType == MUSIC_VIDEO_TYPE_ATV) {
                return VideoResolution.NoVideoTrack
            }

            val adaptiveVideos = playerRes.streamingData?.adaptiveFormats.orEmpty()
                .filter { it.isPlayableVideo() }
            val muxedVideos = playerRes.streamingData?.formats.orEmpty()
                .filter { it.isPlayableVideo() }
            val candidates = adaptiveVideos.ifEmpty { muxedVideos }
            if (candidates.isEmpty()) continue

            val heights = candidates.mapNotNull { it.height }.filter { it > 0 }.distinct().sortedDescending()
            val chosen = selectBestVideoFormat(candidates, maxHeight) ?: continue
            val url = chosen.url ?: continue
            val w = chosen.width ?: 0
            val h = chosen.height ?: 0
            if (h <= 0 || w <= 0) continue

            val headers = mediaHeadersForClient(client)
            if (probeVideoUrl(url, headers)) {
                Timber.tag(TAG).i("Resolved video $videoId ${w}x$h (${chosen.mimeType}) via $clientKey")
                return VideoResolution.Found(
                    VideoStream(
                        videoId = videoId,
                        url = url,
                        width = w,
                        height = h,
                        mimeType = chosen.mimeType,
                        headers = headers,
                        heights = heights,
                        profileId = clientKey,
                    )
                )
            }
        }

        // Fallback to Piped / Invidious if direct YouTube clients fail
        val fallback = AlternativeStreamClient.getVideoStreamData(videoId, maxHeight)
        if (fallback != null && fallback.clientName !in skipClients) {
            return VideoResolution.Found(
                VideoStream(
                    videoId = videoId,
                    url = fallback.streamUrl,
                    width = fallback.width,
                    height = fallback.height,
                    mimeType = fallback.mimeType,
                    headers = mapOf("User-Agent" to YouTubeClient.USER_AGENT_WEB),
                    heights = fallback.availableHeights,
                    profileId = fallback.clientName,
                )
            )
        }

        return VideoResolution.TransientFailure
    }

    private fun PlayerResponse.StreamingData.Format.isPlayableVideo(): Boolean {
        val u = url ?: return false
        if (u.isBlank() || isLiveUrl(u)) return false
        val w = width ?: return false
        val h = height ?: return false
        if (w <= 0 || h <= 0) return false
        return mimeType.startsWith("video/", ignoreCase = true)
    }

    private fun isLiveUrl(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.queryParameter("live") == "1" ||
            parsed.queryParameter("source") == "yt_live_broadcast"
    }

    private fun selectBestVideoFormat(
        formats: List<PlayerResponse.StreamingData.Format>,
        maxHeight: Int,
    ): PlayerResponse.StreamingData.Format? {
        val availableHeights = formats.mapNotNull { it.height }.filter { it > 0 }.distinct()
        if (availableHeights.isEmpty()) return null

        val targetHeight = availableHeights.filter { it <= maxHeight }.maxOrNull()
            ?: availableHeights.minOrNull()
            ?: return null

        val atHeight = formats.filter { it.height == targetHeight }
        return atHeight.maxByOrNull { format ->
            val mime = format.mimeType.lowercase()
            val codecScore = if (maxHeight <= AUTO_HEIGHT) {
                when {
                    "avc1" in mime -> 300
                    "vp9" in mime || "vp09" in mime -> 200
                    "av01" in mime -> 100
                    else -> 50
                }
            } else {
                when {
                    "vp9" in mime || "vp09" in mime -> 300
                    "av01" in mime -> 200
                    "avc1" in mime -> 100
                    else -> 50
                }
            }
            codecScore + (format.fps ?: 30)
        }
    }

    private fun mediaHeadersForClient(client: YouTubeClient): Map<String, String> = buildMap {
        put("User-Agent", client.userAgent)
        when {
            client.clientName == "WEB_REMIX" -> {
                put("Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
                put("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
            }
            client.clientName.startsWith("WEB") ||
                client.clientName == "MWEB" ||
                client.clientName.startsWith("TVHTML5") -> {
                put("Origin", YouTubeClient.ORIGIN_YOUTUBE)
                put("Referer", YouTubeClient.REFERER_YOUTUBE)
            }
        }
    }

    private suspend fun probeVideoUrl(url: String, headers: Map<String, String>): Boolean {
        val length = url.toHttpUrlOrNull()?.queryParameter("clen")?.toLongOrNull()
        val start = if (length != null && length > AUTH_BOUNDARY_BYTES + PROBE_READ_BYTES) AUTH_BOUNDARY_BYTES else 0L
        val window = ChunkedDataSource.rangeBytesFor(url)
        val range = when {
            window != Long.MAX_VALUE -> "bytes=$start-${minOf(start + window, length ?: Long.MAX_VALUE) - 1}"
            length != null -> "bytes=$start-${length - 1}"
            else -> "bytes=$start-"
        }
        val builder = Request.Builder().url(url).header("Range", range)
        headers.forEach { (k, v) -> builder.header(k, v) }
        return suspendCancellableCoroutine { cont ->
            val call = probeClient.newCall(builder.build())
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) {
                        Timber.tag(TAG).w("Video probe failed: ${e.message}")
                        cont.resume(false)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val ok = try {
                        response.use { res ->
                            val contentType = res.header("Content-Type")
                            val okStatus = res.code in 200..299 || res.code == 416
                            okStatus &&
                                contentType?.startsWith("video/", ignoreCase = true) == true &&
                                res.body?.source()?.request(PROBE_READ_BYTES) == true
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("Video probe failed: ${e.message}")
                        false
                    }
                    if (cont.isActive) cont.resume(ok)
                }
            })
        }
    }

    private val controllerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val nextId = mediaItem?.mediaId?.takeIf { it.isNotBlank() }
            val prevId = lastControllerMediaId
            if (nextId == null || nextId != prevId || reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                if (prevId != null) {
                    clearTransientFailure(prevId)
                }
                if (nextId != null) {
                    clearTransientFailure(nextId)
                } else {
                    clearAllTransientFailures()
                }
            }
            lastControllerMediaId = nextId
            refresh()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playing.value = isPlaying
            sync()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) = sync()

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            lastSeekAt = 0L
            sync()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            loadedId?.let { id -> _failedVideoIds.update { it - id } }
            _frameOn.value = top()
            _shownVideoId.value = loadedId
            prefetchNext()
        }

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val width = videoSize.width * videoSize.pixelWidthHeightRatio
            if (width > 0f && videoSize.height > 0) {
                _aspect.value = width / videoSize.height
                _quality.value = _quality.value.copy(activeHeight = videoSize.height)
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && seekStartedAt > 0L) {
                val took = System.currentTimeMillis() - seekStartedAt
                seekLeadMs = ((seekLeadMs + took) / 2).coerceIn(0L, 350L)
                seekStartedAt = 0L
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val id = loadedId ?: return
            Timber.tag(TAG).w("Video playback failed at ${loadedCap}p (${error.errorCodeName}) for $id")
            streams.remove(id)
            val profile = loadedProfile
            if (profile != null) {
                val skip = refused.getOrPut(id) { HashSet() }
                skip.add(profile)
                if (skip.size <= MAX_REFUSED_CLIENTS) {
                    if (!isRefusal(error) && loadedCap > AUTO_HEIGHT) {
                        stepDown.add(id)
                    }
                    load(id, debounce = false)
                    return
                }
            }
            if (loadedCap > AUTO_HEIGHT && stepDown.add(id)) {
                load(id, debounce = false)
                return
            }
            _failedVideoIds.update { it + id }
            unload()
        }
    }
}

@Composable
internal fun rememberIsForeground(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return foreground
}

@Composable
internal fun rememberShowsMusicVideo(mediaMetadata: MediaMetadata?): Boolean {
    val enabled by MusicVideoEngine.videoPlaybackEnabled.collectAsState()
    val detected by MusicVideoEngine.detectedVideoIds.collectAsState()
    val noPic by MusicVideoEngine.noPictureIds.collectAsState()
    val audioOnly by MusicVideoEngine.audioOnlyTracks.collectAsState()
    val id = mediaMetadata?.id ?: return false
    val isVideo = mediaMetadata.isVideo || detected[id] == true
    return enabled && isVideo && id !in noPic && id !in audioOnly
}

/**
 * Displays [videoId]'s synchronized video stream fitted into or cropped to bounds,
 * fading in smoothly once the first frame lands.
 */
@OptIn(UnstableApi::class)
@Composable
internal fun AndroidMusicVideo(
    videoId: String,
    fit: Boolean,
    onRenderedChanged: (Boolean) -> Unit,
    modifier: Modifier,
    priority: Int = 1,
    showErrorOverlay: Boolean = true,
    showLoadingOverlay: Boolean = true,
) {
    val foreground = rememberIsForeground()
    var view by remember { mutableStateOf<TextureView?>(null) }
    DisposableEffect(view, foreground, priority) {
        val target = view
        val release = if (target != null && foreground) MusicVideoEngine.claim(target, priority) else null
        onDispose { release?.invoke() }
    }

    val shown by MusicVideoEngine.shownVideoId.collectAsState()
    val frameOn by MusicVideoEngine.frameOn.collectAsState()
    val aspect by MusicVideoEngine.aspect.collectAsState()
    val failedIds by MusicVideoEngine.failedVideoIds.collectAsState()
    val rendered = view != null && frameOn === view && shown == videoId
    val isFailed = videoId in failedIds && !rendered
    val playing by MusicVideoEngine.playing.collectAsState()

    val awake = rendered && playing
    LaunchedEffect(view, awake) { view?.keepScreenOn = awake }

    val report by rememberUpdatedState(onRenderedChanged)
    LaunchedEffect(rendered) { report(rendered) }
    DisposableEffect(Unit) { onDispose { report(false) } }

    val alpha by animateFloatAsState(
        targetValue = if (rendered) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "musicVideoAlpha",
    )

    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    isOpaque = false
                    this.alpha = 0f
                    view = this
                }
            },
            update = { it.alpha = alpha },
            onRelease = { if (view === it) view = null },
            modifier = Modifier.layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = if (constraints.hasBoundedHeight) {
                    constraints.maxHeight
                } else {
                    (width / aspect).roundToInt()
                }
                val boundsAspect = width.toFloat() / height.coerceAtLeast(1)
                val wider = boundsAspect > aspect
                val (w, h) = if (wider == fit) {
                    (height * aspect).roundToInt() to height
                } else {
                    width to (width / aspect).roundToInt()
                }
                val placeable = measurable.measure(Constraints.fixed(w.coerceAtLeast(1), h.coerceAtLeast(1)))
                layout(width, height) {
                    placeable.place((width - w) / 2, (height - h) / 2)
                }
            },
        )

        if (!rendered && !isFailed && showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        if (isFailed && showErrorOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    onClick = { MusicVideoEngine.retry(videoId) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.16f),
                    contentColor = Color.White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.replay),
                            contentDescription = stringResource(R.string.action_retry),
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.action_retry),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}