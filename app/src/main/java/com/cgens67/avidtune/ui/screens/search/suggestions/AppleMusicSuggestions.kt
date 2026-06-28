package com.cgens67.avidtune.ui.screens.search.suggestions

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// --- Models ---
data class SuggestionTrack(val rank: Int, val title: String, val artist: String, val thumbnailUrl: String?, val appleMusicUrl: String? = null)
data class SuggestionArtist(val rank: Int, val name: String, val thumbnailUrl: String?)
data class SuggestionAlbum(val rank: Int, val title: String, val artist: String, val thumbnailUrl: String?, val appleMusicUrl: String? = null)

val SuggestionRegionSlugToName = mapOf(
    "in" to "India", "us" to "United States", "gb" to "United Kingdom", "ca" to "Canada",
    "au" to "Australia", "jp" to "Japan", "kr" to "South Korea", "br" to "Brazil", "fr" to "France", "de" to "Germany"
)

// --- Scraper ---
object AppleMusicScraper {
    private val client by lazy { OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build() }
    private fun getJson(url: String): JSONArray? = try {
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        client.newCall(req).execute().use { if (!it.isSuccessful) null else JSONObject(it.body?.string() ?: "").getJSONObject("feed").getJSONArray("results") }
    } catch (e: Exception) { null }

    fun fetchTopSongs(cc: String = "us") = getJson("https://rss.applemarketingtools.com/api/v2/$cc/music/most-played/100/songs.json")?.let {
        List(it.length()) { i -> val o = it.getJSONObject(i); SuggestionTrack(i + 1, o.getString("name"), o.getString("artistName"), o.getString("artworkUrl100").replace("100x100", "500x500"), o.getString("url")) }
    } ?: emptyList()

    fun fetchTopAlbums(cc: String = "us") = getJson("https://rss.applemarketingtools.com/api/v2/$cc/music/most-played/50/albums.json")?.let {
        List(it.length()) { i -> val o = it.getJSONObject(i); SuggestionAlbum(i + 1, o.getString("name"), o.getString("artistName"), o.getString("artworkUrl100").replace("100x100", "500x500"), o.getString("url")) }
    } ?: emptyList()

    fun getTrendingArtists(tracks: List<SuggestionTrack>): List<SuggestionArtist> {
        val counts = mutableMapOf<String, Int>()
        val imgs = mutableMapOf<String, String?>()
        tracks.forEach { t -> val a = t.artist.split(",", "&", "feat.", "ft.").first().trim(); counts[a] = (counts[a] ?: 0) + 1; if (imgs[a] == null) imgs[a] = t.thumbnailUrl?.replace("1920x1080", "500x500") }
        return counts.toList().sortedByDescending { it.second }.take(15).mapIndexed { i, p -> SuggestionArtist(i + 1, p.first, imgs[p.first]) }
    }
}

// --- ViewModel ---
@HiltViewModel
class SuggestionsViewModel @Inject constructor() : ViewModel() {
    val suggestionTracks = MutableStateFlow<List<SuggestionTrack>?>(null)
    val suggestionArtists = MutableStateFlow<List<SuggestionArtist>?>(null)
    val suggestionAlbums = MutableStateFlow<List<SuggestionAlbum>?>(null)
    val isLoading = MutableStateFlow(false)
    private var currentLoadedRegion: String? = null

