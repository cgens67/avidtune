package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableSimpMusicKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.music.simpmusic.SimpMusicLyrics
import timber.log.Timber

object SimpMusicLyricsProvider : LyricsProvider {
    override val name = "SimpMusic"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableSimpMusicKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        SimpMusicLyrics.getLyrics(id, duration).getOrThrow()
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            SimpMusicLyrics.getAllLyrics(id, duration) { lyrics ->
                if (lyrics.isNotBlank()) {
                    callback(lyrics)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in SimpMusic getAllLyrics")
        }
    }
}
