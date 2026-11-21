package com.cgens67.avidtune.ui.player

import android.app.Activity
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.SponsorBlockOverlay
import com.cgens67.avidtune.utils.makeTimeString
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val VIDEO_CONTROLS_IDLE_MS = 3_500L

@Composable
internal fun HideSystemBars(hidden: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, hidden) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevBehavior = controller?.systemBarsBehavior
        if (controller != null && hidden) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (controller != null && hidden) {
                controller.show(WindowInsetsCompat.Type.systemBars())
                if (prevBehavior != null) {
                    controller.systemBarsBehavior = prevBehavior
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FullscreenVideoPlayer(
    onExitFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val currentSkipSegments by playerConnection.currentSkipSegments.collectAsState()
    val sponsorBlockEnabled by playerConnection.sponsorBlockEnabled.collectAsState()
    val error by playerConnection.error.collectAsState()

    val currentMetadata = mediaMetadata
    val showsVideo = rememberShowsMusicVideo(currentMetadata)

    LaunchedEffect(currentMetadata?.id, showsVideo) {
        if (currentMetadata == null || !showsVideo) {
            onExitFullscreen()
        }
    }

    if (currentMetadata == null) return

    BackHandler(enabled = active, onBack = onExitFullscreen)

    var rendered by remember(currentMetadata.id) { mutableStateOf(false) }
    val failedVideoIds by MusicVideoEngine.failedVideoIds.collectAsState()
    val isVideoFailed = currentMetadata.id in failedVideoIds && !rendered
    var controlsShown by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    var showQualitySheet by rememberSaveable { mutableStateOf(false) }

    var position by remember(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition.coerceAtLeast(0L)) }
    var duration by remember(playbackState) {
        val raw = playerConnection.player.duration
        mutableLongStateOf(if (raw == C.TIME_UNSET || raw < 0L) 0L else raw)
    }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(playbackState, isPlaying) {
        while (isActive) {
            if (sliderPosition == null) {
                val rawDuration = playerConnection.player.duration
                position = playerConnection.player.currentPosition.coerceAtLeast(0L)
                duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0L) 0L else rawDuration
            }
            delay(100)
        }
    }

    LaunchedEffect(controlsShown, isPlaying, touches, showQualitySheet, sliderPosition) {
        if (controlsShown && isPlaying && !showQualitySheet && sliderPosition == null) {
            delay(VIDEO_CONTROLS_IDLE_MS)
            controlsShown = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { controlsShown = !controlsShown },
    ) {
        if (!rendered) {
            VideoLoadingBackdrop(
                modifier = Modifier.fillMaxSize(),
                videoId = currentMetadata.id,
            )
        }

        AndroidMusicVideo(
            videoId = currentMetadata.id,
            fit = true,
            onRenderedChanged = { rendered = it },
            modifier = Modifier.fillMaxSize(),
            priority = 2,
            showErrorOverlay = !controlsShown,
            showLoadingOverlay = false,
        )

        AnimatedVisibility(
            visible = controlsShown,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.62f),
                            0.30f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.78f),
                        ),
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                touches++
                            }
                        }
                    }
                    .windowInsetsPadding(WindowInsets.displayCutout)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                // Top Bar: Title, Artists & Corner Controls
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = currentMetadata.title,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = currentMetadata.artists.joinToString { it.name },
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    VideoCornerControls(
                        fullscreen = true,
                        onToggleFullscreen = onExitFullscreen,
                        onOpenQuality = { showQualitySheet = true },
                    )
                }

                // Center Transport Controls or Error Banner
                if (error != null) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 460.dp)
                    ) {
                        PlaybackError(
                            error = error!!,
                            mediaId = currentMetadata.id,
                            retry = {
                                playerConnection.player.prepare()
                                playerConnection.player.play()
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .widthIn(max = 420.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { playerConnection.seekToPrevious() },
                            enabled = canSkipPrevious && !playerConnection.isGuestPlaybackRestricted,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_previous),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = if (canSkipPrevious) 1f else 0.35f),
                                modifier = Modifier.size(34.dp),
                            )
                        }

                        Surface(
                            onClick = {
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.togglePlayPause()
                                }
                            },
                            enabled = !playerConnection.isGuestPlaybackRestricted,
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(72.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (playbackState == Player.STATE_BUFFERING) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(34.dp),
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(
                                            when {
                                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                                isPlaying -> R.drawable.pause
                                                else -> R.drawable.play
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(38.dp),
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { playerConnection.seekToNext() },
                            enabled = canSkipNext && !playerConnection.isGuestPlaybackRestricted,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.skip_next),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = if (canSkipNext) 1f else 0.35f),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                // Bottom Scrubber & Timestamps
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    if (isVideoFailed) {
                        Surface(
                            onClick = { MusicVideoEngine.retry(currentMetadata.id) },
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.replay),
                                    contentDescription = stringResource(R.string.action_retry),
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(R.string.action_retry),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    val maxRange = if (duration <= 0L) 0f else duration.toFloat()
                    val displayPos = (sliderPosition ?: position).toFloat().coerceIn(0f, if (maxRange > 0f) maxRange else 1f)
                    val sponsorFractions = remember(currentSkipSegments, duration, sponsorBlockEnabled) {
                        if (!sponsorBlockEnabled || duration <= 0L) emptyList()
                        else currentSkipSegments.map { (startMs, endMs) ->
                            (startMs.toFloat() / duration).coerceIn(0f, 1f) to (endMs.toFloat() / duration).coerceIn(0f, 1f)
                        }
                    }
                    val progressFrac = if (maxRange > 0f) (displayPos / maxRange).coerceIn(0f, 1f) else 0f

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = displayPos,
                            valueRange = 0f..maxRange,
                            enabled = !playerConnection.isGuestPlaybackRestricted && maxRange > 0f,
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerConnection.seekTo(pos)
                                    position = pos
                                }
                                sliderPosition = null
                            },
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                                thumbColor = Color.White,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SponsorBlockOverlay(
                            sponsorSegments = sponsorFractions,
                            activeProgress = progressFrac,
                            trackHeight = 4.dp,
                            thumbOffset = 10.dp,
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = makeTimeString(sliderPosition ?: position),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                        Text(
                            text = if (duration > 0L) makeTimeString(duration) else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }

    if (showQualitySheet) {
        VideoQualitySheet(onDismiss = { showQualitySheet = false })
    }
}

