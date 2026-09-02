@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.together.TogetherRole
import com.cgens67.avidtune.together.TogetherSessionState
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    
    // Theme settings
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val style by rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.DEFAULT)
    
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val disableBlur by rememberPreference(DisableBlurKey, false)
    
    // Player states
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    
    // Listen together guest check
    val sessionState by playerConnection.service.togetherSessionState.collectAsState()
    val isListenTogetherGuest = sessionState is TogetherSessionState.Joined && 
        (sessionState as TogetherSessionState.Joined).role is TogetherRole.Guest

    // Swipe settings
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val swipeSensitivity = 0.73f
    
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(configuration.screenWidthDp, configuration.orientation) {
        configuration.screenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = remember {
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    }

    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    // Extract colors
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColorArgb = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT ||
            playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
            playerBackground == PlayerBackgroundStyle.LIVE_MESH
        ) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    ).drawable as? android.graphics.drawable.BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { bitmap ->
                    val palette = androidx.palette.graphics.Palette.from(bitmap)
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
    
    // Memoize colors
    val backgroundColor = if (pureBlack && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val isDynamicBackground = playerBackground != PlayerBackgroundStyle.DEFAULT
    
    val primaryColor = if (isDynamicBackground) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = if (isDynamicBackground) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (isDynamicBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPreviousLocal = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNextLocal = playerConnection.player.nextMediaItemIndex != -1
                                val tryingToSwipeRight = adjustedDragAmount > 0
                                val tryingToSwipeLeft = adjustedDragAmount < 0
                                val allowLeft = tryingToSwipeLeft && canSkipNextLocal
                                val allowRight = tryingToSwipeRight && canSkipPreviousLocal

                                val canReturnToCenter =
                                    (tryingToSwipeRight && !canSkipPreviousLocal && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNextLocal && offsetXAnimatable.value > 0)

                                if (allowLeft || allowRight || canReturnToCenter) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value
                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val canSkipPreviousLocal = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNextLocal = playerConnection.player.nextMediaItemIndex != -1

                                val shouldChangeSong = (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                    (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    if (currentOffset > 0 && canSkipPreviousLocal) {
                                        playerConnection.seekToPrevious()
                                    } else if (currentOffset <= 0 && canSkipNextLocal) {
                                        playerConnection.seekToNext()
                                    }
                                }
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            }
                        )
                    }
                } else baseModifier
            }
    ) {
        Box(
            modifier = Modifier
                .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                .height(64.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(8.dp)) // slightly rounded corners
                .let {
                    if (style == MiniPlayerStyle.DEFAULT) {
                        it.background(color = if (playerBackground == PlayerBackgroundStyle.LIVE_MESH) Color.Black else MaterialTheme.colorScheme.surfaceContainer)
                    } else {
                        it.background(color = backgroundColor)
                    }
                }
        ) {
            // Background Layers
            if (isDynamicBackground) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(40.dp) else it }
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                            }
                        }
                        PlayerBackgroundStyle.GRADIENT -> {
                            if (gradientColors.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(gradientColors)).background(Color.Black.copy(0.2f)))
                            }
                        }
                        PlayerBackgroundStyle.APPLE_MUSIC -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(48.dp) else it }
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                            }
                        }
                        PlayerBackgroundStyle.LIVE_MESH -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                val infiniteTransition = rememberInfiniteTransition(label = "mesh_transition")
                                val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "rotation")
                                val satMatrix = remember { ColorMatrix().apply { setToSaturation(1.6f) } }
                                Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(satMatrix),
                                        modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(40.dp) else it }.graphicsLayer { rotationZ = rotation }
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            if (style == MiniPlayerStyle.DEFAULT) {
                // Default Mini Player Style
                NewMiniPlayerContent(
                    pureBlack = pureBlack,
                    position = position,
                    duration = duration,
                    playerConnection = playerConnection,
                    isLiveMesh = playerBackground == PlayerBackgroundStyle.LIVE_MESH
                )
            } else {
                // Apple Music Mini Player Style
                // Bottom Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                        .drawWithContent {
                            val progress = if (duration > 0) position.toFloat() / duration else 0f
                            val trackColor = outlineColor.copy(alpha = 0.2f)
                            drawRect(trackColor)
                            drawRect(primaryColor, size = Size(size.width * progress.coerceIn(0f, 1f), size.height))
                        }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    // Cookie 4-Sided Thumbnail
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(MaterialShapes.Cookie4Sided.toShape())
                            .background(color = outlineColor.copy(alpha = 0.2f))
                    ) {
                        mediaMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = onSurfaceColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Song info - title and artist
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val error by playerConnection.error.collectAsState()
                        
                        mediaMetadata?.let { metadata ->
                            Text(
                                text = metadata.title,
                                color = onSurfaceColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (metadata.explicit) {
                                    Icon(
                                        painter = painterResource(R.drawable.explicit),
                                        contentDescription = null,
                                        tint = onSurfaceColor.copy(alpha = 0.7f),
                                        modifier = Modifier.size(14.dp).padding(end = 2.dp)
                                    )
                                }
                                if (metadata.artists.any { it.name.isNotBlank() }) {
                                    Text(
                                        text = metadata.artists.joinToString { it.name },
                                        color = onSurfaceColor.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                                    )
                                }
                            }

                            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                                Text(
                                    text = stringResource(R.string.error_playing),
                                    color = errorColor,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Play/Pause Button
                    val isPlaying by playerConnection.isPlaying.collectAsState()
                    val isMuted by playerConnection.isMuted.collectAsState()
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (isListenTogetherGuest) {
                                    playerConnection.toggleMute()
                                    return@clickable
                                }
                                if (playbackState == Player.STATE_ENDED) {
                                    playerConnection.player.seekTo(0, 0)
                                    playerConnection.player.playWhenReady = true
                                } else {
                                    playerConnection.togglePlayPause()
                                }
                            }
                    ) {
                        Icon(
                            painter = painterResource(
                                when {
                                    isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                                    isPlaying -> R.drawable.pause 
                                    else -> R.drawable.play
                                }
                            ),
                            contentDescription = null,
                            tint = onSurfaceColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Next Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canSkipNext && !isListenTogetherGuest) {
                                playerConnection.seekToNext()
                            }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = "Next",
                            tint = if (canSkipNext && !isListenTogetherGuest) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Visual swipe indicators (Left/Right skip hints)
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        androidx.compose.animation.AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isLiveMesh: Boolean = false
) {
    val progressColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.primary
    val progressTrackColor = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val imageBorderColor = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    
    val context = LocalContext.current
    val (miniPlayerShape) = rememberPreference(
        MiniPlayerThumbnailShapeKey,
        DefaultMiniPlayerThumbnailShape
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(47.dp)
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = progressColor,
                trackColor = progressTrackColor
            )
        } else {
            androidx.compose.material3.CircularWavyProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxSize(),
                color = progressColor,
                trackColor = progressTrackColor
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(37.dp)
                .clip(com.cgens67.avidtune.utils.getMiniPlayerThumbnailShape(miniPlayerShape).toShape())
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = imageBorderColor,
                    shape = com.cgens67.avidtune.utils.getMiniPlayerThumbnailShape(miniPlayerShape).toShape()
                )
        ) {
            val thumbnailUrl = mediaMetadata?.thumbnailUrl
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerTransportButton(
    iconResId: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    containerColorPrimary: Color = MaterialTheme.colorScheme.surface,
    borderColorEnabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    borderColorDisabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
) {
    val containerColor =
        if (isPrimary) containerColorPrimary else Color.Transparent
    val borderColor =
        if (enabled) borderColorEnabled
        else borderColorDisabled
    val iconTint =
        if (enabled) iconColor
        else disabledIconColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(modifier)
            .size(if (isPrimary) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(if (isPrimary) 22.dp else 18.dp)
        )
    }
}

