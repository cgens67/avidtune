package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableNetEaseKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs

@Serializable
private data class NetEaseArtist(
    val name: String? = null
)

@Serializable
private data class NetEaseSong(
    val id: Long,
    val name: String? = null,
    val artists: List<NetEaseArtist> = emptyList(),
    val duration: Int? = null
)

@Serializable
private data class NetEaseSearchResult(
    val songs: List<NetEaseSong> = emptyList()
)

@Serializable
private data class NetEaseSearchResponse(
    val result: NetEaseSearchResult? = null,
    val code: Int = 0
)

@Serializable
private data class NetEaseLyricContent(
    val lyric: String? = null
)

@Serializable
private data class NetEaseLyricResponse(
    val lrc: NetEaseLyricContent? = null,
    val tlyric: NetEaseLyricContent? = null,
    val code: Int = 0
)

object NetEaseLyricsProvider : LyricsProvider {
    override val name = "NetEase"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 5000
            }
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableNetEaseKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        if (title.isBlank()) throw IllegalArgumentException("Title is empty")

        val query = "$title $artist".trim()
        val searchResponse = client.get("https://music.163.com/api/search/get/web") {
            parameter("s", query)
            parameter("type", "1")
            parameter("offset", 0)
            parameter("limit", 5)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Referer", "https://music.163.com/")
        }

        if (searchResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("NetEase search failed with status ${searchResponse.status}")
        }

        val searchData = searchResponse.body<NetEaseSearchResponse>()
        val songs = searchData.result?.songs.orEmpty()
        if (songs.isEmpty()) {
            throw IllegalStateException("No songs found on NetEase for $query")
        }

        val targetDurationMs = if (duration > 0) duration * 1000 else -1
        val bestSong = if (targetDurationMs > 0) {
            songs.minByOrNull { song ->
                val songDuration = song.duration ?: 0
                abs(songDuration - targetDurationMs)
            } ?: songs.first()
        } else {
            songs.first()
        }

        val lyricResponse = client.get("https://music.163.com/api/song/lyric") {
            parameter("id", bestSong.id)
            parameter("lv", "-1")
            parameter("kv", "-1")
            parameter("tv", "-1")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Referer", "https://music.163.com/")
        }

        if (lyricResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("NetEase lyric fetch failed with status ${lyricResponse.status}")
        }

        val lyricData = lyricResponse.body<NetEaseLyricResponse>()
        val lyricText = lyricData.lrc?.lyric

        if (lyricText.isNullOrBlank()) {
            throw IllegalStateException("Lyrics empty for NetEase song ID ${bestSong.id}")
        }

        lyricText
    }
}
