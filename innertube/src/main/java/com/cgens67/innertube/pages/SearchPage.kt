package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class SearchResult(
    val items: List<YTItem>,
    val continuation: String? = null,
)

object SearchPage {
    fun toYTItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        val secondaryLine =
            renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs
                ?.splitBySeparator()
                ?: return null
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
                    playlistId = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.anyWatchEndpoint?.playlistId ?: return null,
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
                    author = secondaryLine.firstOrNull()?.firstOrNull()?.let {
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
                val isVideoOrEpisode = firstRunTextLower in typePrefixes || (secondaryLine.size >= 3 && secondaryLine.firstOrNull()?.firstOrNull()?.navigationEndpoint == null)
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
                    artists = secondaryLine.getOrNull(fallbackIndex)?.oddElements()?.map {
                        Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                    } ?: return null,
                    album = secondaryLine.getOrNull(fallbackIndex + 1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
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
