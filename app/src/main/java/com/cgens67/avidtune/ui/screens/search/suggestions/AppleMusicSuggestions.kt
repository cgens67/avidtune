@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)

package com.cgens67.avidtune.ui.screens.search.suggestions

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CountryCodeToName
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.ui.component.IconButton as AtIconButton
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.NetworkConnectivityObserver
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.filterExplicit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

// --- Preference Key & Region Map ---
val SuggestionRegionKey = stringPreferencesKey("suggestion_region")

val SuggestionRegionSlugToName: Map<String, String> = buildMap {
    put("system", "System Default")
    CountryCodeToName.entries
        .sortedBy { it.value }
        .forEach { (code, name) -> put(code.lowercase(), name) }
}

// --- Models ---
data class SuggestionTrack(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val appleMusicUrl: String? = null
)

data class SuggestionArtist(
    val rank: Int,
    val name: String,
    val thumbnailUrl: String?
)

data class SuggestionAlbum(
    val rank: Int,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val appleMusicUrl: String? = null
)

// --- Scraper ---
object AppleMusicScraper {
    private const val TAG = "AppleMusicScraper"
    private const val CACHE_MAX_AGE_MS = 6 * 60 * 60 * 1000L
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val RSS_HOSTS = listOf(
        "https://rss.marketingtools.apple.com/api/v2",
        "https://rss.applemarketingtools.com/api/v2"
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private data class HttpFetchResult(
        val body: String?,
        val httpCode: Int?
    )

    private fun executeGet(url: String): HttpFetchResult = try {
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute().use { response ->
            if (response.isSuccessful) {
                HttpFetchResult(response.body?.string(), response.code)
            } else {
                HttpFetchResult(null, response.code)
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error executing GET request for $url", e)
        HttpFetchResult(null, null)
    }

    private inline fun <T> parseFeedResults(
        body: String,
        artworkDim: String,
        mapItem: (rank: Int, title: String, artist: String, artwork: String?, url: String?) -> T
    ): List<T> = try {
        val feedObj = JSONObject(body).optJSONObject("feed") ?: return emptyList()
        val results = feedObj.optJSONArray("results") ?: return emptyList()
        val list = ArrayList<T>(results.length())
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val name = item.optString("name", "").trim()
            if (name.isEmpty()) continue
            val artistName = item.optString("artistName", "").trim()
            val rawArtwork = item.optString("artworkUrl100", "").trim()
            val artwork = if (rawArtwork.isNotEmpty()) {
                rawArtwork.replace(Regex("(\\d+)x(\\d+)"), artworkDim)
            } else null
            val url = item.optString("url", "").trim().takeIf { it.isNotEmpty() }
            list.add(mapItem(list.size + 1, name, artistName, artwork, url))
        }
        list
    } catch (e: Exception) {
        Log.e(TAG, "Error parsing Apple Music feed", e)
        emptyList()
    }

    private inline fun <T> fetchFeedWithFallback(
        context: Context,
        countryCode: String,
        limits: List<Int>,
        feedType: String,
        cacheName: String,
        artworkDim: String,
        forceRefresh: Boolean,
        mapItem: (rank: Int, title: String, artist: String, artwork: String?, url: String?) -> T
    ): List<T> {
        val file = File(context.cacheDir, cacheName)
        if (!forceRefresh && file.exists() && System.currentTimeMillis() - file.lastModified() < CACHE_MAX_AGE_MS) {
            val cached = runCatching { file.readText() }.getOrNull()
            if (!cached.isNullOrBlank()) {
                val parsed = parseFeedResults(cached, artworkDim, mapItem)
                if (parsed.isNotEmpty()) return parsed
            }
        }

        val normalizedCountry = countryCode.trim().lowercase()
        for (host in RSS_HOSTS) {
            var hostReachableWithoutServerError = false
            for (limit in limits) {
                val url = "$host/$normalizedCountry/music/most-played/$limit/$feedType.json"
                val result = executeGet(url)
                if (result.body != null) {
                    hostReachableWithoutServerError = true
                    val parsed = parseFeedResults(result.body, artworkDim, mapItem)
                    if (parsed.isNotEmpty()) {
                        runCatching { file.writeText(result.body) }
                        return parsed
                    }
                } else if (result.httpCode != null && result.httpCode < 500) {
                    hostReachableWithoutServerError = true
                } else {
                    break
                }
            }
            if (hostReachableWithoutServerError) {
                break
            }
        }

        if (file.exists()) {
            val stale = runCatching { file.readText() }.getOrNull()
            if (!stale.isNullOrBlank()) {
                val parsed = parseFeedResults(stale, artworkDim, mapItem)
                if (parsed.isNotEmpty()) return parsed
            }
        }

        return emptyList()
    }

    fun fetchTopSongs(
        context: Context,
        countryCode: String = "us",
        forceRefresh: Boolean = false
    ): List<SuggestionTrack> =
        fetchFeedWithFallback(
            context = context,
            countryCode = countryCode,
            limits = listOf(100, 50, 25, 10),
            feedType = "songs",
            cacheName = "apple_songs_$countryCode.json",
            artworkDim = "1400x1400",
            forceRefresh = forceRefresh
        ) { r, t, a, art, u -> SuggestionTrack(r, t, a, art, u) }

    fun fetchTopAlbums(
        context: Context,
        countryCode: String = "us",
        forceRefresh: Boolean = false
    ): List<SuggestionAlbum> =
        fetchFeedWithFallback(
            context = context,
            countryCode = countryCode,
            limits = listOf(20, 50, 10),
            feedType = "albums",
            cacheName = "apple_albums_$countryCode.json",
            artworkDim = "1400x1400",
            forceRefresh = forceRefresh
        ) { r, t, a, art, u -> SuggestionAlbum(r, t, a, art, u) }

    fun fetchTopVideos(
        context: Context,
        countryCode: String = "us",
        forceRefresh: Boolean = false
    ): List<SuggestionTrack> =
        fetchFeedWithFallback(
            context = context,
            countryCode = countryCode,
            limits = listOf(20, 50, 10),
            feedType = "music-videos",
            cacheName = "apple_videos_$countryCode.json",
            artworkDim = "1920x1080",
            forceRefresh = forceRefresh
        ) { r, t, a, art, u -> SuggestionTrack(r, t, a, art, u) }.sortedBy { it.rank }

    fun getTrendingArtists(tracks: List<SuggestionTrack>): List<SuggestionArtist> {
        val counts = mutableMapOf<String, Int>()
        val images = mutableMapOf<String, String?>()
        tracks.forEach { track ->
            val mainArtist = track.artist.split(",", "&", "feat.", "ft.").first().trim()
            if (mainArtist.isNotEmpty()) {
                counts[mainArtist] = (counts[mainArtist] ?: 0) + 1
                if (images[mainArtist] == null) {
                    images[mainArtist] = track.thumbnailUrl?.replace("1920x1080", "500x500")
                }
            }
        }
        return counts.toList()
            .sortedByDescending { it.second }
            .take(15)
            .mapIndexed { index, (name, _) -> SuggestionArtist(index + 1, name, images[name]) }
    }
}

// --- ViewModel ---
@HiltViewModel
class SuggestionsViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
) : ViewModel() {
    private val networkConnectivityObserver = NetworkConnectivityObserver(context)
    private var currentLoadedRegion: String? = null
    private var refreshJob: Job? = null

    private val _suggestionTracks = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionTracks: StateFlow<List<SuggestionTrack>?> = _suggestionTracks

    private val _suggestionArtists = MutableStateFlow<List<SuggestionArtist>?>(null)
    val suggestionArtists: StateFlow<List<SuggestionArtist>?> = _suggestionArtists

    private val _suggestionAlbums = MutableStateFlow<List<SuggestionAlbum>?>(null)
    val suggestionAlbums: StateFlow<List<SuggestionAlbum>?> = _suggestionAlbums

    private val _suggestionVideos = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionVideos: StateFlow<List<SuggestionTrack>?> = _suggestionVideos

    private val _youtubeNewReleases = MutableStateFlow<List<AlbumItem>?>(null)
    val youtubeNewReleases: StateFlow<List<AlbumItem>?> = _youtubeNewReleases

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isManualLoading = MutableStateFlow(false)
    val isManualLoading: StateFlow<Boolean> = _isManualLoading

    private val _isTracksLoading = MutableStateFlow(false)
    val isTracksLoading: StateFlow<Boolean> = _isTracksLoading

    private val _isAlbumsLoading = MutableStateFlow(false)
    val isAlbumsLoading: StateFlow<Boolean> = _isAlbumsLoading

    private val _isVideosLoading = MutableStateFlow(false)
    val isVideosLoading: StateFlow<Boolean> = _isVideosLoading

    init {
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[SuggestionRegionKey] ?: "system" }
                .distinctUntilChanged()
                .collect { refresh(countryCode = it, force = false) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            var wasOffline = false
            networkConnectivityObserver.networkStatus.collect { isConnected ->
                if (isConnected) {
                    val hasData = !_suggestionTracks.value.isNullOrEmpty() ||
                        !_suggestionAlbums.value.isNullOrEmpty() ||
                        !_suggestionVideos.value.isNullOrEmpty() ||
                        !_youtubeNewReleases.value.isNullOrEmpty()
                    if (wasOffline || (!hasData && !_isLoading.value)) {
                        val regionCode = context.dataStore.data.first()[SuggestionRegionKey] ?: "system"
                        refresh(countryCode = regionCode, force = true)
                    }
                    wasOffline = false
                } else {
                    wasOffline = true
                }
            }
        }
    }

