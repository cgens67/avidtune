package com.cgens67.avidtune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.models.*
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.menu.*
import com.cgens67.avidtune.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean = false,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()
        ),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (query.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            onDismiss()
                            navController.navigate("apple_music_trending")
                        },
                        label = { Text(stringResource(R.string.apple_music_trending)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.apple),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        )
                    )
                }
            }
        }

        items(viewState.history, key = { "history_${it.query}" }) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query { delete(history) }
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { suggestion ->
            SuggestionItem(
                query = suggestion,
                online = true,
                onClick = {
                    onSearch(suggestion)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(suggestion, TextRange(suggestion.length)))
                },
                modifier = Modifier.animateItem(),
                pureBlack = pureBlack
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
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
                        text = stringResource(R.string.SearchResutls),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pureBlack) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(viewState.items.distinctBy { it.id }, key = { "item_${it.id}" }) { item ->
            YouTubeListItem(
                item = item,
                isActive = when (item) {
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
                                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = { menuState.dismiss(); onDismiss() })
                                }
                            }
                        }
                    ) {
                        Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else { playerConnection.playQueue(YouTubeQueue.radio(item.toMediaMetadata())); onDismiss() }
                                }
                                is AlbumItem -> { navController.navigate("album/${item.id}"); onDismiss() }
                                is ArtistItem -> { navController.navigate("artist/${item.id}"); onDismiss() }
                                is PlaylistItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                                is EpisodeItem -> {
                                    if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else { playerConnection.playQueue(YouTubeQueue.radio(item.asSongItem().toMediaMetadata())); onDismiss() }
                                }
                                is PodcastItem -> { navController.navigate("online_playlist/${item.id}"); onDismiss() }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = { menuState.dismiss(); onDismiss() })
                                    is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = coroutineScope, onDismiss = { menuState.dismiss(); onDismiss() })
                                }
                            }
                        }
                    )
                    .animateItem()
            )
        }
    }
}
