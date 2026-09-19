package com.cgens67.avidtune.lyrics

import android.content.Context
import timber.log.Timber

interface LyricsProvider {
    val name: String

    fun isEnabled(context: Context): Boolean

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String>

    suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            getLyrics(id, title, artist, duration).onSuccess { lyrics ->
                if (lyrics.isNotBlank() && lyrics != com.cgens67.avidtune.db.entities.LyricsEntity.LYRICS_NOT_FOUND) {
                    callback(lyrics)
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in getAllLyrics for $name")
        }
    }
}
