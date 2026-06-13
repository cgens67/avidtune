@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.player

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.constants.MinimalPlayerDesignKey
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.PlayerButtonsStyle
import com.cgens67.avidtune.constants.PlayerButtonsStyleKey
import com.cgens67.avidtune.constants.PlayerHorizontalPadding
import com.cgens67.avidtune.constants.PlayerTextAlignmentKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.QueuePeekHeight
import com.cgens67.avidtune.constants.SliderStyle
import com.cgens67.avidtune.constants.SliderStyleKey
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.extensions.toggleRepeatMode
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.component.BottomSheet
import com.cgens67.avidtune.ui.component.BottomSheetState
import com.cgens67.avidtune.ui.component.LocalBottomSheetPageState
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayerSliderTrack
import com.cgens67.avidtune.ui.component.rememberBottomSheetState
import com.cgens67.avidtune.ui.menu.PlayerMenu
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.screens.settings.PlayerTextAlignment
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    onOpenFullscreenLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current

    val clipboardManager = LocalClipboardManager.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

    val minimalPlayerDesign by rememberPreference(MinimalPlayerDesignKey, false)
    val disableBlur by rememberPreference(DisableBlurKey, false)

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val isDark = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            isDark && pureBlack
        }
        
    val backgroundColor = if (useBlackBackground && state.value > state.collapsedBound) {
        lerp(MaterialTheme.colorScheme.surfaceContainer, Color.Black, state.progress)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val currentSongLiked = currentSong?.song?.liked == true
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.SQUIGGLY)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    val blurRadius by animateDpAsState(
        targetValue = if (state.isExpanded && playerBackground == PlayerBackgroundStyle.BLUR) 150.dp else 0.dp,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "blurRadius"
    )

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (state.isExpanded && playerBackground != PlayerBackgroundStyle.DEFAULT) 1f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "backgroundAlpha"
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColorArgb = surfaceColor.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground, fallbackColorArgb) {
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.BLUR) {
            gradientColors = listOf(Color.Black, Color.Black)
        }
        if (useBlackBackground && playerBackground != PlayerBackgroundStyle.GRADIENT && playerBackground != PlayerBackgroundStyle.APPLE_MUSIC) {
            gradientColors = listOf(Color.Black, Color.Black)
        } else if (playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.APPLE_MUSIC) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    ImageLoader(context)
                        .execute(
                            ImageRequest
                                .Builder(context)
                                .data(mediaMetadata?.thumbnailUrl)
                                .allowHardware(false)
                                .build(),
                        ).drawable as? BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { bitmap ->
                    val palette = Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColorArgb
                    )

                    withContext(Dispatchers.Main) {
                        gradientColors = extractedColors
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val TextBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
            else -> Color.White
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            else -> Color.Black
        }

    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT -> Pair(TextBackgroundColor, icBackgroundColor)
        PlayerButtonsStyle.PRIMARY -> Pair(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        )
        PlayerButtonsStyle.TERTIARY -> Pair(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        )
    }

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata?.id ?: "")
        .collectAsState(initial = null)

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)

    if (showDetailsDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showDetailsDialog = false },
            containerColor = if (useBlackBackground) Color.Black else AlertDialogDefaults.containerColor,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDetailsDialog = false },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .sizeIn(minWidth = 280.dp, maxWidth = 560.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    listOf(
                        stringResource(R.string.song_title) to mediaMetadata?.title,
                        stringResource(R.string.song_artists) to mediaMetadata?.artists?.joinToString { it.name },
                        stringResource(R.string.media_id) to mediaMetadata?.id,
                        "Itag" to currentFormat?.itag?.toString(),
                        stringResource(R.string.mime_type) to currentFormat?.mimeType,
                        stringResource(R.string.codecs) to currentFormat?.codecs,
                        stringResource(R.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                        stringResource(R.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                        stringResource(R.string.loudness) to currentFormat?.loudnessDb?.let { "$it dB" },
                        stringResource(R.string.volume) to "${(playerConnection.service.playerVolume.value * 100).toInt()}%",
                        stringResource(R.string.file_size) to
                                currentFormat?.contentLength?.let {
                                    Formatter.formatShortFileSize(
                                        context,
                                        it
                                    )
                                },
                    ).forEach { (label, text) ->
                        val displayText = text ?: stringResource(R.string.unknown)
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.titleMedium,
                            modifier =
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(displayText))
                                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            },
        )
    }

    val queueSheetState =
        rememberBottomSheetState(
            dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues()
                .calculateBottomPadding(),
            expandedBound = state.expandedBound,
        )

    val bottomSheetBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT ->
            MaterialTheme.colorScheme.surfaceContainer
        else ->
            if (useBlackBackground) Color.Black
            else MaterialTheme.colorScheme.surfaceContainer
    }

    val queueOnBgColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val isLoading = playbackState == Player.STATE_BUFFERING

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bottomSheetBackgroundColor)
            ) {
                PlayerBackground(
                    playerBackground = playerBackground,
                    mediaMetadata = mediaMetadata,
                    gradientColors = gradientColors,
                    backgroundAlpha = backgroundAlpha,
                    disableBlur = disableBlur
                )
            }
        },
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration,
            )
        },
    ) {
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(top = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        val screenWidth = LocalConfiguration.current.screenWidthDp
                        val thumbnailSize = (screenWidth * 0.4).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                modifier = Modifier.size(thumbnailSize),
                                isPlayerExpanded = state.isExpanded
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier =
                            Modifier
                                .weight(1f)
                                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)),
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            PlayerTitleSection(
                                mediaMetadata = it,
                                textBackgroundColor = TextBackgroundColor,
                                navController = navController,
                                state = state
                            )
                            Spacer(Modifier.height(12.dp))
                            
                            if (minimalPlayerDesign) {
                                PlayerTopActionsV3(
                                    mediaMetadata = it,
                                    textBackgroundColor = TextBackgroundColor,
                                    currentSongLiked = currentSongLiked,
                                    context = context,
                                    playerConnection = playerConnection,
                                    onMoreOptions = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = it,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                )
                            } else {
                                PlayerTopActionsV4(
                                    mediaMetadata = it,
                                    textBackgroundColor = TextBackgroundColor,
                                    currentSongLiked = currentSongLiked,
                                    onShare = {
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.youtube.com/watch?v=${it.id}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    onToggleLike = { playerConnection.toggleLike() },
                                    onMoreOptions = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = it,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            PlayerSliderV4(
                                sliderStyle = sliderStyle,
                                sliderPosition = sliderPosition,
                                position = position,
                                duration = duration,
                                isPlaying = isPlaying,
                                textBackgroundColor = TextBackgroundColor,
                                onValueChange = { sliderPosition = it },
                                onValueChangeFinished = {
                                    sliderPosition?.let { pos ->
                                        playerConnection.player.seekTo(pos)
                                        position = pos
                                    }
                                    sliderPosition = null
                                }
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            
                            PlayerTimeLabelV4(
                                sliderPosition = sliderPosition,
                                position = position,
                                duration = duration,
                                textBackgroundColor = TextBackgroundColor
                            )
                            
                            Spacer(Modifier.height(12.dp))
                            
                            if (minimalPlayerDesign) {
                                PlayerPlaybackControlsV3(
                                    playbackState = playbackState,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    repeatMode = repeatMode,
                                    canSkipPrevious = canSkipPrevious,
                                    canSkipNext = canSkipNext,
                                    textBackgroundColor = TextBackgroundColor,
                                    icBackgroundColor = icBackgroundColor,
                                    playerConnection = playerConnection,
                                    shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
                                )
                            } else {
                                PlayerPlaybackControlsV4(
                                    playbackState = playbackState,
                                    isPlaying = isPlaying,
                                    isLoading = isLoading,
                                    repeatMode = repeatMode,
                                    canSkipPrevious = canSkipPrevious,
                                    canSkipNext = canSkipNext,
                                    textButtonColor = textButtonColor,
                                    iconButtonColor = iconButtonColor,
                                    textBackgroundColor = TextBackgroundColor,
                                    icBackgroundColor = icBackgroundColor,
                                    playerConnection = playerConnection,
                                    shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
                                )
                            }
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier =
                        Modifier
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                            .padding(bottom = queueSheetState.collapsedBound),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection)
                        ) {
                            Thumbnail(
                                sliderPositionProvider = { sliderPosition },
                                onOpenFullscreenLyrics = onOpenFullscreenLyrics,
                                isPlayerExpanded = state.isExpanded
                            )
                        }
                    }

                    mediaMetadata?.let {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PlayerHorizontalPadding),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                PlayerTitleSection(
                                    mediaMetadata = it,
                                    textBackgroundColor = TextBackgroundColor,
                                    navController = navController,
                                    state = state
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            if (minimalPlayerDesign) {
                                PlayerTopActionsV3(
                                    mediaMetadata = it,
                                    textBackgroundColor = TextBackgroundColor,
                                    currentSongLiked = currentSongLiked,
                                    context = context,
                                    playerConnection = playerConnection,
                                    onMoreOptions = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = it,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                )
                            } else {
                                PlayerTopActionsV4(
                                    mediaMetadata = it,
                                    textBackgroundColor = TextBackgroundColor,
                                    currentSongLiked = currentSongLiked,
                                    onShare = {
                                        val intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "https://music.youtube.com/watch?v=${it.id}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    onToggleLike = { playerConnection.toggleLike() },
                                    onMoreOptions = {
                                        menuState.show {
                                            PlayerMenu(
                                                mediaMetadata = it,
                                                navController = navController,
                                                playerBottomSheetState = state,
                                                onShowDetailsDialog = { showDetailsDialog = true },
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        PlayerSliderV4(
                            sliderStyle = sliderStyle,
                            sliderPosition = sliderPosition,
                            position = position,
                            duration = duration,
                            isPlaying = isPlaying,
                            textBackgroundColor = TextBackgroundColor,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                sliderPosition?.let { pos ->
                                    playerConnection.player.seekTo(pos)
                                    position = pos
                                }
                                sliderPosition = null
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        PlayerTimeLabelV4(
                            sliderPosition = sliderPosition,
                            position = position,
                            duration = duration,
                            textBackgroundColor = TextBackgroundColor
                        )

                        Spacer(Modifier.height(12.dp))

                        if (minimalPlayerDesign) {
                            PlayerPlaybackControlsV3(
                                playbackState = playbackState,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                repeatMode = repeatMode,
                                canSkipPrevious = canSkipPrevious,
                                canSkipNext = canSkipNext,
                                textBackgroundColor = TextBackgroundColor,
                                icBackgroundColor = icBackgroundColor,
                                playerConnection = playerConnection,
                                shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
                            )
                        } else {
                            PlayerPlaybackControlsV4(
                                playbackState = playbackState,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                repeatMode = repeatMode,
                                canSkipPrevious = canSkipPrevious,
                                canSkipNext = canSkipNext,
                                textButtonColor = textButtonColor,
                                iconButtonColor = iconButtonColor,
                                textBackgroundColor = TextBackgroundColor,
                                icBackgroundColor = icBackgroundColor,
                                playerConnection = playerConnection,
                                shuffleModeEnabled = playerConnection.player.shuffleModeEnabled
                            )
                        }
                    }

                    Spacer(Modifier.height(30.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor =
                if (useBlackBackground) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
            onBackgroundColor = queueOnBgColor,
            textBackgroundColor = TextBackgroundColor,
        )
    }
}

@Composable
fun PlayerBackground(
    playerBackground: PlayerBackgroundStyle,
    mediaMetadata: MediaMetadata?,
    gradientColors: List<Color>,
    backgroundAlpha: Float,
    disableBlur: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (playerBackground) {
            PlayerBackgroundStyle.BLUR -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                    },
                    label = "blurBackground"
                ) { thumbnailUrl ->
                    if (thumbnailUrl != null) {
                        val useDarkTheme = isSystemInDarkTheme()
                        Box(modifier = Modifier.alpha(backgroundAlpha)) {
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = "Blurred background",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (!disableBlur) it.blur(if (useDarkTheme) 150.dp else 100.dp) else it }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
            }
            PlayerBackgroundStyle.GRADIENT -> {
                AnimatedContent(
                    targetState = gradientColors,
                    transitionSpec = {
                        fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                    },
                    label = "gradientBackground"
                ) { colors ->
                    if (colors.isNotEmpty()) {
                        val gradientColorStops = if (colors.size >= 3) {
                            arrayOf(
                                0.0f to colors[0],
                                0.5f to colors[1],
                                1.0f to colors[2]
                            )
                        } else {
                            arrayOf(
                                0.0f to colors[0],
                                0.6f to colors[0].copy(alpha = 0.7f),
                                1.0f to Color.Black
                            )
                        }
                        Box(
                            Modifier
                                .fillMaxSize()
                                .alpha(backgroundAlpha)
                                .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                .background(Color.Black.copy(alpha = 0.2f))
                        )
                    }
                }
            }
            PlayerBackgroundStyle.APPLE_MUSIC -> {
                AnimatedContent(
                    targetState = mediaMetadata?.thumbnailUrl,
                    transitionSpec = {
                        fadeIn(tween(800)) togetherWith fadeOut(tween(800))
                    },
                    label = "appleMusicBackground",
                    modifier = Modifier.graphicsLayer(alpha = backgroundAlpha)
                ) { thumbnailUrl ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .let { if (!disableBlur) it.blur(80.dp) else it }
                                .graphicsLayer(
                                    alpha = 1f, 
                                    clip = true,
                                    compositingStrategy = CompositingStrategy.Offscreen
                                )
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.4f to Color.Transparent,
                                            0.6f to Color.Black
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.3f),
                                            Color.Black.copy(alpha = 0.7f)
                                        ),
                                        startY = 0.4f
                                    )
                                )
                        )
                    }
                }
            }
            else -> {
                // DEFAULT
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderV4(
    sliderStyle: SliderStyle,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    isPlaying: Boolean,
    textBackgroundColor: Color,
    onValueChange: (Long) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    when (sliderStyle) {
        SliderStyle.DEFAULT -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                colors = SliderDefaults.colors(
                    activeTrackColor = textBackgroundColor,
                    inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                    thumbColor = textBackgroundColor
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
            )
        }
        SliderStyle.SQUIGGLY -> {
            SquigglySlider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                colors = SliderDefaults.colors(
                    activeTrackColor = textBackgroundColor,
                    inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                    thumbColor = textBackgroundColor
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding),
                squigglesSpec = SquigglySlider.SquigglesSpec(
                    amplitude = if (isPlaying) (4.dp).coerceAtLeast(2.dp) else 0.dp,
                    strokeWidth = 3.dp,
                    wavelength = 36.dp,
                ),
            )
        }
        SliderStyle.SLIM -> {
            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = { onValueChange(it.toLong()) },
                onValueChangeFinished = onValueChangeFinished,
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            activeTrackColor = textBackgroundColor,
                            inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                            thumbColor = textBackgroundColor
                        )
                    )
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = textBackgroundColor,
                    inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                    thumbColor = textBackgroundColor
                ),
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )
        }
    }
}

