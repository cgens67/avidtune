package com.arturo254.opentune.models

import com.arturo254.innertube.models.YTItem
import com.arturo254.opentune.db.entities.LocalItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)
