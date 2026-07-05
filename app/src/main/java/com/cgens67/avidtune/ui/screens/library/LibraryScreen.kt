@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
package com.cgens67.avidtune.ui.screens.library

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.*
import androidx.navigation.*
import androidx.navigation.compose.*
import coil.compose.*
import com.cgens67.avidtune.*
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.db.entities.*
import com.cgens67.avidtune.extensions.*
import com.cgens67.avidtune.playback.queues.*
import com.cgens67.avidtune.ui.component.*
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.menu.*
import com.cgens67.avidtune.ui.utils.*
import com.cgens67.avidtune.utils.*
import com.cgens67.avidtune.viewmodels.*
import com.cgens67.innertube.utils.*
import kotlinx.coroutines.*
import java.text.Collator
import java.time.LocalDateTime
import java.util.*

fun dummyPlaylist(name: String) = Playlist(PlaylistEntity(UUID.randomUUID().toString(), name), 0, emptyList())

@Composable fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)
    val lazyListState = rememberLazyListState()
    val filterContent = @Composable {
        Row {
            ChipsRow(
                chips = listOf(
                    LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                    LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                    LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                    LibraryFilter.ARTISTS to stringResource(R.string.filter_artists)
                ),
                currentValue = filterType,
                onValueUpdate = { filterType = if (filterType == it) LibraryFilter.LIBRARY else it },
                modifier = Modifier.weight(1f)
            )
        }
    }
    Box(Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            when (filterType) {
                LibraryFilter.LIBRARY -> LibraryMixScreen(navController = navController, filterContent = filterContent)
                LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController = navController, filterContent = filterContent)
                LibraryFilter.SONGS -> LibrarySongsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
                LibraryFilter.ALBUMS -> LibraryAlbumsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
                LibraryFilter.ARTISTS -> LibraryArtistsScreen(navController = navController, onDeselect = { filterType = LibraryFilter.LIBRARY })
            }
        }
    }
}

