package com.cgens67.paxsenix

import android.content.Context
import com.cgens67.music.betterlyrics.TTMLParser
import com.cgens67.paxsenix.models.LyricsResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.math.abs

object Paxsenix {
    private const val TAG = "Paxsenix"
    private const val PUBLIC_PROXY = "https://lyrics.paxsenix.org"
    private const val APPLE_SEARCH = "https://amp-api.music.apple.com/v1/catalog/us/search"
    private const val MINIMUM_MATCH_SCORE = 10

    private val tokenMutex = Mutex()
    private val cachedAppleToken = AtomicReference<String?>(null)

    @Volatile
    private var appVersion: String = "Unknown"

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val httpClient by lazy {
        // Ensures OkHttp dependency from ktor-client-okhttp is kept active
        OkHttpClient.Builder()
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Keep reference to OkHttp engine class so unused import isn't an issue if inspected
    @Suppress("unused")
    private val engineMarker = OkHttp

    fun init(context: Context) {
        if (appVersion != "Unknown") return
        appVersion = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            Timber.e(e, "Failed to get app version")
            "Unknown"
        }
    }

    suspend fun getLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val durationMs = duration.toDurationMsInput()
            val candidates = searchPublicAppleCandidates(title, artist, durationMs)
            val best = candidates.firstOrNull()
                ?: throw IllegalStateException("No tracks found on Paxsenix")