@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playerConnection: PlayerConnection,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    containerColorPrimary: Color = MaterialTheme.colorScheme.surface,
    borderColorEnabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    borderColorDisabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_previous,
            contentDescription = null,
            onClick = { playerConnection.seekToPrevious() },
            enabled = canSkipPrevious,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            MiniPlayerTransportButton(
                iconResId = when {
                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                    isPlaying -> R.drawable.pause
                    else -> R.drawable.play
                },
                contentDescription = stringResource(
                    if (playbackState == Player.STATE_ENDED || !isPlaying) R.string.play else R.string.play
                ).let {
                    if (isPlaying && playbackState != Player.STATE_ENDED) "Pause" else it
                },
                onClick = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                },
                isPrimary = true,
                iconColor = iconColor,
                disabledIconColor = disabledIconColor,
                containerColorPrimary = containerColorPrimary,
                borderColorEnabled = borderColorEnabled,
                borderColorDisabled = borderColorDisabled
            )
        }

        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_next,
            contentDescription = null,
            onClick = { playerConnection.seekToNext() },
            enabled = canSkipNext,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )
    }
}

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    isLiveMesh: Boolean = false
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val isLoading = playbackState == Player.STATE_BUFFERING

    val primaryTextColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isLiveMesh) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.onSurface
    val disabledIconColor = if (isLiveMesh) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val containerColorPrimary = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
    val borderColorEnabled = if (isLiveMesh) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderColorDisabled = if (isLiveMesh) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        MiniPlayerArtwork(
            mediaMetadata = mediaMetadata,
            position = position,
            duration = duration,
            isLoading = isLoading,
            isLiveMesh = isLiveMesh
        )

        Spacer(modifier = Modifier.width(5.dp))

        mediaMetadata?.let {
            MiniPlayerInfo(
                mediaMetadata = it,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        } ?: Spacer(Modifier.weight(1f))

        Spacer(modifier = Modifier.width(12.dp))

        MiniPlayerTransportControls(
            isPlaying = isPlaying,
            playbackState = playbackState,
            isLoading = isLoading,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            playerConnection = playerConnection,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: MediaMetadata,
    error: androidx.media3.common.PlaybackException?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
