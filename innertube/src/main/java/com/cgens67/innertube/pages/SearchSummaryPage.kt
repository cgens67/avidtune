package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ALBUM
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_USER_CHANNEL
import com.cgens67.innertube.models.MusicCardShelfRenderer
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.models.clean
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.filterMusicVideos
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class SearchSummary(
    val title: String,
    val items: List<YTItem>,
)

data class SearchSummaryPage(
    val summaries: List<SearchSummary>,
) {
    fun filterExplicit(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    SearchSummary(
                        title = s.title,
                        items = s.items.filterExplicit().ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else {
            this
        }

    fun filterMusicVideos(enabled: Boolean) =
        if (enabled) {
            SearchSummaryPage(
                summaries.mapNotNull { s ->
                    val titleLower = s.title.lowercase()
                    if (titleLower.contains("video") || titleLower.contains("vídeo") || titleLower.contains("vidéo") || titleLower.contains("видео") || titleLower.contains("βίντεο")) {
                        return@mapNotNull null
                    }
                    SearchSummary(
                        title = s.title,
                        items = s.items.filterMusicVideos().ifEmpty { return@mapNotNull null },
                    )
                },
            )
        } else {
            this
        }

    companion object {
        fun fromMusicCardShelfRenderer(renderer: MusicCardShelfRenderer): YTItem? {
            val subtitle = renderer.subtitle.runs?.splitBySeparator()
            val firstRunText = subtitle?.firstOrNull()?.firstOrNull()?.text
            val firstRunTextLower = firstRunText?.lowercase()
            val typePrefixes = listOf("episode", "episodio", "video", "vídeo", "vidéo", "видео", "βίντεο", "song", "canción", "cancion", "chanson", "lied", "canção", "canzone", "şarkı", "песня", "piosenka", "歌曲", "曲", "노래", "שיר", "أغنية", "mahnı")
            val isTypePrefix = firstRunTextLower in typePrefixes || ((subtitle?.size ?: 0) >= 3 && subtitle?.firstOrNull()?.firstOrNull()?.navigationEndpoint == null)
            val fallbackIndex = if (isTypePrefix) {
                val startIndex = 1
                val endpointIndex = subtitle?.drop(startIndex)?.indexOfFirst { chunk ->
                    chunk.any { it.navigationEndpoint?.browseEndpoint != null }
                } ?: -1
                if (endpointIndex != -1) startIndex + endpointIndex else startIndex
            } else 0

            return when {
                renderer.onTap.watchEndpoint != null -> {
                    val videoType = renderer.onTap.watchEndpoint.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    val isVideo = videoType == "MUSIC_VIDEO_TYPE_OMV" || videoType == "MUSIC_VIDEO_TYPE_UGC" || 
                        (firstRunTextLower?.let { it.contains("video") || it.contains("vídeo") || it.contains("vidéo") || it.contains("видео") || it.contains("βίντεο") } == true)

                    SongItem(
                        id = renderer.onTap.watchEndpoint.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(fallbackIndex)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        album = subtitle?.getOrNull(fallbackIndex + 1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId!!)
                        },
                        duration = subtitle?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                        isVideo = isVideo,
                    )
                }
                renderer.onTap.browseEndpoint?.isArtistEndpoint == true -> {
                    ArtistItem(
                        id = renderer.onTap.browseEndpoint.browseId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MIX" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                    )
                }
                renderer.onTap.browseEndpoint?.isAlbumEndpoint == true -> {
                    AlbumItem(
                        browseId = renderer.onTap.browseEndpoint.browseId,
                        playlistId = renderer.buttons.firstOrNull()?.buttonRenderer?.command?.anyWatchEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = subtitle?.getOrNull(1)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        year = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                    )
                }
                renderer.onTap.browseEndpoint?.isPlaylistEndpoint == true -> {
                    PlaylistItem(
                        id = renderer.onTap.browseEndpoint.browseId.removePrefix("VL"),
                        title = renderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.joinToString(separator = "") { it.text } ?: return null,
                        author = Artist(id = null, name = renderer.subtitle.runs?.joinToString { it.text } ?: return null),
                        songCountText = null,
                        thumbnail = renderer.thumbnail.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "PLAY_ARROW" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.buttons.find { it.buttonRenderer.icon?.iconType == "MUSIC_SHUFFLE" }?.buttonRenderer?.command?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = null,
                    )
                }
                else -> null
            }
        }

        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): YTItem? {
            val secondaryLine = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator() ?: return null
            val thirdLine = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator() ?: emptyList()
            val listRun = (secondaryLine + thirdLine).clean()
            var album: Album? = null
            val artist: MutableList<Artist> = mutableListOf()
            listRun.forEach { runs ->
                runs.forEach {
                    val pageType = it.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    if (pageType == MUSIC_PAGE_TYPE_ALBUM) {
                        album = Album(name = it.text, id = it.navigationEndpoint.browseEndpoint.browseId)
                    } else if (pageType == MUSIC_PAGE_TYPE_ARTIST || pageType == MUSIC_PAGE_TYPE_USER_CHANNEL || pageType == "MUSIC_PAGE_TYPE_PODCAST_SHOW") {
                        artist.add(Artist(name = it.text, id = it.navigationEndpoint.browseEndpoint.browseId))
                    }
                }
            }
            return when {
                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu.menuRenderer.items.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    )
                }
                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint?.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null,
                        artists = secondaryLine.getOrNull(1)?.oddElements()?.map {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        year = secondaryLine.getOrNull(2)?.firstOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.badges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                    )
                }
                renderer.isPlaylist -> {
                    PlaylistItem(
                        id = renderer.navigationEndpoint?.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                        title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null,
                        author = secondaryLine.getOrNull(1)?.firstOrNull()?.let {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        } ?: return null,
                        songCountText = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.lastOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        playEndpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu.menuRenderer.items.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    )
                }
                renderer.isSong -> {
                    val firstRunText = secondaryLine.firstOrNull()?.firstOrNull()?.text
                    val firstRunTextLower = firstRunText?.lowercase()
                    val typePrefixes = listOf("episode", "episodio", "video", "vídeo", "vidéo", "видео", "βίντεο", "song", "canción", "cancion", "chanson", "lied", "canção", "canzone", "şarkı", "песня", "piosenka", "歌曲", "曲", "노래", "שיר", "أغنية", "mahnı")
                    val isVideoOrEpisode = firstRunTextLower in typePrefixes || ((secondaryLine.size) >= 3 && secondaryLine.firstOrNull()?.firstOrNull()?.navigationEndpoint == null)
                    val fallbackIndex = if (isVideoOrEpisode) {
                        val startIndex = 1
                        val endpointIndex = secondaryLine.drop(startIndex).indexOfFirst { chunk ->
                            chunk.any { it.navigationEndpoint?.browseEndpoint != null }
                        }
                        if (endpointIndex != -1) startIndex + endpointIndex else startIndex
                    } else 0

                    val videoType = renderer.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                        ?: renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    
                    val isVideo = videoType == "MUSIC_VIDEO_TYPE_OMV" || videoType == "MUSIC_VIDEO_TYPE_UGC" || 
                        (firstRunTextLower?.let { it.contains("video") || it.contains("vídeo") || it.contains("vidéo") || it.contains("видео") || it.contains("βίντεο") } == true)

                    SongItem(
                        id = renderer.videoId ?: return null,
                        title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return null,
                        artists = if (artist.isEmpty()) {
                            secondaryLine.getOrNull(fallbackIndex)?.oddElements()?.map {
                                Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                            } ?: return null
                        } else {
                            artist
                        },
                        album = album ?: secondaryLine.getOrNull(fallbackIndex + 1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                            Album(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId!!)
                        },
                        duration = secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit = renderer.badges?.find { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } != null,
                        isVideo = isVideo,
                    )
                }
                else -> null
            }
        }
    }
}
