package com.cgens67.avidtune.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.YTItem
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.SnapLayoutInfoProvider
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.viewmodels.YouTubeBrowseViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YouTubeBrowseScreen(
    navController: NavController,
    viewModel: YouTubeBrowseViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val browseResult by viewModel.result.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val songsLazyGridState = rememberLazyGridState()

    Box(modifier = Modifier.fillMaxSize()) {
        val snapLayoutInfoProviderSongs = remember(songsLazyGridState) { SnapLayoutInfoProvider(lazyGridState = songsLazyGridState) }
        LazyColumn(contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()) {
            if (browseResult == null) {
                item {
                    ShimmerHost { repeat(8) { ListItemPlaceHolder() } }
                }
            }

            browseResult?.items?.fastForEach { resultItem ->
                if (resultItem.items.isNotEmpty()) {
                    resultItem.title?.let { title ->
                        item { NavigationTitle(title) }
                    }

                    if ((resultItem.items.firstOrNull() as? SongItem)?.album != null || resultItem.items.firstOrNull() is EpisodeItem) {
                        item {
                            LazyHorizontalGrid(
                                state = songsLazyGridState,
                                rows = GridCells.Fixed(5),
                                flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProviderSongs),
                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                modifier = Modifier.fillMaxWidth().height(ListItemHeight * 5),
                            ) {
                                items(items = resultItem.items) { song ->
                                    Box(Modifier.width(350.dp)) {
                                        YouTubeListItem(
                                            item = song,
                                            isActive = mediaMetadata?.id == song.id,
                                            isPlaying = isPlaying,
                                            trailingContent = {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            when (song) {
                                                                is SongItem -> YouTubeSongMenu(song = song, navController = navController, onDismiss = menuState::dismiss)
                                                                is EpisodeItem -> YouTubeSongMenu(song = song.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                                                                else -> {} // Should not happen here
                                                            }
                                                        }
                                                    },
                                                ) {
                                                    Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                                                }
                                            },
                                            modifier = Modifier.clickable {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    val queueItem = when(song) {
                                                        is SongItem -> YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata())
                                                        is EpisodeItem -> YouTubeQueue(WatchEndpoint(videoId = song.id), song.asSongItem().toMediaMetadata())
                                                        else -> return@clickable
                                                    }
                                                    playerConnection.playQueue(queueItem)
                                                }
                                            }.animateItem(),
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            LazyRow {
                                items(items = resultItem.items) { item ->
                                    YouTubeGridItem(
                                        item = item,
                                        isActive = when (item) {
                                            is SongItem -> mediaMetadata?.id == item.id
                                            is AlbumItem -> mediaMetadata?.album?.id == item.id
                                            is EpisodeItem -> mediaMetadata?.id == item.id
                                            else -> false
                                        },
                                        isPlaying = isPlaying,
                                        coroutineScope = coroutineScope,
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                                                    is AlbumItem -> navController.navigate("album/${item.id}")
                                                    is ArtistItem -> navController.navigate("artist/${item.id}")
                                                    is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                    is EpisodeItem -> playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.asSongItem().toMediaMetadata()))
                                                    is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    when (item) {
                                                        is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                                        is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                                        is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                                        is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                        is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                                                        is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = menuState::dismiss)
                                                    }
                                                }
                                            },
                                        ).animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    TopAppBar(
        title = { Text(browseResult?.title.orEmpty()) },
        navigationIcon = {
            IconButton(onClick = navController::navigateUp, onLongClick = navController::backToMain) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}
