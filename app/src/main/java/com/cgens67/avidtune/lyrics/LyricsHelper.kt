package com.cgens67.avidtune.lyrics

import android.content.Context
import android.util.LruCache
import com.cgens67.avidtune.constants.LyricsProviderOrderKey
import com.cgens67.avidtune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    private val allProviders = listOf(
        LyricsPlusProvider,
        PaxsenixLyricsProvider,
        BetterLyricsProvider,
        SimpMusicLyricsProvider,
        LrcLibLyricsProvider,
        KuGouLyricsProvider,
        YouTubeSubtitleLyricsProvider,
        YouTubeLyricsProvider
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
        val allResult = mutableListOf<LyricsResult>()
        
        lyricsProviders.forEach { provider ->
            if (provider.isEnabled(context)) {
                provider.getAllLyrics(mediaId, songTitle, songArtists, duration) { lyrics ->
                    val result = LyricsResult(provider.name, lyrics)
                    allResult += result
                    callback(result)
                }
            }
        }
        cache.put(cacheKey, allResult)
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