@Composable fun LibraryMixScreen(navController: NavController, filterContent: @Composable () -> Unit, viewModel: LibraryMixViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current; val haptic = LocalHapticFeedback.current; val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID); val (sortType, onSortTypeChange) = rememberEnumPreference(MixSortTypeKey, MixSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true); val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val topSize by viewModel.topValue.collectAsState(50)
    val likedPlaylist = dummyPlaylist(stringResource(R.string.liked)); val downloadPlaylist = dummyPlaylist(stringResource(R.string.offline)); val topPlaylist = dummyPlaylist(stringResource(R.string.my_top) + " $topSize"); val cachePlaylist = dummyPlaylist(stringResource(R.string.cached_playlist))
    var allItems = viewModel.albums.collectAsState().value + viewModel.artists.collectAsState().value + viewModel.playlists.collectAsState().value
    val collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }
    allItems = when (sortType) {
        MixSortType.CREATE_DATE -> allItems.sortedBy { when (it) { is Album -> it.album.bookmarkedAt; is Artist -> it.artist.bookmarkedAt; is Playlist -> it.playlist.createdAt; else -> LocalDateTime.now() } }
        MixSortType.NAME -> allItems.sortedWith(compareBy(collator) { when (it) { is Album -> it.album.title; is Artist -> it.artist.name; is Playlist -> it.playlist.name; else -> "" } })
        MixSortType.LAST_UPDATED -> allItems.sortedBy { when (it) { is Album -> it.album.lastUpdateTime; is Artist -> it.artist.lastUpdateTime; is Playlist -> it.playlist.lastUpdateTime; else -> LocalDateTime.now() } }
    }.reversed(sortDescending)
    val coroutineScope = rememberCoroutineScope(); val lazyListState = rememberLazyListState(); val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState(); val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0) else lazyGridState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    val headerContent = @Composable { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) { SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { when (it as MixSortType) { MixSortType.CREATE_DATE -> R.string.sort_by_create_date; MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated; MixSortType.NAME -> R.string.sort_by_name } }); Spacer(Modifier.weight(1f)); IconButton({ viewType = viewType.toggle() }, Modifier.padding(start = 6.dp, end = 6.dp)) { Icon(painterResource(if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view), null) } } }
    Box(Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                item(key = "likedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = likedPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/liked") }.animateItem()) }
                item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = downloadPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/downloaded") }.animateItem()) }
                item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = topPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("top_playlist/$topSize") }.animateItem()) }
                item(key = "cachePlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = cachePlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("cache_playlist/cached") }.animateItem()) }
                items(items = allItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                    when (item) {
                        is Playlist -> PlaylistListItem(playlist = item, trailingContent = { IconButton({ menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("local_playlist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }).animateItem())
                        is Artist -> ArtistListItem(artist = item, trailingContent = { IconButton({ menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("artist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }).animateItem())
                        is Album -> AlbumListItem(album = item, isActive = item.id == mediaMetadata?.album?.id, isPlaying = isPlaying, trailingContent = { IconButton({ menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("album/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) } }).animateItem())
                        else -> {}
                    }
                }
            }
        } else {
            LazyVerticalGrid(state = lazyGridState, columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                item(key = "likedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = likedPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("auto_playlist/liked") }).animateItem(), context = LocalContext.current) }
                item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = downloadPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("auto_playlist/downloaded") }).animateItem(), context = LocalContext.current) }
                item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = topPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("top_playlist/$topSize") }).animateItem(), context = LocalContext.current) }
                item(key = "cachePlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = cachePlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("cache_playlist/cached") }).animateItem(), context = LocalContext.current) }
                items(items = allItems, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { item ->
                    when (item) {
                        is Playlist -> PlaylistGridItem(playlist = item, fillMaxWidth = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("local_playlist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { PlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }).animateItem(), context = LocalContext.current)
                        is Artist -> ArtistGridItem(artist = item, fillMaxWidth = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("artist/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { ArtistMenu(originalArtist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss) } }).animateItem())
                        is Album -> AlbumGridItem(album = item, isActive = item.id == mediaMetadata?.album?.id, isPlaying = isPlaying, coroutineScope = coroutineScope, fillMaxWidth = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("album/${item.id}") }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { AlbumMenu(originalAlbum = item, navController = navController, onDismiss = menuState::dismiss) } }).animateItem())
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable fun LibraryPlaylistsScreen(navController: NavController, filterContent: @Composable () -> Unit, viewModel: LibraryPlaylistsViewModel = hiltViewModel(), initialTextFieldValue: String? = null, allowSyncing: Boolean = true) {
    val menuState = LocalMenuState.current; val coroutineScope = rememberCoroutineScope()
    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID); val (sortType, onSortTypeChange) = rememberEnumPreference(PlaylistSortTypeKey, PlaylistSortType.CREATE_DATE)
    val (sortDescending, onSortDescendingChange) = rememberPreference(PlaylistSortDescendingKey, true); val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val playlists by viewModel.allPlaylists.collectAsState(); val topSize by viewModel.topValue.collectAsState(50)
    val likedPlaylist = dummyPlaylist(stringResource(R.string.liked)); val downloadPlaylist = dummyPlaylist(stringResource(R.string.offline)); val topPlaylist = dummyPlaylist(stringResource(R.string.my_top) + " $topSize"); val cachePlaylist = dummyPlaylist(stringResource(R.string.cached_playlist))
    val lazyListState = rememberLazyListState(); val lazyGridState = rememberLazyGridState(); val backStackEntry by navController.currentBackStackEntryAsState(); val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, ""); remember(innerTubeCookie) { "SAPISID" in parseCookieString(innerTubeCookie) }; val (ytmSync) = rememberPreference(YtmSyncKey, true)
    LaunchedEffect(Unit) { if (ytmSync) withContext(Dispatchers.IO) { viewModel.sync() } }
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0) else lazyGridState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }; if (showCreatePlaylistDialog) CreatePlaylistDialog(onDismiss = { showCreatePlaylistDialog = false }, initialTextFieldValue = initialTextFieldValue, allowSyncing = allowSyncing)
    val headerContent = @Composable { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) { SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { when (it as PlaylistSortType) { PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date; PlaylistSortType.NAME -> R.string.sort_by_name; PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count; PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated } }); Spacer(Modifier.weight(1f)); Text(pluralStringResource(R.plurals.n_playlist, playlists.size, playlists.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary); IconButton({ viewType = viewType.toggle() }, Modifier.padding(start = 6.dp, end = 6.dp)) { Icon(painterResource(if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view), null) } } }
    Box(Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                item(key = "likedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = likedPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/liked") }.animateItem()) }
                item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = downloadPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("auto_playlist/downloaded") }.animateItem()) }
                item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistListItem(playlist = topPlaylist, autoPlaylist = true, modifier = Modifier.fillMaxWidth().clickable { navController.navigate("top_playlist/$topSize") }.animateItem()) }
                item(key = "cachePlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = cachePlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("cache_playlist/cached") }).animateItem(), context = LocalContext.current) }
                if (playlists.isEmpty()) item { }
                items(items = playlists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { LibraryPlaylistListItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, playlist = it, modifier = Modifier.animateItem()) }
            }
            HideOnScrollFAB(lazyListState = lazyListState, icon = R.drawable.add, onClick = { showCreatePlaylistDialog = true })
        } else {
            LazyVerticalGrid(state = lazyGridState, columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                item(key = "likedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = likedPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("auto_playlist/liked") }).animateItem(), context = LocalContext.current) }
                item(key = "downloadedPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = downloadPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("auto_playlist/downloaded") }).animateItem(), context = LocalContext.current) }
                item(key = "TopPlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = topPlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("top_playlist/$topSize") }).animateItem(), context = LocalContext.current) }
                item(key = "cachePlaylist", contentType = { CONTENT_TYPE_PLAYLIST }) { PlaylistGridItem(playlist = cachePlaylist, fillMaxWidth = true, autoPlaylist = true, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { navController.navigate("cache_playlist/cached") }).animateItem(), context = LocalContext.current) }
                if (playlists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { }
                items(items = playlists, key = { it.id }, contentType = { CONTENT_TYPE_PLAYLIST }) { LibraryPlaylistGridItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, playlist = it, modifier = Modifier.animateItem(), context = LocalContext.current) }
            }
            HideOnScrollFAB(lazyListState = lazyGridState, icon = R.drawable.add, onClick = { showCreatePlaylistDialog = true })
        }
    }
}

@Composable fun LibrarySongsScreen(navController: NavController, onDeselect: () -> Unit, viewModel: LibrarySongsViewModel = hiltViewModel()) {
    val context = LocalContext.current; val menuState = LocalMenuState.current; val haptic = LocalHapticFeedback.current; val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val (sortType, onSortTypeChange) = rememberEnumPreference(SongSortTypeKey, SongSortType.CREATE_DATE); val (sortDescending, onSortDescendingChange) = rememberPreference(SongSortDescendingKey, true)
    val (ytmSync) = rememberPreference(YtmSyncKey, true); val songs by viewModel.allSongs.collectAsState(); var filter by rememberEnumPreference(SongFilterKey, SongFilter.LIKED)
    LaunchedEffect(Unit) { if (ytmSync) when (filter) { SongFilter.LIKED -> viewModel.syncLikedSongs(); SongFilter.LIBRARY -> viewModel.syncLibrarySongs(); else -> return@LaunchedEffect } }
    val wrappedSongs = songs.map { ItemWrapper(it) }.toMutableList(); var selection by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState(); val backStackEntry by navController.currentBackStackEntryAsState(); val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { lazyListState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    Box(Modifier.fillMaxSize()) {
        VerticalFastScroller(
            listState = lazyListState,
            topContentPadding = 16.dp,
            endContentPadding = 0.dp
        ) {
            LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { Row { Spacer(Modifier.width(12.dp)); FilterChip(label = { Text(stringResource(R.string.songs)) }, selected = true, colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface), onClick = onDeselect, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(painterResource(R.drawable.close), "") }); ChipsRow(chips = listOf(SongFilter.LIKED to stringResource(R.string.filter_liked), SongFilter.LIBRARY to stringResource(R.string.filter_library), SongFilter.DOWNLOADED to stringResource(R.string.filter_downloaded)), currentValue = filter, onValueUpdate = { filter = it }, modifier = Modifier.weight(1f)) } }
                item(key = "header", contentType = CONTENT_TYPE_HEADER) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selection) { val count = wrappedSongs.count { it.isSelected }; IconButton({ selection = false }) { Icon(painterResource(R.drawable.close), null) }; Text(pluralStringResource(R.plurals.n_song, count, count), Modifier.weight(1f)); IconButton({ if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }; IconButton({ menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item }, onDismiss = menuState::dismiss, clearAction = { selection = false }) } }) { Icon(painterResource(R.drawable.more_vert), null) } }
                        else Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) { SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { when (it as SongSortType) { SongSortType.CREATE_DATE -> R.string.sort_by_create_date; SongSortType.NAME -> R.string.sort_by_name; SongSortType.ARTIST -> R.string.sort_by_artist; SongSortType.PLAY_TIME -> R.string.sort_by_play_time } }); Spacer(Modifier.weight(1f)); Text(pluralStringResource(R.plurals.n_song, songs.size, songs.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary) }
                    }
                }
                itemsIndexed(items = wrappedSongs, key = { _, s -> s.item.song.id }, contentType = { _, _ -> CONTENT_TYPE_SONG }) { index, s ->
                    SongListItem(song = s.item, showInLibraryIcon = true, isActive = s.item.id == mediaMetadata?.id, isPlaying = isPlaying, trailingContent = { IconButton({ menuState.show { SongMenu(originalSong = s.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, isSelected = s.isSelected && selection, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (!selection) { if (s.item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue(context.getString(R.string.queue_all_songs), songs.map { it.toMediaItem() }, index)) } else s.isSelected = !s.isSelected }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; s.isSelected = true }).animateItem())
                }
            }
        }
        HideOnScrollFAB(visible = songs.isNotEmpty(), lazyListState = lazyListState, icon = R.drawable.shuffle, onClick = { playerConnection.playQueue(ListQueue(context.getString(R.string.queue_all_songs), songs.shuffled().map { it.toMediaItem() })) })
    }
}

