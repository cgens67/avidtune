@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalAnimationApi::class
)

package com.cgens67.avidtune.ui.screens.library

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AlbumFilter
import com.cgens67.avidtune.constants.AlbumFilterKey
import com.cgens67.avidtune.constants.AlbumSortDescendingKey
import com.cgens67.avidtune.constants.AlbumSortType
import com.cgens67.avidtune.constants.AlbumSortTypeKey
import com.cgens67.avidtune.constants.AlbumViewTypeKey
import com.cgens67.avidtune.constants.ArtistFilter
import com.cgens67.avidtune.constants.ArtistFilterKey
import com.cgens67.avidtune.constants.ArtistSortDescendingKey
import com.cgens67.avidtune.constants.ArtistSortType
import com.cgens67.avidtune.constants.ArtistSortTypeKey
import com.cgens67.avidtune.constants.ArtistViewTypeKey
import com.cgens67.avidtune.constants.CONTENT_TYPE_ALBUM
import com.cgens67.avidtune.constants.CONTENT_TYPE_ARTIST
import com.cgens67.avidtune.constants.CONTENT_TYPE_HEADER
import com.cgens67.avidtune.constants.CONTENT_TYPE_PLAYLIST
import com.cgens67.avidtune.constants.CONTENT_TYPE_SONG
import com.cgens67.avidtune.constants.ChipSortTypeKey
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.GridItemSize
import com.cgens67.avidtune.constants.GridItemsSizeKey
import com.cgens67.avidtune.constants.GridThumbnailHeight
import com.cgens67.avidtune.constants.InnerTubeCookieKey
import com.cgens67.avidtune.constants.LibraryFilter
import com.cgens67.avidtune.constants.LibraryViewType
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.MixSortDescendingKey
import com.cgens67.avidtune.constants.MixSortType
import com.cgens67.avidtune.constants.MixSortTypeKey
import com.cgens67.avidtune.constants.PlaylistSortDescendingKey
import com.cgens67.avidtune.constants.PlaylistSortType
import com.cgens67.avidtune.constants.PlaylistSortTypeKey
import com.cgens67.avidtune.constants.PlaylistViewTypeKey
import com.cgens67.avidtune.constants.SongFilter
import com.cgens67.avidtune.constants.SongFilterKey
import com.cgens67.avidtune.constants.SongSortDescendingKey
import com.cgens67.avidtune.constants.SongSortType
import com.cgens67.avidtune.constants.SongSortTypeKey
import com.cgens67.avidtune.constants.SwipeToSongKey
import com.cgens67.avidtune.constants.YtmSyncKey
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.PlaylistEntity
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.reversed
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.LocalAlbumRadio
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.CreatePlaylistDialog
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.ui.component.IconButton as AtIconButton
import com.cgens67.avidtune.ui.component.LibraryAlbumGridItem
import com.cgens67.avidtune.ui.component.LibraryAlbumSpotlightCard
import com.cgens67.avidtune.ui.component.LibraryArtistGridItem
import com.cgens67.avidtune.ui.component.LibraryArtistSpotlightCard
import com.cgens67.avidtune.ui.component.LibraryPinnedCollectionTile
import com.cgens67.avidtune.ui.component.LibraryPlaylistFeatureCard
import com.cgens67.avidtune.ui.component.LibraryPlaylistGridItem
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.component.SortHeader
import com.cgens67.avidtune.ui.component.SwipeToSongBox
import com.cgens67.avidtune.ui.component.VerticalFastScroller
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.ArtistMenu
import com.cgens67.avidtune.ui.menu.PlaylistMenu
import com.cgens67.avidtune.ui.menu.SelectionSongMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.theme.extractThemeColor
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.getPlaylistImageUri
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.HistoryViewModel
import com.cgens67.avidtune.viewmodels.LibraryAlbumsViewModel
import com.cgens67.avidtune.viewmodels.LibraryArtistsViewModel
import com.cgens67.avidtune.viewmodels.LibraryMixViewModel
import com.cgens67.avidtune.viewmodels.LibraryPlaylistsViewModel
import com.cgens67.avidtune.viewmodels.LibrarySongsViewModel
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.Locale
import java.util.UUID

fun dummyPlaylist(name: String) = Playlist(PlaylistEntity(UUID.randomUUID().toString(), name), 0, emptyList())

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val filterBar: @Composable () -> Unit = {
        LibraryFilterPillStrip(
            currentFilter = filterType,
            onSelectFilter = { selected ->
                filterType = if (filterType == selected && selected != LibraryFilter.LIBRARY) {
                    LibraryFilter.LIBRARY
                } else {
                    selected
                }
            },
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = filterType,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (forward) 48 else -48 })
                    .togetherWith(
                        fadeOut(tween(180)) +
                            slideOutHorizontally(tween(180)) { if (forward) -48 else 48 }
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "library_tab_content"
        ) { type ->
            when (type) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(
                    navController = navController,
                    filterContent = filterBar
                )
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(
                    navController = navController,
                    filterContent = filterBar
                )
                LibraryFilter.SONGS -> LibrarySongsScreen(
                    navController = navController,
                    filterContent = filterBar
                )
                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                    navController = navController,
                    filterContent = filterBar
                )
                LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                    navController = navController,
                    filterContent = filterBar
                )
            }
        }
    }
}

// --- Shared Expressive Components for Library ---

