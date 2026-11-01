package com.cgens67.avidtune.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AlternativeStreamClient {
    private const val TAG = "AlternativeStream"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Public Piped API instances that proxy audio streams
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://piped-api.lunar.icu",
        "https://api.piped.privacydev.net"
    )

    // Public Invidious instances supporting proxied audio
    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.tux.pizza",
        "https://invidious.nerdvpn.de"
    )

    /**
     * Resolves an alternative audio stream URL when native InnerTube fails
     */
    fun getStreamingUrl(videoId: String): String? {
        // 1. Try Piped API instances
        for (instance in PIPED_INSTANCES) {
            try {
                val requestUrl = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(requestUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val audioStreams = json.optJSONArray("audioStreams")
                        if (audioStreams != null && audioStreams.length() > 0) {
                            var bestUrl: String? = null
                            var bestBitrate = -1

                            for (i in 0 until audioStreams.length()) {
                                val stream = audioStreams.getJSONObject(i)
                                val streamUrl = stream.optString("url")
                                val bitrate = stream.optInt("bitrate", 0)

                                if (streamUrl.isNotBlank() && bitrate > bestBitrate) {
                                    bestBitrate = bitrate
                                    bestUrl = streamUrl
                                }
                            }

                            if (bestUrl != null) {
                                Timber.tag(TAG).i("Resolved stream via Piped ($instance)")
                                return bestUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("Piped instance $instance failed: ${e.message}")
            }
        }

        // 2. Try Invidious instances with proxied audio (local=true)
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val streamUrl = "$instance/latest_version?id=$videoId&itag=140&local=true"
                val request = Request.Builder()
                    .url(streamUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .head()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful || response.code in 300..399) {
                        Timber.tag(TAG).i("Resolved stream via Invidious ($instance)")
                        return streamUrl
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("Invidious instance $instance failed: ${e.message}")
            }
        }

        return null
    }
}