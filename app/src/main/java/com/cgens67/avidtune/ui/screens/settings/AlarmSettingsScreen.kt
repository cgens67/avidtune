@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import android.text.format.DateFormat as AndroidDateFormat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.offline.Download
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.alarm.AlarmManagerHelper
import com.cgens67.avidtune.alarm.AlarmState
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    songIdArg: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val fabOffset = if (playerBottom > systemBottom) playerBottom - systemBottom else 0.dp

    var alarms by remember { mutableStateOf(AlarmManagerHelper.getAlarms(context)) }
    var alarmPendingDelete by remember { mutableStateOf<AlarmState?>(null) }

    // Permission states refreshed on resume.
    var hasNotificationPerm by remember { mutableStateOf(hasNotificationPermission(context)) }
    var hasExactAlarmPerm by remember { mutableStateOf(AlarmManagerHelper.canScheduleExactAlarms(context)) }
    var hasFullScreenPerm by remember { mutableStateOf(AlarmManagerHelper.canUseFullScreenIntent(context)) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPerm = granted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                alarms = AlarmManagerHelper.getAlarms(context)
                hasNotificationPerm = hasNotificationPermission(context)
                hasExactAlarmPerm = AlarmManagerHelper.canScheduleExactAlarms(context)
                hasFullScreenPerm = AlarmManagerHelper.canUseFullScreenIntent(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Forward preselected songId (from SongMenu / PlayerMenu) to the dedicated EditAlarmScreen.
    var hasProcessedArg by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(songIdArg) {
        if (!hasProcessedArg && !songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            hasProcessedArg = true
            navController.navigate("alarm_edit?songId=${Uri.encode(songIdArg)}")
        }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var timeRemainingText by remember { mutableStateOf("") }
    var nextAlarmPair by remember { mutableStateOf<Pair<AlarmState, Long>?>(null) }

    val allAlarmsOffText = stringResource(R.string.alarm_all_off)
    val nextLessMinText = stringResource(R.string.alarm_next_less_than_minute)
    val nextInText = stringResource(R.string.alarm_next_in)
    val soundSoonText = stringResource(R.string.alarm_sound_soon)
    val noAlarmsSetText = stringResource(R.string.no_alarms_set)

    LaunchedEffect(alarms) {
        while (isActive) {
            nowMillis = System.currentTimeMillis()
            if (alarms.isEmpty()) {
                timeRemainingText = noAlarmsSetText
                nextAlarmPair = null
            } else {
                val activeAlarms = alarms.filter { it.isEnabled }
                if (activeAlarms.isEmpty()) {
                    timeRemainingText = allAlarmsOffText
                    nextAlarmPair = null
                } else {
                    val nextPair = activeAlarms.map { alarm ->
                        val triggerMs = AlarmManagerHelper.getSnoozedUntil(context, alarm.id)
                            ?: AlarmManagerHelper.getNextAlarmTime(alarm.hour, alarm.minute, alarm.days)
                        alarm to triggerMs
                    }.minByOrNull { it.second }

                    nextAlarmPair = nextPair
                    if (nextPair != null) {
                        val diff = nextPair.second - nowMillis
                        if (diff > 0) {
                            val d = diff / (1000 * 60 * 60 * 24)
                            val h = (diff / (1000 * 60 * 60)) % 24
                            val m = (diff / (1000 * 60)) % 60
                            val parts = buildList {
                                if (d > 0) add("${d}d")
                                if (h > 0) add("${h}h")
                                if (m > 0) add("${m}m")
                            }
                            timeRemainingText = if (parts.isEmpty()) {
                                nextLessMinText
                            } else {
                                String.format(nextInText, parts.joinToString(" "))
                            }
                        } else {
                            timeRemainingText = soundSoonText
                        }
                    }
                }
            }
            delay(1000L)
        }
    }

    val is24Hour = DateFormat.is24HourFormat(context)
    val currentLocale = Locale.getDefault()
    val liveClockText = remember(nowMillis, is24Hour, currentLocale) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", currentLocale).format(Date(nowMillis))
    }

    val hasPermissionWarning = !hasNotificationPerm || !hasExactAlarmPerm || !hasFullScreenPerm

    val openNewAlarm: () -> Unit = {
        navController.navigate("alarm_edit")
    }

    val openEditAlarm: (AlarmState) -> Unit = { alarm ->
        navController.navigate("alarm_edit?alarmId=${Uri.encode(alarm.id)}")
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.alarms),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = liveClockText,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (alarms.isNotEmpty()) {
                FloatingActionButton(
                    onClick = openNewAlarm,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(bottom = fabOffset)
                        .size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_alarm),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Pane: Next Alarm Hero + Permissions Banner
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = playerBottom + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NextAlarmHeroCard(
                        nextAlarmPair = nextAlarmPair,
                        timeRemainingText = timeRemainingText,
                        nowMillis = nowMillis,
                        is24Hour = is24Hour,
                        currentLocale = currentLocale,
                        isCompactPane = true,
                        onNextAlarmClick = openEditAlarm
                    )

                    if (hasPermissionWarning) {
                        AlarmPermissionsWarningCard(
                            needsNotifications = !hasNotificationPerm,
                            needsExactAlarm = !hasExactAlarmPerm,
                            needsFullScreen = !hasFullScreenPerm,
                            onGrantNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onGrantExactAlarm = { AlarmManagerHelper.openExactAlarmSettings(context) },
                            onGrantFullScreen = { AlarmManagerHelper.openFullScreenIntentSettings(context) }
                        )
                    }
                }

                // Right Pane: Alarms List or Expressive Empty State
                LazyColumn(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 96.dp)
                ) {
                    if (alarms.isEmpty()) {
                        item(key = "empty_alarms") {
                            AlarmEmptyStateCard(
                                onAddClick = openNewAlarm,
                                modifier = Modifier.fillParentMaxSize()
                            )
                        }
                    } else {
                        items(alarms, key = { it.id }) { alarm ->
                            val snoozedUntil = AlarmManagerHelper.getSnoozedUntil(context, alarm.id)
                            AlarmCard(
                                alarm = alarm,
                                snoozedUntilMs = snoozedUntil,
                                onCheckedChange = { isChecked ->
                                    val newAlarms = alarms.map {
                                        if (it.id == alarm.id) alarm.copy(isEnabled = isChecked) else it
                                    }
                                    alarms = newAlarms
                                    AlarmManagerHelper.saveAlarms(context, newAlarms)
                                },
                                onClick = { openEditAlarm(alarm) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    alarmPendingDelete = alarm
                                },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = playerBottom + 104.dp)
            ) {
                if (hasPermissionWarning) {
                    item(key = "permissions_banner") {
                        AlarmPermissionsWarningCard(
                            needsNotifications = !hasNotificationPerm,
                            needsExactAlarm = !hasExactAlarmPerm,
                            needsFullScreen = !hasFullScreenPerm,
                            onGrantNotifications = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                            },
                            onGrantExactAlarm = { AlarmManagerHelper.openExactAlarmSettings(context) },
                            onGrantFullScreen = { AlarmManagerHelper.openFullScreenIntentSettings(context) }
                        )
                    }
                }

                if (alarms.isNotEmpty()) {
                    item(key = "next_alarm_hero") {
                        NextAlarmHeroCard(
                            nextAlarmPair = nextAlarmPair,
                            timeRemainingText = timeRemainingText,
                            nowMillis = nowMillis,
                            is24Hour = is24Hour,
                            currentLocale = currentLocale,
                            isCompactPane = false,
                            onNextAlarmClick = openEditAlarm
                        )
                    }
                }

                if (alarms.isEmpty()) {
                    item(key = "empty_alarms") {
                        AlarmEmptyStateCard(
                            onAddClick = openNewAlarm,
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(bottom = 48.dp)
                        )
                    }
                } else {
                    items(alarms, key = { it.id }) { alarm ->
                        val snoozedUntil = AlarmManagerHelper.getSnoozedUntil(context, alarm.id)
                        AlarmCard(
                            alarm = alarm,
                            snoozedUntilMs = snoozedUntil,
                            onCheckedChange = { isChecked ->
                                val newAlarms = alarms.map {
                                    if (it.id == alarm.id) alarm.copy(isEnabled = isChecked) else it
                                }
                                alarms = newAlarms
                                AlarmManagerHelper.saveAlarms(context, newAlarms)
                            },
                            onClick = { openEditAlarm(alarm) },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                alarmPendingDelete = alarm
                            },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }

    alarmPendingDelete?.let { targetAlarm ->
        DeleteAlarmDialog(
            alarm = targetAlarm,
            onDismiss = { alarmPendingDelete = null },
            onConfirmDelete = {
                AlarmManagerHelper.deleteAlarm(context, targetAlarm.id)
                alarms = AlarmManagerHelper.getAlarms(context)
                alarmPendingDelete = null
            }
        )
    }
}

private fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.areNotificationsEnabled() ?: true
    }
}

@Composable
private fun formatAlarmDaysText(days: Set<Int>): String {
    val onceText = stringResource(R.string.alarm_repeat_once)
    val everydayText = stringResource(R.string.alarm_repeat_everyday)
    return remember(days, onceText, everydayText) {
        if (days.isEmpty()) onceText
        else if (days.size == 7) everydayText
        else {
            val format = SimpleDateFormat("E", Locale.getDefault())
            listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
                .filter { days.contains(it) }
                .joinToString(", ") { day ->
                    val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, day) }
                    format.format(c.time)
                }
        }
    }
}