@Composable
private fun LibraryFilterPillStrip(
    currentFilter: LibraryFilter,
    onSelectFilter: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val filters = remember {
        listOf(
            Triple(LibraryFilter.LIBRARY, R.string.filter_all, R.drawable.library_music),
            Triple(LibraryFilter.PLAYLISTS, R.string.filter_playlists, R.drawable.queue_music),
            Triple(LibraryFilter.SONGS, R.string.filter_songs, R.drawable.music_note),
            Triple(LibraryFilter.ALBUMS, R.string.filter_albums, R.drawable.album),
            Triple(LibraryFilter.ARTISTS, R.string.filter_artists, R.drawable.artist)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .horizontalFadingEdge(
                fadeWidth = 14.dp,
                fadeStart = scrollState.canScrollBackward,
                fadeEnd = scrollState.canScrollForward
            )
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { (filter, labelRes, iconRes) ->
            val isSelected = currentFilter == filter
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                animationSpec = tween(220),
                label = "lib_pill_bg"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(220),
                label = "lib_pill_fg"
            )
            val cornerRadius by animateDpAsState(
                targetValue = if (isSelected) 12.dp else 20.dp,
                animationSpec = tween(220),
                label = "lib_pill_radius"
            )

            Surface(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(cornerRadius),
                color = containerColor,
                contentColor = contentColor
            ) {
                Row(
                    modifier = Modifier
                        .animateContentSize(tween(220, easing = FastOutSlowInEasing))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(if (isSelected) R.drawable.check else iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPinnedBentoGrid(
    likedCount: Int,
    downloadedCount: Int,
    topSize: Int,
    cachedCount: Int,
    onLikedClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onTopClick: () -> Unit,
    onCachedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    val likedTitle = stringResource(R.string.liked)
    val likedSub = pluralStringResource(R.plurals.n_song, likedCount, likedCount)
    val offlineTitle = stringResource(R.string.offline)
    val offlineSub = pluralStringResource(R.plurals.n_song, downloadedCount, downloadedCount)
    val topTitle = "${stringResource(R.string.my_top)} $topSize"
    val topSub = stringResource(R.string.stats)
    val cachedTitle = stringResource(R.string.cached_playlist)
    val cachedSub = pluralStringResource(R.plurals.n_song, cachedCount, cachedCount)

    if (isLandscape) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LibraryPinnedCollectionTile(
                title = likedTitle,
                subtitle = likedSub,
                iconRes = R.drawable.favorite,
                accentColor = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onLikedClick)
            )
            LibraryPinnedCollectionTile(
                title = offlineTitle,
                subtitle = offlineSub,
                iconRes = R.drawable.offline,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onDownloadedClick)
            )
            LibraryPinnedCollectionTile(
                title = topTitle,
                subtitle = topSub,
                iconRes = R.drawable.trending_up,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onTopClick)
            )
            LibraryPinnedCollectionTile(
                title = cachedTitle,
                subtitle = cachedSub,
                iconRes = R.drawable.cached,
                accentColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onCachedClick)
            )
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibraryPinnedCollectionTile(
                    title = likedTitle,
                    subtitle = likedSub,
                    iconRes = R.drawable.favorite,
                    accentColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onLikedClick)
                )
                LibraryPinnedCollectionTile(
                    title = offlineTitle,
                    subtitle = offlineSub,
                    iconRes = R.drawable.offline,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onDownloadedClick)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibraryPinnedCollectionTile(
                    title = topTitle,
                    subtitle = topSub,
                    iconRes = R.drawable.trending_up,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onTopClick)
                )
                LibraryPinnedCollectionTile(
                    title = cachedTitle,
                    subtitle = cachedSub,
                    iconRes = R.drawable.cached,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onCachedClick)
                )
            }
        }
    }
}

