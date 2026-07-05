package com.cgens67.avidtune.ui.screens.library

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.*
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.ui.component.*
import com.cgens67.avidtune.ui.menu.*
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.*

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface DatabaseEntryPoint {
    fun database(): MusicDatabase
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(navController: NavController) {
    val defaultFilter by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    
    val libraryFilters = remember {
        listOf(
            LibraryFilter.LIBRARY,
            LibraryFilter.PLAYLISTS,
            LibraryFilter.SONGS,
            LibraryFilter.ARTISTS,
            LibraryFilter.ALBUMS
        )
    }

    val pagerState = rememberPagerState(
        initialPage = libraryFilters.indexOf(defaultFilter).takeIf { it >= 0 } ?: 0
    ) { libraryFilters.size }

    val currentFilter = libraryFilters.getOrElse(pagerState.currentPage) { LibraryFilter.LIBRARY }

    val headerTitle = when (currentFilter) {
        LibraryFilter.LIBRARY -> stringResource(R.string.library_title)
        LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
        LibraryFilter.SONGS -> stringResource(R.string.songs)
        LibraryFilter.ARTISTS -> stringResource(R.string.artists)
        LibraryFilter.ALBUMS -> stringResource(R.string.albums)
    }

    val headerSubtitle = when (currentFilter) {
        LibraryFilter.LIBRARY -> stringResource(R.string.library_subtitle)
        LibraryFilter.PLAYLISTS -> stringResource(R.string.library_playlists_subtitle)
        LibraryFilter.SONGS -> stringResource(R.string.library_songs_subtitle)
        LibraryFilter.ARTISTS -> stringResource(R.string.library_artists_subtitle)
        LibraryFilter.ALBUMS -> stringResource(R.string.library_albums_subtitle)
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val maxHeaderHeight = 90.dp
    val maxHeaderOffsetPx = with(density) { maxHeaderHeight.toPx() }
    var headerOffsetPx by rememberSaveable { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    val newOffset = headerOffsetPx + delta
                    val oldOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-maxHeaderOffsetPx, 0f)
                    return Offset(0f, headerOffsetPx - oldOffset)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0) {
                    val newOffset = headerOffsetPx + delta
                    val oldOffset = headerOffsetPx
                    headerOffsetPx = newOffset.coerceIn(-maxHeaderOffsetPx, 0f)
                    return Offset(0f, headerOffsetPx - oldOffset)
                }
                return Offset.Zero
            }
        }
    }

    val headerHeight = maxHeaderHeight + with(density) { headerOffsetPx.toDp() }
    val progress = 1f + (headerOffsetPx / maxHeaderOffsetPx)
    val tonalStart = MaterialTheme.colorScheme.primaryContainer
    val tonalMiddle = MaterialTheme.colorScheme.secondaryContainer

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!disableBlur) {
            Box(modifier = Modifier.fillMaxWidth().height(430.dp).align(Alignment.TopCenter).drawWithCache {
                val brush = Brush.verticalGradient(
                    0f to tonalStart.copy(alpha = 0.30f),
                    0.42f to tonalMiddle.copy(alpha = 0.14f),
                    1f to Color.Transparent
                )
                onDrawBehind { drawRect(brush) }
            })
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 16.dp)
                .nestedScroll(nestedScrollConnection)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .clipToBounds()
                    .graphicsLayer { alpha = progress }
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = headerTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 32.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = headerSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            val tabListState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()

            LaunchedEffect(defaultFilter, libraryFilters) {
                val selectedFilter = defaultFilter.takeIf { it in libraryFilters } ?: LibraryFilter.LIBRARY
                val selectedPage = libraryFilters.indexOf(selectedFilter).takeIf { it >= 0 } ?: 0
                if (pagerState.currentPage != selectedPage) pagerState.scrollToPage(selectedPage)
            }

            LaunchedEffect(pagerState.currentPage, libraryFilters) {
                headerOffsetPx = 0f
                val targetPage = pagerState.currentPage.coerceIn(0, libraryFilters.lastIndex)
                val targetFilter = libraryFilters.getOrElse(targetPage) { LibraryFilter.LIBRARY }
                val tabWidth = when (targetFilter) {
                    LibraryFilter.LIBRARY -> 116.dp
                    LibraryFilter.PLAYLISTS -> 132.dp
                    LibraryFilter.SONGS -> 102.dp
                    LibraryFilter.ARTISTS -> 116.dp
                    LibraryFilter.ALBUMS -> 110.dp
                }
                val screenWidth = configuration.screenWidthDp.dp
                val targetOffsetDp = (screenWidth - tabWidth) / 2
                val targetOffsetPx = with(density) { targetOffsetDp.roundToPx() }
                tabListState.animateScrollToItem(targetPage, scrollOffset = -targetOffsetPx)
            }

            LazyRow(
                state = tabListState,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(items = libraryFilters, key = { it.name }) { filter ->
                    val page = libraryFilters.indexOf(filter)
                    val label = when (filter) {
                        LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        LibraryFilter.PLAYLISTS -> stringResource(R.string.filter_playlists)
                        LibraryFilter.SONGS -> stringResource(R.string.filter_songs)
                        LibraryFilter.ARTISTS -> stringResource(R.string.filter_artists)
                        LibraryFilter.ALBUMS -> stringResource(R.string.filter_albums)
                    }
                    val iconRes = when (filter) {
                        LibraryFilter.LIBRARY -> R.drawable.music_note
                        LibraryFilter.PLAYLISTS -> R.drawable.list
                        LibraryFilter.SONGS -> R.drawable.music_note
                        LibraryFilter.ARTISTS -> R.drawable.artist
                        LibraryFilter.ALBUMS -> R.drawable.album
                    }
                    ExpressiveTabChip(
                        label = label,
                        iconRes = iconRes,
                        selected = currentFilter == filter,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val currentScreenFilter = libraryFilters.getOrElse(page) { LibraryFilter.LIBRARY }
                val filterContent = @Composable {
                    Row {
                        ChipsRow(
                            chips = listOf(
                                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
                            ),
                            currentValue = currentScreenFilter,
                            onValueUpdate = { selectedFilter ->
                                coroutineScope.launch {
                                    val targetIndex = libraryFilters.indexOf(selectedFilter)
                                    if (targetIndex >= 0) pagerState.animateScrollToPage(targetIndex)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                when (currentScreenFilter) {
                    LibraryFilter.LIBRARY -> LibraryMixScreen(navController = navController, filterContent = filterContent)
                    LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController = navController, filterContent = filterContent)
                    LibraryFilter.SONGS -> LibrarySongsScreen(navController = navController, onDeselect = { coroutineScope.launch { pagerState.animateScrollToPage(0) } })
                    LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController = navController, onDeselect = { coroutineScope.launch { pagerState.animateScrollToPage(0) } })
                    LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController = navController, onDeselect = { coroutineScope.launch { pagerState.animateScrollToPage(0) } })
                }
            }
        }
    }
}

@Composable
fun ExpressiveTabChip(label: String, iconRes: Int, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else if (selected) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "TabChipScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TabChipBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "TabChipContentColor"
    )
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(painter = painterResource(id = iconRes), contentDescription = label, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = contentColor)
    }
}