@Composable fun LibraryAlbumsScreen(navController: NavController, onDeselect: () -> Unit, viewModel: LibraryAlbumsViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current; val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID); var filter by rememberEnumPreference(AlbumFilterKey, AlbumFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(AlbumSortTypeKey, AlbumSortType.CREATE_DATE); val (sortDescending, onSortDescendingChange) = rememberPreference(AlbumSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG); val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val filterContent = @Composable { Row { Spacer(Modifier.width(12.dp)); FilterChip(label = { Text(stringResource(R.string.albums)) }, selected = true, colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface), onClick = onDeselect, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(painterResource(R.drawable.close), "") }); ChipsRow(chips = listOf(AlbumFilter.LIKED to stringResource(R.string.filter_liked), AlbumFilter.LIBRARY to stringResource(R.string.filter_library)), currentValue = filter, onValueUpdate = { filter = it }, modifier = Modifier.weight(1f)) } }
    LaunchedEffect(filter) { if (ytmSync && filter == AlbumFilter.LIKED) withContext(Dispatchers.IO) { viewModel.sync() } }
    val albums by viewModel.allAlbums.collectAsState(); val coroutineScope = rememberCoroutineScope(); val lazyListState = rememberLazyListState(); val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState(); val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0) else lazyGridState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    val headerContent = @Composable { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) { SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { when (it as AlbumSortType) { AlbumSortType.CREATE_DATE -> R.string.sort_by_create_date; AlbumSortType.NAME -> R.string.sort_by_name; AlbumSortType.ARTIST -> R.string.sort_by_artist; AlbumSortType.YEAR -> R.string.sort_by_year; AlbumSortType.SONG_COUNT -> R.string.sort_by_song_count; AlbumSortType.LENGTH -> R.string.sort_by_length; AlbumSortType.PLAY_TIME -> R.string.sort_by_play_time } }); Spacer(Modifier.weight(1f)); Text(pluralStringResource(R.plurals.n_album, albums.size, albums.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary); IconButton({ viewType = viewType.toggle() }, Modifier.padding(start = 6.dp, end = 6.dp)) { Icon(painterResource(if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view), null) } } }
    Box(Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (albums.isEmpty()) item { EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.library_album_empty), modifier = Modifier.animateItem()) }
                items(items = albums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { LibraryAlbumListItem(navController = navController, menuState = menuState, album = it, isActive = it.id == mediaMetadata?.album?.id, isPlaying = isPlaying, modifier = Modifier.animateItem()) }
            }
        } else {
            LazyVerticalGrid(state = lazyGridState, columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (albums.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyPlaceholder(icon = R.drawable.album, text = stringResource(R.string.library_album_empty), modifier = Modifier.animateItem()) }
                items(items = albums, key = { it.id }, contentType = { CONTENT_TYPE_ALBUM }) { LibraryAlbumGridItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, album = it, isActive = it.id == mediaMetadata?.album?.id, isPlaying = isPlaying, modifier = Modifier.animateItem()) }
            }
        }
    }
}