@Composable
private fun LibrarySortAndViewBar(
    countText: String? = null,
    viewType: LibraryViewType? = null,
    onToggleViewType: (() -> Unit)? = null,
    extraActionIcon: Int? = null,
    extraActionDescription: String? = null,
    onExtraActionClick: (() -> Unit)? = null,
    sortHeaderContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            sortHeaderContent()
        }

        if (!countText.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        if (extraActionIcon != null && onExtraActionClick != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onExtraActionClick,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(extraActionIcon),
                        contentDescription = extraActionDescription,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (viewType != null && onToggleViewType != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onToggleViewType,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AnimatedContent(
                        targetState = viewType,
                        transitionSpec = { scaleIn(tween(220)) togetherWith scaleOut(tween(160)) },
                        label = "view_type_icon"
                    ) { vt ->
                        Icon(
                            painter = painterResource(if (vt == LibraryViewType.LIST) R.drawable.grid_view else R.drawable.list),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryPlaylistExpressiveCard(
    playlist: Playlist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customThumbUri = remember(playlist.id) { getPlaylistImageUri(context, playlist.playlist.id) }
    val subtitleText = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
        pluralStringResource(
            R.plurals.n_song,
            playlist.playlist.remoteSongCount,
            playlist.playlist.remoteSongCount
        )
    } else {
        pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
    }

    if (customThumbUri == null) {
        LibraryPlaylistFeatureCard(
            playlist = playlist,
            trailingContent = {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        )
    } else {
        var extractedGlowColor by remember(customThumbUri) { mutableStateOf(Color.Transparent) }
        val glowColor by animateColorAsState(
            targetValue = extractedGlowColor,
            animationSpec = tween(400),
            label = "customPlaylistGlow"
        )
        LaunchedEffect(customThumbUri) {
            val bitmap = runCatching {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(customThumbUri)
                        .size(128)
                        .allowHardware(false)
                        .build()
                ).drawable?.toBitmapOrNull()
            }.getOrNull() ?: return@LaunchedEffect
            extractedGlowColor = withContext(Dispatchers.Default) { bitmap.extractThemeColor() }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = RoundedCornerShape(26.dp),
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(
                            elevation = 34.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = glowColor.copy(alpha = 0.82f),
                            spotColor = glowColor.copy(alpha = 0.96f)
                        )
                ) {
                    AsyncImage(
                        model = customThumbUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = playlist.playlist.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.more_options)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyStateCard(
    @DrawableRes iconRes: Int,
    text: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_lib_pulse")
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
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
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
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onActionClick,
                shapes = ButtonDefaults.shapes(shape = CircleShape, pressedShape = RoundedCornerShape(20.dp)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(painterResource(R.drawable.add), contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LibrarySelectionFloatingToolbar(
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .widthIn(max = 360.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onClose,
                containerColor = cs.surfaceContainerHighest,
                contentColor = cs.onSurface
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = cs.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSelectAll, modifier = Modifier.size(46.dp)) {
                Icon(
                    painter = painterResource(if (allSelected) R.drawable.deselect else R.drawable.select_all),
                    contentDescription = null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(46.dp)) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.more_options),
                    tint = cs.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun Modifier.horizontalFadingEdge(
    fadeWidth: Dp = 14.dp,
    fadeStart: Boolean,
    fadeEnd: Boolean,
): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fadePx = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fadePx > 0f) {
            if (fadeStart) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startX = 0f,
                        endX = fadePx
                    ),
                    topLeft = Offset.Zero,
                    size = Size(fadePx, size.height),
                    blendMode = BlendMode.DstIn
                )
            }
            if (fadeEnd) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startX = size.width - fadePx,
                        endX = size.width
                    ),
                    topLeft = Offset(size.width - fadePx, 0f),
                    size = Size(fadePx, size.height),
                    blendMode = BlendMode.DstIn
                )
            }
        }
    }

// --- Library Mix Screen (Overview) ---

@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val topSizeStr by viewModel.topValue.collectAsState("50")
    val topSize = topSizeStr.toIntOrNull() ?: 50
    val likedCount by database.likedSongsCount().collectAsState(initial = 0)
    val downloads by downloadUtil.downloads.collectAsState()
    val downloadedCount = remember(downloads) {
        downloads.values.count { it.state == Download.STATE_COMPLETED }
    }
    val cachedCount = remember(playerConnection.service.playerCache) {
        runCatching { playerConnection.service.playerCache.keys.size }.getOrDefault(0)
    }

    val artists by viewModel.artists.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    val collator = remember { Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY } }
    val allItems = remember(albums, artists, playlists, sortType, sortDescending) {
        val combined = albums + artists + playlists
        when (sortType) {
            MixSortType.CREATE_DATE -> combined.sortedBy {
                when (it) {
                    is Album -> it.album.bookmarkedAt
                    is Artist -> it.artist.bookmarkedAt
                    is Playlist -> it.playlist.createdAt
                    is Song -> LocalDateTime.now()
                    else -> LocalDateTime.now()
                }
            }
            MixSortType.NAME -> combined.sortedWith(
                compareBy(collator) {
                    when (it) {
                        is Album -> it.album.title
                        is Artist -> it.artist.name
                        is Playlist -> it.playlist.name
                        is Song -> ""
                        else -> ""
                    }
                }
            )
            MixSortType.LAST_UPDATED -> combined.sortedBy {
                when (it) {
                    is Album -> it.album.lastUpdateTime
                    is Artist -> it.artist.lastUpdateTime
                    is Playlist -> it.playlist.lastUpdateTime
                    is Song -> LocalDateTime.now()
                    else -> LocalDateTime.now()
                }
            }
        }.reversed(sortDescending)
    }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false })
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0)
            else lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val headerContent: @Composable () -> Unit = {
        LibrarySortAndViewBar(
            countText = "${allItems.size}",
            viewType = viewType,
            onToggleViewType = { viewType = viewType.toggle() },
            extraActionIcon = R.drawable.add,
            extraActionDescription = stringResource(R.string.create_playlist),
            onExtraActionClick = { showCreatePlaylistDialog = true },
            sortHeaderContent = {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = {
                        when (it) {
                            MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                            MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                            MixSortType.NAME -> R.string.sort_by_name
                        }
                    }
                )
            }
        )
    }

    val openPlaylistMenu: (Playlist) -> Unit = { playlist ->
        menuState.show {
            if (playlist.playlist.isEditable || playlist.songCount != 0) {
                PlaylistMenu(
                    playlist = playlist,
                    coroutineScope = coroutineScope,
                    onDismiss = menuState::dismiss
                )
            } else {
                playlist.playlist.browseId?.let { browseId ->
                    YouTubePlaylistMenu(
                        playlist = PlaylistItem(
                            id = browseId,
                            title = playlist.playlist.name,
                            author = null,
                            songCountText = null,
                            thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                            playEndpoint = WatchEndpoint(
                                playlistId = browseId,
                                params = playlist.playlist.playEndpointParams
                            ),
                            shuffleEndpoint = WatchEndpoint(
                                playlistId = browseId,
                                params = playlist.playlist.shuffleEndpointParams
                            ),
                            radioEndpoint = WatchEndpoint(
                                playlistId = "RDAMPL$browseId",
                                params = playlist.playlist.radioEndpointParams
                            ),
                            isEditable = false
                        ),
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        }
    }

    val openPlaylist: (Playlist) -> Unit = { playlist ->
        if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0) {
            navController.navigate("online_playlist/${playlist.playlist.browseId}")
        } else {
            navController.navigate("local_playlist/${playlist.id}")
        }
    }

    val contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewType,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
            label = "mix_view_type"
        ) { vt ->
            if (vt == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }

                    item(key = "pinned_bento", contentType = CONTENT_TYPE_HEADER) {
                        LibraryPinnedBentoGrid(
                            likedCount = likedCount,
                            downloadedCount = downloadedCount,
                            topSize = topSize,
                            cachedCount = cachedCount,
                            onLikedClick = { navController.navigate("auto_playlist/liked") },
                            onDownloadedClick = { navController.navigate("auto_playlist/downloaded") },
                            onTopClick = { navController.navigate("top_playlist/$topSize") },
                            onCachedClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

                    if (allItems.isEmpty()) {
                        item(key = "mix_empty") {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.library_music,
                                text = stringResource(R.string.playlist_is_empty),
                                actionLabel = stringResource(R.string.create_playlist),
                                onActionClick = { showCreatePlaylistDialog = true }
                            )
                        }
                    } else {
                        items(
                            items = allItems,
                            key = { item ->
                                when (item) {
                                    is Playlist -> "playlist_${item.id}"
                                    is Artist -> "artist_${item.id}"
                                    is Album -> "album_${item.id}"
                                    is Song -> "song_${item.id}"
                                    else -> "item_${item.hashCode()}"
                                }
                            },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { item ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .animateItem()
                            ) {
                                when (item) {
                                    is Playlist -> LibraryPlaylistExpressiveCard(
                                        playlist = item,
                                        onClick = { openPlaylist(item) },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            openPlaylistMenu(item)
                                        },
                                        onMoreClick = { openPlaylistMenu(item) }
                                    )

                                    is Artist -> LibraryArtistSpotlightCard(
                                        artist = item,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(painterResource(R.drawable.more_vert), null)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(26.dp))
                                            .combinedClickable(
                                                onClick = { navController.navigate("artist/${item.id}") },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                }
                                            )
                                    )

                                    is Album -> {
                                        val isActive = item.id == mediaMetadata?.album?.id
                                        LibraryAlbumSpotlightCard(
                                            album = item,
                                            isActive = isActive,
                                            isPlaying = isPlaying,
                                            onPlay = {
                                                coroutineScope.launch {
                                                    database.albumWithSongs(item.id).firstOrNull()?.let { albumWithSongs ->
                                                        playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                                                    }
                                                }
                                            },
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            AlbumMenu(
                                                                originalAlbum = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                ) {
                                                    Icon(painterResource(R.drawable.more_vert), null)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(26.dp))
                                                .combinedClickable(
                                                    onClick = { navController.navigate("album/${item.id}") },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            AlbumMenu(
                                                                originalAlbum = item,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss
                                                            )
                                                        }
                                                    }
                                                )
                                        )
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        filterContent()
                    }

                    item(key = "pinned_bento", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        LibraryPinnedBentoGrid(
                            likedCount = likedCount,
                            downloadedCount = downloadedCount,
                            topSize = topSize,
                            cachedCount = cachedCount,
                            onLikedClick = { navController.navigate("auto_playlist/liked") },
                            onDownloadedClick = { navController.navigate("auto_playlist/downloaded") },
                            onTopClick = { navController.navigate("top_playlist/$topSize") },
                            onCachedClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        headerContent()
                    }

                    if (allItems.isEmpty()) {
                        item(key = "mix_empty", span = { GridItemSpan(maxLineSpan) }) {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.library_music,
                                text = stringResource(R.string.playlist_is_empty),
                                actionLabel = stringResource(R.string.create_playlist),
                                onActionClick = { showCreatePlaylistDialog = true }
                            )
                        }
                    } else {
                        items(
                            items = allItems,
                            key = { item ->
                                when (item) {
                                    is Playlist -> "playlist_${item.id}"
                                    is Artist -> "artist_${item.id}"
                                    is Album -> "album_${item.id}"
                                    is Song -> "song_${item.id}"
                                    else -> "item_${item.hashCode()}"
                                }
                            },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { item ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .animateItem()
                            ) {
                                when (item) {
                                    is Playlist -> LibraryPlaylistGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        playlist = item,
                                        context = context
                                    )
                                    is Artist -> LibraryArtistGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        artist = item
                                    )
                                    is Album -> LibraryAlbumGridItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        album = item,
                                        isActive = item.id == mediaMetadata?.album?.id,
                                        isPlaying = isPlaying
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Library Playlists Screen ---

@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val playlists by viewModel.allPlaylists.collectAsState()
    val topSizeStr by viewModel.topValue.collectAsState("50")
    val topSize = topSizeStr.toIntOrNull() ?: 50
    val likedCount by database.likedSongsCount().collectAsState(initial = 0)
    val downloads by downloadUtil.downloads.collectAsState()
    val downloadedCount = remember(downloads) {
        downloads.values.count { it.state == Download.STATE_COMPLETED }
    }
    val cachedCount = remember(playerConnection) {
        runCatching { playerConnection?.service?.playerCache?.keys?.size ?: 0 }.getOrDefault(0)
    }

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(Unit) {
        if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() }
    }
    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0)
            else lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
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

    val headerContent: @Composable () -> Unit = {
        LibrarySortAndViewBar(
            countText = pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size),
            viewType = viewType,
            onToggleViewType = { viewType = viewType.toggle() },
            sortHeaderContent = {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = {
                        when (it) {
                            PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                            PlaylistSortType.NAME -> R.string.sort_by_name
                            PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                            PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        }
                    }
                )
            }
        )
    }

    val openPlaylistMenu: (Playlist) -> Unit = { playlist ->
        menuState.show {
            if (playlist.playlist.isEditable || playlist.songCount != 0) {
                PlaylistMenu(
                    playlist = playlist,
                    coroutineScope = coroutineScope,
                    onDismiss = menuState::dismiss
                )
            } else {
                playlist.playlist.browseId?.let { browseId ->
                    YouTubePlaylistMenu(
                        playlist = PlaylistItem(
                            id = browseId,
                            title = playlist.playlist.name,
                            author = null,
                            songCountText = null,
                            thumbnail = playlist.thumbnails.getOrNull(0) ?: "",
                            playEndpoint = WatchEndpoint(
                                playlistId = browseId,
                                params = playlist.playlist.playEndpointParams
                            ),
                            shuffleEndpoint = WatchEndpoint(
                                playlistId = browseId,
                                params = playlist.playlist.shuffleEndpointParams
                            ),
                            radioEndpoint = WatchEndpoint(
                                playlistId = "RDAMPL$browseId",
                                params = playlist.playlist.radioEndpointParams
                            ),
                            isEditable = false
                        ),
                        coroutineScope = coroutineScope,
                        onDismiss = menuState::dismiss
                    )
                }
            }
        }
    }

    val openPlaylist: (Playlist) -> Unit = { playlist ->
        if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.remoteSongCount != 0) {
            navController.navigate("online_playlist/${playlist.playlist.browseId}")
        } else {
            navController.navigate("local_playlist/${playlist.id}")
        }
    }

    val contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewType,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
            label = "playlists_view_type"
        ) { vt ->
            if (vt == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 84.dp
                    )
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }

                    item(key = "pinned_bento", contentType = CONTENT_TYPE_HEADER) {
                        LibraryPinnedBentoGrid(
                            likedCount = likedCount,
                            downloadedCount = downloadedCount,
                            topSize = topSize,
                            cachedCount = cachedCount,
                            onLikedClick = { navController.navigate("auto_playlist/liked") },
                            onDownloadedClick = { navController.navigate("auto_playlist/downloaded") },
                            onTopClick = { navController.navigate("top_playlist/$topSize") },
                            onCachedClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

                    if (playlists.isEmpty()) {
                        item(key = "playlists_empty") {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.queue_music,
                                text = stringResource(R.string.playlist_is_empty),
                                actionLabel = stringResource(R.string.create_playlist),
                                onActionClick = { showCreatePlaylistDialog = true }
                            )
                        }
                    } else {
                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .animateItem()
                            ) {
                                LibraryPlaylistExpressiveCard(
                                    playlist = playlist,
                                    onClick = { openPlaylist(playlist) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        openPlaylistMenu(playlist)
                                    },
                                    onMoreClick = { openPlaylistMenu(playlist) }
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 84.dp
                    )
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        filterContent()
                    }

                    item(key = "pinned_bento", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        LibraryPinnedBentoGrid(
                            likedCount = likedCount,
                            downloadedCount = downloadedCount,
                            topSize = topSize,
                            cachedCount = cachedCount,
                            onLikedClick = { navController.navigate("auto_playlist/liked") },
                            onDownloadedClick = { navController.navigate("auto_playlist/downloaded") },
                            onTopClick = { navController.navigate("top_playlist/$topSize") },
                            onCachedClick = { navController.navigate("cache_playlist/cached") },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        headerContent()
                    }

                    if (playlists.isEmpty()) {
                        item(key = "playlists_empty", span = { GridItemSpan(maxLineSpan) }) {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.queue_music,
                                text = stringResource(R.string.playlist_is_empty),
                                actionLabel = stringResource(R.string.create_playlist),
                                onActionClick = { showCreatePlaylistDialog = true }
                            )
                        }
                    } else {
                        items(
                            items = playlists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_PLAYLIST }
                        ) { playlist ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .animateItem()
                            ) {
                                LibraryPlaylistGridItem(
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    playlist = playlist,
                                    context = context
                                )
                            }
                        }
                    }
                }
            }
        }

        if (viewType == LibraryViewType.LIST) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.add
            ) { showCreatePlaylistDialog = true }
        } else {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyGridState,
                icon = R.drawable.add
            ) { showCreatePlaylistDialog = true }
        }
    }
}

