package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.MusicResponsiveHeaderRenderer
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.getItems
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.response.BrowseResponse
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
) {
    companion object {
        fun getPlaylistId(response: BrowseResponse): String? {
            var playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')
            if (playlistId == null)
            {
                playlistId = response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()
                    ?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId
            }
            return playlistId
        }

        fun getTitle(response: BrowseResponse): String? {
            val title = getHeader(response)?.title ?: response.header?.musicDetailHeaderRenderer?.title
            return title?.runs?.firstOrNull()?.text
        }

        fun getYear(response: BrowseResponse): Int? {
            val title = getHeader(response)?.subtitle ?: response.header?.musicDetailHeaderRenderer?.subtitle
            return title?.runs?.lastOrNull()?.text?.toIntOrNull()
        }

        fun getThumbnail(response: BrowseResponse): String? {
            return response.background?.getThumbnailUrl() ?: response.header?.musicDetailHeaderRenderer?.thumbnail
                ?.getThumbnailUrl()
        }

        fun getArtists(response: BrowseResponse): List<Artist> {
            val artists = getHeader(response)?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: emptyList()

            return artists
        }

        private fun getHeader(response: BrowseResponse): MusicResponsiveHeaderRenderer? {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val section =
                tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            val header = section?.musicResponsiveHeaderRenderer
            return header
        }

        fun getSongs(response: BrowseResponse, album: AlbumItem): List<SongItem> {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val shelfRenderer = tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer ?:
                response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer

            val songs = shelfRenderer?.contents?.getItems()?.mapNotNull {
                getSong(it, album)
            }
            return songs ?: emptyList()
        }

        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)
            val views = PageHelper.extractViews(renderer.flexColumns.flatMap { it.musicResponsiveListItemFlexColumnRenderer.text?.runs.orEmpty() })

            return SongItem(
                id = renderer.playlistItemData?.videoId
                    ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.overlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer
                        ?.playNavigationEndpoint?.watchEndpoint?.videoId
                    ?: renderer.flexColumns.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text?.runs?.firstOrNull()
                        ?.navigationEndpoint?.watchEndpoint?.videoId
                    ?: return null,
                title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text ?: return null,
                artists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map{
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }.ifEmpty {
                    renderer.flexColumns.getOrNull(1)
                        ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                        ?.splitBySeparator()?.firstOrNull()?.oddElements()
                        ?.map { Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId) }
                        ?.filter { it.name.isNotBlank() }
                        ?.takeIf { it.isNotEmpty() }
                        ?: album?.artists ?: emptyList()
                },
                album = album?.let {
                    Album(it.title, it.browseId)
                } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                    )
                }!!,
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text?.parseTime() ?: return null,
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: album?.thumbnail!!,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken,
                views = views
            )
        }
    }
}
