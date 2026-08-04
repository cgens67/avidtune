package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.R
import com.cgens67.avidtune.alarm.AlarmManagerHelper
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SwitchPreference
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    songIdArg: String? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = LocalDatabase.current
    val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    var alarmEnabled by remember { mutableStateOf(prefs.getBoolean("alarm_enabled", false)) }
    var alarmTime by remember { mutableStateOf(prefs.getLong("alarm_time", System.currentTimeMillis())) }
    var alarmSongId by remember { mutableStateOf(prefs.getString("alarm_song_id", null) ?: "") }
    var alarmSongTitle by remember { mutableStateOf(prefs.getString("alarm_song_title", "Default Alarm") ?: "Default Alarm") }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }

    val cal = Calendar.getInstance().apply { timeInMillis = alarmTime }

    val timePickerState = rememberTimePickerState(
        initialHour = cal.get(Calendar.HOUR_OF_DAY),
        initialMinute = cal.get(Calendar.MINUTE),
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = alarmTime)

    LaunchedEffect(songIdArg) {
        if (!songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            alarmSongId = songIdArg
            val song = database.song(songIdArg).firstOrNull()
            if (song != null) {
                alarmSongTitle = song.song.title
                prefs.edit().putString("alarm_song_id", alarmSongId).putString("alarm_song_title", alarmSongTitle).apply()
            }
        } else if (alarmSongId.isNotBlank()) {
            val song = database.song(alarmSongId).firstOrNull()
            if (song != null) {
                alarmSongTitle = song.song.title
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            title = "Select Alarm Time",
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

    SettingsPage(
        title = "Alarm",
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = "Configuration",
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Enable Alarm") },
                        icon = { Icon(painterResource(R.drawable.schedule), null) },
                        description = "Wake up to your favorite track",
                        checked = alarmEnabled,
                        onCheckedChange = {
                            alarmEnabled = it
                            prefs.edit().putBoolean("alarm_enabled", it).apply()
                            if (it) {
                                AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                            } else {
                                AlarmManagerHelper.cancelAlarm(context)
                            }
                        }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text("Alarm Time") },
                        description = SimpleDateFormat("HH:mm", Locale.getDefault()).format(alarmTime),
                        icon = { Icon(painterResource(R.drawable.schedule), null) },
                        onClick = { showTimePicker = true }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text("Alarm Date") },
                        description = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(alarmTime),
                        icon = { Icon(painterResource(R.drawable.date_range), null) },
                        onClick = { showDatePicker = true }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text("Alarm Song") },
                        description = if (alarmSongId.isBlank()) "No song selected" else alarmSongTitle,
                        icon = { Icon(painterResource(R.drawable.music_note), null) },
                        onClick = {
                            // Instruct user to pick from UI if none selected, or navigate to library
                            if (alarmSongId.isBlank()) {
                                navController.navigate("library")
                            }
                        }
                    )
                }
            )
        )
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
                .background(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    text = title,
                    style = MaterialTheme.typography.labelMedium
                )
                content()
                Row(modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()) {
                    toggle()
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel) { Text("Cancel") }
                    TextButton(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}
