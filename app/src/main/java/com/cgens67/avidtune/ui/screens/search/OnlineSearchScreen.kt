package com.cgens67.avidtune.ui.screens.search

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
import com.cgens67.innertube.models.*
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

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(query) {
        viewModel.query.value = query
    }

    val openItemMenu: (YTItem) -> Unit = { item ->
        menuState.show {
            when (item) {
                is SongItem -> YouTubeSongMenu(
                    song = item,
                    navController = navController,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
                is AlbumItem -> YouTubeAlbumMenu(
                    albumItem = item,
                    navController = navController,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
                is ArtistItem -> YouTubeArtistMenu(
                    artist = item,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
                is PlaylistItem -> YouTubePlaylistMenu(
                    playlist = item,
                    coroutineScope = scope,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
                is EpisodeItem -> YouTubeSongMenu(
                    song = item.asSongItem(),
                    navController = navController,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
                is PodcastItem -> YouTubePlaylistMenu(
                    playlist = item.asPlaylistItem(),
                    coroutineScope = scope,
                    onDismiss = {
                        menuState.dismiss()
                        onDismiss()
                    }
                )
            }
        }
    }

    val onItemClick: (YTItem) -> Unit = { item ->
        when (item) {
            is SongItem -> {
                if (item.id == mediaMetadata?.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        YouTubeQueue.radio(item.toMediaMetadata())
                    )
                    onDismiss()
                }
            }
            is AlbumItem -> {
                navController.navigate("album/${item.id}")
                onDismiss()
            }
            is ArtistItem -> {
                navController.navigate("artist/${item.id}")
                onDismiss()
            }
            is PlaylistItem -> {
                navController.navigate("online_playlist/${item.id}")
                onDismiss()
            }
            is EpisodeItem -> {
                if (item.id == mediaMetadata?.id) {
                    playerConnection.player.togglePlayPause()
                } else {
                    playerConnection.playQueue(
                        YouTubeQueue.radio(item.asSongItem().toMediaMetadata())
                    )
                    onDismiss()
                }
            }
            is PodcastItem -> {
                navController.navigate("online_playlist/${item.id}")
                onDismiss()
            }
        }
    }

    val isItemActive: (YTItem) -> Boolean = { item ->
        when (item) {
            is SongItem -> mediaMetadata?.id == item.id
            is AlbumItem -> mediaMetadata?.album?.id == item.id
            is EpisodeItem -> mediaMetadata?.id == item.id
            else -> false
        }
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
                                    painter = painterResource(R.drawable.trending_up),
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
                        database.query {
                            delete(history)
                        }
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
                    isActive = isItemActive(item),
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(onClick = { openItemMenu(item) }) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onItemClick(item) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    when (item) {
                                        is SongItem -> YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                        is AlbumItem -> YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                        is ArtistItem -> YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                        is PlaylistItem -> YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                        is EpisodeItem -> YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                        is PodcastItem -> YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            }
                                        )
                                    }
                                }
                            }
                        )
                        .animateItem()
                )
            }
        }
    } else {
        // Dual-Pane Landscape Search Studio
        val leftListState = rememberLazyListState()
        val rightListState = rememberLazyListState()

        LaunchedEffect(leftListState) {
            snapshotFlow { leftListState.firstVisibleItemScrollOffset }
                .drop(1)
                .collect {
                    keyboardController?.hide()
                }
        }
        LaunchedEffect(rightListState) {
            snapshotFlow { rightListState.firstVisibleItemScrollOffset }
                .drop(1)
                .collect {
                    keyboardController?.hide()
                }
        }

        val bgColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.background
        val paneColor = if (pureBlack) Color(0xFF0E0E0E) else MaterialTheme.colorScheme.surfaceContainerLow
        val cardColor = if (pureBlack) Color(0xFF151515) else MaterialTheme.colorScheme.surfaceContainer
        val distinctItems = remember(viewState.items) { viewState.items.distinctBy { it.id } }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            // Left Pane: Trending Banner + History & Suggestions
            Surface(
                color = paneColor,
                shape = RoundedCornerShape(22.dp),
                border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                modifier = Modifier
                    .weight(0.43f)
                    .fillMaxHeight(),
            ) {
                LazyColumn(
                    state = leftListState,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (query.isEmpty()) {
                        item(key = "trending_banner") {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        onDismiss()
                                        navController.navigate("apple_music_trending")
                                    },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(10.dp),
                                            ),
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.trending_up),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.apple_music_trending),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        painter = painterResource(R.drawable.navigate_next),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
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
                                database.query {
                                    delete(history)
                                }
                            },
                            onFillTextField = {
                                onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                            },
                            modifier = Modifier.animateItem(),
                            pureBlack = pureBlack,
                        )
                    }

                    if (viewState.history.isNotEmpty() && viewState.suggestions.isNotEmpty()) {
                        item(key = "suggestions_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
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
                            pureBlack = pureBlack,
                        )
                    }

                    if (viewState.history.isEmpty() && viewState.suggestions.isEmpty() && query.isNotEmpty()) {
                        item(key = "left_search_action") {
                            SuggestionItem(
                                query = query,
                                online = true,
                                onClick = {
                                    onSearch(query)
                                    onDismiss()
                                },
                                onFillTextField = {
                                    onSearch(query)
                                    onDismiss()
                                },
                                pureBlack = pureBlack,
                            )
                        }
                    }
                }
            }

            // Right Pane: Spotlight Top Match + Live Instant Results
            Surface(
                color = paneColor,
                shape = RoundedCornerShape(22.dp),
                border = if (pureBlack) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null,
                modifier = Modifier
                    .weight(0.57f)
                    .fillMaxHeight(),
            ) {
                if (distinctItems.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = distinctItems.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }

                        LazyColumn(
                            state = rightListState,
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            itemsIndexed(
                                items = distinctItems,
                                key = { _, item -> "landscape_item_${item.id}" },
                            ) { index, item ->
                                val active = isItemActive(item)
                                val isSpotlight = index == 0

                                Surface(
                                    color = when {
                                        active -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                        isSpotlight -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.16f)
                                        else -> cardColor
                                    },
                                    shape = RoundedCornerShape(if (isSpotlight) 18.dp else 14.dp),
                                    border = when {
                                        active -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                        isSpotlight -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
                                        else -> null
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(if (isSpotlight) 18.dp else 14.dp))
                                        .animateItem(),
                                ) {
                                    Box(
                                        modifier = Modifier.then(
                                            if (isSpotlight) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                            Color.Transparent,
                                                        )
                                                    )
                                                )
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    ) {
                                        YouTubeListItem(
                                            item = item,
                                            isActive = active,
                                            isPlaying = isPlaying,
                                            trailingContent = {
                                                IconButton(onClick = { openItemMenu(item) }) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                            },
                                            modifier = Modifier.combinedClickable(
                                                onClick = { onItemClick(item) },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    openItemMenu(item)
                                                },
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Empty / Discovery Prompt Stage on Right Pane
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape,
                                    ),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp),
                                )
                            }

                            if (query.isNotEmpty()) {
                                Text(
                                    text = "\"$query\"",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                )
                                Button(
                                    onClick = {
                                        onSearch(query)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.search))
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.search_yt_music),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                )
                                FilledTonalButton(
                                    onClick = {
                                        onDismiss()
                                        navController.navigate("apple_music_trending")
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.trending_up),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.apple_music_trending))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
    pureBlack: Boolean
) {
    val iconContainerColor = if (pureBlack) {
        Color.White.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    }

    val iconTint = if (pureBlack) {
        Color.White.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconContainerColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                painterResource(if (online) R.drawable.search else R.drawable.history),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = if (pureBlack) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                    tint = if (pureBlack) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        IconButton(onClick = onFillTextField) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
                tint = if (pureBlack) Color.White.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}