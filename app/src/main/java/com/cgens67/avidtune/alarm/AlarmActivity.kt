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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.AppFontKey
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.UseSystemFontKey
import com.cgens67.avidtune.extensions.toEnum
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.screens.settings.ThemePalettes
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmActivity : ComponentActivity() {

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
                        // Dismissed or snoozed from the notification.
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

@Composable
private fun AlarmScreenContent(
    alarm: AlarmState?,
    status: AlarmPlaybackStatus,
    onSnooze: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val timeText = remember(nowMillis, is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date(nowMillis))
    }
    val dateText = remember(nowMillis) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(nowMillis))
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Clock & Label Header
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
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )

                Text(
                    text = dateText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Artwork + Track Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(210.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(204.dp)
                            .scale(pulseScale)
                            .alpha(pulseAlpha)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )

                    if (!alarm?.songThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = alarm!!.songThumbnail,
                            contentDescription = alarm.songTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(172.dp)
                                .clip(RoundedCornerShape(28.dp))
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(164.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.schedule),
                                contentDescription = stringResource(R.string.alarm),
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

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

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledTonalButton(
                    onClick = onSnooze,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    val snoozeMin = alarm?.snoozeMinutes ?: 10
                    Text(
                        text = "${stringResource(R.string.alarm_snooze)} (${snoozeMin}m)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onStop,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                ) {
                    Text(
                        text = stringResource(R.string.alarm_stop),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}