@Composable
fun ShortcutCard(
    title: String,
    countText: String,
    iconRes: Int,
    containerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "ShortcutCardScale"
    )
    val isDark = ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val finalBgColor = Color(ColorUtils.blendARGB(MaterialTheme.colorScheme.surfaceContainer.toArgb(), iconColor.toArgb(), if (isDark) 0.08f else 0.06f))
    val iconBgColor = iconColor.copy(alpha = if (isDark) 0.16f else 0.10f)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(26.dp))
            .background(finalBgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = iconRes), contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(text = countText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun rememberArtworkGradient(thumbnailUrl: String?, fallbackColor: Color = MaterialTheme.colorScheme.surfaceVariant): List<Color> {
    val context = LocalContext.current
    var colors by remember(thumbnailUrl) { mutableStateOf(listOf(fallbackColor, fallbackColor.copy(alpha = 0.5f))) }
    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(thumbnailUrl)
            .size(128, 128)
            .allowHardware(false)
            .build()
        val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
        if (result != null) {
            val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                val dominant = palette.dominantSwatch?.rgb ?: fallbackColor.toArgb()
                val vibrant = palette.vibrantSwatch?.rgb 
                    ?: palette.mutedSwatch?.rgb 
                    ?: palette.lightVibrantSwatch?.rgb 
                    ?: fallbackColor.toArgb()
                colors = listOf(Color(dominant), Color(vibrant))
            }
        }
    }
    return colors
}

