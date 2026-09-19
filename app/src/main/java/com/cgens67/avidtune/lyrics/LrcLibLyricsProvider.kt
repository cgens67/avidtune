package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.lrclib.LrcLib
import com.cgens67.avidtune.constants.EnableLrcLibKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import timber.log.Timber

object LrcLibLyricsProvider : LyricsProvider {
    override val name = "LrcLib"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableLrcLibKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        LrcLib.getLyrics(title, artist, duration).getOrThrow()
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            LrcLib.getAllLyrics(title, artist, duration, null) { lyrics ->
                if (lyrics.isNotBlank()) {
                    callback(lyrics)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in LrcLib getAllLyrics")
        }
    }
}
