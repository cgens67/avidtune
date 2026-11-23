package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableMusixmatchKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.math.abs

object MusixmatchLyricsProvider : LyricsProvider {
    override val name = "Musixmatch"

    private const val BASE = "https://apic.musixmatch.com/ws/1.1"
    private const val APP_ID = "mobile-app-v1.0"
    private const val SEARCH_PAGE = "https://www.musixmatch.com/search"
    private const val BROWSER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    // Used only if the public page serving the rotating key is briefly unavailable.
    private const val FALLBACK_SIGNING_SECRET = "f09016176ba43a1cfd1031fbd6b3d26c"

    private val tokenMutex = Mutex()
    private val cachedSecret = AtomicReference<String?>(null)
    private val cachedToken = AtomicReference<String?>(null)
    private val processGuid = UUID.randomUUID().toString()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .callTimeout(8, TimeUnit.SECONDS)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableMusixmatchKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val seconds = if (duration > 10000) duration / 1000 else duration
            val tracks = searchTrack(title, artist)
                ?.sortedByDescending { score(it, title, artist, seconds) }
                ?: throw IllegalStateException("Musixmatch search failed")

            for (track in tracks) {
                val lyrics = fetchTrackLyrics(track)
                if (!lyrics.isNullOrBlank()) {
                    return@withContext lyrics
                }
            }

            throw IllegalStateException("No synced lyrics found on Musixmatch")
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        try {
            withContext(Dispatchers.IO) {
                val seconds = if (duration > 10000) duration / 1000 else duration
                val tracks = searchTrack(title, artist)
                    ?.sortedByDescending { score(it, title, artist, seconds) }
                    ?: return@withContext

                val seen = HashSet<String>()
                var count = 0
                for (track in tracks) {
                    if (count >= 4) break
                    val lyrics = fetchTrackLyrics(track)
                    if (!lyrics.isNullOrBlank() && seen.add(lyrics)) {
                        callback(lyrics)
                        count++
                    }
                }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Error in Musixmatch getAllLyrics")
        }
    }

    private suspend fun fetchTrackLyrics(track: Track): String? {
        if (track.hasRichSync != 0) {
            val rich = fetchRichSync(track.trackId)?.let(::parseRichSyncBodyToLrc)
            if (!rich.isNullOrBlank()) return rich
        }

        if (track.hasSubtitles != 0) {
            val subtitle = fetchSubtitle(track.trackId)
            val lrc = subtitle?.let(::subtitleToLrc)?.takeIf { it.isNotBlank() }
            if (lrc != null) return lrc
        }

        return null
    }

    private fun score(track: Track, title: String, artist: String, seconds: Int): Double {
        var score = 0.0
        val name = track.trackName.trim().lowercase(Locale.ROOT)
        val targetTitle = title.trim().lowercase(Locale.ROOT)
        score += when {
            name == targetTitle -> 80.0
            name.contains(targetTitle) || targetTitle.contains(name) -> 40.0
            else -> 0.0
        }
        val targetArtist = artist.trim().lowercase(Locale.ROOT)
        val trackArtist = track.artistName.trim().lowercase(Locale.ROOT)
        if (targetArtist.isNotEmpty() &&
            (trackArtist.contains(targetArtist) || targetArtist.contains(trackArtist))
        ) {
            score += 40.0
        }
        if (seconds > 0) {
            track.trackLength?.let { length ->
                val diff = abs(length - seconds)
                score += when {
                    diff <= 2 -> 30.0
                    diff <= 5 -> 15.0
                    diff <= 10 -> 5.0
                    else -> -20.0
                }
            }
        }
        return score
    }

    private suspend fun searchTrack(title: String, artist: String): List<Track>? {
        val response = signedGet { token ->
            "$BASE/track.search".toHttpUrl().newBuilder()
                .addQueryParameter("app_id", APP_ID)
                .addQueryParameter("format", "json")
                .addQueryParameter("q_track", title)
                .addQueryParameter("q_artist", artist)
                .addQueryParameter("f_has_lyrics", "1")
                .addQueryParameter("s_track_rating", "desc")
                .addQueryParameter("quorum_factor", "1")
                .addQueryParameter("page_size", "10")
                .addQueryParameter("page", "1")
                .addQueryParameter("usertoken", token)
                .build()
        } ?: return null
        val body = runCatching {
            json.decodeFromString<Envelope<TrackSearchBody>>(response)
        }.getOrNull() ?: return null
        return body.message.body?.trackList?.map { it.track }
    }

    private suspend fun fetchSubtitle(trackId: Long): String? {
        val response = signedGet { token ->
            "$BASE/track.subtitle.get".toHttpUrl().newBuilder()
                .addQueryParameter("app_id", APP_ID)
                .addQueryParameter("format", "json")
                .addQueryParameter("track_id", trackId.toString())
                .addQueryParameter("subtitle_format", "mxm")
                .addQueryParameter("usertoken", token)
                .build()
        } ?: return null
        return runCatching {
            json.decodeFromString<Envelope<SubtitleBody>>(response)
        }.getOrNull()?.message?.body?.subtitle?.subtitleBody
    }

