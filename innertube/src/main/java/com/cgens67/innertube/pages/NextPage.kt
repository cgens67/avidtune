package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Album
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.PlaylistPanelVideoRenderer
import com.cgens67.innertube.models.PlaylistPanelVideoWrapperRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.cgens67.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import com.cgens67.innertube.models.isNotSquare
import com.cgens67.innertube.models.response.NextResponse
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime
import kotlinx.serialization.json.contentOrNull

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint,
)

data class VersionTimeMap(
    val segments: List<Segment>,
) {
    data class Segment(
        val fromStartMs: Long,
        val toStartMs: Long,
        val durationMs: Long,
    )

    fun map(positionMs: Long): Long {
        if (segments.isEmpty()) return positionMs
        segments.firstOrNull { positionMs in it.fromStartMs until (it.fromStartMs + it.durationMs) }?.let { seg ->
            return (seg.toStartMs + (positionMs - seg.fromStartMs)).coerceAtLeast(0L)
        }
        val next = segments.firstOrNull { it.fromStartMs > positionMs }
        if (next != null) return next.toStartMs.coerceAtLeast(0L)
        val last = segments.last()
        return (last.toStartMs + last.durationMs).coerceAtLeast(0L)
    }

    fun inverted(): VersionTimeMap =
        VersionTimeMap(
            segments.map { Segment(fromStartMs = it.toStartMs, toStartMs = it.fromStartMs, durationMs = it.durationMs) }
        )
}

data class VersionCounterpartResult(
    val song: SongItem,
    val timeMap: VersionTimeMap?,
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

        val resolvedMusicVideoType = renderer.navigationEndpoint?.musicVideoType
            ?: if (renderer.thumbnail?.isNotSquare() == true) MUSIC_VIDEO_TYPE_OMV else null

        return SongItem(
            id = videoId,
            title = title,
            artists = artists,
            album = album,
            duration = duration,
            musicVideoType = resolvedMusicVideoType,
            thumbnail = thumbnail,
            explicit = explicit,
            libraryAddToken = libraryTokens.addToken,
            libraryRemoveToken = libraryTokens.removeToken
        )
    }

    fun parseVersionCounterpart(
        response: NextResponse,
        videoId: String,
        wantAudio: Boolean,
    ): VersionCounterpartResult? {
        val tabs = response.contents?.singleColumnMusicWatchNextResultsRenderer
            ?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs.orEmpty()
        val queueTab = tabs.firstNotNullOfOrNull { it.tabRenderer?.content?.musicQueueRenderer }
        val playlistPanel = response.continuationContents?.playlistPanelContinuation
            ?: response.contents?.singleColumnMusicWatchNextResultsRenderer?.playlist?.playlistPanelRenderer
            ?: queueTab?.content?.playlistPanelRenderer
            ?: return null

        fun PlaylistPanelVideoRenderer.fits(): Boolean {
            val type = navigationEndpoint?.musicVideoType ?: return true
            return (type == MUSIC_VIDEO_TYPE_ATV) == wantAudio
        }

        fun timeMapOf(counterpart: PlaylistPanelVideoWrapperRenderer.Counterpart): VersionTimeMap? {
            val segments = counterpart.segmentMap?.segment.orEmpty().mapNotNull { seg ->
                val from = seg.primaryVideoStartTimeMilliseconds?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null
                val to = seg.counterpartVideoStartTimeMilliseconds?.contentOrNull?.toLongOrNull() ?: return@mapNotNull null
                val duration = seg.durationMilliseconds?.contentOrNull?.toLongOrNull()?.takeIf { it > 0L } ?: return@mapNotNull null
                VersionTimeMap.Segment(fromStartMs = from, toStartMs = to, durationMs = duration)
            }
            return segments.takeIf { it.isNotEmpty() }?.let(::VersionTimeMap)
        }

        for (content in playlistPanel.contents.orEmpty()) {
            val wrapper = content.playlistPanelVideoWrapperRenderer ?: continue
            val primary = wrapper.primaryRenderer?.playlistPanelVideoRenderer ?: continue
            val counterparts = wrapper.counterpart.orEmpty()
            val primaryId = primary.videoId ?: primary.navigationEndpoint?.watchEndpoint?.videoId

            if (primaryId == videoId) {
                val match = counterparts.firstOrNull {
                    it.counterpartRenderer?.playlistPanelVideoRenderer?.fits() == true
                } ?: continue
                val renderer = match.counterpartRenderer?.playlistPanelVideoRenderer ?: continue
                val song = fromPlaylistPanelVideoRenderer(renderer) ?: continue
                val adjusted = song.copy(
                    musicVideoType = if (wantAudio) MUSIC_VIDEO_TYPE_ATV else (song.musicVideoType ?: MUSIC_VIDEO_TYPE_OMV)
                )
                return VersionCounterpartResult(adjusted, timeMapOf(match))
            }

            val asCounterpart = counterparts.firstOrNull { cp ->
                val cpRenderer = cp.counterpartRenderer?.playlistPanelVideoRenderer
                val cpId = cpRenderer?.videoId ?: cpRenderer?.navigationEndpoint?.watchEndpoint?.videoId
                cpId == videoId
            } ?: continue

            if (!primary.fits()) continue
            val song = fromPlaylistPanelVideoRenderer(primary) ?: continue
            val adjusted = song.copy(
                musicVideoType = if (wantAudio) MUSIC_VIDEO_TYPE_ATV else (song.musicVideoType ?: MUSIC_VIDEO_TYPE_OMV)
            )
            return VersionCounterpartResult(adjusted, timeMapOf(asCounterpart)?.inverted())
        }
        return null
    }
}