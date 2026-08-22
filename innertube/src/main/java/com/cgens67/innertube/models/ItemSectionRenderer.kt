package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemSectionRenderer(
    val contents: List<Content>?,
    val header: Header? = null,
) {
    @Serializable
    data class Content(
        val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer? = null,
        val musicShelfRenderer: MusicShelfRenderer? = null,
        val musicPlaylistShelfRenderer: MusicPlaylistShelfRenderer? = null,
        val gridRenderer: GridRenderer? = null,
    )

    @Serializable
    data class Header(
        val itemSectionTabbedHeaderRenderer: ItemSectionTabbedHeaderRenderer?,
    )

    @Serializable
    data class ItemSectionTabbedHeaderRenderer(
        val title: Runs?,
    )
}