package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.kugou.KuGou
import com.cgens67.avidtune.constants.EnableKugouKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import timber.log.Timber

object KuGouLyricsProvider : LyricsProvider {
    override val name = "Kugou"
    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableKugouKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int
    ): Result<String> =
        runCatching {
            KuGou.getLyrics(title, artist, duration).getOrThrow()
        }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit
    ) {
        try {
            KuGou.getAllPossibleLyricsOptions(title, artist, duration) { lyrics ->
                if (lyrics.isNotBlank()) {
                    callback(lyrics)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in KuGou getAllLyrics")
        }
    }
}
