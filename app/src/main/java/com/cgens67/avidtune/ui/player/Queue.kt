@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.cgens67.avidtune.ui.player

import android.annotation.SuppressLint
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.extensions.*
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.ui.component.*
import com.cgens67.avidtune.ui.menu.*
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.utils.*
import kotlinx.coroutines.*
import sh.calvin.reorderable.*

@SuppressLint("UnrememberedMutableState", "StringFormatInvalid")
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onBackgroundColor: Color,
    textBackgroundColor: Color
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val automix by playerConnection.service.automixItems.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<Timeline.Window>() }
    var selection by remember { mutableStateOf(false) }
    var showDetailsDialog by rememberSaveable { mutableStateOf(false) }
    var locked by rememberPreference(QueueEditLockKey, true)
    val (enableHapticFeedback) = rememberPreference(booleanPreferencesKey("enable_haptic_feedback"), true)
    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    // Player Background preferences
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )
    val disableBlur by rememberPreference(DisableBlurKey, false)
    val pureBlack by rememberPreference(PureBlackKey, false)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val isCustomBackground = playerBackground != PlayerBackgroundStyle.DEFAULT

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColorArgb = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT ||
            playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
            playerBackground == PlayerBackgroundStyle.LIVE_MESH
        ) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    ImageLoader(context).execute(
                        ImageRequest.Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    ).drawable as? BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { bitmap ->
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()

                    val extracted = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColorArgb
                    )
                    withContext(Dispatchers.Main) {
                        gradientColors = extracted
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val sheetBgColor = when {
        isCustomBackground -> Color.Transparent
        useDarkTheme && pureBlack -> Color.Black
        else -> backgroundColor
    }

    val effectiveOnBgColor = when {
        isCustomBackground -> Color.White
        sheetBgColor.luminance() < 0.5f -> Color.White
        else -> MaterialTheme.colorScheme.onSurface
    }

    fun clearSelection() {
        selection = false
        selectedSongs.clear()
        selectedItems.clear()
    }

    if (selection) BackHandler { clearSelection() }

    val onRemoveMultipleWithUndo: (List<Timeline.Window>) -> Unit = { windows ->
        if (windows.isNotEmpty()) {
            val sorted = windows.sortedBy { it.firstPeriodIndex }
            var i = 0
            sorted.forEach { playerConnection.player.removeMediaItem(it.firstPeriodIndex - i++) }
            dismissJob?.cancel()
            dismissJob = coroutineScope.launch {
                val msg = if (windows.size == 1) {
                    context.getString(R.string.removed_song_from_playlist, sorted.first().mediaItem.metadata?.title)
                } else {
                    context.resources.getQuantityString(R.plurals.removed_songs_from_queue, windows.size, windows.size)
                }
                val res = snackbarHostState.showSnackbar(
                    msg,
                    context.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                if (res == SnackbarResult.ActionPerformed) {
                    sorted.forEach { w ->
                        playerConnection.player.addMediaItem(w.mediaItem)
                        playerConnection.player.moveMediaItem(playerConnection.player.mediaItemCount - 1, w.firstPeriodIndex)
                    }
                }
            }
        }
    }

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            icon = { Icon(painterResource(R.drawable.info), null) },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp).verticalScroll(rememberScrollState())) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.volume) to "${(playerConnection.player.volume * 100).toInt()}%"
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                clipboardManager.setText(AnnotatedString(displayText))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        )
    }

    BottomSheet(
        state = state,
        background = {
            Box(modifier = Modifier.fillMaxSize().background(sheetBgColor)) {
                if (isCustomBackground) {
                    PlayerBackground(
                        playerBackground = playerBackground,
                        mediaMetadata = mediaMetadata,
                        gradientColors = gradientColors,
                        backgroundAlpha = 1f,
                        disableBlur = disableBlur
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                }
            }
        },
        modifier = modifier,
        collapsedContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { state.expandSoft() }) {
                    Icon(
                        painter = painterResource(R.drawable.expand_less),
                        tint = effectiveOnBgColor,
                        contentDescription = null
                    )
                }
            }
        }
    ) {
        val mutableQueueWindows = remember { mutableStateListOf<Timeline.Window>() }
        val queueLength = remember(queueWindows) { queueWindows.sumOf { it.mediaItem.metadata?.duration ?: 0 } }
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
        val headerItems = 1

        val reorderableState = rememberReorderableLazyListState(
            lazyListState,
            scrollThresholdPadding = WindowInsets.systemBars.add(WindowInsets(top = ListItemHeight, bottom = ListItemHeight)).asPaddingValues()
        ) { from, to ->
            if (from.index >= headerItems && to.index >= headerItems) {
                dragInfo = (dragInfo?.first ?: from.index) to to.index
                val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
                val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
                mutableQueueWindows.move(safeFrom, safeTo)
            }
        }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) dragInfo?.let { (from, to) ->
                val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)
                if (safeFrom != safeTo) {
                    if (!playerConnection.player.shuffleModeEnabled) {
                        playerConnection.player.moveMediaItem(safeFrom, safeTo)
                    } else {
                        playerConnection.player.setShuffleOrder(
                            DefaultShuffleOrder(
                                queueWindows.map { it.firstPeriodIndex }.toMutableList().move(safeFrom, safeTo).toIntArray(),
                                System.currentTimeMillis()
                            )
                        )
                    }
                }
                dragInfo = null
            }
        }

        LaunchedEffect(queueWindows) {
            if (!reorderableState.isAnyItemDragging) {
                mutableQueueWindows.apply { clear(); addAll(queueWindows) }
            }
        }

        LaunchedEffect(state.isCollapsed, currentWindowIndex) {
            if (!state.isCollapsed && currentWindowIndex != -1) {
                lazyListState.scrollToItem(currentWindowIndex + headerItems)
            }
        }

        val itemKey = { item: Timeline.Window -> item.uid.hashCode().toLong() xor item.mediaItem.mediaId.hashCode().toLong() }

        Box(modifier = Modifier.fillMaxSize().background(sheetBgColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                CurrentSongHeader(
                    mediaMetadata = mediaMetadata,
                    liked = currentSong?.song?.liked == true,
                    repeatMode = repeatMode,
                    shuffleModeEnabled = playerConnection.player.shuffleModeEnabled,
                    locked = locked,
                    songCount = queueWindows.size,
                    queueDuration = queueLength,
                    backgroundColor = sheetBgColor,
                    onBackgroundColor = effectiveOnBgColor,
                    onToggleLike = { playerConnection.service.toggleLike() },
                    onMenuClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata!!,
                                navController = navController,
                                playerBottomSheetState = playerBottomSheetState,
                                isQueueTrigger = true,
                                onShowDetailsDialog = { showDetailsDialog = true },
                                onDismiss = { menuState.dismiss() }
                            )
                        }
                    },
                    onClearQueueClick = {
                        val w = if (currentWindowIndex in queueWindows.indices) {
                            queueWindows.filterIndexed { i, _ -> i != currentWindowIndex }
                        } else emptyList()
                        onRemoveMultipleWithUndo(w)
                        clearSelection()
                    },
                    onRepeatClick = { playerConnection.player.toggleRepeatMode() },
                    onShuffleClick = { playerConnection.player.shuffleModeEnabled = !playerConnection.player.shuffleModeEnabled },
                    onLockClick = { locked = !locked }
                )

                CompositionLocalProvider(LocalContentColor provides effectiveOnBgColor) {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                            .add(WindowInsets(bottom = ListItemHeight + if (selection) 88.dp else 8.dp))
                            .asPaddingValues(),
                        modifier = Modifier.weight(1f).nestedScroll(state.preUpPostDownNestedScrollConnection)
                    ) {
                        item { Spacer(modifier = Modifier.animateContentSize().height(if (selection) 48.dp else 0.dp)) }

                        itemsIndexed(items = mutableQueueWindows, key = { _, item -> itemKey(item) }) { index, window ->
                            ReorderableItem(state = reorderableState, key = itemKey(window)) {
                                val currentItem by rememberUpdatedState(window)
                                val isActive = index == currentWindowIndex
                                val dismissBoxState = rememberSwipeToDismissBoxState(positionalThreshold = { it })
                                var processedDismiss by remember { mutableStateOf(false) }

                                LaunchedEffect(dismissBoxState.currentValue) {
                                    val dv = dismissBoxState.currentValue
                                    if (!processedDismiss && (dv == SwipeToDismissBoxValue.StartToEnd || dv == SwipeToDismissBoxValue.EndToStart)) {
                                        processedDismiss = true
                                        onRemoveMultipleWithUndo(listOf(currentItem))
                                    }
                                    if (dv == SwipeToDismissBoxValue.Settled) processedDismiss = false
                                }

                                val content: @Composable () -> Unit = {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                                    ) {
                                        MediaMetadataListItem(
                                            mediaMetadata = window.mediaItem.metadata!!,
                                            isSelected = selection && window.mediaItem.metadata!! in selectedSongs,
                                            isActive = isActive,
                                            isPlaying = isPlaying && isActive,
                                            trailingContent = {
                                                IconButton(onClick = {
                                                    menuState.show {
                                                        SelectionMediaMetadataMenu(
                                                            songSelection = selectedSongs,
                                                            onDismiss = { menuState.dismiss() },
                                                            clearAction = { clearSelection() },
                                                            currentItems = selectedItems
                                                        )
                                                    }
                                                }) {
                                                    Icon(painterResource(R.drawable.more_vert), null, tint = effectiveOnBgColor)
                                                }
                                                if (!locked) {
                                                    IconButton(
                                                        onClick = {},
                                                        modifier = Modifier.draggableHandle().graphicsLayer { alpha = 0.99f }
                                                    ) {
                                                        Icon(painterResource(R.drawable.drag_handle), null, tint = effectiveOnBgColor)
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(sheetBgColor)
                                                .combinedClickable(
                                                    onClick = {
                                                        if (selection) {
                                                            if (window.mediaItem.metadata!! in selectedSongs) {
                                                                selectedSongs.remove(window.mediaItem.metadata!!)
                                                                selectedItems.remove(currentItem)
                                                                if (selectedSongs.isEmpty()) selection = false
                                                            } else {
                                                                selectedSongs.add(window.mediaItem.metadata!!)
                                                                selectedItems.add(currentItem)
                                                            }
                                                        } else {
                                                            if (index == currentWindowIndex) {
                                                                playerConnection.player.togglePlayPause()
                                                            } else {
                                                                playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                                                                playerConnection.player.playWhenReady = true
                                                            }
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (enableHapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selection = true
                                                        selectedSongs.clear()
                                                        selectedItems.clear()
                                                        selectedSongs.add(window.mediaItem.metadata!!)
                                                        selectedItems.add(currentItem)
                                                    }
                                                )
                                        )
                                    }
                                }

                                if (locked) content() else SwipeToDismissBox(state = dismissBoxState, backgroundContent = {}) { content() }
                            }
                        }

                        if (automix.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    color = effectiveOnBgColor.copy(alpha = 0.12f)
                                )
                                ItemWithGlowingIcon()
                            }

                            itemsIndexed(items = automix, key = { _, it -> it.mediaId }) { index, item ->
                                Row(horizontalArrangement = Arrangement.Center) {
                                    MediaMetadataListItem(
                                        mediaMetadata = item.metadata!!,
                                        trailingContent = {
                                            IconButton(onClick = { playerConnection.service.playNextAutomix(item, index) }) {
                                                Icon(painterResource(R.drawable.playlist_play), null, tint = effectiveOnBgColor)
                                            }
                                            IconButton(onClick = { playerConnection.service.addToQueueAutomix(item, index) }) {
                                                Icon(painterResource(R.drawable.queue_music), null, tint = effectiveOnBgColor)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    menuState.show {
                                                        PlayerMenu(
                                                            mediaMetadata = item.metadata!!,
                                                            navController = navController,
                                                            playerBottomSheetState = playerBottomSheetState,
                                                            isQueueTrigger = true,
                                                            onShowDetailsDialog = { showDetailsDialog = true },
                                                            onDismiss = { menuState.dismiss() }
                                                        )
                                                    }
                                                }
                                            )
                                            .animateItem()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(bottom = (if (selection) ListItemHeight * 2 + 16.dp else ListItemHeight) + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                        .align(Alignment.BottomCenter)
                )

                AnimatedVisibility(
                    visible = selection,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = ListItemHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
                ) {
                    QueueSelectionFloatingToolbar(
                        allSelected = selectedSongs.size == mutableQueueWindows.size,
                        pureBlack = isCustomBackground || sheetBgColor == Color.Black,
                        onClose = ::clearSelection,
                        onToggleSelectAll = {
                            if (selectedSongs.size == mutableQueueWindows.size) {
                                clearSelection()
                            } else {
                                selectedSongs.clear()
                                selectedItems.clear()
                                mutableQueueWindows.forEach { w ->
                                    selectedSongs.add(w.mediaItem.metadata!!)
                                    selectedItems.add(w)
                                }
                            }
                        },
                        onMenuAction = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = { menuState.dismiss() },
                                    clearAction = { clearSelection() },
                                    currentItems = selectedItems
                                )
                            }
                        },
                        onDelete = {
                            onRemoveMultipleWithUndo(selectedItems.toList())
                            clearSelection()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueSelectionFloatingToolbar(
    allSelected: Boolean,
    pureBlack: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onMenuAction: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier.widthIn(max = 420.dp),
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(
                onClick = onClose,
                containerColor = if (pureBlack) Color.White.copy(0.12f) else cs.surfaceContainerHighest,
                contentColor = if (pureBlack) Color.White else cs.onSurface
            ) {
                Icon(painterResource(R.drawable.close), null, modifier = Modifier.size(22.dp))
            }
        },
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = if (pureBlack) Color.Black else cs.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QueueSelectionToolbarAction(
                icon = if (allSelected) R.drawable.deselect else R.drawable.select_all,
                desc = null,
                onClick = onToggleSelectAll,
                tint = if (pureBlack) Color.White else cs.onSurface
            )
            QueueSelectionToolbarAction(
                icon = R.drawable.more_vert,
                desc = null,
                onClick = onMenuAction,
                tint = cs.primary
            )
            QueueSelectionToolbarAction(
                icon = R.drawable.delete,
                desc = stringResource(R.string.delete),
                onClick = onDelete,
                tint = cs.error
            )
        }
    }
}

@Composable
private fun QueueSelectionToolbarAction(
    icon: Int,
    desc: String?,
    onClick: () -> Unit,
    tint: Color
) = IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
    Icon(painterResource(icon), desc, modifier = Modifier.size(22.dp), tint = tint)
}

@Composable
fun CurrentSongHeader(
    mediaMetadata: MediaMetadata?,
    liked: Boolean,
    repeatMode: Int,
    shuffleModeEnabled: Boolean,
    locked: Boolean,
    songCount: Int,
    queueDuration: Int,
    backgroundColor: Color,
    onBackgroundColor: Color,
    onToggleLike: () -> Unit,
    onMenuClick: () -> Unit,
    onClearQueueClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLockClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp, bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(onBackgroundColor.copy(0.4f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AsyncImage(
                model = mediaMetadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(onBackgroundColor.copy(0.06f))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    mediaMetadata?.title ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor
                )
                Text(
                    mediaMetadata?.artists?.joinToString(", ") { it.name } ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = onBackgroundColor.copy(0.6f)
                )
            }
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(contentColor = if (liked) MaterialTheme.colorScheme.primary else onBackgroundColor)
            ) {
                Icon(
                    painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                    null,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(onBackgroundColor.copy(0.06f))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onLockClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = onBackgroundColor.copy(0.7f))
                ) {
                    Icon(painterResource(if (locked) R.drawable.lock else R.drawable.lock_open), null, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = onBackgroundColor.copy(0.7f))
                ) {
                    Icon(painterResource(R.drawable.more_vert), null, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = onClearQueueClick,
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(painterResource(R.drawable.delete), null, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                pluralStringResource(R.plurals.n_song, songCount, songCount) + "  •  " + makeTimeString(queueDuration * 1000L),
                style = MaterialTheme.typography.labelMedium,
                color = onBackgroundColor.copy(0.55f),
                modifier = Modifier.padding(end = 14.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val uc = ToggleButtonDefaults.toggleButtonColors(containerColor = onBackgroundColor.copy(0.12f), contentColor = onBackgroundColor)
            val cc = ToggleButtonDefaults.toggleButtonColors(checkedContainerColor = onBackgroundColor.copy(0.22f), checkedContentColor = onBackgroundColor)

            ToggleButton(
                checked = shuffleModeEnabled,
                onCheckedChange = { onShuffleClick() },
                modifier = Modifier.weight(1f).size(48.dp),
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                colors = if (shuffleModeEnabled) cc else uc
            ) {
                Icon(painterResource(R.drawable.shuffle), null, modifier = Modifier.size(22.dp))
            }
            ToggleButton(
                checked = repeatMode != Player.REPEAT_MODE_OFF,
                onCheckedChange = { onRepeatClick() },
                modifier = Modifier.weight(1f).size(48.dp),
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                colors = if (repeatMode != Player.REPEAT_MODE_OFF) cc else uc
            ) {
                Icon(
                    painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(stringResource(R.string.continue_playing), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = onBackgroundColor)
        Spacer(modifier = Modifier.height(2.dp))
        Text(stringResource(R.string.autoplaying_similar_content), style = MaterialTheme.typography.bodySmall, color = onBackgroundColor.copy(0.5f))
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = onBackgroundColor.copy(0.08f), thickness = 1.dp)
    }
}

@Composable
fun ItemWithGlowingIcon() {
    val t = rememberInfiniteTransition("glowingIcon")
    val s by t.animateFloat(1f, 1.15f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "scale")
    val a by t.animateFloat(0.3f, 0.7f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "alpha")

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Icon(
                    painterResource(R.drawable.ia_icon),
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp).scale(s).alpha(a)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                stringResource(R.string.similar_content),
                fontSize = 16.sp,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
