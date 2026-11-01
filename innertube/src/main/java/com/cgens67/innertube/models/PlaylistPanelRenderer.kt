package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelRenderer(
    val title: String? = null,
    val titleText: Runs? = null,
    val shortBylineText: Runs? = null,
    val contents: List<Content>? = null,
    val isInfinite: Boolean? = null,
    val numItemsToShow: Int? = null,
    val playlistId: String? = null,
    val continuations: List<Continuation>? = null,
) {
    @Serializable
    data class Content(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
        val playlistPanelVideoWrapperRenderer: PlaylistPanelVideoWrapperRenderer? = null,
        val automixPreviewVideoRenderer: AutomixPreviewVideoRenderer? = null,
    ) {
        val videoRenderer: PlaylistPanelVideoRenderer?
            get() = playlistPanelVideoRenderer
                ?: playlistPanelVideoWrapperRenderer?.primaryRenderer?.playlistPanelVideoRenderer
    }
}

@Serializable
data class PlaylistPanelVideoWrapperRenderer(
    val primaryRenderer: PrimaryRenderer? = null,
) {
    @Serializable
    data class PrimaryRenderer(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
    )
}