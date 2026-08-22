package com.cgens67.innertube.pages

import com.cgens67.innertube.models.SongItem

data class PlaylistContinuationPage(
    val songs: List<SongItem>,
    val continuation: String?,
)