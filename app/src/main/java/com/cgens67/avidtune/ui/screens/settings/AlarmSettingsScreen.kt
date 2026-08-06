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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntrinsicSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.R
import com.cgens67.avidtune.alarm.AlarmManagerHelper
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var alarmHour by remember { mutableIntStateOf(prefs.getInt("alarm_hour", 8)) }
    var alarmMinute by remember { mutableIntStateOf(prefs.getInt("alarm_minute", 0)) }
    var alarmDays by remember { 
        mutableStateOf(prefs.getStringSet("alarm_days", emptySet())?.map { it.toInt() }?.toSet() ?: emptySet()) 
    }
    
    var alarmSongId by remember { mutableStateOf(prefs.getString("alarm_song_id", null) ?: "") }
    var alarmSongTitle by remember { mutableStateOf(prefs.getString("alarm_song_title", "No Alarm Sound") ?: "No Alarm Sound") }
    var alarmSongArtist by remember { mutableStateOf<String?>(null) }
    var alarmSongThumbnail by remember { mutableStateOf<String?>(null) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }

    val cal = remember { Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() } }

    val timePickerState = rememberTimePickerState(
        initialHour = alarmHour,
        initialMinute = alarmMinute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Calculate the next exact timestamp the alarm should ring based on time and days
    fun getNextAlarmTime(hour: Int, minute: Int, days: Set<Int>): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (days.isEmpty()) {
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis
        }

        for (i in 0..7) {
            val candidate = target.clone() as Calendar
            candidate.add(Calendar.DAY_OF_MONTH, i)
            if (days.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                if (candidate.after(now)) {
                    return candidate.timeInMillis
                }
            }
        }
        return target.timeInMillis
    }

    val nextAlarmTime = remember(alarmHour, alarmMinute, alarmDays) {
        getNextAlarmTime(alarmHour, alarmMinute, alarmDays)
    }

    // Process navigation argument ONLY on first launch to avoid the sticky bug
    var hasProcessedArg by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(songIdArg) {
        if (!hasProcessedArg && !songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            hasProcessedArg = true
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

    // Refresh song info whenever ID changes (e.g. from the search bottom sheet)
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
    LaunchedEffect(nextAlarmTime, alarmEnabled) {
        while (isActive) {
            if (alarmEnabled) {
                val diff = nextAlarmTime - System.currentTimeMillis()
                if (diff > 0) {
                    val days = diff / (1000 * 60 * 60 * 24)
                    val hours = (diff / (1000 * 60 * 60)) % 24
                    val minutes = (diff / (1000 * 60)) % 60
                    
                    timeRemainingText = when {
                        days > 0 -> "Alarm in $days days ${hours}h ${minutes}m"
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
                alarmHour = timePickerState.hour
                alarmMinute = timePickerState.minute
                prefs.edit()
                    .putInt("alarm_hour", alarmHour)
                    .putInt("alarm_minute", alarmMinute)
                    .apply()
                if (alarmEnabled) {
                    AlarmManagerHelper.setAlarm(context, getNextAlarmTime(alarmHour, alarmMinute, alarmDays), alarmSongId)
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
                        val finalTime = cal.timeInMillis
                        prefs.edit().putLong("alarm_time", finalTime).apply()
                        if (alarmEnabled) {
                            AlarmManagerHelper.setAlarm(context, finalTime, alarmSongId)
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

    if (showSearchSheet) {
        AlarmSongSearchSheet(
            onDismiss = { showSearchSheet = false },
            onSongSelected = { songItem ->
                alarmSongId = songItem.id
                alarmSongTitle = songItem.title
                alarmSongArtist = songItem.artists.joinToString { it.name }
                alarmSongThumbnail = songItem.thumbnail
                
                prefs.edit()
                    .putString("alarm_song_id", alarmSongId)
                    .putString("alarm_song_title", alarmSongTitle)
                    .apply()

                // Insert to local database so AlarmManager can find it without loading YouTube network API
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    database.transaction {
                        insert(songItem.toMediaMetadata())
                    }
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Alarm Settings") },
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
            Spacer(modifier = Modifier.height(24.dp))

            // Main Time Display
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val calToDisplay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, alarmHour)
                set(Calendar.MINUTE, alarmMinute)
            }
            val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
            val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { showTimePicker = true }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = timeFormat.format(calToDisplay.time),
                    fontSize = 86.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!is24Hour) {
                    Text(
                        text = amPmFormat.format(calToDisplay.time),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
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
                shape = RoundedCornerShape(32.dp),
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
                                    AlarmManagerHelper.setAlarm(context, nextAlarmTime, alarmSongId)
                                } else {
                                    AlarmManagerHelper.cancelAlarm(context)
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 24.dp),
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
                                    AlarmManagerHelper.setAlarm(context, nextAlarmTime, alarmSongId)
                                } else {
                                    AlarmManagerHelper.cancelAlarm(context)
                                }
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Repeat Days Section
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                        Text(
                            text = "Repeat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        val daysList = listOf(
                            "M" to Calendar.MONDAY,
                            "T" to Calendar.TUESDAY,
                            "W" to Calendar.WEDNESDAY,
                            "T" to Calendar.THURSDAY,
                            "F" to Calendar.FRIDAY,
                            "S" to Calendar.SATURDAY,
                            "S" to Calendar.SUNDAY
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysList.forEach { (label, dayValue) ->
                                val isSelected = alarmDays.contains(dayValue)
                                
                                val circleColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "dayColor"
                                )
                                val textColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "dayTextColor"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(circleColor)
                                        .clickable {
                                            val newDays = if (isSelected) alarmDays - dayValue else alarmDays + dayValue
                                            alarmDays = newDays
                                            prefs.edit().putStringSet("alarm_days", newDays.map { it.toString() }.toSet()).apply()
                                            
                                            // Reschedule if currently active
                                            if (alarmEnabled) {
                                                val updatedTime = getNextAlarmTime(alarmHour, alarmMinute, newDays)
                                                AlarmManagerHelper.setAlarm(context, updatedTime, alarmSongId)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Alarm Sound row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (alarmSongId.isNotBlank()) {
                                    // Remove the song and turn off the alarm safely
                                    alarmSongId = ""
                                    alarmSongTitle = "No Alarm Sound"
                                    alarmSongArtist = null
                                    alarmSongThumbnail = null
                                    alarmEnabled = false
                                    
                                    prefs.edit()
                                        .putString("alarm_song_id", "")
                                        .putString("alarm_song_title", "No Alarm Sound")
                                        .putBoolean("alarm_enabled", false)
                                        .apply()
                                        
                                    AlarmManagerHelper.cancelAlarm(context)
                                    Toast.makeText(context, "Alarm sound removed. Alarm turned off.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // Open Search Bottom Sheet
                                    showSearchSheet = true
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 20.dp)
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
                                        .size(52.dp)
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
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
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
                                        text = "Tap to search for a track",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSongSearchSheet(
    onDismiss: () -> Unit,
    onSongSelected: (SongItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<SongItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

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
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            // Header
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
                            text = "Select Alarm Sound",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Search YouTube Music",
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
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Search Input Field
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
                        onValueChange = { newQuery ->
                            query = newQuery
                            if (newQuery.isNotBlank()) {
                                isSearching = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    delay(500) // Debounce
                                    YouTube.search(newQuery, YouTube.SearchFilter.FILTER_SONG).onSuccess { res ->
                                        withContext(Dispatchers.Main) {
                                            searchResults = res.items.filterIsInstance<SongItem>()
                                            isSearching = false
                                        }
                                    }.onFailure { withContext(Dispatchers.Main) { isSearching = false } }
                                }
                            } else {
                                searchResults = emptyList()
                                isSearching = false
                            }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search for a song...",
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
                            contentDescription = "Clear",
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

            // Results
            if (isSearching) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(painter = painterResource(R.drawable.search), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text(if (query.isBlank()) "Type to search songs" else "No songs found for '$query'", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            modifier = Modifier.fillMaxWidth().clickable {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onSongSelected(song) }
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.thumbnail,
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
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

                                // Display duration
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

        LaunchedEffect(Unit) {
            delay(150)
            focusRequester.requestFocus()
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
