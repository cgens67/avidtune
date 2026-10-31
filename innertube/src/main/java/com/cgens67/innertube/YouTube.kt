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
import com.cgens67.innertube.pages.NewPipeExtractor
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

object YouTube {
    private val innerTube = InnerTube()

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
                songs = (response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents ?: response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.contents)?.getItems()?.mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it) } ?: emptyList(),
                songsContinuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
                continuation = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.continuations?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        runCatching {
            val response = innerTube.browse(client = WEB_REMIX, continuation = continuation, setLogin = true).body<BrowseResponse>()
            val contents = response.continuationContents?.sectionListContinuation?.contents?.mapNotNull { it.musicPlaylistShelfRenderer?.contents ?: it.musicShelfRenderer?.contents }?.flatten() ?: response.continuationContents?.musicPlaylistShelfContinuation?.contents ?: response.continuationContents?.musicShelfContinuation?.contents ?: response.onResponseReceivedActions.orEmpty().flatMap { it.appendContinuationItemsAction?.continuationItems.orEmpty() }
            val songs = contents.mapNotNull { it.musicResponsiveListItemRenderer }.mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it) }
            PlaylistContinuationPage(songs = songs, continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation() ?: response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation() ?: response.continuationContents?.musicShelfContinuation?.continuations?.getContinuation() ?: response.onResponseReceivedActions?.firstOrNull()?.appendContinuationItemsAction?.continuationItems?.getContinuation())
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
        val missingNames = items.filter { (it is SongItem && it.artists.any { a -> a.id == null }) || (it is AlbumItem && it.artists?.any { a -> a.id == null } == true) || (it is PlaylistItem && it.author?.id == null) || (it is EpisodeItem && it.author?.id == null) || (it is PodcastItem && it.author?.id == null) }.flatMap {
            when(it) {
                is SongItem -> it.artists.filter { a -> a.id == null }.map { a -> a.name }
                is AlbumItem -> it.artists?.filter { a -> a.id == null }?.map { a -> a.name } ?: emptyList()
                is PlaylistItem -> listOfNotNull(it.author?.name)
                is EpisodeItem -> listOfNotNull(it.author?.name)
                is PodcastItem -> listOfNotNull(it.author?.name)
                else -> emptyList()
            }
        }.toSet()
        if (missingNames.isEmpty()) return items
        val resolved = coroutineScope {
            missingNames.map { name -> async { val res = search(name, SearchFilter.FILTER_ARTIST).getOrNull(); name to (res?.items?.filterIsInstance<ArtistItem>()?.firstOrNull { it.title.trim().equals(name.trim(), ignoreCase = true) }?.id) } }.awaitAll().filter { it.second != null }.associate { it.first to it.second!! }
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
        val missingNames = items.filter { (it is SongItem && it.artists.any { a -> a.id == null }) || (it is AlbumItem && it.artists?.any { a -> a.id == null } == true) || (it is PlaylistItem && it.author?.id == null) || (it is EpisodeItem && it.author?.id == null) || (it is PodcastItem && it.author?.id == null) }.flatMap {
            when(it) {
                is SongItem -> it.artists.filter { a -> a.id == null }.map { a -> a.name }
                is AlbumItem -> it.artists?.filter { a -> a.id == null }?.map { a -> a.name } ?: emptyList()
                is PlaylistItem -> listOfNotNull(it.author?.name)
                is EpisodeItem -> listOfNotNull(it.author?.name)
                is PodcastItem -> listOfNotNull(it.author?.name)
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