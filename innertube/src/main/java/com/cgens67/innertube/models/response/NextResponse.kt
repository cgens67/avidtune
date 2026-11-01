package com.cgens67.innertube.models.response

import com.cgens67.innertube.models.NavigationEndpoint
import com.cgens67.innertube.models.PlaylistPanelRenderer
import com.cgens67.innertube.models.Tabs
import com.cgens67.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val currentVideoEndpoint: NavigationEndpoint? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer? = null,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults? = null,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer? = null,
            val playlist: PlaylistWrapper? = null,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer? = null,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab> = emptyList(),
                )
            }

            @Serializable
            data class PlaylistWrapper(
                val playlistPanelRenderer: PlaylistPanelRenderer? = null,
            )
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer? = null,
    )
}