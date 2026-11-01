package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaylistPanelVideoRenderer(
    val title: Runs? = null,
    val lengthText: Runs? = null,
    val longBylineText: Runs? = null,
    val shortBylineText: Runs? = null,
    val badges: List<Badges>? = null,
    val videoId: String? = null,
    val playlistSetVideoId: String? = null,
    val selected: Boolean = false,
    val thumbnail: Thumbnails? = null,
    val unplayableText: Runs? = null,
    val menu: Menu? = null,
    val navigationEndpoint: NavigationEndpoint? = null,
)