package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
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
import androidx.compose.ui.layout.ContentScale
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = LocalDatabase.current
    val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)

    var alarmEnabled by remember { mutableStateOf(prefs.getBoolean("alarm_enabled", false)) }
    var alarmTime by remember { mutableStateOf(prefs.getLong("alarm_time", System.currentTimeMillis())) }
    var alarmSongId by remember { mutableStateOf(prefs.getString("alarm_song_id", null) ?: "") }
    var alarmSongTitle by remember { mutableStateOf(prefs.getString("alarm_song_title", "Default Alarm") ?: "Default Alarm") }
    var alarmSongArtist by remember { mutableStateOf<String?>(null) }
    var alarmSongThumbnail by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(songIdArg, alarmSongId) {
        val targetId = if (!songIdArg.isNullOrBlank() && songIdArg != "{songId}") songIdArg else alarmSongId
        if (targetId.isNotBlank()) {
            val song = database.song(targetId).firstOrNull()
            if (song != null) {
                alarmSongId = targetId
                alarmSongTitle = song.song.title
                alarmSongArtist = song.artists.joinToString { it.name }
                alarmSongThumbnail = song.song.thumbnailUrl
                prefs.edit().putString("alarm_song_id", alarmSongId).putString("alarm_song_title", alarmSongTitle).apply()
            }
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

    SettingsPage(
        title = "Alarm",
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Hero Clock Card
            HeroClockCard(
                alarmTime = alarmTime,
                alarmEnabled = alarmEnabled,
                onToggleEnabled = { isEnabled ->
                    alarmEnabled = isEnabled
                    prefs.edit().putBoolean("alarm_enabled", isEnabled).apply()
                    if (isEnabled) {
                        AlarmManagerHelper.setAlarm(context, alarmTime, alarmSongId)
                    } else {
                        AlarmManagerHelper.cancelAlarm(context)
                    }
                },
                onClick = { showTimePicker = true }
            )

            // Date Card
            DateSelectionCard(
                alarmTime = alarmTime,
                onClick = { showDatePicker = true }
            )

            // Song Card
            Text(
                text = "ALARM SOUND",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )

            SongSelectionCard(
                songTitle = alarmSongTitle,
                songArtist = alarmSongArtist,
                thumbnailUrl = alarmSongThumbnail,
                hasSong = alarmSongId.isNotBlank(),
                onClick = { navController.navigate("library") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroClockCard(
    alarmTime: Long,
    alarmEnabled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

    val containerColor by animateColorAsState(
        targetValue = if (alarmEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )
    val contentColor by animateColorAsState(
        targetValue = if (alarmEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarmEnabled) 8.dp else 2.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Optional subtle gradient overlay when enabled
            if (alarmEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (alarmEnabled) "ALARM ON" else "ALARM OFF",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor.copy(alpha = 0.7f),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = timeFormat.format(alarmTime),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                        if (!is24Hour) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = amPmFormat.format(alarmTime),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor.copy(alpha = 0.8f),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = alarmEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun DateSelectionCard(
    alarmTime: Long,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.schedule),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(alarmTime),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SongSelectionCard(
    songTitle: String,
    songArtist: String?,
    thumbnailUrl: String?,
    hasSong: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (hasSong && !thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Song Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (hasSong) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!songArtist.isNullOrBlank()) {
                        Text(
                            text = songArtist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Pick a song from library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to browse your music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.arrow_forward),
                contentDescription = "Select Song",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
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
