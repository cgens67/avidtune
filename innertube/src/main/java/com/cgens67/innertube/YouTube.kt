package com.cgens67.innertube

import com.cgens67.innertube.models.AccountInfo
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.Artist
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.BrowseEndpoint
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.GridRenderer
import com.cgens67.innertube.models.MediaInfo
import com.cgens67.innertube.models.MusicCarouselShelfRenderer
import com.cgens67.innertube.models.MusicMultiRowListItemRenderer
import com.cgens67.innertube.models.MusicResponsiveListItemRenderer
import com.cgens67.innertube.models.MusicShelfRenderer
import com.cgens67.innertube.models.MusicTwoRowItemRenderer
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.Run
import com.cgens67.innertube.models.Runs
import com.cgens67.innertube.models.SearchSuggestions
import com.cgens67.innertube.models.SectionListRenderer
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.models.YouTubeAccount
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.YouTubeLocale
import com.cgens67.innertube.models.extractYouTubeAccounts
import com.cgens67.innertube.models.getContinuation
import com.cgens67.innertube.models.getItems
import com.cgens67.innertube.models.oddElements
import com.cgens67.innertube.models.splitBySeparator
import com.cgens67.innertube.utils.parseTime
import com.cgens67.innertube.models.response.AccountMenuResponse
import com.cgens67.innertube.models.response.BrowseResponse
import com.cgens67.innertube.models.response.CreatePlaylistResponse
import com.cgens67.innertube.models.response.EditPlaylistResponse
import com.cgens67.innertube.models.response.FeedbackResponse
import com.cgens67.innertube.models.response.GetQueueResponse
import com.cgens67.innertube.models.response.GetSearchSuggestionsResponse
import com.cgens67.innertube.models.response.GetTranscriptResponse
import com.cgens67.innertube.models.response.NextResponse
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.models.response.SearchResponse
import com.cgens67.innertube.pages.AlbumPage
import com.cgens67.innertube.pages.ArtistItemsContinuationPage
import com.cgens67.innertube.pages.ArtistItemsPage
import com.cgens67.innertube.pages.ArtistPage
import com.cgens67.innertube.pages.BrowseResult
import com.cgens67.innertube.pages.ChartsPage
import com.cgens67.innertube.pages.ExplorePage
import com.cgens67.innertube.pages.HistoryPage
import com.cgens67.innertube.pages.HomePage
import com.cgens67.innertube.pages.LibraryContinuationPage
import com.cgens67.innertube.pages.LibraryPage
import com.cgens67.innertube.pages.MoodAndGenres
import com.cgens67.innertube.pages.NewReleaseAlbumPage
import com.cgens67.innertube.pages.NextPage
import com.cgens67.innertube.pages.NextResult
import com.cgens67.innertube.pages.PageHelper
import com.cgens67.innertube.pages.PlaylistContinuationPage
import com.cgens67.innertube.pages.PlaylistPage
import com.cgens67.innertube.pages.PodcastPage
import com.cgens67.innertube.pages.RelatedPage
import com.cgens67.innertube.pages.SearchPage
import com.cgens67.innertube.pages.SearchResult
import com.cgens67.innertube.pages.SearchSuggestionPage
import com.cgens67.innertube.pages.SearchSummary
import com.cgens67.innertube.pages.SearchSummaryPage
import com.metrolist.innertubex.models.YouTubeClient.Companion.WEB
import com.metrolist.innertubex.models.YouTubeClient.Companion.WEB_REMIX
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.FilterInputStream
import java.io.InputStream
import java.net.Proxy
import kotlinx.coroutines.CancellationException

/**
 * Parse useful data with [InnerTube] sending requests.
 * Modified from [ViMusic](https://github.com/vfsfitvnm/ViMusic)
 */
