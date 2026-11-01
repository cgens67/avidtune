package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val subtitle = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
            val artists = PageHelper.extractArtists(subtitle).ifEmpty {
                if (album?.playlistId?.startsWith("OLAK") == true ||
                    album?.browseId?.startsWith("FEmusic_library_privately_owned_release_detail") == true
                ) {
                    subtitle?.splitBySeparator()?.firstOrNull()?.oddElements()
                        ?.mapNotNull { run ->
                            run.text.trim().takeIf { it.isNotEmpty() && run.navigationEndpoint == null }
                                ?.let { Artist(it, null) }
                        }.orEmpty()
                } else emptyList()
            }.ifEmpty { album?.artists.orEmpty() }

            return SongItem(
                id =
                    renderer.playlistItemData?.videoId
                        ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                        ?: renderer.overlay
                            ?.musicItemThumbnailOverlayRenderer
                            ?.content
                            ?.musicPlayButtonRenderer
                            ?.playNavigationEndpoint
                            ?.watchEndpoint
                            ?.videoId
                        ?: renderer.flexColumns
                            .firstOrNull()
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text
                            ?.runs
                            ?.firstOrNull()
                            ?.navigationEndpoint
                            ?.watchEndpoint
                            ?.videoId
                        ?: return null,
                title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text ?: return null,
                artists = artists,
                album =
                    album?.let {
                        Album(it.title, it.browseId)
                    } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                        )
                    }!!,
                duration =
                    renderer.fixedColumns
                        ?.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?.parseTime() ?: return null,
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: album?.thumbnail!!,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}