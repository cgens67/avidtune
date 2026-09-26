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
import android.content.res.Configuration
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.media3.common.Player
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
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.extensions.toggleRepeatMode
import com.cgens67.avidtune.models.MediaMetadata
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

enum class PlayerInternalState { COVER, LYRICS, QUEUE }

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

@Composable
fun PlayerV2Slider(
    sliderStyle: SliderStyle,
    currentPos: Long,
    duration: Long,
    isPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    adaptivePrimary: Color,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                        onValueChange(value.toLong())
                    }
                },
                onValueChangeFinished = {
                    if (!isListenTogetherGuest) {
                        onValueChangeFinished()
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
                            useDarkTheme = useDarkTheme
                        )
                    )
                },
                modifier = modifier.fillMaxWidth()
            )
        }
        SliderStyle.DEFAULT -> {
            Slider(
                value = currentPos.toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { value ->
                    if (!isListenTogetherGuest) onValueChange(value.toLong())
                },
                onValueChangeFinished = {
                    if (!isListenTogetherGuest) onValueChangeFinished()
                },
                enabled = !isListenTogetherGuest,
                colors = SliderDefaults.colors(
                    activeTrackColor = adaptivePrimary,
                    inactiveTrackColor = adaptivePrimary.copy(alpha = 0.25f),
                    thumbColor = adaptivePrimary
                ),
                modifier = modifier.fillMaxWidth()
            )
        }
        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = currentPos.toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { value ->
                    if (!isListenTogetherGuest) onValueChange(value.toLong())
                },
                onValueChangeFinished = {
                    if (!isListenTogetherGuest) onValueChangeFinished()
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
                modifier = modifier.fillMaxWidth()
            )
        }
        SliderStyle.SLIM -> {
            Slider(
                value = currentPos.toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { value ->
                    if (!isListenTogetherGuest) onValueChange(value.toLong())
                },
                onValueChangeFinished = {
                    if (!isListenTogetherGuest) onValueChangeFinished()
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
                modifier = modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlayerV2(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null)
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    var playerState by remember { mutableStateOf(PlayerInternalState.COVER) }

    val togetherSessionState by playerConnection.service.togetherSessionState.collectAsState()
    val isListenTogetherGuest = (togetherSessionState as? TogetherSessionState.Joined)?.role is TogetherRole.Guest
    val isMuted by playerConnection.isMuted.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val playerVolume by playerConnection.service.playerVolume.collectAsState()

    BackHandler(enabled = playerState != PlayerInternalState.COVER) {
        playerState = PlayerInternalState.COVER
    }

    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(playerState) {
        if (playerState != PlayerInternalState.LYRICS) {
            controlsVisible = true
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

    if (isLandscape) {
        PlayerV2Landscape(
            state = state,
            navController = navController,
            mediaMetadata = mediaMetadata,
            currentSong = currentSong,
            currentLyrics = currentLyrics,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            sliderPosition = sliderPosition,
            onSliderPositionChange = { sliderPosition = it },
            onSeekTo = { pos ->
                playerConnection.player.seekTo(pos)
                position = pos
                sliderPosition = null
            },
            playerState = playerState,
            onPlayerStateChange = { playerState = it },
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            isListenTogetherGuest = isListenTogetherGuest,
            isMuted = isMuted,
            playerVolume = playerVolume,
            sliderStyle = sliderStyle,
            playerBackground = playerBackground,
            thumbnailCornerRadius = thumbnailCornerRadius,
            coverResolution = coverResolution,
            showPlayerThumbnailShadow = showPlayerThumbnailShadow,
            playerThumbnailShadowElevation = playerThumbnailShadowElevation,
            hidePlayerThumbnail = hidePlayerThumbnail,
            minimalPlayerDesign = minimalPlayerDesign,
            bluetoothDeviceName = bluetoothDeviceName,
            onShowAudioDeviceBottomSheet = { showAudioDeviceBottomSheet = true },
            adaptivePrimary = adaptivePrimary,
            adaptiveSecondary = adaptiveSecondary,
            adaptiveSurface = adaptiveSurface,
            modifier = modifier
        )
    } else {
        PlayerV2Portrait(
            state = state,
            navController = navController,
            mediaMetadata = mediaMetadata,
            currentSong = currentSong,
            currentLyrics = currentLyrics,
            isPlaying = isPlaying,
            position = position,
            duration = duration,
            sliderPosition = sliderPosition,
            onSliderPositionChange = { sliderPosition = it },
            onSeekTo = { pos ->
                playerConnection.player.seekTo(pos)
                position = pos
                sliderPosition = null
            },
            playerState = playerState,
            onPlayerStateChange = { playerState = it },
            controlsVisible = controlsVisible,
            onToggleControlsVisible = { controlsVisible = !controlsVisible },
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            isListenTogetherGuest = isListenTogetherGuest,
            isMuted = isMuted,
            playerVolume = playerVolume,
            sliderStyle = sliderStyle,
            playerBackground = playerBackground,
            thumbnailCornerRadius = thumbnailCornerRadius,
            coverResolution = coverResolution,
            showPlayerThumbnailShadow = showPlayerThumbnailShadow,
            playerThumbnailShadowElevation = playerThumbnailShadowElevation,
            hidePlayerThumbnail = hidePlayerThumbnail,
            minimalPlayerDesign = minimalPlayerDesign,
            bluetoothDeviceName = bluetoothDeviceName,
            onShowAudioDeviceBottomSheet = { showAudioDeviceBottomSheet = true },
            adaptivePrimary = adaptivePrimary,
            adaptiveSecondary = adaptiveSecondary,
            adaptiveSurface = adaptiveSurface,
            modifier = modifier
        )
    }

    if (showAudioDeviceBottomSheet) {
        AudioDeviceBottomSheet(onDismiss = { showAudioDeviceBottomSheet = false })
    }
}

@Composable
private fun PlayerV2Landscape(
    state: BottomSheetState,
    navController: NavController,
    mediaMetadata: MediaMetadata?,
    currentSong: Song?,
    currentLyrics: LyricsEntity?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    sliderPosition: Long?,
    onSliderPositionChange: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    playerState: PlayerInternalState,
    onPlayerStateChange: (PlayerInternalState) -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    playerVolume: Float,
    sliderStyle: SliderStyle,
    playerBackground: PlayerBackgroundStyle,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    showPlayerThumbnailShadow: Boolean,
    playerThumbnailShadowElevation: Float,
    hidePlayerThumbnail: Boolean,
    minimalPlayerDesign: Boolean,
    bluetoothDeviceName: String?,
    onShowAudioDeviceBottomSheet: () -> Unit,
    adaptivePrimary: Color,
    adaptiveSecondary: Color,
    adaptiveSurface: Color,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val coverRadius = thumbnailCornerRadius.dp
    val isAppleMusicBg = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC

    val isLiked = currentSong?.song?.liked == true
    val currentPos = sliderPosition ?: position

    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    Row(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (playerState == PlayerInternalState.COVER) {
            // Left Side: Hero Album Artwork
            Box(
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.88f)
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
                        .clickable(enabled = isAppleMusicBg) { onPlayerStateChange(PlayerInternalState.LYRICS) }
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
                }
            }

            // Right Side: Title, Actions, Slider, Controls, Utility Bar
            Column(
                modifier = Modifier
                    .weight(1.05f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar in Right Pane
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
                            tint = adaptivePrimary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = playerConnection::toggleLike,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                                contentDescription = null,
                                tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

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
                    }
                }

                // Title and Artist Info
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = mediaMetadata?.title ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = adaptivePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee(iterations = 1, initialDelayMillis = 2000, velocity = 30.dp)
                            .clickable {
                                state.collapseSoft()
                                mediaMetadata?.album?.id?.let { navController.navigate("album/$it") }
                            }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = mediaMetadata?.artists?.firstOrNull()?.name ?: stringResource(R.string.unknown),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = adaptiveSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .basicMarquee(iterations = 1, initialDelayMillis = 2000, velocity = 30.dp)
                            .clickable {
                                state.collapseSoft()
                                mediaMetadata?.artists?.firstOrNull()?.id?.let { navController.navigate("artist/$it") }
                            }
                    )
                }

                // Slider & Time
                Column(modifier = Modifier.fillMaxWidth()) {
                    PlayerV2Slider(
                        sliderStyle = sliderStyle,
                        currentPos = currentPos,
                        duration = duration,
                        isPlaying = isPlaying,
                        isListenTogetherGuest = isListenTogetherGuest,
                        adaptivePrimary = adaptivePrimary,
                        playerBackground = playerBackground,
                        useDarkTheme = isSystemInDarkTheme(),
                        onValueChange = onSliderPositionChange,
                        onValueChangeFinished = {
                            sliderPosition?.let { onSeekTo(it) }
                        }
                    )
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
                }

                // 5-Button Transport Controls: Shuffle, Previous, Play/Pause, Next, Repeat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled },
                        enabled = !isListenTogetherGuest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = stringResource(R.string.shuffle),
                            tint = if (shuffleModeEnabled) adaptivePrimary else adaptiveSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (!isListenTogetherGuest && canSkipPrevious) {
                                playerConnection.player.seekToPrevious()
                            }
                        },
                        enabled = !isListenTogetherGuest && canSkipPrevious,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = if (canSkipPrevious) adaptivePrimary else adaptivePrimary.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp)
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
                        color = adaptivePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (isListenTogetherGuest) {
                                Icon(
                                    painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                    contentDescription = stringResource(R.string.volume),
                                    modifier = Modifier.size(28.dp),
                                    tint = adaptivePrimary
                                )
                            } else {
                                Icon(
                                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                    contentDescription = stringResource(if (isPlaying) R.string.media3_controls_pause_description else R.string.play),
                                    modifier = Modifier.size(32.dp),
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
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = if (canSkipNext) adaptivePrimary else adaptivePrimary.copy(alpha = 0.35f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.player.toggleRepeatMode() },
                        enabled = !isListenTogetherGuest,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                    else -> R.drawable.repeat
                                }
                            ),
                            contentDescription = null,
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) adaptivePrimary else adaptiveSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Volume slider row (if not minimal design)
                if (!minimalPlayerDesign) {
                    var volumeSliderPosition by remember { mutableStateOf<Float?>(null) }
                    val currentEffectiveVolume = volumeSliderPosition ?: (if (isMuted) 0f else playerVolume)

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_mute),
                            contentDescription = stringResource(R.string.volume_down),
                            tint = adaptiveSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    volumeSliderPosition = null
                                    playerConnection.toggleMute()
                                }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Slider(
                            value = currentEffectiveVolume.coerceIn(0f, 1f),
                            onValueChange = { newValue ->
                                volumeSliderPosition = newValue
                                playerConnection.setVolume(newValue)
                            },
                            onValueChangeFinished = { volumeSliderPosition = null },
                            valueRange = 0f..1f,
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            track = { sliderState ->
                                PlayerSliderTrack(
                                    sliderState = sliderState,
                                    trackHeight = 4.dp,
                                    colors = PlayerSliderColors.getSliderColors(
                                        activeColor = adaptivePrimary.copy(alpha = 0.8f),
                                        playerBackground = playerBackground,
                                        useDarkTheme = isSystemInDarkTheme()
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f).height(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = stringResource(R.string.volume_up),
                            tint = adaptiveSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    volumeSliderPosition = null
                                    playerConnection.setVolume(1f)
                                }
                        )
                    }
                }

                // Bottom utility bar: Lyrics, Device, Queue
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPlayerStateChange(PlayerInternalState.LYRICS) }) {
                        Icon(
                            painter = painterResource(R.drawable.lyrics),
                            contentDescription = stringResource(R.string.lyrics),
                            tint = adaptiveSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = onShowAudioDeviceBottomSheet) {
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = stringResource(R.string.audio_output),
                            tint = adaptiveSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(onClick = { onPlayerStateChange(PlayerInternalState.QUEUE) }) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = stringResource(R.string.queue),
                            tint = adaptiveSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        } else {
            // Split layout for LYRICS or QUEUE in landscape
            // Left Pane: Compact Album Art + Info + Playback controls
            Column(
                modifier = Modifier
                    .weight(0.85f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top row with close/return to cover and output device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onPlayerStateChange(PlayerInternalState.COVER) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.expand_more),
                            contentDescription = stringResource(R.string.back),
                            tint = adaptivePrimary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onShowAudioDeviceBottomSheet,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.volume_up),
                                contentDescription = null,
                                tint = adaptiveSecondary
                            )
                        }

                        IconButton(
                            onClick = {
                                onPlayerStateChange(
                                    if (playerState == PlayerInternalState.LYRICS) PlayerInternalState.QUEUE
                                    else PlayerInternalState.LYRICS
                                )
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playerState == PlayerInternalState.LYRICS) R.drawable.queue_music else R.drawable.lyrics
                                ),
                                contentDescription = null,
                                tint = adaptivePrimary
                            )
                        }
                    }
                }

                // Middle: Compact Cover & Song details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isAppleMusicBg) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(adaptiveSurface)
                                .clickable { onPlayerStateChange(PlayerInternalState.COVER) }
                        ) {
                            AsyncImage(
                                model = mediaMetadata?.thumbnailUrl?.resize(coverResolution.size, coverResolution.size),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata?.title ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = adaptivePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee()
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mediaMetadata?.artists?.firstOrNull()?.name ?: stringResource(R.string.unknown),
                            style = MaterialTheme.typography.bodySmall,
                            color = adaptiveSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = playerConnection::toggleLike,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                            contentDescription = null,
                            tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Slider & Time
                Column(modifier = Modifier.fillMaxWidth()) {
                    PlayerV2Slider(
                        sliderStyle = sliderStyle,
                        currentPos = currentPos,
                        duration = duration,
                        isPlaying = isPlaying,
                        isListenTogetherGuest = isListenTogetherGuest,
                        adaptivePrimary = adaptivePrimary,
                        playerBackground = playerBackground,
                        useDarkTheme = isSystemInDarkTheme(),
                        onValueChange = onSliderPositionChange,
                        onValueChangeFinished = {
                            sliderPosition?.let { onSeekTo(it) }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = makeTimeString(currentPos),
                            color = adaptiveSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "-" + makeTimeString(maxOf(0L, duration - currentPos)),
                            color = adaptiveSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Compact Transport Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (!isListenTogetherGuest && canSkipPrevious) {
                                playerConnection.player.seekToPrevious()
                            }
                        },
                        enabled = !isListenTogetherGuest && canSkipPrevious,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = adaptivePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            if (isListenTogetherGuest) playerConnection.toggleMute()
                            else playerConnection.player.togglePlayPause()
                        },
                        shape = CircleShape,
                        color = adaptivePrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                painter = painterResource(
                                    if (isListenTogetherGuest) {
                                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                    } else {
                                        if (isPlaying) R.drawable.pause else R.drawable.play
                                    }
                                ),
                                contentDescription = null,
                                tint = adaptivePrimary,
                                modifier = Modifier.size(28.dp)
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = adaptivePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Right Pane: Lyrics or Queue taking full height
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(adaptiveSurface.copy(alpha = 0.12f))
            ) {
                if (playerState == PlayerInternalState.LYRICS) {
                    LyricsV2(
                        mediaMetadata = mediaMetadata,
                        showLyrics = true,
                        positionProvider = { sliderPosition ?: position },
                        textColor = adaptivePrimary
                    )
                } else if (playerState == PlayerInternalState.QUEUE) {
                    QueueV2(
                        navController = navController,
                        playerBottomSheetState = state,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerV2Portrait(
    state: BottomSheetState,
    navController: NavController,
    mediaMetadata: MediaMetadata?,
    currentSong: Song?,
    currentLyrics: LyricsEntity?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    sliderPosition: Long?,
    onSliderPositionChange: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    playerState: PlayerInternalState,
    onPlayerStateChange: (PlayerInternalState) -> Unit,
    controlsVisible: Boolean,
    onToggleControlsVisible: () -> Unit,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    playerVolume: Float,
    sliderStyle: SliderStyle,
    playerBackground: PlayerBackgroundStyle,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    showPlayerThumbnailShadow: Boolean,
    playerThumbnailShadowElevation: Float,
    hidePlayerThumbnail: Boolean,
    minimalPlayerDesign: Boolean,
    bluetoothDeviceName: String?,
    onShowAudioDeviceBottomSheet: () -> Unit,
    adaptivePrimary: Color,
    adaptiveSecondary: Color,
    adaptiveSurface: Color,
    modifier: Modifier = Modifier
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val currentPos = sliderPosition ?: position

    val isLiked = currentSong?.song?.liked == true
    val coverRadius = thumbnailCornerRadius.dp
    val isAppleMusicBg = playerBackground == PlayerBackgroundStyle.APPLE_MUSIC

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.changes.any { it.pressed }) {
                            if (playerState == PlayerInternalState.COVER) {
                                if (!controlsVisible) onToggleControlsVisible()
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
                    if (targetState == PlayerInternalState.COVER) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp)
                        ) {
                            Spacer(modifier = Modifier.weight(1f))

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
                                    .clickable(enabled = isAppleMusicBg) { onPlayerStateChange(PlayerInternalState.LYRICS) }
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
                            }

                            Spacer(modifier = Modifier.weight(1f))

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
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = stringResource(R.string.more_options),
                                                    tint = adaptivePrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (targetState == PlayerInternalState.LYRICS || targetState == PlayerInternalState.QUEUE) {
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
                                            .clickable { onPlayerStateChange(PlayerInternalState.COVER) }
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
                                    IconButton(onClick = playerConnection::toggleLike) {
                                        Icon(
                                            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                                            contentDescription = if (isLiked) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library),
                                            tint = if (isLiked) MaterialTheme.colorScheme.error else adaptivePrimary,
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
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        LyricsV2(
                                            mediaMetadata = mediaMetadata,
                                            showLyrics = true,
                                            positionProvider = { currentPos },
                                            textColor = adaptivePrimary
                                        )

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(16.dp)
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(adaptivePrimary.copy(alpha = 0.15f))
                                                .clickable { onToggleControlsVisible() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(if (controlsVisible) R.drawable.expand_more else R.drawable.expand_less),
                                                contentDescription = "Toggle Fullscreen",
                                                tint = adaptivePrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                } else {
                                    QueueV2(
                                        navController = navController,
                                        playerBottomSheetState = state,
                                        modifier = Modifier.fillMaxSize(),
                                        onControlsVisibilityChange = { _ -> }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                        slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { it } +
                        expandVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                       slideOutVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } +
                       shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing))
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
                        PlayerV2Slider(
                            sliderStyle = sliderStyle,
                            currentPos = currentPos,
                            duration = duration,
                            isPlaying = isPlaying,
                            isListenTogetherGuest = isListenTogetherGuest,
                            adaptivePrimary = adaptivePrimary,
                            playerBackground = playerBackground,
                            useDarkTheme = isSystemInDarkTheme(),
                            onValueChange = onSliderPositionChange,
                            onValueChangeFinished = {
                                sliderPosition?.let { onSeekTo(it) }
                            }
                        )

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
                        val isLyricsActive = playerState == PlayerInternalState.LYRICS
                        IconButton(
                            onClick = {
                                onPlayerStateChange(if (isLyricsActive) PlayerInternalState.COVER else PlayerInternalState.LYRICS)
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
                                onClick = onShowAudioDeviceBottomSheet,
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
                                    text = bluetoothDeviceName,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = adaptiveSecondary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(max = 84.dp)
                                )
                            }
                        }

                        val isQueueActive = playerState == PlayerInternalState.QUEUE
                        IconButton(
                            onClick = {
                                onPlayerStateChange(if (isQueueActive) PlayerInternalState.COVER else PlayerInternalState.QUEUE)
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
    }
}
