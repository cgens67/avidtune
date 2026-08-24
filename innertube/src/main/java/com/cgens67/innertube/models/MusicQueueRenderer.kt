package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicQueueRenderer(
    val content: Content?,
    val header: Header?
) {
    @Serializable
    data class Content(
        val playlistPanelRenderer: PlaylistPanelRenderer?
    )

    @Serializable
    data class Header(
        val musicQueueHeaderRenderer: MusicQueueHeaderRenderer?
    ) {
        @Serializable
        data class MusicQueueHeaderRenderer(
            val subtitle: Runs?
        )
    }
}