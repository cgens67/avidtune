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

    // Verified Piped instances
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.ducks.party",
        "https://api.piped.private.coffee"
    )

    // Verified Invidious instances
    private val INVIDIOUS_INSTANCES = listOf(
        "https://yt.chocolatemoo53.com"
    )

    fun getStreamingUrl(videoId: String): String? {
        Timber.tag(TAG).i("Starting alternative stream resolution for video ID: $videoId")

        // 1. Try Piped instances
        for (instance in PIPED_INSTANCES) {
            val url = "$instance/streams/$videoId"
            Timber.tag(TAG).d("[Piped] Requesting: $url")

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorSnippet = response.body?.string()?.take(200)?.trim() ?: "No body"
                        Timber.tag(TAG).w("[Piped] $instance returned HTTP ${response.code}: $errorSnippet")
                        return@use
                    }

                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        Timber.tag(TAG).w("[Piped] $instance returned an empty response body")
                        return@use
                    }

                    val json = JSONObject(body)
                    val audioStreams = json.optJSONArray("audioStreams")

                    if (audioStreams == null || audioStreams.length() == 0) {
                        Timber.tag(TAG).w("[Piped] $instance returned 0 audio streams (audioStreams array is empty)")
                        return@use
                    }

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
                        Timber.tag(TAG).i("[Piped] Resolved stream from $instance (bitrate: ${bestBitrate / 1000} kbps)")
                        return bestUrl
                    } else {
                        Timber.tag(TAG).w("[Piped] $instance had ${audioStreams.length()} audio streams but none had a valid URL")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Piped] Connection to $instance failed: ${e.message}")
            }
        }

        Timber.tag(TAG).d("Piped instances exhausted or returned no audio. Falling back to Invidious...")

        // 2. Fallback to Invidious instances
        for (instance in INVIDIOUS_INSTANCES) {
            val url = "$instance/api/v1/videos/$videoId"
            Timber.tag(TAG).d("[Invidious] Requesting: $url")

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorSnippet = response.body?.string()?.take(200)?.trim() ?: "No body"
                        Timber.tag(TAG).w("[Invidious] $instance returned HTTP ${response.code}: $errorSnippet")
                        return@use
                    }

                    val body = response.body?.string()
                    if (body.isNullOrBlank()) {
                        Timber.tag(TAG).w("[Invidious] $instance returned an empty response body")
                        return@use
                    }

                    val json = JSONObject(body)

                    if (json.has("error")) {
                        Timber.tag(TAG).w("[Invidious] $instance returned error: ${json.optString("error")}")
                        return@use
                    }

                    val adaptiveFormats = json.optJSONArray("adaptiveFormats")
                    if (adaptiveFormats == null || adaptiveFormats.length() == 0) {
                        Timber.tag(TAG).w("[Invidious] $instance returned 0 adaptiveFormats")
                        return@use
                    }

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
                        Timber.tag(TAG).i("[Invidious] Resolved stream from $instance (bitrate: ${bestBitrate / 1000} kbps)")
                        return bestUrl
                    } else {
                        Timber.tag(TAG).w("[Invidious] $instance returned adaptiveFormats, but none with type 'audio/'")
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Invidious] Connection to $instance failed: ${e.message}")
            }
        }

        Timber.tag(TAG).e("All alternative stream providers failed for video ID: $videoId")
        return null
    }
}