    fun refresh(countryCode: String = "us", force: Boolean = false) {
        val cc = countryCode.lowercase()
        if (isLoading.value && !force && currentLoadedRegion == cc) return
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true
            if (currentLoadedRegion != cc || force) {
                suggestionTracks.value = null; suggestionArtists.value = null; suggestionAlbums.value = null
            }
            try {
                coroutineScope {
                    launch {
                        val tracks = AppleMusicScraper.fetchTopSongs(cc)
                        if (tracks.isNotEmpty()) { suggestionTracks.value = tracks; suggestionArtists.value = AppleMusicScraper.getTrendingArtists(tracks) }
                    }
                    launch {
                        val albums = AppleMusicScraper.fetchTopAlbums(cc)
                        if (albums.isNotEmpty()) suggestionAlbums.value = albums
                    }
                }
                currentLoadedRegion = cc
            } catch (e: Exception) { e.printStackTrace() } finally { isLoading.value = false }
        }
    }

    fun playTrack(t: SuggestionTrack, p: PlayerConnection?) = viewModelScope.launch(Dispatchers.IO) {
        YouTube.search("${t.title} ${t.artist}", YouTube.SearchFilter.FILTER_SONG).onSuccess { res ->
            val songs = res.items.filterIsInstance<SongItem>()
            val best = songs.firstOrNull { s -> s.title.equals(t.title, true) && s.artists.any { a -> t.artist.contains(a.name, true) } } ?: songs.firstOrNull { s -> s.artists.any { a -> t.artist.contains(a.name, true) } } ?: songs.firstOrNull()
            best?.let { withContext(Dispatchers.Main) { p?.playQueue(YouTubeQueue(WatchEndpoint(videoId = it.id), it.toMediaMetadata())) } }
        }
    }

    fun navigateToArtist(a: SuggestionArtist, nav: NavController) = viewModelScope.launch(Dispatchers.IO) {
        YouTube.search(a.name, YouTube.SearchFilter.FILTER_ARTIST).onSuccess { res ->
            res.items.filterIsInstance<ArtistItem>().firstOrNull()?.let { artist -> withContext(Dispatchers.Main) { nav.navigate("artist/${artist.id}") } }
        }
    }

    fun navigateToAlbum(a: SuggestionAlbum, nav: NavController) = viewModelScope.launch(Dispatchers.IO) {
        YouTube.search("${a.title} ${a.artist}", YouTube.SearchFilter.FILTER_ALBUM).onSuccess { res ->
            res.items.filterIsInstance<AlbumItem>().firstOrNull()?.let { album -> withContext(Dispatchers.Main) { nav.navigate("album/${album.id}") } }
        }
    }
}

// --- Composable UI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleMusicTrendingScreen(
    navController: NavController,
    viewModel: SuggestionsViewModel = hiltViewModel()
) {
    val tracks by viewModel.suggestionTracks.collectAsState()
    val artists by viewModel.suggestionArtists.collectAsState()
    val albums by viewModel.suggestionAlbums.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val context = LocalContext.current
    
    var showRegionSheet by remember { mutableStateOf(false) }
    var regionCode by remember { mutableStateOf("us") }

    LaunchedEffect(regionCode) { viewModel.refresh(regionCode) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Setup animated region button state
    val regionInteractionSource = remember { MutableInteractionSource() }
    val isRegionPressed by regionInteractionSource.collectIsPressedAsState()
    val regionScale by animateFloatAsState(targetValue = if (isRegionPressed) 0.95f else 1f, label = "region_scale")

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.apple_music_trending)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                actions = {
                    Surface(
                        onClick = { showRegionSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        interactionSource = regionInteractionSource,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .scale(regionScale)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.language),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = (SuggestionRegionSlugToName[regionCode] ?: "US").uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading && tracks == null) {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator() 
                }
            }

            tracks?.let { t ->
                TrendingAppleMusicSection(t, onTrackClick = {
                    Toast.makeText(context, context.getString(R.string.loading_item, it.title), Toast.LENGTH_SHORT).show()
                    viewModel.playTrack(it, playerConnection)
                })
            }

            artists?.let { a ->
                TopArtistsSection(a, onArtistClick = {
                    Toast.makeText(context, context.getString(R.string.loading_item, it.name), Toast.LENGTH_SHORT).show()
                    viewModel.navigateToArtist(it, navController)
                })
            }

            albums?.let { a ->
                TrendingAlbumsSection(a, onAlbumClick = {
                    Toast.makeText(context, context.getString(R.string.loading_item, it.title), Toast.LENGTH_SHORT).show()
                    viewModel.navigateToAlbum(it, navController)
                })
            }

            if (tracks != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.data_from_apple_music), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }

    if (showRegionSheet) {
        ModalBottomSheet(onDismissRequest = { showRegionSheet = false }) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), contentPadding = PaddingValues(bottom = 32.dp)) {
                item { Text(stringResource(R.string.select_region), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)) }
                val regions = SuggestionRegionSlugToName.toList()
                itemsIndexed(regions) { index, (slug, name) ->
                    val selected = slug == regionCode
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val top by animateDpAsState(if (isPressed) 36.dp else if (regions.size == 1 || index == 0) 20.dp else 4.dp, label = "top")
                    val bottom by animateDpAsState(if (isPressed) 36.dp else if (regions.size == 1 || index == regions.size - 1) 20.dp else 4.dp, label = "bottom")

                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = if (selected) { { Icon(Icons.Default.Check, "Selected") } } else null,
                        colors = if (selected) ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(0.3f), leadingIconColor = MaterialTheme.colorScheme.primary)
                        else ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)),
                        modifier = Modifier.padding(vertical = 2.dp).clip(if (selected) CircleShape else RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                            .clickable(onClick = { regionCode = slug; showRegionSheet = false }, interactionSource = interactionSource, indication = null)
                    )
                }
            }
        }
    }
}

