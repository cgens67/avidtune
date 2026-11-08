package com.cgens67.avidtune.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AlternativeStreamClient {
    private const val TAG = "AlternativeStream"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Verified working Piped servers
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.ducks.party",
        "https://api.piped.private.coffee"
    )

    // Verified working Invidious servers
    private val INVIDIOUS_INSTANCES = listOf(
        "https://yt.chocolatemoo53.com"
    )

    fun getStreamingUrl(videoId: String): String? {
        // 1. Try Piped instances first
        for (instance in PIPED_INSTANCES) {
            try {
                val request = Request.Builder()
                    .url("$instance/streams/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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
                Timber.tag(TAG).w("Piped $instance failed for $videoId: ${e.message}")
            }
        }

        // 2. Fallback to Invidious if Piped fails or has no audio streams
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val request = Request.Builder()
                    .url("$instance/api/v1/videos/$videoId")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = JSONObject(body)
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                        
                        if (adaptiveFormats != null && adaptiveFormats.length() > 0) {
                            var bestUrl: String? = null
                            var bestBitrate = -1

                            for (i in 0 until adaptiveFormats.length()) {
                                val format = adaptiveFormats.getJSONObject(i)
                                val type = format.optString("type", "")

                                if (type.startsWith("audio/")) {
                                    val streamUrl = format.optString("url")
                                    val bitrate = format.optInt("bitrate", 0)

                                    if (streamUrl.isNotBlank() && bitrate > bestBitrate) {
                                        bestBitrate = bitrate
                                        bestUrl = streamUrl
                                    }
                                }
                            }

                            if (bestUrl != null) {
                                Timber.tag(TAG).i("Resolved stream via Invidious ($instance)")
                                return bestUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w("Invidious $instance failed for $videoId: ${e.message}")
            }
        }

        return null
    }
}