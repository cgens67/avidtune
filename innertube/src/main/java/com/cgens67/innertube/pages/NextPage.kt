package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.PlaylistPanelVideoRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator() ?: return null

        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

        return SongItem(
            id = renderer.videoId
                ?: renderer.navigationEndpoint.watchEndpoint?.videoId
                ?: return null,
            title =
                renderer.title
                    ?.runs
                    ?.firstOrNull()
                    ?.text ?: return null,
            artists =
                longByLineRuns.firstOrNull()?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                    )
                } ?: return null,
            album =
                longByLineRuns
                    .getOrNull(1)
                    ?.firstOrNull()
                    ?.takeIf {
                        it.navigationEndpoint?.browseEndpoint != null
                    }?.let {
                        Album(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId!!,
                        )
                    },
            duration =
                renderer.lengthText
                    ?.runs
                    ?.firstOrNull()
                    ?.text
                    ?.parseTime() ?: return null,
            musicVideoType = renderer.navigationEndpoint.musicVideoType,
            thumbnail =
                renderer.thumbnail.thumbnails
                    .lastOrNull()
                    ?.url ?: return null,
            explicit =
                renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }
}