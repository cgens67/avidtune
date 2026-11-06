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

    private val VIEW_PLAY_REGEX = Regex(
        """(?i)\d+[\d.,\s]*[kmb万億]?\s*(views|plays|reproducciones|visualizaciones|vistas|aufrufe|vues|reproduções|escuchas|ecoutes|écoutes|visualizzazioni|weergaven|wyświetlenia|odtworzeń|zhlédnutí|visninger|katselukertaa|visningar|просмотров|прослушиваний|переглядів|회|回|次|再生|播放)"""
    )

    private val ARTIST_SPLIT_REGEX = Regex(
        """,?\s*&\s*|,?\s+(?i:and)\s+|\s+(?i:feat\.?|ft\.?|featuring)\s+|,+\s+|,\s*[/;]\s*|\s*[/;]\s*|,?\s+(?i:x)\s+"""
    )

    private val PRESERVED_ARTIST_NAMES = setOf(
        "tyler, the creator",
        "earth, wind & fire",
        "earth, wind and fire",
        "kool & the gang",
        "kool and the gang",
        "simon & garfunkel",
        "simon and garfunkel",
        "crosby, stills, nash & young",
        "crosby, stills, nash and young",
        "crosby, stills & nash",
        "crosby, stills and nash",
        "hall & oates",
        "hall and oates",
        "brooks & dunn",
        "above & beyond",
        "tom petty and the heartbreakers",
        "tom petty & the heartbreakers",
        "huey lewis & the news",
        "huey lewis and the news",
        "kc & the sunshine band",
        "kc and the sunshine band",
        "bob marley & the wailers",
        "bob marley and the wailers",
        "sly & the family stone",
        "sly and the family stone",
        "marina and the diamonds",
        "mumford & sons",
        "mumford and sons",
        "of monsters and men",
        "king gizzard & the lizard wizard",
        "king gizzard and the lizard wizard",
        "joan jett & the blackhearts",
        "joan jett and the blackhearts",
        "the mamas & the papas",
        "the mamas and the papas",
        "blood, sweat & tears",
        "blood, sweat and tears",
        "iron & wine",
        "iron and wine",
        "fitz and the tantrums",
        "hootie & the blowfish",
        "hootie and the blowfish"
    )

    fun splitArtistNames(raw: String): List<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (PRESERVED_ARTIST_NAMES.contains(trimmed.lowercase())) {
            return listOf(trimmed)
        }
        val parts = trimmed.split(ARTIST_SPLIT_REGEX)
            .map { it.trim() }
            .filter { it.isNotBlank() }
        return if (parts.isEmpty()) listOf(trimmed) else parts
    }

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

    fun extractViews(runs: List<Run>?): String? =
        runs.orEmpty().firstNotNullOfOrNull { run ->
            val text = run.text.replace('\u00a0', ' ').trim()
            if (run.navigationEndpoint == null &&
                !text.isDurationText() &&
                (VIEW_PLAY_REGEX.containsMatchIn(text) ||
                    text.endsWith("views", ignoreCase = true) ||
                    text.endsWith("plays", ignoreCase = true))
            ) {
                text
            } else null
        }

    private fun String.isDurationText(): Boolean = matches(Regex("""\d{1,2}[:.,]\d{2}(?:[:.,]\d{2})?"""))
}