    private suspend fun fetchRichSync(trackId: Long): String? {
        val response = signedGet { token ->
            "$BASE/track.richsync.get".toHttpUrl().newBuilder()
                .addQueryParameter("app_id", APP_ID)
                .addQueryParameter("format", "json")
                .addQueryParameter("track_id", trackId.toString())
                .addQueryParameter("usertoken", token)
                .build()
        } ?: return null
        return runCatching {
            json.decodeFromString<Envelope<RichSyncBody>>(response)
        }.getOrNull()?.message?.body?.richsync?.richsyncBody
    }

    /**
     * Converts Musixmatch rich-sync JSON into AvidTune's extended word-sync LRC format:
     * `[mm:ss.cc]line text` followed by `<word:startSec:endSec|...>`.
     */
    private fun parseRichSyncBodyToLrc(body: String): String? {
        val entries = runCatching {
            json.decodeFromString<List<RichSyncEntry>>(body)
        }.getOrNull() ?: return null

        val sb = StringBuilder()
        for (entry in entries.sortedBy { it.startSeconds }) {
            val lineStartMs = secondsToMs(entry.startSeconds)
            val lineEndMs = maxOf(lineStartMs, secondsToMs(entry.endSeconds))
            var previousStartMs = lineStartMs

            val wordRuns = mutableListOf<Triple<String, Double, Double>>()
            for (index in entry.fragments.indices) {
                val fragment = entry.fragments[index]
                val cleanFragment = fragment.text.trim()
                val startMs = maxOf(
                    lineStartMs,
                    previousStartMs,
                    secondsToMs(entry.startSeconds + fragment.offsetSeconds),
                )
                val nextMs = entry.fragments.getOrNull(index + 1)?.let {
                    maxOf(startMs, secondsToMs(entry.startSeconds + it.offsetSeconds))
                } ?: lineEndMs
                val endMs = maxOf(startMs, minOf(lineEndMs, nextMs))
                previousStartMs = startMs

                if (cleanFragment.isNotEmpty()) {
                    val safeWord = cleanFragment.replace("|", "")
                    if (safeWord.isNotEmpty()) {
                        wordRuns.add(Triple(safeWord, startMs / 1000.0, endMs / 1000.0))
                    }
                }
            }

            val text = entry.text.trim().ifEmpty {
                entry.fragments.joinToString("") { it.text }.trim()
            }
            if (text.isEmpty()) continue

            val effectiveStartMs = minOf(
                lineStartMs,
                wordRuns.firstOrNull()?.let { (it.second * 1000.0).toLong() } ?: lineStartMs,
            )
            sb.append(formatLrcTime(effectiveStartMs)).append(text).append('\n')
            if (wordRuns.isNotEmpty()) {
                sb.append('<')
                wordRuns.forEachIndexed { idx, (word, startSec, endSec) ->
                    sb.append(word).append(':').append(startSec).append(':').append(endSec)
                    if (idx < wordRuns.lastIndex) sb.append('|')
                }
                sb.append(">\n")
            }
        }
        return sb.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun subtitleToLrc(subtitleBody: String): String {
        val lines = runCatching { json.decodeFromString<List<SubtitleLine>>(subtitleBody) }
            .getOrNull() ?: return ""
        return buildString {
            for (line in lines) {
                if (line.text.isBlank()) continue
                val totalMs = secondsToMs(line.time.total)
                val minutes = totalMs / 1000 / 60
                val seconds = (totalMs / 1000) % 60
                val millis = totalMs % 1000
                appendLine(
                    "[" + "%02d:%02d.%03d".format(Locale.US, minutes, seconds, millis) + "]" + line.text,
                )
            }
        }.trim()
    }

    private fun formatLrcTime(timeMs: Long): String {
        val m = timeMs / 60000
        val s = (timeMs % 60000) / 1000
        val c = (timeMs % 1000) / 10
        return "[%02d:%02d.%02d]".format(Locale.US, m, s, c)
    }

    private fun secondsToMs(seconds: Double): Long = (seconds * 1_000.0).toLong()

    private suspend fun signedGet(buildUrl: (token: String) -> HttpUrl): String? {
        val secret = getSecret()
        val token = getToken(secret) ?: return null
        val first = apiGet(sign(buildUrl(token).toString(), secret))
        if (first != null && !looksUnauthorized(first)) return first

        cachedToken.set(null)
        cachedSecret.set(null)
        val freshSecret = getSecret()
        val freshToken = getToken(freshSecret) ?: return null
        return apiGet(sign(buildUrl(freshToken).toString(), freshSecret))
    }

    private fun looksUnauthorized(body: String): Boolean =
        runCatching { json.decodeFromString<Envelope<JsonElement>>(body) }
            .getOrNull()?.message?.header?.statusCode?.let { it == 401 || it == 402 } ?: false

    private suspend fun getToken(secret: String): String? = cachedToken.get() ?: tokenMutex.withLock {
        cachedToken.get() ?: fetchToken(secret)?.also(cachedToken::set)
    }

    private suspend fun fetchToken(secret: String): String? {
        val url = "$BASE/token.get".toHttpUrl().newBuilder()
            .addQueryParameter("app_id", APP_ID)
            .addQueryParameter("guid", processGuid)
            .addQueryParameter("format", "json")
            .build()
        val body = apiGet(sign(url.toString(), secret)) ?: return null
        return runCatching {
            json.decodeFromString<Envelope<TokenBody>>(body)
        }.getOrNull()?.message?.body?.userToken
    }

    private suspend fun getSecret(): String = cachedSecret.get() ?: runCatching {
        val page = browserGet(SEARCH_PAGE, "text/html,application/xhtml+xml")
            ?: error("search page unavailable")
        val script = APP_SCRIPT.find(page)?.groupValues?.get(1)
            ?: error("application script not found")
        val scriptUrl = SEARCH_PAGE.toHttpUrl().resolve(script)?.toString()
            ?: error("invalid application script URL")
        val javascript = browserGet(scriptUrl, "*/*") ?: error("application script unavailable")
        val encoded = ENCODED_SECRET.find(javascript)?.groupValues?.get(1)
            ?: error("signing key not found")
        String(Base64.getDecoder().decode(encoded.reversed()), Charsets.UTF_8)
            .takeIf { it.isNotBlank() }
            ?: error("empty signing key")
    }.getOrElse { FALLBACK_SIGNING_SECRET }.also(cachedSecret::set)

    private fun sign(url: String, secret: String): String {
        val normalized = url.replace("%20", "+").replace(" ", "+")
        val date = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val raw = mac.doFinal("$normalized$date".toByteArray(Charsets.UTF_8))
        val signature = Base64.getEncoder().encodeToString(raw)
        return "$normalized&signature=${URLEncoder.encode(signature, "UTF-8")}" +
            "&signature_protocol=sha256"
    }

    private suspend fun apiGet(url: String): String? =
        request(url, "application/json, text/plain, */*")

    private suspend fun browserGet(url: String, accept: String): String? =
        request(url, accept, cookie = "mxm_bab=AB")

    private suspend fun request(url: String, accept: String, cookie: String? = null): String? =
        suspendCancellableCoroutine { cont ->
            val request = Request.Builder().url(url)
                .header("User-Agent", BROWSER_AGENT)
                .header("Accept", accept)
                .header("Accept-Language", "en-US,en;q=0.9")
                .apply { if (cookie != null) header("Cookie", cookie) }
                .build()
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = runCatching {
                        response.use { if (it.isSuccessful) it.body?.string() else null }
                    }.getOrNull()
                    if (cont.isActive) cont.resume(result)
                }
            })
        }

    @Serializable
    private data class Envelope<T>(val message: Message<T>)

    @Serializable
    private data class Message<T>(val header: Header, val body: T? = null)

    @Serializable
    private data class Header(@SerialName("status_code") val statusCode: Int = 0)

    @Serializable
    private data class TokenBody(@SerialName("user_token") val userToken: String)

    @Serializable
    private data class TrackSearchBody(
        @SerialName("track_list") val trackList: List<TrackWrapper> = emptyList(),
    )

    @Serializable
    private data class TrackWrapper(val track: Track)

    @Serializable
    private data class Track(
        @SerialName("track_id") val trackId: Long,
        @SerialName("track_name") val trackName: String,
        @SerialName("artist_name") val artistName: String = "",
        @SerialName("track_length") val trackLength: Int? = null,
        @SerialName("has_subtitles") val hasSubtitles: Int? = null,
        @SerialName("has_richsync") val hasRichSync: Int? = null,
    )

    @Serializable
    private data class SubtitleBody(val subtitle: Subtitle? = null)

    @Serializable
    private data class Subtitle(@SerialName("subtitle_body") val subtitleBody: String)

    @Serializable
    private data class SubtitleLine(val text: String, val time: SubtitleTime)

    @Serializable
    private data class SubtitleTime(val total: Double)

    @Serializable
    private data class RichSyncBody(val richsync: RichSync? = null)

    @Serializable
    private data class RichSync(@SerialName("richsync_body") val richsyncBody: String? = null)

    @Serializable
    private data class RichSyncEntry(
        @SerialName("ts") val startSeconds: Double,
        @SerialName("te") val endSeconds: Double,
        @SerialName("l") val fragments: List<RichSyncFragment> = emptyList(),
        @SerialName("x") val text: String = "",
    )

    @Serializable
    private data class RichSyncFragment(
        @SerialName("c") val text: String,
        @SerialName("o") val offsetSeconds: Double,
    )

    private val APP_SCRIPT = Regex(
        """src=["']([^"']*/_next/static/chunks/pages/_app-[^"']+\.js)["']""",
        RegexOption.IGNORE_CASE,
    )
    private val ENCODED_SECRET = Regex("""from\(\s*["']([^"']+)["']\s*\.split""")
}