@Composable
fun DeleteAlarmDialog(
    alarm: AlarmState,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val context = LocalContext.current
    val is24Hour = AndroidDateFormat.is24HourFormat(context)
    val cal = remember(alarm.hour, alarm.minute) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
        }
    }
    val timePart = remember(alarm.hour, alarm.minute, is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault()).format(cal.time)
    }
    val amPmPart = remember(alarm.hour, alarm.minute, is24Hour) {
        if (is24Hour) null else SimpleDateFormat("a", Locale.getDefault()).format(cal.time)
    }
    val daysText = formatAlarmDaysText(alarm.days)
    val songText = if (alarm.songId.isBlank()) {
        stringResource(R.string.alarm_default_tone)
    } else {
        alarm.songTitle
    }

    val expressiveShapes = ButtonDefaults.shapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(20.dp)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 400.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Expressive Trash Icon Badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "${stringResource(R.string.delete)} ${stringResource(R.string.alarm)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Embedded Alarm Preview Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!alarm.songThumbnail.isNullOrBlank()) {
                            AsyncImage(
                                model = alarm.songThumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.schedule),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            if (alarm.label.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = alarm.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = timePart,
                                    fontSize = 28.sp,
                                    lineHeight = 32.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (amPmPart != null) {
                                    Text(
                                        text = amPmPart,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "$daysText • $songText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                // Morphing Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onDismiss,
                        shapes = expressiveShapes,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = stringResource(android.R.string.cancel),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = onConfirmDelete,
                        shapes = expressiveShapes,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NextAlarmHeroCard(
    nextAlarmPair: Pair<AlarmState, Long>?,
    timeRemainingText: String,
    nowMillis: Long,
    is24Hour: Boolean,
    currentLocale: Locale,
    isCompactPane: Boolean,
    onNextAlarmClick: (AlarmState) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAlarm = nextAlarmPair?.first
    val triggerMs = nextAlarmPair?.second

    val infiniteTransition = rememberInfiniteTransition(label = "hero_alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val heroTimeOnly = remember(triggerMs, nowMillis, is24Hour, currentLocale) {
        val ms = triggerMs ?: nowMillis
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", currentLocale).format(Date(ms))
    }
    val heroAmPm = remember(triggerMs, nowMillis, is24Hour, currentLocale) {
        if (is24Hour) null
        else SimpleDateFormat("a", currentLocale).format(Date(triggerMs ?: nowMillis))
    }

    val heroSubDate = remember(triggerMs, nowMillis, currentLocale) {
        val ms = triggerMs ?: nowMillis
        val pattern = DateFormat.getBestDateTimePattern(currentLocale, "EEEEMMMd")
        SimpleDateFormat(pattern, currentLocale).format(Date(ms))
    }

    val pillColor = if (activeAlarm != null) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val pillTextColor = if (activeAlarm != null) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .then(
                if (activeAlarm != null) {
                    Modifier.clickable { onNextAlarmClick(activeAlarm) }
                } else {
                    Modifier
                }
            )
    ) {
        if (isCompactPane) {
            // Landscape left-pane layout: artwork sits cleanly on top row beside the pill so the clock never wraps/collides.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pillColor,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = timeRemainingText.ifBlank { stringResource(R.string.alarms) },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = pillTextColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    HeroAlarmBadge(
                        activeAlarm = activeAlarm,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                        outerSize = 58.dp,
                        innerSize = 48.dp
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = heroTimeOnly,
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Light,
                        color = if (activeAlarm != null) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                    if (heroAmPm != null) {
                        Text(
                            text = heroAmPm,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeAlarm != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            },
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 7.dp, start = 6.dp)
                        )
                    }
                }

                Text(
                    text = if (activeAlarm != null) {
                        val labelPart = activeAlarm.label.ifBlank { stringResource(R.string.alarm_wake_up) }
                        "$labelPart • $heroSubDate"
                    } else {
                        heroSubDate
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Portrait layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = pillColor
                    ) {
                        Text(
                            text = timeRemainingText.ifBlank { stringResource(R.string.alarms) },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = pillTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = heroTimeOnly,
                            fontSize = 46.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Light,
                            color = if (activeAlarm != null) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            },
                            maxLines = 1,
                            softWrap = false
                        )
                        if (heroAmPm != null) {
                            Text(
                                text = heroAmPm,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeAlarm != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                },
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(bottom = 8.dp, start = 6.dp)
                            )
                        }
                    }

                    Text(
                        text = if (activeAlarm != null) {
                            val labelPart = activeAlarm.label.ifBlank { stringResource(R.string.alarm_wake_up) }
                            "$labelPart • $heroSubDate"
                        } else {
                            heroSubDate
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(12.dp))

                HeroAlarmBadge(
                    activeAlarm = activeAlarm,
                    pulseScale = pulseScale,
                    pulseAlpha = pulseAlpha,
                    outerSize = 84.dp,
                    innerSize = 68.dp
                )
            }
        }
    }
}

