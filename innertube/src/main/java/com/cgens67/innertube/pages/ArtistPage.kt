package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.MusicCarouselShelfRenderer
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.MusicShelfRenderer
import com.cgens67.innertube.models.MusicTwoRowItemRenderer
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.Run
import com.cgens67.innertube.models.SectionListRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.utils.parseTime
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.getItems
import com.cgens67.innertube.models.splitArtistsByConjunction
import com.cgens67.innertube.models.splitBySeparator

data class ArtistSection(
    val title: String,
    val items: List<YTItem>,
    val moreEndpoint: BrowseEndpoint?,
)

data class ArtistPage(
    val artist: ArtistItem,
    val sections: List<ArtistSection>,
    val description: String?,
    val subscriberCountText: String?,
    val monthlyListenerCount: String? = null,
    val descriptionRuns: List<Run>? = null,
    val isSubscribed: Boolean = false,
) {
    companion object {
        fun fromSectionListRendererContent(content: SectionListRenderer.Content): ArtistSection? {
            return when {
                content.musicShelfRenderer != null -> fromMusicShelfRenderer(content.musicShelfRenderer)
                content.musicCarouselShelfRenderer != null -> fromMusicCarouselShelfRenderer(content.musicCarouselShelfRenderer)
                else -> null
            }
        }

        private fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): ArtistSection? {
            return ArtistSection(
                title = renderer.title?.runs?.firstOrNull()?.text ?: "",
                items = renderer.contents?.getItems()?.mapNotNull {
                    fromMusicResponsiveListItemRenderer(it)
                }?.ifEmpty { null } ?: return null,
                moreEndpoint = renderer.title?.runs?.firstOrNull()?.navigationEndpoint?.browseEndpoint
            )
        }

        private fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): ArtistSection? {
            return ArtistSection(
                title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return null,
                items = renderer.contents.mapNotNull { content ->
                    content.musicTwoRowItemRenderer?.let { twoRowRenderer ->
                        fromMusicTwoRowItemRenderer(twoRowRenderer)
                    } ?: content.musicResponsiveListItemRenderer?.let { listItemRenderer ->
                        fromMusicResponsiveListItemRenderer(listItemRenderer)
                    }
                }.ifEmpty { null } ?: return null,
                moreEndpoint = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint
            )
        }

        private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val subtitleLine = renderer.flexColumns
                .getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer
                ?.text
                ?.runs

            val subtitleGroups = subtitleLine?.splitBySeparator()

            val artistRuns = subtitleGroups
                ?.getOrNull(0)
                ?.splitArtistsByConjunction()
                ?.filter { it.text.isNotBlank() && it.text != "&" && it.text != "," }
                ?.map { run ->
                    Artist(
                        name = run.text.trim(),
                        id = run.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }

            val album = renderer.flexColumns.lastOrNull()
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                ?.firstOrNull()?.let {
                    if (it.navigationEndpoint?.browseEndpoint?.browseId != null) {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint.browseEndpoint.browseId
                        )
                    } else null
                }

            val durationFromSubtitle = subtitleGroups
                ?.drop(1)
                ?.firstOrNull { group ->
                    group.firstOrNull()?.text?.parseTime() != null
                }
                ?.firstOrNull()
                ?.text
                ?.parseTime()

            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

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
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text ?: return null,
                artists = artistRuns ?: return null,
                album = album,
                duration = durationFromSubtitle
                    ?: renderer.fixedColumns?.firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer?.text
                        ?.runs?.firstOrNull()
                        ?.text?.parseTime(),
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                endpoint = renderer.overlay?.musicItemThumbnailOverlayRenderer?.content
                    ?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }

        private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isSong -> {
                    val subtitleRuns = renderer.subtitle?.runs ?: return null
                    val expandedRuns = subtitleRuns.splitArtistsByConjunction()
                    val artistRuns = expandedRuns.filter { run ->
                        run.text.isNotBlank() && run.text != "&" && run.text != "," &&
                        run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true
                    }
                    val subtitleGroups = subtitleRuns.splitBySeparator()
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = artistRuns.map { run ->
                            Artist(
                                name = run.text.trim(),
                                id = run.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }.ifEmpty { null } ?: return null,
                        album = null,
                        duration = subtitleGroups.lastOrNull()
                            ?.firstOrNull()
                            ?.takeIf { it.navigationEndpoint == null }
                            ?.text?.parseTime(),
                        musicVideoType = renderer.musicVideoType,
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.anyWatchEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = null,
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.find {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } != null
                    )
                }

                renderer.isPlaylist -> {
                    PlaylistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = Artist(
                            name = renderer.subtitle?.runs?.lastOrNull()?.text ?: return null,
                            id = null
                        ),
                        songCountText = null,
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        playEndpoint = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null
                    )
                }

                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        channelId = renderer.menu?.menuRenderer?.items?.find {
                            it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE"
                        }?.toggleMenuServiceItemRenderer?.defaultServiceEndpoint?.subscribeEndpoint?.channelIds?.firstOrNull(),
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    )
                }

                renderer.isEpisode -> {
                    val videoId = renderer.thumbnailOverlay
                        ?.musicItemThumbnailOverlayRenderer?.content
                        ?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchEndpoint?.videoId ?: return null
                    EpisodeItem(
                        id = videoId,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = renderer.subtitle?.runs?.firstOrNull()?.let {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        },
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        endpoint = WatchEndpoint(videoId = videoId),
                        publishDateText = renderer.subtitle?.runs?.lastOrNull()?.text,
                    )
                }

                renderer.isPodcast -> {
                    PodcastItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        author = renderer.subtitle?.runs?.firstOrNull()?.let {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        },
                        episodeCountText = renderer.subtitle?.runs?.lastOrNull()?.text,
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl(),
                        playEndpoint = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchPlaylistEndpoint,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    )
                }

                else -> null
            }
        }
    }
}