            fetchLyricsForTrack(best.id)
                ?: throw IllegalStateException("No lyrics available from Paxsenix")
        }
    }

    suspend fun getAllLyrics(
        title: String,
        artist: String,
        duration: Int,
        album: String? = null,
        callback: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val durationMs = duration.toDurationMsInput()
        val candidates = searchPublicAppleCandidates(title, artist, durationMs)
        if (candidates.isEmpty()) return@withContext

        val collected = mutableListOf<Pair<String, Int>>()
        for (candidate in candidates.take(3)) {
            val lrc = fetchLyricsForTrack(candidate.id) ?: continue
            val quality = getQuality(lrc)
            collected.add(lrc to quality)
            if (quality == 3) break
        }

        collected.sortedByDescending { it.second }.forEach { (lrc, _) ->
            callback(lrc)
        }
    }

    private suspend fun searchPublicAppleCandidates(
        title: String,
        artist: String,
        durationMs: Long,
    ): List<Candidate> {
        val cleanedTitle = cleanTitle(title).ifBlank { title.trim() }
        val cleanedArtist = cleanArtist(artist).ifBlank { artist.trim() }

        val primaryQuery = "$cleanedTitle $cleanedArtist".trim()
        val primary = queryAppleSearch(primaryQuery, cleanedTitle, cleanedArtist, durationMs)
        if (primary.isNotEmpty()) return primary

        if (cleanedTitle.isNotBlank() && !cleanedTitle.equals(primaryQuery, ignoreCase = true)) {
            return queryAppleSearch(cleanedTitle, cleanedTitle, cleanedArtist, durationMs)
        }
        return emptyList()
    }

    private suspend fun queryAppleSearch(
        term: String,
        wantedTitle: String,
        wantedArtist: String,
        durationMs: Long,
    ): List<Candidate> {
        if (term.isBlank()) return emptyList()
        val url = APPLE_SEARCH.toHttpUrl().newBuilder()
            .addQueryParameter("term", term)
            .addQueryParameter("types", "songs")
            .addQueryParameter("limit", "10")
            .addQueryParameter("l", "en-US")
            .build()
            .toString()

        var token = appleToken() ?: return emptyList()
        var (code, body) = executeGet(
            url = url,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Origin" to "https://music.apple.com",
                "Referer" to "https://music.apple.com/",
            ),
        )

        if (code == 401) {
            cachedAppleToken.set(null)
            token = appleToken() ?: return emptyList()
            val retry = executeGet(
                url = url,
                headers = mapOf(
                    "Authorization" to "Bearer $token",
                    "Origin" to "https://music.apple.com",
                    "Referer" to "https://music.apple.com/",
                ),
            )
            code = retry.first
            body = retry.second
        }

        if (code !in 200..299 || body.isNullOrBlank()) return emptyList()
        val root = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return emptyList()
        val candidates = buildList { root.collectCandidates(this) }

        return candidates
            .map { it to it.score(wantedTitle, wantedArtist, durationMs) }
            .filter { it.second >= MINIMUM_MATCH_SCORE }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    private suspend fun fetchLyricsForTrack(id: String): String? {
        val url = "$PUBLIC_PROXY/apple-music/lyrics".toHttpUrl().newBuilder()
            .addQueryParameter("id", id)
            .addQueryParameter("ttml", "true")
            .build()
            .toString()

        val (code, raw) = executeGet(
            url = url,
            headers = mapOf("User-Agent" to "Cgens67/$appVersion"),
        )
        if (code !in 200..299 || raw.isNullOrBlank()) return null

        val response = runCatching { json.decodeFromString<LyricsResponse>(raw) }.getOrNull()
            ?: return null

        if (!response.ttmlContent.isNullOrBlank()) {
            val converted = convertTTMLToAppFormat(response.ttmlContent)
            if (converted.isNotBlank()) return converted
        }

        if (response.content.isNotEmpty()) {
            val fromContent = convertContentToAppFormat(response)
            if (fromContent.isNotBlank()) return fromContent
        }

        if (!response.elrcMultiPerson.isNullOrBlank()) return response.elrcMultiPerson
        if (!response.elrc.isNullOrBlank()) return response.elrc
        if (!response.plain.isNullOrBlank()) return response.plain

        return null
    }

    private fun convertTTMLToAppFormat(ttml: String): String {
        return try {
            val parsedLines = TTMLParser.parseTTML(ttml)
            TTMLParser.toLRC(parsedLines).trim()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "TTML conversion failed: ${e.message}")
            ""
        }
    }

    private fun convertContentToAppFormat(response: LyricsResponse): String {
        val isSyllable = response.type.equals("Syllable", ignoreCase = true)
        if (!isSyllable) {
            return response.content.mapNotNull { line ->
                val text = joinSyllables(line.text.map { it.text to it.part })
                text.takeIf { it.isNotBlank() }
            }.joinToString("\n")
        }

        return buildString {
            response.content.forEach { line ->
                val words = line.text.filter { it.text.isNotBlank() }
                if (words.isEmpty()) return@forEach

                val lineText = joinSyllables(words.map { it.text to it.part })
                if (lineText.isBlank()) return@forEach

                val timeMs = line.timestamp
                val minutes = timeMs / 1000 / 60
                val seconds = (timeMs / 1000) % 60
                val centiseconds = (timeMs % 1000) / 10

                val agent = when {
                    line.background -> "{bg}"
                    line.oppositeTurn -> "{agent:v2}"
                    else -> "{agent:v1}"
                }

                appendLine(
                    String.format(
                        Locale.US,
                        "[%02d:%02d.%02d]%s%s",
                        minutes,
                        seconds,
                        centiseconds,
                        agent,
                        lineText,
                    ),
                )

                val wordsData = words.joinToString("|") { word ->
                    val safeWord = word.text.trim().replace("|", "")
                    val startSec = word.timestamp / 1000.0
                    val endMs = if (word.endtime > word.timestamp) word.endtime else word.timestamp + 800L
                    val endSec = endMs / 1000.0
                    "$safeWord:$startSec:$endSec"
                }
                if (wordsData.isNotBlank()) {
                    appendLine("<$wordsData>")
                }
            }
        }.trim()
    }

    private fun joinSyllables(syllables: List<Pair<String, Boolean>>): String {
        val nonBlank = syllables.filter { it.first.isNotBlank() }
        if (nonBlank.isEmpty()) return ""
        return buildString {
            nonBlank.forEachIndexed { index, (raw, isPart) ->
                append(raw.trim())
                if (!isPart && index < nonBlank.lastIndex) {
                    append(' ')
                }
            }
        }.trim()
    }

    private fun getQuality(lrc: String): Int {
        if (lrc.isBlank()) return 0
        val hasWordTimings = (lrc.contains("<") && lrc.contains(">") && (lrc.contains("|") || lrc.contains(":"))) ||
            lrc.contains(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>"))
        if (hasWordTimings) return 3

        val hasLineTimings = lrc.contains(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}]")) ||
            lrc.contains(Regex("^\\[bg:.*]", RegexOption.MULTILINE))
        return if (hasLineTimings) 2 else 1
    }

    private suspend fun appleToken(): String? = cachedAppleToken.get() ?: tokenMutex.withLock {
        cachedAppleToken.get() ?: scrapeAppleToken()?.also(cachedAppleToken::set)
    }

    private suspend fun scrapeAppleToken(): String? {
        val (_, page) = executeGet("https://music.apple.com/us/new")
        val scriptPath = page?.let { APPLE_INDEX_SCRIPT.find(it)?.value } ?: return null
        val (_, script) = executeGet("https://music.apple.com$scriptPath")
        return script?.let { APPLE_TOKEN.find(it)?.value }
    }

    private suspend fun executeGet(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): Pair<Int, String?> = suspendCancellableCoroutine { cont ->
        val builder = Request.Builder().url(url)
        if (!headers.containsKey("User-Agent")) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            )
        }
        headers.forEach { (k, v) -> builder.header(k, v) }

        val call = httpClient.newCall(builder.build())
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resume(-1 to null)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use { res ->
                        res.code to if (res.isSuccessful) res.body?.string() else null
                    }
                }.getOrElse { -1 to null }
                if (cont.isActive) cont.resume(result)
            }
        })
    }

    private fun JsonElement.collectCandidates(into: MutableList<Candidate>) {
        when (this) {
            is JsonArray -> forEach { it.collectCandidates(into) }
            is JsonObject -> {
                toCandidate()?.let(into::add)
                values.forEach { it.collectCandidates(into) }
            }
            else -> Unit
        }
    }

    private fun JsonObject.toCandidate(): Candidate? {
        val details = this["attributes"] as? JsonObject ?: this
        val id = firstString(ID_KEYS) ?: details.firstString(ID_KEYS) ?: return null
        val title = details.firstString(TITLE_KEYS) ?: return null
        val artist = details.firstString(ARTIST_KEYS) ?: details.artistNames().orEmpty()
        return Candidate(id, title, artist, details.firstLong(DURATION_KEYS).toDurationMs())
    }

    private fun JsonObject.artistNames(): String? = when (val artists = this["artists"] ?: this["artist"]) {
        is JsonPrimitive -> artists.contentOrNull
        is JsonObject -> artists.firstString(listOf("name", "artistName", "title"))
        is JsonArray -> artists.mapNotNull {
            when (it) {
                is JsonPrimitive -> it.contentOrNull
                is JsonObject -> it.firstString(listOf("name", "artistName", "title"))
                else -> null
            }
        }.joinToString(", ").takeIf(String::isNotEmpty)
        else -> null
    }

    private fun JsonObject.firstString(keys: List<String>): String? =
        keys.firstNotNullOfOrNull { (this[it] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf(String::isNotEmpty) }

    private fun JsonObject.firstLong(keys: List<String>): Long? =
        keys.firstNotNullOfOrNull { (this[it] as? JsonPrimitive)?.longOrNull }

    private fun Long?.toDurationMs(): Long = when {
        this == null || this <= 0 -> 0
        this < 10_000 -> this * 1000
        else -> this
    }

    private fun Int.toDurationMsInput(): Long = when {
        this <= 0 -> 0L
        this > 10_000 -> this.toLong()
        else -> this * 1000L
    }

    private fun Candidate.score(wantedTitle: String, wantedArtist: String, wantedDuration: Long): Int {
        var score = textScore(cleanTitle(title), wantedTitle, 20, 10) +
            textScore(artist, wantedArtist, 15, 5)
        if (wantedDuration > 0 && durationMs > 0) {
            score += when {
                abs(durationMs - wantedDuration) < 3_000 -> 10
                abs(durationMs - wantedDuration) < 10_000 -> 5
                else -> 0
            }
        }
        return score
    }

    private fun textScore(candidate: String, wanted: String, exact: Int, partial: Int): Int = when {
        candidate.isBlank() || wanted.isBlank() -> 0
        candidate.equals(wanted, ignoreCase = true) -> exact
        candidate.contains(wanted, ignoreCase = true) || wanted.contains(candidate, ignoreCase = true) -> partial
        else -> 0
    }

    private fun cleanTitle(title: String): String {
        var cleaned = title.trim()
        for (pattern in TITLE_CLEANUP_PATTERNS) {
            cleaned = cleaned.replace(pattern, "")
        }
        return cleaned.trim()
    }

    private fun cleanArtist(artist: String): String {
        var cleaned = artist.trim()
        for (separator in ARTIST_SEPARATORS) {
            if (cleaned.contains(separator, ignoreCase = true)) {
                cleaned = cleaned.split(separator, ignoreCase = true, limit = 2)[0]
                break
            }
        }
        return cleaned.trim()
    }

    private data class Candidate(val id: String, val title: String, val artist: String, val durationMs: Long)

    private val ID_KEYS = listOf("id", "trackId", "track_id", "realId")
    private val TITLE_KEYS = listOf("name", "title", "trackName", "track_name")
    private val ARTIST_KEYS = listOf("artistName", "artist_name")
    private val DURATION_KEYS = listOf("durationInMillis", "durationMs", "duration_ms", "duration")

    private val APPLE_INDEX_SCRIPT = Regex("""/assets/index~[^"]+\.js""")
    private val APPLE_TOKEN = Regex("""eyJ[A-Za-z0-9_-]+\.eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+""")

    private val TITLE_CLEANUP_PATTERNS = listOf(
        Regex("""\s*\(.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\[.*?(official|video|audio|lyrics|lyric|visualizer|hd|hq|4k|remaster|remix|live|acoustic|version|edit|extended|radio|clean|explicit).*?]""", RegexOption.IGNORE_CASE),
        Regex("""\s*【.*?】"""),
        Regex("""\s*\|.*$"""),
        Regex("""\s*-\s*(official|video|audio|lyrics|lyric|visualizer).*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(feat\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*\(ft\..*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\s*feat\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*ft\..*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*\([^)]*\d{4}[^)]*\)""", RegexOption.IGNORE_CASE),
    )

    private val ARTIST_SEPARATORS = listOf(
        " & ", " and ", ", ", " x ", " X ", " feat. ", " feat ", " ft. ", " ft ", " featuring ", " with ",
    )
}