@Composable fun LibraryArtistsScreen(navController: NavController, onDeselect: () -> Unit, viewModel: LibraryArtistsViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current; var viewType by rememberEnumPreference(ArtistViewTypeKey, LibraryViewType.GRID); var filter by rememberEnumPreference(ArtistFilterKey, ArtistFilter.LIKED)
    val (sortType, onSortTypeChange) = rememberEnumPreference(ArtistSortTypeKey, ArtistSortType.CREATE_DATE); val (sortDescending, onSortDescendingChange) = rememberPreference(ArtistSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG); val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val filterContent = @Composable { Row { Spacer(Modifier.width(12.dp)); FilterChip(label = { Text(stringResource(R.string.artists)) }, selected = true, colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surface), onClick = onDeselect, shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(painterResource(R.drawable.close), "") }); ChipsRow(chips = listOf(ArtistFilter.LIKED to stringResource(R.string.filter_liked), ArtistFilter.LIBRARY to stringResource(R.string.filter_library)), currentValue = filter, onValueUpdate = { filter = it }, modifier = Modifier.weight(1f)) } }
    LaunchedEffect(filter) { if (ytmSync && filter == ArtistFilter.LIKED) withContext(Dispatchers.IO) { viewModel.sync() } }
    val artists by viewModel.allArtists.collectAsState(); val coroutineScope = rememberCoroutineScope(); val lazyListState = rememberLazyListState(); val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState(); val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()
    LaunchedEffect(scrollToTop?.value) { if (scrollToTop?.value == true) { if (viewType == LibraryViewType.LIST) lazyListState.animateScrollToItem(0) else lazyGridState.animateScrollToItem(0); backStackEntry?.savedStateHandle?.set("scrollToTop", false) } }
    val headerContent = @Composable { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) { SortHeader(sortType = sortType, sortDescending = sortDescending, onSortTypeChange = onSortTypeChange, onSortDescendingChange = onSortDescendingChange, sortTypeText = { when (it as ArtistSortType) { ArtistSortType.CREATE_DATE -> R.string.sort_by_create_date; ArtistSortType.NAME -> R.string.sort_by_name; ArtistSortType.SONG_COUNT -> R.string.sort_by_song_count; ArtistSortType.PLAY_TIME -> R.string.sort_by_play_time } }); Spacer(Modifier.weight(1f)); Text(pluralStringResource(R.plurals.n_artist, artists.size, artists.size), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary); IconButton({ viewType = viewType.toggle() }, Modifier.padding(start = 6.dp, end = 6.dp)) { Icon(painterResource(if (viewType == LibraryViewType.LIST) R.drawable.list else R.drawable.grid_view), null) } } }
    Box(Modifier.fillMaxSize()) {
        if (viewType == LibraryViewType.LIST) {
            LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (artists.isEmpty()) item { EmptyPlaceholder(icon = R.drawable.artist, text = stringResource(R.string.library_artist_empty), modifier = Modifier.animateItem()) }
                items(items = artists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { LibraryArtistListItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, modifier = Modifier.animateItem(), artist = it) }
            }
        } else {
            LazyVerticalGrid(state = lazyGridState, columns = GridCells.Adaptive(GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp), contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
                item(key = "filter", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { filterContent() }; item(key = "header", span = { GridItemSpan(maxLineSpan) }, contentType = CONTENT_TYPE_HEADER) { headerContent() }
                if (artists.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { EmptyPlaceholder(icon = R.drawable.artist, text = stringResource(R.string.library_artist_empty), modifier = Modifier.animateItem()) }
                items(items = artists, key = { it.id }, contentType = { CONTENT_TYPE_ARTIST }) { LibraryArtistGridItem(navController = navController, menuState = menuState, coroutineScope = coroutineScope, modifier = Modifier.animateItem(), artist = it) }
            }
        }
    }
}