@Composable
fun PlayerTimeLabelV4(
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    textBackgroundColor: Color,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding + 4.dp),
    ) {
        Text(
            text = makeTimeString(sliderPosition ?: position),
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
            style = MaterialTheme.typography.labelMedium,
            color = textBackgroundColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerTitleSection(
    mediaMetadata: MediaMetadata,
    textBackgroundColor: Color,
    navController: NavController,
    state: BottomSheetState,
) {
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val context = LocalContext.current
    
    AnimatedContent(
        targetState = mediaMetadata.title,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "",
    ) { title ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = textBackgroundColor,
            modifier = Modifier
                .basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp)
                .combinedClickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        if (mediaMetadata.album != null) {
                            state.collapseSoft()
                            navController.navigate("album/${mediaMetadata.album.id}")
                        }
                    },
                    onLongClick = {
                        clipboardManager.setText(AnnotatedString(title))
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                ),
        )
    }

    Spacer(Modifier.height(6.dp))

    val annotatedString = buildAnnotatedString {
        mediaMetadata.artists.forEachIndexed { index, artist ->
            val tag = "artist_${artist.id.orEmpty()}"
            pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty())
            withStyle(SpanStyle(color = textBackgroundColor, fontSize = 16.sp)) {
                append(artist.name)
            }
            pop()
            if (index != mediaMetadata.artists.lastIndex) append(", ")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .basicMarquee()
            .padding(end = 12.dp)
    ) {
        var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        var clickOffset by remember { mutableStateOf<Offset?>(null) }
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.titleMedium.copy(color = textBackgroundColor),
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
                .combinedClickable(
                    enabled = true,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        val tapPosition = clickOffset
                        val layout = layoutResult
                        if (tapPosition != null && layout != null) {
                            val offset = layout.getOffsetForPosition(tapPosition)
                            annotatedString.getStringAnnotations(offset, offset)
                                .firstOrNull()
                                ?.let { ann ->
                                    val artistId = ann.item
                                    if (artistId.isNotBlank()) {
                                        navController.navigate("artist/$artistId")
                                        state.collapseSoft()
                                    }
                                }
                        }
                    },
                    onLongClick = {
                        clipboardManager.setText(annotatedString)
                        Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                    }
                )
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerTopActionsV4(
    mediaMetadata: MediaMetadata,
    textBackgroundColor: Color,
    currentSongLiked: Boolean,
    onShare: () -> Unit,
    onToggleLike: () -> Unit,
    onMoreOptions: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onShare,
            shape = RoundedCornerShape(14.dp),
            color = textBackgroundColor.copy(alpha = 0.12f),
            modifier = Modifier
                .height(44.dp)
                .width(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = null,
                    tint = textBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Surface(
            onClick = onToggleLike,
            shape = RoundedCornerShape(14.dp),
            color = if (currentSongLiked)
                MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
            else textBackgroundColor.copy(alpha = 0.12f),
            modifier = Modifier
                .height(44.dp)
                .width(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(
                        if (currentSongLiked) R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint = if (currentSongLiked)
                        MaterialTheme.colorScheme.error
                    else textBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // More menu button - cinematic glass card
        Surface(
            onClick = onMoreOptions,
            shape = RoundedCornerShape(14.dp),
            color = textBackgroundColor.copy(alpha = 0.12f),
            modifier = Modifier
                .height(44.dp)
                .width(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.more_horiz),
                    contentDescription = null,
                    tint = textBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlayerPlaybackControlsV4(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textButtonColor: Color,
    iconButtonColor: Color,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    playerConnection: PlayerConnection,
    shuffleModeEnabled: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        val baseLarge = 56.dp
        val baseSmall = 46.dp
        val baseGap = 12.dp
        val baseLargeIcon = 28.dp
        val baseSmallIcon = 22.dp
        val baseLargeRadius = 18.dp
        val baseSmallRadius = 16.dp
        val centerSize = 88.dp
        val centerPadding = 40.dp
        val sideTotal = (maxWidth - centerSize - centerPadding) / 2f
        val scale =
            ((sideTotal - baseGap) / (baseLarge + baseSmall)).coerceAtMost(1f).coerceAtLeast(0.6f)
        val large = baseLarge * scale
        val small = baseSmall * scale
        val gap = baseGap * scale
        val largeIcon = baseLargeIcon * scale
        val smallIcon = baseSmallIcon * scale
        val largeRadius = baseLargeRadius * scale
        val smallRadius = baseSmallRadius * scale

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = {
                        playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                    },
                    shape = RoundedCornerShape(smallRadius),
                    color = textBackgroundColor.copy(
                        alpha = if (shuffleModeEnabled) 0.2f else 0.08f
                    ),
                    modifier = Modifier.size(small)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (shuffleModeEnabled) 1f else 0.6f
                            ),
                            modifier = Modifier.size(smallIcon)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(gap))

                Surface(
                    onClick = { playerConnection.seekToPrevious() },
                    enabled = canSkipPrevious,
                    shape = RoundedCornerShape(largeRadius),
                    color = textBackgroundColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(large)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipPrevious) 1f else 0.4f
                            ),
                            modifier = Modifier.size(largeIcon)
                        )
                    }
                }
            }

            Surface(
                onClick = {
                    if (playbackState == STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
                shape = RoundedCornerShape(28.dp),
                color = textButtonColor,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .size(88.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularWavyProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = iconButtonColor,
                        )
                    } else {
                        Icon(
                            painter = painterResource(
                                when {
                                    playbackState == STATE_ENDED -> R.drawable.replay
                                    isPlaying -> R.drawable.pause
                                    else -> R.drawable.play
                                }
                            ),
                            contentDescription = null,
                            tint = iconButtonColor,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { playerConnection.seekToNext() },
                    enabled = canSkipNext,
                    shape = RoundedCornerShape(largeRadius),
                    color = textBackgroundColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(large)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = textBackgroundColor.copy(
                                alpha = if (canSkipNext) 1f else 0.4f
                            ),
                            modifier = Modifier.size(largeIcon)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(gap))

                Surface(
                    onClick = { playerConnection.player.toggleRepeatMode() },
                    shape = RoundedCornerShape(smallRadius),
                    color = textBackgroundColor.copy(
                        alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.2f else 0.08f
                    ),
                    modifier = Modifier.size(small)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
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
                            tint = textBackgroundColor.copy(
                                alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.6f else 1f
                            ),
                            modifier = Modifier.size(smallIcon)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerTopActionsV3(
    mediaMetadata: MediaMetadata,
    textBackgroundColor: Color,
    currentSongLiked: Boolean,
    context: Context,
    playerConnection: PlayerConnection,
    onMoreOptions: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.share),
                contentDescription = null,
                tint = textBackgroundColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { playerConnection.toggleLike() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (currentSongLiked) R.drawable.favorite
                    else R.drawable.favorite_border
                ),
                contentDescription = null,
                tint = if (currentSongLiked)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                else textBackgroundColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onMoreOptions() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription = null,
                tint = textBackgroundColor.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PlayerPlaybackControlsV3(
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    repeatMode: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    textBackgroundColor: Color,
    icBackgroundColor: Color,
    playerConnection: PlayerConnection,
    shuffleModeEnabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerHorizontalPadding)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(
                        alpha = if (shuffleModeEnabled) 1f else 0.4f
                    ),
                    modifier = Modifier.size(22.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(textBackgroundColor.copy(alpha = 0.08f))
                    .clickable(enabled = canSkipPrevious) {
                        playerConnection.seekToPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_previous),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 0.9f else 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(50))
                    .background(textBackgroundColor)
                    .clickable {
                        if (playbackState == STATE_ENDED) {
                            playerConnection.player.seekTo(0, 0)
                            playerConnection.player.playWhenReady = true
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = icBackgroundColor,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            when {
                                playbackState == STATE_ENDED -> R.drawable.replay
                                isPlaying -> R.drawable.pause
                                else -> R.drawable.play
                            }
                        ),
                        contentDescription = null,
                        tint = icBackgroundColor,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(textBackgroundColor.copy(alpha = 0.08f))
                    .clickable(enabled = canSkipNext) {
                        playerConnection.seekToNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(alpha = if (canSkipNext) 0.9f else 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { playerConnection.player.toggleRepeatMode() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        when (repeatMode) {
                            Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL -> R.drawable.repeat
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                            else -> R.drawable.repeat
                        }
                    ),
                    contentDescription = null,
                    tint = textBackgroundColor.copy(
                        alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.4f else 1f
                    ),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
