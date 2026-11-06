package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.PlaylistPanelVideoRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint,
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val videoId = renderer.videoId
            ?: renderer.navigationEndpoint?.watchEndpoint?.videoId
            ?: return null

        val title = renderer.title
            ?.runs
            ?.firstOrNull()
            ?.text ?: return null

        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator()
        val shortByLineRuns = renderer.shortBylineText?.runs?.splitBySeparator()

        val artists = PageHelper.extractArtists(renderer.longBylineText?.runs)
            .ifEmpty { PageHelper.extractArtists(renderer.shortBylineText?.runs) }
            .ifEmpty {
                val bylineRuns = renderer.shortBylineText?.runs ?: renderer.longBylineText?.runs
                val fallbackName = bylineRuns?.joinToString("") { it.text }?.trim()
                if (!fallbackName.isNullOrBlank()) {
                    PageHelper.splitArtistNames(fallbackName).map { Artist(it, null) }
                } else emptyList()
            }

        val album = (longByLineRuns ?: shortByLineRuns)?.let { groups ->
            groups.getOrNull(1)?.firstOrNull()?.takeIf {
                it.navigationEndpoint?.browseEndpoint != null
            }?.let {
                val browseId = it.navigationEndpoint?.browseEndpoint?.browseId
                if (browseId != null) Album(name = it.text, id = browseId) else null
            }
        }

        val duration = renderer.lengthText
            ?.runs
            ?.firstOrNull()
            ?.text
            ?.parseTime()

        val thumbnail = renderer.thumbnail?.thumbnails
            ?.lastOrNull()
            ?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val explicit = renderer.badges?.any {
            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
        } == true

        return SongItem(
            id = videoId,
            title = title,
            artists = artists,
            album = album,
            duration = duration,
            musicVideoType = renderer.navigationEndpoint?.musicVideoType,
            thumbnail = thumbnail,
            explicit = explicit,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }
}