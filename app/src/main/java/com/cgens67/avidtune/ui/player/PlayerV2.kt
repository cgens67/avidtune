@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
    ExperimentalFoundationApi::class,
)

package com.cgens67.avidtune.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.HidePlayerThumbnailKey
import com.cgens67.avidtune.constants.MinimalPlayerDesignKey
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.PlayerThumbnailShadowElevationKey
import com.cgens67.avidtune.constants.ShowPlayerThumbnailShadowKey
import com.cgens67.avidtune.constants.SliderStyle
import com.cgens67.avidtune.constants.SliderStyleKey
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.together.TogetherRole
import com.cgens67.avidtune.together.TogetherSessionState
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.BottomSheetState
import com.cgens67.avidtune.ui.component.LocalBottomSheetPageState
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.LyricsV2
import com.cgens67.avidtune.ui.component.PlayerSliderTrack
import com.cgens67.avidtune.ui.menu.LyricsMenu
import com.cgens67.avidtune.ui.menu.PlayerMenu
import com.cgens67.avidtune.ui.menu.ShowMediaInfo
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.saket.squiggles.SquigglySlider

enum class PlayerInternalState { COVER, LYRICS_PREVIEW, LYRICS, QUEUE_PREVIEW, QUEUE }

fun Modifier.customSoftShadow(
    elevation: Dp,
    cornerRadius: Dp = 12.dp,
    enabled: Boolean = true
): Modifier = if (enabled && elevation > 0.dp) {
    this.shadow(
        elevation = elevation,
        shape = RoundedCornerShape(cornerRadius),
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.25f),
        spotColor = Color.Black.copy(alpha = 0.35f)
    )
} else this

fun Modifier.SwipeGesture(
    enabled: Boolean = true,
    onSwipeLeft: () -> Unit = {},
    onSwipeRight: () -> Unit = {}
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
    var totalDrag = 0f
    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { _, dragAmount ->
            totalDrag += dragAmount
        },
        onDragEnd = {
            if (totalDrag > 80f) {
                onSwipeRight()
            } else if (totalDrag < -80f) {
                onSwipeLeft()
            }
        },
        onDragCancel = { totalDrag = 0f }
    )
}

object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        activeColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ): SliderColors = SliderDefaults.colors(
        activeTrackColor = activeColor,
        inactiveTrackColor = activeColor.copy(alpha = 0.25f),
        thumbColor = activeColor
    )
}

