package com.cgens67.innertube.pages

import com.cgens67.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)