package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab> = emptyList(),
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer? = null,
    ) {
        @Serializable
        data class TabRenderer(
            val title: String? = null,
            val content: Content? = null,
            val endpoint: NavigationEndpoint? = null,
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer? = null,
                val musicQueueRenderer: MusicQueueRenderer? = null,
            )
        }
    }
}