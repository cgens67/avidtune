package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_ARTIST
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_LIBRARY_ARTIST
import com.cgens67.innertube.models.BrowseEndpoint.BrowseEndpointContextSupportedConfigs.BrowseEndpointContextMusicConfig.Companion.MUSIC_PAGE_TYPE_USER_CHANNEL
import com.cgens67.innertube.models.Menu
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.cgens67.innertube.models.Run
import com.cgens67.innertube.utils.parseTime

object PageHelper {
    private val LIBRARY_ADD_ICONS = setOf("LIBRARY_ADD", "BOOKMARK_BORDER")
    private val LIBRARY_SAVED_ICONS = setOf("LIBRARY_SAVED", "BOOKMARK", "LIBRARY_REMOVE")
    private val ALL_LIBRARY_ICONS = LIBRARY_ADD_ICONS + LIBRARY_SAVED_ICONS

    data class LibraryFeedbackTokens(
        val addToken: String?,
        val removeToken: String?,
    )

    fun isLibraryIcon(iconType: String?): Boolean {
        if (iconType == null) return false
        if (iconType == "KEEP" || iconType == "KEEP_OFF") return false
        return iconType in ALL_LIBRARY_ICONS || iconType.startsWith("LIBRARY_")
    }

    fun isAddLibraryIcon(iconType: String?): Boolean = iconType in LIBRARY_ADD_ICONS
    fun isSavedLibraryIcon(iconType: String?): Boolean = iconType in LIBRARY_SAVED_ICONS

    fun extractRuns(
        columns: List<FlexColumn>,
        typeLike: String,
    ): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs ?: continue
            for (run in runs) {
                val typeStr =
                    run.navigationEndpoint
                        ?.watchEndpoint
                        ?.watchEndpointMusicSupportedConfigs
                        ?.watchEndpointMusicConfig
                        ?.musicVideoType
                        ?: run.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseEndpointContextSupportedConfigs
                            ?.browseEndpointContextMusicConfig
                            ?.pageType
                        ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }

    fun extractLibraryTokensFromMenuItems(menuItems: List<Menu.MenuRenderer.Item>?): LibraryFeedbackTokens {
        if (menuItems == null) return LibraryFeedbackTokens(null, null)

        var addToken: String? = null
        var removeToken: String? = null

        for (item in menuItems) {
            val toggleRenderer = item.toggleMenuServiceItemRenderer ?: continue
            val iconType = toggleRenderer.defaultIcon.iconType

            if (iconType == "KEEP" || iconType == "KEEP_OFF") continue
            if (!isLibraryIcon(iconType)) continue

            val defaultToken = toggleRenderer.defaultServiceEndpoint.feedbackEndpoint?.feedbackToken
            val toggledToken = toggleRenderer.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken

            when {
                isAddLibraryIcon(iconType) -> {
                    if (addToken == null) addToken = defaultToken
                    if (removeToken == null) removeToken = toggledToken
                }
                isSavedLibraryIcon(iconType) -> {
                    if (removeToken == null) removeToken = defaultToken
                    if (addToken == null) addToken = toggledToken
                }
            }
        }

        return LibraryFeedbackTokens(addToken, removeToken)
    }

    fun extractArtists(runs: List<Run>?): List<Artist> =
        runs.orEmpty().mapNotNull { run ->
            val endpoint = run.navigationEndpoint?.browseEndpoint ?: return@mapNotNull null
            val pageType = endpoint.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
            if (run.text.isBlank() ||
                !(
                    pageType == MUSIC_PAGE_TYPE_ARTIST || pageType == MUSIC_PAGE_TYPE_LIBRARY_ARTIST ||
                        pageType == MUSIC_PAGE_TYPE_USER_CHANNEL || pageType == null && endpoint.browseId.startsWith("UC")
                )
            ) {
                null
            } else {
                Artist(run.text.trim(), endpoint.browseId)
            }
        }

    fun extractDuration(runs: List<Run>?): Int? =
        runs.orEmpty().firstNotNullOfOrNull { run ->
            run.text
                .trim()
                .takeIf { it.isDurationText() }
                ?.parseTime()
        }

    private fun String.isDurationText(): Boolean = matches(Regex("""\d{1,2}[:.,]\d{2}(?:[:.,]\d{2})?"""))
}