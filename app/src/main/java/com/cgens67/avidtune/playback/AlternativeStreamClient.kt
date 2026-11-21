package com.cgens67.avidtune.playback

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import java.util.concurrent.TimeUnit

data class AlternativeStreamData(
    val streamUrl: String,
    val clientName: String,
    val itag: Int = 0,
    val mimeType: String = "audio/webm",
    val codecs: String = "opus",
    val bitrate: Int = 0,
    val sampleRate: Int? = 48000,
    val contentLength: Long = 0L,
)

data class AlternativeVideoStreamData(
    val streamUrl: String,
    val clientName: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val availableHeights: List<Int>,
)

object AlternativeStreamClient {
    private const val TAG = "AlternativeStream"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.ducks.party",
        "https://api.piped.private.coffee"
    )

    private val INVIDIOUS_INSTANCES = listOf(
        "https://yt.chocolatemoo53.com"
    )

    fun getStreamData(videoId: String): AlternativeStreamData? {
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

                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val audioStreams = json.optJSONArray("audioStreams")

                    if (audioStreams == null || audioStreams.length() == 0) {
                        Timber.tag(TAG).w("[Piped] $instance returned 0 audio streams")
                        return@use
                    }

                    var bestStreamObj: JSONObject? = null
                    var bestBitrate = -1

                    for (i in 0 until audioStreams.length()) {
                        val stream = audioStreams.getJSONObject(i)
                        val streamUrl = stream.optString("url")
                        val bitrate = stream.optInt("bitrate", 0)

                        if (streamUrl.isNotBlank() && bitrate > bestBitrate) {
                            bestBitrate = bitrate
                            bestStreamObj = stream
                        }
                    }

                    if (bestStreamObj != null) {
                        val streamUrl = bestStreamObj.optString("url")
                        val host = runCatching { URI(instance).host }.getOrNull() ?: instance
                        val rawMime = bestStreamObj.optString("mimeType", "audio/webm")
                        val mimeType = rawMime.substringBefore(";").trim()
                        val codecs = bestStreamObj.optString("codec", "opus")
                        val itag = bestStreamObj.optInt("itag", 251)
                        val contentLength = bestStreamObj.optLong("contentLength", 0L)

                        Timber.tag(TAG).i("[Piped] Resolved stream from $instance (bitrate: ${bestBitrate / 1000} kbps)")

                        return AlternativeStreamData(
                            streamUrl = streamUrl,
                            clientName = "Piped ($host)",
                            itag = itag,
                            mimeType = mimeType,
                            codecs = codecs,
                            bitrate = bestBitrate,
                            sampleRate = 48000,
                            contentLength = contentLength
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Piped] Connection to $instance failed: ${e.message}")
            }
        }

        Timber.tag(TAG).d("Piped instances exhausted. Falling back to Invidious with proxying...")

        // 2. Try Invidious with ?local=true
        for (instance in INVIDIOUS_INSTANCES) {
            val url = "$instance/api/v1/videos/$videoId?local=true"
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

                    val body = response.body?.string() ?: return@use
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

                    var bestFormatObj: JSONObject? = null
                    var bestBitrate = -1

                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.getJSONObject(i)
                        val type = format.optString("type", "")

                        if (type.startsWith("audio/")) {
                            val streamUrl = format.optString("url")
                            val bitrate = format.optInt("bitrate", 0)

                            if (streamUrl.isNotBlank() && bitrate > bestBitrate) {
                                bestBitrate = bitrate
                                bestFormatObj = format
                            }
                        }
                    }

                    if (bestFormatObj != null) {
                        var streamUrl = bestFormatObj.optString("url")
                        if (streamUrl.startsWith("/")) {
                            streamUrl = "$instance$streamUrl"
                        }

                        val host = runCatching { URI(instance).host }.getOrNull() ?: instance
                        val rawType = bestFormatObj.optString("type", "audio/webm; codecs=\"opus\"")
                        val mimeType = rawType.substringBefore(";").trim()
                        val codecs = if (rawType.contains("codecs=")) {
                            rawType.substringAfter("codecs=").replace("\"", "").replace("'", "").trim()
                        } else {
                            "opus"
                        }
                        val itag = bestFormatObj.optInt("itag", 251)
                        val contentLength = bestFormatObj.optLong("clen", bestFormatObj.optLong("contentLength", 0L))
                        val sampleRate = bestFormatObj.optInt("audioSampleRate", 48000)

                        Timber.tag(TAG).i("[Invidious] Resolved stream from $instance (bitrate: ${bestBitrate / 1000} kbps)")

                        return AlternativeStreamData(
                            streamUrl = streamUrl,
                            clientName = "Invidious ($host)",
                            itag = itag,
                            mimeType = mimeType,
                            codecs = codecs,
                            bitrate = bestBitrate,
                            sampleRate = sampleRate,
                            contentLength = contentLength
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Invidious] Connection to $instance failed: ${e.message}")
            }
        }

        Timber.tag(TAG).e("All alternative stream providers failed for video ID: $videoId")
        return null
    }

    fun getVideoStreamData(videoId: String, maxHeight: Int): AlternativeVideoStreamData? {
        data class Candidate(
            val url: String,
            val width: Int,
            val height: Int,
            val mimeType: String,
            val videoOnly: Boolean,
        )

        fun pickCandidate(candidates: List<Candidate>, sourceName: String): AlternativeVideoStreamData? {
            if (candidates.isEmpty()) return null
            val heights = candidates.map { it.height }.filter { it > 0 }.distinct().sortedDescending()
            val targetHeight = heights.filter { it <= maxHeight }.maxOrNull() ?: heights.minOrNull() ?: return null
            val chosen = candidates.filter { it.height == targetHeight }.maxByOrNull { c ->
                val videoOnlyScore = if (c.videoOnly) 1000 else 0
                val mime = c.mimeType.lowercase()
                val codecScore = if (maxHeight <= 1080) {
                    when {
                        "mp4" in mime || "avc1" in mime -> 300
                        "vp9" in mime || "webm" in mime -> 200
                        else -> 100
                    }
                } else {
                    when {
                        "vp9" in mime || "webm" in mime -> 300
                        "mp4" in mime -> 200
                        else -> 100
                    }
                }
                videoOnlyScore + codecScore
            } ?: return null

            return AlternativeVideoStreamData(
                streamUrl = chosen.url,
                clientName = sourceName,
                width = chosen.width,
                height = chosen.height,
                mimeType = chosen.mimeType,
                availableHeights = heights,
            )
        }

        for (instance in PIPED_INSTANCES) {
            val url = "$instance/streams/$videoId"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val videoStreams = json.optJSONArray("videoStreams") ?: return@use
                    val candidates = buildList {
                        for (i in 0 until videoStreams.length()) {
                            val s = videoStreams.getJSONObject(i)
                            val streamUrl = s.optString("url")
                            val mime = s.optString("mimeType", "video/mp4")
                            val h = s.optInt("height", 0).takeIf { it > 0 }
                                ?: Regex("""(\d+)p""").find(s.optString("quality"))?.groupValues?.get(1)?.toIntOrNull()
                                ?: 0
                            val w = s.optInt("width", 0).takeIf { it > 0 } ?: (h * 16 / 9)
                            if (streamUrl.isNotBlank() && h > 0 && mime.startsWith("video/", ignoreCase = true)) {
                                add(Candidate(streamUrl, w, h, mime, s.optBoolean("videoOnly", true)))
                            }
                        }
                    }
                    val host = runCatching { URI(instance).host }.getOrNull() ?: instance
                    pickCandidate(candidates, "Piped ($host)")?.let { return it }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Piped Video] Connection to $instance failed: ${e.message}")
            }
        }

        for (instance in INVIDIOUS_INSTANCES) {
            val url = "$instance/api/v1/videos/$videoId?local=true"
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    if (json.has("error")) return@use
                    val adaptiveFormats = json.optJSONArray("adaptiveFormats") ?: return@use
                    val candidates = buildList {
                        for (i in 0 until adaptiveFormats.length()) {
                            val f = adaptiveFormats.getJSONObject(i)
                            val type = f.optString("type", "")
                            if (!type.startsWith("video/", ignoreCase = true)) continue
                            var streamUrl = f.optString("url")
                            if (streamUrl.isBlank()) continue
                            if (streamUrl.startsWith("/")) streamUrl = "$instance$streamUrl"
                            val sizeParts = f.optString("size").split("x")
                            val parsedW = sizeParts.getOrNull(0)?.toIntOrNull() ?: 0
                            val parsedH = sizeParts.getOrNull(1)?.toIntOrNull()
                                ?: Regex("""(\d+)p""").find(f.optString("qualityLabel"))?.groupValues?.get(1)?.toIntOrNull()
                                ?: 0
                            val w = if (parsedW > 0) parsedW else (parsedH * 16 / 9)
                            if (parsedH > 0) {
                                add(Candidate(streamUrl, w, parsedH, type, true))
                            }
                        }
                    }
                    val host = runCatching { URI(instance).host }.getOrNull() ?: instance
                    pickCandidate(candidates, "Invidious ($host)")?.let { return it }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[Invidious Video] Connection to $instance failed: ${e.message}")
            }
        }

        return null
    }

    fun getStreamingUrl(videoId: String): String? = getStreamData(videoId)?.streamUrl
}