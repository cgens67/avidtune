@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.screens

import android.content.res.Configuration
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.HistorySource
import com.cgens67.avidtune.constants.InnerTubeCookieKey
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.PauseListenHistoryKey
import com.cgens67.avidtune.db.entities.Event
import com.cgens67.avidtune.db.entities.EventWithSong
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.IconButton as AtIconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.menu.SelectionMediaMetadataMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.isScrollingUp
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.DateAgo
import com.cgens67.avidtune.viewmodels.HistoryViewModel
import com.cgens67.innertube.utils.parseCookieString
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class PendingUndoHistory(
    val id: Long,
    val events: List<Event>,
    val message: String,
)

@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val fabOffset = if (playerBottom > systemBottom) playerBottom - systemBottom else 0.dp

    val historySource by viewModel.historySource.collectAsState()
    val events by viewModel.events.collectAsState()
    val historyPage by viewModel.historyPage

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (pauseListenHistory, onPauseListenHistoryChange) = rememberPreference(
        key = PauseListenHistoryKey,
        defaultValue = false
    )

    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && historySource == HistorySource.REMOTE) {
            viewModel.historySource.value = HistorySource.LOCAL
        }
    }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    var selection by remember { mutableStateOf(false) }
    val selectedLocalEventIds = remember { mutableStateListOf<Long>() }
    val selectedRemoteSongIds = remember { mutableStateListOf<String>() }

    fun clearSelection() {
        selection = false
        selectedLocalEventIds.clear()
        selectedRemoteSongIds.clear()
    }

    LaunchedEffect(historySource) {
        clearSelection()
    }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
            focusManager.clearFocus()
        }
    } else if (selection) {
        BackHandler {
            clearSelection()
        }
    }

    val searchQueryText = query.text.trim()

    val filteredEvents = remember(events, searchQueryText) {
        if (searchQueryText.isEmpty()) {
            events
        } else {
            events.mapValues { (_, list) ->
                list.filter { entry ->
                    entry.song.song.title.contains(searchQueryText, ignoreCase = true) ||
                        entry.song.artists.any { it.name.contains(searchQueryText, ignoreCase = true) }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    val allFilteredLocalEvents = remember(filteredEvents) {
        filteredEvents.values.flatten()
    }

    val filteredRemoteSections = remember(historyPage, searchQueryText) {
        val sections = historyPage?.sections ?: return@remember null
        if (searchQueryText.isEmpty()) {
            sections
        } else {
            sections.map { section ->
                section.copy(
                    songs = section.songs.filter { song ->
                        song.title.contains(searchQueryText, ignoreCase = true) ||
                            song.artists.any { it.name.contains(searchQueryText, ignoreCase = true) }
                    }
                )
            }.filter { it.songs.isNotEmpty() }
        }
    }

    val allFilteredRemoteSongs = remember(filteredRemoteSections) {
        filteredRemoteSections?.flatMap { it.songs }.orEmpty()
    }

    val isRemote = historySource == HistorySource.REMOTE && isLoggedIn

    val totalTrackCount = if (isRemote) {
        allFilteredRemoteSongs.size
    } else {
        allFilteredLocalEvents.size
    }

    val totalPlayTimeMs = remember(allFilteredLocalEvents) {
        allFilteredLocalEvents.sumOf { it.event.playTime }
    }

    val latestArtworkUrl = remember(isRemote, allFilteredLocalEvents, allFilteredRemoteSongs) {
        if (isRemote) {
            allFilteredRemoteSongs.firstOrNull()?.thumbnail
        } else {
            allFilteredLocalEvents.firstOrNull()?.song?.song?.thumbnailUrl
        }
    }

    val latestTrackTitle = remember(isRemote, allFilteredLocalEvents, allFilteredRemoteSongs) {
        if (isRemote) {
            allFilteredRemoteSongs.firstOrNull()?.title
        } else {
            allFilteredLocalEvents.firstOrNull()?.song?.song?.title
        }
    }

    val lazyListState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    var pendingUndo by remember { mutableStateOf<PendingUndoHistory?>(null) }
    var lastUndoState by remember { mutableStateOf<PendingUndoHistory?>(null) }
    val restoredEventGenerations = remember { mutableStateMapOf<Long, Int>() }

    LaunchedEffect(pendingUndo) {
        if (pendingUndo != null) {
            lastUndoState = pendingUndo
        }
    }

    LaunchedEffect(pendingUndo?.id) {
        val currentId = pendingUndo?.id ?: return@LaunchedEffect
        delay(4500L)
        if (pendingUndo?.id == currentId) {
            pendingUndo = null
        }
    }

    var isRefreshingRemote by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val triggerRemoteRefresh: () -> Unit = {
        isRefreshingRemote = true
        viewModel.fetchRemoteHistory()
        coroutineScope.launch {
            delay(750L)
            isRefreshingRemote = false
        }
    }

    val removeLocalEventsWithUndo: (List<EventWithSong>) -> Unit = { toRemove ->
        if (toRemove.isNotEmpty()) {
            val rawEvents = toRemove.map { it.event }
            database.query {
                rawEvents.forEach { delete(it) }
            }
            val msg = if (toRemove.size == 1) {
                "${toRemove.first().song.song.title} • ${context.getString(R.string.history)}"
            } else {
                val countStr = context.resources.getQuantityString(R.plurals.n_song, toRemove.size, toRemove.size)
                "$countStr • ${context.getString(R.string.history)}"
            }
            pendingUndo = PendingUndoHistory(
                id = System.nanoTime(),
                events = rawEvents,
                message = msg
            )
        }
    }

    val shuffleAllAction: () -> Unit = {
        if (isRemote) {
            if (allFilteredRemoteSongs.isNotEmpty()) {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.history),
                        items = allFilteredRemoteSongs.shuffled().map { it.toMediaItem() }
                    )
                )
            }
        } else {
            if (allFilteredLocalEvents.isNotEmpty()) {
                playerConnection.playQueue(
                    ListQueue(
                        title = context.getString(R.string.history),
                        items = allFilteredLocalEvents.shuffled().map { it.song.toMediaItem() }
                    )
                )
            }
        }
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val timeFormatter = remember(is24Hour) {
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
    }

    val downloads by LocalDownloadUtil.current.downloads.collectAsState()

    val isContentEmpty = if (isRemote) {
        historyPage != null && allFilteredRemoteSongs.isEmpty()
    } else {
        allFilteredLocalEvents.isEmpty()
    }

    val selectedCount = if (isRemote) selectedRemoteSongIds.size else selectedLocalEventIds.size
    val totalSelectableCount = if (isRemote) {
        allFilteredRemoteSongs.distinctBy { it.id }.size
    } else {
        allFilteredLocalEvents.size
    }
    val allSelected = totalSelectableCount > 0 && selectedCount == totalSelectableCount

    val timelineListContent: LazyListScope.() -> Unit = {
        if (isRemote) {
            when {
                historyPage == null -> {
                    item(key = "remote_loading") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ContainedLoadingIndicator()
                        }
                    }
                }

                filteredRemoteSections.isNullOrEmpty() -> {
                    item(key = "remote_empty") {
                        HistoryEmptyStateCard(
                            isSearching = searchQueryText.isNotEmpty(),
                            isPaused = false,
                            onResumeHistory = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                }

                else -> {
                    filteredRemoteSections.forEachIndexed { sectionIndex, section ->
                        item(key = "remote_header_${sectionIndex}_${section.title}") {
                            HistorySectionHeader(
                                title = section.title,
                                count = section.songs.size,
                                modifier = Modifier.animateItem()
                            )
                        }

                        itemsIndexed(
                            items = section.songs,
                            key = { songIdx, song -> "remote_${sectionIndex}_${song.id}_$songIdx" }
                        ) { songIdx, song ->
                            val isFirst = songIdx == 0
                            val isLast = songIdx == section.songs.lastIndex
                            val isSelected = selection && song.id in selectedRemoteSongIds
                            val isActive = song.id == mediaMetadata?.id
                            val downloadState = downloads[song.id]?.state

                            HistorySegmentedSongCard(
                                title = song.title,
                                subtitle = joinByBullet(
                                    song.artists.joinToString { it.name },
                                    song.duration?.let { makeTimeString(it * 1000L) }
                                ),
                                thumbnailUrl = song.thumbnail,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                trailingTimeLabel = null,
                                isFirst = isFirst,
                                isLast = isLast,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                isSelected = isSelected,
                                isExplicit = song.explicit,
                                isLiked = false,
                                isInLibrary = false,
                                isDownloaded = downloadState == Download.STATE_COMPLETED,
                                onClick = {
                                    if (selection) {
                                        if (song.id in selectedRemoteSongIds) {
                                            selectedRemoteSongIds.remove(song.id)
                                            if (selectedRemoteSongIds.isEmpty()) selection = false
                                        } else {
                                            selectedRemoteSongIds.add(song.id)
                                        }
                                    } else {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selection = true
                                    if (song.id !in selectedRemoteSongIds) {
                                        selectedRemoteSongIds.add(song.id)
                                    }
                                },
                                onMoreClick = {
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )

                            if (!isLast) {
                                Spacer(Modifier.height(3.dp))
                            } else {
                                Spacer(Modifier.height(14.dp))
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredEvents.isEmpty()) {
                item(key = "local_empty") {
                    HistoryEmptyStateCard(
                        isSearching = searchQueryText.isNotEmpty(),
                        isPaused = pauseListenHistory && searchQueryText.isEmpty(),
                        onResumeHistory = { onPauseListenHistoryChange(false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            } else {
                filteredEvents.forEach { (dateAgo, dateEvents) ->
                    val dKey = dateAgoKey(dateAgo)
                    item(key = "local_header_$dKey") {
                        HistorySectionHeader(
                            title = formatDateAgoTitle(context, dateAgo),
                            count = dateEvents.size,
                            modifier = Modifier.animateItem()
                        )
                    }

                    itemsIndexed(
                        items = dateEvents,
                        key = { _, eventWithSong ->
                            val gen = restoredEventGenerations[eventWithSong.event.id] ?: 0
                            "local_${dKey}_${eventWithSong.event.id}_$gen"
                        }
                    ) { index, eventWithSong ->
                        val currentItem by rememberUpdatedState(eventWithSong)
                        val gen = restoredEventGenerations[eventWithSong.event.id] ?: 0
                        val isFirst = index == 0
                        val isLast = index == dateEvents.lastIndex
                        val isSelected = selection && eventWithSong.event.id in selectedLocalEventIds
                        val isActive = eventWithSong.song.id == mediaMetadata?.id
                        val downloadState = downloads[eventWithSong.song.id]?.state

                        val cardShape = segmentedCardShape(
                            isFirst = isFirst,
                            isLast = isLast,
                            isSelected = isSelected
                        )

                        val dismissBoxState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { it * 0.45f }
                        )
                        var processedDismiss by remember(eventWithSong.event.id, gen) { mutableStateOf(false) }
                        var armed by remember(eventWithSong.event.id, gen) {
                            mutableStateOf(dismissBoxState.currentValue == SwipeToDismissBoxValue.Settled)
                        }

                        LaunchedEffect(eventWithSong.event.id, gen) {
                            if (dismissBoxState.currentValue != SwipeToDismissBoxValue.Settled) {
                                dismissBoxState.snapTo(SwipeToDismissBoxValue.Settled)
                            }
                        }

                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (armed && !processedDismiss && dv == SwipeToDismissBoxValue.EndToStart) {
                                processedDismiss = true
                                removeLocalEventsWithUndo(listOf(currentItem))
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                armed = true
                                processedDismiss = false
                            }
                        }

                        val cardContent: @Composable () -> Unit = {
                            HistorySegmentedSongCard(
                                title = eventWithSong.song.song.title,
                                subtitle = joinByBullet(
                                    eventWithSong.song.artists.joinToString { it.name },
                                    makeTimeString(eventWithSong.song.song.duration * 1000L)
                                ),
                                thumbnailUrl = eventWithSong.song.song.thumbnailUrl,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                trailingTimeLabel = runCatching {
                                    eventWithSong.event.timestamp.format(timeFormatter)
                                }.getOrNull(),
                                isFirst = isFirst,
                                isLast = isLast,
                                isActive = isActive,
                                isPlaying = isPlaying,
                                isSelected = isSelected,
                                isExplicit = eventWithSong.song.song.title.contains("explicit", ignoreCase = true),
                                isLiked = eventWithSong.song.song.liked,
                                isInLibrary = eventWithSong.song.song.inLibrary != null,
                                isDownloaded = downloadState == Download.STATE_COMPLETED,
                                onClick = {
                                    if (selection) {
                                        val eid = eventWithSong.event.id
                                        if (eid in selectedLocalEventIds) {
                                            selectedLocalEventIds.remove(eid)
                                            if (selectedLocalEventIds.isEmpty()) selection = false
                                        } else {
                                            selectedLocalEventIds.add(eid)
                                        }
                                    } else {
                                        if (eventWithSong.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            val startIdx = allFilteredLocalEvents.indexOfFirst {
                                                it.event.id == eventWithSong.event.id
                                            }.coerceAtLeast(0)
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = formatDateAgoTitle(context, dateAgo),
                                                    items = allFilteredLocalEvents.map { it.song.toMediaItem() },
                                                    startIndex = startIdx
                                                )
                                            )
                                        }
                                    }
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selection = true
                                    val eid = eventWithSong.event.id
                                    if (eid !in selectedLocalEventIds) {
                                        selectedLocalEventIds.add(eid)
                                    }
                                },
                                onMoreClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = eventWithSong.song,
                                            event = eventWithSong.event,
                                            navController = navController,
                                            onDismiss = menuState::dismiss
                                        )
                                    }
                                }
                            )
                        }

                        Box(modifier = Modifier.animateItem()) {
                            if (selection) {
                                cardContent()
                            } else {
                                SwipeToDismissBox(
                                    state = dismissBoxState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val isSwiping = dismissBoxState.targetValue == SwipeToDismissBoxValue.EndToStart
                                        val bgColor by animateColorAsState(
                                            targetValue = if (isSwiping) {
                                                MaterialTheme.colorScheme.errorContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            label = "swipe_bg"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(cardShape)
                                                .background(bgColor)
                                                .padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.delete),
                                                contentDescription = stringResource(R.string.remove_from_history),
                                                tint = if (isSwiping) {
                                                    MaterialTheme.colorScheme.onErrorContainer
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                },
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                ) {
                                    cardContent()
                                }
                            }
                        }

                        if (!isLast) {
                            Spacer(Modifier.height(3.dp))
                        } else {
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }

    val fabBottomExtra by animateDpAsState(
        targetValue = if (pendingUndo != null) 76.dp else 0.dp,
        animationSpec = tween(220),
        label = "fab_undo_clearance"
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = Triple(selection, isSearching, selectedCount),
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "history_top_bar_title"
                    ) { (isSel, isSearch, selCount) ->
                        when {
                            isSel -> {
                                Text(
                                    text = pluralStringResource(R.plurals.n_selected, selCount, selCount),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            isSearch -> {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.search),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        BasicTextField(
                                            value = query,
                                            onValueChange = { query = it },
                                            singleLine = true,
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(focusRequester),
                                            decorationBox = { innerTextField ->
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.CenterStart
                                                ) {
                                                    if (query.text.isEmpty()) {
                                                        Text(
                                                            text = stringResource(R.string.search),
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        if (query.text.isNotEmpty()) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.clear),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .clickable { query = TextFieldValue() }
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                Text(text = stringResource(R.string.history))
                            }
                        }
                    }
                },
                navigationIcon = {
                    AtIconButton(
                        onClick = {
                            when {
                                isSearching -> {
                                    isSearching = false
                                    query = TextFieldValue()
                                    focusManager.clearFocus()
                                }
                                selection -> clearSelection()
                                else -> navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!isSearching && !selection) {
                                navController.backToMain()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(
                                if (selection || isSearching) R.drawable.close else R.drawable.arrow_back
                            ),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (!selection && !isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                        if (isRemote) {
                            IconButton(onClick = triggerRemoteRefresh) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = stringResource(R.string.action_retry)
                                )
                            }
                        } else if (events.isNotEmpty()) {
                            IconButton(onClick = { showClearHistoryDialog = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.delete_history),
                                    contentDescription = stringResource(R.string.clear_listen_history)
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !selection && !isContentEmpty && lazyListState.isScrollingUp(),
                enter = fadeIn(tween(220)) + slideInVertically { it },
                exit = fadeOut(tween(180)) + slideOutVertically { it }
            ) {
                FloatingActionButton(
                    onClick = shuffleAllAction,
                    shape = FloatingActionButtonDefaults.shape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = fabOffset + fabBottomExtra)
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
                .imePadding()
                .let { mod ->
                    if (isRemote) {
                        mod.pullToRefresh(
                            state = pullToRefreshState,
                            isRefreshing = isRefreshingRemote,
                            onRefresh = triggerRemoteRefresh
                        )
                    } else mod
                }
        ) {
            if (isLandscape && !isSearching) {
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
                        HistoryHeroSummaryCard(
                            isRemote = isRemote,
                            trackCount = totalTrackCount,
                            totalPlayTimeMs = totalPlayTimeMs,
                            latestTrackTitle = latestTrackTitle,
                            latestArtworkUrl = latestArtworkUrl,
                            onShuffleClick = shuffleAllAction,
                            hasTracks = !isContentEmpty,
                            isCompactPane = true
                        )

                        if (isLoggedIn) {
                            HistorySourceSwitcher(
                                selectedSource = historySource,
                                onSelectSource = { source ->
                                    viewModel.historySource.value = source
                                    if (source == HistorySource.REMOTE) {
                                        viewModel.fetchRemoteHistory()
                                    }
                                }
                            )
                        }

                        AnimatedVisibility(
                            visible = !isRemote && pauseListenHistory,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PausedHistoryWarningCard(
                                onResume = { onPauseListenHistoryChange(false) }
                            )
                        }
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(0.60f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 104.dp)
                    ) {
                        timelineListContent()
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 104.dp)
                ) {
                    if (!isSearching) {
                        item(key = "hero_summary") {
                            HistoryHeroSummaryCard(
                                isRemote = isRemote,
                                trackCount = totalTrackCount,
                                totalPlayTimeMs = totalPlayTimeMs,
                                latestTrackTitle = latestTrackTitle,
                                latestArtworkUrl = latestArtworkUrl,
                                onShuffleClick = shuffleAllAction,
                                hasTracks = !isContentEmpty,
                                isCompactPane = false
                            )
                            Spacer(Modifier.height(14.dp))
                        }

                        if (isLoggedIn) {
                            item(key = "source_switcher") {
                                HistorySourceSwitcher(
                                    selectedSource = historySource,
                                    onSelectSource = { source ->
                                        viewModel.historySource.value = source
                                        if (source == HistorySource.REMOTE) {
                                            viewModel.fetchRemoteHistory()
                                        }
                                    }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        if (!isRemote && pauseListenHistory) {
                            item(key = "paused_history_banner") {
                                PausedHistoryWarningCard(
                                    onResume = { onPauseListenHistoryChange(false) }
                                )
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                    }

                    timelineListContent()
                }
            }

            if (isRemote) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            val scale = if (isRefreshingRemote) {
                                1f
                            } else {
                                LinearOutSlowInEasing.transform(pullToRefreshState.distanceFraction)
                                    .coerceIn(0f, 1f)
                            }
                            scaleX = scale
                            scaleY = scale
                        }
                ) {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshingRemote
                    )
                }
            }

            AnimatedVisibility(
                visible = pendingUndo != null && !selection,
                enter = fadeIn(tween(200)) + slideInVertically { it },
                exit = fadeOut(tween(150)) + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(20f)
                    .padding(bottom = fabOffset + 16.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val undoState = pendingUndo ?: lastUndoState
                if (undoState != null) {
                    HistoryUndoBar(
                        message = undoState.message,
                        onUndo = {
                            if (pendingUndo?.id == undoState.id) {
                                val eventsToRestore = undoState.events
                                pendingUndo = null
                                eventsToRestore.forEach { ev ->
                                    restoredEventGenerations[ev.id] =
                                        (restoredEventGenerations[ev.id] ?: 0) + 1
                                }
                                database.query {
                                    eventsToRestore.forEach { insert(it) }
                                }
                            }
                        },
                        onDismiss = {
                            if (pendingUndo?.id == undoState.id) {
                                pendingUndo = null
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = selection,
                enter = fadeIn(tween(220)) + slideInVertically { it },
                exit = fadeOut(tween(180)) + slideOutVertically { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(21f)
                    .padding(bottom = fabOffset + 16.dp)
                    .padding(horizontal = 16.dp)
            ) {
                HistorySelectionFloatingToolbar(
                    allSelected = allSelected,
                    canDelete = !isRemote,
                    onClose = ::clearSelection,
                    onToggleSelectAll = {
                        if (isRemote) {
                            val distinctIds = allFilteredRemoteSongs.map { it.id }.distinct()
                            if (allSelected) {
                                selectedRemoteSongIds.clear()
                            } else {
                                selectedRemoteSongIds.clear()
                                selectedRemoteSongIds.addAll(distinctIds)
                            }
                        } else {
                            if (allSelected) {
                                selectedLocalEventIds.clear()
                            } else {
                                selectedLocalEventIds.clear()
                                selectedLocalEventIds.addAll(allFilteredLocalEvents.map { it.event.id })
                            }
                        }
                    },
                    onOpenMenu = {
                        val metadataList = if (isRemote) {
                            val idSet = selectedRemoteSongIds.toSet()
                            allFilteredRemoteSongs
                                .filter { it.id in idSet }
                                .distinctBy { it.id }
                                .map { it.toMediaMetadata() }
                        } else {
                            val idSet = selectedLocalEventIds.toSet()
                            allFilteredLocalEvents
                                .filter { it.event.id in idSet }
                                .map { it.song.toMediaMetadata() }
                        }
                        if (metadataList.isNotEmpty()) {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = metadataList,
                                    onDismiss = menuState::dismiss,
                                    clearAction = ::clearSelection,
                                    currentItems = emptyList()
                                )
                            }
                        }
                    },
                    onDeleteSelected = {
                        if (!isRemote) {
                            val idSet = selectedLocalEventIds.toSet()
                            val toRemove = allFilteredLocalEvents.filter { it.event.id in idSet }
                            clearSelection()
                            removeLocalEventsWithUndo(toRemove)
                        }
                    }
                )
            }
        }
    }

    if (showClearHistoryDialog) {
        ClearHistoryConfirmDialog(
            trackCount = allFilteredLocalEvents.size,
            onDismiss = { showClearHistoryDialog = false },
            onConfirm = {
                showClearHistoryDialog = false
                pendingUndo = null
                lastUndoState = null
                clearSelection()
                database.query { clearListenHistory() }
            }
        )
    }
}

@Composable
private fun HistoryUndoBar(
    message: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 520.dp)
            .pointerInput(Unit) {
                // Consume all pointer events so taps never pass through to list rows behind the bar.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.delete_history),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.inversePrimary
                )
            ) {
                Text(
                    text = stringResource(R.string.undo),
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    tint = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryHeroSummaryCard(
    isRemote: Boolean,
    trackCount: Int,
    totalPlayTimeMs: Long,
    latestTrackTitle: String?,
    latestArtworkUrl: String?,
    onShuffleClick: () -> Unit,
    hasTracks: Boolean,
    isCompactPane: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "history_hero_pulse")
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

    val sourceLabel = stringResource(if (isRemote) R.string.remote_history else R.string.local_history)
    val countLabel = pluralStringResource(R.plurals.n_song, trackCount, trackCount)
    val subLine = remember(isRemote, totalPlayTimeMs, latestTrackTitle, hasTracks) {
        when {
            !hasTracks -> null
            !isRemote && totalPlayTimeMs > 0L -> {
                listOfNotNull(makeTimeString(totalPlayTimeMs), latestTrackTitle).joinToString(" • ")
            }
            else -> latestTrackTitle
        }
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .then(if (hasTracks) Modifier.clickable(onClick = onShuffleClick) else Modifier)
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasTracks) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasTracks) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    HistoryHeroArtworkBadge(
                        artworkUrl = latestArtworkUrl,
                        hasTracks = hasTracks,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        outerSize = 58.dp,
                        innerSize = 48.dp
                    )
                }

                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subLine.isNullOrBlank()) {
                    Text(
                        text = subLine,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (hasTracks) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = sourceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (hasTracks) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Text(
                        text = countLabel,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!subLine.isNullOrBlank()) {
                        Text(
                            text = subLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                HistoryHeroArtworkBadge(
                    artworkUrl = latestArtworkUrl,
                    hasTracks = hasTracks,
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
private fun HistoryHeroArtworkBadge(
    artworkUrl: String?,
    hasTracks: Boolean,
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
        if (hasTracks) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }

        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl.resize(300, 300),
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
                        if (hasTracks) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.history),
                    contentDescription = stringResource(R.string.history),
                    tint = if (hasTracks) {
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

@Composable
private fun HistorySourceSwitcher(
    selectedSource: HistorySource,
    onSelectSource: (HistorySource) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        ToggleButton(
            checked = selectedSource == HistorySource.LOCAL,
            onCheckedChange = { if (it) onSelectSource(HistorySource.LOCAL) },
            modifier = Modifier
                .weight(1f)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
        ) {
            Icon(
                painter = painterResource(R.drawable.history),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.local_history),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ToggleButton(
            checked = selectedSource == HistorySource.REMOTE,
            onCheckedChange = { if (it) onSelectSource(HistorySource.REMOTE) },
            modifier = Modifier
                .weight(1f)
                .semantics { role = Role.RadioButton },
            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
        ) {
            Icon(
                painter = painterResource(R.drawable.language),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.remote_history),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PausedHistoryWarningCard(
    onResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f))
            ) {
                Icon(
                    painter = painterResource(R.drawable.pause),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = stringResource(R.string.pause_listen_history),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = onResume,
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(16.dp)
                )
            ) {
                Text(
                    text = stringResource(R.string.play),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(
    title: String,
    count: Int,
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = pluralStringResource(R.plurals.n_song, count, count),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

private fun segmentedCardShape(
    isFirst: Boolean,
    isLast: Boolean,
    isSelected: Boolean
): RoundedCornerShape {
    return if (isSelected) {
        RoundedCornerShape(20.dp)
    } else {
        RoundedCornerShape(
            topStart = if (isFirst) 22.dp else 6.dp,
            topEnd = if (isFirst) 22.dp else 6.dp,
            bottomStart = if (isLast) 22.dp else 6.dp,
            bottomEnd = if (isLast) 22.dp else 6.dp
        )
    }
}

@Composable
private fun HistorySegmentedSongCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    thumbnailCornerRadius: Float,
    trailingTimeLabel: String?,
    isFirst: Boolean,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    isExplicit: Boolean,
    isLiked: Boolean,
    isInLibrary: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = segmentedCardShape(
        isFirst = isFirst,
        isLast = isLast,
        isSelected = isSelected
    )

    val rowBackgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isActive -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        animationSpec = tween(220),
        label = "history_card_bg"
    )

    val titleColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        isActive -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val subtitleColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
        isActive -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

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
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp.coerceAtMost(14.dp)))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = thumbnailUrl?.resize(200, 200),
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

                if (isSelected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.done),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = titleColor
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isExplicit) {
                        Icon(
                            painter = painterResource(R.drawable.explicit),
                            contentDescription = null,
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (isLiked) {
                        Icon(
                            painter = painterResource(R.drawable.favorite),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (isInLibrary) {
                        Icon(
                            painter = painterResource(R.drawable.library_add_check),
                            contentDescription = null,
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (isDownloaded) {
                        Icon(
                            painter = painterResource(R.drawable.offline),
                            contentDescription = null,
                            tint = subtitleColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!trailingTimeLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = titleColor.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = trailingTimeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = subtitleColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

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

@Composable
private fun HistoryEmptyStateCard(
    isSearching: Boolean,
    isPaused: Boolean,
    onResumeHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_history_pulse")
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
                    painter = painterResource(if (isSearching) R.drawable.search else R.drawable.history),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(
                when {
                    isSearching -> R.string.no_results_found
                    isPaused -> R.string.pause_listen_history
                    else -> R.string.history
                }
            ),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(
                when {
                    isSearching -> R.string.try_another_term
                    else -> R.string.check_back_later
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (isPaused) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onResumeHistory,
                shapes = ButtonDefaults.shapes(
                    shape = CircleShape,
                    pressedShape = RoundedCornerShape(20.dp)
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.play),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HistorySelectionFloatingToolbar(
    allSelected: Boolean,
    canDelete: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onOpenMenu: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .widthIn(max = 420.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onClose,
                containerColor = cs.surfaceContainerHighest,
                contentColor = cs.onSurface
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = cs.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .height(52.dp)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSelectAll, modifier = Modifier.size(46.dp)) {
                Icon(
                    painter = painterResource(if (allSelected) R.drawable.deselect else R.drawable.select_all),
                    contentDescription = null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onOpenMenu, modifier = Modifier.size(46.dp)) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = stringResource(R.string.more_options),
                    tint = cs.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (canDelete) {
                IconButton(onClick = onDeleteSelected, modifier = Modifier.size(46.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = stringResource(R.string.delete),
                        tint = cs.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearHistoryConfirmDialog(
    trackCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val expressiveShapes = ButtonDefaults.shapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(20.dp)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete_history),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.clear_listen_history),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.clear_listen_history_confirm),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Text(
                        text = pluralStringResource(R.plurals.n_song, trackCount, trackCount),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shapes = expressiveShapes,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        shapes = expressiveShapes,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.clear),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun dateAgoKey(dateAgo: DateAgo): String = when (dateAgo) {
    DateAgo.Today -> "today"
    DateAgo.Yesterday -> "yesterday"
    DateAgo.ThisWeek -> "this_week"
    DateAgo.LastWeek -> "last_week"
    is DateAgo.Other -> "other_${dateAgo.date}"
}

private fun formatDateAgoTitle(
    context: android.content.Context,
    dateAgo: DateAgo
): String = when (dateAgo) {
    DateAgo.Today -> context.getString(R.string.today)
    DateAgo.Yesterday -> context.getString(R.string.yesterday)
    DateAgo.ThisWeek -> context.getString(R.string.this_week)
    DateAgo.LastWeek -> context.getString(R.string.last_week)
    is DateAgo.Other -> dateAgo.date.format(
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    ).replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