    fun refresh(countryCode: String = "system", force: Boolean = false) {
        val resolvedCode = if (countryCode == "system") {
            Locale.getDefault().country.lowercase().ifBlank { "us" }
        } else {
            countryCode.lowercase()
        }

        if (!force && currentLoadedRegion == resolvedCode) return

        refreshJob?.cancel()
        currentLoadedRegion = resolvedCode

        _isLoading.value = true
        _isTracksLoading.value = true
        _isAlbumsLoading.value = true
        _isVideosLoading.value = true
        if (force) _isManualLoading.value = true

        _suggestionTracks.value = null
        _suggestionArtists.value = null
        _suggestionAlbums.value = null
        _suggestionVideos.value = null
        if (force) {
            _youtubeNewReleases.value = null
        }

        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                coroutineScope {
                    launch {
                        try {
                            val tracks = AppleMusicScraper.fetchTopSongs(context, resolvedCode, force)
                            _suggestionTracks.value = tracks
                            _suggestionArtists.value = if (tracks.isNotEmpty()) {
                                AppleMusicScraper.getTrendingArtists(tracks)
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch songs", e)
                            _suggestionTracks.value = emptyList()
                            _suggestionArtists.value = emptyList()
                        } finally {
                            _isTracksLoading.value = false
                        }
                    }
                    launch {
                        try {
                            _suggestionAlbums.value = AppleMusicScraper.fetchTopAlbums(context, resolvedCode, force)
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch albums", e)
                            _suggestionAlbums.value = emptyList()
                        } finally {
                            _isAlbumsLoading.value = false
                        }
                    }
                    launch {
                        try {
                            _suggestionVideos.value = AppleMusicScraper.fetchTopVideos(context, resolvedCode, force)
                        } catch (e: Exception) {
                            Log.e("SuggestionsViewModel", "Failed to fetch videos", e)
                            _suggestionVideos.value = emptyList()
                        } finally {
                            _isVideosLoading.value = false
                        }
                    }
                    if (_youtubeNewReleases.value == null) {
                        launch {
                            runCatching {
                                YouTube.newReleaseAlbums().onSuccess { albums ->
                                    val artists = mutableMapOf<Int, String>()
                                    val favArtists = mutableMapOf<Int, String>()
                                    database.allArtistsByPlayTime().first().let { list ->
                                        var favIdx = 0
                                        for ((idx, artist) in list.withIndex()) {
                                            artists[idx] = artist.id
                                            if (artist.artist.bookmarkedAt != null) favArtists[favIdx++] = artist.id
                                        }
                                    }
                                    _youtubeNewReleases.value = albums.sortedBy { album ->
                                        album.artists.orEmpty().mapNotNull { it.id }.firstNotNullOfOrNull { id ->
                                            if (id in favArtists.values) favArtists.entries.firstOrNull { it.value == id }?.key
                                            else artists.entries.firstOrNull { it.value == id }?.key
                                        } ?: Int.MAX_VALUE
                                    }.filterExplicit(context.dataStore.get(HideExplicitKey, false))
                                }.onFailure {
                                    if (_youtubeNewReleases.value == null) _youtubeNewReleases.value = emptyList()
                                }
                            }.onFailure {
                                Log.e("SuggestionsViewModel", "Failed to fetch YouTube new releases", it)
                                if (_youtubeNewReleases.value == null) _youtubeNewReleases.value = emptyList()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SuggestionsViewModel", "Failed to fetch suggestions", e)
            } finally {
                _isTracksLoading.value = false
                _isAlbumsLoading.value = false
                _isVideosLoading.value = false
                _isLoading.value = false
                _isManualLoading.value = false
            }
        }
    }

    private fun artistMatches(ytArtistName: String, appleMusicArtist: String): Boolean {
        val ytNorm = ytArtistName.trim().lowercase()
        val apNorm = appleMusicArtist.trim().lowercase()
        return apNorm.contains(ytNorm) || ytNorm.contains(apNorm)
    }

    private fun playFromSearch(
        title: String,
        artist: String,
        filter: YouTube.SearchFilter,
        playerConnection: PlayerConnection?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.search("$title $artist", filter).onSuccess { res ->
                val songs = res.items.filterIsInstance<SongItem>()
                val bestMatch = songs.firstOrNull { s ->
                    s.title.equals(title, true) && s.artists.any { artistMatches(it.name, artist) }
                } ?: songs.firstOrNull { s ->
                    s.title.contains(title, true) && s.artists.any { artistMatches(it.name, artist) }
                } ?: songs.firstOrNull { s ->
                    s.artists.any { artistMatches(it.name, artist) }
                }

                withContext(Dispatchers.Main) {
                    if (bestMatch != null) {
                        playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = bestMatch.id)))
                    } else {
                        Toast.makeText(context, "\"$title\" is not available on YouTube Music", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun playTrack(track: SuggestionTrack, playerConnection: PlayerConnection?) =
        playFromSearch(track.title, track.artist, YouTube.SearchFilter.FILTER_SONG, playerConnection)

    fun playVideo(video: SuggestionTrack, playerConnection: PlayerConnection?) =
        playFromSearch(video.title, video.artist, YouTube.SearchFilter.FILTER_VIDEO, playerConnection)

    fun navigateToArtist(artist: SuggestionArtist, navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.search(artist.name, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { res ->
                res.items.filterIsInstance<ArtistItem>().firstOrNull()?.let {
                    withContext(Dispatchers.Main) { navController.navigate("artist/${it.id}") }
                }
            }
        }
    }

    fun navigateToAlbum(album: SuggestionAlbum, navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            YouTube.search("${album.title} ${album.artist}", YouTube.SearchFilter.FILTER_ALBUM).onSuccess { res ->
                res.items.filterIsInstance<AlbumItem>().firstOrNull()?.let {
                    withContext(Dispatchers.Main) { navController.navigate("album/${it.id}") }
                }
            }
        }
    }
}

private enum class TrendingCategoryFilter { ALL, SONGS, ALBUMS, VIDEOS }

// --- Main Screen (Material Design 3 Expressive) ---
@Composable
fun AppleMusicTrendingScreen(
    navController: NavController,
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val suggestionTracks by viewModel.suggestionTracks.collectAsState()
    val suggestionArtists by viewModel.suggestionArtists.collectAsState()
    val suggestionAlbums by viewModel.suggestionAlbums.collectAsState()
    val suggestionVideos by viewModel.suggestionVideos.collectAsState()
    val youtubeNewReleases by viewModel.youtubeNewReleases.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isManualLoading by viewModel.isManualLoading.collectAsState()
    val isTracksLoading by viewModel.isTracksLoading.collectAsState()
    val isAlbumsLoading by viewModel.isAlbumsLoading.collectAsState()
    val isVideosLoading by viewModel.isVideosLoading.collectAsState()

    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val (regionCode, setRegionCode) = rememberPreference(SuggestionRegionKey, "system")
    val resolvedRegionCode = remember(regionCode) {
        if (regionCode == "system") Locale.getDefault().country.lowercase().ifBlank { "us" }
        else regionCode.lowercase()
    }
    val resolvedRegionName = remember(resolvedRegionCode) {
        SuggestionRegionSlugToName[resolvedRegionCode] ?: resolvedRegionCode.uppercase()
    }

    var showRegionSheet by rememberSaveable { mutableStateOf(false) }
    var selectedFilter by rememberSaveable { mutableStateOf(TrendingCategoryFilter.ALL) }

    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val hasAnyContent = !suggestionTracks.isNullOrEmpty() || !suggestionArtists.isNullOrEmpty() ||
        !suggestionAlbums.isNullOrEmpty() || !suggestionVideos.isNullOrEmpty() || !youtubeNewReleases.isNullOrEmpty()

    val infiniteRefreshTransition = rememberInfiniteTransition(label = "refresh_spin")
    val refreshRotation by infiniteRefreshTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "refresh_rotation"
    )

    val onTrackClick: (SuggestionTrack) -> Unit = { track ->
        Toast.makeText(context, context.getString(R.string.loading_item, track.title), Toast.LENGTH_SHORT).show()
        viewModel.playTrack(track, playerConnection)
    }
    val onAlbumClick: (SuggestionAlbum) -> Unit = { album ->
        Toast.makeText(context, context.getString(R.string.loading_item, album.title), Toast.LENGTH_SHORT).show()
        viewModel.navigateToAlbum(album, navController)
    }
    val onVideoClick: (SuggestionTrack) -> Unit = { video ->
        Toast.makeText(context, context.getString(R.string.loading_item, video.title), Toast.LENGTH_SHORT).show()
        viewModel.playVideo(video, playerConnection)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.apple_music_trending),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    AtIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh(regionCode, force = true) },
                        enabled = !isLoading
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.sync),
                            contentDescription = stringResource(R.string.sync),
                            modifier = Modifier.graphicsLayer {
                                rotationZ = if (isLoading) refreshRotation else 0f
                            }
                        )
                    }

                    Surface(
                        onClick = { showRegionSheet = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(painterResource(R.drawable.language), contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = resolvedRegionCode.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .pullToRefresh(
                    state = pullToRefreshState,
                    isRefreshing = isManualLoading,
                    onRefresh = { viewModel.refresh(regionCode, force = true) }
                )
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.38f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = playerBottom + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        suggestionTracks?.firstOrNull()?.let { topTrack ->
                            TrendingSpotlightCard(
                                regionName = resolvedRegionName,
                                topTrack = topTrack,
                                onClick = { onTrackClick(topTrack) }
                            )
                        }
                        TrendingCategoryFilterBar(
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it }
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(0.62f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 96.dp)
                    ) {
                        trendingContentItems(
                            isLoading = isLoading,
                            isTracksLoading = isTracksLoading,
                            isAlbumsLoading = isAlbumsLoading,
                            isVideosLoading = isVideosLoading,
                            hasAnyContent = hasAnyContent,
                            selectedFilter = selectedFilter,
                            resolvedRegionCode = resolvedRegionCode,
                            suggestionTracks = suggestionTracks,
                            youtubeNewReleases = youtubeNewReleases,
                            suggestionArtists = suggestionArtists,
                            suggestionAlbums = suggestionAlbums,
                            suggestionVideos = suggestionVideos,
                            onTrackClick = onTrackClick,
                            onMoreSongsClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts") },
                            onNewReleaseClick = { album ->
                                Toast.makeText(context, context.getString(R.string.loading_item, album.title), Toast.LENGTH_SHORT).show()
                                navController.navigate("album/${album.id}")
                            },
                            onMoreNewReleasesClick = { navController.navigate("new_release") },
                            onArtistClick = { artist ->
                                Toast.makeText(context, context.getString(R.string.loading_item, artist.name), Toast.LENGTH_SHORT).show()
                                viewModel.navigateToArtist(artist, navController)
                            },
                            onAlbumClick = onAlbumClick,
                            onMoreAlbumsClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts/albums") },
                            onVideoClick = onVideoClick,
                            onMoreVideosClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts/videos") },
                            onRefreshClick = { viewModel.refresh(regionCode, force = true) },
                            horizontalInset = 0.dp
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 96.dp)
                ) {
                    suggestionTracks?.firstOrNull()?.let { topTrack ->
                        item(key = "hero_spotlight") {
                            TrendingSpotlightCard(
                                regionName = resolvedRegionName,
                                topTrack = topTrack,
                                onClick = { onTrackClick(topTrack) },
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    item(key = "category_filter") {
                        TrendingCategoryFilterBar(
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    trendingContentItems(
                        isLoading = isLoading,
                        isTracksLoading = isTracksLoading,
                        isAlbumsLoading = isAlbumsLoading,
                        isVideosLoading = isVideosLoading,
                        hasAnyContent = hasAnyContent,
                        selectedFilter = selectedFilter,
                        resolvedRegionCode = resolvedRegionCode,
                        suggestionTracks = suggestionTracks,
                        youtubeNewReleases = youtubeNewReleases,
                        suggestionArtists = suggestionArtists,
                        suggestionAlbums = suggestionAlbums,
                        suggestionVideos = suggestionVideos,
                        onTrackClick = onTrackClick,
                        onMoreSongsClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts") },
                        onNewReleaseClick = { album ->
                            Toast.makeText(context, context.getString(R.string.loading_item, album.title), Toast.LENGTH_SHORT).show()
                            navController.navigate("album/${album.id}")
                        },
                        onMoreNewReleasesClick = { navController.navigate("new_release") },
                        onArtistClick = { artist ->
                            Toast.makeText(context, context.getString(R.string.loading_item, artist.name), Toast.LENGTH_SHORT).show()
                            viewModel.navigateToArtist(artist, navController)
                        },
                        onAlbumClick = onAlbumClick,
                        onMoreAlbumsClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts/albums") },
                        onVideoClick = onVideoClick,
                        onMoreVideosClick = { uriHandler.openUri("https://music.apple.com/$resolvedRegionCode/charts/videos") },
                        onRefreshClick = { viewModel.refresh(regionCode, force = true) },
                        horizontalInset = 20.dp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        val s = if (isManualLoading) 1f
                        else LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction).coerceIn(0f, 1f)
                        scaleX = s
                        scaleY = s
                    }
            ) {
                PullToRefreshDefaults.LoadingIndicator(state = pullToRefreshState, isRefreshing = isManualLoading)
            }

            HideOnScrollFAB(
                visible = !suggestionTracks.isNullOrEmpty(),
                lazyListState = listState,
                icon = R.drawable.shuffle,
                onClick = { suggestionTracks?.randomOrNull()?.let(onTrackClick) }
            )
        }
    }

    if (showRegionSheet) {
        SuggestionRegionSheet(
            currentRegionSlug = regionCode,
            onRegionSelected = { setRegionCode(it) },
            onDismiss = { showRegionSheet = false }
        )
    }
}

private fun LazyListScope.trendingContentItems(
    isLoading: Boolean,
    isTracksLoading: Boolean,
    isAlbumsLoading: Boolean,
    isVideosLoading: Boolean,
    hasAnyContent: Boolean,
    selectedFilter: TrendingCategoryFilter,
    resolvedRegionCode: String,
    suggestionTracks: List<SuggestionTrack>?,
    youtubeNewReleases: List<AlbumItem>?,
    suggestionArtists: List<SuggestionArtist>?,
    suggestionAlbums: List<SuggestionAlbum>?,
    suggestionVideos: List<SuggestionTrack>?,
    onTrackClick: (SuggestionTrack) -> Unit,
    onMoreSongsClick: () -> Unit,
    onNewReleaseClick: (AlbumItem) -> Unit,
    onMoreNewReleasesClick: () -> Unit,
    onArtistClick: (SuggestionArtist) -> Unit,
    onAlbumClick: (SuggestionAlbum) -> Unit,
    onMoreAlbumsClick: () -> Unit,
    onVideoClick: (SuggestionTrack) -> Unit,
    onMoreVideosClick: () -> Unit,
    onRefreshClick: () -> Unit,
    horizontalInset: Dp
) {
    when (selectedFilter) {
        TrendingCategoryFilter.ALL -> {
            if (isLoading && !hasAnyContent) {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
                return
            }

            if (!hasAnyContent && !isLoading) {
                item(key = "empty_state") {
                    TrendingEmptyStateCard(
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalInset, vertical = 32.dp)
                    )
                }
                return
            }

            when {
                isTracksLoading && suggestionTracks.isNullOrEmpty() -> {
                    item(key = "songs_loading_section") {
                        TrendingSongsSectionHeader(trackCount = 0, horizontalInset = horizontalInset)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                        Spacer(Modifier.height(14.dp))
                    }
                }
                !suggestionTracks.isNullOrEmpty() -> {
                    item(key = "songs_section") {
                        TrendingSongsPagerSection(
                            tracks = suggestionTracks,
                            horizontalInset = horizontalInset,
                            onTrackClick = onTrackClick,
                            onMoreClick = onMoreSongsClick
                        )
                    }
                }
                else -> {
                    item(key = "songs_empty_section") {
                        TrendingSongsSectionHeader(trackCount = 0, horizontalInset = horizontalInset)
                        TrendingSectionEmptyCard(
                            onRefreshClick = onRefreshClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalInset)
                                .padding(bottom = 14.dp)
                        )
                    }
                }
            }

            youtubeNewReleases?.takeIf { it.isNotEmpty() }?.let { albums ->
                item(key = "yt_new_releases") {
                    YouTubeLatestAlbumsSection(
                        albums = albums,
                        horizontalInset = horizontalInset,
                        onAlbumClick = onNewReleaseClick,
                        onMoreClick = onMoreNewReleasesClick
                    )
                }
            }

            suggestionArtists?.takeIf { it.isNotEmpty() }?.let { artists ->
                item(key = "artists_section") {
                    TopArtistsSection(
                        artists = artists,
                        horizontalInset = horizontalInset,
                        onArtistClick = onArtistClick
                    )
                }
            }

            when {
                isAlbumsLoading && suggestionAlbums.isNullOrEmpty() -> {
                    item(key = "albums_loading_section") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                }
                !suggestionAlbums.isNullOrEmpty() -> {
                    item(key = "albums_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalInset + 4.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.trending_albums),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                onClick = onMoreAlbumsClick,
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = stringResource(R.string.more),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    items(
                        items = suggestionAlbums.chunked(2),
                        key = { row -> "album_row_${row.first().rank}" }
                    ) { rowAlbums ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalInset)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowAlbums.forEach { album ->
                                TrendingAlbumGridCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowAlbums.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    item(key = "albums_more_footer") {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = horizontalInset)
                                .padding(bottom = 14.dp)
                        ) {
                            ViewMoreAppleMusicCard(
                                shape = RoundedCornerShape(20.dp),
                                onClick = onMoreAlbumsClick
                            )
                        }
                    }
                }
            }

            when {
                isVideosLoading && suggestionVideos.isNullOrEmpty() -> {
                    item(key = "videos_loading_section") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                }
                !suggestionVideos.isNullOrEmpty() -> {
                    item(key = "videos_section") {
                        TrendingVideosSection(
                            videos = suggestionVideos,
                            horizontalInset = horizontalInset,
                            onVideoClick = onVideoClick,
                            onMoreClick = onMoreVideosClick
                        )
                    }
                }
            }
        }

        TrendingCategoryFilter.SONGS -> {
            if (isTracksLoading && suggestionTracks.isNullOrEmpty()) {
                item(key = "songs_tab_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
                return
            }

            if (suggestionTracks.isNullOrEmpty()) {
                item(key = "songs_tab_empty") {
                    TrendingEmptyStateCard(
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalInset, vertical = 32.dp)
                    )
                }
                return
            }

            item(key = "songs_header") {
                TrendingSongsSectionHeader(
                    trackCount = suggestionTracks.size,
                    horizontalInset = horizontalInset
                )
            }
            val totalItems = suggestionTracks.size + 1
            items(totalItems, key = { idx ->
                if (idx < suggestionTracks.size) "song_${suggestionTracks[idx].rank}_${suggestionTracks[idx].title}" else "songs_more"
            }) { idx ->
                val isFirst = idx == 0
                val isLast = idx == totalItems - 1
                val shape = RoundedCornerShape(
                    topStart = if (isFirst) 22.dp else 6.dp,
                    topEnd = if (isFirst) 22.dp else 6.dp,
                    bottomStart = if (isLast) 22.dp else 6.dp,
                    bottomEnd = if (isLast) 22.dp else 6.dp
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalInset)
                        .padding(bottom = if (isLast) 14.dp else 3.dp)
                ) {
                    if (idx == suggestionTracks.size) {
                        ViewMoreAppleMusicCard(shape = shape, onClick = onMoreSongsClick)
                    } else {
                        val track = suggestionTracks[idx]
                        TrendingSongRow(
                            track = track,
                            shape = shape,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }

        TrendingCategoryFilter.ALBUMS -> {
            if (isAlbumsLoading && suggestionAlbums.isNullOrEmpty()) {
                item(key = "albums_tab_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
                return
            }

            if (suggestionAlbums.isNullOrEmpty()) {
                item(key = "albums_tab_empty") {
                    TrendingEmptyStateCard(
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalInset, vertical = 32.dp)
                    )
                }
                return
            }

            item(key = "albums_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset + 4.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.trending_albums),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        onClick = onMoreAlbumsClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.more),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            items(
                items = suggestionAlbums.chunked(2),
                key = { row -> "album_row_${row.first().rank}" }
            ) { rowAlbums ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset)
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowAlbums.forEach { album ->
                        TrendingAlbumGridCard(
                            album = album,
                            onClick = { onAlbumClick(album) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowAlbums.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item(key = "albums_more_footer") {
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalInset)
                        .padding(bottom = 14.dp)
                ) {
                    ViewMoreAppleMusicCard(
                        shape = RoundedCornerShape(20.dp),
                        onClick = onMoreAlbumsClick
                    )
                }
            }
        }

        TrendingCategoryFilter.VIDEOS -> {
            if (isVideosLoading && suggestionVideos.isNullOrEmpty()) {
                item(key = "videos_tab_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ContainedLoadingIndicator()
                    }
                }
                return
            }

            if (suggestionVideos.isNullOrEmpty()) {
                item(key = "videos_tab_empty") {
                    TrendingEmptyStateCard(
                        onRefreshClick = onRefreshClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalInset, vertical = 32.dp)
                    )
                }
                return
            }

            item(key = "videos_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset + 4.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trending Music Videos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        onClick = onMoreVideosClick,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.more),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            items(suggestionVideos, key = { "video_${it.rank}_${it.title}" }) { video ->
                TrendingVideoCard(
                    video = video,
                    onClick = { onVideoClick(video) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset)
                        .padding(bottom = 12.dp)
                        .aspectRatio(16f / 9f)
                )
            }
            item(key = "videos_more_footer") {
                Box(
                    modifier = Modifier
                        .padding(horizontal = horizontalInset)
                        .padding(bottom = 14.dp)
                ) {
                    ViewMoreAppleMusicCard(
                        shape = RoundedCornerShape(20.dp),
                        onClick = onMoreVideosClick
                    )
                }
            }
        }
    }

    item(key = "footer") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.data_from_apple_music),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "${stringResource(R.string.app_name)} • ${resolvedRegionCode.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun TrendingSpotlightCard(
    regionName: String,
    topTrack: SuggestionTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!topTrack.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = topTrack.thumbnailUrl,
                    contentDescription = topTrack.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trending_up),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "#1 • $regionName",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = topTrack.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = topTrack.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = stringResource(R.string.play),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun TrendingCategoryFilterBar(
    selected: TrendingCategoryFilter,
    onSelect: (TrendingCategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        TrendingCategoryFilter.ALL to stringResource(R.string.filter_all),
        TrendingCategoryFilter.SONGS to stringResource(R.string.songs),
        TrendingCategoryFilter.ALBUMS to stringResource(R.string.albums),
        TrendingCategoryFilter.VIDEOS to stringResource(R.string.videos)
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        items.forEachIndexed { idx, (filter, label) ->
            ToggleButton(
                checked = selected == filter,
                onCheckedChange = { if (it) onSelect(filter) },
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
                shapes = when (idx) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    items.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                }
            ) {
                Text(
                    text = label,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrendingSongsSectionHeader(
    trackCount: Int,
    horizontalInset: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalInset + 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.trending_songs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = pluralStringResource(R.plurals.n_song, trackCount, trackCount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TrendingSongsPagerSection(
    tracks: List<SuggestionTrack>,
    horizontalInset: Dp,
    onTrackClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    val totalItems = tracks.size + 1
    val pagerState = rememberPagerState(pageCount = { (totalItems + 4) / 5 })

    Column(modifier = Modifier.fillMaxWidth()) {
        TrendingSongsSectionHeader(
            trackCount = tracks.size,
            horizontalInset = horizontalInset
        )

        HorizontalPager(
            state = pagerState,
            verticalAlignment = Alignment.Top,
            contentPadding = PaddingValues(horizontal = horizontalInset),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(tween(300, easing = FastOutSlowInEasing))
        ) { page ->
            val startIdx = page * 5
            val endIdx = minOf(startIdx + 5, totalItems)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                for (i in startIdx until endIdx) {
                    val isFirst = i == startIdx
                    val isLast = i == endIdx - 1
                    val shape = RoundedCornerShape(
                        topStart = if (isFirst) 22.dp else 6.dp,
                        topEnd = if (isFirst) 22.dp else 6.dp,
                        bottomStart = if (isLast) 22.dp else 6.dp,
                        bottomEnd = if (isLast) 22.dp else 6.dp
                    )

                    if (i == tracks.size) {
                        ViewMoreAppleMusicCard(shape = shape, onClick = onMoreClick)
                    } else {
                        val track = tracks[i]
                        TrendingSongRow(
                            track = track,
                            shape = shape,
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
            }
        }

        if (pagerState.pageCount > 1) {
            Spacer(Modifier.height(10.dp))
            TrendingPagerDotsIndicator(
                pageCount = pagerState.pageCount,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TrendingSongRow(
    track: SuggestionTrack,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    val isTopThree = track.rank <= 3
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isTopThree) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.width(36.dp)
        ) {
            Text(
                text = "#${track.rank}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isTopThree) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        if (track.thumbnailUrl != null) {
            SubcomposeAsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() } },
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            painter = painterResource(R.drawable.play),
            contentDescription = stringResource(R.string.play),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun ViewMoreAppleMusicCard(
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painterResource(R.drawable.resource_public),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "View more on Apple Music",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrendingPagerDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val maxDots = 8
    val visibleCount = pageCount.coerceAtMost(maxDots)
    val animatedPage by animateFloatAsState(
        targetValue = currentPage.coerceIn(0, pageCount - 1).toFloat(),
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "dots_page"
    )
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)
    val dotDiameter = 7.dp
    val activeWidth = 20.dp
    val spacing = 6.dp
    val pad = 6.dp
    val canvasWidth = dotDiameter * (visibleCount - 1) + activeWidth + spacing * (visibleCount - 1) + pad * 2

    Canvas(modifier = modifier.width(canvasWidth).height(10.dp)) {
        val dotPx = dotDiameter.toPx()
        val activePx = activeWidth.toPx()
        val gapPx = spacing.toPx()
        val padPx = pad.toPx()
        val stepPx = dotPx + gapPx
        val maxStart = (pageCount - visibleCount).toFloat().coerceAtLeast(0f)
        val windowStart = if (pageCount <= maxDots) 0f else (animatedPage - 3f).coerceIn(0f, maxStart)
        val cameraX = windowStart * stepPx

        var cursorX = 0f
        for (i in 0 until pageCount) {
            val activeFrac = (1f - abs(i - animatedPage)).coerceIn(0f, 1f)
            val widthPx = lerp(dotPx, activePx, activeFrac)
            val relIndex = i - windowStart
            if (relIndex > -1.2f && relIndex < visibleCount + 0.2f) {
                val alpha = when {
                    relIndex < 0f -> (1f + relIndex).coerceIn(0f, 1f)
                    relIndex > visibleCount - 1f -> (visibleCount - relIndex).coerceIn(0f, 1f)
                    else -> 1f
                }
                if (alpha > 0.01f) {
                    val left = padPx + (cursorX - cameraX)
                    val baseColor = lerpColor(inactiveColor, activeColor, activeFrac)
                    drawRoundRect(
                        color = baseColor.copy(alpha = baseColor.alpha * alpha),
                        topLeft = Offset(left, (size.height - dotPx) / 2f),
                        size = Size(widthPx, dotPx),
                        cornerRadius = CornerRadius(dotPx / 2f, dotPx / 2f)
                    )
                }
            }
            cursorX += widthPx + gapPx
        }
    }
}

@Composable
private fun YouTubeLatestAlbumsSection(
    albums: List<AlbumItem>,
    horizontalInset: Dp,
    onAlbumClick: (AlbumItem) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        NavigationTitle(
            title = stringResource(R.string.new_release_albums),
            onClick = onMoreClick,
            modifier = Modifier.padding(horizontal = (horizontalInset - 12.dp).coerceAtLeast(0.dp))
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalInset),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(albums, key = { it.id }) { album ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .width(152.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onAlbumClick(album) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AsyncImage(
                            model = album.thumbnail,
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(album.artists?.joinToString { it.name }.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TopArtistsSection(
    artists: List<SuggestionArtist>,
    horizontalInset: Dp,
    onArtistClick: (SuggestionArtist) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trending_artists),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = horizontalInset + 4.dp, vertical = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalInset),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(artists, key = { "${it.rank}_${it.name}" }) { artist ->
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .width(136.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onArtistClick(artist) }
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
                            SubcomposeAsyncImage(
                                model = artist.thumbnailUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() } },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (artist.rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset((-2).dp, (-2).dp)
                            ) {
                                Text(
                                    text = "#${artist.rank}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (artist.rank == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TrendingAlbumGridCard(
    album: SuggestionAlbum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                SubcomposeAsyncImage(
                    model = album.thumbnailUrl,
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LoadingIndicator() } },
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = CircleShape,
                    color = if (album.rank <= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "#${album.rank}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (album.rank <= 3) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrendingVideosSection(
    videos: List<SuggestionTrack>,
    horizontalInset: Dp,
    onVideoClick: (SuggestionTrack) -> Unit,
    onMoreClick: () -> Unit
) {
    val carouselState = rememberCarouselState(itemCount = { videos.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalInset + 4.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trending Music Videos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                onClick = onMoreClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = stringResource(R.string.more),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 300.dp,
            itemSpacing = 10.dp,
            contentPadding = PaddingValues(horizontal = horizontalInset),
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
        ) { i ->
            val video = videos[i]
            TrendingVideoCard(
                video = video,
                onClick = { onVideoClick(video) },
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(RoundedCornerShape(24.dp))
            )
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun TrendingVideoCard(
    video: SuggestionTrack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                        startY = 180f
                    )
                )
        )
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "#${video.rank}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = video.artist,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrendingSectionEmptyCard(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.trending_up),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onRefreshClick,
                shapes = ButtonDefaults.shapes(shape = CircleShape, pressedShape = RoundedCornerShape(16.dp)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(painterResource(R.drawable.sync), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.action_retry), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TrendingEmptyStateCard(
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_trending_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.trending_up),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.check_back_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRefreshClick,
            shapes = ButtonDefaults.shapes(shape = CircleShape, pressedShape = RoundedCornerShape(20.dp)),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Icon(painterResource(R.drawable.sync), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_retry), fontWeight = FontWeight.Bold)
        }
    }
}

// --- Region Picker Sheet ---
@Composable
fun SuggestionRegionSheet(
    currentRegionSlug: String,
    onRegionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredRegions = remember(searchQuery) {
        SuggestionRegionSlugToName.toList()
            .filter { it.first != "system" }
            .filter {
                it.second.contains(searchQuery, ignoreCase = true) ||
                    it.first.contains(searchQuery, ignoreCase = true)
            }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState()

    val dismissWithSelection: (String) -> Unit = { slug ->
        onRegionSelected(slug)
        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
            if (!bottomSheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = bottomSheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.select_region),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                ) {
                    Icon(
                        painterResource(R.drawable.search),
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item(key = "system_item") {
                    RegionListItem(
                        title = stringResource(R.string.system_default),
                        code = Locale.getDefault().country.uppercase().ifBlank { "US" },
                        selected = currentRegionSlug == "system",
                        items = 1,
                        index = 0,
                        onClick = { dismissWithSelection("system") }
                    )
                    Spacer(Modifier.height(14.dp))
                }

                if (filteredRegions.isEmpty()) {
                    item(key = "empty_regions") {
                        Text(
                            text = stringResource(R.string.no_results_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    itemsIndexed(filteredRegions, key = { _, pair -> pair.first }) { index, (slug, name) ->
                        RegionListItem(
                            title = name,
                            code = slug.uppercase(),
                            selected = slug == currentRegionSlug,
                            items = filteredRegions.size,
                            index = index,
                            onClick = { dismissWithSelection(slug) }
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }
    }

    LaunchedEffect(searchQuery) {
        if (filteredRegions.isNotEmpty()) listState.scrollToItem(0)
    }
}

@Composable
private fun RegionListItem(
    title: String,
    code: String,
    selected: Boolean,
    items: Int,
    index: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val top by animateDpAsState(if (isPressed) 36.dp else if (items == 1 || index == 0) 20.dp else 4.dp, label = "top")
    val bottom by animateDpAsState(if (isPressed) 36.dp else if (items == 1 || index == items - 1) 20.dp else 4.dp, label = "bottom")

    ListItem(
        headlineContent = { Text(title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
        leadingContent = if (selected) {
            { Icon(painterResource(R.drawable.check), contentDescription = "Selected") }
        } else null,
        trailingContent = {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = code,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        },
        colors = if (selected) {
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                leadingIconColor = MaterialTheme.colorScheme.primary
            )
        } else {
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        },
        modifier = Modifier
            .clip(
                if (selected) CircleShape
                else RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
            )
            .clickable(onClick = onClick, interactionSource = interactionSource, indication = null)
    )
}
