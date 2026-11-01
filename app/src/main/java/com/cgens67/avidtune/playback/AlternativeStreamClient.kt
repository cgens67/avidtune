package com.cgens67.avidtune.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AlternativeStreamClient {
    private const val TAG = "AlternativeStream"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Active public Piped instances that proxy audio
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://piped-api.privacy.com.de",
        "https://api.piped.yt"
    )

    fun getStreamingUrl(videoId: String): String? {
        for (instance in PIPED_INSTANCES) {
            try {
                val request = Request.Builder()
                    .url("$instance/streams/$videoId")
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
                                Timber.tag(TAG).i("Resolved fallback stream via Piped ($instance)")
                                return bestUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("Piped instance $instance failed for $videoId: ${e.message}")
            }
        }
        return null
    }
}