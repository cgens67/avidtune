package com.cgens67.innertube.pages

import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.Menu
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer.FlexColumn
import com.cgens67.innertube.models.Run
import com.cgens67.innertube.models.splitArtistsByConjunction
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime
import timber.log.Timber

object PageHelper {
    private val LIBRARY_ADD_ICONS = setOf("LIBRARY_ADD", "BOOKMARK_BORDER")
    private val LIBRARY_SAVED_ICONS = setOf("LIBRARY_SAVED", "BOOKMARK", "LIBRARY_REMOVE")
    private val ALL_LIBRARY_ICONS = LIBRARY_ADD_ICONS + LIBRARY_SAVED_ICONS

    data class LibraryFeedbackTokens(
        val addToken: String?,      // Token to add song to library (from BOOKMARK_BORDER)
        val removeToken: String?    // Token to remove song from library (from BOOKMARK)
    )

    fun isLibraryIcon(iconType: String?): Boolean {
        if (iconType == null) return false
        if (iconType == "KEEP" || iconType == "KEEP_OFF") return false
        return iconType in ALL_LIBRARY_ICONS || iconType.startsWith("LIBRARY_")
    }

    fun isAddLibraryIcon(iconType: String?): Boolean {
        return iconType in LIBRARY_ADD_ICONS
    }

    fun isSavedLibraryIcon(iconType: String?): Boolean {
        return iconType in LIBRARY_SAVED_ICONS
    }

    fun extractRuns(columns: List<FlexColumn>, typeLike: String): List<Run> {
        val filteredRuns = mutableListOf<Run>()
        for (column in columns) {
            val runs = column.musicResponsiveListItemFlexColumnRenderer.text?.runs
                ?: continue

            for (run in runs) {
                val typeStr = run.navigationEndpoint?.watchEndpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                    ?: run.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    ?: continue

                if (typeLike in typeStr) {
                    filteredRuns.add(run)
                }
            }
        }
        return filteredRuns
    }

    fun extractLibraryTokensFromMenuItems(
        menuItems: List<Menu.MenuRenderer.Item>?
    ): LibraryFeedbackTokens {
        if (menuItems == null) return LibraryFeedbackTokens(null, null)

        var addToken: String? = null
        var removeToken: String? = null

        for (item in menuItems) {
            val toggleRenderer = item.toggleMenuServiceItemRenderer ?: continue
            val iconType = toggleRenderer.defaultIcon?.iconType ?: continue

            if (iconType == "KEEP" || iconType == "KEEP_OFF") continue

            if (!isLibraryIcon(iconType)) continue

            val defaultToken = toggleRenderer.defaultServiceEndpoint?.feedbackEndpoint?.feedbackToken
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

    fun extractFeedbackToken(menu: Menu.MenuRenderer.Item.ToggleMenuServiceRenderer?, type: String): String? {
        if (menu == null) return null
        val defaultToken = menu.defaultServiceEndpoint?.feedbackEndpoint?.feedbackToken
        val toggledToken = menu.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken
        val iconType = menu.defaultIcon?.iconType

        val songNotInLibrary = iconType in LIBRARY_ADD_ICONS

        return when (type) {
            "LIBRARY_ADD" -> {
                if (songNotInLibrary) {
                    defaultToken
                } else {
                    toggledToken
                }
            }
            "LIBRARY_REMOVE", "LIBRARY_SAVED" -> {
                if (songNotInLibrary) {
                    toggledToken
                } else {
                    defaultToken
                }
            }
            else -> if (iconType == type) defaultToken else toggledToken
        }
    }

    fun extractArtists(runs: List<Run>?): List<Artist> {
        val sections = runs.orEmpty().splitBySeparator()
        val linkedArtists = sections.flatMap { section ->
            val expandedRuns = section.splitArtistsByConjunction()
            if (expandedRuns.none { it.isArtistRun() }) return@flatMap emptyList()
            expandedRuns.mapNotNull { run ->
                val browseEndpoint = run.navigationEndpoint?.browseEndpoint
                val browseId = browseEndpoint?.browseId
                when {
                    browseId?.startsWith("UC") == true || browseEndpoint?.isArtistEndpoint == true ->
                        Artist(run.text, browseId)
                    browseId == null && !run.text.isMetadataText() -> Artist(run.text.trim(), null)
                    else -> null
                }
            }
        }
        if (linkedArtists.isNotEmpty()) return linkedArtists

        return sections.firstNotNullOfOrNull { section ->
            section
                .splitArtistsByConjunction()
                .filter { run ->
                    run.navigationEndpoint?.browseEndpoint == null &&
                        run.text.isNotBlank() &&
                        run.text.trim() != "," &&
                        !run.text.isMetadataText()
                }
                .map { Artist(it.text.trim(), null) }
                .takeIf { it.isNotEmpty() }
        }.orEmpty()
    }

    fun extractDuration(runs: List<Run>?): Int? =
        runs.orEmpty().firstNotNullOfOrNull { run ->
            run.text.trim().takeIf { it.isDurationText() }?.parseTime()
        }

    private fun Run.isArtistRun(): Boolean {
        val browseEndpoint = navigationEndpoint?.browseEndpoint
        return browseEndpoint?.browseId?.startsWith("UC") == true || browseEndpoint?.isArtistEndpoint == true
    }

    private fun String.isMetadataText(): Boolean {
        val value = trim()
        val lower = value.lowercase().replace('\u00a0', ' ')
        return value.isDurationText() ||
            value.matches(Regex("""(?:19|20)\d{2}""")) ||
            lower in setOf("song", "video", "single", "album", "episode", "playlist", "podcast") ||
            lower.contains("monthly audience") ||
            lower.matches(Regex("""\d[\d.,]*[kmb]?\s*(?:views?|plays?|likes?|subscribers?)"""))
    }

    private fun String.isDurationText(): Boolean =
        matches(Regex("""\d{1,2}[:.,]\d{2}(?:[:.,]\d{2})?"""))

}