fun getConnectedBluetoothDeviceName(context: Context): String? {
    return try {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            devices.firstOrNull {
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER
            }?.productName?.toString()
        } else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun AudioDeviceBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val bluetoothDevice = remember { getConnectedBluetoothDeviceName(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.audio_output),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (bluetoothDevice != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bluetoothDevice,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.connected_bluetooth),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (bluetoothDevice == null)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.volume_up),
                        contentDescription = null,
                        tint = if (bluetoothDevice == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.this_device),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.internal_speaker),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bluetoothDevice == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent("android.settings.panel.action.MEDIA_OUTPUT")
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(stringResource(R.string.open_system_output_switcher))
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerV2(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = (context as? android.app.Activity) ?: run {
            var ctx = context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is android.app.Activity) break
                ctx = ctx.baseContext
            }
            ctx as? android.app.Activity
        }
        val originalOrientation = activity?.requestedOrientation ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val queueWindows by playerConnection.queueWindows.collectAsState()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsState()
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    
    // State management: defaults to COVER
    var playerState by remember { mutableStateOf(PlayerInternalState.COVER) }

    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val isListenTogetherGuest = (togetherSessionState as? TogetherSessionState.Joined)?.role is TogetherRole.Guest
    val isMuted by playerConnection.isMuted.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerVolume by playerConnection.service.playerVolume.collectAsState()

    // Back handler smoothly steps down from full expanded -> cover
    BackHandler(enabled = playerState != PlayerInternalState.COVER) {
        playerState = PlayerInternalState.COVER
    }

    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(playerState) {
        if (playerState == PlayerInternalState.LYRICS) {
            controlsVisible = true
            delay(1500L)
            if (playerState == PlayerInternalState.LYRICS) {
                controlsVisible = false
            }
        } else {
            controlsVisible = true
        }
    }

    LaunchedEffect(lastInteractionTime) {
        if (playerState == PlayerInternalState.LYRICS) {
            controlsVisible = true
            delay(2000L)
            if (playerState == PlayerInternalState.LYRICS) {
                controlsVisible = false
            }
        }
    }

    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }

    val minimalPlayerDesign by rememberPreference(MinimalPlayerDesignKey, defaultValue = false)

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val sliderStyle by rememberEnumPreference<SliderStyle>(
        key = SliderStyleKey,
        defaultValue = SliderStyle.EXPANDING
    )

    val showPlayerThumbnailShadow by rememberPreference(ShowPlayerThumbnailShadowKey, defaultValue = false)
    val playerThumbnailShadowElevation by rememberPreference(PlayerThumbnailShadowElevationKey, defaultValue = 8f)
    val hidePlayerThumbnail by rememberPreference(HidePlayerThumbnailKey, false)

    var showAudioDeviceBottomSheet by remember { mutableStateOf(false) }

    val bluetoothDeviceName by produceState<String?>(initialValue = getConnectedBluetoothDeviceName(context)) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val scope = this
        val updateDeviceState: () -> Unit = {
            scope.value = getConnectedBluetoothDeviceName(context)
            scope.launch {
                delay(500)
                scope.value = getConnectedBluetoothDeviceName(context)
            }
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                updateDeviceState()
            }
        }
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : android.media.AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateDeviceState()
                }
                override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                    updateDeviceState()
                }
            }
        } else null

        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction("android.bluetooth.adapter.action.STATE_CHANGED")
            addAction("android.bluetooth.device.action.ACL_CONNECTED")
            addAction("android.bluetooth.device.action.ACL_DISCONNECTED")
            addAction("android.media.AUDIO_BECOMING_NOISY")
        }

        context.registerReceiver(receiver, filter)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
            am.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }

        awaitDispose {
            context.unregisterReceiver(receiver)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                am.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(100)
                if (sliderPosition == null) {
                    val rawDuration = playerConnection.player.duration
                    position = playerConnection.player.currentPosition.coerceAtLeast(0L)
                    duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
                }
            }
        }
    }

    LaunchedEffect(mediaMetadata?.id) {
        val rawDuration = playerConnection.player.duration
        position = playerConnection.player.currentPosition.coerceAtLeast(0L)
        duration = if (rawDuration == C.TIME_UNSET || rawDuration < 0) 0L else rawDuration
    }

    val adaptivePrimary by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurface
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        },
        label = "adaptivePrimary"
    )
    val adaptiveSecondary by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onSurfaceVariant
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White.copy(alpha = 0.7f)
        },
        label = "adaptiveSecondary"
    )
    val adaptiveSurface by animateColorAsState(
        targetValue = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant
            PlayerBackgroundStyle.BLUR,
            PlayerBackgroundStyle.GRADIENT,
            PlayerBackgroundStyle.APPLE_MUSIC,
            PlayerBackgroundStyle.LIVE_MESH -> Color.White.copy(alpha = 0.2f)
        },
        label = "adaptiveSurface"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            if (playerState == PlayerInternalState.LYRICS) {
                                lastInteractionTime = System.currentTimeMillis()
                            } else {
                                controlsVisible = true
                            }
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                .padding(bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(adaptivePrimary.copy(alpha = 0.3f))
                        .clickable { state.collapseSoft() }
                )
            }

            SharedTransitionLayout(
                modifier = Modifier.weight(1f)
            ) {
                AnimatedContent(
                    targetState = playerState,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) togetherWith
                         fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)))
                            .apply { targetContentZIndex = 1f }
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "InternalWindow"
                ) { targetState ->
                    when (targetState) {
                        PlayerInternalState.COVER,
                        PlayerInternalState.LYRICS_PREVIEW,
                        PlayerInternalState.QUEUE_PREVIEW -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 24.dp)
                            ) {
                                Spacer(modifier = Modifier.weight(1f))

                                val coverRadius = thumbnailCornerRadius.dp
                                val isAppleMusicBg = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC

                                Box(
                                    modifier = Modifier
                                        .sharedElement(
                                            rememberSharedContentState(key = "coverArt"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ ->
                                                tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            }
                                        )
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .customSoftShadow(
                                            elevation = playerThumbnailShadowElevation.dp,
                                            cornerRadius = coverRadius,
                                            enabled = showPlayerThumbnailShadow && !isAppleMusicBg
                                        )
                                        .background(
                                            if (isAppleMusicBg) Color.Transparent else adaptiveSurface,
                                            RoundedCornerShape(coverRadius)
                                        )
                                        .clip(RoundedCornerShape(coverRadius))
                                        .clickable { 
                                            // Quick preview toggle on tap
                                            playerState = if (playerState == PlayerInternalState.LYRICS_PREVIEW) 
                                                PlayerInternalState.COVER 
                                            else 
                                                PlayerInternalState.LYRICS_PREVIEW
                                        }
                                        .SwipeGesture(
                                            enabled = !isListenTogetherGuest,
                                            onSwipeLeft = { if (canSkipNext) playerConnection.player.seekToNext() },
                                            onSwipeRight = { if (canSkipPrevious) playerConnection.player.seekToPrevious() }
                                        )
                                ) {
                                    if (isAppleMusicBg) {
                                        // Hidden when Apple Music background style is active
                                    } else if (hidePlayerThumbnail) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.avidtune_monochrome),
                                                contentDescription = null,
                                                modifier = Modifier.size(72.dp),
                                                tint = adaptivePrimary.copy(alpha = 0.5f)
                                            )
                                        }
                                    } else {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(mediaMetadata?.thumbnailUrl?.resize(coverResolution.size, coverResolution.size))
                                                .memoryCachePolicy(CachePolicy.ENABLED)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .networkCachePolicy(CachePolicy.ENABLED)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = stringResource(R.string.cover_art),
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Inline preview overlay for Lyrics
                                    if (targetState == PlayerInternalState.LYRICS_PREVIEW) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.72f),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                LyricsV2(
                                                    mediaMetadata = mediaMetadata,
                                                    showLyrics = true,
                                                    positionProvider = { sliderPosition ?: position },
                                                    textColor = Color.White,
                                                    modifier = Modifier.fillMaxSize().padding(12.dp)
                                                )
                                                // Expand Button
                                                Surface(
                                                    onClick = { playerState = PlayerInternalState.LYRICS },
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(12.dp)
                                                        .size(36.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.expand_more),
                                                            contentDescription = "Expand Lyrics",
                                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Inline preview overlay for Queue
                                    if (targetState == PlayerInternalState.QUEUE_PREVIEW) {
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.78f),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Up next",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        // Expand Button
                                                        Surface(
                                                            onClick = { playerState = PlayerInternalState.QUEUE },
                                                            shape = CircleShape,
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.expand_more),
                                                                    contentDescription = "Expand Queue",
                                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = 180f }
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    // Show next 3 upcoming songs
                                                    val nextSongs = queueWindows.drop(currentWindowIndex + 1).take(3)
                                                    if (nextSongs.isEmpty()) {
                                                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = "No upcoming songs",
                                                                color = Color.White.copy(alpha = 0.6f),
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                        }
                                                    } else {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            nextSongs.forEach { window ->
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                    AsyncImage(
                                                                        model = window.mediaItem.metadata?.thumbnailUrl,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                                                        contentScale = ContentScale.Crop
                                                                    )
                                                                    Spacer(modifier = Modifier.width(10.dp))
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = window.mediaItem.metadata?.title ?: "",
                                                                            color = Color.White,
                                                                            style = MaterialTheme.typography.bodyMedium,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                        Text(
                                                                            text = window.mediaItem.metadata?.artists?.joinToString { it.name } ?: "",
                                                                            color = Color.White.copy(alpha = 0.6f),
                                                                            style = MaterialTheme.typography.bodySmall,
                                                                            maxLines = 1,
                                                                            overflow = TextOverflow.Ellipsis
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Track Meta Info
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mediaMetadata?.title ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "title"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                boundsTransform = BoundsTransform { _, _ ->
                                                    tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                }
                                            ).clickable {
                                                state.collapseSoft()
                                                mediaMetadata?.album?.id?.let { navController.navigate("album/$it") }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = mediaMetadata?.artists?.firstOrNull()?.name ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = adaptiveSecondary,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "artist"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                boundsTransform = BoundsTransform { _, _ ->
                                                    tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                }
                                            ).clickable {
                                                state.collapseSoft()
                                                mediaMetadata?.artists?.firstOrNull()?.id?.let { navController.navigate("artist/$it") }
                                            }
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            boundsTransform = BoundsTransform { _, _ ->
                                                tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                            }
                                        )
                                    ) {
                                        val isLiked = currentSong?.song?.liked == true

                                        if (minimalPlayerDesign) {
                                            IconButton(
                                                onClick = playerConnection::toggleLike,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                                                    contentDescription = if (isLiked) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library),
                                                    tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

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
                                                                        ShowMediaInfo(it) {
                                                                            bottomSheetPageState.dismiss()
                                                                        }
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
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(adaptivePrimary.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                IconButton(onClick = playerConnection::toggleLike) {
                                                    Icon(
                                                        painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                                                        contentDescription = if (isLiked) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library),
                                                        tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(adaptivePrimary.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
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
                                                                            ShowMediaInfo(it) {
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
                                                        painter = painterResource(R.drawable.more_horiz),
                                                        contentDescription = stringResource(R.string.more_options),
                                                        tint = adaptivePrimary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PlayerInternalState.LYRICS,
                        PlayerInternalState.QUEUE -> {
                            // Fully Expanded Mode
                            Column(modifier = Modifier.fillMaxSize()) {
                                val miniRadius = (thumbnailCornerRadius / 2f).coerceAtLeast(4f).dp
                                val isAppleMusicBg = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!isAppleMusicBg) {
                                        Box(
                                            modifier = Modifier
                                                .sharedElement(
                                                    rememberSharedContentState(key = "coverArt"),
                                                    animatedVisibilityScope = this@AnimatedContent,
                                                    zIndexInOverlay = 1f,
                                                    boundsTransform = BoundsTransform { _, _ ->
                                                        tween(durationMillis = 500, easing = FastOutSlowInEasing)
                                                    }
                                                )
                                                .size(64.dp)
                                                .customSoftShadow(
                                                    elevation = playerThumbnailShadowElevation.dp / 2f,
                                                    cornerRadius = miniRadius,
                                                    enabled = showPlayerThumbnailShadow
                                                )
                                                .background(adaptiveSurface, RoundedCornerShape(miniRadius))
                                                .clip(RoundedCornerShape(miniRadius))
                                                .clickable { playerState = PlayerInternalState.COVER }
                                        ) {
                                            if (hidePlayerThumbnail) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.avidtune_monochrome),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(32.dp),
                                                        tint = adaptivePrimary.copy(alpha = 0.5f)
                                                    )
                                                }
                                            } else {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(LocalContext.current)
                                                        .data(mediaMetadata?.thumbnailUrl?.resize(coverResolution.size, coverResolution.size))
                                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                                        .diskCachePolicy(CachePolicy.ENABLED)
                                                        .networkCachePolicy(CachePolicy.ENABLED)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = stringResource(R.string.cover_art),
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mediaMetadata?.title ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = adaptivePrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "title"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                enter = fadeIn(tween(400)),
                                                exit = fadeOut(tween(300))
                                            )
                                        )
                                        Text(
                                            text = mediaMetadata?.artists?.firstOrNull()?.name ?: stringResource(R.string.unknown),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = adaptiveSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.sharedBounds(
                                                rememberSharedContentState(key = "artist"),
                                                animatedVisibilityScope = this@AnimatedContent,
                                                enter = fadeIn(tween(400)),
                                                exit = fadeOut(tween(300))
                                            )
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.sharedBounds(
                                            rememberSharedContentState(key = "actionButtons"),
                                            animatedVisibilityScope = this@AnimatedContent,
                                            enter = fadeIn(tween(400)),
                                            exit = fadeOut(tween(300))
                                        )
                                    ) {
                                        // Collapse to Cover Button
                                        IconButton(onClick = { playerState = PlayerInternalState.COVER }) {
                                            Icon(
                                                painter = painterResource(R.drawable.close),
                                                contentDescription = stringResource(R.string.collapse),
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (targetState == PlayerInternalState.QUEUE) {
                                                    menuState.show {
                                                        PlayerMenu(
                                                            mediaMetadata = mediaMetadata,
                                                            navController = navController,
                                                            playerBottomSheetState = state,
                                                            onShowDetailsDialog = {
                                                                mediaMetadata?.id?.let {
                                                                    bottomSheetPageState.show {
                                                                        ShowMediaInfo(it) {
                                                                            bottomSheetPageState.dismiss()
                                                                        }
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = menuState::dismiss
                                                        )
                                                    }
                                                } else {
                                                    menuState.show {
                                                        LyricsMenu(
                                                            lyricsEntity = currentLyrics,
                                                            mediaMetadata = mediaMetadata!!,
                                                            onDismiss = menuState::dismiss,
                                                            navController = navController
                                                        )
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = stringResource(R.string.more_options),
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .animateEnterExit(
                                            enter = fadeIn(tween(350, easing = FastOutSlowInEasing)),
                                            exit = fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                                        )
                                ) {
                                    if (targetState == PlayerInternalState.LYRICS) {
                                        LyricsV2(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = true,
                                            positionProvider = { sliderPosition ?: position },
                                            textColor = adaptivePrimary
                                        )
                                    } else {
                                        QueueV2(
                                            navController = navController,
                                            playerBottomSheetState = state,
                                            modifier = Modifier.fillMaxSize(),
                                            onControlsVisibilityChange = { isVisible ->
                                                controlsVisible = isVisible
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Controls & Bottom Action Bar
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                        slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { it / 3 },
                exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                       slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 3 }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        val currentPos = sliderPosition ?: position

                        when (sliderStyle) {
                            SliderStyle.EXPANDING -> {
                                val trackInteractionSource = remember { MutableInteractionSource() }
                                val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
                                val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
                                val isTrackActive = isTrackDragged || isTrackPressed

                                val trackHeight by animateDpAsState(
                                    targetValue = if (isTrackActive) 12.dp else 6.dp,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                    label = "trackScale"
                                )

                                Slider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { value ->
                                        if (!isListenTogetherGuest) {
                                            sliderPosition = value.toLong()
                                        }
                                    },
                                    onValueChangeFinished = {
                                        if (!isListenTogetherGuest) {
                                            sliderPosition?.let { pos ->
                                                playerConnection.player.seekTo(pos)
                                                position = pos
                                                sliderPosition = null
                                            }
                                        }
                                    },
                                    enabled = !isListenTogetherGuest,
                                    interactionSource = trackInteractionSource,
                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            trackHeight = trackHeight,
                                            colors = PlayerSliderColors.getSliderColors(
                                                activeColor = adaptivePrimary.copy(alpha = 0.8f),
                                                playerBackground = playerBackground,
                                                useDarkTheme = isSystemInDarkTheme()
                                            )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            SliderStyle.DEFAULT -> {
                                Slider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { value ->
                                        if (!isListenTogetherGuest) sliderPosition = value.toLong()
                                    },
                                    onValueChangeFinished = {
                                        if (!isListenTogetherGuest) {
                                            sliderPosition?.let { pos ->
                                                playerConnection.player.seekTo(pos)
                                                position = pos
                                                sliderPosition = null
                                            }
                                        }
                                    },
                                    enabled = !isListenTogetherGuest,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = adaptivePrimary,
                                        inactiveTrackColor = adaptivePrimary.copy(alpha = 0.25f),
                                        thumbColor = adaptivePrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            SliderStyle.SQUIGGLY -> {
                                SquigglySlider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { value ->
                                        if (!isListenTogetherGuest) sliderPosition = value.toLong()
                                    },
                                    onValueChangeFinished = {
                                        if (!isListenTogetherGuest) {
                                            sliderPosition?.let { pos ->
                                                playerConnection.player.seekTo(pos)
                                                position = pos
                                                sliderPosition = null
                                            }
                                        }
                                    },
                                    enabled = !isListenTogetherGuest,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = adaptivePrimary,
                                        inactiveTrackColor = adaptivePrimary.copy(alpha = 0.25f),
                                        thumbColor = adaptivePrimary
                                    ),
                                    squigglesSpec = SquigglySlider.SquigglesSpec(
                                        amplitude = if (isPlaying) (4.dp).coerceAtLeast(2.dp) else 0.dp,
                                        strokeWidth = 3.dp,
                                        wavelength = 36.dp,
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            SliderStyle.SLIM -> {
                                Slider(
                                    value = currentPos.toFloat(),
                                    valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                                    onValueChange = { value ->
                                        if (!isListenTogetherGuest) sliderPosition = value.toLong()
                                    },
                                    onValueChangeFinished = {
                                        if (!isListenTogetherGuest) {
                                            sliderPosition?.let { pos ->
                                                playerConnection.player.seekTo(pos)
                                                position = pos
                                                sliderPosition = null
                                            }
                                        }
                                    },
                                    enabled = !isListenTogetherGuest,
                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = adaptivePrimary,
                                                inactiveTrackColor = adaptivePrimary.copy(alpha = 0.25f),
                                                thumbColor = adaptivePrimary
                                            ),
                                            trackHeight = 3.dp
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = makeTimeString(currentPos),
                                color = adaptiveSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "-" + makeTimeString(maxOf(0L, duration - currentPos)),
                                color = adaptiveSecondary,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(if (minimalPlayerDesign) 10.dp else 16.dp))

                        // Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (minimalPlayerDesign) {
                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest && canSkipPrevious) {
                                            playerConnection.player.seekToPrevious()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipPrevious,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipPrevious) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_previous),
                                        contentDescription = null,
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Surface(
                                    onClick = {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                        } else {
                                            playerConnection.player.togglePlayPause()
                                        }
                                    },
                                    shape = CircleShape,
                                    color = adaptivePrimary.copy(alpha = 0.14f),
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        if (isListenTogetherGuest) {
                                            Icon(
                                                painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                                contentDescription = stringResource(R.string.volume),
                                                modifier = Modifier.size(36.dp),
                                                tint = adaptivePrimary
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                                contentDescription = stringResource(if (isPlaying) R.string.media3_controls_pause_description else R.string.play),
                                                modifier = Modifier.size(38.dp),
                                                tint = adaptivePrimary
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipNext,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipNext) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = null,
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest && canSkipPrevious) {
                                            playerConnection.player.seekToPrevious()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipPrevious,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipPrevious) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_previous),
                                        contentDescription = null,
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (isListenTogetherGuest) {
                                            playerConnection.toggleMute()
                                        } else {
                                            playerConnection.player.togglePlayPause()
                                        }
                                    },
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    if (isListenTogetherGuest) {
                                        Icon(
                                            painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                            contentDescription = stringResource(R.string.volume),
                                            modifier = Modifier.size(64.dp),
                                            tint = adaptivePrimary
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                            contentDescription = stringResource(if (isPlaying) R.string.media3_controls_pause_description else R.string.play),
                                            modifier = Modifier.size(80.dp),
                                            tint = adaptivePrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (!isListenTogetherGuest && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    },
                                    enabled = !isListenTogetherGuest && canSkipNext,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .alpha(if (isListenTogetherGuest || !canSkipNext) 0.4f else 1f)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = null,
                                        tint = adaptivePrimary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }

                        if (!minimalPlayerDesign) {
                            Spacer(modifier = Modifier.height(24.dp))

                            // Audio Volume Row - Synchronized with Player Menu Volume
                            var volumeSliderPosition by remember { mutableStateOf<Float?>(null) }
                            val currentEffectiveVolume = volumeSliderPosition ?: (if (isMuted) 0f else playerVolume)
                            val volumeInteractionSource = remember { MutableInteractionSource() }
                            val isVolDragged by volumeInteractionSource.collectIsDraggedAsState()
                            val isVolPressed by volumeInteractionSource.collectIsPressedAsState()
                            val isVolActive = isVolDragged || isVolPressed

                            val volHeight by animateDpAsState(
                                targetValue = if (isVolActive) 12.dp else 6.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                label = "volHeight"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_mute),
                                    contentDescription = stringResource(R.string.volume_down),
                                    tint = adaptiveSecondary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            volumeSliderPosition = null
                                            playerConnection.toggleMute()
                                        }
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Slider(
                                    value = currentEffectiveVolume.coerceIn(0f, 1f),
                                    onValueChange = { newValue ->
                                        volumeSliderPosition = newValue
                                        playerConnection.setVolume(newValue)
                                    },
                                    onValueChangeFinished = {
                                        volumeSliderPosition = null
                                    },
                                    valueRange = 0f..1f,
                                    interactionSource = volumeInteractionSource,
                                    thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                                    track = { sliderState ->
                                        PlayerSliderTrack(
                                            sliderState = sliderState,
                                            trackHeight = volHeight,
                                            colors = PlayerSliderColors.getSliderColors(
                                                activeColor = adaptivePrimary.copy(alpha = 0.8f),
                                                playerBackground = playerBackground,
                                                useDarkTheme = isSystemInDarkTheme()
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = stringResource(R.string.volume_up),
                                    tint = adaptiveSecondary,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable {
                                            volumeSliderPosition = null
                                            playerConnection.setVolume(1f)
                                        }
                                )
                            }
                        }
                    }

                    // Bottom Utility Action Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = if (minimalPlayerDesign) 12.dp else 24.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isLyricsActive = playerState == PlayerInternalState.LYRICS || playerState == PlayerInternalState.LYRICS_PREVIEW
                        IconButton(
                            onClick = {
                                playerState = when (playerState) {
                                    PlayerInternalState.LYRICS -> PlayerInternalState.COVER
                                    PlayerInternalState.LYRICS_PREVIEW -> PlayerInternalState.LYRICS
                                    else -> PlayerInternalState.LYRICS_PREVIEW
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lyrics),
                                contentDescription = stringResource(R.string.lyrics),
                                tint = if (isLyricsActive) adaptivePrimary else adaptiveSecondary,
                                modifier = Modifier.size(if (minimalPlayerDesign) 24.dp else 28.dp)
                            )
                        }

                        Box(contentAlignment = Alignment.Center) {
                            IconButton(
                                onClick = { showAudioDeviceBottomSheet = true },
                                modifier = Modifier.background(Color.Transparent, RoundedCornerShape(12.dp))
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.volume_up),
                                    contentDescription = stringResource(R.string.audio_output),
                                    tint = adaptiveSecondary,
                                    modifier = Modifier.size(if (minimalPlayerDesign) 24.dp else 28.dp)
                                )
                            }
                            if (bluetoothDeviceName != null) {
                                Text(
                                    text = bluetoothDeviceName!!,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = adaptiveSecondary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(max = 84.dp)
                                )
                            }
                        }

                        val isQueueActive = playerState == PlayerInternalState.QUEUE || playerState == PlayerInternalState.QUEUE_PREVIEW
                        IconButton(
                            onClick = {
                                playerState = when (playerState) {
                                    PlayerInternalState.QUEUE -> PlayerInternalState.COVER
                                    PlayerInternalState.QUEUE_PREVIEW -> PlayerInternalState.QUEUE
                                    else -> PlayerInternalState.QUEUE_PREVIEW
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = stringResource(R.string.queue),
                                tint = if (isQueueActive) adaptivePrimary else adaptiveSecondary,
                                modifier = Modifier.size(if (minimalPlayerDesign) 24.dp else 28.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showAudioDeviceBottomSheet) {
            AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
        }
    }
}
