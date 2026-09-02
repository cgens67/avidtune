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
                .clip(RoundedCornerShape(8.dp)) // Apple rectangular shape with slightly rounded corners
                .background(color = backgroundColor)
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
