package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.paxsenix.Paxsenix
import com.cgens67.avidtune.constants.EnablePaxsenixKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import timber.log.Timber

object PaxsenixLyricsProvider : LyricsProvider {
    private const val TAG = "PaxsenixProvider"
    
    override val name = "Paxsenix"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnablePaxsenixKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> {
        Timber.tag(TAG).d("getLyrics called: title='$title', artist='$artist', duration=$duration")
        
        return try {
            val result = Paxsenix.getLyrics(title, artist, duration, null)
            result.onSuccess { lyrics ->
                Timber.tag(TAG).i("Success! Got ${lyrics.length} chars of lyrics")
            }.onFailure { e ->
                Timber.tag(TAG).e(e, "Failed to get lyrics")
            }
            result
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Exception in getLyrics")
            Result.failure(e)
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        Timber.tag(TAG).d("getAllLyrics called")
        try {
            Paxsenix.getAllLyrics(title, artist, duration, null) { lyrics ->
                if (lyrics.isNotBlank()) {
                    callback(lyrics)
                }
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Error fetching lyrics from Paxsenix")
        }
    }
}
