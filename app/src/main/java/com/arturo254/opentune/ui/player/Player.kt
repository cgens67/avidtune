package com.arturo254.opentune.ui.player

import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
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
import coil.size.Precision
import com.arturo254.opentune.LocalDatabase
import com.arturo254.opentune.LocalDownloadUtil
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DefaultPlayPauseButtonShape
import com.arturo254.opentune.constants.DefaultSmallButtonsShape
import com.arturo254.opentune.constants.PlayPauseButtonShapeKey
import com.arturo254.opentune.constants.PlayerBackgroundStyle
import com.arturo254.opentune.constants.PlayerBackgroundStyleKey
import com.arturo254.opentune.constants.PlayerButtonsStyle
import com.arturo254.opentune.constants.PlayerButtonsStyleKey
import com.arturo254.opentune.constants.PlayerHorizontalPadding
import com.arturo254.opentune.constants.PlayerTextAlignmentKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.constants.QueuePeekHeight
import com.arturo254.opentune.constants.ShowLyricsKey
import com.arturo254.opentune.constants.SliderStyle
import com.arturo254.opentune.constants.SliderStyleKey
import com.arturo254.opentune.constants.SmallButtonsShapeKey
import com.arturo254.opentune.extensions.togglePlayPause
import com.arturo254.opentune.extensions.toggleRepeatMode
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.ui.component.BottomSheet
import com.arturo254.opentune.ui.component.BottomSheetState
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.component.PlayerSliderTrack
import com.arturo254.opentune.ui.component.ResizableIconButton
import com.arturo254.opentune.ui.component.rememberBottomSheetState
import com.arturo254.opentune.ui.menu.PlayerMenu
import com.arturo254.opentune.ui.screens.settings.DarkMode
import com.arturo254.opentune.ui.screens.settings.PlayerTextAlignment
import com.arturo254.opentune.ui.theme.PlayerColorExtractor
import com.arturo254.opentune.ui.theme.PlayerSliderColors
import com.arturo254.opentune.ui.theme.extractGradientColors
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.ui.utils.resize
import com.arturo254.opentune.utils.getPlayPauseShape
import com.arturo254.opentune.utils.getSmallButtonShape
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.utils.rememberEnumPreference
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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

    var showFullscreenLyrics by remember { mutableStateOf(false) }
    val playerConnection = LocalPlayerConnection.current ?: return

    val playerTextAlignment by rememberEnumPreference(
        PlayerTextAlignmentKey,
        PlayerTextAlignment.CENTER
    )

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
    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }
    val useBlackBackground =
        remember(isSystemInDarkTheme, darkTheme, pureBlack) {
            val useDarkTheme =
                if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
            useDarkTheme && pureBlack
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
    val automix by playerConnection.service.automixItems.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)
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

    var changeColor by remember {
        mutableStateOf(false)
    }

    // Animations for background effects
    var backgroundImageUrl by remember { mutableStateOf<String?>(null) }
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

    val overlayAlpha by animateFloatAsState(
        targetValue = when {
            !state.isExpanded -> 0f
            playerBackground == PlayerBackgroundStyle.BLUR -> 0.3f
            playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2 -> 0.2f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "overlayAlpha"
    )

    val playerButtonsStyle by rememberEnumPreference(
        key = PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )

    if (!canSkipNext && automix.isNotEmpty()) {
        playerConnection.service.addToQueueAutomix(automix[0], 0)
    }

    // Obtener el color del tema antes de LaunchedEffect
    val surfaceColor = MaterialTheme.colorScheme.surface
    val fallbackColorArgb = surfaceColor.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground, fallbackColorArgb) {
        // Update image URL for smooth transitions
        backgroundImageUrl = mediaMetadata?.thumbnailUrl?.resize(1200, 1200)

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
                                .data(mediaMetadata?.thumbnailUrl?.resize(1200, 1200))
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
            PlayerBackgroundStyle.BLUR -> Color.White
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.Black
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.White
                } else {
                    changeColor = false
                    Color.White
                }
            }
        }

    val icBackgroundColor =
        when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.surface
            PlayerBackgroundStyle.BLUR -> Color.Black
            else -> {
                val whiteContrast =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.first().toArgb(),
                            Color.White.toArgb(),
                        )
                    } else {
                        2.0
                    }
                val blackContrast: Double =
                    if (gradientColors.size >= 2) {
                        ColorUtils.calculateContrast(
                            gradientColors.last().toArgb(),
                            Color.Black.toArgb(),
                        )
                    } else {
                        2.0
                    }
                if (gradientColors.size >= 2 &&
                    whiteContrast < 2f &&
                    blackContrast > 2f
                ) {
                    changeColor = true
                    Color.White
                } else if (whiteContrast > 2f && blackContrast < 2f) {
                    changeColor = true
                    Color.Black
                } else {
                    changeColor = false
                    Color.Black
                }
            }
        }

    // Sistema de color scheme con PRIMARY y TERTIARY colors
    val (textButtonColor, iconButtonColor) = when (playerButtonsStyle) {
        PlayerButtonsStyle.DEFAULT ->
            if (useDarkTheme) Pair(Color.White, Color.Black)
            else Pair(Color.Black, Color.White)
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

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }
    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón para cancelar si hay temporizador activo
                    if (sleepTimerEnabled) {
                        TextButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.clear()
                            },
                        ) {
                            Text(stringResource(R.string.cancel_timer))
                        }
                    }

                    // Botón OK (cambia función según estado)
                    TextButton(
                        onClick = {
                            showSleepTimerDialog = false
                            if (sleepTimerEnabled) {
                                // Si ya hay timer activo, OK solo cierra el diálogo
                            } else {
                                // Si no hay timer, inicia uno nuevo
                                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                            }
                        },
                    ) {
                        Text(
                            text = if (sleepTimerEnabled)
                                stringResource(android.R.string.ok)
                            else
                                stringResource(R.string.start_timer)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Mostrar estado actual si hay timer activo
                    if (sleepTimerEnabled) {
                        Text(
                            text = stringResource(R.string.sleep_timer_active),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                                stringResource(R.string.end_of_song)
                            } else {
                                makeTimeString(sleepTimerTimeLeft)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Configuración del timer (mostrar solo si no hay timer activo)
                    if (!sleepTimerEnabled) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                        )

                        OutlinedButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.start(-1)
                            },
                        ) {
                            Text(stringResource(R.string.end_of_song))
                        }
                    }
                }
            },
        )
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl?.resize(600, 600),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            mediaMetadata.artists.joinToString { it.name },
                            makeTimeString(mediaMetadata.duration * 1000L),
                        ),
                )
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Botón para cancelar si hay temporizador activo
                    if (sleepTimerEnabled) {
                        TextButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.clear()
                            },
                        ) {
                            Text(stringResource(R.string.cancel_timer))
                        }
                    }

                    // Botón OK (cambia función según estado)
                    TextButton(
                        onClick = {
                            showSleepTimerDialog = false
                            if (sleepTimerEnabled) {
                                // Si ya hay timer activo, OK solo cierra el diálogo
                            } else {
                                // Si no hay timer, inicia uno nuevo
                                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                            }
                        },
                    ) {
                        Text(
                            text = if (sleepTimerEnabled)
                                stringResource(android.R.string.ok)
                            else
                                stringResource(R.string.start_timer)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Mostrar estado actual si hay timer activo
                    if (sleepTimerEnabled) {
                        Text(
                            text = stringResource(R.string.sleep_timer_active),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                                stringResource(R.string.end_of_song)
                            } else {
                                makeTimeString(sleepTimerTimeLeft)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Configuración del timer (mostrar solo si no hay timer activo)
                    if (!sleepTimerEnabled) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                        )

                        OutlinedButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.start(-1)
                            },
                        ) {
                            Text(stringResource(R.string.end_of_song))
                        }
                    }
                }
            },
        )
    }

    if (isQueueTrigger != true) {
        // State to track if audio is muted
        var isMuted by remember { mutableStateOf(false) }
        var previousVolume by remember { mutableFloatStateOf(playerVolume.value) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(
                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            isMuted = !isMuted
                            if (isMuted) {
                                previousVolume = playerVolume.value
                                playerConnection.service.playerVolume.value = 0f
                            } else {
                                playerConnection.service.playerVolume.value = previousVolume
                            }
                        },
                )

                Slider(
                    value = if (isMuted) 0f else playerVolume.value,
                    onValueChange = { volume ->
                        if (!isMuted) {
                            playerConnection.service.playerVolume.value = volume
                            previousVolume = volume
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                )
            }

            if (sleepTimerEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                            stringResource(R.string.end_of_song)
                        } else {
                            makeTimeString(sleepTimerTimeLeft)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.start_radio),
                        onClick = {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    WatchEndpoint(videoId = mediaMetadata.id),
                                    mediaMetadata
                                )
                            )
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            MenuGroup(
                items = buildList {
                    if (artists.isNotEmpty()) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.artist),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }
                    if (mediaMetadata.album != null) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.album.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            MenuGroup(
                items = listOf(
                    when (download?.state) {
                        androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                }
                            )
                        }

                        androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                        androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.downloading)) },
                                icon = {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                }
                            )
                        }

                        else -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        insert(mediaMetadata)
                                    }
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                }
                            )
                        }
                    }
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            MenuGroup(
                items = buildList {
                    if (librarySong?.song?.inLibrary != null) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_from_library)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.library_add_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.query {
                                        inLibrary(mediaMetadata.id, null)
                                    }
                                }
                            )
                        )
                    } else {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.add_to_library)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.library_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        insert(mediaMetadata)
                                        inLibrary(mediaMetadata.id, LocalDateTime.now())
                                    }
                                }
                            )
                        )
                    }

                    add(
                        MenuItemData(
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                onShowDetailsDialog()
                                onDismiss()
                            }
                        )
                    )

                    if (isQueueTrigger != true) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.sleep_timer)) },
                                description = {
                                    Text(
                                        text = if (sleepTimerEnabled) {
                                            if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                                                stringResource(R.string.end_of_song)
                                            } else {
                                                makeTimeString(sleepTimerTimeLeft)
                                            }
                                        } else {
                                            stringResource(R.string.sleep_timer_desc)
                                        }
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.bedtime),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (sleepTimerEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    showSleepTimerDialog = true
                                }
                            )
                        )
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.advanced)) },
                                description = { Text(text = stringResource(R.string.advanced_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.tune),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showPitchTempoDialog = true
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = R.drawable.speed,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                ValueAdjuster(
                    icon = R.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun <T> ValueAdjuster(
    @DrawableRes icon: Int,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = null,
            )
        }
    }
}