object YouTube {
    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }
    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }
    var authUser: String
        get() = innerTube.authUser
        set(value) {
            innerTube.authUser = value
        }
    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }

    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) {
            innerTube.proxyAuth = value
        }
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    fun extractionTransport(): InnerTube.ExtractionTransport = innerTube.extractionTransport()

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        runCatching {
            val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries =
                    response.contents
                        ?.getOrNull(0)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            content.searchSuggestionRenderer
                                ?.suggestion
                                ?.runs
                                ?.joinToString(separator = "") { it.text }
                        }.orEmpty(),
                recommendedItems =
                    response.contents
                        ?.getOrNull(1)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        }.orEmpty(),
            )
        }

    // Background metadata resolution must never write to account search history.
    suspend fun searchSummary(
        query: String,
        recordSearchHistory: Boolean = false,
    ): Result<SearchSummaryPage> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, setLogin = recordSearchHistory).body<SearchResponse>()
            val allSummaries = mutableListOf<SearchSummary>()

            response.contents
                ?.tabbedSearchResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { section ->
                    if (section.musicCardShelfRenderer != null) {
                        val cardRenderer = section.musicCardShelfRenderer
                        val cardArtists = if (cardRenderer.onTap.browseEndpoint?.isArtistEndpoint == true) {
                            listOfNotNull(
                                cardRenderer.title.runs?.firstOrNull()?.text?.let { name ->
                                    Artist(name, cardRenderer.onTap.browseEndpoint.browseId)
                                },
                            )
                        } else {
                            PageHelper.extractArtists(cardRenderer.subtitle.runs)
                        }
                        val items =
                            listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(cardRenderer))
                                .plus(
                                    cardRenderer.contents
                                        ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                        ?.mapNotNull { renderer ->
                                            SearchPage.toYTItem(renderer, cardArtists)
                                        }
                                        .orEmpty(),
                                ).distinctBy { it.id }

                        if (items.isNotEmpty()) {
                            allSummaries.add(
                                SearchSummary(
                                    title =
                                        cardRenderer.header
                                            ?.musicCardShelfHeaderBasicRenderer
                                            ?.title
                                            ?.runs
                                            ?.firstOrNull()
                                            ?.text
                                            ?: YouTubeConstants.DEFAULT_TOP_RESULT,
                                    items = items,
                                ),
                            )
                        }
                    } else if (section.musicShelfRenderer != null) {
                        val items =
                            section.musicShelfRenderer.contents
                                ?.getItems()
                                ?.mapNotNull { SearchPage.toYTItem(it) }
                                ?.distinctBy { it.id }
                                ?: emptyList()

                        if (items.isEmpty()) return@forEach

                        val apiTitle =
                            section.musicShelfRenderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text

                        if (apiTitle != null) {
                            allSummaries.add(SearchSummary(title = apiTitle, items = items))
                        } else {
                            allSummaries.addAll(groupItemsByType(items))
                        }
                    } else if (section.itemSectionRenderer != null) {
                        val items =
                            section.itemSectionRenderer.contents
                                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                ?.mapNotNull { SearchPage.toYTItem(it) }
                                ?.distinctBy { it.id }
                                ?: emptyList()

                        if (items.isNotEmpty()) {
                            allSummaries.addAll(groupItemsByType(items))
                        }
                    }
                }

            val mergedSummaries =
                allSummaries
                    .groupBy { it.title }
                    .map { (title, sections) ->
                        SearchSummary(
                            title = title,
                            items = sections.flatMap { it.items }.distinctBy { it.id },
                        )
                    }
                    .sortedBy { summary ->
                        when (summary.title) {
                            YouTubeConstants.DEFAULT_TOP_RESULT -> 0
                            "Songs" -> 1
                            "Videos" -> 2
                            "Albums" -> 3
                            "Artists" -> 4
                            "Playlists" -> 5
                            "Podcasts" -> 6
                            "Episodes" -> 7
                            "Profiles" -> 8
                            else -> 9
                        }
                    }

            SearchSummaryPage(summaries = mergedSummaries)
        }

    private fun groupItemsByType(items: List<YTItem>): List<SearchSummary> {
        val grouped =
            items.groupBy { item ->
                when (item) {
                    is EpisodeItem -> "Episodes"
                    is PodcastItem -> "Podcasts"
                    is AlbumItem -> "Albums"
                    is ArtistItem -> if (item.isProfile) "Profiles" else "Artists"
                    is PlaylistItem -> "Playlists"
                    is SongItem -> when {
                        item.isEpisode -> "Episodes"
                        item.isVideoSong -> "Videos"
                        else -> "Songs"
                    }
                }
            }

        val sectionOrder =
            listOf(
                "Songs", "Videos", "Albums", "Artists", "Playlists",
                "Podcasts", "Episodes", "Profiles", YouTubeConstants.DEFAULT_OTHER_RESULTS,
            )

        return sectionOrder.mapNotNull { sectionName ->
            grouped[sectionName]?.takeIf { it.isNotEmpty() }?.let { groupItems ->
                SearchSummary(title = sectionName, items = groupItems)
            }
        }
    }

    suspend fun search(
        query: String,
        filter: SearchFilter,
        recordSearchHistory: Boolean = false,
    ): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, filter.value, setLogin = recordSearchHistory).body<SearchResponse>()
            val searchItems = mutableListOf<YTItem>()
            var searchContinuation: String? = null

            response.contents
                ?.tabbedSearchResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { section ->
                    if (section.musicShelfRenderer != null) {
                        val shelf = section.musicShelfRenderer
                        val items = shelf.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
                        searchItems.addAll(items)
                        if (searchContinuation == null) {
                            searchContinuation = shelf.continuations?.getContinuation()
                        }
                    } else if (section.itemSectionRenderer != null) {
                        val items =
                            section.itemSectionRenderer.contents
                                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                ?.mapNotNull { SearchPage.toYTItem(it) }
                                ?: emptyList()
                        searchItems.addAll(items)
                    }
                }

            SearchResult(
                items = searchItems.distinctBy { it.id },
                continuation = searchContinuation,
            )
        }

    suspend fun searchContinuation(
        continuation: String,
        recordSearchHistory: Boolean = false,
    ): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, continuation = continuation, setLogin = recordSearchHistory).body<SearchResponse>()
            val items =
                response.continuationContents
                    ?.musicShelfContinuation
                    ?.contents
                    ?.mapNotNull {
                        SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                    } ?: emptyList()
            SearchResult(
                items = items,
                continuation =
                    if (items.isEmpty()) {
                        null
                    } else {
                        response.continuationContents
                            ?.musicShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                    },
            )
        }

    suspend fun album(
        browseId: String,
        withSongs: Boolean = true,
    ): Result<AlbumPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            if (browseId.contains("FEmusic_library_privately_owned_release_detail")) {
                val playlistId =
                    response.header
                        ?.musicDetailHeaderRenderer
                        ?.menu
                        ?.menuRenderer
                        ?.topLevelButtons
                        ?.firstOrNull()
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint
                        ?.playlistId!!
                val albumItem =
                    AlbumItem(
                        browseId = browseId,
                        playlistId = playlistId,
                        title =
                            response.header.musicDetailHeaderRenderer.title.runs
                                ?.firstOrNull()
                                ?.text!!,
                        artists =
                            PageHelper
                                .extractArtists(response.header.musicDetailHeaderRenderer.subtitle.runs)
                                .takeIf { it.isNotEmpty() },
                        year =
                            response.header.musicDetailHeaderRenderer.subtitle.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()!!
                                .url,
                        explicit = false,
                    )
                return@runCatching AlbumPage(
                    album = albumItem,
                    songs =
                        response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicShelfRenderer
                            ?.contents
                            ?.getItems()
                            ?.mapNotNull {
                                AlbumPage.getSong(it, albumItem)
                            }!!
                            .toMutableList(),
                    otherVersions = emptyList(),
                )
            } else {
                val playlistId =
                    response.microformat
                        ?.microformatDataRenderer
                        ?.urlCanonical
                        ?.substringAfterLast('=')!!
                val albumItem =
                    AlbumItem(
                        browseId = browseId,
                        playlistId = playlistId,
                        title =
                            response.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text!!,
                        artists =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.straplineTextOne
                                ?.runs
                                ?.let(PageHelper::extractArtists)
                                ?.takeIf { it.isNotEmpty() },
                        year =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url!!,
                        explicit = false,
                    )
                val albumSongsList =
                    if (withSongs) {
                        albumSongs(
                            playlistId,
                            albumItem,
                        ).getOrThrow()
                    } else {
                        emptyList()
                    }
                val performer =
                    albumSongsList.firstOrNull()?.artists?.firstOrNull()?.takeIf { first ->
                        first.name.isNotBlank() &&
                            albumSongsList.all { it.artists.firstOrNull()?.name == first.name }
                    }
                val resolvedAlbum =
                    if (performer != null && albumItem.artists?.any { it.name == performer.name } != true) {
                        albumItem.copy(artists = listOf(performer))
                    } else {
                        albumItem
                    }
                return@runCatching AlbumPage(
                    album = resolvedAlbum,
                    songs = albumSongsList,
                    otherVersions =
                        response.contents.twoColumnBrowseResultsRenderer.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.getOrNull(1)
                            ?.musicCarouselShelfRenderer
                            ?.contents
                            ?.mapNotNull { it.musicTwoRowItemRenderer }
                            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                            .orEmpty(),
                )
            }
        }

    suspend fun albumSongs(
        playlistId: String,
        album: AlbumItem? = null,
    ): Result<List<SongItem>> =
        runCatching {
            var response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
            val shelf =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.secondaryContents
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
            val shelfContents =
                shelf?.musicPlaylistShelfRenderer?.contents
                    ?: shelf?.musicShelfRenderer?.contents
                    ?: emptyList()
            val songs =
                shelfContents
                    .getItems()
                    .mapNotNull {
                        AlbumPage.getSong(it, album)
                    }
                    .toMutableList()
            var continuation = shelfContents.getContinuation()
            val seenContinuations = mutableSetOf<String>()
            var requestCount = 0
            val maxRequests = 50

            while (continuation != null && requestCount < maxRequests) {
                if (continuation in seenContinuations) break
                seenContinuations.add(continuation)
                requestCount++

                response =
                    innerTube.browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                    ).body<BrowseResponse>()
                songs +=
                    response.onResponseReceivedActions
                        ?.firstOrNull()
                        ?.appendContinuationItemsAction
                        ?.continuationItems
                        ?.getItems()
                        ?.mapNotNull {
                            AlbumPage.getSong(it, album)
                        }.orEmpty()
                continuation =
                    response.continuationContents
                        ?.musicPlaylistShelfContinuation
                        ?.continuations
                        ?.getContinuation()
            }
            songs
        }

    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()

            fun mapRuns(runs: List<Run>?): List<Run>? =
                runs?.map { run ->
                    Run(
                        text = run.text,
                        navigationEndpoint = run.navigationEndpoint,
                    )
                }

            val descriptionRuns =
                response.contents
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
                    ?.musicDescriptionShelfRenderer
                    ?.description
                    ?.runs
                    ?.let(::mapRuns)
                    ?: response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.description
                        ?.runs
                        ?.let(::mapRuns)

            val immersiveSubscribed =
                response.header
                    ?.musicImmersiveHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.subscribed
            val visualSubscribed =
                response.header
                    ?.musicVisualHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.subscribed
            val isSubscribed = immersiveSubscribed ?: visualSubscribed ?: false

            val channelIdFromVisual =
                response.header
                    ?.musicVisualHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.channelId

            ArtistPage(
                artist =
                    ArtistItem(
                        id = browseId,
                        title =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text
                                ?: response.header
                                    ?.musicHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text!!,
                        thumbnail =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.foregroundThumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicDetailHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl(),
                        channelId =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.subscriptionButton
                                ?.subscribeButtonRenderer
                                ?.channelId
                                ?: channelIdFromVisual,
                        playEndpoint =
                            response.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveListItemRenderer
                                ?.overlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchEndpoint,
                        shuffleEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.playButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint
                                ?: response.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.firstOrNull()
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.firstOrNull()
                                    ?.musicShelfRenderer
                                    ?.contents
                                    ?.firstOrNull()
                                    ?.musicResponsiveListItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint,
                        radioEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.startRadioButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                    ),
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                description = descriptionRuns?.joinToString(separator = "") { it.text },
                subscriberCountText =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton2
                        ?.subscribeButtonRenderer
                        ?.subscriberCountWithSubscribeText
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?: response.header
                            ?.musicImmersiveHeaderRenderer
                            ?.subscriptionButton
                            ?.subscribeButtonRenderer
                            ?.longSubscriberCountText
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                        ?: response.header
                            ?.musicImmersiveHeaderRenderer
                            ?.subscriptionButton
                            ?.subscribeButtonRenderer
                            ?.shortSubscriberCountText
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                monthlyListenerCount =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.monthlyListenerCount
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                descriptionRuns = descriptionRuns,
                isSubscribed = isSubscribed,
            )
        }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            val sectionContent =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()

            val gridRenderer = sectionContent?.gridRenderer
            val musicCarouselShelfRenderer = sectionContent?.musicCarouselShelfRenderer
            val musicPlaylistShelfRenderer = sectionContent?.musicPlaylistShelfRenderer
            val musicShelfRenderer = sectionContent?.musicShelfRenderer

            when {
                gridRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            gridRenderer.header
                                ?.gridHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                .orEmpty(),
                        items =
                            gridRenderer.items.mapNotNull {
                                it.musicTwoRowItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                                }
                            },
                        continuation = gridRenderer.continuations?.getContinuation(),
                    )
                }

                musicCarouselShelfRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            musicCarouselShelfRenderer.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                .orEmpty(),
                        items =
                            musicCarouselShelfRenderer.contents.mapNotNull { content ->
                                content.musicTwoRowItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                                } ?: content.musicResponsiveListItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(renderer)
                                }
                            },
                        continuation = null,
                    )
                }

                musicShelfRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            musicShelfRenderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text
                                ?: "",
                        items =
                            musicShelfRenderer.contents?.getItems()?.mapNotNull {
                                ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                            } ?: emptyList(),
                        continuation = musicShelfRenderer.continuations?.getContinuation(),
                    )
                }

                else -> {
                    ArtistItemsPage(
                        title =
                            response.header
                                ?.musicHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: "",
                        items =
                            musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                                ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                            } ?: emptyList(),
                        continuation = musicPlaylistShelfRenderer?.contents?.getContinuation(),
                    )
                }
            }
        }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()

            when {
                response.continuationContents?.gridContinuation != null -> {
                    val gridContinuation = response.continuationContents.gridContinuation
                    val items =
                        gridContinuation.items.mapNotNull {
                            it.musicTwoRowItemRenderer?.let { renderer ->
                                ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                            }
                        }
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else gridContinuation.continuations?.getContinuation(),
                    )
                }

                response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                    val musicPlaylistShelfContinuation = response.continuationContents.musicPlaylistShelfContinuation
                    val items =
                        musicPlaylistShelfContinuation.contents.getItems().mapNotNull {
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                        }
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else musicPlaylistShelfContinuation.continuations?.getContinuation(),
                    )
                }

                else -> {
                    val continuationItems =
                        response.onResponseReceivedActions
                            ?.firstOrNull()
                            ?.appendContinuationItemsAction
                            ?.continuationItems
                    val items =
                        continuationItems?.getItems()?.mapNotNull {
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                        } ?: emptyList()
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else continuationItems?.getContinuation(),
                    )
                }
            }
        }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "VL$playlistId",
                        setLogin = true,
                    ).body<BrowseResponse>()
            val base =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
            val header =
                base?.musicResponsiveHeaderRenderer
                    ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

            val editable = base?.musicEditablePlaylistDetailHeaderRenderer != null

            val description: String? =
                header?.description?.musicDescriptionShelfRenderer?.description?.runs?.joinToString("") { it.text }
                    ?: base?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header?.musicDetailHeaderRenderer
                        ?.description?.runs?.joinToString("") { it.text }
                    ?: response.header?.musicDetailHeaderRenderer
                        ?.description?.runs?.joinToString("") { it.text }

            val author: Artist? = run {
                val fromStrapline = header?.straplineTextOne?.runs
                    ?.firstOrNull()
                    ?.let { Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId) }
                if (fromStrapline != null) return@run fromStrapline

                val detailSubtitle =
                    base
                        ?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header
                        ?.musicDetailHeaderRenderer
                        ?.subtitle
                        ?.runs
                        ?: response.header
                            ?.musicDetailHeaderRenderer
                            ?.subtitle
                            ?.runs
                val fromDetail = PageHelper.extractArtists(detailSubtitle).firstOrNull()
                if (fromDetail != null) return@run fromDetail

                val fromHeaderSubtitle = PageHelper.extractArtists(header?.subtitle?.runs).firstOrNull()
                if (fromHeaderSubtitle != null) return@run fromHeaderSubtitle

                val facepile = header?.facepile?.avatarStackViewModel
                if (facepile != null) {
                    val name = facepile.text?.content
                    val browseId = facepile.rendererContext?.commandContext?.onTap?.innertubeCommand?.browseEndpoint?.browseId
                    if (name != null) return@run Artist(name = name, id = browseId)
                }

                val fromMusicHeaderStrapline = response.header?.musicHeaderRenderer
                    ?.straplineTextOne?.runs?.firstOrNull()
                    ?.let { Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId) }
                if (fromMusicHeaderStrapline != null) return@run fromMusicHeaderStrapline

                PlaylistPage.ownerFromDetailSubtitle(detailSubtitle)
            }

            val authorAvatarUrl: String? = header?.facepile
                ?.avatarStackViewModel?.avatars?.firstOrNull()
                ?.avatarViewModel?.image?.sources?.firstOrNull()
                ?.url

            PlaylistPage(
                playlist =
                    PlaylistItem(
                        id = playlistId,
                        title =
                            header?.title?.runs?.firstOrNull()?.text
                                ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                                ?: "",
                        author = author,
                        songCountText =
                            (header?.secondSubtitle
                                ?: response.header?.musicHeaderRenderer?.secondSubtitle)
                                ?.runs
                                ?.findLast {
                                    it.text.any { c -> c.isDigit() } &&
                                        !it.text.contains("view", ignoreCase = true) &&
                                        !it.text.contains("hour", ignoreCase = true) &&
                                        !it.text.contains("minute", ignoreCase = true)
                                }?.text,
                        thumbnail =
                            header?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url
                                ?: response.header
                                    ?.musicHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.thumbnails
                                    ?.lastOrNull()
                                    ?.url
                                ?: "",
                        playEndpoint = null,
                        shuffleEndpoint =
                            header?.buttons?.lastOrNull()
                                ?.menuRenderer?.items?.firstOrNull()
                                ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: response.header?.musicHeaderRenderer?.buttons?.lastOrNull()
                                    ?.menuRenderer?.items?.firstOrNull()
                                    ?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        radioEndpoint =
                            header?.buttons?.getOrNull(2)
                                ?.menuRenderer?.items?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                                ?: response.header?.musicHeaderRenderer?.buttons?.getOrNull(2)
                                    ?.menuRenderer?.items?.find {
                                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        isEditable = editable,
                        description = description,
                        authorAvatarUrl = authorAvatarUrl,
                    ),
                songs = run {
                    val twoColShelf =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                    val twoColContents =
                        twoColShelf?.musicPlaylistShelfRenderer?.contents
                            ?: twoColShelf?.musicShelfRenderer?.contents
                    val singleColShelf =
                        response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                    val singleColContents =
                        singleColShelf?.musicPlaylistShelfRenderer?.contents
                            ?: singleColShelf?.musicShelfRenderer?.contents
                    (twoColContents ?: singleColContents)
                        ?.getItems()
                        ?.mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it) }
                        ?: emptyList()
                },
                songsContinuation = run {
                    val twoColShelf =
                        response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                    val twoColContents =
                        twoColShelf?.musicPlaylistShelfRenderer?.contents
                            ?: twoColShelf?.musicShelfRenderer?.contents
                    val twoColContinuations =
                        twoColShelf?.musicPlaylistShelfRenderer?.continuations
                            ?: twoColShelf?.musicShelfRenderer?.continuations
                    val singleColShelf =
                        response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                    val singleColContents =
                        singleColShelf?.musicPlaylistShelfRenderer?.contents
                            ?: singleColShelf?.musicShelfRenderer?.contents
                    val singleColContinuations =
                        singleColShelf?.musicPlaylistShelfRenderer?.continuations
                            ?: singleColShelf?.musicShelfRenderer?.continuations
                    val mergedContents = twoColContents ?: singleColContents
                    val mergedContinuations = twoColContinuations ?: singleColContinuations
                    mergedContents?.getContinuation() ?: mergedContinuations?.getContinuation()
                },
                continuation =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val mainContents: List<MusicShelfRenderer.Content> =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.mapNotNull { content: SectionListRenderer.Content ->
                        content.musicPlaylistShelfRenderer?.contents
                            ?: content.musicShelfRenderer?.contents
                    }
                    ?.flatten()
                    ?: emptyList()

            val shelfContents: List<MusicShelfRenderer.Content> =
                response.continuationContents?.musicPlaylistShelfContinuation?.contents ?: emptyList()

            val musicShelfContinuationContents: List<MusicShelfRenderer.Content> =
                response.continuationContents?.musicShelfContinuation?.contents ?: emptyList()

            val appendedContents: List<MusicShelfRenderer.Content> =
                response.onResponseReceivedActions
                    .orEmpty()
                    .flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }

            val allContents = mainContents + shelfContents + musicShelfContinuationContents + appendedContents

            val songs =
                allContents
                    .mapNotNull { content: MusicShelfRenderer.Content -> content.musicResponsiveListItemRenderer }
                    .mapNotNull { renderer -> PlaylistPage.fromMusicResponsiveListItemRenderer(renderer) }

            val nextContinuation =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.continuations
                    ?.getContinuation()
                    ?: response.continuationContents
                        ?.musicPlaylistShelfContinuation
                        ?.continuations
                        ?.getContinuation()
                    ?: response.continuationContents
                        ?.musicShelfContinuation
                        ?.continuations
                        ?.getContinuation()
                    ?: appendedContents.getContinuation()

            PlaylistContinuationPage(
                songs = songs,
                continuation = nextContinuation,
            )
        }

    suspend fun podcast(podcastId: String): Result<PodcastPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = podcastId,
                        setLogin = true,
                    ).body<BrowseResponse>()

            var header =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
                    ?.musicResponsiveHeaderRenderer

            if (header == null) {
                header =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
            }

            val subscribeToggle =
                header
                    ?.buttons
                    ?.flatMap { button -> button.menuRenderer?.items ?: emptyList() }
                    ?.find { it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE" }
                    ?.toggleMenuServiceItemRenderer

            val channelId = subscribeToggle?.defaultServiceEndpoint?.subscribeEndpoint?.channelIds?.firstOrNull()
            val isChannelSubscribed = subscribeToggle?.isSelected == true

            var libraryTokens =
                header
                    ?.buttons
                    ?.flatMap { button -> button.menuRenderer?.items ?: emptyList() }
                    ?.let { menuItems -> PageHelper.extractLibraryTokensFromMenuItems(menuItems) }

            if (libraryTokens?.addToken == null && libraryTokens?.removeToken == null) {
                header?.buttons?.forEach { button ->
                    button.toggleButtonRenderer?.let { toggle ->
                        val iconType = toggle.defaultIcon?.iconType
                        if (iconType != null && PageHelper.isLibraryIcon(iconType)) {
                            val defaultToken = toggle.defaultServiceEndpoint?.feedbackEndpoint?.feedbackToken
                            val toggledToken = toggle.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken
                            libraryTokens =
                                if (PageHelper.isAddLibraryIcon(iconType)) {
                                    PageHelper.LibraryFeedbackTokens(defaultToken, toggledToken)
                                } else {
                                    PageHelper.LibraryFeedbackTokens(toggledToken, defaultToken)
                                }
                        }
                    }
                }
            }

            val podcastItem =
                PodcastItem(
                    id = podcastId,
                    title = header?.title?.runs?.firstOrNull()?.text ?: "",
                    author =
                        header?.straplineTextOne?.runs?.firstOrNull()?.let {
                            Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                        },
                    episodeCountText = header?.secondSubtitle?.runs?.firstOrNull()?.text,
                    thumbnail = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url,
                    playEndpoint =
                        header
                            ?.buttons
                            ?.find {
                                it.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.icon?.iconType == "PLAY_ARROW"
                            }?.menuRenderer
                            ?.items
                            ?.firstOrNull()
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    shuffleEndpoint =
                        header
                            ?.buttons
                            ?.find {
                                it.menuRenderer?.items?.any { item -> item.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" } == true
                            }?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    libraryAddToken = libraryTokens?.addToken,
                    libraryRemoveToken = libraryTokens?.removeToken,
                    channelId = channelId,
                )

            val secondaryContents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents
            var episodeContents = secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents
            if (episodeContents == null) {
                episodeContents = secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents
            }
            if (episodeContents == null) {
                episodeContents =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.find { it.musicShelfRenderer != null }
                        ?.musicShelfRenderer
                        ?.contents
            }

            val multiRowItems = episodeContents?.mapNotNull { it.musicMultiRowListItemRenderer } ?: emptyList()
            val episodes = multiRowItems.mapNotNull { renderer -> PodcastPage.fromMusicMultiRowListItemRenderer(renderer, podcastItem) }

            PodcastPage(
                podcast = podcastItem,
                episodes = episodes,
                continuation =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicShelfRenderer
                        ?.continuations
                        ?.getContinuation()
                        ?: response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.find { it.musicShelfRenderer != null }
                            ?.musicShelfRenderer
                            ?.continuations
                            ?.getContinuation(),
                isChannelSubscribed = isChannelSubscribed,
            )
        }

    suspend fun home(
        continuation: String? = null,
        params: String? = null,
    ): Result<HomePage> =
        runCatching {
            if (continuation != null) {
                return@runCatching homeContinuation(continuation).getOrThrow()
            }

            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_home", params = params).body<BrowseResponse>()
            val cont =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.continuations
                    ?.getContinuation()
            val sectionListRender =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
            val carousels = sectionListRender?.contents?.mapNotNull { it.musicCarouselShelfRenderer } ?: emptyList()
            val sections = carousels.mapNotNull { HomePage.Section.fromMusicCarouselShelfRenderer(it) }.toMutableList()
            val chips = sectionListRender?.header?.chipCloudRenderer?.chips?.mapNotNull { HomePage.Chip.fromChipCloudChipRenderer(it) }
            HomePage(chips, sections, cont)
        }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
            val cont = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
            HomePage(
                null,
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.mapNotNull { it.musicCarouselShelfRenderer }
                    ?.mapNotNull { HomePage.Section.fromMusicCarouselShelfRenderer(it) }
                    .orEmpty(),
                cont,
            )
        }

    suspend fun explore(): Result<ExplorePage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore").body<BrowseResponse>()
            ExplorePage(
                newReleaseAlbums =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.find {
                            it.musicCarouselShelfRenderer
                                ?.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.moreContentButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId == "FEmusic_new_releases_albums"
                        }?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull { it.musicTwoRowItemRenderer }
                        ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                        .orEmpty(),
                moodAndGenres =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.find {
                            it.musicCarouselShelfRenderer
                                ?.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.moreContentButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.browseEndpoint
                                ?.browseId == "FEmusic_moods_and_genres"
                        }?.musicCarouselShelfRenderer
                        ?.contents
                        ?.mapNotNull { it.musicNavigationButtonRenderer }
                        ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                        .orEmpty(),
            )
        }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_new_releases_albums").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.gridRenderer
                ?.items
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty()
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents!!
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
        }

    suspend fun browse(
        browseId: String,
        params: String?,
    ): Result<BrowseResult> =
        runCatching {
            val needsLogin = browseId.startsWith("FEmusic_library") || browseId == "VLSE" || browseId == "VLRDPN"
            val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params, setLogin = needsLogin).body<BrowseResponse>()
            val sectionContents =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
            BrowseResult(
                title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                items =
                    sectionContents
                        ?.mapNotNull { content ->
                            when {
                                content.gridRenderer != null -> {
                                    BrowseResult.Item(
                                        title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                                        items =
                                            content.gridRenderer.items
                                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                                .mapNotNull { renderer ->
                                                    LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                                                        ?: RelatedPage.fromMusicTwoRowItemRenderer(renderer)
                                                },
                                    )
                                }

                                content.musicCarouselShelfRenderer != null -> {
                                    val carouselItems = mutableListOf<YTItem>()
                                    for (carouselContent in content.musicCarouselShelfRenderer.contents) {
                                        val item = carouselContent.musicTwoRowItemRenderer?.let { renderer ->
                                            LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                                                ?: RelatedPage.fromMusicTwoRowItemRenderer(renderer)
                                        } ?: carouselContent.musicMultiRowListItemRenderer?.let { renderer ->
                                            PodcastPage.fromMusicMultiRowListItemRenderer(renderer)
                                        } ?: carouselContent.musicResponsiveListItemRenderer?.let { renderer ->
                                            LibraryPage.fromMusicResponsiveListItemRenderer(renderer)
                                                ?: RelatedPage.fromMusicResponsiveListItemRenderer(renderer)
                                        }
                                        if (item != null) {
                                            carouselItems.add(item)
                                        }
                                    }
                                    BrowseResult.Item(
                                        title = content.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                                        items =
                                            content.musicCarouselShelfRenderer.contents.mapNotNull { childContent ->
                                                childContent.musicTwoRowItemRenderer?.let { renderer ->
                                                    LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                                                        ?: RelatedPage.fromMusicTwoRowItemRenderer(renderer)
                                                } ?: childContent.musicMultiRowListItemRenderer?.let { renderer ->
                                                    PodcastPage.fromMusicMultiRowListItemRenderer(renderer)
                                                } ?: childContent.musicResponsiveListItemRenderer?.let { renderer ->
                                                    LibraryPage.fromMusicResponsiveListItemRenderer(renderer)
                                                        ?: RelatedPage.fromMusicResponsiveListItemRenderer(renderer)
                                                }
                                            },
                                    )
                                }

                                content.musicShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title = content.musicShelfRenderer.title?.runs?.firstOrNull()?.text,
                                        items =
                                            content.musicShelfRenderer.contents
                                                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                                                ?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
                                                ?: emptyList(),
                                    )
                                }

                                content.musicPlaylistShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title = null,
                                        items =
                                            content.musicPlaylistShelfRenderer.contents
                                                .getItems()
                                                .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) },
                                    )
                                }

                                else -> null
                            }
                        }.orEmpty(),
            )
        }

    suspend fun library(
        browseId: String,
        tabIndex: Int = 0,
    ): Result<LibraryPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = browseId,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val isUploaded = browseId.startsWith("FEmusic_library_privately_owned")
            val tab =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.getOrNull(tabIndex)
                    ?: response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.tabs
                        ?.getOrNull(tabIndex)
            val contents =
                tab
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull {
                        it.gridRenderer != null ||
                            it.musicShelfRenderer != null ||
                            it.musicPlaylistShelfRenderer != null ||
                            it.itemSectionRenderer?.contents.orEmpty().any { child ->
                                child.gridRenderer != null ||
                                    child.musicShelfRenderer != null ||
                                    child.musicPlaylistShelfRenderer != null
                            }
                    }
            val nestedContents = contents?.itemSectionRenderer?.contents.orEmpty()
            val gridRenderer = contents?.gridRenderer
                ?: nestedContents.firstNotNullOfOrNull { it.gridRenderer }
            val playlistShelfRenderer = contents?.musicPlaylistShelfRenderer
                ?: nestedContents.firstNotNullOfOrNull { it.musicPlaylistShelfRenderer }
            val musicShelfRenderer = contents?.musicShelfRenderer
                ?: nestedContents.firstNotNullOfOrNull { it.musicShelfRenderer }

            when {
                gridRenderer != null -> {
                    val gridItems = gridRenderer.items
                    val parsedItems =
                        gridItems
                            .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                            .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                    LibraryPage(
                        items = parsedItems,
                        continuation = gridRenderer.continuations?.getContinuation(),
                    )
                }

                playlistShelfRenderer != null -> {
                    LibraryPage(
                        items =
                            playlistShelfRenderer.contents
                                .getItems()
                                .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded) },
                        continuation = playlistShelfRenderer.continuations?.getContinuation(),
                    )
                }

                musicShelfRenderer != null -> {
                    val listItemRenderers = musicShelfRenderer.contents.orEmpty()
                        .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                    val parsedItems =
                        listItemRenderers.mapNotNull { renderer ->
                            LibraryPage.fromMusicResponsiveListItemRenderer(renderer, isUploaded)
                        }
                    LibraryPage(
                        items = parsedItems,
                        continuation = musicShelfRenderer.continuations?.getContinuation(),
                    )
                }

                else -> LibraryPage(items = emptyList(), continuation = null)
            }.copy(isUploaded = isUploaded)
        }

    suspend fun libraryContinuation(
        continuation: String,
        isUploaded: Boolean = false,
    ) = runCatching {
        val response =
            innerTube
                .browse(
                    client = WEB_REMIX,
                    continuation = continuation,
                    setLogin = true,
                ).body<BrowseResponse>()

        val contents = response.continuationContents

        when {
            contents?.gridContinuation != null -> {
                LibraryContinuationPage(
                    items =
                        contents.gridContinuation.items
                            .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                            .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                    continuation = contents.gridContinuation.continuations?.getContinuation(),
                )
            }

            else -> {
                LibraryContinuationPage(
                    items =
                        contents
                            ?.musicShelfContinuation
                            ?.contents!!
                            .mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                            .mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded) },
                    continuation = contents.musicShelfContinuation.continuations?.getContinuation(),
                )
            }
        }
    }

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "FEmusic_charts",
                        params = "ggMGCgQIgAQ%3D",
                        continuation = continuation,
                    ).body<BrowseResponse>()

            val sections = mutableListOf<ChartsPage.ChartSection>()

            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { content ->
                    content.musicCarouselShelfRenderer?.let { renderer ->
                        val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return@forEach
                        val items = renderer.contents.mapNotNull { item ->
                            when {
                                item.musicResponsiveListItemRenderer != null -> convertToChartItem(item.musicResponsiveListItemRenderer)
                                item.musicTwoRowItemRenderer != null -> convertMusicTwoRowItem(item.musicTwoRowItemRenderer)
                                else -> null
                            }
                        }

                        if (items.isNotEmpty()) {
                            sections.add(
                                ChartsPage.ChartSection(
                                    title = title,
                                    items = items,
                                    chartType = determineChartType(title),
                                ),
                            )
                        }
                    }

                    content.gridRenderer?.let { renderer ->
                        val title = renderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: return@let
                        val items = renderer.items.mapNotNull { item ->
                            item.musicTwoRowItemRenderer?.let { convertMusicTwoRowItem(it) }
                        }

                        if (items.isNotEmpty()) {
                            sections.add(
                                ChartsPage.ChartSection(
                                    title = title,
                                    items = items,
                                    chartType = ChartsPage.ChartType.NEW_RELEASES,
                                ),
                            )
                        }
                    }
                }

            ChartsPage(
                sections = sections,
                continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation(),
            )
        }

    private fun determineChartType(title: String): ChartsPage.ChartType =
        when {
            title.contains("Trending", ignoreCase = true) -> ChartsPage.ChartType.TRENDING
            title.contains("Top", ignoreCase = true) -> ChartsPage.ChartType.TOP
            else -> ChartsPage.ChartType.GENRE
        }

    private fun convertToChartItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        return try {
            when {
                renderer.flexColumns.size >= 3 && renderer.videoId != null -> {
                    val firstColumn = renderer.flexColumns.getOrNull(0)?.musicResponsiveListItemFlexColumnRenderer?.text ?: return null
                    val secondColumn = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text ?: return null
                    val titleRun = firstColumn.runs?.firstOrNull() ?: return null
                    val title = titleRun.text.takeIf { it.isNotBlank() } ?: return null
                    val artists = PageHelper.extractArtists(secondColumn.runs)
                    val thirdColumn = renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text

                    SongItem(
                        id = renderer.videoId!!,
                        title = title,
                        artists = artists,
                        thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: return null,
                        musicVideoType = renderer.musicVideoType,
                        explicit = renderer.badges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true,
                        chartPosition = thirdColumn?.runs?.firstOrNull()?.text?.toIntOrNull(),
                        chartChange = thirdColumn?.runs?.getOrNull(1)?.text,
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun convertMusicTwoRowItem(renderer: MusicTwoRowItemRenderer): YTItem? {
        return try {
            when {
                renderer.isSong -> {
                    val subtitle = renderer.subtitle?.runs ?: return null
                    val title = renderer.title.runs?.firstOrNull()?.text ?: return null
                    val artists = PageHelper.extractArtists(subtitle)
                    val videoId = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null

                    SongItem(
                        id = videoId,
                        title = title,
                        artists = artists,
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        musicVideoType = renderer.musicVideoType,
                        explicit = renderer.subtitleBadges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true,
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchPlaylistEndpoint?.playlistId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        artists = PageHelper.extractArtists(renderer.subtitle?.runs),
                        year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.getThumbnailUrl() ?: return null,
                        explicit = renderer.subtitleBadges?.any { it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE" } == true,
                    )
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun musicHistory() =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "FEmusic_history", setLogin = true).body<BrowseResponse>()
            HistoryPage(
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull { it.musicShelfRenderer?.let(HistoryPage::fromMusicShelfRenderer) },
            )
        }

    suspend fun likeVideo(videoId: String, like: Boolean) =
        runCatching {
            if (like) innerTube.likeVideo(WEB_REMIX, videoId) else innerTube.unlikeVideo(WEB_REMIX, videoId)
        }

    suspend fun likePlaylist(playlistId: String, like: Boolean) =
        runCatching {
            if (like) innerTube.likePlaylist(WEB_REMIX, playlistId) else innerTube.unlikePlaylist(WEB_REMIX, playlistId)
        }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean, params: String? = null) =
        runCatching {
            val subscribeParams = params ?: "EgIIAhgA"
            if (subscribe) {
                innerTube.subscribeChannel(WEB_REMIX, channelId, subscribeParams)
            } else {
                innerTube.unsubscribeChannel(WEB_REMIX, channelId, subscribeParams)
            }
        }

    suspend fun savePodcast(podcastId: String, save: Boolean) =
        runCatching {
            val playlistId = podcastId.removePrefix("MPSP")
            if (save) innerTube.likePlaylist(WEB_REMIX, playlistId) else innerTube.unlikePlaylist(WEB_REMIX, playlistId)
        }

    suspend fun addEpisodeToSavedEpisodes(videoId: String) =
        runCatching { innerTube.addToPlaylist(WEB_REMIX, "SE", videoId) }

    suspend fun removeEpisodeFromSavedEpisodes(videoId: String, setVideoId: String) =
        runCatching { innerTube.removeFromPlaylist(WEB_REMIX, "SE", videoId, setVideoId) }

    suspend fun libraryPodcastChannels(): Result<LibraryPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "FEmusic_library_non_music_audio_channels_list", setLogin = true).body<BrowseResponse>()
            val contentList = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents ?: emptyList()
            val items = contentList.flatMap { content ->
                when {
                    content.gridRenderer != null -> content.gridRenderer.items.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer).mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                    content.musicShelfRenderer != null -> content.musicShelfRenderer.contents?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList()
                    content.musicCarouselShelfRenderer != null -> content.musicCarouselShelfRenderer.contents.mapNotNull { childContent ->
                        childContent.musicTwoRowItemRenderer?.let { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                            ?: childContent.musicMultiRowListItemRenderer?.let { PodcastPage.fromMusicMultiRowListItemRenderer(it) }
                            ?: childContent.musicResponsiveListItemRenderer?.let { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
                    }
                    else -> emptyList()
                }
            }
            LibraryPage(items = items, continuation = null)
        }

    suspend fun libraryPodcastEpisodes(): Result<LibraryPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "FEmusic_library_non_music_audio_list", setLogin = true).body<BrowseResponse>()
            val contents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            val items = when {
                contents?.gridRenderer != null -> contents.gridRenderer.items.mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer).mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) }
                contents?.musicShelfRenderer != null -> contents.musicShelfRenderer.contents?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList()
                else -> emptyList()
            }
            LibraryPage(items = items, continuation = null)
        }

    suspend fun savedPodcastShows(): Result<List<PodcastItem>> =
        runCatching { libraryPodcastEpisodes().getOrThrow().items.filterIsInstance<PodcastItem>() }

    suspend fun newEpisodes(): Result<List<SongItem>> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "VLRDPN", setLogin = true).body<BrowseResponse>()
            val twoColumn = response.contents?.twoColumnBrowseResultsRenderer
            val sections = mutableListOf<SectionListRenderer.Content>()

            twoColumn?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.let { sections.addAll(it) }
            twoColumn?.secondaryContents?.sectionListRenderer?.contents?.let { secContents ->
                sections.addAll(secContents.map { secContent ->
                    SectionListRenderer.Content(
                        musicCarouselShelfRenderer = null,
                        musicShelfRenderer = secContent.musicShelfRenderer,
                        musicCardShelfRenderer = null,
                        musicPlaylistShelfRenderer = secContent.musicPlaylistShelfRenderer,
                        musicDescriptionShelfRenderer = null,
                        musicResponsiveHeaderRenderer = null,
                        musicEditablePlaylistDetailHeaderRenderer = null,
                        gridRenderer = null,
                        itemSectionRenderer = null,
                    )
                })
            }

            val episodesList = mutableListOf<SongItem>()

            fun processShelf(shelf: MusicShelfRenderer) {
                shelf.contents?.mapNotNull { it.musicMultiRowListItemRenderer }?.forEach { renderer ->
                    if (renderer.onTap?.watchEndpoint?.videoId == null) return@forEach
                    val title = renderer.title?.runs?.firstOrNull()?.text ?: return@forEach
                    val subtitleGroups = renderer.subtitle?.runs?.splitBySeparator()
                    val duration = subtitleGroups?.lastOrNull { group -> group.firstOrNull()?.text?.parseTime() != null }?.firstOrNull()?.text?.parseTime()
                    val artists = PodcastPage.extractPodcastArtists(renderer, shelf.title?.runs)

                    episodesList.add(
                        SongItem(
                            id = renderer.onTap.watchEndpoint.videoId,
                            title = title,
                            artists = artists,
                            album = null,
                            duration = duration,
                            thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: "",
                            isEpisode = true,
                        ),
                    )
                }
            }

            sections.forEach { section ->
                section.musicShelfRenderer?.let { shelf -> processShelf(shelf) }
                section.musicCarouselShelfRenderer?.let { carousel ->
                    carousel.contents.forEach { carouselContent ->
                        carouselContent.musicMultiRowListItemRenderer?.let { renderer ->
                            if (renderer.onTap?.watchEndpoint?.videoId == null) return@let
                            val title = renderer.title?.runs?.firstOrNull()?.text ?: return@let
                            val subtitleGroups = renderer.subtitle?.runs?.splitBySeparator()
                            val duration = subtitleGroups?.lastOrNull { group -> group.firstOrNull()?.text?.parseTime() != null }?.firstOrNull()?.text?.parseTime()
                            val artists = PodcastPage.extractPodcastArtists(renderer, carousel.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs)

                            episodesList.add(
                                SongItem(
                                    id = renderer.onTap.watchEndpoint.videoId,
                                    title = title,
                                    artists = artists,
                                    album = null,
                                    duration = duration,
                                    thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: "",
                                    isEpisode = true,
                                ),
                            )
                        }
                    }
                }
            }

            coroutineScope {
                episodesList.map { episode ->
                    async {
                        if (episode.artists.isNotEmpty()) return@async episode
                        val mediaInfo = innerTube.getMediaInfo(episode.id).getOrNull()
                        if (mediaInfo?.author != null) {
                            episode.copy(artists = listOf(Artist(mediaInfo.author, mediaInfo.authorId)))
                        } else episode
                    }
                }.awaitAll()
            }
        }

    suspend fun newEpisodesPlaylistInfo(): Result<PlaylistItem> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "VLRDPN", setLogin = true).body<BrowseResponse>()
            val thumbnail: String? =
                response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicVisualHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url
                    ?: response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicMultiRowListItemRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()

            val title =
                response.header?.musicImmersiveHeaderRenderer?.title?.runs?.joinToString("") { it.text }
                    ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.joinToString("") { it.text }
                    ?: "New Episodes"

            PlaylistItem(
                id = "RDPN",
                title = title,
                author = null,
                songCountText = null,
                thumbnail = thumbnail,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
            )
        }

    suspend fun episodesForLater(): Result<List<SongItem>> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "VLSE", setLogin = true).body<BrowseResponse>()
            val contents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()
            val shelfContents = contents?.musicPlaylistShelfRenderer?.contents ?: contents?.musicShelfRenderer?.contents

            shelfContents
                ?.mapNotNull { it.musicResponsiveListItemRenderer }
                ?.mapNotNull { renderer ->
                    val videoId = renderer.videoId ?: return@mapNotNull null
                    val setVideoId = renderer.playlistSetVideoId
                    val title = renderer.flexColumns.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text ?: return@mapNotNull null
                    val subtitleRuns = renderer.flexColumns.getOrNull(1)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs
                    val subtitleGroups = subtitleRuns?.splitBySeparator()

                    val duration =
                        subtitleGroups
                            ?.drop(1)
                            ?.firstOrNull { group -> group.firstOrNull()?.text?.parseTime() != null }
                            ?.firstOrNull()?.text?.parseTime()
                            ?: renderer.fixedColumns?.firstOrNull()?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.text?.parseTime()

                    SongItem(
                        id = videoId,
                        title = title,
                        artists = PodcastPage.extractPodcastByline(subtitleRuns),
                        album = null,
                        duration = duration,
                        thumbnail = renderer.thumbnail?.getThumbnailUrl() ?: "",
                        setVideoId = setVideoId,
                        isEpisode = true,
                    )
                } ?: emptyList()
        }

    suspend fun getChannelId(browseId: String): String {
        artist(browseId).onSuccess { return it.artist.channelId ?: "" }
        return ""
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) =
        runCatching { innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId) }

    suspend fun addPlaylistToPlaylist(playlistId: String, addPlaylistId: String) =
        runCatching { innerTube.addPlaylistToPlaylist(WEB_REMIX, playlistId, addPlaylistId) }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String) =
        runCatching { innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId) }

    suspend fun moveSongPlaylist(playlistId: String, setVideoId: String, successorSetVideoId: String?) =
        runCatching { innerTube.moveSongPlaylist(WEB_REMIX, playlistId, setVideoId, successorSetVideoId) }

    suspend fun createPlaylist(title: String) =
        innerTube.createPlaylist(WEB_REMIX, title).body<CreatePlaylistResponse>().playlistId

    suspend fun renamePlaylist(playlistId: String, name: String) =
        runCatching { innerTube.renamePlaylist(WEB_REMIX, playlistId, name) }

    suspend fun uploadCustomThumbnailLink(playlistId: String, image: ByteArray) =
        runCatching {
            innerTube
                .setPlaylistThumbnail(WEB_REMIX, playlistId, image)
                .body<EditPlaylistResponse>()
                .newHeader
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicResponsiveHeaderRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.getThumbnailUrl()
        }

    suspend fun removeThumbnailPlaylist(playlistId: String) =
        runCatching {
            innerTube
                .removePlaylistThumbnail(WEB_REMIX, playlistId)
                .body<EditPlaylistResponse>()
                .newHeader
                ?.musicEditablePlaylistDetailHeaderRenderer
                ?.header
                ?.musicResponsiveHeaderRenderer
                ?.thumbnail
                ?.musicThumbnailRenderer
                ?.getThumbnailUrl()
        }

    suspend fun deletePlaylist(playlistId: String) =
        runCatching { innerTube.deletePlaylist(WEB_REMIX, playlistId) }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        client: YouTubeClient,
        signatureTimestamp: Int? = null,
        poToken: String? = null,
    ): Result<PlayerResponse> =
        runCatching {
            innerTube.player(client, videoId, playlistId, signatureTimestamp, poToken).body<PlayerResponse>()
        }

    suspend fun registerPlayback(
        videoId: String,
        clientPlaybackNonce: String,
        isHistoryEnabled: () -> Boolean,
    ) = runCatching {
        innerTube.registerPlayback(
            videoId = videoId,
            isHistoryEnabled = isHistoryEnabled,
            cpn = clientPlaybackNonce,
        )
    }.onFailure {
        if (it is CancellationException) throw it
    }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                innerTube
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()
            val playlistPanelRenderer =
                response.continuationContents?.playlistPanelContinuation
                    ?: response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.get(0)
                        ?.tabRenderer
                        ?.content
                        ?.musicQueueRenderer
                        ?.content
                        ?.playlistPanelRenderer!!
            val title =
                response.contents.singleColumnMusicWatchNextResultsRenderer
                    ?.tabbedRenderer
                    ?.watchNextTabbedResultsRenderer
                    ?.tabs
                    ?.get(0)
                    ?.tabRenderer
                    ?.content
                    ?.musicQueueRenderer
                    ?.header
                    ?.musicQueueHeaderRenderer
                    ?.subtitle
                    ?.runs
                    ?.firstOrNull()
                    ?.text
            val items =
                playlistPanelRenderer.contents.mapNotNull { content ->
                    content.playlistPanelVideoRenderer
                        ?.let(NextPage::fromPlaylistPanelVideoRenderer)
                        ?.let { it to content.playlistPanelVideoRenderer.selected }
                }
            val songs = items.map { it.first }
            val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

            playlistPanelRenderer.contents
                .lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
                ?.let { watchPlaylistEndpoint ->
                    return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                        result.copy(
                            title = title,
                            items = songs + result.items,
                            lyricsEndpoint =
                                response.contents.singleColumnMusicWatchNextResultsRenderer
                                    ?.tabbedRenderer
                                    ?.watchNextTabbedResultsRenderer
                                    ?.tabs
                                    ?.getOrNull(1)
                                    ?.tabRenderer
                                    ?.endpoint
                                    ?.browseEndpoint,
                            relatedEndpoint =
                                response.contents.singleColumnMusicWatchNextResultsRenderer
                                    ?.tabbedRenderer
                                    ?.watchNextTabbedResultsRenderer
                                    ?.tabs
                                    ?.getOrNull(2)
                                    ?.tabRenderer
                                    ?.endpoint
                                    ?.browseEndpoint,
                            currentIndex = currentIndex,
                            endpoint = watchPlaylistEndpoint,
                        )
                    }
                }
            NextResult(
                title = title,
                items = songs,
                currentIndex = currentIndex,
                lyricsEndpoint =
                    response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.getOrNull(1)
                        ?.tabRenderer
                        ?.endpoint
                        ?.browseEndpoint,
                relatedEndpoint =
                    response.contents.singleColumnMusicWatchNextResultsRenderer
                        ?.tabbedRenderer
                        ?.watchNextTabbedResultsRenderer
                        ?.tabs
                        ?.getOrNull(2)
                        ?.tabRenderer
                        ?.endpoint
                        ?.browseEndpoint,
                continuation = playlistPanelRenderer.continuations?.getContinuation(),
                endpoint = endpoint,
            )
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.joinToString(separator = "") { it.text }
        }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>()
            val songs = mutableListOf<SongItem>()
            val albums = mutableListOf<AlbumItem>()
            val artists = mutableListOf<ArtistItem>()
            val playlists = mutableListOf<PlaylistItem>()
            response.contents?.sectionListRenderer?.contents?.forEach { sectionContent ->
                sectionContent.musicCarouselShelfRenderer?.contents?.forEach { content ->
                    when (
                        val item =
                            content.musicResponsiveListItemRenderer?.let(RelatedPage.Companion::fromMusicResponsiveListItemRenderer)
                                ?: content.musicTwoRowItemRenderer?.let(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                    ) {
                        is SongItem -> {
                            if (content.musicResponsiveListItemRenderer
                                    ?.overlay
                                    ?.musicItemThumbnailOverlayRenderer
                                    ?.content
                                    ?.musicPlayButtonRenderer
                                    ?.playNavigationEndpoint
                                    ?.watchEndpoint
                                    ?.watchEndpointMusicSupportedConfigs
                                    ?.watchEndpointMusicConfig
                                    ?.musicVideoType == MUSIC_VIDEO_TYPE_ATV
                            ) {
                                songs.add(item)
                            }
                        }
                        is AlbumItem -> albums.add(item)
                        is ArtistItem -> artists.add(item)
                        is PlaylistItem -> playlists.add(item)
                        else -> {}
                    }
                }
            }
            RelatedPage(songs, albums, artists, playlists)
        }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> =
        runCatching {
            if (videoIds != null) {
                assert(videoIds.size <= MAX_GET_QUEUE_SIZE)
            }
            innerTube
                .getQueue(WEB_REMIX, videoIds, playlistId)
                .body<GetQueueResponse>()
                .queueDatas
                .mapNotNull {
                    it.content.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
                }
        }

    suspend fun transcript(videoId: String): Result<String> =
        runCatching {
            val response = innerTube.getTranscript(WEB, videoId).body<GetTranscriptResponse>()
            response.actions
                ?.firstOrNull()
                ?.updateEngagementPanelAction
                ?.content
                ?.transcriptRenderer
                ?.body
                ?.transcriptBodyRenderer
                ?.cueGroups
                ?.joinToString(separator = "\n") { group ->
                    val time = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.startOffsetMs
                    val text = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.cue.simpleText.trim('♪').trim(' ')
                    "[%02d:%02d.%03d]$text".format(time / 60000, (time / 1000) % 60, time % 1000)
                }!!
        }

    suspend fun visitorData(): Result<String> =
        runCatching {
            checkNotNull(innerTube.fetchFreshVisitorData()) { "YouTube did not return visitor data" }
        }

    suspend fun accountInfo(): Result<AccountInfo> =
        runCatching {
            innerTube
                .accountMenu(WEB_REMIX)
                .body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer
                .header
                ?.activeAccountHeaderRenderer
                ?.toAccountInfo()!!
        }

    suspend fun accountsList(): Result<List<YouTubeAccount>> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.accountsList().bodyAsText())
                .extractYouTubeAccounts()
        }

    suspend fun feedback(tokens: List<String>): Result<Boolean> =
        runCatching {
            innerTube
                .feedback(WEB_REMIX, tokens)
                .body<FeedbackResponse>()
                .feedbackResponses
                .all { it.isProcessed }
        }

    suspend fun addSongToLibrary(videoId: String): Result<Boolean> =
        runCatching {
            val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val song = nextResult.items.find { it.id == videoId } ?: throw Exception("Song not found in next response")
            val addToken = song.libraryAddToken ?: throw Exception("Add to library token not available")
            feedback(listOf(addToken)).getOrThrow()
        }

    suspend fun removeSongFromLibrary(videoId: String): Result<Boolean> =
        runCatching {
            val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val song = nextResult.items.find { it.id == videoId } ?: throw Exception("Song not found in next response")
            val removeToken = song.libraryRemoveToken ?: throw Exception("Remove from library token not available")
            feedback(listOf(removeToken)).getOrThrow()
        }

    suspend fun toggleSongLibrary(
        videoId: String,
        addToLibrary: Boolean,
    ): Result<Boolean> =
        runCatching {
            if (addToLibrary) {
                addSongToLibrary(videoId).getOrThrow()
            } else {
                removeSongFromLibrary(videoId).getOrThrow()
            }
        }

    suspend fun getMediaInfo(videoId: String): Result<MediaInfo> =
        runCatching {
            innerTube.getMediaInfo(videoId).getOrThrow()
        }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQJQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_EPISODE = SearchFilter("EgWKAQJYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_PROFILE = SearchFilter("EgWKAQJYAWoSEAUQCRADEAQQEBAVEAoQDhAR")
        }
    }

    const val MAX_GET_QUEUE_SIZE = 1000

    suspend fun uploadSong(
        filename: String,
        contentLength: Long,
        content: () -> InputStream,
        onProgress: ((Float) -> Unit)? = null,
    ): Result<Boolean> =
        runCatching {
            require(contentLength in 1 until MAX_UPLOAD_SIZE)
            onProgress?.invoke(0f)
            onProgress?.invoke(0.05f)
            val uploadResponse =
                innerTube.uploadSong(
                    filename = filename,
                    contentLength = contentLength,
                    content = {
                        UploadProgressInputStream(content(), contentLength) { progress ->
                            onProgress?.invoke(0.05f + progress * 0.95f)
                        }
                    },
                )

            val uploaded = uploadResponse.headers["X-Goog-Upload-Status"] == "final"
            if (uploaded) onProgress?.invoke(1f)
            uploaded
        }

    suspend fun deleteUploadedSong(entityId: String): Result<Boolean> =
        runCatching {
            innerTube.deletePrivatelyOwnedEntity(entityId)
            true
        }

    val SUPPORTED_UPLOAD_TYPES = listOf("mp3", "m4a", "wma", "flac", "ogg")
    const val MAX_UPLOAD_SIZE = 314572800L

    suspend fun resolveArtistIds(items: List<YTItem>): List<YTItem> {
        val missingNames = mutableSetOf<String>()
        for (item in items) {
            when (item) {
                is SongItem -> item.artists.filter { it.id == null }.forEach { missingNames.add(it.name) }
                is AlbumItem -> item.artists?.filter { it.id == null }?.forEach { missingNames.add(it.name) }
                is PlaylistItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                is EpisodeItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                is PodcastItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                else -> {}
            }
        }

        if (missingNames.isEmpty()) return items

        val resolved = coroutineScope {
            val semaphore = kotlinx.coroutines.sync.Semaphore(8)
            missingNames.map { name ->
                async {
                    semaphore.acquire()
                    try {
                        val searchResult = search(name, SearchFilter.FILTER_ARTIST).getOrNull()
                        val normalizedName = name.trim()
                        val artistId = searchResult?.items
                            ?.filterIsInstance<ArtistItem>()
                            ?.firstOrNull { candidate ->
                                candidate.title.trim().equals(normalizedName, ignoreCase = true)
                            }?.id
                        if (artistId != null) name to artistId else null
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

        fun Artist.resolve() = if (id == null) resolved[name]?.let { copy(id = it) } ?: this else this
        return items.map { item ->
            when (item) {
                is SongItem -> item.copy(artists = item.artists.map { it.resolve() })
                is AlbumItem -> item.copy(artists = item.artists?.map { it.resolve() })
                is PlaylistItem -> item.copy(author = item.author?.resolve())
                is EpisodeItem -> item.copy(author = item.author?.resolve())
                is PodcastItem -> item.copy(author = item.author?.resolve())
                else -> item
            }
        }
    }

    suspend fun resolveArtistIdMap(items: List<YTItem>): Map<String, String> {
        val missingNames = mutableSetOf<String>()
        for (item in items) {
            when (item) {
                is SongItem -> item.artists.filter { it.id == null }.forEach { missingNames.add(it.name) }
                is AlbumItem -> item.artists?.filter { it.id == null }?.forEach { missingNames.add(it.name) }
                is PlaylistItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                is EpisodeItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                is PodcastItem -> item.author?.let { if (it.id == null) missingNames.add(it.name) }
                else -> {}
            }
        }

        if (missingNames.isEmpty()) return emptyMap()

        return coroutineScope {
            val semaphore = kotlinx.coroutines.sync.Semaphore(8)
            missingNames.map { name ->
                async {
                    semaphore.acquire()
                    try {
                        val searchResult = search(name, SearchFilter.FILTER_ARTIST).getOrNull()
                        val normalizedName = name.trim()
                        val artistId = searchResult?.items
                            ?.filterIsInstance<ArtistItem>()
                            ?.firstOrNull { candidate ->
                                candidate.title.trim().equals(normalizedName, ignoreCase = true)
                            }?.id
                        if (artistId != null) name to artistId else null
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
    }
}

internal class UploadProgressInputStream(
    input: InputStream,
    private val contentLength: Long,
    private val onProgress: (Float) -> Unit,
) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int =
        super.read().also { value ->
            if (value >= 0) reportBytesRead(1)
        }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { count ->
            if (count > 0) reportBytesRead(count)
        }

    private fun reportBytesRead(count: Int) {
        bytesRead += count
        onProgress((bytesRead.toFloat() / contentLength).coerceAtMost(1f))
    }
}