@Composable
private fun HeroAlarmBadge(
    activeAlarm: AlarmState?,
    pulseScale: Float,
    pulseAlpha: Float,
    outerSize: androidx.compose.ui.unit.Dp,
    innerSize: androidx.compose.ui.unit.Dp,
) {
    val haloSize = outerSize - 4.dp
    val circleSize = innerSize - 4.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(outerSize)
    ) {
        if (activeAlarm != null) {
            Box(
                modifier = Modifier
                    .size(haloSize)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }

        if (!activeAlarm?.songThumbnail.isNullOrBlank()) {
            AsyncImage(
                model = activeAlarm!!.songThumbnail,
                contentDescription = activeAlarm.songTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(innerSize)
                    .clip(RoundedCornerShape(if (innerSize >= 60.dp) 20.dp else 14.dp))
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(circleSize)
                    .clip(CircleShape)
                    .background(
                        if (activeAlarm != null) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            ) {
                Icon(
                    painter = painterResource(R.drawable.schedule),
                    contentDescription = stringResource(R.string.alarm),
                    tint = if (activeAlarm != null) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(circleSize * 0.5f)
                )
            }
        }
    }
}

@Composable
private fun AlarmEmptyStateCard(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_alarm_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val expressiveShapes = ButtonDefaults.shapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(28.dp),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(164.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(124.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    painter = painterResource(R.drawable.schedule),
                    contentDescription = null,
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.no_alarms_set),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.alarm_create_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onAddClick,
            shapes = expressiveShapes,
            contentPadding = PaddingValues(horizontal = 28.dp),
            modifier = Modifier.height(72.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.add_alarm),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AlarmPermissionsWarningCard(
    needsNotifications: Boolean,
    needsExactAlarm: Boolean,
    needsFullScreen: Boolean,
    onGrantNotifications: () -> Unit,
    onGrantExactAlarm: () -> Unit,
    onGrantFullScreen: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    painter = painterResource(R.drawable.error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = stringResource(R.string.permissions_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = stringResource(R.string.alarm_permissions_warning_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (needsNotifications) {
                    FilledTonalButton(
                        onClick = onGrantNotifications,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.notification), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (needsExactAlarm) {
                    FilledTonalButton(
                        onClick = onGrantExactAlarm,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.alarm_exact_permission), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (needsFullScreen) {
                    FilledTonalButton(
                        onClick = onGrantFullScreen,
                        shape = CircleShape,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.alarm_fullscreen_permission), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmState,
    snoozedUntilMs: Long? = null,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
    }
    val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

    val daysText = formatAlarmDaysText(alarm.days)

    val displaySongTitle = if (alarm.songId.isBlank()) {
        stringResource(R.string.alarm_default_tone)
    } else {
        alarm.songTitle
    }

    val containerColor by animateColorAsState(
        targetValue = if (alarm.isEnabled) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "alarm_card_bg"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!alarm.songThumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = alarm.songThumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .alpha(if (alarm.isEnabled) 1f else 0.55f)
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (alarm.isEnabled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.schedule),
                        contentDescription = null,
                        tint = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (alarm.label.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = alarm.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(cal.time),
                        fontSize = 42.sp,
                        lineHeight = 46.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        },
                        maxLines = 1,
                        softWrap = false
                    )
                    if (!is24Hour) {
                        Text(
                            text = amPmFormat.format(cal.time),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            },
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$daysText • $displaySongTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (snoozedUntilMs != null) {
                    val snoozeTimeStr = SimpleDateFormat(
                        if (is24Hour) "HH:mm" else "h:mm a",
                        Locale.getDefault()
                    ).format(Date(snoozedUntilMs))
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.alarm_snoozed_until, snoozeTimeStr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onCheckedChange,
                thumbContent = {
                    Icon(
                        painter = painterResource(
                            id = if (alarm.isEnabled) R.drawable.check else R.drawable.close
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedIconColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}

@Composable
fun EditAlarmScreen(
    navController: NavController,
    alarmId: String? = null,
    songIdArg: String? = null,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val existingAlarm = remember(alarmId) {
        alarmId?.let { AlarmManagerHelper.getAlarm(context, it) }
    }
    val isNew = existingAlarm == null

    var currentAlarm by remember(alarmId) {
        mutableStateOf(existingAlarm ?: AlarmState(isEnabled = true))
    }

    // Resolve preselected songId when opened from SongMenu / PlayerMenu.
    LaunchedEffect(songIdArg) {
        if (!songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            val dbSong = withContext(Dispatchers.IO) { database.song(songIdArg).firstOrNull() }
            if (dbSong != null) {
                currentAlarm = currentAlarm.copy(
                    songId = songIdArg,
                    songTitle = dbSong.song.title,
                    songArtist = dbSong.artists.joinToString { it.name },
                    songThumbnail = dbSong.song.thumbnailUrl,
                    isEnabled = true
                )
            } else {
                val activeMeta = playerConnection?.mediaMetadata?.value?.takeIf { it.id == songIdArg }
                if (activeMeta != null) {
                    withContext(Dispatchers.IO) { database.transaction { insert(activeMeta) } }
                    currentAlarm = currentAlarm.copy(
                        songId = activeMeta.id,
                        songTitle = activeMeta.title,
                        songArtist = activeMeta.artists.joinToString { it.name },
                        songThumbnail = activeMeta.thumbnailUrl,
                        isEnabled = true
                    )
                } else {
                    val fetched = withContext(Dispatchers.IO) {
                        YouTube.queue(listOf(songIdArg)).getOrNull()?.firstOrNull()
                    }
                    if (fetched != null) {
                        val meta = fetched.toMediaMetadata()
                        withContext(Dispatchers.IO) { database.transaction { insert(meta) } }
                        currentAlarm = currentAlarm.copy(
                            songId = meta.id,
                            songTitle = meta.title,
                            songArtist = meta.artists.joinToString { it.name },
                            songThumbnail = meta.thumbnailUrl,
                            isEnabled = true
                        )
                    }
                }
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = currentAlarm.hour,
        initialMinute = currentAlarm.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )

    LaunchedEffect(currentAlarm.hour, currentAlarm.minute) {
        timePickerState.hour = currentAlarm.hour
        timePickerState.minute = currentAlarm.minute
    }

    if (showTimePicker) {
        AvidTimePickerDialog(
            title = stringResource(R.string.alarm_set_time),
            onCancel = { showTimePicker = false },
            onConfirm = {
                currentAlarm = currentAlarm.copy(
                    hour = timePickerState.hour,
                    minute = timePickerState.minute
                )
                showTimePicker = false
            },
            toggle = {
                IconButton(onClick = { isTimeInput = !isTimeInput }) {
                    Icon(painterResource(if (isTimeInput) R.drawable.schedule else R.drawable.edit), null)
                }
            }
        ) {
            if (isTimeInput) TimeInput(state = timePickerState) else TimePicker(state = timePickerState)
        }
    }

    if (showDeleteConfirm && !isNew) {
        DeleteAlarmDialog(
            alarm = currentAlarm,
            onDismiss = { showDeleteConfirm = false },
            onConfirmDelete = {
                showDeleteConfirm = false
                AlarmManagerHelper.deleteAlarm(context, currentAlarm.id)
                navController.navigateUp()
            }
        )
    }

    val expressiveButtonShapes = ButtonDefaults.shapes(
        shape = CircleShape,
        pressedShape = RoundedCornerShape(24.dp),
    )

    val is24Hour = DateFormat.is24HourFormat(context)
    val calToDisplay = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, currentAlarm.hour)
        set(Calendar.MINUTE, currentAlarm.minute)
    }
    val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

    val daysList = remember {
        val shortWeekdays = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
        listOf(
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
        ).map { day ->
            val raw = shortWeekdays[day].orEmpty()
            val label = raw.take(3).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            label to day
        }
    }

    val bottomClearance = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val saveAlarm: () -> Unit = {
        val updated = currentAlarm.copy(isEnabled = if (isNew) true else currentAlarm.isEnabled)
        val existingList = AlarmManagerHelper.getAlarms(context)
        val newAlarms = if (existingList.any { it.id == updated.id }) {
            existingList.map { if (it.id == updated.id) updated else it }
        } else {
            existingList + updated
        }
        AlarmManagerHelper.saveAlarms(context, newAlarms)
        navController.navigateUp()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) stringResource(R.string.create_alarm) else stringResource(R.string.edit_alarm),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                painter = painterResource(R.drawable.delete),
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .imePadding()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Left Pane: Hero Time Card, Label, Repeat Days, Action Buttons
                Column(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = bottomClearance + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AlarmHeroTimeCard(
                        timeText = timeFormat.format(calToDisplay.time),
                        amPmText = if (is24Hour) null else amPmFormat.format(calToDisplay.time),
                        onClick = { showTimePicker = true }
                    )

                    OutlinedTextField(
                        value = currentAlarm.label,
                        onValueChange = { currentAlarm = currentAlarm.copy(label = it) },
                        label = { Text(stringResource(R.string.alarm_label)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.alarm_repeat),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        AlarmRepeatDaysRow(
                            daysList = daysList,
                            selectedDays = currentAlarm.days,
                            onToggleDay = { dayValue ->
                                val newDays = if (currentAlarm.days.contains(dayValue)) {
                                    currentAlarm.days - dayValue
                                } else {
                                    currentAlarm.days + dayValue
                                }
                                currentAlarm = currentAlarm.copy(days = newDays)
                            }
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    EditAlarmActionButtons(
                        expressiveButtonShapes = expressiveButtonShapes,
                        onCancel = { navController.navigateUp() },
                        onSave = saveAlarm
                    )
                }

                // Right Pane: Sound, Vibration, Volume, Snooze, Fade-in
                Column(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp, bottom = bottomClearance + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    AlarmSoundSelectorSection(
                        alarm = currentAlarm,
                        onOpenSearch = { showSearchSheet = true },
                        onClearSong = {
                            currentAlarm = currentAlarm.copy(
                                songId = "",
                                songTitle = "",
                                songArtist = null,
                                songThumbnail = null
                            )
                        }
                    )

                    AlarmVibrateAndVolumeSection(
                        alarm = currentAlarm,
                        onToggleVibrate = { currentAlarm = currentAlarm.copy(vibrate = !currentAlarm.vibrate) },
                        onVolumeChange = { pct -> currentAlarm = currentAlarm.copy(volumePercent = pct) }
                    )

                    AlarmSnoozeAndFadeSection(
                        alarm = currentAlarm,
                        onSnoozeChange = { mins -> currentAlarm = currentAlarm.copy(snoozeMinutes = mins) },
                        onFadeChange = { secs -> currentAlarm = currentAlarm.copy(fadeInSeconds = secs) }
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = bottomClearance + 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AlarmHeroTimeCard(
                    timeText = timeFormat.format(calToDisplay.time),
                    amPmText = if (is24Hour) null else amPmFormat.format(calToDisplay.time),
                    onClick = { showTimePicker = true }
                )

                OutlinedTextField(
                    value = currentAlarm.label,
                    onValueChange = { currentAlarm = currentAlarm.copy(label = it) },
                    label = { Text(stringResource(R.string.alarm_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.alarm_repeat),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    AlarmRepeatDaysRow(
                        daysList = daysList,
                        selectedDays = currentAlarm.days,
                        onToggleDay = { dayValue ->
                            val newDays = if (currentAlarm.days.contains(dayValue)) {
                                currentAlarm.days - dayValue
                            } else {
                                currentAlarm.days + dayValue
                            }
                            currentAlarm = currentAlarm.copy(days = newDays)
                        }
                    )
                }

                AlarmSoundSelectorSection(
                    alarm = currentAlarm,
                    onOpenSearch = { showSearchSheet = true },
                    onClearSong = {
                        currentAlarm = currentAlarm.copy(
                            songId = "",
                            songTitle = "",
                            songArtist = null,
                            songThumbnail = null
                        )
                    }
                )

                AlarmVibrateAndVolumeSection(
                    alarm = currentAlarm,
                    onToggleVibrate = { currentAlarm = currentAlarm.copy(vibrate = !currentAlarm.vibrate) },
                    onVolumeChange = { pct -> currentAlarm = currentAlarm.copy(volumePercent = pct) }
                )

                AlarmSnoozeAndFadeSection(
                    alarm = currentAlarm,
                    onSnoozeChange = { mins -> currentAlarm = currentAlarm.copy(snoozeMinutes = mins) },
                    onFadeChange = { secs -> currentAlarm = currentAlarm.copy(fadeInSeconds = secs) }
                )

                Spacer(Modifier.height(4.dp))

                EditAlarmActionButtons(
                    expressiveButtonShapes = expressiveButtonShapes,
                    onCancel = { navController.navigateUp() },
                    onSave = saveAlarm
                )
            }
        }
    }

    if (showSearchSheet) {
        AlarmSongSearchSheet(
            onDismiss = { showSearchSheet = false },
            onSongSelected = { songId, title, artist, thumbnail ->
                currentAlarm = currentAlarm.copy(
                    songId = songId,
                    songTitle = title,
                    songArtist = artist,
                    songThumbnail = thumbnail,
                    isEnabled = true
                )
            },
            onYouTubeSongSelected = { songItem ->
                withContext(Dispatchers.IO) {
                    database.transaction { insert(songItem.toMediaMetadata()) }
                }
            }
        )
    }
}

@Composable
private fun AlarmHeroTimeCard(
    timeText: String,
    amPmText: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(vertical = 20.dp, horizontal = 20.dp)
        ) {
            Text(
                text = timeText,
                fontSize = 64.sp,
                lineHeight = 68.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                softWrap = false
            )
            if (amPmText != null) {
                Text(
                    text = amPmText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun AlarmRepeatDaysRow(
    daysList: List<Pair<String, Int>>,
    selectedDays: Set<Int>,
    onToggleDay: (Int) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        daysList.forEach { (label, dayValue) ->
            val isSelected = selectedDays.contains(dayValue)
            val circleColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                label = ""
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = ""
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(CircleShape)
                    .background(circleColor)
                    .clickable { onToggleDay(dayValue) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
private fun AlarmSoundSelectorSection(
    alarm: AlarmState,
    onOpenSearch: () -> Unit,
    onClearSong: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(R.string.alarm_sound),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Surface(
            onClick = onOpenSearch,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (alarm.songId.isNotBlank()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = alarm.songThumbnail,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            alarm.songTitle,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        alarm.songArtist?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onClearSong) {
                        Icon(
                            painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_default_tone), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.alarm_tap_to_select_track), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmVibrateAndVolumeSection(
    alarm: AlarmState,
    onToggleVibrate: () -> Unit,
    onVolumeChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(
            onClick = onToggleVibrate,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.alarm_vibrate), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = alarm.vibrate,
                    onCheckedChange = { onToggleVibrate() }
                )
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.volume), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${alarm.volumePercent}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = alarm.volumePercent.toFloat(),
                onValueChange = { onVolumeChange(it.roundToInt().coerceIn(10, 100)) },
                valueRange = 10f..100f
            )
        }
    }
}

@Composable
private fun AlarmSnoozeAndFadeSection(
    alarm: AlarmState,
    onSnoozeChange: (Int) -> Unit,
    onFadeChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.alarm_snooze_duration), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val snoozeOptions = listOf(5, 10, 15, 20)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                snoozeOptions.forEachIndexed { index, mins ->
                    ToggleButton(
                        checked = alarm.snoozeMinutes == mins,
                        onCheckedChange = { onSnoozeChange(mins) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            snoozeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text("${mins}m", fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.alarm_fade_in), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val fadeOptions = listOf(0, 10, 20, 30)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                fadeOptions.forEachIndexed { index, secs ->
                    ToggleButton(
                        checked = alarm.fadeInSeconds == secs,
                        onCheckedChange = { onFadeChange(secs) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            fadeOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(
                            text = if (secs == 0) stringResource(R.string.off) else "${secs}s",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAlarmActionButtons(
    expressiveButtonShapes: androidx.compose.material3.ButtonShapes,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onCancel,
            shapes = expressiveButtonShapes,
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
        ) {
            Text(
                text = stringResource(android.R.string.cancel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Button(
            onClick = onSave,
            shapes = expressiveButtonShapes,
            modifier = Modifier
                .weight(1f)
                .height(72.dp)
        ) {
            Text(
                text = stringResource(R.string.save),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class SearchSheetTabContentState {
    LOADING, EMPTY, RESULTS
}

@Composable
private fun AlarmSheetEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AlarmSongSearchSheet(
    onDismiss: () -> Unit,
    onSongSelected: (songId: String, title: String, artist: String?, thumbnail: String?) -> Unit,
    onYouTubeSongSelected: suspend (SongItem) -> Unit
) {
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val allLibrarySongsState by database.allSongs().collectAsState(initial = null)
    val downloads by downloadUtil.downloads.collectAsState()

    val filteredLibrarySongs = remember(allLibrarySongsState, downloads, query) {
        val allSongs = allLibrarySongsState ?: return@remember emptyList()
        val q = query.trim()
        val base = allSongs.sortedByDescending { downloads[it.id]?.state == Download.STATE_COMPLETED }
        if (q.isBlank()) base else base.filter { song ->
            song.song.title.contains(q, ignoreCase = true) ||
                song.artists.any { it.name.contains(q, ignoreCase = true) }
        }
    }

    // Properly debounced search; shows loading animation immediately while waiting/fetching.
    LaunchedEffect(query, selectedTab) {
        if (selectedTab != 1) return@LaunchedEffect
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(450L)
        val result = withContext(Dispatchers.IO) {
            YouTube.search(trimmed, YouTube.SearchFilter.FILTER_SONG).getOrNull()
        }
        searchResults = result?.items?.filterIsInstance<SongItem>().orEmpty()
        isSearching = false
    }

    val nestedScrollConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                return androidx.compose.ui.geometry.Offset(0f, available.y)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .nestedScroll(nestedScrollConnection)
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.select_alarm_sound),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.alarm_search_ytm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.filter_library)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.search)) }
                )
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )

                    BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            if (selectedTab == 1) {
                                isSearching = it.trim().isNotBlank()
                            }
                        },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.alarm_search_placeholder),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (query.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable {
                                    query = ""
                                    searchResults = emptyList()
                                    isSearching = false
                                }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                val libraryState = when {
                    allLibrarySongsState == null -> SearchSheetTabContentState.LOADING
                    filteredLibrarySongs.isEmpty() -> SearchSheetTabContentState.EMPTY
                    else -> SearchSheetTabContentState.RESULTS
                }

                AnimatedContent(
                    targetState = libraryState,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "library_tab_state",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { state ->
                    when (state) {
                        SearchSheetTabContentState.LOADING -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                ContainedLoadingIndicator()
                            }
                        }

                        SearchSheetTabContentState.EMPTY -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                AlarmSheetEmptyState(
                                    title = stringResource(R.string.no_results_found),
                                    subtitle = stringResource(R.string.try_another_term)
                                )
                            }
                        }

                        SearchSheetTabContentState.RESULTS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredLibrarySongs, key = { it.id }) { song ->
                                    val isDownloaded = downloads[song.id]?.state == Download.STATE_COMPLETED
                                    AlarmLocalSongRow(
                                        song = song,
                                        isDownloaded = isDownloaded,
                                        onClick = {
                                            onSongSelected(
                                                song.id,
                                                song.song.title,
                                                song.artists.joinToString { it.name },
                                                song.song.thumbnailUrl
                                            )
                                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                val searchTabState = when {
                    isSearching -> SearchSheetTabContentState.LOADING
                    searchResults.isEmpty() -> SearchSheetTabContentState.EMPTY
                    else -> SearchSheetTabContentState.RESULTS
                }

                AnimatedContent(
                    targetState = searchTabState,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "search_tab_state",
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) { state ->
                    when (state) {
                        SearchSheetTabContentState.LOADING -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ContainedLoadingIndicator()
                                    Text(
                                        text = "${stringResource(R.string.search)}...",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        SearchSheetTabContentState.EMPTY -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (query.isBlank()) {
                                    AlarmSheetEmptyState(
                                        title = stringResource(R.string.alarm_type_to_search),
                                        subtitle = stringResource(R.string.alarm_search_ytm_desc)
                                    )
                                } else {
                                    AlarmSheetEmptyState(
                                        title = stringResource(R.string.no_results_found),
                                        subtitle = stringResource(R.string.try_another_term)
                                    )
                                }
                            }
                        }

                        SearchSheetTabContentState.RESULTS -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchResults, key = { it.id }) { song ->
                                    Surface(
                                        onClick = {
                                            coroutineScope.launch {
                                                onYouTubeSongSelected(song)
                                                onSongSelected(
                                                    song.id,
                                                    song.title,
                                                    song.artists.joinToString { it.name },
                                                    song.thumbnail
                                                )
                                                sheetState.hide()
                                            }.invokeOnCompletion { onDismiss() }
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = song.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .clip(RoundedCornerShape(14.dp)),
                                                contentScale = ContentScale.Crop
                                            )

                                            Spacer(Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = song.title,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = song.artists.joinToString(", ") { it.name },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(Modifier.width(8.dp))

                                            val durationText = song.duration?.let { makeTimeString(it * 1000L) } ?: ""
                                            if (durationText.isNotEmpty()) {
                                                Text(
                                                    text = durationText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    }
}

@Composable
private fun AlarmLocalSongRow(
    song: Song,
    isDownloaded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isDownloaded) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = stringResource(R.string.downloaded_songs),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AvidTimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .background(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                content()
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = onConfirm,
                        shape = CircleShape,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}