package com.cgens67.innertube.pages

import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.models.filterExplicit
import com.cgens67.innertube.models.filterMusicVideos

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items =
                    items.mapNotNull {
                        it.copy(
                            items =
                                it.items
                                    .filterExplicit()
                                    .ifEmpty { return@mapNotNull null },
                        )
                    },
            )
        } else {
            this
        }

    fun filterMusicVideos(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items =
                    items.mapNotNull {
                        val isVideoSection = it.title?.lowercase()?.let { title ->
                            title.contains("video") || title.contains("vídeo") || title.contains("vidéo") || title.contains("видео") || title.contains("βίντεο")
                        } == true
                        if (isVideoSection) return@mapNotNull null

                        it.copy(
                            items =
                                it.items
                                    .filterMusicVideos()
                                    .ifEmpty { return@mapNotNull null },
                        )
                    },
            )
        } else {
            this
        }
}
