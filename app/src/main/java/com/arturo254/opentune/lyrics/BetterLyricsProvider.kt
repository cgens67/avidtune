package com.arturo254.opentune.lyrics

import android.content.Context
import com.metrolist.music.betterlyrics.BetterLyrics
import com.arturo254.opentune.constants.EnableBetterLyricsKey
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get

object BetterLyricsProvider : LyricsProvider {
    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title, artist, duration, null)
}