@Composable
internal fun VideoLoadingBackdrop(
    modifier: Modifier = Modifier,
    videoId: String? = null,
) {
    val failedIds by MusicVideoEngine.failedVideoIds.collectAsState()
    val isFailed = if (videoId != null) videoId in failedIds else failedIds.isNotEmpty()
    Box(modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (!isFailed) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
internal fun VideoCornerControls(
    fullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onOpenQuality: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoQualityBox(onClick = onOpenQuality)
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onToggleFullscreen),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun MusicVideoQualityUi.selected(): Int? =
    if (held == null || held !in heights) activeHeight else held

@Composable
private fun VideoQualityBox(onClick: () -> Unit) {
    val quality by MusicVideoEngine.quality.collectAsState()
    if (quality.heights.size < 2) return
    val label = quality.selected()?.let(::videoQualityLabel)
        ?: stringResource(R.string.video_quality_auto)
    Box(
        modifier = Modifier
            .height(30.dp)
            .widthIn(min = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
            label = "videoQualityLabel",
        ) { text ->
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoQualitySheet(onDismiss: () -> Unit) {
    val quality by MusicVideoEngine.quality.collectAsState()
    val selected = quality.selected()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxSheetHeight = (configuration.screenHeightDp * if (isLandscape) 0.78f else 0.75f).dp

    val dismissWithAnimation: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = if (isLandscape) 16.dp else 24.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isLandscape) 10.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.video_quality),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                selected?.let { activeHeight ->
                    val badgeLabel = if (activeHeight >= 720) {
                        "${videoQualityLabel(activeHeight)} • ${activeHeight}p"
                    } else {
                        "${activeHeight}p"
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QualityRow(
                    name = stringResource(R.string.video_quality_auto),
                    detail = if (quality.held == null) {
                        selected?.let { if (it >= 720) "${videoQualityLabel(it)} • ${it}p" else "${it}p" }
                    } else {
                        null
                    },
                    active = quality.held == null,
                    compact = isLandscape,
                ) {
                    MusicVideoEngine.setQuality(null)
                    dismissWithAnimation()
                }

                if (isLandscape) {
                    quality.heights.chunked(2).forEach { rowHeights ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowHeights.forEach { height ->
                                QualityRow(
                                    name = videoQualityLabel(height),
                                    detail = if (height >= 720) "${height}p" else null,
                                    active = quality.held != null && selected == height,
                                    compact = true,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    MusicVideoEngine.setQuality(height)
                                    dismissWithAnimation()
                                }
                            }
                            if (rowHeights.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    quality.heights.forEach { height ->
                        QualityRow(
                            name = videoQualityLabel(height),
                            detail = if (height >= 720) "${height}p" else null,
                            active = quality.held != null && selected == height,
                            compact = false,
                        ) {
                            MusicVideoEngine.setQuality(height)
                            dismissWithAnimation()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityRow(
    name: String,
    detail: String?,
    active: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        enabled = !active,
        shape = RoundedCornerShape(16.dp),
        color = if (active) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compact) 11.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (active) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (active) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}