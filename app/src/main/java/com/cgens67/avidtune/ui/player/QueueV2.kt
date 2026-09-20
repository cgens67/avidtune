@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.QueueEditLockKey
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.extensions.move
import com.cgens67.avidtune.extensions.toggleRepeatMode
import com.cgens67.avidtune.together.TogetherRole
import com.cgens67.avidtune.together.TogetherSessionState
import com.cgens67.avidtune.ui.component.ActionPromptDialog
import com.cgens67.avidtune.ui.component.BottomSheetState
import com.cgens67.avidtune.ui.component.LocalBottomSheetPageState
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.MediaMetadataListItem
import com.cgens67.avidtune.ui.menu.PlayerMenu
import com.cgens67.avidtune.ui.menu.ShowMediaInfo
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt

@Composable
fun QueueV2(
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    onControlsVisibilityChange: (Boolean) -> Unit = {}
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current

    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsState()

    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val isGuest = (togetherSessionState as? TogetherSessionState.Joined)?.role is TogetherRole.Guest

    var locked by rememberPreference(QueueEditLockKey, true)
    val isQueueEffectivelyLocked = locked || isGuest

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var sleepTimerValue by remember { mutableFloatStateOf(30f) }
    val sleepTimerEnabled = remember(
        playerConnection.service.sleepTimer.triggerTime,
        playerConnection.service.sleepTimer.pauseWhenSongEnd
    ) {
        playerConnection.service.sleepTimer.isActive
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val adaptivePrimary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurface else Color.White
    val adaptiveSecondary = if (playerBackground == PlayerBackgroundStyle.DEFAULT) MaterialTheme.colorScheme.onSurfaceVariant else Color.White.copy(alpha = 0.7f)

    val lazyListState = rememberLazyListState()

    var previousIndex by remember { mutableIntStateOf(0) }
    var previousScrollOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            Triple(lazyListState.firstVisibleItemIndex, lazyListState.firstVisibleItemScrollOffset, lazyListState.isScrollInProgress)
        }.collect { (index, offset, isScrollInProgress) ->
            if (index == 0 && offset < 50) {
                onControlsVisibilityChange(true)
            } else if (isScrollInProgress) {
                if (index != previousIndex) {
                    onControlsVisibilityChange(index <= previousIndex)
                } else {
                    val delta = offset - previousScrollOffset
                    if (delta > 15) {
                        onControlsVisibilityChange(false)
                    } else if (delta < -15) {
                        onControlsVisibilityChange(true)
                    }
                }
            }
            previousIndex = index
            previousScrollOffset = offset
        }
    }

    val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val currentPlayingUid = remember(currentWindowIndex, queueWindows) {
        if (currentWindowIndex in queueWindows.indices) {
            queueWindows[currentWindowIndex].uid
        } else null
    }

    LaunchedEffect(queueWindows) {
        mutableQueueWindows.apply {
            clear()
            addAll(queueWindows)
        }
    }

    val headerItems = 0

    LaunchedEffect(mutableQueueWindows.size, currentWindowIndex) {
        if (currentWindowIndex in mutableQueueWindows.indices) {
            lazyListState.scrollToItem(currentWindowIndex)
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState
    ) { from, to ->
        val currentDragInfo = dragInfo
        dragInfo = if (currentDragInfo == null) {
            from.index to to.index
        } else {
            currentDragInfo.first to to.index
        }

        val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
        mutableQueueWindows.move(safeFrom, safeTo)
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                if (!shuffleModeEnabled) {
                    playerConnection.player.moveMediaItem(safeFrom, safeTo)
                } else {
                    playerConnection.player.setShuffleOrder(
                        DefaultShuffleOrder(
                            queueWindows.map { it.firstPeriodIndex }
                                .toMutableList()
                                .move(safeFrom, safeTo)
                                .toIntArray(),
                            System.currentTimeMillis()
                        )
                    )
                }
                dragInfo = null
            }
        }
    }

    CompositionLocalProvider(LocalContentColor provides adaptivePrimary) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Control Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pillShape = RoundedCornerShape(16.dp)
                val activeColor = adaptivePrimary.copy(alpha = 0.25f)
                val inactiveColor = adaptivePrimary.copy(alpha = 0.1f)

                // Shuffle
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(if (shuffleModeEnabled) activeColor else inactiveColor, pillShape)
                        .clip(pillShape)
                        .clickable(enabled = !isGuest) {
                            playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = adaptivePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Repeat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(if (repeatMode != Player.REPEAT_MODE_OFF) activeColor else inactiveColor, pillShape)
                        .clip(pillShape)
                        .clickable(enabled = !isGuest) { playerConnection.player.toggleRepeatMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = null,
                        tint = adaptivePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Timer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(if (sleepTimerEnabled) activeColor else inactiveColor, pillShape)
                        .clip(pillShape)
                        .clickable(enabled = !isGuest) {
                            if (sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.bedtime),
                            contentDescription = stringResource(R.string.sleep_timer),
                            tint = adaptivePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        if (sleepTimerEnabled) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = makeTimeString(sleepTimerTimeLeft.coerceAtLeast(0L)),
                                style = MaterialTheme.typography.labelSmall,
                                color = adaptivePrimary
                            )
                        }
                    }
                }
            }

            // Queue Header Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (queueTitle != null) {
                            Text(
                                text = stringResource(R.string.playing_from),
                                style = MaterialTheme.typography.labelMedium,
                                color = adaptiveSecondary
                            )
                            Text(
                                text = queueTitle!!,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = adaptivePrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.queue),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = adaptivePrimary
                            )
                        }
                    }
                    if (!isGuest) {
                        IconButton(onClick = { locked = !locked }) {
                            Icon(
                                painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                contentDescription = if (locked) stringResource(R.string.unlock_queue) else stringResource(R.string.lock_queue),
                                tint = adaptiveSecondary
                            )
                        }
                    }
                }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 120.dp, top = 4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() }
                ) { _, window ->
                    ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode()
                    ) {
                        val isActive = window.uid == currentPlayingUid

                        val dismissBoxState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { totalDistance -> totalDistance }
                        )
                        var processedDismiss by remember { mutableStateOf(false) }

                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                processedDismiss = true
                                playerConnection.player.removeMediaItem(window.firstPeriodIndex)
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val content: @Composable () -> Unit = {
                            MediaMetadataListItem(
                                mediaMetadata = window.mediaItem.metadata!!,
                                isSelected = false,
                                isActive = isActive,
                                isPlaying = isPlaying && isActive,
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlayerMenu(
                                                        mediaMetadata = window.mediaItem.metadata!!,
                                                        navController = navController,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        isQueueTrigger = true,
                                                        onShowDetailsDialog = {
                                                            window.mediaItem.mediaId.let { mediaId ->
                                                                bottomSheetPageState.show {
                                                                    ShowMediaInfo(mediaId) {
                                                                        bottomSheetPageState.dismiss()
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onDismiss = menuState::dismiss
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = stringResource(R.string.more_options)
                                            )
                                        }

                                        if (!isQueueEffectivelyLocked) {
                                            IconButton(
                                                onClick = { },
                                                modifier = Modifier.draggableHandle()
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.drag_handle),
                                                    contentDescription = stringResource(R.string.drag_to_reorder)
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isGuest) {
                                        playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                        playerConnection.player.playWhenReady = true
                                    }
                            )
                        }

                        if (isQueueEffectivelyLocked) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {
                                    val color by animateColorAsState(
                                        targetValue = when (dismissBoxState.targetValue) {
                                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                                            else -> MaterialTheme.colorScheme.error
                                        }, label = "dismissColor"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp, horizontal = 16.dp)
                                            .background(color, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        val iconAlpha by animateFloatAsState(
                                            targetValue = if (dismissBoxState.targetValue != SwipeToDismissBoxValue.Settled) 1f else 0f,
                                            label = "iconAlpha"
                                        )
                                        Icon(
                                            painter = painterResource(R.drawable.delete),
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier
                                                .padding(end = 16.dp)
                                                .alpha(iconAlpha),
                                            tint = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                },
                                content = { content() },
                                enableDismissFromStartToEnd = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSleepTimerDialog) {
        ActionPromptDialog(
            titleBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.sleep_timer),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            onDismiss = { showSleepTimerDialog = false },
            onConfirm = {
                showSleepTimerDialog = false
                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
            },
            onCancel = { showSleepTimerDialog = false },
            onReset = { sleepTimerValue = 30f },
            content = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }
}
