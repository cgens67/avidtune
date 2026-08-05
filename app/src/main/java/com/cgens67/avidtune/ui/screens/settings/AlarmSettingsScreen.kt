package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.R
import com.cgens67.avidtune.alarm.AlarmManagerHelper
import com.cgens67.avidtune.ui.component.SettingsPage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    songIdArg: String? = null
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    var alarmEnabled by remember { mutableStateOf(prefs.getBoolean("alarm_enabled", false)) }
    var alarmTime by remember { mutableStateOf(prefs.getLong("alarm_time", System.currentTimeMillis())) }
    var alarmSongId by remember { mutableStateOf(prefs.getString("alarm_song_id", null) ?: "") }
    var alarmSongTitle by remember { mutableStateOf(prefs.getString("alarm_song_title", "No Alarm Sound") ?: "No Alarm Sound") }
    var alarmSongArtist by remember { mutableStateOf<String?>(null) }
    var alarmSongThumbnail by remember { mutableStateOf<String?>(null) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }

    val cal = remember { Calendar.getInstance().apply { timeInMillis = alarmTime } }

    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = alarmTime)

    // Handle song argument from navigation ONLY on first launch
    LaunchedEffect(songIdArg) {
        if (!songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            alarmSongId = songIdArg
            val song = database.song(songIdArg).firstOrNull()
            if (song != null) {
                alarmSongTitle = song.song.title
                alarmSongArtist = song.artists.joinToString { it.name }
                alarmSongThumbnail = song.song.thumbnailUrl
                prefs.edit()
                    .putString("alarm_song_id", alarmSongId)
                    .putString("alarm_song_title", alarmSongTitle)
                    .apply()
            }
        }
    }

    // Update details when alarmSongId changes
    LaunchedEffect(alarmSongId) {
        if (alarmSongId.isNotBlank()) {
            val song = database.song(alarmSongId).firstOrNull()
            if (song != null) {
                alarmSongTitle = song.song.title
                alarmSongArtist = song.artists.joinToString { it.name }
                alarmSongThumbnail = song.song.thumbnailUrl
            }
        }
    }

    // Time remaining calculator
    var timeRemainingText by remember { mutableStateOf("") }
    LaunchedEffect(alarmTime, alarmEnabled) {
        while (true) {
            if (alarmEnabled) {
                var triggerTime = alarmTime
                if (triggerTime <= System.currentTimeMillis()) {
                    triggerTime += 24 * 60 * 60 * 1000
                }
                val diff = triggerTime - System.currentTimeMillis()
                if (diff > 0) {
                    val hours = diff / (1000 * 60 * 60)
                    val minutes = (diff / (1000 * 60)) % 60
                    val days = hours / 24
                    val remainingHours = hours % 24
                    
                    timeRemainingText = when {
                        days > 0 -> "Alarm in $days days ${remainingHours}h ${minutes}m"
                        hours > 0 -> "Alarm in ${hours}h ${minutes}m"
                        minutes > 0 -> "Alarm in ${minutes}m"
                        else -> "Alarm in less than a minute"
                    }
                } else {
                    timeRemainingText = "Alarm will sound soon"
                }
            } else {
                timeRemainingText = "Alarm is off"
            }
            delay(1000)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "Set Alarm Time",
            onCancel = { showTimePicker = false },
            onConfirm = {
                cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                cal.set(Calendar.MINUTE, timePickerState.minute)
                cal.set(Calendar.SECOND, 0)
                alarmTime = cal.timeInMillis
                prefs.edit().putLong("alarm_time", alarmTime).apply()
                if (alarmEnabled) {
                    AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                }
                showTimePicker = false
            },
            toggle = {
                IconButton(onClick = { isTimeInput = !isTimeInput }) {
                    Icon(painterResource(if (isTimeInput) R.drawable.schedule else R.drawable.edit), null)
                }
            }
        ) {
            if (isTimeInput) {
                TimeInput(state = timePickerState)
            } else {
                TimePicker(state = timePickerState)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selected ->
                        val dateCal = Calendar.getInstance().apply { timeInMillis = selected }
                        cal.set(Calendar.YEAR, dateCal.get(Calendar.YEAR))
                        cal.set(Calendar.MONTH, dateCal.get(Calendar.MONTH))
                        cal.set(Calendar.DAY_OF_MONTH, dateCal.get(Calendar.DAY_OF_MONTH))
                        alarmTime = cal.timeInMillis
                        prefs.edit().putLong("alarm_time", alarmTime).apply()
                        if (alarmEnabled) {
                            AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Alarm") }, // Removed non-existent resource reference
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Main Time Display
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = timeFormat.format(alarmTime),
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!is24Hour) {
                    Text(
                        text = amPmFormat.format(alarmTime),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
                    )
                }
            }

            Text(
                text = timeRemainingText,
                style = MaterialTheme.typography.bodyLarge,
                color = if (alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Unified Settings Card
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Turn on/off toggle row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!alarmEnabled && alarmSongId.isBlank()) {
                                    Toast.makeText(context, "Please select an alarm sound first.", Toast.LENGTH_SHORT).show()
                                    return@clickable
                                }
                                val newState = !alarmEnabled
                                alarmEnabled = newState
                                prefs.edit().putBoolean("alarm_enabled", newState).apply()
                                if (newState) {
                                    AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                                } else {
                                    AlarmManagerHelper.cancelAlarm(context)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (alarmEnabled) "Alarm on" else "Alarm off",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = alarmEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && alarmSongId.isBlank()) {
                                    Toast.makeText(context, "Please select an alarm sound first.", Toast.LENGTH_SHORT).show()
                                    return@Switch
                                }
                                alarmEnabled = isChecked
                                prefs.edit().putBoolean("alarm_enabled", isChecked).apply()
                                if (isChecked) {
                                    AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                                } else {
                                    AlarmManagerHelper.cancelAlarm(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Date Selection row
                    val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(alarmTime),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Alarm Sound row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (alarmSongId.isNotBlank()) {
                                    // RESET the song
                                    alarmSongId = ""
                                    alarmSongTitle = "No Alarm Sound"
                                    alarmSongArtist = null
                                    alarmSongThumbnail = null
                                    
                                    // Automatically turn off the alarm
                                    alarmEnabled = false

                                    prefs.edit()
                                        .putString("alarm_song_id", "")
                                        .putString("alarm_song_title", "No Alarm Sound")
                                        .putBoolean("alarm_enabled", false)
                                        .apply()
                                        
                                    AlarmManagerHelper.cancelAlarm(context)
                                } else {
                                    // Navigate to library so they can choose
                                    navController.navigate("library") {
                                        // Mimics Bottom Nav Bar behavior, prevents stacking backstack
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        Text(
                            text = "Alarm sound",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        if (alarmSongId.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AsyncImage(
                                    model = alarmSongThumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = alarmSongTitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    alarmSongArtist?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Remove song",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        // Fixed missing resource issue, safely using music_note as fallback
                                        painter = painterResource(R.drawable.music_note),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "No Alarm Sound",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Tap to choose a track from library",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TimePickerDialog(
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
                .height(IntrinsicSize.Min)
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
                Row(modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    Button(onClick = onConfirm, modifier = Modifier.padding(start = 8.dp)) { Text("OK") }
                }
            }
        }
    }
}
