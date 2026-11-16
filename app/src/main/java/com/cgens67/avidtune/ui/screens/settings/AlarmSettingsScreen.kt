package com.cgens67.avidtune.ui.screens.settings

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    songIdArg: String? = null
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()

    val playerBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
    val systemBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val fabOffset = if (playerBottom > systemBottom) playerBottom - systemBottom else 0.dp

    var alarms by remember { mutableStateOf(AlarmManagerHelper.getAlarms(context)) }
    var editingAlarm by remember { mutableStateOf<AlarmState?>(null) }
    var isNewAlarm by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

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

    // Handle preselected songId when launched from SongMenu / PlayerMenu.
    var hasProcessedArg by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(songIdArg) {
        if (!hasProcessedArg && !songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            hasProcessedArg = true
            val dbSong = withContext(Dispatchers.IO) { database.song(songIdArg).firstOrNull() }
            if (dbSong != null) {
                isNewAlarm = true
                editingAlarm = AlarmState(
                    songId = songIdArg,
                    songTitle = dbSong.song.title,
                    songArtist = dbSong.artists.joinToString { it.name },
                    songThumbnail = dbSong.song.thumbnailUrl,
                    isEnabled = true
                )
            } else {
                val activeMeta = playerConnection?.mediaMetadata?.value?.takeIf { it.id == songIdArg }
                if (activeMeta != null) {
                    withContext(Dispatchers.IO) {
                        database.transaction { insert(activeMeta) }
                    }
                    isNewAlarm = true
                    editingAlarm = AlarmState(
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
                        isNewAlarm = true
                        editingAlarm = AlarmState(
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

    var timeRemainingText by remember { mutableStateOf("") }
    val allAlarmsOffText = stringResource(R.string.alarm_all_off)
    val nextLessMinText = stringResource(R.string.alarm_next_less_than_minute)
    val nextInText = stringResource(R.string.alarm_next_in)
    val soundSoonText = stringResource(R.string.alarm_sound_soon)

    LaunchedEffect(alarms) {
        while (isActive) {
            if (alarms.isEmpty()) {
                timeRemainingText = ""
            } else {
                val activeAlarms = alarms.filter { it.isEnabled }
                if (activeAlarms.isEmpty()) {
                    timeRemainingText = allAlarmsOffText
                } else {
                    val nextTimes = activeAlarms.map { alarm ->
                        AlarmManagerHelper.getSnoozedUntil(context, alarm.id)
                            ?: AlarmManagerHelper.getNextAlarmTime(alarm.hour, alarm.minute, alarm.days)
                    }
                    val nextTime = nextTimes.minOrNull()
                    if (nextTime != null) {
                        val diff = nextTime - System.currentTimeMillis()
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

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.alarms)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    isNewAlarm = true
                    editingAlarm = AlarmState(isEnabled = true)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = fabOffset)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            if (!hasNotificationPerm || !hasExactAlarmPerm || !hasFullScreenPerm) {
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

            if (timeRemainingText.isNotEmpty()) {
                item(key = "next_alarm_text") {
                    Text(
                        text = timeRemainingText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            if (alarms.isEmpty()) {
                item(key = "empty_alarms") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.schedule),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .alpha(0.5f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_alarms_set),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.alarm_create_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                        onClick = {
                            isNewAlarm = false
                            editingAlarm = alarm
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (editingAlarm != null) {
        EditAlarmBottomSheet(
            initialAlarm = editingAlarm!!,
            isNew = isNewAlarm,
            onDismiss = { editingAlarm = null },
            onSave = { updatedAlarm ->
                val newAlarms = if (alarms.any { it.id == updatedAlarm.id }) {
                    alarms.map { if (it.id == updatedAlarm.id) updatedAlarm else it }
                } else {
                    alarms + updatedAlarm
                }
                alarms = newAlarms
                AlarmManagerHelper.saveAlarms(context, newAlarms)
                editingAlarm = null
            },
            onDelete = {
                AlarmManagerHelper.deleteAlarm(context, editingAlarm!!.id)
                alarms = AlarmManagerHelper.getAlarms(context)
                editingAlarm = null
            },
            onOpenSearch = { showSearchSheet = true }
        )
    }

    if (showSearchSheet) {
        AlarmSongSearchSheet(
            onDismiss = { showSearchSheet = false },
            onSongSelected = { songId, title, artist, thumbnail ->
                editingAlarm = editingAlarm?.copy(
                    songId = songId,
                    songTitle = title,
                    songArtist = artist,
                    songThumbnail = thumbnail,
                    isEnabled = true
                )
            },
            onYouTubeSongSelected = { songItem ->
                coroutineScope.launch(Dispatchers.IO) {
                    database.transaction { insert(songItem.toMediaMetadata()) }
                }
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
private fun AlarmPermissionsWarningCard(
    needsNotifications: Boolean,
    needsExactAlarm: Boolean,
    needsFullScreen: Boolean,
    onGrantNotifications: () -> Unit,
    onGrantExactAlarm: () -> Unit,
    onGrantFullScreen: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
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
                    FilledTonalButton(onClick = onGrantNotifications, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.notification), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (needsExactAlarm) {
                    FilledTonalButton(onClick = onGrantExactAlarm, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.alarm_exact_permission), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (needsFullScreen) {
                    FilledTonalButton(onClick = onGrantFullScreen, modifier = Modifier.weight(1f)) {
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

    val onceText = stringResource(R.string.alarm_repeat_once)
    val everydayText = stringResource(R.string.alarm_repeat_everyday)

    val daysText = remember(alarm.days, onceText, everydayText) {
        if (alarm.days.isEmpty()) onceText
        else if (alarm.days.size == 7) everydayText
        else {
            val format = SimpleDateFormat("E", Locale.getDefault())
            listOf(
                Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
            )
                .filter { alarm.days.contains(it) }
                .joinToString(", ") { day ->
                    val c = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, day) }
                    format.format(c.time)
                }
        }
    }

    val displaySongTitle = if (alarm.songId.isBlank()) {
        stringResource(R.string.alarm_default_tone)
    } else {
        alarm.songTitle
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (alarm.label.isNotBlank()) {
                    Text(
                        text = alarm.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(cal.time),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        color = if (alarm.isEnabled) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f)
                    )
                    if (!is24Hour) {
                        Text(
                            text = amPmFormat.format(cal.time),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f),
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!alarm.songThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = alarm.songThumbnail,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                    Text(
                        text = "$daysText • $displaySongTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (snoozedUntilMs != null) {
                    val snoozeTimeStr = SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault()).format(Date(snoozedUntilMs))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.alarm_snoozed_until, snoozeTimeStr),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmBottomSheet(
    initialAlarm: AlarmState,
    isNew: Boolean,
    onSave: (AlarmState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentAlarm by remember(initialAlarm.id) { mutableStateOf(initialAlarm) }

    // Sync song selection changes made by AlarmSongSearchSheet while this sheet is open.
    LaunchedEffect(initialAlarm.songId, initialAlarm.songTitle, initialAlarm.songThumbnail) {
        currentAlarm = currentAlarm.copy(
            songId = initialAlarm.songId,
            songTitle = initialAlarm.songTitle,
            songArtist = initialAlarm.songArtist,
            songThumbnail = initialAlarm.songThumbnail,
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = currentAlarm.hour,
        initialMinute = currentAlarm.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isNew) stringResource(R.string.create_alarm) else stringResource(R.string.edit_alarm),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    if (!isNew) {
                        TextButton(
                            onClick = {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDelete() }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    } else {
                        TextButton(onClick = {
                            coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        }) {
                            Text(stringResource(android.R.string.cancel))
                        }
                    }
                    Button(onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onSave(currentAlarm.copy(isEnabled = if (isNew) true else currentAlarm.isEnabled))
                        }
                    }) {
                        Text(stringResource(R.string.save))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val is24Hour = DateFormat.is24HourFormat(context)
            val calToDisplay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, currentAlarm.hour)
                set(Calendar.MINUTE, currentAlarm.minute)
            }
            val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showTimePicker = true }
                    .padding(8.dp)
            ) {
                Text(
                    text = timeFormat.format(calToDisplay.time),
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!is24Hour) {
                    Text(
                        text = amPmFormat.format(calToDisplay.time),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = currentAlarm.label,
                onValueChange = { currentAlarm = currentAlarm.copy(label = it) },
                label = { Text(stringResource(R.string.alarm_label)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            Text(stringResource(R.string.alarm_repeat), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

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

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysList.forEach { (label, dayValue) ->
                    val isSelected = currentAlarm.days.contains(dayValue)
                    val circleColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, label = "")
                    val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(circleColor)
                            .clickable {
                                val newDays = if (isSelected) currentAlarm.days - dayValue else currentAlarm.days + dayValue
                                currentAlarm = currentAlarm.copy(days = newDays)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(stringResource(R.string.alarm_sound), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenSearch() } // Always clickable to replace the song!
            ) {
                if (currentAlarm.songId.isNotBlank()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = currentAlarm.songThumbnail,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentAlarm.songTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            currentAlarm.songArtist?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        IconButton(onClick = {
                            currentAlarm = currentAlarm.copy(songId = "", songTitle = "", songArtist = null, songThumbnail = null)
                        }) {
                            Icon(painterResource(R.drawable.close), contentDescription = stringResource(R.string.clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
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

            Spacer(Modifier.height(20.dp))

            // Vibration toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { currentAlarm = currentAlarm.copy(vibrate = !currentAlarm.vibrate) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.alarm_vibrate), style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = currentAlarm.vibrate,
                    onCheckedChange = { currentAlarm = currentAlarm.copy(vibrate = it) }
                )
            }

            Spacer(Modifier.height(12.dp))

            // Volume slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.volume), style = MaterialTheme.typography.titleMedium)
                Text("${currentAlarm.volumePercent}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = currentAlarm.volumePercent.toFloat(),
                onValueChange = { currentAlarm = currentAlarm.copy(volumePercent = it.roundToInt().coerceIn(10, 100)) },
                valueRange = 10f..100f
            )

            Spacer(Modifier.height(12.dp))

            // Snooze duration pills
            Text(stringResource(R.string.alarm_snooze_duration), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 10, 15, 20).forEach { mins ->
                    FilterChip(
                        selected = currentAlarm.snoozeMinutes == mins,
                        onClick = { currentAlarm = currentAlarm.copy(snoozeMinutes = mins) },
                        label = { Text("${mins}m") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Fade-in duration pills
            Text(stringResource(R.string.alarm_fade_in), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 10, 20, 30).forEach { secs ->
                    FilterChip(
                        selected = currentAlarm.fadeInSeconds == secs,
                        onClick = { currentAlarm = currentAlarm.copy(fadeInSeconds = secs) },
                        label = { Text(if (secs == 0) stringResource(R.string.off) else "${secs}s") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSongSearchSheet(
    onDismiss: () -> Unit,
    onSongSelected: (songId: String, title: String, artist: String?, thumbnail: String?) -> Unit,
    onYouTubeSongSelected: (SongItem) -> Unit
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

    val allLibrarySongs by database.allSongs().collectAsState(initial = emptyList())
    val downloads by downloadUtil.downloads.collectAsState()

    val filteredLibrarySongs = remember(allLibrarySongs, downloads, query) {
        val q = query.trim()
        val base = allLibrarySongs.sortedByDescending { downloads[it.id]?.state == Download.STATE_COMPLETED }
        if (q.isBlank()) base else base.filter { song ->
            song.song.title.contains(q, ignoreCase = true) ||
                song.artists.any { it.name.contains(q, ignoreCase = true) }
        }
    }

    // Properly debounced search that cancels automatically when `query` or `selectedTab` changes.
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
                            .clip(RoundedCornerShape(12.dp))
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
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        onValueChange = { query = it },
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
                                .clickable { query = ""; searchResults = emptyList() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTab == 0) {
                if (filteredLibrarySongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_results_found),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
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
            } else {
                if (isSearching) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painter = painterResource(R.drawable.search), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text(
                                if (query.isBlank()) stringResource(R.string.alarm_type_to_search)
                                else stringResource(R.string.alarm_no_songs_found, query),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchResults, key = { it.id }) { song ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onYouTubeSongSelected(song)
                                        onSongSelected(
                                            song.id,
                                            song.title,
                                            song.artists.joinToString { it.name },
                                            song.thumbnail
                                        )
                                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                                    }
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
                                            .clip(RoundedCornerShape(12.dp)),
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

@Composable
private fun AlarmLocalSongRow(
    song: Song,
    isDownloaded: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    .clip(RoundedCornerShape(12.dp)),
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
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
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
                    Button(onClick = onConfirm, modifier = Modifier.padding(start = 8.dp)) { Text(stringResource(android.R.string.ok)) }
                }
            }
        }
    }
}