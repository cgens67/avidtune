package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableLrcRedKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.music.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

object LrcRedLyricsProvider : LyricsProvider {
    override val name = "LrcRed"

    private const val BASE = "https://lrc.red/"
    private const val DURATION_TOLERANCE_SECONDS = 3.0

    private val ISRC = Regex("""[A-Z]{2}[A-Z0-9]{3}\d{7}""")
    private val BRACKETED = Regex("""[(\[][^)\]]*[)\]]""")
    private val COMBINING_MARKS = Regex("""\p{Mn}+""")
    private val WHITESPACE = Regex("""\s+""")
    private val ARTIST_SEPARATORS = Regex(
        """\s*(?:,|&|;|/|\s+and\s+|\s+x\s+|\s+with\s+|\s+feat\.?\s+|\s+ft\.?\s+)\s*""",
        RegexOption.IGNORE_CASE,
    )

    private val VERSION_WORDS = setOf(
        "live", "remix", "remixed", "mix", "acoustic", "unplugged", "instrumental",
        "karaoke", "cappella", "acapella", "demo", "edit", "version", "cover",
        "sped", "slowed", "reverb", "nightcore", "lofi", "orchestral", "extended",
    )

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableLrcRedKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        val durationMs = if (duration > 10000) duration.toLong() else if (duration > 0) duration * 1000L else 0L
        val knownUrl = documentUrl(id)
        if (knownUrl != null) {
            fetchAndParseTtml(knownUrl)?.let { return@runCatching it }
        }

        val hits = searchHits(title, artist)
        val bestHit = best(hits, title, artist, durationMs)
            ?: throw IllegalStateException("No matching lyrics found on lrc.red")

        val foundUrl = bestHit.isrc?.let(::documentUrl)?.takeIf { it != knownUrl }
            ?: throw IllegalStateException("Invalid ISRC on lrc.red hit")

        fetchAndParseTtml(foundUrl)
            ?: throw IllegalStateException("Failed to fetch or parse TTML from lrc.red")
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            val durationMs = if (duration > 10000) duration.toLong() else if (duration > 0) duration * 1000L else 0L
            val seenIsrcs = HashSet<String>()

            val knownUrl = documentUrl(id)
            if (knownUrl != null) {
                seenIsrcs.add(id.trim().uppercase(Locale.ROOT))
                fetchAndParseTtml(knownUrl)?.let(callback)
            }

            val hits = searchHits(title, artist)
            val bestHit = best(hits, title, artist, durationMs)
            val orderedHits = buildList {
                if (bestHit != null) add(bestHit)
                hits.forEach { hit ->
                    if (hit != bestHit) add(hit)
                }
            }

            var emitted = 0
            for (hit in orderedHits) {
                if (emitted >= 5) break
                val isrc = hit.isrc?.trim()?.uppercase(Locale.ROOT) ?: continue
                if (!seenIsrcs.add(isrc)) continue
                val url = documentUrl(isrc) ?: continue
                val lrc = fetchAndParseTtml(url)
                if (!lrc.isNullOrBlank()) {
                    callback(lrc)
                    emitted++
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in LrcRed getAllLyrics")
        }
    }

    private fun documentUrl(isrc: String): String? =
        isrc.trim().uppercase(Locale.ROOT).takeIf { ISRC.matches(it) }?.let { "${BASE}s/$it.ttml" }

    private suspend fun fetchAndParseTtml(url: String): String? = runCatching {
        val response = client.get(url)
        if (!response.status.isSuccess()) return@runCatching null
        val ttml = response.bodyAsText()
        if (ttml.isBlank()) return@runCatching null
        val parsedLines = TTMLParser.parseTTML(ttml)
        if (parsedLines.isEmpty()) return@runCatching null
        TTMLParser.toLRC(parsedLines).trim().takeIf { it.isNotBlank() }
    }.getOrNull()

    private suspend fun searchHits(title: String, artist: String): List<Hit> = runCatching {
        val query = "$title $artist".trim()
        if (query.isBlank()) return@runCatching emptyList()
        val response = client.get("${BASE}search.json") {
            parameter("q", query)
        }
        if (!response.status.isSuccess()) return@runCatching emptyList()
        val body = response.bodyAsText()
        json.decodeFromString<Response>(body).hits.orEmpty()
    }.getOrDefault(emptyList())

    private fun best(hits: List<Hit>, title: String, artist: String, durationMs: Long): Hit? {
        val wantedTitle = coreOf(title)
        val wantedVersion = versionOf(title)
        val wantedArtists = artistsOf(artist)
        val seconds = durationMs / 1000.0

        fun distance(hit: Hit): Double =
            if (durationMs > 0 && hit.duration != null) abs(hit.duration - seconds) else 0.0

        return hits
            .filter { hit ->
                val name = hit.title ?: return@filter false
                hit.isrc?.let(::documentUrl) != null &&
                    coreOf(name) == wantedTitle &&
                    versionOf(name) == wantedVersion &&
                    (wantedArtists.isEmpty() || artistsOf(hit.artist.orEmpty()).any { it in wantedArtists }) &&
                    distance(hit) <= DURATION_TOLERANCE_SECONDS
            }
            .minByOrNull(::distance)
    }

    private fun coreOf(title: String): String =
        normalized(title.replace(BRACKETED, " ").substringBefore(" - "))
            .ifEmpty { normalized(title) }

    private fun versionOf(title: String): Set<String> {
        val extras = BRACKETED.findAll(title).joinToString(" ") { it.value } +
            " " + title.substringAfter(" - ", "")
        return normalized(extras).split(' ').filter { it in VERSION_WORDS }.toSet()
    }

    private fun artistsOf(artist: String): Set<String> =
        artist.split(ARTIST_SEPARATORS).map(::normalized).filter { it.isNotEmpty() }.toSet()

    private fun normalized(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()

    @Serializable
    private data class Response(
        val hits: List<Hit>? = null,
        val next: String? = null,
    )

    @Serializable
    private data class Hit(
        val isrc: String? = null,
        val title: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val year: Int? = null,
        val duration: Double? = null,
    )
}