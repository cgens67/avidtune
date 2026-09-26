package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.music.betterlyrics.TTMLParser
import com.cgens67.avidtune.constants.EnableBiniLyricsKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
private data class BiniLyricsApiResponse(
    val total: Int? = null,
    val source: String? = null,
    val results: List<BiniLyricsResult> = emptyList(),
    val error: String? = null,
)

@Serializable
private data class BiniLyricsResult(
    val id: String? = null,
    val track_name: String? = null,
    val artist_name: String? = null,
    val album_name: String? = null,
    val duration: Int? = null,
    val isrc: String? = null,
    val timing_type: String? = null,
    val lyricsUrl: String? = null,
)

object BiniLyricsProvider : LyricsProvider {
    override val name = "BiniLyrics"
    private const val BASE_URL = "https://lyrics-api.binimum.org/"

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableBiniLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        val response = client.get(BASE_URL) {
            parameter("track", title)
            parameter("artist", artist)
            if (duration > 0) parameter("duration", duration)
        }

        if (!response.status.isSuccess()) throw IllegalStateException("BiniLyrics HTTP Error: ${response.status}")

        val payload = response.body<BiniLyricsApiResponse>()
        val selectedResult = payload.results.firstOrNull { !it.lyricsUrl.isNullOrBlank() }
            ?: throw IllegalStateException("No lyrics found on BiniLyrics")

        val ttmlResponse = client.get(selectedResult.lyricsUrl!!)
        if (!ttmlResponse.status.isSuccess()) throw IllegalStateException("Failed to fetch TTML from BiniLyrics")

        val ttml = ttmlResponse.body<String>()
        val parsedLines = TTMLParser.parseTTML(ttml)
        val lrc = TTMLParser.toLRC(parsedLines).trim()
        if (lrc.isBlank()) throw IllegalStateException("Empty LRC from BiniLyrics")

        lrc
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            val response = client.get(BASE_URL) {
                parameter("track", title)
                parameter("artist", artist)
                if (duration > 0) parameter("duration", duration)
            }

            if (!response.status.isSuccess()) return

            val payload = response.body<BiniLyricsApiResponse>()
            payload.results.filter { !it.lyricsUrl.isNullOrBlank() }.forEach { result ->
                try {
                    val ttmlResponse = client.get(result.lyricsUrl!!)
                    if (ttmlResponse.status.isSuccess()) {
                        val ttml = ttmlResponse.body<String>()
                        val parsedLines = TTMLParser.parseTTML(ttml)
                        val lrc = TTMLParser.toLRC(parsedLines).trim()
                        if (lrc.isNotBlank()) {
                            callback(lrc)
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag("BiniLyrics").e(e, "Failed to fetch/parse one result")
                }
            }
        } catch (e: Throwable) {
            Timber.tag("BiniLyrics").e(e, "Error in BiniLyrics getAllLyrics")
        }
    }
}
