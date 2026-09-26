package com.cgens67.avidtune.lyrics

import android.content.Context
import android.util.LruCache
import com.cgens67.avidtune.constants.LyricsProviderOrderKey
import com.cgens67.avidtune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val allProviders = listOf(
        AvidLyricsProvider, // 1
        LyricsPlusProvider, // 2
        PaxsenixLyricsProvider, // 3
        BiniLyricsProvider, // 4
        BetterLyricsProvider, // 5
        SimpMusicLyricsProvider, // 6
        LrcLibLyricsProvider, // 7
        KuGouLyricsProvider, // 8
        NetEaseLyricsProvider, // 9
        GeniusLyricsProvider, // 10
        YouTubeSubtitleLyricsProvider, // 11
        YouTubeLyricsProvider // 12
    )

    private suspend fun getOrderedProviders(): List<LyricsProvider> {
        val orderStr = context.dataStore.data.first()[LyricsProviderOrderKey]
        return if (orderStr == null) {
            allProviders
        } else {
            val orderNames = orderStr.split(",")
            val ordered = orderNames.mapNotNull { name -> allProviders.find { it.name == name } }
            val missing = allProviders.filter { it !in ordered }
            ordered + missing
        }
    }

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)

    suspend fun getLyrics(mediaMetadata: MediaMetadata): LyricsResult {
        val cached = cache.get(mediaMetadata.id)?.firstOrNull()
        if (cached != null) {
            return cached
        }
        
        val lyricsProviders = getOrderedProviders()
        
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                try {
                    provider
                        .getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            mediaMetadata.artists.joinToString { it.name },
                            mediaMetadata.duration,
                        ).onSuccess { lyrics ->
                            if (lyrics.isNotBlank() && lyrics != LYRICS_NOT_FOUND) {
                                return LyricsResult(provider.name, lyrics)
                            }
                        }.onFailure {
                            // Suppress failure and continue to the next provider
                        }
                } catch (e: Throwable) {
                    Timber.e(e, "Error fetching lyrics from ${provider.name}")
                }
            }
        }
        return LyricsResult("Unknown", LYRICS_NOT_FOUND)
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        val cacheKey = "$songArtists-$songTitle".replace(" ", "")
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }
        
        val lyricsProviders = getOrderedProviders()
        val allResult = Collections.synchronizedList(mutableListOf<LyricsResult>())
        
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                try {
                    provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                        if (lyrics.isNotBlank() && lyrics != LYRICS_NOT_FOUND) {
                            val result = LyricsResult(provider.name, lyrics)
                            allResult += result
                            callback(result)
                        }
                    }
                } catch (e: Throwable) {
                    Timber.e(e, "Error fetching all lyrics from ${provider.name}")
                }
            }
        }
        cache.put(cacheKey, synchronized(allResult) { allResult.toList() })
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
