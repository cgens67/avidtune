package com.cgens67.innertube.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

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
    val counterpart: List<Counterpart>? = null,
) {
    @Serializable
    data class PrimaryRenderer(
        val playlistPanelVideoRenderer: PlaylistPanelVideoRenderer? = null,
    )

    @Serializable
    data class Counterpart(
        val counterpartRenderer: PrimaryRenderer? = null,
        val segmentMap: SegmentMap? = null,
    )

    @Serializable
    data class SegmentMap(
        val segment: List<Segment>? = null,
    ) {
        @Serializable
        data class Segment(
            val primaryVideoStartTimeMilliseconds: JsonPrimitive? = null,
            val counterpartVideoStartTimeMilliseconds: JsonPrimitive? = null,
            val durationMilliseconds: JsonPrimitive? = null,
        )
    }
}