@Composable
fun TrendingAppleMusicSection(tracks: List<SuggestionTrack>, onTrackClick: (SuggestionTrack) -> Unit) {
    val displayTracks = tracks.take(30)
    val pagerState = rememberPagerState(pageCount = { (displayTracks.size + 4) / 5 })
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.trending_songs),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 8.dp)
        )
        HorizontalPager(state = pagerState, verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().animateContentSize(tween(300, easing = FastOutSlowInEasing))) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val startIdx = page * 5; val endIdx = minOf(startIdx + 5, displayTracks.size)
                for (i in startIdx until endIdx) {
                    val track = displayTracks[i]
                    val isTop = i == startIdx; val isBottom = i == endIdx - 1
                    val shape = when {
                        isTop && isBottom -> RoundedCornerShape(24.dp)
                        isTop -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        isBottom -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                        else -> RoundedCornerShape(4.dp)
                    }
                    
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(400, delayMillis = (i % 5) * 80)) +
                                slideInVertically(tween(400, delayMillis = (i % 5) * 80), initialOffsetY = { it / 4 })
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceContainer).clickable { onTrackClick(track) }) {
                            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                                Text(track.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
                                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)) {
                                    Text("#${track.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (track.thumbnailUrl != null) AsyncImage(model = track.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.padding(16.dp).clip(MaterialTheme.shapes.large).size(80.dp))
                        }
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    label = "color"
                )
                val width by animateDpAsState(
                    targetValue = if (isSelected) 18.dp else 7.dp,
                    label = "width"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(width = width, height = 7.dp)
                )
            }
        }
    }
}

@Composable
fun TopArtistsSection(artists: List<SuggestionArtist>, onArtistClick: (SuggestionArtist) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.trending_artists), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            itemsIndexed(artists) { index, artist ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = index * 80)) +
                            slideInHorizontally(tween(400, delayMillis = index * 80), initialOffsetX = { it / 4 })
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(100.dp).clickable { onArtistClick(artist) }) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(model = artist.thumbnailUrl, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                            Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                                Box(contentAlignment = Alignment.Center) { Text(artist.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(artist.name, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingAlbumsSection(albums: List<SuggestionAlbum>, onAlbumClick: (SuggestionAlbum) -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.trending_albums), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)) {
            itemsIndexed(albums) { index, album ->
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(400, delayMillis = index * 80)) +
                            slideInHorizontally(tween(400, delayMillis = index * 80), initialOffsetX = { it / 4 })
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp).clickable { onAlbumClick(album) }) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(model = album.thumbnailUrl, contentDescription = album.title, contentScale = ContentScale.Crop, modifier = Modifier.size(120.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                            Surface(modifier = Modifier.size(28.dp).offset((-4).dp, (-4).dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 4.dp) {
                                Box(contentAlignment = Alignment.Center) { Text(album.rank.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold) }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(album.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                        Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
