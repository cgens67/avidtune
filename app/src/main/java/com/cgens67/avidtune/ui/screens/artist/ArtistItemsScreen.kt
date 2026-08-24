package com.cgens67.avidtune.ui.screens.artist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.filterVideoSongs
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.GridThumbnailHeight
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.HideMusicVideosKey
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.component.shimmer.GridItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.viewmodels.ArtistItemsViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistItemsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistItemsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val title by viewModel.title.collectAsState()
    val itemsPage by viewModel.itemsPage.collectAsState()

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    LaunchedEffect(lazyGridState) {
        snapshotFlow {
            lazyGridState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    if (itemsPage == null) {
        ShimmerHost(
            modifier = Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current),
        ) {
            repeat(8) {
                ListItemPlaceHolder()
            }
        }
    }

    if (itemsPage?.items?.firstOrNull() is SongItem || itemsPage?.items?.firstOrNull() is EpisodeItem) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id },
            ) { item ->
                YouTubeListItem(
                    item = item,
                    isActive =
                        when (item) {
                            is SongItem -> mediaMetadata?.id == item.id
                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                            is EpisodeItem -> mediaMetadata?.id == item.id
                            else -> false
                        },
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem ->
                                            YouTubeSongMenu(
                                                song = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )

                                        is AlbumItem ->
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )

                                        is ArtistItem ->
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = menuState::dismiss,
                                            )

                                        is PlaylistItem ->
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        is EpisodeItem ->
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        is PodcastItem ->
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    modifier =
                        Modifier
                            .clickable {
                                when (item) {
                                    is SongItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint
                                                        ?: WatchEndpoint(videoId = item.id),
                                                    item.toMediaMetadata()
                                                ),
                                            )
                                        }
                                    }
                                    is EpisodeItem -> {
                                        if (item.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    item.endpoint
                                                        ?: WatchEndpoint(videoId = item.id),
                                                    item.asSongItem().toMediaMetadata()
                                                ),
                                            )
                                        }
                                    }

                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                )
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost {
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }
        }
    } else {
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(minSize = GridThumbnailHeight + 24.dp),
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            items(
                items = itemsPage?.items.orEmpty(),
                key = { it.id }
            ) { item ->
                YouTubeGridItem(
                    item = item,
                    isActive = when (item) {
                        is SongItem -> mediaMetadata?.id == item.id
                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                        is EpisodeItem -> mediaMetadata?.id == item.id
                        else -> false
                    },
                    isPlaying = isPlaying,
                    fillMaxWidth = true,
                    coroutineScope = coroutineScope,
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                when (item) {
                                    is SongItem -> playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                    is EpisodeItem -> playerConnection.playQueue(
                                        YouTubeQueue(
                                            item.endpoint ?: WatchEndpoint(videoId = item.id),
                                            item.asSongItem().toMediaMetadata()
                                        )
                                    )

                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                    is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )

                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )

                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = menuState::dismiss
                                        )

                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                        is EpisodeItem -> YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                        is PodcastItem -> YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            }
                        )
                        .animateItem()
                )
            }

            if (itemsPage?.continuation != null) {
                item(key = "loading") {
                    ShimmerHost(Modifier.animateItem()) {
                        GridItemPlaceHolder(fillMaxWidth = true)
                    }
                }
            }
        }
    }

    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}