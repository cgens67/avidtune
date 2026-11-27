package com.cgens67.avidtune.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.AppFontKey
import com.cgens67.avidtune.constants.AppTextSize
import com.cgens67.avidtune.constants.AppTextSizeKey
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.UseSystemFontKey
import com.cgens67.avidtune.extensions.toEnum
import com.cgens67.avidtune.ui.component.LocaleManager
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.screens.settings.ThemePalettes
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.rememberEnumPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val localeContext = LocaleManager.getInstance(newBase).applyLocaleToContext(newBase)

        var appTextSizeStr = AppTextSize.SYSTEM.name
        try {
            val savedSize = runBlocking {
                localeContext.dataStore.data.first()[AppTextSizeKey]
            }
            if (savedSize != null) {
                appTextSizeStr = savedSize
            }
        } catch (_: Exception) {
        }

        val appTextSize = runCatching {
            AppTextSize.valueOf(appTextSizeStr)
        }.getOrDefault(AppTextSize.SYSTEM)

        val config = Configuration(localeContext.resources.configuration)
        if (appTextSize != AppTextSize.SYSTEM) {
            config.fontScale = when (appTextSize) {
                AppTextSize.SMALL -> 0.85f
                AppTextSize.MEDIUM -> 1.0f
                AppTextSize.LARGE -> 1.15f
                AppTextSize.EXTRA_LARGE -> 1.3f
                else -> config.fontScale
            }
        }

        super.attachBaseContext(localeContext.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val alarmId = intent.getStringExtra(AlarmManagerHelper.EXTRA_ALARM_ID)
        val initialAlarm = alarmId?.let { AlarmManagerHelper.getAlarm(this, it) }

        val darkThemePref = dataStore.get(DarkModeKey).toEnum(DarkMode.AUTO)
        val pureBlack = dataStore.get(PureBlackKey) ?: false
        val customThemeColor = dataStore.get(CustomThemeColorKey) ?: ThemePalettes.Default.id
        val useSystemFont = dataStore.get(UseSystemFontKey) ?: false
        val appFontStr = dataStore.get(AppFontKey) ?: ""
        val appFont = if (appFontStr.isNotEmpty()) {
            runCatching { AppFont.valueOf(appFontStr) }.getOrDefault(AppFont.SYSTEM)
        } else {
            if (useSystemFont) AppFont.SYSTEM else AppFont.SF_PRO
        }

        setContent {
            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (darkThemePref) {
                DarkMode.ON -> true
                DarkMode.OFF -> false
                DarkMode.AUTO -> isSystemDark
            }

            val decodedPalette = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)
            val seedPalette: ThemeSeedPalette
            val themeColor: Color
            if (decodedPalette != null) {
                seedPalette = decodedPalette
                themeColor = decodedPalette.primary
            } else {
                val p = ThemePalettes.findById(customThemeColor, useDarkTheme)
                    ?: ThemePalettes.findByPrimaryColor(customThemeColor, useDarkTheme)
                    ?: ThemePalettes.Default
                seedPalette = ThemeSeedPalette(p.primary, p.secondary, p.tertiary, p.neutral)
                themeColor = p.primary
            }

            AvidTuneTheme(
                darkTheme = useDarkTheme,
                pureBlack = pureBlack,
                themeColor = if (themeColor == ThemePalettes.Default.primary) DefaultThemeColor else themeColor,
                seedPalette = seedPalette,
                appFont = appFont
            ) {
                val activeInfo by AlarmService.activeAlarmState.collectAsState()
                var hasSeenActive by remember { mutableStateOf(activeInfo != null) }

                LaunchedEffect(activeInfo) {
                    if (activeInfo != null) {
                        hasSeenActive = true
                    } else if (hasSeenActive) {
                        finishAndRemoveTask()
                    }
                }

                // Prevent accidental dismissal via hardware Back button.
                BackHandler(enabled = true) {}

                val displayAlarm = activeInfo?.alarm ?: initialAlarm
                val status = activeInfo?.status ?: AlarmPlaybackStatus.LOADING_SONG

                AlarmScreenContent(
                    alarm = displayAlarm,
                    status = status,
                    onSnooze = {
                        startService(
                            Intent(this@AlarmActivity, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_SNOOZE
                            }
                        )
                        finishAndRemoveTask()
                    },
                    onStop = {
                        startService(
                            Intent(this@AlarmActivity, AlarmService::class.java).apply {
                                action = AlarmService.ACTION_STOP
                            }
                        )
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmScreenContent(
    alarm: AlarmState?,
    status: AlarmPlaybackStatus,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    LaunchedEffect(Unit) {
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val currentLocale = Locale.getDefault()
    val timeText = remember(nowMillis, is24Hour, currentLocale) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", currentLocale).format(Date(nowMillis))
    }
    val datePattern = remember(currentLocale) {
        DateFormat.getBestDateTimePattern(currentLocale, "EEEEMMMd")
    }
    val dateText = remember(nowMillis, datePattern, currentLocale) {
        SimpleDateFormat(datePattern, currentLocale).format(Date(nowMillis))
    }

    val highResThumbnail = remember(alarm?.songThumbnail, coverResolution) {
        alarm?.songThumbnail?.resize(coverResolution.size, coverResolution.size)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Material 3 Expressive: full pill at rest -> rounded rectangle when pressed.
    val expressiveButtonShapes = ButtonDefaults.shapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(28.dp),
    )

    val baseSnoozeLabel = stringResource(R.string.alarm_snooze)
        .replace(Regex("""\s*\([^)]*\)"""), "")
        .trim()
    val snoozeMin = alarm?.snoozeMinutes ?: 10

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLandscape) {
            val isCompactHeight = configuration.screenHeightDp < 400
            val artworkOuterSize = if (isCompactHeight) 172.dp else 196.dp
            val artworkSize = if (isCompactHeight) 144.dp else 164.dp
            val buttonHeight = if (isCompactHeight) 84.dp else 96.dp
            val clockFontSize = if (isCompactHeight) 52.sp else 60.sp

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Pane: Artwork + Track Info
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    AlarmTrackInfoSection(
                        alarm = alarm,
                        status = status,
                        highResThumbnail = highResThumbnail,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        outerSize = artworkOuterSize,
                        artworkSize = artworkSize,
                        spacerHeight = 16.dp
                    )
                }

                // Right Pane: Clock Header + Morphing Expressive Buttons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .weight(1.05f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    AlarmClockHeader(
                        alarm = alarm,
                        timeText = timeText,
                        dateText = dateText,
                        clockFontSize = clockFontSize
                    )

                    Spacer(Modifier.height(16.dp))

                    AlarmActionButtonsRow(
                        snoozeText = "$baseSnoozeLabel (${snoozeMin}m)",
                        buttonHeight = buttonHeight,
                        expressiveButtonShapes = expressiveButtonShapes,
                        onSnooze = onSnooze,
                        onStop = onStop
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AlarmClockHeader(
                    alarm = alarm,
                    timeText = timeText,
                    dateText = dateText,
                    clockFontSize = 64.sp
                )

                AlarmTrackInfoSection(
                    alarm = alarm,
                    status = status,
                    highResThumbnail = highResThumbnail,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    outerSize = 216.dp,
                    artworkSize = 180.dp,
                    spacerHeight = 24.dp
                )

                AlarmActionButtonsRow(
                    snoozeText = "$baseSnoozeLabel (${snoozeMin}m)",
                    buttonHeight = 96.dp,
                    expressiveButtonShapes = expressiveButtonShapes,
                    onSnooze = onSnooze,
                    onStop = onStop
                )
            }
        }
    }
}