@Composable fun CachePlaylistScreen(navController: NavController, scrollBehavior: TopAppBarScrollBehavior, viewModel: HistoryViewModel = hiltViewModel()) {
    val menuState = LocalMenuState.current; val playerConnection = LocalPlayerConnection.current ?: return; val haptic = LocalHapticFeedback.current; val focusManager = LocalFocusManager.current
    val isPlaying by playerConnection.isPlaying.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState(); val events by viewModel.events.collectAsState()
    val playerCache = playerConnection.service.playerCache; val cachedSongIds = remember(playerCache) { playerCache?.keys?.mapNotNull { it.toString() }?.toSet() ?: emptySet() }
    val allSongs = remember(events, cachedSongIds) { events.values.flatten().map { it.song }.distinctBy { it.id }.filter { it.id in cachedSongIds } }
    val wrappedSongs = remember(allSongs) { mutableStateListOf<ItemWrapper<Song>>().apply { addAll(allSongs.map { ItemWrapper(it) }) } }
    var selection by remember { mutableStateOf(false) }; var isSearching by remember { mutableStateOf(false) }; var query by remember { mutableStateOf(TextFieldValue()) }
    val focusRequester = remember { FocusRequester() }; val lazyListState = rememberLazyListState()
    LaunchedEffect(isSearching) { if (isSearching) focusRequester.requestFocus() }
    val filteredSongs = remember(wrappedSongs, query) { if (query.text.isEmpty()) wrappedSongs else wrappedSongs.filter { s -> s.item.title.contains(query.text, true) || s.item.artists.any { it.name.contains(query.text, true) } } }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = lazyListState, contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            if (filteredSongs.isEmpty()) item { EmptyPlaceholder(icon = R.drawable.music_note, text = stringResource(R.string.playlist_is_empty)) }
            else {
                if (!isSearching) item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AlbumThumbnailSize).clip(RoundedCornerShape(ThumbnailCornerRadius))) { AsyncImage(model = filteredSongs.first().item.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ThumbnailCornerRadius))) }
                            Column(verticalArrangement = Arrangement.Center) { Text(stringResource(R.string.cached_playlist), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(pluralStringResource(R.plurals.n_song, filteredSongs.size, filteredSongs.size), style = MaterialTheme.typography.bodyMedium) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { playerConnection.playQueue(ListQueue("Cache Songs", filteredSongs.map { it.item.toMediaItem() })) }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding, modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.play), null, Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text(stringResource(R.string.play)) }
                            OutlinedButton(onClick = { playerConnection.playQueue(ListQueue("Cache Songs", filteredSongs.shuffled().map { it.item.toMediaItem() })) }, contentPadding = ButtonDefaults.ButtonWithIconContentPadding, modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.shuffle), null, Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text(stringResource(R.string.shuffle)) }
                        }
                    }
                }
                itemsIndexed(items = filteredSongs, key = { _, s -> s.item.id }) { index, s ->
                    SongListItem(song = s.item, isActive = s.item.id == mediaMetadata?.id, isPlaying = isPlaying, isSelected = s.isSelected && selection, showInLibraryIcon = true, trailingContent = { IconButton({ menuState.show { SongMenu(originalSong = s.item, navController = navController, onDismiss = menuState::dismiss) } }) { Icon(painterResource(R.drawable.more_vert), null) } }, modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if (!selection) { if (s.item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(ListQueue("Cache Songs", filteredSongs.map { it.item.toMediaItem() }, index)) } else s.isSelected = !s.isSelected }, onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); if (!selection) selection = true; wrappedSongs.forEach { it.isSelected = false }; s.isSelected = true }).animateItem())
                }
            }
        }
        TopAppBar(title = {
            when {
                selection -> { val count = wrappedSongs.count { it.isSelected }; Text(pluralStringResource(R.plurals.n_song, count, count), style = MaterialTheme.typography.titleLarge) }
                isSearching -> TextField(value = query, onValueChange = { query = it }, placeholder = { Text(stringResource(R.string.search), style = MaterialTheme.typography.titleLarge) }, singleLine = true, textStyle = MaterialTheme.typography.titleLarge, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                else -> Text(stringResource(R.string.cached_playlist), style = MaterialTheme.typography.titleLarge)
            }
        }, navigationIcon = { IconButton(onClick = { when { isSearching -> { isSearching = false; query = TextFieldValue(); focusManager.clearFocus() }; selection -> selection = false; else -> navController.navigateUp() } }, onLongClick = { if (!isSearching && !selection) navController.backToMain() }) { Icon(painterResource(if (selection) R.drawable.close else R.drawable.arrow_back), null) } }, actions = {
            if (selection) { val count = wrappedSongs.count { it.isSelected }; IconButton({ if (count == wrappedSongs.size) wrappedSongs.forEach { it.isSelected = false } else wrappedSongs.forEach { it.isSelected = true } }) { Icon(painterResource(if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all), null) }; IconButton({ menuState.show { SelectionSongMenu(songSelection = wrappedSongs.filter { it.isSelected }.map { it.item }, onDismiss = menuState::dismiss, clearAction = { selection = false }) } }) { Icon(painterResource(R.drawable.more_vert), null) } }
            else if (!isSearching) IconButton({ isSearching = true }) { Icon(painterResource(R.drawable.search), null) }
        })
    }
}
