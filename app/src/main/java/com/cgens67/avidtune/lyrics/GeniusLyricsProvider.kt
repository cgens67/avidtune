package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableGeniusKey
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

@Serializable
private data class GeniusPrimaryArtist(
    val name: String? = null
)

@Serializable
private data class GeniusResult(
    val url: String? = null,
    val title: String? = null,
    val primary_artist: GeniusPrimaryArtist? = null
)

@Serializable
private data class GeniusHit(
    val type: String? = null,
    val index: String? = null,
    val result: GeniusResult? = null
)

@Serializable
private data class GeniusSection(
    val type: String? = null,
    val hits: List<GeniusHit> = emptyList()
)

@Serializable
private data class GeniusResponse(
    val sections: List<GeniusSection> = emptyList()
)

@Serializable
private data class GeniusSearchRoot(
    val response: GeniusResponse? = null
)

object GeniusLyricsProvider : LyricsProvider {
    override val name = "Genius"

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
        context.dataStore[EnableGeniusKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        if (title.isBlank()) throw IllegalArgumentException("Title is empty")

        val query = "$title $artist".trim()
        val searchResponse = client.get("https://genius.com/api/search/multi") {
            parameter("q", query)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        }

        if (searchResponse.status != HttpStatusCode.OK) {
            throw IllegalStateException("Genius search failed with status ${searchResponse.status}")
        }

        val searchData = searchResponse.body<GeniusSearchRoot>()
        val sections = searchData.response?.sections.orEmpty()

        // Filter specifically for song hits
        val songHits = sections.flatMap { it.hits }
            .filter { hit ->
                val isSong = hit.type == "song" || hit.index == "song" || hit.result?.url?.contains("-lyrics") == true
                isSong && !hit.result?.url.isNullOrBlank()
            }

        if (songHits.isEmpty()) {
            throw IllegalStateException("No song results found on Genius for $query")
        }

        // Find best matching hit based on title and artist
        val cleanTitle = title.lowercase().trim()
        val cleanArtist = artist.lowercase().trim()

        val bestHit = songHits.firstOrNull { hit ->
            val resultTitle = hit.result?.title?.lowercase().orEmpty()
            val resultArtist = hit.result?.primary_artist?.name?.lowercase().orEmpty()

            (resultTitle.contains(cleanTitle) || cleanTitle.contains(resultTitle)) &&
                    (cleanArtist.isBlank() || resultArtist.contains(cleanArtist) || cleanArtist.contains(resultArtist))
        } ?: songHits.first()

        val songUrl = bestHit.result?.url
            ?: throw IllegalStateException("No URL found for Genius song")

        val lyricsText = withContext(Dispatchers.IO) {
            val doc = Jsoup.connect(songUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(10000)
                .get()

            // Remove non-lyric metadata, headers, scripts, annotations, and contributor bars
            doc.select("[data-exclude-from-selection=true]").remove()
            doc.select("header").remove()
            doc.select("script").remove()
            doc.select("style").remove()

            // Convert <br> tags into newlines
            doc.select("br").forEach { it.replaceWith(org.jsoup.nodes.TextNode("\n")) }

            // Extract lyric containers
            val containers = doc.select("div[data-lyrics-container=true]")
            val extractedText = if (containers.isNotEmpty()) {
                containers.joinToString("\n\n") { container ->
                    container.wholeText().trim()
                }
            } else {
                doc.select(".lyrics").text().trim()
            }

            // Remove residual header junk if present (using inline (?is) flag to ignore case and allow dot matches all)
            extractedText
                .replace(Regex("(?is)^\\d*\\s*Contributors?.*?(Lyrics|Letras|Paroles)\\n*"), "")
                .replace(Regex("(?i)^.*?Lyrics\\b\\n*"), "")
                .trim()
        }

        if (lyricsText.isBlank()) {
            throw IllegalStateException("Lyrics empty on Genius for $songUrl")
        }

        lyricsText
    }
}
