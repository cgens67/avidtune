package com.cgens67.avidtune.utils

import android.content.Context
import android.content.Intent

object ShareUtils {
    const val BASE_SHARE_URL = "https://avidtune.vercel.app"

    fun buildSongShareUrl(videoId: String, positionMs: Long? = null): String {
        val seconds = positionMs?.let { it / 1000 }?.takeIf { it > 0 }
        return if (seconds != null) {
            "$BASE_SHARE_URL/$videoId/$seconds"
        } else {
            "$BASE_SHARE_URL/$videoId"
        }
    }

    fun shareSong(
        context: Context,
        videoId: String,
        title: String? = null,
        positionMs: Long? = null
    ) {
        val url = buildSongShareUrl(videoId, positionMs)
        val shareText = if (!title.isNullOrBlank()) "$title: $url" else url
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}