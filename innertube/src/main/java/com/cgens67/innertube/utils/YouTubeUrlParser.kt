package com.cgens67.innertube.utils

import com.cgens67.innertube.models.WatchEndpoint

object YouTubeUrlParser {
    sealed class ParsedUrl {
        abstract val id: String

        data class Video(
            override val id: String,
        ) : ParsedUrl()

        data class Playlist(
            override val id: String,
        ) : ParsedUrl()

        data class Album(
            override val id: String,
        ) : ParsedUrl()

        data class Artist(
            override val id: String,
        ) : ParsedUrl()
    }

    private val VIDEO_URL_PATTERNS =
        listOf(
            Regex("""(?:https?://)?(?:www\.)?(?:music\.)?youtube\.com/watch\?.*v=([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.)?(?:music\.)?youtube\.com/watch\?v=([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?youtu\.be/([a-zA-Z0-9_-]{11})"""),
            Regex("""(?:https?://)?(?:www\.)?youtube\.com/shorts/([a-zA-Z0-9_-]{11})"""),
        )

    private val PLAYLIST_URL_PATTERN =
        Regex(
            """(?:https?://)?(?:www\.)?(?:music\.)?youtube\.com/playlist\?.*list=([a-zA-Z0-9_-]+)""",
        )

    private val ALBUM_URL_PATTERN =
        Regex(
            """(?:https?://)?(?:www\.)?music\.youtube\.com/playlist\?.*list=([a-zA-Z0-9_-]+)""",
        )

    private val ARTIST_URL_PATTERNS =
        listOf(
            Regex("""(?:https?://)?(?:www\.)?music\.youtube\.com/channel/([a-zA-Z0-9_-]+)"""),
            Regex("""(?:https?://)?(?:www\.)?music\.youtube\.com/browse/(MPRE[a-zA-Z0-9_-]+)"""),
        )

    fun isYouTubeUrl(text: String): Boolean = parse(text) != null

    fun parse(url: String): ParsedUrl? {
        val trimmedUrl = url.trim()

        for (pattern in VIDEO_URL_PATTERNS) {
            pattern.find(trimmedUrl)?.let { matchResult ->
                matchResult.groupValues.getOrNull(1)?.let { videoId ->
                    return ParsedUrl.Video(videoId)
                }
            }
        }

        if (!trimmedUrl.contains("music.youtube.com")) {
            PLAYLIST_URL_PATTERN.find(trimmedUrl)?.let { matchResult ->
                matchResult.groupValues.getOrNull(1)?.let { playlistId ->
                    return ParsedUrl.Playlist(playlistId)
                }
            }
        }

        if (trimmedUrl.contains("music.youtube.com")) {
            ALBUM_URL_PATTERN.find(trimmedUrl)?.let { matchResult ->
                matchResult.groupValues.getOrNull(1)?.let { playlistId ->
                    return ParsedUrl.Album(playlistId)
                }
            }

            for (pattern in ARTIST_URL_PATTERNS) {
                pattern.find(trimmedUrl)?.let { matchResult ->
                    matchResult.groupValues.getOrNull(1)?.let { artistId ->
                        return ParsedUrl.Artist(artistId)
                    }
                }
            }
        }

        return null
    }

    fun extractVideoId(url: String): String? = (parse(url) as? ParsedUrl.Video)?.id

    fun extractPlaylistId(url: String): String? {
        val parsed = parse(url)
        return when (parsed) {
            is ParsedUrl.Playlist -> parsed.id
            is ParsedUrl.Album -> parsed.id
            else -> null
        }
    }

    fun createWatchEndpoint(url: String): WatchEndpoint? =
        extractVideoId(url)?.let { videoId ->
            WatchEndpoint(videoId = videoId)
        }
}