@Composable
private fun AlarmClockHeader(
    alarm: AlarmState?,
    timeText: String,
    dateText: String,
    clockFontSize: TextUnit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (!alarm?.label.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = alarm!!.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        } else {
            Text(
                text = stringResource(R.string.alarm_wake_up),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = timeText,
            fontSize = clockFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )

        Text(
            text = dateText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AlarmTrackInfoSection(
    alarm: AlarmState?,
    status: AlarmPlaybackStatus,
    highResThumbnail: String?,
    pulseScale: Float,
    pulseAlpha: Float,
    outerSize: Dp,
    artworkSize: Dp,
    spacerHeight: Dp,
) {
    val context = LocalContext.current
    val haloSize = outerSize - 6.dp
    val fallbackCircleSize = artworkSize - 16.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(outerSize)
        ) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            if (!highResThumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(highResThumbnail)
                        .crossfade(true)
                        .build(),
                    contentDescription = alarm?.songTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(artworkSize)
                        .clip(RoundedCornerShape(28.dp))
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(fallbackCircleSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule),
                        contentDescription = stringResource(R.string.alarm),
                        modifier = Modifier.size(fallbackCircleSize * 0.48f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(spacerHeight))

        Text(
            text = alarm?.songTitle?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.no_alarm_sound),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (!alarm?.songArtist.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = alarm!!.songArtist!!,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(12.dp))

        if (status == AlarmPlaybackStatus.LOADING_SONG) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(R.string.alarm_loading_song),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (status == AlarmPlaybackStatus.PLAYING_FALLBACK && !alarm?.songId.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = stringResource(R.string.alarm_using_fallback_tone),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AlarmActionButtonsRow(
    snoozeText: String,
    buttonHeight: Dp,
    expressiveButtonShapes: androidx.compose.material3.ButtonShapes,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilledTonalButton(
            onClick = onSnooze,
            shapes = expressiveButtonShapes,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            Text(
                text = snoozeText,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Button(
            onClick = onStop,
            shapes = expressiveButtonShapes,
            contentPadding = PaddingValues(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
                .weight(1f)
                .height(buttonHeight)
        ) {
            Text(
                text = stringResource(R.string.alarm_stop),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}