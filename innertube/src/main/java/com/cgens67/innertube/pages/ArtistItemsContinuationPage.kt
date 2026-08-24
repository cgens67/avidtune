package com.cgens67.innertube.pages

import com.cgens67.innertube.models.YTItem

data class ArtistItemsContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)