// --- Library Songs Screen ---

@Composable
fun LibrarySongsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibrarySongsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val downloads by LocalDownloadUtil.current.downloads.collectAsState()

    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val songs by viewModel.allSongs.collectAsState()
    var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    LaunchedEffect(filter, ytmSync) {
        if (ytmSync) {
            when (filter) {
                SongFilter.LIKED -> viewModel.syncLikedSongs()
                SongFilter.LIBRARY -> viewModel.syncLibrarySongs()
                else -> Unit
            }
        }
    }

    var selection by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }

    fun clearSelection() {
        selection = false
        selectedSongIds.clear()
    }

    LaunchedEffect(filter) {
        clearSelection()
    }

    BackHandler(enabled = selection) {
        clearSelection()
    }

    val lazyListState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val allSelected = songs.isNotEmpty() && selectedSongIds.size == songs.size
    val contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = lazyListState,
            thumbColor = MaterialTheme.colorScheme.primary,
            topContentPadding = contentPadding.calculateTopPadding(),
            endContentPadding = 6.dp
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 92.dp
                )
            ) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }

                item(key = "subfilter", contentType = CONTENT_TYPE_HEADER) {
                    val subFilters = remember {
                        listOf(
                            Triple(SongFilter.LIKED, R.string.filter_liked, R.drawable.favorite),
                            Triple(SongFilter.LIBRARY, R.string.filter_library, R.drawable.library_music),
                            Triple(SongFilter.DOWNLOADED, R.string.filter_downloaded, R.drawable.offline)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                    ) {
                        subFilters.forEachIndexed { idx, (songFilter, labelRes, iconRes) ->
                            ToggleButton(
                                checked = filter == songFilter,
                                onCheckedChange = { if (it) filter = songFilter },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics { role = Role.RadioButton },
                                shapes = when (idx) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    subFilters.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(labelRes),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    LibrarySortAndViewBar(
                        countText = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                        sortHeaderContent = {
                            SortHeader(
                                sortType = sortType,
                                sortDescending = sortDescending,
                                onSortTypeChange = onSortTypeChange,
                                onSortDescendingChange = onSortDescendingChange,
                                sortTypeText = {
                                    when (it) {
                                        SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                        SongSortType.NAME -> R.string.sort_by_name
                                        SongSortType.ARTIST -> R.string.sort_by_artist
                                        SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                                    }
                                }
                            )
                        }
                    )
                }

                if (songs.isEmpty()) {
                    item(key = "songs_empty") {
                        LibraryEmptyStateCard(
                            iconRes = when (filter) {
                                SongFilter.LIKED -> R.drawable.favorite
                                SongFilter.DOWNLOADED -> R.drawable.offline
                                SongFilter.LIBRARY -> R.drawable.music_note
                            },
                            text = stringResource(R.string.playlist_is_empty)
                        )
                    }
                } else {
                    itemsIndexed(
                        items = songs,
                        key = { _, song -> song.song.id },
                        contentType = { _, _ -> CONTENT_TYPE_SONG }
                    ) { index, song ->
                        val isFirst = index == 0
                        val isLast = index == songs.lastIndex
                        val isSelected = selection && song.id in selectedSongIds
                        val isActive = song.id == mediaMetadata?.id
                        val download = downloads[song.id]

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        ) {
                            LibrarySegmentedSongCard(
                                song = song,
                                downloadState = download?.state,
                                downloadPercent = download?.percentDownloaded ?: -1f,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                isFirst = isFirst,
                                isLast = isLast,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                isSelected = isSelected,
                                swipeEnabled = !selection,
                                onClick = {
                                    if (selection) {
                                        if (song.id in selectedSongIds) {
                                            selectedSongIds.remove(song.id)
                                            if (selectedSongIds.isEmpty()) selection = false
                                        } else {
                                            selectedSongIds.add(song.id)
                                        }
                                    } else {
                                        if (isActive) {
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
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selection = true
                                    if (song.id !in selectedSongIds) {
                                        selectedSongIds.add(song.id)
                                    }
                                },
                                onMoreClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }

                        if (!isLast) {
                            Spacer(Modifier.height(3.dp))
                        }
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = !selection && songs.isNotEmpty(),
            lazyListState = lazyListState,
            icon = R.drawable.shuffle
        ) {
            playerConnection.playQueue(
                ListQueue(
                    title = context.getString(R.string.queue_all_songs),
                    items = songs.shuffled().map { it.toMediaItem() }
                )
            )
        }

        AnimatedVisibility(
            visible = selection,
            enter = fadeIn(tween(220)) + slideInVertically { it },
            exit = fadeOut(tween(180)) + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(20f)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LibrarySelectionFloatingToolbar(
                allSelected = allSelected,
                onClose = ::clearSelection,
                onToggleSelectAll = {
                    if (allSelected) {
                        selectedSongIds.clear()
                    } else {
                        selectedSongIds.clear()
                        selectedSongIds.addAll(songs.map { it.id })
                    }
                },
                onOpenMenu = {
                    val selected = songs.filter { it.id in selectedSongIds }
                    if (selected.isNotEmpty()) {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = selected,
                                onDismiss = menuState::dismiss,
                                clearAction = ::clearSelection
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun LibrarySegmentedSongCard(
    song: Song,
    downloadState: Int?,
    downloadPercent: Float,
    thumbnailCornerRadius: Float,
    isFirst: Boolean,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    swipeEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val swipePref by rememberPreference(SwipeToSongKey, defaultValue = false)

    val cardShape = if (isSelected) {
        RoundedCornerShape(20.dp)
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) 22.dp else 6.dp,
            topEnd = if (isFirst) 22.dp else 6.dp,
            bottomStart = if (isLast) 22.dp else 6.dp,
            bottomEnd = if (isLast) 22.dp else 6.dp
        )
    }

    val rowBackgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isActive -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "lib_song_card_bg"
    )

    val titleColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isActive -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        isActive -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val rowContent: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight + 4.dp)
                .clip(cardShape)
                .background(rowBackgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(thumbnailCornerRadius.dp.coerceAtMost(14.dp)))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl?.resize(200, 200),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isActive) {
                        PlayingIndicatorBox(
                            isActive = true,
                            playWhenReady = isPlaying,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        )
                    }

                    if (isSelected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = titleColor
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (song.song.liked) {
                            Icon(
                                painter = painterResource(R.drawable.favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (song.song.inLibrary != null) {
                            Icon(
                                painter = painterResource(R.drawable.library_add_check),
                                contentDescription = null,
                                tint = subtitleColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                Icon(
                                    painter = painterResource(R.drawable.offline),
                                    contentDescription = null,
                                    tint = subtitleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                if (downloadPercent > 0f) {
                                    CircularProgressIndicator(
                                        progress = { downloadPercent / 100f },
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.song.duration * 1000L)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = stringResource(R.string.more_options),
                        tint = subtitleColor
                    )
                }
            }
        }
    }

    if (swipeEnabled && swipePref) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = modifier
                .fillMaxWidth()
                .clip(cardShape),
            contentBackgroundColor = rowBackgroundColor
        ) {
            rowContent()
        }
    } else {
        Box(modifier = modifier.fillMaxWidth()) {
            rowContent()
        }
    }
}

// --- Library Albums Screen ---

@Composable
fun LibraryAlbumsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryAlbumsViewModel = hiltViewModel()
) {
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(filter, ytmSync) {
        if (ytmSync && filter == AlbumFilter.LIKED) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val albums by viewModel.allAlbums.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0)
            else lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val subFilterRow: @Composable () -> Unit = {
        val subFilters = remember {
            listOf(
                Triple(AlbumFilter.LIKED, R.string.filter_liked, R.drawable.favorite),
                Triple(AlbumFilter.LIBRARY, R.string.filter_library, R.drawable.library_music)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            subFilters.forEachIndexed { idx, (albumFilter, labelRes, iconRes) ->
                ToggleButton(
                    checked = filter == albumFilter,
                    onCheckedChange = { if (it) filter = albumFilter },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = if (idx == 0) {
                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                    } else {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    }
                ) {
                    Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(labelRes),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val headerContent: @Composable () -> Unit = {
        LibrarySortAndViewBar(
            countText = pluralStringResource(R.plurals.n_album, albums.size, albums.size),
            viewType = viewType,
            onToggleViewType = { viewType = viewType.toggle() },
            sortHeaderContent = {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = {
                        when (it) {
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
            }
        )
    }

    val contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewType,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
            label = "albums_view_type"
        ) { vt ->
            if (vt == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "subfilter", contentType = CONTENT_TYPE_HEADER) { subFilterRow() }
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

                    if (albums.isEmpty()) {
                        item(key = "albums_empty") {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.album,
                                text = stringResource(R.string.library_album_empty)
                            )
                        }
                    } else {
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ALBUM }
                        ) { album ->
                            val isActive = album.id == mediaMetadata?.album?.id
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .animateItem()
                            ) {
                                LibraryAlbumSpotlightCard(
                                    album = album,
                                    isActive = isActive,
                                    isPlaying = isPlaying,
                                    onPlay = {
                                        coroutineScope.launch {
                                            database.albumWithSongs(album.id).firstOrNull()?.let { albumWithSongs ->
                                                playerConnection.playQueue(LocalAlbumRadio(albumWithSongs))
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = album,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(painterResource(R.drawable.more_vert), null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(26.dp))
                                        .combinedClickable(
                                            onClick = { navController.navigate("album/${album.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = album,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        filterContent()
                    }
                    item(key = "subfilter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        subFilterRow()
                    }
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        headerContent()
                    }

                    if (albums.isEmpty()) {
                        item(key = "albums_empty", span = { GridItemSpan(maxLineSpan) }) {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.album,
                                text = stringResource(R.string.library_album_empty)
                            )
                        }
                    } else {
                        items(
                            items = albums,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ALBUM }
                        ) { album ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .animateItem()
                            ) {
                                LibraryAlbumGridItem(
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    album = album,
                                    isActive = album.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Library Artists Screen ---

@Composable
fun LibraryArtistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryArtistsViewModel = hiltViewModel()
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID)
    var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSortTypeKey, ArtistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    LaunchedEffect(filter, ytmSync) {
        if (ytmSync && filter == ArtistFilter.LIKED) {
            withContext(Dispatchers.IO) { viewModel.sync() }
        }
    }

    val artists by viewModel.allArtists.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0)
            else lazyGridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val subFilterRow: @Composable () -> Unit = {
        val subFilters = remember {
            listOf(
                Triple(ArtistFilter.LIKED, R.string.filter_liked, R.drawable.favorite),
                Triple(ArtistFilter.LIBRARY, R.string.filter_library, R.drawable.library_music)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            subFilters.forEachIndexed { idx, (artistFilter, labelRes, iconRes) ->
                ToggleButton(
                    checked = filter == artistFilter,
                    onCheckedChange = { if (it) filter = artistFilter },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.RadioButton },
                    shapes = if (idx == 0) {
                        ButtonGroupDefaults.connectedLeadingButtonShapes()
                    } else {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    }
                ) {
                    Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(labelRes),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    val headerContent: @Composable () -> Unit = {
        LibrarySortAndViewBar(
            countText = pluralStringResource(R.plurals.n_artist, artists.size, artists.size),
            viewType = viewType,
            onToggleViewType = { viewType = viewType.toggle() },
            sortHeaderContent = {
                SortHeader(
                    sortType = sortType,
                    sortDescending = sortDescending,
                    onSortTypeChange = onSortTypeChange,
                    onSortDescendingChange = onSortDescendingChange,
                    sortTypeText = {
                        when (it) {
                            ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date
                            ArtistSortType.NAME -> R.string.sort_by_name
                            ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count
                            ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time
                        }
                    }
                )
            }
        )
    }

    val contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewType,
            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(220)) },
            label = "artists_view_type"
        ) { vt ->
            if (vt == LibraryViewType.LIST) {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }
                    item(key = "subfilter", contentType = CONTENT_TYPE_HEADER) { subFilterRow() }
                    item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }

                    if (artists.isEmpty()) {
                        item(key = "artists_empty") {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.artist,
                                text = stringResource(R.string.library_artist_empty)
                            )
                        }
                    } else {
                        items(
                            items = artists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ARTIST }
                        ) { artist ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 5.dp)
                                    .animateItem()
                            ) {
                                LibraryArtistSpotlightCard(
                                    artist = artist,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = artist,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(painterResource(R.drawable.more_vert), null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(26.dp))
                                        .combinedClickable(
                                            onClick = { navController.navigate("artist/${artist.id}") },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = artist,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
                    contentPadding = PaddingValues(
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding() + 20.dp
                    )
                ) {
                    item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        filterContent()
                    }
                    item(key = "subfilter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        subFilterRow()
                    }
                    item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) {
                        headerContent()
                    }

                    if (artists.isEmpty()) {
                        item(key = "artists_empty", span = { GridItemSpan(maxLineSpan) }) {
                            LibraryEmptyStateCard(
                                iconRes = R.drawable.artist,
                                text = stringResource(R.string.library_artist_empty)
                            )
                        }
                    } else {
                        items(
                            items = artists,
                            key = { it.id },
                            contentType = { CONTENT_TYPE_ARTIST }
                        ) { artist ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .animateItem()
                            ) {
                                LibraryArtistGridItem(
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    artist = artist
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Overhauled Cache Playlist Screen ---

@Composable
fun CachePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val events by viewModel.events.collectAsState()
    val dbSongs by database.allSongs().collectAsState(initial = emptyList())
    val downloads by LocalDownloadUtil.current.downloads.collectAsState()

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val cachedSongIds = remember(playerConnection.service.playerCache) {
        runCatching {
            playerConnection.service.playerCache.keys.map { it.toString() }.toSet()
        }.getOrDefault(emptySet())
    }

    val allSongs = remember(events, dbSongs, cachedSongIds) {
        val fromEvents = events.values.flatten().map { it.song }
        (fromEvents + dbSongs)
            .distinctBy { it.id }
            .filter { it.id in cachedSongIds }
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) focusRequester.requestFocus()
    }

    val filteredSongs = remember(allSongs, query.text) {
        val q = query.text.trim()
        if (q.isEmpty()) allSongs
        else allSongs.filter { s ->
            s.song.title.contains(q, ignoreCase = true) ||
                s.artists.any { it.name.contains(q, ignoreCase = true) }
        }
    }

    var selection by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }

    fun clearSelection() {
        selection = false
        selectedSongIds.clear()
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
            focusManager.clearFocus()
        }
    } else if (selection) {
        BackHandler { clearSelection() }
    }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val lazyListState = rememberLazyListState()
    val landscapeScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val fabOffset = if (playerBottom > systemBottom) playerBottom - systemBottom else 0.dp

    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColor = surfaceColor.toArgb()
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val headerThumbnail = allSongs.firstOrNull()?.song?.thumbnailUrl

    LaunchedEffect(headerThumbnail) {
        if (headerThumbnail != null) {
            val result = runCatching {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(headerThumbnail)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                ).drawable
            }.getOrNull()
            val bitmap = (result as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()
                }
                gradientColors = PlayerColorExtractor.extractGradientColors(
                    palette = palette,
                    fallbackColor = fallbackColor
                )
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val gradientAlpha by remember {
        derivedStateOf {
            if (isLandscape) {
                (1f - (landscapeScrollState.value / 500f)).coerceIn(0f, 1f)
            } else {
                if (lazyListState.firstVisibleItemIndex == 0) {
                    (1f - (lazyListState.firstVisibleItemScrollOffset / 800f)).coerceIn(0f, 1f)
                } else 0f
            }
        }
    }

    val playlistTitle = stringResource(R.string.cached_playlist)
    val isCurrentCachePlaying = remember(mediaMetadata, allSongs) {
        val currentId = mediaMetadata?.id
        currentId != null && allSongs.any { it.id == currentId }
    }

    val onPlayClick: () -> Unit = {
        if (allSongs.isNotEmpty()) {
            if (isPlaying && isCurrentCachePlaying) {
                playerConnection.player.pause()
            } else if (isCurrentCachePlaying) {
                playerConnection.player.play()
            } else {
                playerConnection.playQueue(
                    ListQueue(
                        title = playlistTitle,
                        items = allSongs.map { it.toMediaItem() }
                    )
                )
            }
        }
    }

    val onShuffleClick: () -> Unit = {
        if (allSongs.isNotEmpty()) {
            playerConnection.playQueue(
                ListQueue(
                    title = playlistTitle,
                    items = allSongs.shuffled().map { it.toMediaItem() }
                )
            )
        }
    }

    val cacheTrackList: LazyListScope.() -> Unit = {
        if (filteredSongs.isEmpty()) {
            item(key = "cache_empty") {
                LibraryEmptyStateCard(
                    iconRes = if (isSearching) R.drawable.search else R.drawable.cached,
                    text = stringResource(if (isSearching) R.string.no_results_found else R.string.playlist_is_empty)
                )
            }
        } else {
            if (!isSearching) {
                item(key = "tracklist_count") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.songs),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            itemsIndexed(
                items = filteredSongs,
                key = { _, song -> song.id }
            ) { index, song ->
                val isFirst = index == 0
                val isLast = index == filteredSongs.lastIndex
                val isSelected = selection && song.id in selectedSongIds
                val isActive = song.id == mediaMetadata?.id
                val download = downloads[song.id]

                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .animateItem()
                ) {
                    LibrarySegmentedSongCard(
                        song = song,
                        downloadState = download?.state,
                        downloadPercent = download?.percentDownloaded ?: -1f,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        isFirst = isFirst,
                        isLast = isLast,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        isSelected = isSelected,
                        swipeEnabled = !selection,
                        onClick = {
                            if (selection) {
                                if (song.id in selectedSongIds) {
                                    selectedSongIds.remove(song.id)
                                    if (selectedSongIds.isEmpty()) selection = false
                                } else {
                                    selectedSongIds.add(song.id)
                                }
                            } else {
                                if (isActive) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = playlistTitle,
                                            items = filteredSongs.map { it.toMediaItem() },
                                            startIndex = index
                                        )
                                    )
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selection = true
                            if (song.id !in selectedSongIds) {
                                selectedSongIds.add(song.id)
                            }
                        },
                        onMoreClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                    isFromCache = true
                                )
                            }
                        }
                    )
                }

                if (!isLast) {
                    Spacer(Modifier.height(3.dp))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        if (gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (isLandscape) 1f else 0.65f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        val isLightMode = surfaceColor.luminance() > 0.5f

                        fun adaptColor(c: Color, alphaFactor: Float): Color {
                            val a = (gradientAlpha * alphaFactor).coerceIn(0f, 1f)
                            return if (isLightMode) {
                                if (c.luminance() < 0.25f) c.copy(alpha = a * 0.32f)
                                else c.copy(alpha = a * 0.55f)
                            } else {
                                c.copy(alpha = a)
                            }
                        }

                        val c0 = gradientColors[0]
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(adaptColor(c0, 0.70f), adaptColor(c0, 0.28f), Color.Transparent),
                                center = Offset(width * 0.5f, height * 0.25f),
                                radius = width * 0.78f
                            )
                        )
                        if (gradientColors.size >= 2) {
                            val c1 = gradientColors[1]
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(adaptColor(c1, 0.50f), Color.Transparent),
                                    center = Offset(width * 0.2f, height * 0.55f),
                                    radius = width * 0.65f
                                )
                            )
                        }
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, surfaceColor.copy(alpha = 0.7f), surfaceColor),
                                startY = height * 0.45f,
                                endY = height
                            )
                        )
                    }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        when {
                            selection -> {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = pluralStringResource(R.plurals.n_song, selectedSongIds.size, selectedSongIds.size),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            isSearching -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.search),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        BasicTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester),
                                            decorationBox = { innerTextField ->
                                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                                    if (query.text.isEmpty()) {
                                                        Text(
                                                            text = stringResource(R.string.search),
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        if (query.text.isNotEmpty()) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.clear),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .clickable { query = TextFieldValue() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            AtIconButton(
                                onClick = {
                                    when {
                                        isSearching -> {
                                            isSearching = false
                                            query = TextFieldValue()
                                            focusManager.clearFocus()
                                        }
                                        selection -> clearSelection()
                                        else -> navController.navigateUp()
                                    }
                                },
                                onLongClick = {
                                    if (!isSearching && !selection) navController.backToMain()
                                }
                            ) {
                                Icon(
                                    painter = painterResource(if (isSearching || selection) R.drawable.close else R.drawable.arrow_back),
                                    contentDescription = stringResource(R.string.back),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        if (!isSearching && !selection && allSongs.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = stringResource(R.string.search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent,
                        scrolledContainerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            val bottomClearance = playerBottom + if (selection) 96.dp else 24.dp
            if (isLandscape && !isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(
                                start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(layoutDirection) + 16.dp,
                                bottom = bottomClearance
                            )
                            .verticalScroll(landscapeScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CachePlaylistHeroHeader(
                            title = playlistTitle,
                            songs = allSongs,
                            artworkSize = 156.dp,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            coverSize = coverResolution.size,
                            isPlaying = isPlaying,
                            isCurrentCachePlaying = isCurrentCachePlaying,
                            onPlayClick = onPlayClick,
                            onShuffleClick = onShuffleClick
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        VerticalFastScroller(
                            listState = lazyListState,
                            thumbColor = MaterialTheme.colorScheme.primary,
                            topContentPadding = 8.dp,
                            endContentPadding = 6.dp
                        ) {
                            LazyColumn(
                                state = lazyListState,
                                contentPadding = PaddingValues(
                                    top = 8.dp,
                                    bottom = bottomClearance,
                                    end = LocalPlayerAwareWindowInsets.current
                                        .asPaddingValues()
                                        .calculateEndPadding(layoutDirection) + 12.dp
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                cacheTrackList()
                            }
                        }
                    }
                }
            } else {
                VerticalFastScroller(
                    listState = lazyListState,
                    thumbColor = MaterialTheme.colorScheme.primary,
                    topContentPadding = paddingValues.calculateTopPadding(),
                    endContentPadding = 6.dp
                ) {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            bottom = bottomClearance
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (!isSearching && allSongs.isNotEmpty()) {
                            item(key = "cache_header") {
                                CachePlaylistHeroHeader(
                                    title = playlistTitle,
                                    songs = allSongs,
                                    artworkSize = 220.dp,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    coverSize = coverResolution.size,
                                    isPlaying = isPlaying,
                                    isCurrentCachePlaying = isCurrentCachePlaying,
                                    onPlayClick = onPlayClick,
                                    onShuffleClick = onShuffleClick
                                )
                            }
                        }

                        cacheTrackList()
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selection,
            enter = fadeIn(tween(220)) + slideInVertically { it },
            exit = fadeOut(tween(180)) + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(21f)
                .padding(bottom = fabOffset + 16.dp, start = 16.dp, end = 16.dp)
        ) {
            LibrarySelectionFloatingToolbar(
                allSelected = filteredSongs.isNotEmpty() && selectedSongIds.size == filteredSongs.size,
                onClose = ::clearSelection,
                onToggleSelectAll = {
                    if (selectedSongIds.size == filteredSongs.size) {
                        selectedSongIds.clear()
                    } else {
                        selectedSongIds.clear()
                        selectedSongIds.addAll(filteredSongs.map { it.id })
                    }
                },
                onOpenMenu = {
                    val selected = filteredSongs.filter { it.id in selectedSongIds }
                    if (selected.isNotEmpty()) {
                        menuState.show {
                            SelectionSongMenu(
                                songSelection = selected,
                                onDismiss = menuState::dismiss,
                                clearAction = ::clearSelection
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CachePlaylistHeroHeader(
    title: String,
    songs: List<Song>,
    artworkSize: Dp,
    thumbnailCornerRadius: Float,
    coverSize: Int,
    isPlaying: Boolean,
    isCurrentCachePlaying: Boolean,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = if (artworkSize <= 160.dp) 16.dp else 32.dp
    val totalDuration = remember(songs) { songs.sumOf { it.song.duration } }
    val hours = totalDuration / 3600
    val minutes = (totalDuration % 3600) / 60
    val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    val artworkUrl = songs.firstOrNull()?.song?.thumbnailUrl

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl.resize(coverSize, coverSize),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.cached),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(artworkSize * 0.4f)
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 14.dp)
        )

        Text(
            text = listOf(
                pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                durationStr
            ).joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val playInteraction = remember { MutableInteractionSource() }
            val isPlayPressed by playInteraction.collectIsPressedAsState()
            val playRadius by animateDpAsState(
                targetValue = if (isPlayPressed) 28.dp else 16.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "cache_play_radius"
            )

            Button(
                onClick = onPlayClick,
                interactionSource = playInteraction,
                shape = RoundedCornerShape(playRadius),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(if (isPlaying && isCurrentCachePlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying && isCurrentCachePlaying) {
                        stringResource(R.string.media3_controls_pause_description)
                    } else {
                        stringResource(R.string.play)
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            val shuffleInteraction = remember { MutableInteractionSource() }
            val isShufflePressed by shuffleInteraction.collectIsPressedAsState()
            val shuffleRadius by animateDpAsState(
                targetValue = if (isShufflePressed) 28.dp else 16.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "cache_shuffle_radius"
            )

            FilledTonalButton(
                onClick = onShuffleClick,
                interactionSource = shuffleInteraction,
                shape = RoundedCornerShape(shuffleRadius),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.shuffle), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
