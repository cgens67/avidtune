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

    val strSongs = stringResource(R.string.filter_songs)
    val strVideos = stringResource(R.string.filter_videos)
    val strAlbums = stringResource(R.string.filter_albums)
    val strArtists = stringResource(R.string.filter_artists)
    val strPlaylists = stringResource(R.string.filter_playlists)

    // Group identical categories together, separate videos from songs, and sort them
    val mergedSummaries = remember(searchSummary, strSongs, strVideos, strAlbums, strArtists, strPlaylists) {
        val originalFirstTitle = searchSummary?.summaries?.firstOrNull()?.title ?: ""

        searchSummary?.summaries
            ?.flatMap { summary ->
                // YouTube Music API might group songs and videos together under "Songs"
                if (summary.title == "Songs") {
                    val actualSongs = summary.items.filter { it !is SongItem || !isActuallyVideo(it) }
                    val actualVideos = summary.items.filter { it is SongItem && isActuallyVideo(it) }

                    listOfNotNull(
                        if (actualSongs.isNotEmpty()) SearchSummary(strSongs, actualSongs) else null,
                        if (actualVideos.isNotEmpty()) SearchSummary(strVideos, actualVideos) else null
                    )
                } else {
                    // Translate other hardcoded categories
                    val newTitle = when (summary.title) {
                        "Albums" -> strAlbums
                        "Artists" -> strArtists
                        "Playlists" -> strPlaylists
                        "Videos" -> strVideos
                        else -> summary.title
                    }
                    listOf(SearchSummary(newTitle, summary.items))
                }
            }
            ?.groupBy { it.title }
            ?.map { (title, summaries) ->
                SearchSummary(
                    title = title,
                    items = summaries.flatMap { it.items }.distinctBy { it.id }
                )
            }
            ?.sortedBy { summary ->
                val title = summary.title.lowercase()
                when {
                    // 1. Top Result
                    summary.title == originalFirstTitle && originalFirstTitle !in listOf(strSongs, strVideos, strAlbums, strArtists, strPlaylists, "Songs", "Videos", "Albums", "Artists", "Playlists") -> 1
                    // 2. Songs
                    title == strSongs.lowercase() || title.contains("song") || title.contains("cancion") || title.contains("canción") -> 2
                    // 3. Videos
                    title == strVideos.lowercase() || title.contains("video") -> 3
                    // 4. Albums
                    title == strAlbums.lowercase() || title.contains("album") || title.contains("álbum") -> 4
                    // 5. Artists
                    title == strArtists.lowercase() || title.contains("artist") || title.contains("artista") -> 5
                    // 6. Playlists
                    title == strPlaylists.lowercase() || title.contains("playlist") || title.contains("lista") -> 6
                    // 7. Others
                    else -> 7
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

            if (mergedSummaries?.isEmpty() == true) {
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

        if (searchFilter == null && searchSummary == null || searchFilter != null && itemsPage == null) {
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
            }
        )
    }
}

private fun isActuallyVideo(item: YTItem): Boolean {
    if (item !is SongItem) return false
    val type = item.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return type == "MUSIC_VIDEO_TYPE_OMV" || type == "MUSIC_VIDEO_TYPE_UGC"
}
