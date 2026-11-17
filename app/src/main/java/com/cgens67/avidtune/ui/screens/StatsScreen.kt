@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.screens

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.StatPeriod
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.SongWithStats
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.IconButton as AtIconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.ArtistMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.isScrollingUp
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.viewmodels.StatsViewModel
import com.cgens67.innertube.models.WatchEndpoint
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalInset = if (isLandscape) 0.dp else 20.dp

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val fabOffset = if (playerBottom > systemBottom) playerBottom - systemBottom else 0.dp

    val indexChips by viewModel.indexChips.collectAsState()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsState()
    val mostPlayedSongsStats by viewModel.mostPlayedSongsStats.collectAsState()
    val mostPlayedArtists by viewModel.mostPlayedArtists.collectAsState()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsState()
    val firstEvent by viewModel.firstEvent.collectAsState()
    val selectedOption by viewModel.selectedOption.collectAsState()

    val songsById = remember(mostPlayedSongs) { mostPlayedSongs.associateBy { it.id } }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val artistsRowState = rememberLazyListState()
    val albumsRowState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val currentDate = remember { LocalDateTime.now() }

    val weeklyDates = remember(firstEvent) {
        val fe = firstEvent
        if (fe != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(fe.event.timestamp.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")

                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)

                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year

                    val text = when {
                        startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                        startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                        else -> "${date.dayOfMonth} - $endDateFormatted"
                    }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }
    }

    val monthlyDates = remember(firstEvent) {
        val fe = firstEvent
        if (fe != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1)
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(fe.event.timestamp.withDayOfMonth(1))
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text = if (date.year != currentDate.year) {
                        "$formattedDate ${date.year}"
                    } else {
                        formattedDate
                    }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }
    }

    val yearlyDates = remember(firstEvent) {
        val fe = firstEvent
        if (fe != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(fe.event.timestamp)
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }
    }

    val continuousChips = listOf(
        StatPeriod.WEEK_1.ordinal to pluralStringResource(R.plurals.n_week, 1, 1),
        StatPeriod.MONTH_1.ordinal to pluralStringResource(R.plurals.n_month, 1, 1),
        StatPeriod.MONTH_3.ordinal to pluralStringResource(R.plurals.n_month, 3, 3),
        StatPeriod.MONTH_6.ordinal to pluralStringResource(R.plurals.n_month, 6, 6),
        StatPeriod.YEAR_1.ordinal to pluralStringResource(R.plurals.n_year, 1, 1),
        StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
    )

    val activePeriodChips = when (selectedOption) {
        OptionStats.WEEKS -> weeklyDates
        OptionStats.MONTHS -> monthlyDates
        OptionStats.YEARS -> yearlyDates
        OptionStats.CONTINUOUS -> continuousChips
    }

    val activePeriodLabel = remember(activePeriodChips, indexChips, selectedOption) {
        activePeriodChips.firstOrNull { it.first == indexChips }?.second
            ?: activePeriodChips.firstOrNull()?.second
            ?: ""
    }

    val periodOptions = listOf(
        OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
        OptionStats.WEEKS to stringResource(R.string.weeks),
        OptionStats.MONTHS to stringResource(R.string.months),
        OptionStats.YEARS to stringResource(R.string.years),
    )

    val totalPlayTimeMs = remember(mostPlayedSongsStats) {
        mostPlayedSongsStats.sumOf { it.timeListened ?: 0L }
    }

    val topSongStat = mostPlayedSongsStats.firstOrNull()
    val hasAnyStats = mostPlayedSongsStats.isNotEmpty() || mostPlayedArtists.isNotEmpty() || mostPlayedAlbums.isNotEmpty()

    val shuffleMostPlayedAction: () -> Unit = {
        if (mostPlayedSongs.isNotEmpty()) {
            playerConnection.playQueue(
                ListQueue(
                    title = context.getString(R.string.most_played_songs),
                    items = mostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled()
                )
            )
        }
    }

    val songsPerPage = if (isLandscape) 3 else 4
    val songPages = remember(mostPlayedSongsStats, songsPerPage) {
        mostPlayedSongsStats.chunked(songsPerPage)
    }

    val statsSectionsContent: LazyListScope.() -> Unit = {
        if (!hasAnyStats) {
            item(key = "stats_empty") {
                StatsEmptyStateCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalInset, vertical = 36.dp)
                )
            }
        } else {
            if (mostPlayedSongsStats.isNotEmpty()) {
                item(key = "mostPlayedSongsHeader") {
                    StatsSectionHeader(
                        title = stringResource(R.string.songs),
                        countText = pluralStringResource(R.plurals.n_song, mostPlayedSongsStats.size, mostPlayedSongsStats.size),
                        modifier = Modifier
                            .padding(horizontal = horizontalInset)
                            .animateItem()
                    )
                }

                item(key = "mostPlayedSongsPager") {
                    AnimatedContent(
                        targetState = songPages,
                        transitionSpec = {
                            (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                                slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 16 })
                                .togetherWith(fadeOut(tween(180)))
                                .using(SizeTransform(clip = false))
                        },
                        label = "songs_period_transition",
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem()
                    ) { pages ->
                        val songsPagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

                        LaunchedEffect(selectedOption, indexChips) {
                            if (songsPagerState.currentPage != 0) {
                                songsPagerState.animateScrollToPage(0)
                            }
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            HorizontalPager(
                                state = songsPagerState,
                                verticalAlignment = Alignment.Top,
                                contentPadding = PaddingValues(horizontal = horizontalInset),
                                pageSpacing = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize()
                            ) { page ->
                                val pageItems = pages.getOrElse(page) { emptyList() }
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    pageItems.forEachIndexed { idxInPage, songStat ->
                                        val globalIndex = page * songsPerPage + idxInPage
                                        val matchedSong = songsById[songStat.id]
                                        val isActive = songStat.id == mediaMetadata?.id

                                        StatsSegmentedSongCard(
                                            rank = globalIndex + 1,
                                            songStat = songStat,
                                            thumbnailCornerRadius = thumbnailCornerRadius,
                                            isFirst = idxInPage == 0,
                                            isLast = idxInPage == pageItems.lastIndex,
                                            isActive = isActive,
                                            isPlaying = isPlaying,
                                            onClick = {
                                                if (isActive) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = WatchEndpoint(songStat.id),
                                                            preloadItem = matchedSong?.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                matchedSong?.let { fullSong ->
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = fullSong,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            },
                                            onMoreClick = if (matchedSong != null) {
                                                {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = matchedSong,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }

                            if (pages.size > 1) {
                                Spacer(Modifier.height(10.dp))
                                StatsPagerDotsIndicator(
                                    pageCount = pages.size,
                                    currentPage = songsPagerState.currentPage,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                }
            }

            if (mostPlayedArtists.isNotEmpty()) {
                item(key = "mostPlayedArtistsHeader") {
                    StatsSectionHeader(
                        title = stringResource(R.string.artists),
                        countText = pluralStringResource(R.plurals.n_artist, mostPlayedArtists.size, mostPlayedArtists.size),
                        modifier = Modifier
                            .padding(horizontal = horizontalInset)
                            .animateItem()
                    )
                }

                item(key = "mostPlayedArtistsRow") {
                    LazyRow(
                        state = artistsRowState,
                        contentPadding = PaddingValues(horizontal = horizontalInset),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isLandscape) {
                                    Modifier.horizontalFadingEdge(
                                        fadeWidth = 20.dp,
                                        fadeStart = artistsRowState.canScrollBackward,
                                        fadeEnd = artistsRowState.canScrollForward
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = mostPlayedArtists,
                            key = { _, artist -> artist.id },
                        ) { index, artist ->
                            StatsArtistCard(
                                rank = index + 1,
                                artist = artist,
                                onClick = {
                                    navController.navigate("artist/${artist.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        ArtistMenu(
                                            originalArtist = artist,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            if (mostPlayedAlbums.isNotEmpty()) {
                item(key = "mostPlayedAlbumsHeader") {
                    StatsSectionHeader(
                        title = stringResource(R.string.albums),
                        countText = pluralStringResource(R.plurals.n_album, mostPlayedAlbums.size, mostPlayedAlbums.size),
                        modifier = Modifier
                            .padding(horizontal = horizontalInset)
                            .animateItem()
                    )
                }

                item(key = "mostPlayedAlbumsRow") {
                    LazyRow(
                        state = albumsRowState,
                        contentPadding = PaddingValues(horizontal = horizontalInset),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isLandscape) {
                                    Modifier.horizontalFadingEdge(
                                        fadeWidth = 20.dp,
                                        fadeStart = albumsRowState.canScrollBackward,
                                        fadeEnd = albumsRowState.canScrollForward
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .animateItem()
                    ) {
                        itemsIndexed(
                            items = mostPlayedAlbums,
                            key = { _, album -> album.id },
                        ) { index, album ->
                            StatsAlbumCard(
                                rank = index + 1,
                                album = album,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                isActive = album.id == mediaMetadata?.album?.id,
                                isPlaying = isPlaying,
                                onClick = {
                                    navController.navigate("album/${album.id}")
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    menuState.show {
                                        AlbumMenu(
                                            originalAlbum = album,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats)) },
                navigationIcon = {
                    AtIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = { navController.navigate("insight") },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.auto_awesome),
                                contentDescription = stringResource(R.string.avidtune_insight),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = mostPlayedSongs.isNotEmpty() && lazyListState.isScrollingUp(),
                enter = fadeIn(tween(220)) + slideInVertically { it },
                exit = fadeOut(tween(180)) + slideOutVertically { it }
            ) {
                FloatingActionButton(
                    onClick = shuffleMostPlayedAction,
                    shape = FloatingActionButtonDefaults.shape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = fabOffset)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.40f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 8.dp, bottom = playerBottom + 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        StatsPeriodSelectorRow(
                            chips = activePeriodChips,
                            options = periodOptions,
                            selectedOption = selectedOption,
                            onSelectionChange = {
                                viewModel.selectedOption.value = it
                                viewModel.indexChips.value = 0
                            },
                            currentValue = indexChips,
                            onValueUpdate = { viewModel.indexChips.value = it }
                        )

                        StatsHeroSummaryCard(
                            periodLabel = activePeriodLabel,
                            totalPlayTimeMs = totalPlayTimeMs,
                            songsCount = mostPlayedSongsStats.size,
                            artistsCount = mostPlayedArtists.size,
                            albumsCount = mostPlayedAlbums.size,
                            topTrackTitle = topSongStat?.title,
                            topArtworkUrl = topSongStat?.thumbnailUrl,
                            hasData = hasAnyStats,
                            isCompactPane = true,
                            onClick = shuffleMostPlayedAction
                        )
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 104.dp)
                    ) {
                        statsSectionsContent()
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 104.dp)
                ) {
                    item(key = "period_selector") {
                        StatsPeriodSelectorRow(
                            chips = activePeriodChips,
                            options = periodOptions,
                            selectedOption = selectedOption,
                            onSelectionChange = {
                                viewModel.selectedOption.value = it
                                viewModel.indexChips.value = 0
                            },
                            currentValue = indexChips,
                            onValueUpdate = { viewModel.indexChips.value = it },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    item(key = "stats_hero_summary") {
                        StatsHeroSummaryCard(
                            periodLabel = activePeriodLabel,
                            totalPlayTimeMs = totalPlayTimeMs,
                            songsCount = mostPlayedSongsStats.size,
                            artistsCount = mostPlayedArtists.size,
                            albumsCount = mostPlayedAlbums.size,
                            topTrackTitle = topSongStat?.title,
                            topArtworkUrl = topSongStat?.thumbnailUrl,
                            hasData = hasAnyStats,
                            isCompactPane = false,
                            onClick = shuffleMostPlayedAction,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                    }

                    statsSectionsContent()
                }
            }
        }
    }
}

private fun Modifier.horizontalFadingEdge(
    fadeWidth: Dp = 20.dp,
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

@Composable
private fun StatsPeriodSelectorRow(
    chips: List<Pair<Int, String>>,
    options: List<Pair<OptionStats, String>>,
    selectedOption: OptionStats,
    onSelectionChange: (OptionStats) -> Unit,
    currentValue: Int,
    onValueUpdate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val chipsScrollState = rememberScrollState()
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "period_arrow_rotation"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Surface(
                onClick = { expanded = !expanded },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, top = 9.dp, end = 10.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = when (selectedOption) {
                            OptionStats.WEEKS -> stringResource(id = R.string.weeks)
                            OptionStats.MONTHS -> stringResource(id = R.string.months)
                            OptionStats.YEARS -> stringResource(id = R.string.years)
                            OptionStats.CONTINUOUS -> stringResource(id = R.string.continuous)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer(rotationZ = arrowRotation)
                    )
                }
            }

            MaterialTheme(
                shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(20.dp))
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 6.dp)
                ) {
                    options.forEach { (option, label) ->
                        val isSelected = option == selectedOption
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(
                                        if (isSelected) R.drawable.radio_button_checked
                                        else R.drawable.radio_button_unchecked
                                    ),
                                    contentDescription = null,
                                    tint = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            onClick = {
                                onSelectionChange(option)
                                expanded = false
                            },
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalFadingEdge(
                    fadeWidth = 12.dp,
                    fadeStart = chipsScrollState.canScrollBackward,
                    fadeEnd = chipsScrollState.canScrollForward
                )
                .horizontalScroll(chipsScrollState),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            chips.forEach { (value, label) ->
                val isSelected = currentValue == value
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    animationSpec = tween(220),
                    label = "period_chip_bg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(220),
                    label = "period_chip_fg"
                )
                val cornerRadius by animateDpAsState(
                    targetValue = if (isSelected) 12.dp else 20.dp,
                    animationSpec = tween(220),
                    label = "period_chip_radius"
                )

                Surface(
                    onClick = { onValueUpdate(value) },
                    shape = RoundedCornerShape(cornerRadius),
                    color = containerColor,
                    contentColor = contentColor
                ) {
                    Row(
                        modifier = Modifier
                            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(180)) + expandHorizontally(tween(220)),
                            exit = fadeOut(tween(140)) + shrinkHorizontally(tween(180))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                        Text(
                            text = label,
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
}

@Composable
private fun StatsHeroSummaryCard(
    periodLabel: String,
    totalPlayTimeMs: Long,
    songsCount: Int,
    artistsCount: Int,
    albumsCount: Int,
    topTrackTitle: String?,
    topArtworkUrl: String?,
    hasData: Boolean,
    isCompactPane: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stats_hero_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val headlineTime = remember(totalPlayTimeMs, hasData) {
        if (hasData && totalPlayTimeMs > 0L) makeTimeString(totalPlayTimeMs) else "0:00"
    }

    val countsSummary = listOf(
        pluralStringResource(R.plurals.n_song, songsCount, songsCount),
        pluralStringResource(R.plurals.n_artist, artistsCount, artistsCount),
        pluralStringResource(R.plurals.n_album, albumsCount, albumsCount),
    ).joinToString(" • ")

    val badgeText = periodLabel.ifBlank { stringResource(R.string.stats) }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .animateContentSize(animationSpec = tween(240, easing = FastOutSlowInEasing))
            .then(if (hasData) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (isCompactPane) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatsAnimatedPeriodBadge(
                        badgeText = badgeText,
                        hasData = hasData,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(Modifier.width(10.dp))

                    StatsHeroArtworkBadge(
                        artworkUrl = topArtworkUrl,
                        hasData = hasData,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        outerSize = 58.dp,
                        innerSize = 48.dp
                    )
                }

                AnimatedContent(
                    targetState = headlineTime,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                            .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 3 })
                            .using(SizeTransform(clip = false))
                    },
                    label = "hero_time_compact"
                ) { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedContent(
                    targetState = countsSummary,
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(150)))
                    },
                    label = "hero_counts_compact"
                ) { summary ->
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AnimatedContent(
                    targetState = topTrackTitle.orEmpty(),
                    transitionSpec = {
                        fadeIn(tween(220)).togetherWith(fadeOut(tween(150)))
                    },
                    label = "hero_top_track_compact"
                ) { track ->
                    if (track.isNotBlank()) {
                        Text(
                            text = "#1 $track",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatsAnimatedPeriodBadge(
                        badgeText = badgeText,
                        hasData = hasData
                    )

                    AnimatedContent(
                        targetState = headlineTime,
                        transitionSpec = {
                            (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 3 })
                                .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 3 })
                                .using(SizeTransform(clip = false))
                        },
                        label = "hero_time_portrait"
                    ) { time ->
                        Text(
                            text = time,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedContent(
                        targetState = countsSummary,
                        transitionSpec = {
                            fadeIn(tween(220)).togetherWith(fadeOut(tween(150)))
                        },
                        label = "hero_counts_portrait"
                    ) { summary ->
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedContent(
                        targetState = topTrackTitle.orEmpty(),
                        transitionSpec = {
                            fadeIn(tween(220)).togetherWith(fadeOut(tween(150)))
                        },
                        label = "hero_top_track_portrait"
                    ) { track ->
                        if (track.isNotBlank()) {
                            Text(
                                text = "#1 $track",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                StatsHeroArtworkBadge(
                    artworkUrl = topArtworkUrl,
                    hasData = hasData,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    outerSize = 84.dp,
                    innerSize = 68.dp
                )
            }
        }
    }
}

@Composable
private fun StatsAnimatedPeriodBadge(
    badgeText: String,
    hasData: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (hasData) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(220),
        label = "hero_badge_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (hasData) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "hero_badge_fg"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        modifier = modifier.animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
    ) {
        AnimatedContent(
            targetState = badgeText,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 })
                    .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 })
                    .using(SizeTransform(clip = false))
            },
            label = "hero_badge_text"
        ) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
private fun StatsHeroArtworkBadge(
    artworkUrl: String?,
    hasData: Boolean,
    pulseScale: Float,
    pulseAlpha: Float,
    outerSize: Dp,
    innerSize: Dp,
) {
    val haloSize = outerSize - 4.dp
    val circleSize = innerSize - 4.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(outerSize)
    ) {
        if (hasData) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }

        Crossfade(
            targetState = artworkUrl,
            animationSpec = tween(280),
            label = "hero_artwork_crossfade"
        ) { url ->
            if (!url.isNullOrBlank()) {
                AsyncImage(
                    model = url.resize(300, 300),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(innerSize)
                        .clip(RoundedCornerShape(if (innerSize >= 60.dp) 20.dp else 14.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(
                            if (hasData) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.equalizer),
                        contentDescription = stringResource(R.string.stats),
                        tint = if (hasData) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(circleSize * 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSectionHeader(
    title: String,
    countText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.animateContentSize(animationSpec = tween(220))
        ) {
            AnimatedContent(
                targetState = countText,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 })
                        .togetherWith(fadeOut(tween(140)) + slideOutVertically(tween(140)) { -it / 2 })
                        .using(SizeTransform(clip = false))
                },
                label = "section_count_text"
            ) { count ->
                Text(
                    text = count,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

private fun segmentedStatsCardShape(
    isFirst: Boolean,
    isLast: Boolean
): RoundedCornerShape {
    return RoundedCornerShape(
        topStart = if (isFirst) 22.dp else 6.dp,
        topEnd = if (isFirst) 22.dp else 6.dp,
        bottomStart = if (isLast) 22.dp else 6.dp,
        bottomEnd = if (isLast) 22.dp else 6.dp
    )
}

@Composable
private fun StatsSegmentedSongCard(
    rank: Int,
    songStat: SongWithStats,
    thumbnailCornerRadius: Float,
    isFirst: Boolean,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val cardShape = segmentedStatsCardShape(isFirst = isFirst, isLast = isLast)

    val rowBackgroundColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "stats_song_card_bg"
    )

    val titleColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isTopThree = rank <= 3

    Box(
        modifier = modifier
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
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when {
                    isActive -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f)
                    isTopThree -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                modifier = Modifier.widthIn(min = 32.dp)
            ) {
                Text(
                    text = "#$rank",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isActive -> MaterialTheme.colorScheme.onSecondaryContainer
                        isTopThree -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp.coerceAtMost(14.dp)))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = songStat.thumbnailUrl.resize(200, 200),
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
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = songStat.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )

                Text(
                    text = joinByBullet(
                        pluralStringResource(
                            R.plurals.n_time,
                            songStat.songCountListened,
                            songStat.songCountListened,
                        ),
                        makeTimeString(songStat.timeListened),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onMoreClick != null) {
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
}

@Composable
private fun StatsPagerDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    if (pageCount <= 1) return

    val maxVisibleDots = 8
    val safeCurrentPage = currentPage.coerceIn(0, pageCount - 1)
    val visibleCount = pageCount.coerceAtMost(maxVisibleDots)

    val animatedPage by animateFloatAsState(
        targetValue = safeCurrentPage.toFloat(),
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "pager_indicator_page"
    )

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.32f)

    val dotDiameter = 7.dp
    val activeWidth = 20.dp
    val spacing = 6.dp
    val edgePadding = 6.dp

    val totalDotsWidth = dotDiameter * (visibleCount - 1) + activeWidth + spacing * (visibleCount - 1)
    val canvasWidth = totalDotsWidth + edgePadding * 2

    Canvas(
        modifier = modifier
            .width(canvasWidth)
            .height(10.dp)
    ) {
        val dotPx = dotDiameter.toPx()
        val activePx = activeWidth.toPx()
        val gapPx = spacing.toPx()
        val padPx = edgePadding.toPx()
        val stepPx = dotPx + gapPx
        val centerY = size.height / 2f

        val maxStart = (pageCount - visibleCount).toFloat().coerceAtLeast(0f)
        val windowStart = if (pageCount <= maxVisibleDots) {
            0f
        } else {
            (animatedPage - 3f).coerceIn(0f, maxStart)
        }
        val cameraX = windowStart * stepPx

        var cursorX = 0f
        for (i in 0 until pageCount) {
            val distFromPage = abs(i - animatedPage)
            val activeFraction = (1f - distFromPage).coerceIn(0f, 1f)
            val widthPx = lerp(dotPx, activePx, activeFraction)

            val relIndex = i - windowStart
            if (relIndex > -1.2f && relIndex < visibleCount + 0.2f) {
                val leftEdgeFactor = if (windowStart > 0f) {
                    val t = ((relIndex + 0.8f) / 1.8f).coerceIn(0f, 1f)
                    lerp(1f, t, windowStart.coerceIn(0f, 1f))
                } else {
                    1f
                }
                val rightRemain = (visibleCount - 1) - relIndex
                val rightEdgeFactor = if (windowStart < maxStart) {
                    val t = ((rightRemain + 0.8f) / 1.8f).coerceIn(0f, 1f)
                    lerp(1f, t, (maxStart - windowStart).coerceIn(0f, 1f))
                } else {
                    1f
                }

                val edgeFactor = minOf(leftEdgeFactor, rightEdgeFactor)
                val finalScale = lerp(0.55f, 1f, maxOf(edgeFactor, activeFraction))
                val finalAlpha = when {
                    relIndex < 0f -> (1f + relIndex).coerceIn(0f, 1f)
                    relIndex > visibleCount - 1f -> (visibleCount - relIndex).coerceIn(0f, 1f)
                    else -> 1f
                }

                if (finalAlpha > 0.01f) {
                    val drawWidth = widthPx * finalScale
                    val drawHeight = dotPx * finalScale
                    val slotCenterX = padPx + (cursorX - cameraX) + widthPx / 2f
                    val drawLeft = slotCenterX - drawWidth / 2f
                    val drawTop = centerY - drawHeight / 2f

                    val baseColor = lerpColor(inactiveColor, activeColor, activeFraction)
                    drawRoundRect(
                        color = baseColor.copy(alpha = baseColor.alpha * finalAlpha),
                        topLeft = Offset(drawLeft, drawTop),
                        size = Size(drawWidth, drawHeight),
                        cornerRadius = CornerRadius(drawHeight / 2f, drawHeight / 2f)
                    )
                }
            }

            cursorX += widthPx + gapPx
        }
    }
}

@Composable
private fun StatsArtistCard(
    rank: Int,
    artist: Artist,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .width(140.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!artist.artist.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artist.artist.thumbnailUrl.resize(300, 300),
                        contentDescription = artist.artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.artist),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (rank == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset((-2).dp, (-2).dp)
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rank == 1) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = artist.artist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = joinByBullet(
                        pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                        makeTimeString(artist.timeListened?.toLong())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatsAlbumCard(
    rank: Int,
    album: Album,
    thumbnailCornerRadius: Float,
    isActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "stats_album_card_bg"
    )

    val titleColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = if (isActive) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val artCorner = thumbnailCornerRadius.dp.coerceIn(10.dp, 20.dp)
    val playCount = album.songCountListened ?: 0

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = modifier
            .width(152.dp)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(artCorner))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (!album.album.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = album.album.thumbnailUrl.resize(400, 400),
                        contentDescription = album.album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.album),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

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

                Surface(
                    shape = CircleShape,
                    color = if (rank == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (rank == 1) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = album.album.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = joinByBullet(
                        pluralStringResource(R.plurals.n_time, playCount, playCount),
                        makeTimeString(album.timeListened?.toLong())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatsEmptyStateCard(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_stats_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
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
                    painter = painterResource(R.drawable.equalizer),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.stats),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.check_back_later),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }
