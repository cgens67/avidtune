package com.cgens67.avidtune.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CONTENT_TYPE_LIST
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.LocalItem
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.ui.component.AlbumListItem
import com.cgens67.avidtune.ui.component.ArtistListItem
import com.cgens67.avidtune.ui.component.ChipsRow
import com.cgens67.avidtune.ui.component.EmptyPlaceholder
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlaylistListItem
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.viewmodels.LocalFilter
import com.cgens67.avidtune.viewmodels.LocalSearchViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocalSearchScreen(
    query: String,
    navController: NavController,
    onDismiss: () -> Unit,
    isFromCache: Boolean = false,
    pureBlack: Boolean,
    viewModel: LocalSearchViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val searchFilter by viewModel.filter.collectAsState()
    val result by viewModel.result.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    if (!isLandscape) {
        val lazyListState = rememberLazyListState()

        LaunchedEffect(Unit) {
            snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
                .drop(1)
                .collect {
                    keyboardController?.hide()
                }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
        ) {
            ChipsRow(
                chips = listOf(
                    LocalFilter.ALL to stringResource(R.string.filter_all),
                    LocalFilter.SONG to stringResource(R.string.filter_songs),
                    LocalFilter.ALBUM to stringResource(R.string.filter_albums),
                    LocalFilter.ARTIST to stringResource(R.string.filter_artists),
                    LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
                ),
                currentValue = searchFilter,
                onValueUpdate = { viewModel.filter.value = it },
            )

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f),
            ) {
                result.map.forEach { (filter, items) ->
                    if (result.filter == LocalFilter.ALL) {
                        item(key = filter) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight)
                                    .clickable { viewModel.filter.value = filter }
                                    .padding(start = 12.dp, end = 18.dp),
                            ) {
                                Text(
                                    text = stringResource(
                                        when (filter) {
                                            LocalFilter.SONG -> R.string.filter_songs
                                            LocalFilter.ALBUM -> R.string.filter_albums
                                            LocalFilter.ARTIST -> R.string.filter_artists
                                            LocalFilter.PLAYLIST -> R.string.filter_playlists
                                            LocalFilter.ALL -> error("")
                                        }
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                )

                                Icon(
                                    painter = painterResource(R.drawable.navigate_next),
                                    contentDescription = null,
                                )
                            }
                        }
                    }

                    items(
                        items = items,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_LIST },
                    ) { item ->
                        when (item) {
                            is Song -> SongListItem(
                                song = item,
                                showInLibraryIcon = true,
                                isActive = item.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                trailingContent = {
                                    IconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        onDismiss()
                                                        menuState.dismiss()
                                                    },
                                                    isFromCache = isFromCache
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .combinedClickable(
                                        onClick = {
                                            if (item.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                val songs = result.map
                                                    .getOrDefault(LocalFilter.SONG, emptyList())
                                                    .filterIsInstance<Song>()
                                                    .map { it.toMediaItem() }
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = context.getString(R.string.queue_searched_songs),
                                                        items = songs,
                                                        startIndex = songs.indexOfFirst { it.mediaId == item.id },
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = item,
                                                    navController = navController,
                                                    onDismiss = {
                                                        onDismiss()
                                                        menuState.dismiss()
                                                    },
                                                    isFromCache = isFromCache
                                                )
                                            }
                                        }
                                    )
                                    .animateItem(),
                            )

                            is Album -> AlbumListItem(
                                album = item,
                                isActive = item.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .clickable {
                                        onDismiss()
                                        navController.navigate("album/${item.id}")
                                    }
                                    .animateItem(),
                            )

                            is Artist -> ArtistListItem(
                                artist = item,
                                modifier = Modifier
                                    .clickable {
                                        onDismiss()
                                        navController.navigate("artist/${item.id}")
                                    }
                                    .animateItem(),
                            )

                            is Playlist -> PlaylistListItem(
                                playlist = item,
                                modifier = Modifier
                                    .clickable {
                                        onDismiss()
                                        navController.navigate("local_playlist/${item.id}")
                                    }
                                    .animateItem(),
                            )
                        }
                    }
                }

                if (result.query.isNotEmpty() && result.map.isEmpty()) {
                    item(key = "no_result") {
                        EmptyPlaceholder(
                            icon = R.drawable.search,
                            text = stringResource(R.string.no_results_found),
                        )
                    }
                }
            }
        }
    } else {
        LocalSearchLandscapeLayout(
            searchFilter = searchFilter,
            resultMap = result.map,
            resultFilter = result.filter,
            resultQuery = result.query,
            isPlaying = isPlaying,
            activeSongId = mediaMetadata?.id,
            activeAlbumId = mediaMetadata?.album?.id,
            pureBlack = pureBlack,
            onFilterSelect = { viewModel.filter.value = it },
            onSongClick = { song ->
                if (song.id == mediaMetadata?.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    val songs = result.map
                        .getOrDefault(LocalFilter.SONG, emptyList())
                        .filterIsInstance<Song>()
                        .map { it.toMediaItem() }
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.queue_searched_songs),
                            items = songs,
                            startIndex = songs.indexOfFirst { it.mediaId == song.id },
                        )
                    )
                }
            },
            onSongMenu = { song ->
                menuState.show {
                    SongMenu(
                        originalSong = song,
                        navController = navController,
                        onDismiss = {
                            onDismiss()
                            menuState.dismiss()
                        },
                        isFromCache = isFromCache,
                    )
                }
            },
            onAlbumClick = { album ->
                onDismiss()
                navController.navigate("album/${album.id}")
            },
            onArtistClick = { artist ->
                onDismiss()
                navController.navigate("artist/${artist.id}")
            },
            onPlaylistClick = { playlist ->
                onDismiss()
                navController.navigate("local_playlist/${playlist.id}")
            },
            onHideKeyboard = { keyboardController?.hide() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalSearchLandscapeLayout(
    searchFilter: LocalFilter,
    resultMap: Map<LocalFilter, List<LocalItem>>,
    resultFilter: LocalFilter,
    resultQuery: String,
    isPlaying: Boolean,
    activeSongId: String?,
    activeAlbumId: String?,
    pureBlack: Boolean,
    onFilterSelect: (LocalFilter) -> Unit,
    onSongClick: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onHideKeyboard: () -> Unit,
) {
    val bgColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
    val paneColor = if (pureBlack) Color(0xFF0E0E0E) else MaterialTheme.colorScheme.surfaceContainerLow
    val cardColor = if (pureBlack) Color(0xFF151515) else MaterialTheme.colorScheme.surfaceContainer

    val filters = listOf(
        LocalFilter.ALL to stringResource(R.string.filter_all),
        LocalFilter.SONG to stringResource(R.string.filter_songs),
        LocalFilter.ALBUM to stringResource(R.string.filter_albums),
        LocalFilter.ARTIST to stringResource(R.string.filter_artists),
        LocalFilter.PLAYLIST to stringResource(R.string.filter_playlists),
    )

    val totalCount = resultMap.values.sumOf { it.size }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        // Left Filter Rail
        Surface(
            color = paneColor,
            shape = RoundedCornerShape(20.dp),
            border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
            modifier = Modifier
                .width(204.dp)
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.library_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.search_library),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                )

                filters.forEach { (filter, label) ->
                    val selected = searchFilter == filter
                    val count = when {
                        filter == LocalFilter.ALL && resultFilter == LocalFilter.ALL -> totalCount
                        else -> resultMap[filter]?.size
                    }

                    Surface(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onFilterSelect(filter) },
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (count != null && count > 0) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Workspace
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            if (resultMap.isEmpty()) {
                Surface(
                    color = paneColor,
                    shape = RoundedCornerShape(22.dp),
                    border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (resultQuery.isNotEmpty()) {
                            EmptyPlaceholder(
                                icon = R.drawable.search,
                                text = stringResource(R.string.no_results_found),
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(24.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.library_music),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp),
                                )
                                Text(
                                    text = stringResource(R.string.search_library),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            } else if (resultFilter == LocalFilter.ALL) {
                val songs = resultMap[LocalFilter.SONG].orEmpty()
                val collections = resultMap.entries.filter { it.key != LocalFilter.SONG && it.value.isNotEmpty() }

                if (songs.isNotEmpty() && collections.isNotEmpty()) {
                    // Dual-Pane Split: Songs on Left-Center, Albums/Artists/Playlists on Right
                    val songsListState = rememberLazyListState()
                    val collectionsListState = rememberLazyListState()

                    LaunchedEffect(songsListState, collectionsListState) {
                        snapshotFlow { songsListState.firstVisibleItemScrollOffset }
                            .drop(1)
                            .collect { onHideKeyboard() }
                    }
                    LaunchedEffect(collectionsListState) {
                        snapshotFlow { collectionsListState.firstVisibleItemScrollOffset }
                            .drop(1)
                            .collect { onHideKeyboard() }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Surface(
                            color = paneColor,
                            shape = RoundedCornerShape(22.dp),
                            border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                            modifier = Modifier
                                .weight(1.12f)
                                .fillMaxHeight(),
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                LandscapeSectionBanner(
                                    title = stringResource(R.string.filter_songs),
                                    count = songs.size,
                                    showJumpArrow = true,
                                    onClick = { onFilterSelect(LocalFilter.SONG) },
                                )
                                LazyColumn(
                                    state = songsListState,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    items(
                                        items = songs,
                                        key = { it.id },
                                        contentType = { CONTENT_TYPE_LIST },
                                    ) { item ->
                                        LocalSearchItemCard(
                                            item = item,
                                            isActive = item.id == activeSongId,
                                            isPlaying = isPlaying,
                                            cardColor = cardColor,
                                            onSongClick = onSongClick,
                                            onSongMenu = onSongMenu,
                                            onAlbumClick = onAlbumClick,
                                            onArtistClick = onArtistClick,
                                            onPlaylistClick = onPlaylistClick,
                                            modifier = Modifier.animateItem(),
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = paneColor,
                            shape = RoundedCornerShape(22.dp),
                            border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                            modifier = Modifier
                                .weight(0.88f)
                                .fillMaxHeight(),
                        ) {
                            LazyColumn(
                                state = collectionsListState,
                                contentPadding = PaddingValues(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                collections.forEach { (filter, items) ->
                                    item(key = "header_$filter") {
                                        LandscapeSectionBanner(
                                            title = stringResource(
                                                when (filter) {
                                                    LocalFilter.ALBUM -> R.string.filter_albums
                                                    LocalFilter.ARTIST -> R.string.filter_artists
                                                    LocalFilter.PLAYLIST -> R.string.filter_playlists
                                                    else -> R.string.filter_songs
                                                }
                                            ),
                                            count = items.size,
                                            showJumpArrow = true,
                                            onClick = { onFilterSelect(filter) },
                                        )
                                    }
                                    items(
                                        items = items,
                                        key = { it.id },
                                        contentType = { CONTENT_TYPE_LIST },
                                    ) { item ->
                                        val active = item is Album && item.id == activeAlbumId
                                        LocalSearchItemCard(
                                            item = item,
                                            isActive = active,
                                            isPlaying = isPlaying,
                                            cardColor = cardColor,
                                            onSongClick = onSongClick,
                                            onSongMenu = onSongMenu,
                                            onAlbumClick = onAlbumClick,
                                            onArtistClick = onArtistClick,
                                            onPlaylistClick = onPlaylistClick,
                                            modifier = Modifier.animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Only Songs OR Only Collections -> Expand into a 2-column Grid
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState) {
                        snapshotFlow { gridState.firstVisibleItemScrollOffset }
                            .drop(1)
                            .collect { onHideKeyboard() }
                    }

                    Surface(
                        color = paneColor,
                        shape = RoundedCornerShape(22.dp),
                        border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            resultMap.forEach { (filter, items) ->
                                item(key = "grid_header_$filter", span = { GridItemSpan(2) }) {
                                    LandscapeSectionBanner(
                                        title = stringResource(
                                            when (filter) {
                                                LocalFilter.SONG -> R.string.filter_songs
                                                LocalFilter.ALBUM -> R.string.filter_albums
                                                LocalFilter.ARTIST -> R.string.filter_artists
                                                LocalFilter.PLAYLIST -> R.string.filter_playlists
                                                LocalFilter.ALL -> R.string.filter_all
                                            }
                                        ),
                                        count = items.size,
                                        showJumpArrow = true,
                                        onClick = { onFilterSelect(filter) },
                                    )
                                }
                                items(
                                    items = items,
                                    key = { it.id },
                                ) { item ->
                                    val active = when (item) {
                                        is Song -> item.id == activeSongId
                                        is Album -> item.id == activeAlbumId
                                        else -> false
                                    }
                                    LocalSearchItemCard(
                                        item = item,
                                        isActive = active,
                                        isPlaying = isPlaying,
                                        cardColor = cardColor,
                                        onSongClick = onSongClick,
                                        onSongMenu = onSongMenu,
                                        onAlbumClick = onAlbumClick,
                                        onArtistClick = onArtistClick,
                                        onPlaylistClick = onPlaylistClick,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Specific Filter Selected -> 2-column Grid
                val gridState = rememberLazyGridState()
                val activeItems = resultMap.values.flatten()

                LaunchedEffect(gridState) {
                    snapshotFlow { gridState.firstVisibleItemScrollOffset }
                        .drop(1)
                        .collect { onHideKeyboard() }
                }

                Surface(
                    color = paneColor,
                    shape = RoundedCornerShape(22.dp),
                    border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LandscapeSectionBanner(
                            title = stringResource(
                                when (resultFilter) {
                                    LocalFilter.SONG -> R.string.filter_songs
                                    LocalFilter.ALBUM -> R.string.filter_albums
                                    LocalFilter.ARTIST -> R.string.filter_artists
                                    LocalFilter.PLAYLIST -> R.string.filter_playlists
                                    LocalFilter.ALL -> R.string.filter_all
                                }
                            ),
                            count = activeItems.size,
                            showJumpArrow = false,
                            onClick = null,
                        )
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            items(
                                items = activeItems,
                                key = { it.id },
                            ) { item ->
                                val active = when (item) {
                                    is Song -> item.id == activeSongId
                                    is Album -> item.id == activeAlbumId
                                    else -> false
                                }
                                LocalSearchItemCard(
                                    item = item,
                                    isActive = active,
                                    isPlaying = isPlaying,
                                    cardColor = cardColor,
                                    onSongClick = onSongClick,
                                    onSongMenu = onSongMenu,
                                    onAlbumClick = onAlbumClick,
                                    onArtistClick = onArtistClick,
                                    onPlaylistClick = onPlaylistClick,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeSectionBanner(
    title: String,
    count: Int,
    showJumpArrow: Boolean,
    onClick: (() -> Unit)?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            shape = CircleShape,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (showJumpArrow) {
            Spacer(Modifier.width(6.dp))
            Icon(
                painter = painterResource(R.drawable.navigate_next),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LocalSearchItemCard(
    item: LocalItem,
    isActive: Boolean,
    isPlaying: Boolean,
    cardColor: Color,
    onSongClick: (Song) -> Unit,
    onSongMenu: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            cardColor
        },
        shape = RoundedCornerShape(14.dp),
        border = if (isActive) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
    ) {
        when (item) {
            is Song -> SongListItem(
                song = item,
                showInLibraryIcon = true,
                isActive = isActive,
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(onClick = { onSongMenu(item) }) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.combinedClickable(
                    onClick = { onSongClick(item) },
                    onLongClick = { onSongMenu(item) },
                ),
            )

            is Album -> AlbumListItem(
                album = item,
                isActive = isActive,
                isPlaying = isPlaying,
                modifier = Modifier.clickable { onAlbumClick(item) },
            )

            is Artist -> ArtistListItem(
                artist = item,
                modifier = Modifier.clickable { onArtistClick(item) },
            )

            is Playlist -> PlaylistListItem(
                playlist = item,
                modifier = Modifier.clickable { onPlaylistClick(item) },
            )
        }
    }
}