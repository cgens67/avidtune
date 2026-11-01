package com.cgens67.innertube.pages

import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.MusicTwoRowItemRenderer

object NewReleaseAlbumPage {
    fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): AlbumItem? {
        return AlbumItem(
            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
            playlistId =
                renderer.thumbnailOverlay
                    ?.musicItemThumbnailOverlayRenderer
                    ?.content
                    ?.musicPlayButtonRenderer
                    ?.playNavigationEndpoint
                    ?.watchPlaylistEndpoint
                    ?.playlistId ?: return null,
            title =
                renderer.title.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                PageHelper.extractArtists(renderer.subtitle?.runs),
            year =
                renderer.subtitle
                    ?.runs
                    ?.lastOrNull()
                    ?.text
                    ?.toIntOrNull(),
            thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
            explicit =
                renderer.subtitleBadges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
        )
    }
}