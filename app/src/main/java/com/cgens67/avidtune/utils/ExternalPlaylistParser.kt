package com.cgens67.avidtune.utils

import com.cgens67.innertube.YouTube
import com.cgens67.innertube.utils.completed
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import timber.log.Timber
import java.net.URI
import java.net.URLDecoder

data class ParsedExternalTrack(
    val title: String,
    val artist: String,
)

data class ParsedExternalPlaylist(
    val title: String,
    val source: ExternalSource,
    val tracks: List<ParsedExternalTrack>,
)

enum class ExternalSource(val displayName: String, val iconRes: Int) {
    SPOTIFY("Spotify", com.cgens67.avidtune.R.drawable.music_note),
    APPLE_MUSIC("Apple Music", com.cgens67.avidtune.R.drawable.apple),
    YOUTUBE_MUSIC("YouTube Music", com.cgens67.avidtune.R.drawable.avidtune),
    UNKNOWN("Unknown", com.cgens67.avidtune.R.drawable.link)
}

object ExternalPlaylistParser {

    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            expectSuccess = false
        }
    }

    fun detectSource(url: String): ExternalSource {
        val lower = url.lowercase().trim()
        return when {
            lower.contains("spotify.com") || lower.contains("spoti.fi") -> ExternalSource.SPOTIFY
            lower.contains("music.apple.com") || lower.contains("apple.co") -> ExternalSource.APPLE_MUSIC
            lower.contains("youtube.com") || lower.contains("youtu.be") -> ExternalSource.YOUTUBE_MUSIC
            else -> ExternalSource.UNKNOWN
        }
    }

    suspend fun parseUrl(inputUrl: String): Result<ParsedExternalPlaylist> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmed = inputUrl.trim()
            val source = detectSource(trimmed)

            when (source) {
                ExternalSource.SPOTIFY -> parseSpotifyUrl(trimmed)
                ExternalSource.APPLE_MUSIC -> parseAppleMusicUrl(trimmed)
                ExternalSource.YOUTUBE_MUSIC -> parseYouTubeUrl(trimmed)
                ExternalSource.UNKNOWN -> throw IllegalArgumentException("Unsupported or invalid music link.")
            }
        }
    }

    // --- SPOTIFY PARSER ---

    private suspend fun parseSpotifyUrl(url: String): ParsedExternalPlaylist {
        val uri = runCatching { URI(url) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid Spotify URL")
        val path = uri.path ?: ""

        val isPlaylist = path.contains("/playlist/")
        val isAlbum = path.contains("/album/")
        val isTrack = path.contains("/track/")

        val id = path.substringAfterLast("/").substringBefore("?")
        if (id.isBlank()) throw IllegalArgumentException("Could not extract Spotify ID")

        val embedType = when {
            isPlaylist -> "playlist"
            isAlbum -> "album"
            isTrack -> "track"
            else -> "playlist"
        }

        val embedUrl = "https://open.spotify.com/embed/$embedType/$id"
        val response = client.get(embedUrl) {
            header("User-Agent", USER_AGENT)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to load Spotify page (HTTP ${response.status.value})")
        }

        val html = response.bodyAsText()
        val doc = Jsoup.parse(html)

        val pageTitle = doc.select("meta[property=og:title]").attr("content")
            .ifBlank { doc.title().removeSuffix(" | Spotify") }
            .ifBlank { "Spotify Playlist" }

        val tracks = mutableListOf<ParsedExternalTrack>()

        val scriptElement = doc.select("script#session, script#__NEXT_DATA__, script#initial-state").firstOrNull()
        if (scriptElement != null) {
            val jsonText = scriptElement.html().trim()
            runCatching {
                extractTracksFromSpotifyJson(jsonText, tracks)
            }
        }

        if (tracks.isEmpty()) {
            val items = doc.select("li, .track-item, [data-testid=track-row]")
            for (item in items) {
                val name = item.select(".track-name, .name, span[dir=auto]").firstOrNull()?.text() ?: ""
                val artist = item.select(".artist-name, .artists, span.sublist").firstOrNull()?.text() ?: ""
                if (name.isNotBlank()) {
                    tracks.add(ParsedExternalTrack(name, artist))
                }
            }
        }

        if (tracks.isEmpty()) {
            val ogDescription = doc.select("meta[property=og:description]").attr("content")
            if (ogDescription.isNotBlank()) {
                val parts = ogDescription.split(" · ")
                val trackPart = parts.getOrNull(1) ?: ogDescription
                val rawSongs = trackPart.split(", ")
                for (song in rawSongs) {
                    val songName = song.trim()
                    if (songName.isNotBlank()) {
                        tracks.add(ParsedExternalTrack(title = songName, artist = ""))
                    }
                }
            }
        }

        if (tracks.isEmpty() && isTrack) {
            val artistName = doc.select("meta[property=og:title]").attr("content")
            val subtitle = doc.select("meta[property=og:description]").attr("content")
            tracks.add(ParsedExternalTrack(title = pageTitle, artist = subtitle))
        }

        if (tracks.isEmpty()) {
            throw IllegalStateException("No tracks could be extracted from this Spotify link.")
        }

        return ParsedExternalPlaylist(
            title = pageTitle,
            source = ExternalSource.SPOTIFY,
            tracks = tracks
        )
    }

    private fun extractTracksFromSpotifyJson(jsonText: String, outTracks: MutableList<ParsedExternalTrack>) {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val entity = root["entity"]?.jsonObject ?: root

        val trackList = entity["tracks"]?.jsonObject?.get("items")?.jsonArray
            ?: entity["items"]?.jsonArray

        trackList?.forEach { item ->
            val obj = item.jsonObject
            val trackObj = obj["track"]?.jsonObject ?: obj
            val name = trackObj["name"]?.jsonPrimitive?.content ?: ""

            val artistsArray = trackObj["artists"]?.jsonArray
                ?: trackObj["artists"]?.jsonObject?.get("items")?.jsonArray

            val artistNames = artistsArray?.mapNotNull {
                it.jsonObject["name"]?.jsonPrimitive?.content
            }?.joinToString(", ") ?: ""

            if (name.isNotBlank()) {
                outTracks.add(ParsedExternalTrack(title = name, artist = artistNames))
            }
        }
    }

    // --- APPLE MUSIC PARSER ---

    private suspend fun parseAppleMusicUrl(url: String): ParsedExternalPlaylist {
        val response = client.get(url) {
            header("User-Agent", USER_AGENT)
        }

        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Failed to load Apple Music page (HTTP ${response.status.value})")
        }

        val html = response.bodyAsText()
        val doc = Jsoup.parse(html)

        val pageTitle = doc.select("meta[property=og:title]").attr("content")
            .ifBlank { doc.title().substringBefore(" on Apple Music") }
            .ifBlank { "Apple Music Playlist" }

        val tracks = mutableListOf<ParsedExternalTrack>()

        val jsonLdScripts = doc.select("script[type=application/ld+json]")
        for (script in jsonLdScripts) {
            runCatching {
                val jsonElement = json.parseToJsonElement(script.html())
                val obj = jsonElement.jsonObject
                val type = obj["@type"]?.jsonPrimitive?.content

                if (type == "MusicPlaylist" || type == "MusicAlbum") {
                    val trackArray = obj["track"]?.jsonArray ?: obj["tracks"]?.jsonArray
                    trackArray?.forEach { t ->
                        val tObj = t.jsonObject
                        val title = tObj["name"]?.jsonPrimitive?.content ?: ""
                        val artistObj = tObj["byArtist"]?.jsonObject
                        val artistName = artistObj?.get("name")?.jsonPrimitive?.content ?: ""

                        if (title.isNotBlank()) {
                            tracks.add(ParsedExternalTrack(title = title, artist = artistName))
                        }
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            val songRows = doc.select(".songs-list-row, [data-testid=track-item]")
            for (row in songRows) {
                val title = row.select(".songs-list-row__song-name, [data-testid=track-title]").text()
                val artist = row.select(".songs-list-row__by-line, [data-testid=track-artist]").text()
                if (title.isNotBlank()) {
                    tracks.add(ParsedExternalTrack(title = title, artist = artist))
                }
            }
        }

        if (tracks.isEmpty()) {
            throw IllegalStateException("No tracks could be extracted from this Apple Music link.")
        }

        return ParsedExternalPlaylist(
            title = pageTitle,
            source = ExternalSource.APPLE_MUSIC,
            tracks = tracks
        )
    }

    // --- YOUTUBE MUSIC PARSER ---

    private suspend fun parseYouTubeUrl(url: String): ParsedExternalPlaylist {
        val uri = runCatching { URI(url) }.getOrNull()
            ?: throw IllegalArgumentException("Invalid YouTube URL")

        val queryParams = uri.rawQuery?.split("&")?.associate {
            val parts = it.split("=")
            parts[0] to (parts.getOrNull(1)?.let { v -> URLDecoder.decode(v, "UTF-8") } ?: "")
        } ?: emptyMap()

        val listId = queryParams["list"]
            ?: if (url.contains("playlist?list=")) url.substringAfter("list=").substringBefore("&") else null

        if (listId.isNullOrBlank()) {
            val videoId = queryParams["v"] ?: uri.path?.substringAfterLast("/")?.takeIf { it.length == 11 }
            if (!videoId.isNullOrBlank()) {
                val nextResult = YouTube.next(com.cgens67.innertube.models.WatchEndpoint(videoId = videoId)).getOrNull()
                val songTitle = nextResult?.items?.firstOrNull()?.title ?: "YouTube Track"
                val artistName = nextResult?.items?.firstOrNull()?.artists?.joinToString(", ") { it.name } ?: ""
                return ParsedExternalPlaylist(
                    title = songTitle,
                    source = ExternalSource.YOUTUBE_MUSIC,
                    tracks = listOf(ParsedExternalTrack(title = songTitle, artist = artistName))
                )
            }
            throw IllegalArgumentException("No playlist ID ('list=...') found in YouTube link.")
        }

        val playlistPage = YouTube.playlist(listId).completed().getOrElse {
            throw IllegalStateException("Failed to load YouTube Music playlist: ${it.message}")
        }

        val tracks = playlistPage.songs.map { song ->
            ParsedExternalTrack(
                title = song.title,
                artist = song.artists.joinToString(", ") { it.name }
            )
        }

        return ParsedExternalPlaylist(
            title = playlistPage.playlist.title,
            source = ExternalSource.YOUTUBE_MUSIC,
            tracks = tracks
        )
    }
}
