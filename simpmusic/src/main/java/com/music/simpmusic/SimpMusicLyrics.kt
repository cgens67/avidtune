package com.music.simpmusic

import com.music.simpmusic.models.LyricsData
import com.music.simpmusic.models.SimpMusicApiResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.math.abs

object SimpMusicLyrics {
    private const val BASE_URL = "https://api-lyrics.simpmusic.org/v1/"
    private const val FALLBACK_URL = "https://vivi-yt-music-server.onrender.com/v1/"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                        explicitNulls = false
                    },
                )
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }

            defaultRequest {
                url(BASE_URL)
                header(HttpHeaders.Accept, "application/json")
                header(HttpHeaders.UserAgent, "SimpMusicLyrics/1.0")
                header(HttpHeaders.ContentType, "application/json")
            }

            expectSuccess = false
        }
    }

    private fun processLyrics(lyrics: String): String {
        // Fix HTML entities
        var processed = lyrics.replace("&#x27;", "'")
            .replace("&#39;", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")

        val result = StringBuilder()
        val lines = processed.lines()

        val timeTagRegex = Regex("^\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]")
        val chunkRegex = Regex("(<\\d{2}:\\d{2}\\.\\d{2,3}>)?([^<]*)")

        for (line in lines) {
            val timeMatch = timeTagRegex.find(line)
            if (timeMatch != null) {
                val timeTag = timeMatch.value
                val textPart = line.substring(timeTag.length).trimStart()

                val textWithoutSync = textPart.replace(Regex("<[^>]+>"), "").trim()
                if (textWithoutSync.startsWith("(")) {
                    // The whole line is background
                    result.append(timeTag).append(" {bg}").append(textPart).append("\n")
                } else if (textWithoutSync.contains("(")) {
                    // Split main and background vocals
                    val mainPart = java.lang.StringBuilder()
                    val bgPart = java.lang.StringBuilder()
                    var inBg = false
                    var firstBgTimeTag: String? = null

                    val matches = chunkRegex.findAll(textPart)
                    for (match in matches) {
                        val syncTag = match.groups[1]?.value
                        val text = match.groups[2]?.value ?: ""
                        if (syncTag == null && text.isEmpty()) continue
                        
                        val firstNonWhitespaceIdx = text.indexOfFirst { !it.isWhitespace() }
                        val isFirstCharParen = firstNonWhitespaceIdx != -1 && text[firstNonWhitespaceIdx] == '('
                        
                        if (isFirstCharParen && !inBg) {
                            inBg = true
                            if (firstBgTimeTag == null && syncTag != null) {
                                firstBgTimeTag = syncTag
                            }
                            if (syncTag != null) bgPart.append(syncTag)
                        } else {
                            if (syncTag != null) {
                                if (inBg) bgPart.append(syncTag) else mainPart.append(syncTag)
                            }
                        }
                        
                        for (i in text.indices) {
                            val c = text[i]
                            if (c == '(') {
                                inBg = true
                            }
                            
                            if (inBg) bgPart.append(c) else mainPart.append(c)
                            
                            if (c == ')') {
                                inBg = false
                            }
                        }
                    }

                    val mainStr = mainPart.toString().trim()
                    val bgStr = bgPart.toString().trim()

                    if (mainStr.isNotEmpty()) {
                        result.append(timeTag).append(" ").append(mainStr.replace(Regex(" {2,}"), " ")).append("\n")
                    }
                    if (bgStr.isNotEmpty()) {
                        // Inherit or adjust the time tag for the background vocal line
                        val bgTimeTag = firstBgTimeTag?.replace("<", "[")?.replace(">", "]") ?: timeTag
                        result.append(bgTimeTag).append(" {bg}").append(bgStr).append("\n")
                    }
                } else {
                    result.append(line).append("\n")
                }
            } else {
                result.append(line).append("\n")
            }
        }
        return result.toString().trimEnd()
    }

    suspend fun getLyricsByVideoId(videoId: String): List<LyricsData> {
        val primaryAttempt = runCatching {
            val response = client.get(BASE_URL + videoId)
            
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<SimpMusicApiResponse>()
                if (apiResponse.success) {
                    apiResponse.data
                } else {
                    emptyList() // Successfully responded, but no lyrics
                }
            } else {
                null // Return null to trigger fallback (e.g. 502, 403, etc.)
            }
        }.getOrNull()

        if (primaryAttempt != null) {
            return primaryAttempt
        }

        // Fallback attempt
        return runCatching {
            val response = client.get(FALLBACK_URL + videoId)
            
            if (response.status == HttpStatusCode.OK) {
                val apiResponse = response.body<SimpMusicApiResponse>()
                if (apiResponse.success) {
                    apiResponse.data
                } else {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun getLyrics(
        videoId: String,
        duration: Int = 0,
    ): Result<String> = runCatching {
        val tracks = getLyricsByVideoId(videoId)
        
        if (tracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        // Filter tracks that match duration within tolerance (10 seconds)
        val validTracks = if (duration > 0) {
            tracks.filter { track ->
                abs((track.duration ?: 0) - duration) <= 10
            }
        } else {
            tracks
        }

        if (validTracks.isEmpty()) {
            throw IllegalStateException("Lyrics unavailable")
        }

        val bestMatch = if (duration > 0 && validTracks.size > 1) {
            validTracks.minByOrNull { track ->
                abs((track.duration ?: 0) - duration)
            }
        } else {
            validTracks.firstOrNull()
        }

        // Prioritize richSyncLyrics for word-by-word sync, then syncedLyrics, then plainLyrics
        val lyrics = bestMatch?.richSyncLyrics?.takeIf { it.isNotBlank() }
            ?: bestMatch?.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: bestMatch?.plainLyrics?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Lyrics unavailable")
        
        processLyrics(lyrics)
    }

    suspend fun getAllLyrics(
        videoId: String,
        duration: Int = 0,
        callback: (String) -> Unit,
    ) {
        val tracks = getLyricsByVideoId(videoId)
        var count = 0
        var plain = 0

        val sortedTracks = if (duration > 0) {
            tracks.sortedBy { abs((it.duration ?: 0) - duration) }
        } else {
            tracks
        }

        sortedTracks.forEach { track ->
            if (count <= 4) {
                // Check duration match - relaxed to 10 seconds or skip if duration is 0
                val durationMatch = duration <= 0 || abs((track.duration ?: 0) - duration) <= 10

                // Prioritize richSyncLyrics for word-by-word sync
                if (track.richSyncLyrics != null && track.richSyncLyrics.isNotBlank() && durationMatch) {
                    count++
                    callback(processLyrics(track.richSyncLyrics))
                } else if (track.syncedLyrics != null && track.syncedLyrics.isNotBlank() && durationMatch) {
                    count++
                    callback(processLyrics(track.syncedLyrics))
                }
                if (track.plainLyrics != null && track.plainLyrics.isNotBlank() && durationMatch && plain == 0) {
                    count++
                    plain++
                    callback(processLyrics(track.plainLyrics))
                }
            }
        }
    }
}
