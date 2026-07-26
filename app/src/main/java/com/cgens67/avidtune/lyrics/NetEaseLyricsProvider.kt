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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
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
    val nolyric: Boolean? = null,
    val uncollected: Boolean? = null,
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

    private suspend fun searchSongs(query: String): List<NetEaseSong> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // 1. Try POST to NetEase Web API
        val response = runCatching {
            client.post("https://music.163.com/api/search/get/web") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Referer", "https://music.163.com/")
                header("Cookie", "appver=2.0.2; os=pc;")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("s=$encodedQuery&type=1&offset=0&limit=5")
            }
        }.getOrNull()

        if (response != null && response.status == HttpStatusCode.OK) {
            val searchData = runCatching { response.body<NetEaseSearchResponse>() }.getOrNull()
            val songs = searchData?.result?.songs.orEmpty()
            if (songs.isNotEmpty()) return songs
        }

        // 2. Fallback to GET endpoint
        val getResponse = runCatching {
            client.get("https://music.163.com/api/search/get") {
                parameter("s", query)
                parameter("type", "1")
                parameter("offset", 0)
                parameter("limit", 5)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                header("Referer", "https://music.163.com/")
                header("Cookie", "appver=2.0.2; os=pc;")
            }
        }.getOrNull()

        if (getResponse != null && getResponse.status == HttpStatusCode.OK) {
            val searchData = runCatching { getResponse.body<NetEaseSearchResponse>() }.getOrNull()
            return searchData?.result?.songs.orEmpty()
        }

        return emptyList()
    }

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        if (title.isBlank()) throw IllegalArgumentException("Title is empty")

        // Search with Title + Artist, fallback to Title only
        var songs = searchSongs("$title $artist".trim())
        if (songs.isEmpty() && artist.isNotBlank()) {
            songs = searchSongs(title.trim())
        }

        if (songs.isEmpty()) {
            throw IllegalStateException("No songs found on NetEase for $title $artist")
        }

        // Match song by closest duration or pick the first
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
            parameter("os", "pc")
            parameter("lv", "-1")
            parameter("kv", "-1")
            parameter("tv", "-1")
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header("Referer", "https://music.163.com/")
            header("Cookie", "appver=2.0.2; os=pc;")
        }

        if (lyricResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("NetEase lyric fetch failed with status ${lyricResponse.status}")
        }

        val lyricData = lyricResponse.body<NetEaseLyricResponse>()

        if (lyricData.nolyric == true || lyricData.uncollected == true) {
            throw IllegalStateException("No lyrics available for NetEase song ID ${bestSong.id}")
        }

        val lyricText = lyricData.lrc?.lyric

        if (lyricText.isNullOrBlank() || lyricText.contains("纯音乐，请欣赏")) {
            throw IllegalStateException("Lyrics empty or instrumental for NetEase song ID ${bestSong.id}")
        }

        lyricText
    }
}
