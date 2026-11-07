package com.cgens67.avidtune.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import com.cgens67.avidtune.constants.ShareLinkFormat
import com.cgens67.avidtune.constants.ShareLinkFormatKey
import com.cgens67.avidtune.extensions.toEnum

object ShareUtils {
    const val AVIDTUNE_BASE_URL = "https://avidtune.vercel.app"

    fun buildAvidTuneUrl(videoId: String, positionMs: Long? = null): String {
        val seconds = positionMs?.let { it / 1000 }?.takeIf { it > 0 }
        return if (seconds != null) {
            "$AVIDTUNE_BASE_URL/$videoId/$seconds"
        } else {
            "$AVIDTUNE_BASE_URL/$videoId"
        }
    }

    fun buildYouTubeMusicUrl(videoId: String, positionMs: Long? = null): String {
        val seconds = positionMs?.let { it / 1000 }?.takeIf { it > 0 }
        return if (seconds != null) {
            "https://music.youtube.com/watch?v=$videoId&t=$seconds"
        } else {
            "https://music.youtube.com/watch?v=$videoId"
        }
    }

    fun buildSongShareUrl(videoId: String, positionMs: Long? = null): String {
        return buildAvidTuneUrl(videoId, positionMs)
    }

    fun buildSongShareUrl(context: Context, videoId: String, positionMs: Long? = null): String {
        val format = context.dataStore.get(ShareLinkFormatKey).toEnum(ShareLinkFormat.AVIDTUNE)
        return when (format) {
            ShareLinkFormat.AVIDTUNE, ShareLinkFormat.ASK -> buildAvidTuneUrl(videoId, positionMs)
            ShareLinkFormat.YOUTUBE_MUSIC -> buildYouTubeMusicUrl(videoId, positionMs)
        }
    }

    private fun launchShareIntent(context: Context, url: String, title: String? = null) {
        val shareText = if (!title.isNullOrBlank()) "$title: $url" else url
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    fun shareSong(
        context: Context,
        videoId: String,
        title: String? = null,
        positionMs: Long? = null
    ) {
        val format = context.dataStore.get(ShareLinkFormatKey).toEnum(ShareLinkFormat.AVIDTUNE)

        when (format) {
            ShareLinkFormat.AVIDTUNE -> {
                val url = buildAvidTuneUrl(videoId, positionMs)
                launchShareIntent(context, url, title)
            }
            ShareLinkFormat.YOUTUBE_MUSIC -> {
                val url = buildYouTubeMusicUrl(videoId, positionMs)
                launchShareIntent(context, url, title)
            }
            ShareLinkFormat.ASK -> {
                val avidTuneUrl = buildAvidTuneUrl(videoId, positionMs)
                val ytmUrl = buildYouTubeMusicUrl(videoId, positionMs)

                val options = arrayOf(
                    "AvidTune ($avidTuneUrl)",
                    "YouTube Music ($ytmUrl)"
                )

                AlertDialog.Builder(context)
                    .setTitle("Share song as")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> launchShareIntent(context, avidTuneUrl, title)
                            1 -> launchShareIntent(context, ytmUrl, title)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
}