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
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_REMIX
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
import com.cgens67.innertube.pages.NewPipeExtractor
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

object YouTube {
    private val innerTube = InnerTube()

    data class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
        }
    }

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) { innerTube.locale = value }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) { innerTube.visitorData = value }
    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) { innerTube.dataSyncId = value }
    var authUser: String
        get() = innerTube.authUser
        set(value) { innerTube.authUser = value }
    var cookie: String?
        get() = innerTube.cookie
        set(value) { innerTube.cookie = value }
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) { innerTube.proxy = value }
    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) { innerTube.proxyAuth = value }
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) { innerTube.useLoginForBrowse = value }

    fun extractionTransport(): InnerTube.ExtractionTransport = innerTube.extractionTransport()

    suspend fun visitorData(): Result<String?> =
        runCatching {
            innerTube.fetchFreshVisitorData().also {
                if (it != null) visitorData = it
            }
        }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        runCatching {
            val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries = response.contents?.getOrNull(0)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull { content ->
                    content.searchSuggestionRenderer?.suggestion?.runs?.joinToString(separator = "") { it.text }
                }.orEmpty(),
                recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull {
                    it.musicResponsiveListItemRenderer?.let { renderer -> SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer) }
                }.orEmpty(),
            )
        }

    suspend fun searchSummary(query: String, recordSearchHistory: Boolean = false): Result<SearchSummaryPage> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, setLogin = recordSearchHistory).body<SearchResponse>()
            val allSummaries = mutableListOf<SearchSummary>()

            response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.forEach { section ->
                if (section.musicCardShelfRenderer != null) {
                    val cardRenderer = section.musicCardShelfRenderer
                    val cardArtists = if (cardRenderer.onTap.browseEndpoint?.isArtistEndpoint == true) {
                        listOfNotNull(cardRenderer.title.runs?.firstOrNull()?.text?.let { name -> Artist(name, cardRenderer.onTap.browseEndpoint.browseId) })
                    } else {
                        PageHelper.extractArtists(cardRenderer.subtitle.runs)
                    }
                    val items = listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(cardRenderer)).plus(
                        cardRenderer.contents?.mapNotNull { it.musicResponsiveListItemRenderer }?.mapNotNull { renderer ->
                            SearchPage.toYTItem(renderer, cardArtists)
                        }.orEmpty(),
                    ).distinctBy { it.id }

                    if (items.isNotEmpty()) {
                        allSummaries.add(SearchSummary(title = cardRenderer.header?.musicCardShelfHeaderBasicRenderer?.title?.runs?.firstOrNull()?.text ?: YouTubeConstants.DEFAULT_TOP_RESULT, items = items))
                    }
                } else if (section.musicShelfRenderer != null) {
                    val items = section.musicShelfRenderer.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) }?.distinctBy { it.id } ?: emptyList()
                    if (items.isEmpty()) return@forEach
                    val apiTitle = section.musicShelfRenderer.title?.runs?.firstOrNull()?.text
                    if (apiTitle != null) {
                        allSummaries.add(SearchSummary(title = apiTitle, items = items))
                    } else {
                        allSummaries.addAll(groupItemsByType(items))
                    }
                } else if (section.itemSectionRenderer != null) {
                    val items = section.itemSectionRenderer.contents?.mapNotNull { it.musicResponsiveListItemRenderer }?.mapNotNull { SearchPage.toYTItem(it) }?.distinctBy { it.id } ?: emptyList()
                    if (items.isNotEmpty()) {
                        allSummaries.addAll(groupItemsByType(items))
                    }
                }
            }

            val mergedSummaries = allSummaries.groupBy { it.title }.map { (title, sections) ->
                SearchSummary(title = title, items = sections.flatMap { it.items }.distinctBy { it.id })
            }.sortedBy { summary ->
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
        val grouped = items.groupBy { item ->
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

        val sectionOrder = listOf("Songs", "Videos", "Albums", "Artists", "Playlists", "Podcasts", "Episodes", "Profiles", YouTubeConstants.DEFAULT_OTHER_RESULTS)
        return sectionOrder.mapNotNull { sectionName ->
            grouped[sectionName]?.takeIf { it.isNotEmpty() }?.let { groupItems -> SearchSummary(title = sectionName, items = groupItems) }
        }
    }

    suspend fun search(query: String, filter: SearchFilter, recordSearchHistory: Boolean = false): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, filter.value, setLogin = recordSearchHistory).body<SearchResponse>()
            val searchItems = mutableListOf<YTItem>()
            var searchContinuation: String? = null

            response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.forEach { section ->
                if (section.musicShelfRenderer != null) {
                    val shelf = section.musicShelfRenderer
                    val items = shelf.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
                    searchItems.addAll(items)
                    if (searchContinuation == null) searchContinuation = shelf.continuations?.getContinuation()
                } else if (section.itemSectionRenderer != null) {
                    val items = section.itemSectionRenderer.contents?.mapNotNull { it.musicResponsiveListItemRenderer }?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
                    searchItems.addAll(items)
                }
            }
            SearchResult(items = searchItems.distinctBy { it.id }, continuation = searchContinuation)
        }

    suspend fun searchContinuation(continuation: String, recordSearchHistory: Boolean = false): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, continuation = continuation, setLogin = recordSearchHistory).body<SearchResponse>()
            val items = response.continuationContents?.musicShelfContinuation?.contents?.mapNotNull { SearchPage.toYTItem(it.musicResponsiveListItemRenderer) } ?: emptyList()
            SearchResult(items = items, continuation = if (items.isEmpty()) null else response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation())
        }

    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            if (browseId.contains("FEmusic_library_privately_owned_release_detail")) {
                val playlistId = response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId!!
                val albumItem = AlbumItem(
                    browseId = browseId, playlistId = playlistId, title = response.header.musicDetailHeaderRenderer.title.runs?.firstOrNull()?.text!!,
                    artists = PageHelper.extractArtists(response.header.musicDetailHeaderRenderer.subtitle.runs).takeIf { it.isNotEmpty() },
                    year = response.header.musicDetailHeaderRenderer.subtitle.runs?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()!!.url, explicit = false
                )
                return@runCatching AlbumPage(
                    album = albumItem,
                    songs = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.getItems()?.mapNotNull { AlbumPage.getSong(it, albumItem) }!!.toMutableList(),
                    otherVersions = emptyList(),
                )
            } else {
                val playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
                val albumItem = AlbumItem(
                    browseId = browseId, playlistId = playlistId, title = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    artists = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.let(PageHelper::extractArtists)?.takeIf { it.isNotEmpty() },
                    year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url!!, explicit = false
                )
                val albumSongsList = if (withSongs) albumSongs(playlistId, albumItem).getOrThrow() else emptyList()
                val performer = albumSongsList.firstOrNull()?.artists?.firstOrNull()?.takeIf { first -> first.name.isNotBlank() && albumSongsList.all { it.artists.firstOrNull()?.name == first.name } }
                val resolvedAlbum = if (performer != null && albumItem.artists?.any { it.name == performer.name } != true) albumItem.copy(artists = listOf(performer)) else albumItem
                return@runCatching AlbumPage(
                    album = resolvedAlbum, songs = albumSongsList,
                    otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(1)?.musicCarouselShelfRenderer?.contents?.mapNotNull { it.musicTwoRowItemRenderer }?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer).orEmpty()
                )
            }
        }

    suspend fun albumSongs(playlistId: String, album: AlbumItem? = null): Result<List<SongItem>> =
        runCatching {
            var response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
            val shelf = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()
            val shelfContents = shelf?.musicPlaylistShelfRenderer?.contents ?: shelf?.musicShelfRenderer?.contents ?: emptyList()
            val songs = shelfContents.getItems().mapNotNull { AlbumPage.getSong(it, album) }.toMutableList()
            var continuation = shelfContents.getContinuation()
            val seenContinuations = mutableSetOf<String>()
            var requestCount = 0
            val maxRequests = 50

            while (continuation != null && requestCount < maxRequests) {
                if (continuation in seenContinuations) break
                seenContinuations.add(continuation)
                requestCount++
                response = innerTube.browse(client = WEB_REMIX, continuation = continuation).body<BrowseResponse>()
                songs += response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getItems()?.mapNotNull { AlbumPage.getSong(it, album) }.orEmpty()
                continuation = response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()
            }
            songs
        }

    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            fun mapRuns(runs: List<Run>?): List<Run>? = runs?.map { Run(text = it.text, navigationEndpoint = it.navigationEndpoint) }
            val descriptionRuns = response.contents?.sectionListRenderer?.contents?.firstOrNull { it.musicDescriptionShelfRenderer != null }?.musicDescriptionShelfRenderer?.description?.runs?.let(::mapRuns) ?: response.header?.musicImmersiveHeaderRenderer?.description?.runs?.let(::mapRuns)
            val isSubscribed = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscribed ?: response.header?.musicVisualHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.subscribed ?: false
            val channelId = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId ?: response.header?.musicVisualHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.channelId
            
            ArtistPage(
                artist = ArtistItem(
                    id = browseId,
                    title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    thumbnail = response.header?.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: response.header?.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: response.header?.musicDetailHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    channelId = channelId,
                    playEndpoint = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint,
                    shuffleEndpoint = response.header?.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.firstOrNull()?.musicResponsiveListItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    radioEndpoint = response.header?.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
                ),
                sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                description = descriptionRuns?.joinToString(separator = "") { it.text },
                subscriberCountText = response.header?.musicImmersiveHeaderRenderer?.subscriptionButton2?.subscribeButtonRenderer?.subscriberCountWithSubscribeText?.runs?.firstOrNull()?.text ?: response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.longSubscriberCountText?.runs?.firstOrNull()?.text ?: response.header?.musicImmersiveHeaderRenderer?.subscriptionButton?.subscribeButtonRenderer?.shortSubscriberCountText?.runs?.firstOrNull()?.text,
                monthlyListenerCount = response.header?.musicImmersiveHeaderRenderer?.monthlyListenerCount?.runs?.firstOrNull()?.text,
                descriptionRuns = descriptionRuns,
                isSubscribed = isSubscribed,
            )
        }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            val sectionContent = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            
            when {
                sectionContent?.gridRenderer != null -> ArtistItemsPage(title = sectionContent.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(), items = sectionContent.gridRenderer.items.mapNotNull { it.musicTwoRowItemRenderer?.let { renderer -> ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer) } }, continuation = sectionContent.gridRenderer.continuations?.getContinuation())
                sectionContent?.musicCarouselShelfRenderer != null -> ArtistItemsPage(title = sectionContent.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(), items = sectionContent.musicCarouselShelfRenderer.contents.mapNotNull { it.musicTwoRowItemRenderer?.let { renderer -> ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer) } ?: it.musicResponsiveListItemRenderer?.let { renderer -> ArtistItemsPage.fromMusicResponsiveListItemRenderer(renderer) } }, continuation = null)
                sectionContent?.musicShelfRenderer != null -> ArtistItemsPage(title = sectionContent.musicShelfRenderer.title?.runs?.firstOrNull()?.text ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: "", items = sectionContent.musicShelfRenderer.contents?.getItems()?.mapNotNull { ArtistItemsPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList(), continuation = sectionContent.musicShelfRenderer.continuations?.getContinuation())
                else -> ArtistItemsPage(title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: "", items = sectionContent?.musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull { ArtistItemsPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList(), continuation = sectionContent?.musicPlaylistShelfRenderer?.contents?.getContinuation())
            }
        }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
            when {
                response.continuationContents?.gridContinuation != null -> {
                    val cont = response.continuationContents.gridContinuation
                    val items = cont.items.mapNotNull { it.musicTwoRowItemRenderer?.let { renderer -> ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer) } }
                    ArtistItemsContinuationPage(items = items, continuation = if (items.isEmpty()) null else cont.continuations?.getContinuation())
                }
                response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                    val cont = response.continuationContents.musicPlaylistShelfContinuation
                    val items = cont.contents.getItems().mapNotNull { ArtistItemsPage.fromMusicResponsiveListItemRenderer(it) }
                    ArtistItemsContinuationPage(items = items, continuation = if (items.isEmpty()) null else cont.continuations?.getContinuation())
                }
                else -> {
                    val cont = response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems
                    val items = cont?.getItems()?.mapNotNull { ArtistItemsPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList()
                    ArtistItemsContinuationPage(items = items, continuation = if (items.isEmpty()) null else cont?.getContinuation())
                }
            }
        }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = "VL$playlistId", setLogin = true).body<BrowseResponse>()
            val base = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            val header = base?.musicResponsiveHeaderRenderer ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
            val editable = base?.musicEditablePlaylistDetailHeaderRenderer != null
            val description: String? = header?.description?.musicDescriptionShelfRenderer?.description?.runs?.joinToString("") { it.text } ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicDetailHeaderRenderer?.description?.runs?.joinToString("") { it.text } ?: response.header?.musicDetailHeaderRenderer?.description?.runs?.joinToString("") { it.text }
            
            val shelfContents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents 
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents
                ?: emptyList()
                
            val shelfContinuations = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.continuations 
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.continuations
                
            val rootContinuations = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.continuations 
                ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.continuations

            PlaylistPage(
                playlist = PlaylistItem(
                    id = playlistId,
                    title = header?.title?.runs?.firstOrNull()?.text ?: response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text ?: "",
                    author = PlaylistPage.ownerFromDetailSubtitle(header?.subtitle?.runs ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs),
                    songCountText = (header?.secondSubtitle ?: response.header?.musicHeaderRenderer?.secondSubtitle)?.runs?.findLast { it.text.any { c -> c.isDigit() } }?.text,
                    thumbnail = header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url ?: response.header?.musicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnails?.lastOrNull()?.url ?: "",
                    playEndpoint = null,
                    shuffleEndpoint = header?.buttons?.lastOrNull()?.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: response.header?.musicHeaderRenderer?.buttons?.lastOrNull()?.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    radioEndpoint = header?.buttons?.getOrNull(2)?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: response.header?.musicHeaderRenderer?.buttons?.getOrNull(2)?.menuRenderer?.items?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MIX" }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                    isEditable = editable,
                    description = description,
                    authorAvatarUrl = header?.facepile?.avatarStackViewModel?.avatars?.firstOrNull()?.avatarViewModel?.image?.sources?.firstOrNull()?.url
                ),
                songs = shelfContents.getItems().mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it) },
                songsContinuation = shelfContinuations?.getContinuation() ?: shelfContents.getContinuation(),
                continuation = rootContinuations?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, continuation = continuation, setLogin = true).body<BrowseResponse>()
            val contents = response.continuationContents?.sectionListContinuation?.contents?.mapNotNull { it.musicPlaylistShelfRenderer?.contents ?: it.musicShelfRenderer?.contents }?.flatten() 
                ?: response.continuationContents?.musicPlaylistShelfContinuation?.contents 
                ?: response.continuationContents?.musicShelfContinuation?.contents 
                ?: response.onResponseReceivedActions.orEmpty().flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }
            val songs = contents.mapNotNull { it.musicResponsiveListItemRenderer }.mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it) }
            PlaylistContinuationPage(
                songs = songs, 
                continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation() 
                    ?: response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation() 
                    ?: response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation() 
                    ?: response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getContinuation()
                    ?: contents.getContinuation()
            )
        }

    suspend fun library(browseId: String, params: String? = null): Result<LibraryPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, browseId = browseId, params = params, setLogin = true).body<BrowseResponse>()
            val isUploaded = browseId.contains("upload", ignoreCase = true) || browseId.contains("privately_owned", ignoreCase = true)
            val sectionContent = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                ?: response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()

            val items: List<YTItem>
            val continuation: String?

            when {
                sectionContent?.gridRenderer != null -> {
                    items = sectionContent.gridRenderer.items.mapNotNull { it.musicTwoRowItemRenderer?.let(LibraryPage::fromMusicTwoRowItemRenderer) }
                    continuation = sectionContent.gridRenderer.continuations?.getContinuation()
                }
                sectionContent?.musicShelfRenderer != null -> {
                    items = sectionContent.musicShelfRenderer.contents?.getItems()?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded) } ?: emptyList()
                    continuation = sectionContent.musicShelfRenderer.continuations?.getContinuation() ?: sectionContent.musicShelfRenderer.contents?.getContinuation()
                }
                sectionContent?.musicPlaylistShelfRenderer != null -> {
                    items = sectionContent.musicPlaylistShelfRenderer.contents.getItems().mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded) }
                    continuation = sectionContent.musicPlaylistShelfRenderer.continuations?.getContinuation() ?: sectionContent.musicPlaylistShelfRenderer.contents.getContinuation()
                }
                else -> {
                    items = emptyList()
                    continuation = null
                }
            }
            LibraryPage(items = items, continuation = continuation, isUploaded = isUploaded)
        }

    suspend fun libraryContinuation(continuation: String, isUploaded: Boolean = false): Result<LibraryContinuationPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, continuation = continuation, setLogin = true).body<BrowseResponse>()
            val items = when {
                response.continuationContents?.gridContinuation != null -> {
                    response.continuationContents.gridContinuation.items.mapNotNull {
                        it.musicTwoRowItemRenderer?.let(LibraryPage::fromMusicTwoRowItemRenderer)
                    }
                }
                response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                    response.continuationContents.musicPlaylistShelfContinuation.contents.getItems().mapNotNull {
                        LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded)
                    }
                }
                response.continuationContents?.musicShelfContinuation != null -> {
                    response.continuationContents.musicShelfContinuation.contents?.getItems()?.mapNotNull {
                        LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded)
                    } ?: emptyList()
                }
                else -> {
                    val cont = response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems
                    cont?.getItems()?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it, isUploaded) } ?: emptyList()
                }
            }
            val nextContinuation = response.continuationContents?.gridContinuation?.continuations?.getContinuation()
                ?: response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()
                ?: response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation()
                ?: response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getContinuation()

            LibraryContinuationPage(
                items = items,
                continuation = if (items.isEmpty()) null else nextContinuation
            )
        }

    suspend fun explore(): Result<ExplorePage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_explore").body<BrowseResponse>()
            val sectionList = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?: emptyList()

            val newReleaseAlbums = mutableListOf<AlbumItem>()
            val moodAndGenres = mutableListOf<MoodAndGenres.Item>()

            sectionList.forEach { content ->
                content.musicCarouselShelfRenderer?.let { carousel ->
                    carousel.contents.forEach { item ->
                        item.musicTwoRowItemRenderer?.let { renderer ->
                            NewReleaseAlbumPage.fromMusicTwoRowItemRenderer(renderer)?.let { newReleaseAlbums.add(it) }
                        }
                    }
                }
                content.gridRenderer?.let { _ ->
                    MoodAndGenres.fromSectionListRendererContent(content)?.let { moodAndGenres.addAll(it.items) }
                }
            }

            ExplorePage(
                newReleaseAlbums = newReleaseAlbums,
                moodAndGenres = moodAndGenres
            )
        }

    suspend fun home(): Result<HomePage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_home").body<BrowseResponse>()
            val sectionListRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
            val chips = sectionListRenderer?.header?.chipCloudRenderer?.chips?.mapNotNull {
                HomePage.Chip.fromChipCloudChipRenderer(it)
            }
            val sections = sectionListRenderer?.contents?.mapNotNull { content ->
                content.musicCarouselShelfRenderer?.let {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }
            } ?: emptyList()
            HomePage(
                chips = chips,
                sections = sections,
                continuation = sectionListRenderer?.continuations?.getContinuation()
            )
        }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_new_releases_albums").body<BrowseResponse>()
            val contents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty()
            contents.flatMap { content ->
                content.gridRenderer?.items?.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { r -> NewReleaseAlbumPage.fromMusicTwoRowItemRenderer(r) }
                } ?: content.musicCarouselShelfRenderer?.contents?.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { r -> NewReleaseAlbumPage.fromMusicTwoRowItemRenderer(r) }
                }.orEmpty()
            }
        }

    suspend fun musicHistory(): Result<HistoryPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_history", setLogin = true).body<BrowseResponse>()
            val sectionList = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty()
            val sections = sectionList.mapNotNull { it.musicShelfRenderer }.map { HistoryPage.fromMusicShelfRenderer(it) }
            HistoryPage(sections = sections)
        }

    suspend fun accountInfo(): Result<AccountInfo> =
        runCatching {
            val response = innerTube.accountMenu(WEB_REMIX).body<AccountMenuResponse>()
            response.actions.first().openPopupAction.popup.multiPageMenuRenderer.header!!.activeAccountHeaderRenderer.toAccountInfo()
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_moods_and_genres").body<BrowseResponse>()
            val contents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty()
            contents.mapNotNull { MoodAndGenres.fromSectionListRendererContent(it) }
        }

    suspend fun browse(browseId: String, params: String? = null): Result<BrowseResult> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId, params).body<BrowseResponse>()
            val title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                ?: response.header?.musicDetailHeaderRenderer?.title?.runs?.firstOrNull()?.text
            val sectionList = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents
                ?: emptyList()
            val items = sectionList.mapNotNull { content ->
                val sectionTitle = content.musicShelfRenderer?.title?.runs?.firstOrNull()?.text
                    ?: content.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: content.gridRenderer?.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text
                val sectionItems = when {
                    content.musicShelfRenderer != null -> content.musicShelfRenderer.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) }.orEmpty()
                    content.musicCarouselShelfRenderer != null -> content.musicCarouselShelfRenderer.contents.mapNotNull { 
                        it.musicTwoRowItemRenderer?.let { r -> RelatedPage.fromMusicTwoRowItemRenderer(r) }
                            ?: it.musicResponsiveListItemRenderer?.let { r -> SearchPage.toYTItem(r) }
                    }
                    content.gridRenderer != null -> content.gridRenderer.items.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { r -> RelatedPage.fromMusicTwoRowItemRenderer(r) }
                    }
                    else -> emptyList()
                }
                if (sectionItems.isNotEmpty()) BrowseResult.Item(sectionTitle, sectionItems) else null
            }
            BrowseResult(title = title, items = items)
        }

    suspend fun queue(videoIds: List<String>? = null, playlistId: String? = null): Result<List<SongItem>> =
        runCatching {
            val response = innerTube.getQueue(WEB_REMIX, videoIds, playlistId).body<GetQueueResponse>()
            response.queueDatas
                .mapNotNull { it.content.videoRenderer }
                .mapNotNull(NextPage::fromPlaylistPanelVideoRenderer)
        }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val effectivePlaylistId = endpoint.playlistId
                ?: endpoint.videoId?.let { "RDAMVM$it" }

            val response = innerTube.next(
                client = WEB_REMIX,
                videoId = endpoint.videoId,
                playlistId = effectivePlaylistId,
                playlistSetVideoId = endpoint.playlistSetVideoId,
                index = endpoint.index,
                params = endpoint.params,
                continuation = continuation
            ).body<NextResponse>()

            val tabs = response.contents?.singleColumnMusicWatchNextResultsRenderer
                ?.tabbedRenderer?.watchNextTabbedResultsRenderer?.tabs.orEmpty()

            val queueTab = tabs.firstNotNullOfOrNull { it.tabRenderer?.content?.musicQueueRenderer }

            val playlistPanel = response.continuationContents?.playlistPanelContinuation
                ?: response.contents?.singleColumnMusicWatchNextResultsRenderer?.playlist?.playlistPanelRenderer
                ?: queueTab?.content?.playlistPanelRenderer

            val items = playlistPanel?.contents
                ?.mapNotNull { it.videoRenderer }
                ?.mapNotNull(NextPage::fromPlaylistPanelVideoRenderer)
                .orEmpty()

            val lyricsEndpoint = tabs.getOrNull(1)?.tabRenderer?.endpoint?.browseEndpoint
            val relatedEndpoint = tabs.getOrNull(2)?.tabRenderer?.endpoint?.browseEndpoint

            val rawTitle = playlistPanel?.title
                ?: playlistPanel?.titleText?.runs?.firstOrNull()?.text
                ?: playlistPanel?.shortBylineText?.runs?.firstOrNull()?.text
                ?: (if (endpoint.playlistId == null) "Radio" else null)

            NextResult(
                title = rawTitle,
                items = items,
                currentIndex = playlistPanel?.contents?.indexOfFirst { it.videoRenderer?.selected == true }?.takeIf { it != -1 },
                lyricsEndpoint = lyricsEndpoint,
                relatedEndpoint = relatedEndpoint,
                continuation = playlistPanel?.continuations?.getContinuation(),
                endpoint = endpoint
            )
        }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            val sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty()
            val songs = mutableListOf<SongItem>()
            val albums = mutableListOf<AlbumItem>()
            val artists = mutableListOf<ArtistItem>()
            val playlists = mutableListOf<PlaylistItem>()

            sections.forEach { section ->
                section.musicShelfRenderer?.contents?.getItems()?.mapNotNull { RelatedPage.fromMusicResponsiveListItemRenderer(it) }?.let { songs.addAll(it) }
                section.musicCarouselShelfRenderer?.contents?.forEach { item ->
                    item.musicResponsiveListItemRenderer?.let { RelatedPage.fromMusicResponsiveListItemRenderer(it)?.let { song -> songs.add(song) } }
                    item.musicTwoRowItemRenderer?.let {
                        when (val ytItem = RelatedPage.fromMusicTwoRowItemRenderer(it)) {
                            is AlbumItem -> albums.add(ytItem)
                            is ArtistItem -> artists.add(ytItem)
                            is PlaylistItem -> playlists.add(ytItem)
                            is SongItem -> songs.add(ytItem)
                            else -> {}
                        }
                    }
                }
            }

            RelatedPage(
                songs = songs,
                albums = albums,
                artists = artists,
                playlists = playlists
            )
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
        response.contents?.sectionListRenderer?.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.runs?.joinToString(separator = "") { it.text }
    }

    suspend fun transcript(videoId: String): Result<String> = runCatching {
        val response = innerTube.getTranscript(WEB_REMIX, videoId).body<GetTranscriptResponse>()
        response.actions?.firstOrNull()?.updateEngagementPanelAction?.content?.transcriptRenderer?.body?.transcriptBodyRenderer?.cueGroups?.flatMap { it.transcriptCueGroupRenderer.cues }?.joinToString(separator = "\n") { cue ->
            val startMs = cue.transcriptCueRenderer.startOffsetMs
            val min = startMs / 60000
            val sec = (startMs % 60000) / 1000
            val ms = (startMs % 1000) / 10
            String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s", min, sec, ms, cue.transcriptCueRenderer.cue.simpleText)
        } ?: throw IllegalStateException("No transcript found")
    }

    suspend fun player(
        videoId: String,
        playlistId: String? = null,
        client: YouTubeClient = WEB_REMIX,
        signatureTimestamp: Int? = null,
        poToken: String? = null,
    ): Result<PlayerResponse> = runCatching {
        innerTube.player(client, videoId, playlistId, signatureTimestamp, poToken).body()
    }

    suspend fun registerPlayback(
        videoId: String,
        clientPlaybackNonce: String,
        isHistoryEnabled: () -> Boolean = { true }
    ): Result<Boolean> = runCatching {
        innerTube.registerPlayback(videoId, clientPlaybackNonce, isHistoryEnabled)
    }

    suspend fun likeVideo(videoId: String, like: Boolean) = runCatching {
        if (like) innerTube.likeVideo(WEB_REMIX, videoId) else innerTube.unlikeVideo(WEB_REMIX, videoId)
    }

    suspend fun likePlaylist(playlistId: String, like: Boolean) = runCatching {
        if (like) innerTube.likePlaylist(WEB_REMIX, playlistId) else innerTube.unlikePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun subscribeChannel(channelId: String, subscribe: Boolean) = runCatching {
        if (subscribe) innerTube.subscribeChannel(WEB_REMIX, channelId) else innerTube.unsubscribeChannel(WEB_REMIX, channelId)
    }

    suspend fun getChannelId(artistId: String): String = runCatching {
        artist(artistId).getOrThrow().artist.channelId ?: artistId
    }.getOrDefault(artistId)

    suspend fun addToPlaylist(playlistId: String, videoId: String) = runCatching {
        innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId)
    }

    suspend fun addPlaylistToPlaylist(playlistId: String, addedPlaylistId: String) = runCatching {
        innerTube.addPlaylistToPlaylist(WEB_REMIX, playlistId, addedPlaylistId)
    }

    suspend fun createPlaylist(title: String): Result<String> = runCatching {
        innerTube.createPlaylist(WEB_REMIX, title).body<CreatePlaylistResponse>().playlistId
    }

    suspend fun renamePlaylist(playlistId: String, name: String) = runCatching {
        innerTube.renamePlaylist(WEB_REMIX, playlistId, name)
    }

    suspend fun deletePlaylist(playlistId: String) = runCatching {
        innerTube.deletePlaylist(WEB_REMIX, playlistId)
    }

    suspend fun removeFromPlaylist(playlistId: String, videoId: String, setVideoId: String) = runCatching {
        innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId)
    }

    suspend fun moveSongPlaylist(playlistId: String, setVideoId: String, successorSetVideoId: String?) = runCatching {
        innerTube.moveSongPlaylist(WEB_REMIX, playlistId, setVideoId, successorSetVideoId)
    }

    suspend fun newEpisodes(): Result<List<EpisodeItem>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, "FEmusic_new_episodes").body<BrowseResponse>()
            val contents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents.orEmpty()
            val episodes = contents.mapNotNull { it.musicMultiRowListItemRenderer }.mapNotNull { renderer ->
                val ep = PodcastPage.fromMusicMultiRowListItemRenderer(renderer) ?: return@mapNotNull null
                val artists = PodcastPage.extractPodcastArtists(renderer)
                ep.copy(author = artists.firstOrNull())
            }

            episodes.map { episode ->
                if (episode.author == null) {
                    val mediaInfo = runCatching { innerTube.getMediaInfo(episode.id).getOrNull() }.getOrNull()
                    if (mediaInfo?.author != null && mediaInfo.authorId != null) {
                        episode.copy(author = Artist(mediaInfo.author, mediaInfo.authorId))
                    } else {
                        episode
                    }
                } else {
                    episode
                }
            }
        }

    suspend fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>> = NewPipeExtractor.newPipePlayer(videoId)

    suspend fun newPipePlayer(videoId: String, tempRes: PlayerResponse): PlayerResponse? {
        if (tempRes.playabilityStatus.status != "OK") return null
        val streamsList = getNewPipeStreamUrls(videoId)
        if (streamsList.isEmpty()) return null
        val decodedSigResponse = tempRes.copy(streamingData = tempRes.streamingData?.copy(
            formats = tempRes.streamingData.formats?.map { it.copy(url = streamsList.find { s -> s.first == it.itag }?.second ?: it.url) },
            adaptiveFormats = tempRes.streamingData.adaptiveFormats.map { it.copy(url = streamsList.find { s -> s.first == it.itag }?.second ?: it.url) }
        ))
        val urlList = (decodedSigResponse.streamingData?.adaptiveFormats?.mapNotNull { it.url }?.toMutableList() ?: mutableListOf()).apply { decodedSigResponse.streamingData?.formats?.mapNotNull { it.url }?.let { addAll(it) } }
        return if (urlList.isNotEmpty()) decodedSigResponse else null
    }

    suspend fun resolveArtistIds(items: List<YTItem>): List<YTItem> {
        val normalizedItems = items.map { item ->
            when (item) {
                is SongItem -> {
                    val expanded = item.artists.flatMap { artist ->
                        if (artist.id == null) PageHelper.splitArtistNames(artist.name).map { Artist(it, null) }
                        else listOf(artist)
                    }
                    if (expanded != item.artists) item.copy(artists = expanded) else item
                }
                is AlbumItem -> {
                    val expanded = item.artists?.flatMap { artist ->
                        if (artist.id == null) PageHelper.splitArtistNames(artist.name).map { Artist(it, null) }
                        else listOf(artist)
                    }
                    if (expanded != item.artists) item.copy(artists = expanded) else item
                }
                else -> item
            }
        }

        val missingNames = normalizedItems.filter { (it is SongItem && it.artists.any { a -> a.id == null }) || (it is AlbumItem && it.artists?.any { a -> a.id == null } == true) || (it is PlaylistItem && it.author?.id == null) || (it is EpisodeItem && it.author?.id == null) || (it is PodcastItem && it.author?.id == null) }.flatMap {
            when(it) {
                is SongItem -> it.artists.filter { a -> a.id == null }.map { a -> a.name }
                is AlbumItem -> it.artists?.filter { a -> a.id == null }?.map { a -> a.name } ?: emptyList()
                is PlaylistItem -> listOfNotNull(it.author?.name)
                is EpisodeItem -> listOfNotNull(it.author?.name)
                is PodcastItem -> listOfNotNull(it.author?.name)
                else -> emptyList()
            }
        }.toSet()
        if (missingNames.isEmpty()) return normalizedItems
        val resolved = coroutineScope {
            missingNames.map { name -> async { val res = search(name, SearchFilter.FILTER_ARTIST).getOrNull(); name to (res?.items?.filterIsInstance<ArtistItem>()?.firstOrNull { it.title.trim().equals(name.trim(), ignoreCase = true) }?.id) } }.awaitAll().filter { it.second != null }.associate { it.first to it.second!! }
        }
        fun Artist.resolve() = if (id == null) resolved[name]?.let { copy(id = it) } ?: this else this
        return normalizedItems.map { item ->
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
        val missingNames = items.filter { (it is SongItem && it.artists.any { a -> a.id == null }) || (it is AlbumItem && it.artists?.any { a -> a.id == null } == true) || (it is PlaylistItem && it.author?.id == null) || (it is EpisodeItem && it.author?.id == null) || (it is PodcastItem && it.author?.id == null) }.flatMap {
            when(it) {
                is SongItem -> it.artists.flatMap { a -> if (a.id == null) PageHelper.splitArtistNames(a.name) else emptyList() }
                is AlbumItem -> it.artists.orEmpty().flatMap { a -> if (a.id == null) PageHelper.splitArtistNames(a.name) else emptyList() }
                is PlaylistItem -> listOfNotNull(it.author?.name?.let { n -> if (it.author.id == null) n else null })
                is EpisodeItem -> listOfNotNull(it.author?.name?.let { n -> if (it.author.id == null) n else null })
                is PodcastItem -> listOfNotNull(it.author?.name?.let { n -> if (it.author.id == null) n else null })
                else -> emptyList()
            }
        }.toSet()
        if (missingNames.isEmpty()) return emptyMap()
        return coroutineScope {
            missingNames.map { name -> async { val res = search(name, SearchFilter.FILTER_ARTIST).getOrNull(); name to (res?.items?.filterIsInstance<ArtistItem>()?.firstOrNull { it.title.trim().equals(name.trim(), ignoreCase = true) }?.id) } }.awaitAll().filter { it.second != null }.associate { it.first to it.second!! }
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
        super.read().also { value -> if (value >= 0) reportBytesRead(1) }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { count -> if (count > 0) reportBytesRead(count) }

    private fun reportBytesRead(count: Int) {
        bytesRead += count
        onProgress((bytesRead.toFloat() / contentLength).coerceAtMost(1f))
    }
}