@Composable
fun rememberArtworkCardColor(thumbnailUrl: String?, fallbackColor: Color = MaterialTheme.colorScheme.surfaceVariant): Color {
    val gradientColors = rememberArtworkGradient(thumbnailUrl = thumbnailUrl, fallbackColor = fallbackColor)
    val useDarkTheme = ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    return remember(gradientColors, useDarkTheme, pureBlack) {
        val baseColor = gradientColors.firstOrNull() ?: fallbackColor
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(baseColor.toArgb(), hsv)
        if (useDarkTheme) {
            val s = (hsv[1] * 0.45f).coerceIn(0.06f, 0.20f)
            val v = if (pureBlack) 0.18f else 0.12f
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], s, v)))
        } else {
            val s = (hsv[1] * 0.30f).coerceIn(0.03f, 0.12f)
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], s, 0.95f)))
        }
    }
}

@Composable
fun ExpressivePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        content()
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).size(36.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PlaylistListCard(playlist: Playlist, onClick: () -> Unit, onPlay: () -> Unit, onMenuClick: () -> Unit) {
    val cardBgColor = rememberArtworkCardColor(thumbnailUrl = playlist.thumbnails.getOrNull(0), fallbackColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1.0f, label = "PlaylistListCardScale")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(32.dp))
            .background(cardBgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = playlist.thumbnails.getOrNull(0),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.playlist.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${playlist.songCount} ${stringResource(R.string.filter_songs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
        IconButton(
            onClick = onPlay,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.size(36.dp)
        ) {
            Icon(painter = painterResource(id = R.drawable.play), contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
            Icon(painter = painterResource(id = R.drawable.more_vert), contentDescription = null)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistGridCard(playlist: Playlist, onClick: () -> Unit, onPlay: () -> Unit, onLongClick: () -> Unit) {
    val cardBgColor = rememberArtworkCardColor(thumbnailUrl = playlist.thumbnails.getOrNull(0), fallbackColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1.0f, label = "PlaylistGridCardScale")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(32.dp))
            .background(cardBgColor)
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(26.dp))) {
            AsyncImage(
                model = playlist.thumbnails.getOrNull(0),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(id = R.drawable.play), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${playlist.songCount} ${stringResource(R.string.filter_songs)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val topSize by viewModel.topValue.collectAsState(initial = 50)

    val albumsState = viewModel.albums.collectAsState()
    val artistState = viewModel.artists.collectAsState()
    val playlistState = viewModel.playlists.collectAsState()

    var allItems = albumsState.value + artistState.value + playlistState.value
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    
    allItems = when (sortType) {
        MixSortType.CREATE_DATE -> allItems.sortedBy { item ->
            when (item) {
                is Album -> item.album.bookmarkedAt
                is Artist -> item.artist.bookmarkedAt
                is Playlist -> item.playlist.createdAt
                else -> LocalDateTime.now()
            }
        }
        MixSortType.NAME -> allItems.sortedWith(
            compareBy(collator) { item ->
                when (item) {
                    is Album -> item.album.title
                    is Artist -> item.artist.name
                    is Playlist -> item.playlist.name
                    else -> ""
                }
            }
        )
        MixSortType.LAST_UPDATED -> allItems.sortedBy { item ->
            when (item) {
                is Album -> item.album.lastUpdateTime
                is Artist -> item.artist.lastUpdateTime
                is Playlist -> item.playlist.lastUpdateTime
                else -> LocalDateTime.now()
            }
        }
    }.let { if (sortDescending) it.reversed() else it }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sort ->
                    when (sort) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                }
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { viewType = viewType.toggle() },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = if (viewType == LibraryViewType.LIST) lazyListState else rememberLazyListState(),
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            if (viewType == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "shortcuts", contentType = "shortcuts") {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    ShortcutCard(
                                        title = stringResource(R.string.liked),
                                        countText = stringResource(R.string.filter_liked),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("auto_playlist/liked") }
                                    )
                                    ShortcutCard(
                                        title = stringResource(R.string.offline),
                                        countText = stringResource(R.string.filter_downloaded),
                                        iconRes = R.drawable.offline,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("auto_playlist/downloaded") }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    ShortcutCard(
                                        title = stringResource(R.string.cached_playlist),
                                        countText = stringResource(R.string.instant_playback),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("cache_playlist/cached") }
                                    )
                                    ShortcutCard(
                                        title = stringResource(R.string.my_top) + " $topSize",
                                        countText = stringResource(R.string.most_played_sort),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("top_playlist/$topSize") }
                                    )
                                }
                            }
                        }
                    }
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                    items(items = allItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(onClick = {
                                            menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        }) {
                                            Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("local_playlist/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem()
                                )
                            }
                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    trailingContent = {
                                        IconButton(onClick = {
                                            menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                        }) {
                                            Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("artist/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem()
                                )
                            }
                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(onClick = {
                                            menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) }
                                        }) {
                                            Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem()
                                )
                            }
                            else -> {}
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "shortcuts", span = { GridItemSpan(maxLineSpan) }, contentType = "shortcuts") {
                        Column(modifier = Modifier.padding(vertical = 12.dp)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    ShortcutCard(
                                        title = stringResource(R.string.liked),
                                        countText = stringResource(R.string.filter_liked),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("auto_playlist/liked") }
                                    )
                                    ShortcutCard(
                                        title = stringResource(R.string.offline),
                                        countText = stringResource(R.string.filter_downloaded),
                                        iconRes = R.drawable.offline,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("auto_playlist/downloaded") }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    ShortcutCard(
                                        title = stringResource(R.string.cached_playlist),
                                        countText = stringResource(R.string.instant_playback),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("cache_playlist/cached") }
                                    )
                                    ShortcutCard(
                                        title = stringResource(R.string.my_top) + " $topSize",
                                        countText = stringResource(R.string.most_played_sort),
                                        iconRes = R.drawable.music_note,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.weight(1f),
                                        onClick = { navController.navigate("top_playlist/$topSize") }
                                    )
                                }
                            }
                        }
                    }
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                    items(items = allItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistGridItem(
                                    playlist = item,
                                    fillMaxWidth = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("local_playlist/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem(),
                                    context = LocalContext.current
                                )
                            }
                            is Artist -> {
                                ArtistGridItem(
                                    artist = item,
                                    fillMaxWidth = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("artist/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem(),
                                )
                            }
                            is Album -> {
                                AlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    fillMaxWidth = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${item.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) }
                                            }
                                        )
                                        .animateItem(),
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current

    val context = LocalContext.current
    val database = remember(context) {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            DatabaseEntryPoint::class.java
        ).database()
    }

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val playlists by viewModel.allPlaylists.collectAsState()
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sort ->
                    when (sort) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                    }
                }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            IconButton(
                onClick = { viewType = viewType.toggle() },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = if (viewType == LibraryViewType.LIST) lazyListState else rememberLazyListState(),
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            if (viewType == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                    items(items = playlists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { playlist ->
                        PlaylistListCard(
                            playlist = playlist,
                            onClick = {
                                if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0) {
                                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                                } else {
                                    navController.navigate("local_playlist/${playlist.id}")
                                }
                            },
                            onPlay = {
                                val pc = playerConnection ?: return@PlaylistListCard
                                coroutineScope.launch {
                                    val playlistSongs = database.playlistSongs(playlist.id).first()
                                    if (playlistSongs.isNotEmpty()) {
                                        pc.playQueue(
                                            ListQueue(
                                                title = playlist.playlist.name,
                                                items = playlistSongs.map { it.toMediaItem() }
                                            )
                                        )
                                    }
                                }
                            },
                            onMenuClick = {
                                menuState.show { PlaylistMenu(playlist = playlist, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                            }
                        )
                    }
                }
                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = { showCreatePlaylistDialog = true }
                )
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                    items(items = playlists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { playlist ->
                        PlaylistGridCard(
                            playlist = playlist,
                            onClick = {
                                if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0) {
                                    navController.navigate("online_playlist/${playlist.playlist.browseId}")
                                } else {
                                    navController.navigate("local_playlist/${playlist.id}")
                                }
                            },
                            onPlay = {
                                val pc = playerConnection ?: return@PlaylistGridCard
                                coroutineScope.launch {
                                    val playlistSongs = database.playlistSongs(playlist.id).first()
                                    if (playlistSongs.isNotEmpty()) {
                                        pc.playQueue(
                                            ListQueue(
                                                title = playlist.playlist.name,
                                                items = playlistSongs.map { it.toMediaItem() }
                                            )
                                        )
                                    }
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show { PlaylistMenu(playlist = playlist, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) }
                            }
                        )
                    }
                }
                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = { showCreatePlaylistDialog = true }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibrarySongsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val songs by viewModel.allSongs.collectAsState()
    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    LaunchedEffect(filter) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                else -> {}
            }
        }
    }

    val wrappedSongs = remember(songs) { songs.map { ItemWrapper(it) }.toMutableList() }
    var selection by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) {
                    Row {
                        Spacer(Modifier.width(12.dp))
                        FilterChip(
                            label = { Text(stringResource(R.string.songs)) },
                            selected = true,
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                            onClick = onDeselect,
                            shape = RoundedCornerShape(16.dp),
                            leadingIcon = { Icon(painter = painterResource(R.drawable.close), contentDescription = "") }
                        )
                        ChipsRow(
                            chips = listOf(
                                SongFilter.LIKED to stringResource(R.string.filter_liked),
                                SongFilter.LIBRARY to stringResource(R.string.filter_library),
                                SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded),
                            ),
                            currentValue = filter,
                            onValueUpdate = { filter = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selection) {
                            val count = wrappedSongs.count { it.isSelected }
                            IconButton(onClick = { selection = false }) {
                                Icon(painter = painterResource(R.drawable.close), contentDescription = null)
                            }
                            Text(
                                text = pluralStringResource(R.plurals.n_song, count, count),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val allSelected = count == wrappedSongs.size
                                wrappedSongs.forEach { it.isSelected = !allSelected }
                            }) {
                                Icon(
                                    painter = painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all),
                                    contentDescription = null
                                )
                            }
                            IconButton(onClick = {
                                menuState.show {
                                    SelectionSongMenu(
                                        songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
                                        onDismiss = menuState::dismiss,
                                        clearAction = { selection = false }
                                    )
                                }
                            }) {
                                Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                SortHeader(
                                    sortType = sortType,
                                    sortDescending = sortDescending,
                                    onSortTypeChange = onSortTypeChange,
                                    onSortDescendingChange = onSortDescendingChange,
                                    sortTypeText = { sort ->
                                        when (sort) {
                                            SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                            SongSortType.NAME -> R.string.sort_by_name
                                            SongSortType.ARTIST -> R.string.sort_by_artist
                                            SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                        }
                                    }
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                itemsIndexed(
                    items = wrappedSongs,
                    key = { _, item -> item.item.song.id },
                    contentType = { _, _ -> CONTENT_TYPE_SONG }
                ) { index, songWrapper ->
                    SongListItem(
                        song = songWrapper.item,
                        showInLibraryIcon = true,
                        isActive = songWrapper.item.id == mediaMetadata?.id,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(onClick = {
                                menuState.show {
                                    SongMenu(originalSong = songWrapper.item, navController = navController, onDismiss = menuState::dismiss)
                                }
                            }) {
                                Icon(painter = painterResource(R.drawable.more_vert), contentDescription = null)
                            }
                        },
                        isSelected = songWrapper.isSelected && selection,
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = context.getString(R.string.queue_all_songs),
                                                    items = songs.map { it.toMediaItem() },
                                                    startIndex = index
                                                )
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (!selection) {
                                        selection = true
                                        wrappedSongs.forEach { it.isSelected = false }
                                        songWrapper.isSelected = true
                                    }
                                }
                            )
                            .animateItem()
                    )
                }
            }
        }

        HideOnScrollFAB(
            visible = songs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.queue_all_songs),
                        items = songs.shuffled().map { it.toMediaItem() }
                    )
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibraryAlbumsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(filter) {
        if (ytmSync && filter == AlbumFilter.LIKED) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val albums by viewModel.allAlbums.collectAsState()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val filterContent = @Composable {
        Row {
            Spacer(Modifier.width(12.dp))
            FilterChip(
                label = { Text(stringResource(R.string.albums)) },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = onDeselect,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(painter = painterResource(R.drawable.close), contentDescription = "") }
            )
            ChipsRow(
                chips = listOf(
                    AlbumFilter.LIKED to stringResource(R.string.filter_liked),
                    AlbumFilter.LIBRARY to stringResource(R.string.filter_library)
                ),
                currentValue = filter,
                onValueUpdate = { filter = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sort ->
                    when (sort) {
                        AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date
                        AlbumSortType.NAME -> R.string.sort_by_name
                        AlbumSortType.ARTIST -> R.string.sort_by_artist
                        AlbumSortType.YEAR -> R.string.sort_by_year
                        AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count
                        AlbumSortType.LENGTH -> R.string.sort_by_length
                        AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time
                    }
                }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.n_album, albums.size, albums.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            IconButton(
                onClick = { viewType = viewType.toggle() },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (albums.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.album,
                            text = stringResource(R.string.library_album_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                items(items = albums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { album ->
                    LibraryAlbumListItem(
                        navController = navController,
                        menuState = menuState,
                        album = album,
                        isActive = album.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }
                item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (albums.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyPlaceholder(
                            icon = R.drawable.album,
                            text = stringResource(R.string.library_album_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                items(items = albums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { album ->
                    LibraryAlbumGridItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        album = album,
                        isActive = album.id == mediaMetadata?.album?.id,
                        isPlaying = isPlaying,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    onDeselect: () -> Unit,
    viewModel: LibraryArtistsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSortTypeKey, ArtistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(filter) {
        if (ytmSync && filter == ArtistFilter.LIKED) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val artists by viewModel.allArtists.collectAsState()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()

    val filterContent = @Composable {
        Row {
            Spacer(Modifier.width(12.dp))
            FilterChip(
                label = { Text(stringResource(R.string.artists)) },
                selected = true,
                colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface),
                onClick = onDeselect,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(painter = painterResource(R.drawable.close), contentDescription = "") }
            )
            ChipsRow(
                chips = listOf(
                    ArtistFilter.LIKED to stringResource(R.string.filter_liked),
                    ArtistFilter.LIBRARY to stringResource(R.string.filter_library)
                ),
                currentValue = filter,
                onValueUpdate = { filter = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp)
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sort ->
                    when (sort) {
                        ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        ArtistSortType.NAME -> R.string.sort_by_name
                        ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
                    }
                }
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = pluralStringResource(R.plurals.n_artist, artists.size, artists.size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            IconButton(
                onClick = { viewType = viewType.toggle() },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view
                    ),
                    contentDescription = null
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (artists.isEmpty()) {
                    item {
                        EmptyPlaceholder(
                            icon = R.drawable.artist,
                            text = stringResource(R.string.library_artist_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                items(items = artists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { artist ->
                    LibraryArtistListItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        artist = artist,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                state = lazyGridState,
                columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }
                item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (artists.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyPlaceholder(
                            icon = R.drawable.artist,
                            text = stringResource(R.string.library_artist_empty),
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                items(items = artists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { artist ->
                    LibraryArtistGridItem(
                        navController = navController,
                        menuState = menuState,
                        coroutineScope = coroutineScope,
                        artist = artist,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.cached_playlist)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.cached_playlist),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No cached songs available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
