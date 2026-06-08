package com.cgens67.avidtune.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_ALBUM
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_ARTIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_COMMUNITY_PLAYLIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_FEATURED_PLAYLIST
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_SONG
import com.cgens67.innertube.YouTube.SearchFilter.Companion.FILTER_VIDEO
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.YTItem
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
import com.cgens67.innertube.pages.SearchSummary
import com.cgens67.avidtune.viewmodels.OnlineSearchViewModel
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
    val lazyListState = rememberLazyListState()

    val searchFilter by viewModel.filter.collectAsState()
    val searchSummary = viewModel.summaryPage
    
    val itemsPage by remember(searchFilter) {
        derivedStateOf {
            searchFilter?.value?.let {
                viewModel.viewStateMap[it]
            }
        }
    }

    val filterSongsTitle = stringResource(R.string.filter_songs)
    val filterVideosTitle = stringResource(R.string.filter_videos)
    val filterAlbumsTitle = stringResource(R.string.filter_albums)
    val filterArtistsTitle = stringResource(R.string.filter_artists)
    val filterPlaylistsTitle = stringResource(R.string.filter_playlists)

    // Reconstruct sections extracting actual contents and keeping strict priority
    val allModeSections = remember(searchSummary, itemsPage) {
        if (searchSummary == null) return@remember emptyList<SearchSummary>()

        val allItems = searchSummary.summaries.flatMap { it.items }.distinctBy { it.id }
        
        // Ensure the absolute first item returned retains its "Top result" priority
        val firstSummaryTitle = searchSummary.summaries.firstOrNull()?.title ?: ""
        val topResultItems = searchSummary.summaries.firstOrNull()?.items ?: emptyList()

        val remainingFromSummary = allItems.filterNot { it.id in topResultItems.map { t -> t.id } }

        // Analyze specific YouTube music video types to separate regular songs from videos
        val songs = remainingFromSummary.filterIsInstance<SongItem>().filter {
            val type = it.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
            type != "MUSIC_VIDEO_TYPE_OMV" && type != "MUSIC_VIDEO_TYPE_UGC"
        }.toMutableList()
        
        val videos = remainingFromSummary.filterIsInstance<SongItem>().filter {
            val type = it.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
            type == "MUSIC_VIDEO_TYPE_OMV" || type == "MUSIC_VIDEO_TYPE_UGC"
        }.toMutableList()

        val albums = remainingFromSummary.filterIsInstance<AlbumItem>().toMutableList()
        val artists = remainingFromSummary.filterIsInstance<ArtistItem>().toMutableList()
        val playlists = remainingFromSummary.filterIsInstance<PlaylistItem>().toMutableList()

        // Append any cached items loaded directly from the specific tabs ("Original logic")
        viewModel.viewStateMap[FILTER_SONG.value]?.items?.filterIsInstance<SongItem>()?.let { songs.addAll(it) }
        viewModel.viewStateMap[FILTER_VIDEO.value]?.items?.filterIsInstance<SongItem>()?.let { videos.addAll(it) }
        viewModel.viewStateMap[FILTER_ALBUM.value]?.items?.filterIsInstance<AlbumItem>()?.let { albums.addAll(it) }
        viewModel.viewStateMap[FILTER_ARTIST.value]?.items?.filterIsInstance<ArtistItem>()?.let { artists.addAll(it) }
        viewModel.viewStateMap[FILTER_COMMUNITY_PLAYLIST.value]?.items?.filterIsInstance<PlaylistItem>()?.let { playlists.addAll(it) }
        viewModel.viewStateMap[FILTER_FEATURED_PLAYLIST.value]?.items?.filterIsInstance<PlaylistItem>()?.let { playlists.addAll(it) }

        buildList {
            if (topResultItems.isNotEmpty()) {
                add(SearchSummary(title = firstSummaryTitle, items = topResultItems.distinctBy { it.id }))
            }
            if (songs.isNotEmpty()) {
                add(SearchSummary(title = filterSongsTitle, items = songs.distinctBy { it.id }))
            }
            if (videos.isNotEmpty()) {
                add(SearchSummary(title = filterVideosTitle, items = videos.distinctBy { it.id }))
            }
            if (albums.isNotEmpty()) {
                add(SearchSummary(title = filterAlbumsTitle, items = albums.distinctBy { it.id }))
            }
            if (artists.isNotEmpty()) {
                add(SearchSummary(title = filterArtistsTitle, items = artists.distinctBy { it.id }))
            }
            if (playlists.isNotEmpty()) {
                add(SearchSummary(title = filterPlaylistsTitle, items = playlists.distinctBy { it.id }))
            }
        }
    }

    val isAllModeLoaded =
        searchSummary != null ||
            listOf(
                FILTER_SONG,
                FILTER_VIDEO,
                FILTER_ALBUM,
                FILTER_ARTIST,
                FILTER_COMMUNITY_PLAYLIST,
                FILTER_FEATURED_PLAYLIST,
            ).all { viewModel.viewStateMap.containsKey(it.value) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo.any { it.key == "loading" }
        }.collect { shouldLoadMore ->
            if (!shouldLoadMore) return@collect
            viewModel.loadMore()
        }
    }

    val ytItemContent: @Composable LazyItemScope.(YTItem) -> Unit = { item: YTItem ->
        val longClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                }
            }
        }
        YouTubeListItem(
            item = item,
            isActive =
            when (item) {
                is SongItem -> mediaMetadata?.id == item.id
                is AlbumItem -> mediaMetadata?.album?.id == item.id
                else -> false
            },
            isPlaying = isPlaying,
            trailingContent = {
                IconButton(
                    onClick = longClick,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                    )
                }
            },
            modifier =
            Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = item.id),
                                            item.toMediaMetadata()
                                        )
                                    )
                                }
                            }

                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = longClick,
                )
                .animateItem(),
        )
    }

    LazyColumn(
        state = lazyListState,
        contentPadding =
        LocalPlayerAwareWindowInsets.current
            .add(WindowInsets(top = SearchFilterHeight + 8.dp))
            .asPaddingValues(),
    ) {
        if (searchFilter == null) {
            allModeSections.forEachIndexed { index, summary ->
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
                            text = summary.title,
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

            if (allModeSections.isEmpty() && isAllModeLoaded) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
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
                        repeat(3) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            }

            if (itemsPage?.items?.isEmpty() == true) {
                item {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (searchFilter == null && allModeSections.isEmpty() && !isAllModeLoaded || searchFilter != null && itemsPage == null) {
            item {
                ShimmerHost {
                    repeat(8) {
                        ListItemPlaceHolder()
                    }
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
            chips =
            listOf(
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
            },
        )
    }
}
