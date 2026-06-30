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

    private fun processLyrics(rawLyrics: String): String {
        if (rawLyrics.isBlank()) return rawLyrics

        // 1. Fix HTML entities (specifically &#x27; / &#39;)
        val processed = rawLyrics
            .replace("&#x27;", "'")
            .replace("&#x27", "'")
            .replace("&#39;", "'")
            .replace("&#39", "'")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")

        val lines = processed.lines()
        val result = StringBuilder()

        // Regex for capturing standard line timestamps
        val timeTagRegex = """^(\[\d{2,}:\d{2}\.\d{2,3}\])(.*)""".toRegex()
        
        // Regex for capturing trailing parentheses (backing vocals) and their preceding word-sync tag
        val splitRegex = """^(.*?)(\s*<\d{2,}:\d{2}\.\d{2,3}>\s*)?(\([^)]+\))$""".toRegex()

        for (line in lines) {
            val match = timeTagRegex.matchEntire(line.trim())
            if (match != null) {
                val timeTag = match.groupValues[1]
                val content = match.groupValues[2].trim()

                val splitMatch = splitRegex.matchEntire(content)
                if (splitMatch != null) {
                    val mainPart = splitMatch.groupValues[1].trim()
                    val syncTag = splitMatch.groupValues[2]
                    val bgPart = splitMatch.groupValues[3].trim()

                    // Check if there's actually main text before the parentheses
                    val cleanMain = mainPart.replace(Regex("""<\d{2,}:\d{2}\.\d{2,3}>"""), "").trim()
                    
                    if (cleanMain.isEmpty()) {
                        // 2A. Whole line is just background vocals
                        result.append(timeTag).append("{bg}").append(content).append("\n")
                    } else {
                        // 2B. Split main vocal and background vocal onto separate lines
                        result.append(timeTag).append(mainPart).append("\n")

                        val bgTimeTag = if (syncTag.isNotBlank()) {
                            val extractedTime = Regex("""<(\d{2,}:\d{2}\.\d{2,3})>""").find(syncTag)?.groupValues?.get(1)
                            if (extractedTime != null) "[$extractedTime]" else timeTag
                        } else timeTag

                        result.append(bgTimeTag).append("{bg}").append(syncTag.trimStart()).append(bgPart).append("\n")
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
