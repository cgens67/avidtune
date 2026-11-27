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
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppBarHeight
import com.cgens67.avidtune.constants.SearchFilterHeight
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.ChipsRow
import com.cgens67.avidtune.ui.component.EmptyPlaceholder
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.viewmodels.OnlineSearchViewModel
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.pages.SearchSummary
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchResult(
    navController: NavController,
    viewModel: OnlineSearchViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // Group identical categories together and sort them according to the requested order
    val mergedSummaries = remember(searchSummary) {
        searchSummary?.summaries
            ?.groupBy { it.title }
            ?.map { (title, summaries) ->
                SearchSummary(
                    title = title,
                    items = summaries.flatMap { it.items }.distinctBy { it.id }
                )
            }
            ?.sortedBy { summary ->
                val title = summary.title.lowercase()
                val isFirstFromApi = searchSummary.summaries.firstOrNull()?.title == summary.title

                // Priority Mapping: 1=Top Result, 2=Songs, 3=Videos, 4=Episodes/Podcasts, 5=Albums, 6=Artists, 7=Playlists, 8=Profiles, 9=Other
                when {
                    title.contains("top") || title.contains("principal") || title.contains("result") || title.contains("resultado") -> 1
                    title.contains("song") || title.contains("cancion") || title.contains("canción") -> 2
                    title.contains("video") -> 3
                    title.contains("episode") || title.contains("episodio") || title.contains("podcast") -> 4
                    title.contains("album") || title.contains("álbum") -> 5
                    title.contains("artist") || title.contains("artista") -> 6
                    title.contains("playlist") || title.contains("lista") -> 7
                    title.contains("profile") || title.contains("perfil") -> 8
                    isFirstFromApi -> 1 // Fallback if API returned it first but it's localized differently
                    summary.items.firstOrNull() is SongItem -> 2
                    summary.items.firstOrNull() is AlbumItem -> 5
                    summary.items.firstOrNull() is ArtistItem -> 6
                    summary.items.firstOrNull() is PlaylistItem -> 7
                    summary.items.firstOrNull() is EpisodeItem -> 4
                    summary.items.firstOrNull() is PodcastItem -> 4
                    else -> 9
                }
            }
    }

    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    val openItemMenu: (YTItem) -> Unit = { item ->
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
    }

    val onItemClick: (YTItem) -> Unit = { item ->
        when (item) {
            is SongItem -> {
                if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
            }
            is AlbumItem -> navController.navigate("album/${item.id}")
            is ArtistItem -> navController.navigate("artist/${item.id}")
            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
            is EpisodeItem -> {
                if (item.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                else playerConnection.playQueue(YouTubeQueue(WatchEndpoint(videoId = item.id), item.asSongItem().toMediaMetadata()))
            }
            is PodcastItem -> navController.navigate("online_playlist/${item.id}")
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

        LaunchedEffect(lazyListState) {
            snapshotFlow {
                lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
            }.collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                viewModel.loadMore()
            }
        }

        val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
            YouTubeListItem(
                item = item,
                isActive = isItemActive(item),
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(onClick = { openItemMenu(item) }) {
                        Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = { onItemClick(item) },
                        onLongClick = { openItemMenu(item) },
                    )
                    .animateItem(),
            )
        }

        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.add(WindowInsets(top = SearchFilterHeight + 8.dp)).asPaddingValues(),
        ) {
            if (searchFilter == null) {
                mergedSummaries?.forEachIndexed { index, summary ->
                    if (index > 0) {
                        item(key = "divider_$index") {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }

                    item(key = "section_header_${summary.title}_$index") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = getTranslatedSearchSummaryTitle(summary.title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    itemsIndexed(
                        items = summary.items,
                        key = { itemIndex, item -> "${summary.title}/${item.id}/$itemIndex" },
                    ) { _, item ->
                        ytItemContent(item)
                    }

                    item(key = "section_spacer_${summary.title}_$index") {
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (mergedSummaries?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found))
                    }
                }
            } else {
                items(
                    items = itemsPage?.items.orEmpty().distinctBy { it.id },
                    key = { "filtered_${it.id}" },
                    itemContent = ytItemContent,
                )

                if (itemsPage?.continuation != null) {
                    item(key = "loading") {
                        ShimmerHost {
                            repeat(3) { ListItemPlaceHolder() }
                        }
                    }
                }

                if (itemsPage?.items?.isEmpty() == true) {
                    item {
                        EmptyPlaceholder(icon = R.drawable.search, text = stringResource(R.string.no_results_found))
                    }
                }
            }

            if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                item {
                    ShimmerHost {
                        repeat(8) { ListItemPlaceHolder() }
                    }
                }
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top).add(WindowInsets(top = AppBarHeight)))
                .fillMaxWidth()
        ) {
            ChipsRow(
                chips = listOf(
                    null to stringResource(R.string.filter_all),
                    FILTER_SONG to stringResource(R.string.filter_songs),
                    FILTER_VIDEO to stringResource(R.string.filter_videos),
                    FILTER_ALBUM to stringResource(R.string.filter_albums),
                    FILTER_ARTIST to stringResource(R.string.filter_artists),
                    FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
                    FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
                ),
                currentValue = searchFilter,
                onValueUpdate = {
                    if (viewModel.filter.value != it) {
                        viewModel.filter.value = it
                    }
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(0)
                    }
                }
            )
        }
    } else {
        val lazyGridState = rememberLazyGridState()

        LaunchedEffect(lazyGridState) {
            snapshotFlow {
                lazyGridState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
            }.collect { shouldLoadMore ->
                if (!shouldLoadMore) return@collect
                viewModel.loadMore()
            }
        }

        val filters = listOf(
            null to stringResource(R.string.filter_all),
            FILTER_SONG to stringResource(R.string.filter_songs),
            FILTER_VIDEO to stringResource(R.string.filter_videos),
            FILTER_ALBUM to stringResource(R.string.filter_albums),
            FILTER_ARTIST to stringResource(R.string.filter_artists),
            FILTER_COMMUNITY_PLAYLIST to stringResource(R.string.filter_community_playlists),
            FILTER_FEATURED_PLAYLIST to stringResource(R.string.filter_featured_playlists),
        )

        val hasSpotlightBanner =
            searchFilter == null &&
                !mergedSummaries.isNullOrEmpty() &&
                mergedSummaries[0].items.isNotEmpty() &&
                mergedSummaries[0].items.size <= 3

        val sectionGridIndices = remember(mergedSummaries, hasSpotlightBanner) {
            buildMap<Int, Int> {
                if (mergedSummaries == null) return@buildMap
                if (hasSpotlightBanner) {
                    put(0, 0)
                    var runningIndex = 1
                    for (idx in 1 until mergedSummaries.size) {
                        put(idx, runningIndex)
                        runningIndex += 1 + mergedSummaries[idx].items.size
                    }
                } else {
                    var runningIndex = 0
                    mergedSummaries.forEachIndexed { idx, summary ->
                        put(idx, runningIndex)
                        runningIndex += 1 + summary.items.size
                    }
                }
            }
        }

        val topBottomInsets = LocalPlayerAwareWindowInsets.current
        val bottomPadding = topBottomInsets.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding()

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(topBottomInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Left Vertical Filter & Section Jump Rail
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(196.dp)
                    .fillMaxHeight(),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = bottomPadding + 10.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filters, key = { it.first?.value ?: "all" }) { (filterValue, label) ->
                        val selected = searchFilter == filterValue
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (viewModel.filter.value != filterValue) {
                                        viewModel.filter.value = filterValue
                                    }
                                    coroutineScope.launch {
                                        lazyGridState.animateScrollToItem(0)
                                    }
                                },
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                            }
                                        )
                                )
                                Spacer(Modifier.width(9.dp))
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
                                )
                            }
                        }
                    }

                    if (searchFilter == null && !mergedSummaries.isNullOrEmpty()) {
                        item(key = "jump_divider") {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 5.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            )
                        }

                        itemsIndexed(
                            items = mergedSummaries,
                            key = { idx, summary -> "jump_${summary.title}_$idx" },
                        ) { idx, summary ->
                            val targetIndex = sectionGridIndices[idx] ?: 0
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            lazyGridState.animateScrollToItem(targetIndex)
                                        }
                                    },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                ) {
                                    Text(
                                        text = getTranslatedSearchSummaryTitle(summary.title),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = summary.items.size.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Results Stage
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = bottomPadding + 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (searchFilter == null) {
                        if (hasSpotlightBanner && mergedSummaries != null) {
                            val primarySummary = mergedSummaries[0]
                            item(key = "hero_spotlight_banner", span = { GridItemSpan(2) }) {
                                LandscapeTopResultSpotlight(
                                    summary = primarySummary,
                                    isPlaying = isPlaying,
                                    isItemActive = isItemActive,
                                    onItemClick = onItemClick,
                                    onItemMenu = openItemMenu,
                                    onFilterJump = mapSummaryToFilter(primarySummary)?.let { targetFilter ->
                                        {
                                            viewModel.filter.value = targetFilter
                                            coroutineScope.launch { lazyGridState.animateScrollToItem(0) }
                                        }
                                    },
                                )
                            }
                        }

                        val remainingSummaries = if (hasSpotlightBanner) {
                            mergedSummaries?.drop(1).orEmpty()
                        } else {
                            mergedSummaries.orEmpty()
                        }
                        val indexOffset = if (hasSpotlightBanner) 1 else 0

                        remainingSummaries.forEachIndexed { localIndex, summary ->
                            val sectionIndex = localIndex + indexOffset
                            val mappedFilter = mapSummaryToFilter(summary)

                            item(
                                key = "grid_section_header_${summary.title}_$sectionIndex",
                                span = { GridItemSpan(2) },
                            ) {
                                LandscapeCategoryHeader(
                                    title = getTranslatedSearchSummaryTitle(summary.title),
                                    count = summary.items.size,
                                    onFilterJump = mappedFilter?.let { targetFilter ->
                                        {
                                            viewModel.filter.value = targetFilter
                                            coroutineScope.launch { lazyGridState.animateScrollToItem(0) }
                                        }
                                    },
                                    modifier = Modifier.padding(top = if (sectionIndex > 0) 6.dp else 0.dp),
                                )
                            }

                            itemsIndexed(
                                items = summary.items,
                                key = { itemIndex, item -> "grid_${summary.title}/${item.id}/$itemIndex" },
                            ) { _, item ->
                                LandscapeYouTubeItemCard(
                                    item = item,
                                    isActive = isItemActive(item),
                                    isPlaying = isPlaying,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    onClick = { onItemClick(item) },
                                    onMenuClick = { openItemMenu(item) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        if (mergedSummaries?.isEmpty() == true) {
                            item(key = "empty_summary", span = { GridItemSpan(2) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.no_results_found),
                                )
                            }
                        }
                    } else {
                        items(
                            items = itemsPage?.items.orEmpty().distinctBy { it.id },
                            key = { "grid_filtered_${it.id}" },
                        ) { item ->
                            LandscapeYouTubeItemCard(
                                item = item,
                                isActive = isItemActive(item),
                                isPlaying = isPlaying,
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                onClick = { onItemClick(item) },
                                onMenuClick = { openItemMenu(item) },
                                modifier = Modifier.animateItem(),
                            )
                        }

                        if (itemsPage?.continuation != null) {
                            item(key = "loading", span = { GridItemSpan(2) }) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        ShimmerHost { repeat(2) { ListItemPlaceHolder() } }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        ShimmerHost { repeat(2) { ListItemPlaceHolder() } }
                                    }
                                }
                            }
                        }

                        if (itemsPage?.items?.isEmpty() == true) {
                            item(key = "empty_filtered", span = { GridItemSpan(2) }) {
                                EmptyPlaceholder(
                                    icon = R.drawable.search,
                                    text = stringResource(R.string.no_results_found),
                                )
                            }
                        }
                    }

                    if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
                        item(key = "initial_loading", span = { GridItemSpan(2) }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    ShimmerHost { repeat(4) { ListItemPlaceHolder() } }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    ShimmerHost { repeat(4) { ListItemPlaceHolder() } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapeTopResultSpotlight(
    summary: SearchSummary,
    isPlaying: Boolean,
    isItemActive: (YTItem) -> Boolean,
    onItemClick: (YTItem) -> Unit,
    onItemMenu: (YTItem) -> Unit,
    onFilterJump: (() -> Unit)?,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (summary.items.size == 1) {
                val singleItem = summary.items.first()
                val active = isItemActive(singleItem)
                val typeLabel = when (singleItem) {
                    is SongItem -> stringResource(R.string.songs)
                    is AlbumItem -> stringResource(R.string.albums)
                    is ArtistItem -> stringResource(R.string.artists)
                    is PlaylistItem -> stringResource(R.string.playlists)
                    is EpisodeItem, is PodcastItem -> stringResource(R.string.episodes)
                }

                // Spotlight header row with type pill + quick primary action button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ) {
                        Text(
                            text = getTranslatedSearchSummaryTitle(summary.title),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                        shape = CircleShape,
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onItemClick(singleItem) },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    if (singleItem is SongItem || singleItem is EpisodeItem) {
                                        R.drawable.play
                                    } else {
                                        R.drawable.navigate_next
                                    }
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                // Full-width YouTubeListItem so subtitle & status badges never truncate
                Surface(
                    color = if (active) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = if (active) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                ) {
                    YouTubeListItem(
                        item = singleItem,
                        isActive = active,
                        isPlaying = isPlaying,
                        trailingContent = {
                            IconButton(onClick = { onItemMenu(singleItem) }) {
                                Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onItemClick(singleItem) },
                            onLongClick = { onItemMenu(singleItem) },
                        ),
                    )
                }
            } else {
                LandscapeCategoryHeader(
                    title = getTranslatedSearchSummaryTitle(summary.title),
                    count = summary.items.size,
                    onFilterJump = onFilterJump,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    summary.items.forEach { item ->
                        Box(modifier = Modifier.weight(1f)) {
                            LandscapeYouTubeItemCard(
                                item = item,
                                isActive = isItemActive(item),
                                isPlaying = isPlaying,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f),
                                onClick = { onItemClick(item) },
                                onMenuClick = { onItemMenu(item) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeCategoryHeader(
    title: String,
    count: Int,
    onFilterJump: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
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
        if (onFilterJump != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onFilterJump),
            ) {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LandscapeYouTubeItemCard(
    item: YTItem,
    isActive: Boolean,
    isPlaying: Boolean,
    containerColor: Color,
    onClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            containerColor
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
        YouTubeListItem(
            item = item,
            isActive = isActive,
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(onClick = onMenuClick) {
                    Icon(painterResource(R.drawable.more_vert), contentDescription = null)
                }
            },
            modifier = Modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onMenuClick,
            ),
        )
    }
}

private fun mapSummaryToFilter(summary: SearchSummary) =
    summary.title.lowercase().let { titleLower ->
        when {
            titleLower.contains("top") || titleLower.contains("principal") || titleLower.contains("result") || titleLower.contains("resultado") -> null
            titleLower.contains("song") || titleLower.contains("cancion") || titleLower.contains("canción") -> FILTER_SONG
            titleLower.contains("video") || titleLower.contains("vídeo") -> FILTER_VIDEO
            titleLower.contains("album") || titleLower.contains("álbum") -> FILTER_ALBUM
            titleLower.contains("artist") || titleLower.contains("artista") -> FILTER_ARTIST
            titleLower.contains("playlist") || titleLower.contains("lista") -> FILTER_COMMUNITY_PLAYLIST
            summary.items.firstOrNull() is SongItem -> FILTER_SONG
            summary.items.firstOrNull() is AlbumItem -> FILTER_ALBUM
            summary.items.firstOrNull() is ArtistItem -> FILTER_ARTIST
            summary.items.firstOrNull() is PlaylistItem -> FILTER_COMMUNITY_PLAYLIST
            else -> null
        }
    }

@Composable
private fun getTranslatedSearchSummaryTitle(title: String): String {
    val titleLower = title.lowercase()
    return when {
        titleLower.contains("top") || titleLower.contains("principal") || titleLower.contains("result") || titleLower.contains("resultado") -> stringResource(R.string.top_result)
        titleLower.contains("song") || titleLower.contains("cancion") || titleLower.contains("canción") -> stringResource(R.string.songs)
        titleLower.contains("video") || titleLower.contains("vídeo") -> stringResource(R.string.videos)
        titleLower.contains("episode") || titleLower.contains("episodio") || titleLower.contains("podcast") -> stringResource(R.string.episodes)
        titleLower.contains("album") || titleLower.contains("álbum") -> stringResource(R.string.albums)
        titleLower.contains("artist") || titleLower.contains("artista") -> stringResource(R.string.artists)
        titleLower.contains("playlist") || titleLower.contains("lista") -> stringResource(R.string.playlists)
        titleLower.contains("profile") || titleLower.contains("perfil") -> stringResource(R.string.profiles)
        titleLower.contains("other") || titleLower.contains("otros") -> stringResource(R.string.other)
        else -> title
    }
}