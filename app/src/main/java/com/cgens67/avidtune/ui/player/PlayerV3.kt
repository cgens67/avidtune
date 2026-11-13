@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.BottomSheetState
import com.cgens67.avidtune.ui.component.LocalBottomSheetPageState
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.menu.PlayerMenu
import com.cgens67.avidtune.ui.menu.ShowMediaInfo
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

fun Morph.toComposePath(progress: Float, path: Path = Path()): Path {
    var first = true
    path.rewind()
    forEachCubic(progress) { bezier ->
        if (first) {
            path.moveTo(bezier.anchor0X, bezier.anchor0Y)
            first = false
        }
        path.cubicTo(
            bezier.control0X,
            bezier.control0Y,
            bezier.control1X,
            bezier.control1Y,
            bezier.anchor1X,
            bezier.anchor1Y
        )
    }
    path.close()
    return path
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphPolygonShape(
    start: RoundedPolygon,
    end: RoundedPolygon,
    private val progress: Float
) : Shape {
    private val morph = Morph(start, end)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toComposePath(progress = progress)
        path.transform(Matrix().apply { scale(x = size.width, y = size.height) })
        val bounds = path.getBounds()
        val boundsCenter = Offset(bounds.left + bounds.width / 2f, bounds.top + bounds.height / 2f)
        val sizeCenter = Offset(size.width / 2f, size.height / 2f)
        path.translate(sizeCenter - boundsCenter)
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MorphingSurface(
    onClick: () -> Unit,
    startShape: RoundedPolygon,
    endShape: RoundedPolygon,
    containerColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val progress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "morphProgress"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "buttonScale"
    )

    val shape = remember(startShape, endShape, progress) {
        MorphPolygonShape(startShape, endShape, progress)
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(containerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun PlayerV3(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked = currentSong?.song?.liked == true

    val repeatMode by playerConnection.repeatMode.collectAsState()
    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerVolume by playerConnection.service.playerVolume.collectAsState()
    val isMuted by playerConnection.isMuted.collectAsState()

    val isSystemDark = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemDark) {
        if (darkTheme == DarkMode.AUTO) isSystemDark else darkTheme == DarkMode.ON
    }
    val surfaceLuminance = MaterialTheme.colorScheme.surface.luminance()
    val isDarkTheme = useDarkTheme && surfaceLuminance < 0.5f

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val isAppleMusicStyle = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC || playerBackground == PlayerBackgroundStyle.IMMERSIVE_ARTWORK

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    var thumbnailCornerRadius by remember { mutableFloatStateOf(24f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var scrubbedPosition by remember { mutableStateOf<Long?>(null) }

    var showSeekFeedback by remember { mutableStateOf(false) }
    var seekDirectionText by remember { mutableStateOf("") }

    LaunchedEffect(showSeekFeedback) {
        if (showSeekFeedback) {
            delay(800)
            showSeekFeedback = false
        }
    }

    LaunchedEffect(isPlaying) {
        while (isActive) {
            delay(100)
            if (scrubbedPosition == null) {
                val rawDuration = playerConnection.player.duration
                position = playerConnection.player.currentPosition.coerceAtLeast(0L)
                duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
            }
        }
    }

    val currentDisplayPos = scrubbedPosition ?: position
    val progressFraction = if (duration > 0) (currentDisplayPos.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val volumeFraction = if (isMuted) 0f else playerVolume.coerceIn(0f, 1f)

    val adaptivePrimary = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }
    val adaptiveSecondary = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> if (isDarkTheme) Color.White.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color.White.copy(alpha = 0.65f)
    }

    val adaptiveSurface = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> if (isDarkTheme) Color(0xFF262628) else MaterialTheme.colorScheme.surfaceContainerHigh
        else -> Color.White.copy(alpha = 0.22f)
    }

    val tankFillColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> if (isDarkTheme) Color.White else MaterialTheme.colorScheme.onSurface
        else -> Color.White
    }

    var showQueueSheet by remember { mutableStateOf(false) }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                ArtworkWithGestureBox(
                    mediaMetadata = mediaMetadata,
                    coverResolution = coverResolution,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    adaptiveSurface = adaptiveSurface,
                    isAppleMusicStyle = isAppleMusicStyle,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    showSeekFeedback = showSeekFeedback,
                    seekDirectionText = seekDirectionText,
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    onSwipePrevious = { if (canSkipPrevious) playerConnection.seekToPrevious() },
                    onSwipeNext = { if (canSkipNext) playerConnection.seekToNext() },
                    onDoubleTapRewind = {
                        playerConnection.rewind()
                        seekDirectionText = "-${playerConnection.seekIncrementMs / 1000}s"
                        showSeekFeedback = true
                    },
                    onDoubleTapForward = {
                        playerConnection.fastForward()
                        seekDirectionText = "+${playerConnection.seekIncrementMs / 1000}s"
                        showSeekFeedback = true
                    },
                    modifier = Modifier.fillMaxHeight(0.9f).aspectRatio(1f)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { state.collapseSoft() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = stringResource(R.string.back),
                            tint = adaptivePrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(38.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(adaptivePrimary.copy(alpha = 0.25f))
                            .clickable { state.collapseSoft() }
                    )

                    IconButton(
                        onClick = {
                            menuState.show {
                                PlayerMenu(
                                    mediaMetadata = mediaMetadata,
                                    navController = navController,
                                    playerBottomSheetState = state,
                                    onShowDetailsDialog = {
                                        mediaMetadata?.id?.let {
                                            bottomSheetPageState.show {
                                                ShowMediaInfo(it) { bottomSheetPageState.dismiss() }
                                            }
                                        }
                                    },
                                    onDismiss = menuState::dismiss
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = stringResource(R.string.more_options),
                            tint = adaptivePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                TitleAndSquircleActionsRow(
                    mediaMetadata = mediaMetadata,
                    adaptivePrimary = adaptivePrimary,
                    adaptiveSecondary = adaptiveSecondary,
                    adaptiveSurface = adaptiveSurface,
                    isLiked = isLiked,
                    onToggleLike = { playerConnection.toggleLike() },
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    onOpenQueue = { showQueueSheet = true },
                    navController = navController,
                    state = state
                )

                Spacer(Modifier.height(14.dp))

                TanksAndPlayRow(
                    currentDisplayPos = currentDisplayPos,
                    progressFraction = progressFraction,
                    volumeFraction = volumeFraction,
                    isMuted = isMuted,
                    playerVolume = playerVolume,
                    duration = duration,
                    adaptivePrimary = adaptivePrimary,
                    adaptiveSurface = adaptiveSurface,
                    tankFillColor = tankFillColor,
                    playbackState = playbackState,
                    isPlaying = isPlaying,
                    onSeekFraction = { frac ->
                        if (duration > 0) scrubbedPosition = (frac * duration).toLong()
                    },
                    onSeekRelease = {
                        scrubbedPosition?.let {
                            playerConnection.player.seekTo(it)
                            position = it
                        }
                        scrubbedPosition = null
                    },
                    onVolumeChange = { playerConnection.setVolume(it) },
                    onPlayPauseToggle = {
                        if (playbackState == Player.STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.togglePlayPause()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                )

                Spacer(Modifier.height(14.dp))

                BottomPillControlsRow(
                    adaptiveSurface = adaptiveSurface,
                    adaptivePrimary = adaptivePrimary,
                    adaptiveSecondary = adaptiveSecondary,
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    onToggleShuffle = { playerConnection.toggleShuffle() },
                    onSkipPrevious = { playerConnection.seekToPrevious() },
                    onSkipNext = { playerConnection.seekToNext() },
                    onToggleRepeat = { playerConnection.toggleReplayMode() }
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { state.collapseSoft() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_more),
                        contentDescription = stringResource(R.string.back),
                        tint = adaptivePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(adaptivePrimary.copy(alpha = 0.25f))
                        .clickable { state.collapseSoft() }
                )

                IconButton(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    mediaMetadata?.id?.let {
                                        bottomSheetPageState.show {
                                            ShowMediaInfo(it) { bottomSheetPageState.dismiss() }
                                        }
                                    }
                                },
                                onDismiss = menuState::dismiss
                            )
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = stringResource(R.string.more_options),
                        tint = adaptivePrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Artwork Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.25f, fill = false)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                ArtworkWithGestureBox(
                    mediaMetadata = mediaMetadata,
                    coverResolution = coverResolution,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    adaptiveSurface = adaptiveSurface,
                    isAppleMusicStyle = isAppleMusicStyle,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    showSeekFeedback = showSeekFeedback,
                    seekDirectionText = seekDirectionText,
                    onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                    onSwipePrevious = { if (canSkipPrevious) playerConnection.seekToPrevious() },
                    onSwipeNext = { if (canSkipNext) playerConnection.seekToNext() },
                    onDoubleTapRewind = {
                        playerConnection.rewind()
                        seekDirectionText = "-${playerConnection.seekIncrementMs / 1000}s"
                        showSeekFeedback = true
                    },
                    onDoubleTapForward = {
                        playerConnection.fastForward()
                        seekDirectionText = "+${playerConnection.seekIncrementMs / 1000}s"
                        showSeekFeedback = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(10.dp))

            // Title and 3 Squircles
            TitleAndSquircleActionsRow(
                mediaMetadata = mediaMetadata,
                adaptivePrimary = adaptivePrimary,
                adaptiveSecondary = adaptiveSecondary,
                adaptiveSurface = adaptiveSurface,
                isLiked = isLiked,
                onToggleLike = { playerConnection.toggleLike() },
                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                onOpenQueue = { showQueueSheet = true },
                navController = navController,
                state = state
            )

            Spacer(Modifier.height(14.dp))

            // Middle Row: Left Progress Tank | Center Play/Pause | Right Volume Tank
            TanksAndPlayRow(
                currentDisplayPos = currentDisplayPos,
                progressFraction = progressFraction,
                volumeFraction = volumeFraction,
                isMuted = isMuted,
                playerVolume = playerVolume,
                duration = duration,
                adaptivePrimary = adaptivePrimary,
                adaptiveSurface = adaptiveSurface,
                tankFillColor = tankFillColor,
                playbackState = playbackState,
                isPlaying = isPlaying,
                onSeekFraction = { frac ->
                    if (duration > 0) scrubbedPosition = (frac * duration).toLong()
                },
                onSeekRelease = {
                    scrubbedPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    scrubbedPosition = null
                },
                onVolumeChange = { playerConnection.setVolume(it) },
                onPlayPauseToggle = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )

            Spacer(Modifier.height(16.dp))

            // Bottom Dual Pills
            BottomPillControlsRow(
                adaptiveSurface = adaptiveSurface,
                adaptivePrimary = adaptivePrimary,
                adaptiveSecondary = adaptiveSecondary,
                shuffleModeEnabled = shuffleModeEnabled,
                repeatMode = repeatMode,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                onToggleShuffle = { playerConnection.toggleShuffle() },
                onSkipPrevious = { playerConnection.seekToPrevious() },
                onSkipNext = { playerConnection.seekToNext() },
                onToggleRepeat = { playerConnection.toggleReplayMode() }
            )

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
        ) {
            QueueV2(
                navController = navController,
                playerBottomSheetState = state,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ArtworkWithGestureBox(
    mediaMetadata: MediaMetadata?,
    coverResolution: CoverResolution,
    thumbnailCornerRadius: Float,
    adaptiveSurface: Color,
    isAppleMusicStyle: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    showSeekFeedback: Boolean,
    seekDirectionText: String,
    onOpenFullscreenLyrics: () -> Unit,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    onDoubleTapRewind: () -> Unit,
    onDoubleTapForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
            .background(if (isAppleMusicStyle) Color.Transparent else adaptiveSurface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpenFullscreenLyrics() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2f) {
                            onDoubleTapRewind()
                        } else {
                            onDoubleTapForward()
                        }
                    }
                )
            }
            .SwipeGesture(
                enabled = true,
                onSwipeLeft = onSwipeNext,
                onSwipeRight = onSwipePrevious
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isAppleMusicStyle) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaMetadata?.thumbnailUrl?.resize(coverResolution.size, coverResolution.size))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.cover_art),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        AnimatedVisibility(
            visible = showSeekFeedback,
            enter = fadeIn(tween(150)) + scaleIn(animationSpec = tween(150)),
            exit = fadeOut(tween(250)) + scaleOut(animationSpec = tween(250)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.75f),
                shadowElevation = 8.dp
            ) {
                Text(
                    text = seekDirectionText,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun TitleAndSquircleActionsRow(
    mediaMetadata: MediaMetadata?,
    adaptivePrimary: Color,
    adaptiveSecondary: Color,
    adaptiveSurface: Color,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onOpenFullscreenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    navController: NavController,
    state: BottomSheetState
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = mediaMetadata?.title ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = adaptivePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .basicMarquee()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (mediaMetadata?.album?.id != null) {
                                state.collapseSoft()
                                navController.navigate("album/${mediaMetadata.album.id}")
                            }
                        }
                    )
            )
            Spacer(Modifier.height(3.dp))

            val artistsList = mediaMetadata?.artists.orEmpty()
            val annotatedArtistString = remember(artistsList, adaptiveSecondary) {
                buildAnnotatedString {
                    artistsList.forEachIndexed { index, artist ->
                        val tag = "artist_${artist.id.orEmpty()}"
                        pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
                        withStyle(SpanStyle(color = adaptiveSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)) {
                            append(artist.name)
                        }
                        pop()
                        if (index != artistsList.lastIndex) {
                            append(", ")
                        }
                    }
                }
            }

            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            var clickOffset by remember { mutableStateOf<Offset?>(null) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            ) {
                Text(
                    text = annotatedArtistString,
                    style = MaterialTheme.typography.bodyMedium.copy(color = adaptiveSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { layoutResult = it },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val tapPosition = event.changes.firstOrNull()?.position
                                    if (tapPosition != null) {
                                        clickOffset = tapPosition
                                    }
                                }
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                val tapPosition = clickOffset
                                val layout = layoutResult
                                var navigated = false
                                if (tapPosition != null && layout != null) {
                                    val offset = layout.getOffsetForPosition(tapPosition)
                                    annotatedArtistString.getStringAnnotations(offset, offset)
                                        .firstOrNull()
                                        ?.let { ann ->
                                            val artistId = ann.item
                                            if (artistId.isNotBlank()) {
                                                navController.navigate("artist/$artistId")
                                                state.collapseSoft()
                                                navigated = true
                                            }
                                        }
                                }
                                if (!navigated) {
                                    val firstId = artistsList.firstOrNull { !it.id.isNullOrBlank() }?.id
                                    if (!firstId.isNullOrBlank()) {
                                        navController.navigate("artist/$firstId")
                                        state.collapseSoft()
                                    }
                                }
                            }
                        )
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 1. Like Button
            MorphingSurface(
                onClick = onToggleLike,
                startShape = MaterialShapes.Square,
                endShape = MaterialShapes.Circle,
                containerColor = if (isLiked) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else adaptiveSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = stringResource(R.string.action_like),
                    tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. Lyrics Button
            MorphingSurface(
                onClick = onOpenFullscreenLyrics,
                startShape = MaterialShapes.Square,
                endShape = MaterialShapes.Circle,
                containerColor = adaptiveSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = stringResource(R.string.lyrics),
                    tint = adaptivePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3. Queue Button
            MorphingSurface(
                onClick = onOpenQueue,
                startShape = MaterialShapes.Square,
                endShape = MaterialShapes.Circle,
                containerColor = adaptiveSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.queue),
                    tint = adaptivePrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TanksAndPlayRow(
    currentDisplayPos: Long,
    progressFraction: Float,
    volumeFraction: Float,
    isMuted: Boolean,
    playerVolume: Float,
    duration: Long,
    adaptivePrimary: Color,
    adaptiveSurface: Color,
    tankFillColor: Color,
    playbackState: Int,
    isPlaying: Boolean,
    onSeekFraction: (Float) -> Unit,
    onSeekRelease: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onPlayPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Left Progress Tank
        VerticalTankSlider(
            label = makeTimeString(currentDisplayPos),
            progressFraction = progressFraction,
            fillColor = tankFillColor,
            containerColor = adaptiveSurface,
            adaptivePrimary = adaptivePrimary,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onDragFraction = onSeekFraction,
            onRelease = onSeekRelease
        )

        Spacer(Modifier.width(14.dp))

        // 2. Center: Circle with Smooth Wavy Circular Progress & Cookie Play/Pause
        val density = LocalDensity.current
        val wavyStrokeWidth = 10.dp
        val wavyStrokePx = with(density) { wavyStrokeWidth.toPx() }
        val centerBtnRadiusPx = with(density) { 46.dp.toPx() }

        var isUserDragging by remember { mutableStateOf(false) }

        val animatedProgress by animateFloatAsState(
            targetValue = progressFraction,
            animationSpec = if (isUserDragging) {
                snap()
            } else {
                spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            },
            label = "animatedProgress"
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .aspectRatio(1f)
                .pointerInput(duration) {
                    fun calculateFrac(pos: Offset): Float {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = pos.x - cx
                        val dy = pos.y - cy
                        var angleRad = atan2(dy.toDouble(), dx.toDouble()) + (PI / 2)
                        if (angleRad < 0) angleRad += 2 * PI
                        return (angleRad / (2 * PI)).toFloat().coerceIn(0f, 1f)
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = down.position.x - cx
                        val dy = down.position.y - cy
                        val distance = sqrt(dx * dx + dy * dy)
                        if (distance > centerBtnRadiusPx) {
                            down.consume()
                            isUserDragging = true
                            onSeekFraction(calculateFrac(down.position))

                            val pointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (!change.pressed) {
                                    isUserDragging = false
                                    onSeekRelease()
                                    break
                                }
                                change.consume()
                                onSeekFraction(calculateFrac(change.position))
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(adaptiveSurface.copy(alpha = 0.5f))
            )

            CircularWavyProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = tankFillColor,
                trackColor = adaptiveSurface,
                stroke = Stroke(width = wavyStrokePx, cap = StrokeCap.Round),
                trackStroke = Stroke(width = wavyStrokePx, cap = StrokeCap.Round),
                wavelength = 24.dp,
                waveSpeed = if (isPlaying) 20.dp else 0.dp,
                amplitude = { if (isUserDragging) 0.35f else 0.85f },
                gapSize = 0.dp
            )

            MorphingSurface(
                onClick = onPlayPauseToggle,
                startShape = MaterialShapes.Cookie6Sided,
                endShape = MaterialShapes.Cookie4Sided,
                containerColor = tankFillColor,
                modifier = Modifier.size(92.dp)
            ) {
                Icon(
                    painter = painterResource(
                        when {
                            playbackState == Player.STATE_ENDED -> R.drawable.replay
                            isPlaying -> R.drawable.pause
                            else -> R.drawable.play
                        }
                    ),
                    contentDescription = null,
                    tint = if (tankFillColor.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        // 3. Right Volume Tank
        val volumePercentage = if (isMuted) 0 else (playerVolume * 100).toInt()
        VerticalTankSlider(
            label = "$volumePercentage%",
            progressFraction = volumeFraction,
            fillColor = tankFillColor,
            containerColor = adaptiveSurface,
            adaptivePrimary = adaptivePrimary,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onDragFraction = onVolumeChange,
            onRelease = { }
        )
    }
}

@Composable
private fun BottomPillControlsRow(
    adaptiveSurface: Color,
    adaptivePrimary: Color,
    adaptiveSecondary: Color,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onToggleShuffle: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleRepeat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pill: [ Shuffle | Skip Previous ]
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = adaptiveSurface,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .clickable { onToggleShuffle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = if (shuffleModeEnabled) MaterialTheme.colorScheme.primary else adaptiveSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(adaptivePrimary.copy(alpha = 0.15f))
                )

                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .clickable(enabled = canSkipPrevious) { onSkipPrevious() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_previous),
                        contentDescription = null,
                        tint = if (canSkipPrevious) adaptivePrimary else adaptiveSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Right Pill: [ Skip Next | Repeat ]
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = adaptiveSurface,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .clickable(enabled = canSkipNext) { onSkipNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = null,
                        tint = if (canSkipNext) adaptivePrimary else adaptiveSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(adaptivePrimary.copy(alpha = 0.15f))
                )

                Box(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .clickable { onToggleRepeat() },
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
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else adaptiveSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalTankSlider(
    label: String,
    progressFraction: Float,
    fillColor: Color,
    containerColor: Color,
    adaptivePrimary: Color,
    modifier: Modifier = Modifier,
    onDragFraction: (Float) -> Unit,
    onRelease: () -> Unit
) {
    val animatedFraction by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tankFraction"
    )

    val isSubmerged = animatedFraction >= 0.87f
    val targetTextColor = if (isSubmerged) {
        if (fillColor.luminance() > 0.5f) Color(0xFF161616) else Color.White
    } else {
        adaptivePrimary
    }

    val animatedTextColor by animateColorAsState(
        targetValue = targetTextColor,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "tankTextColorAnim"
    )

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(containerColor)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val h = size.height.toFloat()
                        val frac = (1f - (offset.y / h)).coerceIn(0f, 1f)
                        onDragFraction(frac)
                    },
                    onVerticalDrag = { change, _ ->
                        val h = size.height.toFloat()
                        val frac = (1f - (change.position.y / h)).coerceIn(0f, 1f)
                        onDragFraction(frac)
                    },
                    onDragEnd = { onRelease() },
                    onDragCancel = { onRelease() }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = size.height.toFloat()
                    val frac = (1f - (offset.y / h)).coerceIn(0f, 1f)
                    onDragFraction(frac)
                    onRelease()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillHeight = size.height * animatedFraction
            val topY = size.height - fillHeight
            drawRect(
                color = fillColor,
                topLeft = Offset(0f, topY),
                size = Size(size.width, fillHeight)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = animatedTextColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )
    }
}