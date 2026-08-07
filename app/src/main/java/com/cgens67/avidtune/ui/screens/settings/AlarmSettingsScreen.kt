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
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import com.cgens67.avidtune.alarm.AlarmState
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.utils.backToMain
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    songIdArg: String? = null
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    
    var alarms by remember { mutableStateOf(AlarmManagerHelper.getAlarms(context)) }
    var editingAlarm by remember { mutableStateOf<AlarmState?>(null) }
    var showSearchSheet by remember { mutableStateOf(false) }

    // If passed a song from another screen, create a new alarm automatically
    var hasProcessedArg by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(songIdArg) {
        if (!hasProcessedArg && !songIdArg.isNullOrBlank() && songIdArg != "{songId}") {
            hasProcessedArg = true
            val song = database.song(songIdArg).firstOrNull()
            if (song != null) {
                val newAlarm = AlarmState(
                    songId = songIdArg,
                    songTitle = song.song.title,
                    songArtist = song.artists.joinToString { it.name },
                    songThumbnail = song.song.thumbnailUrl,
                    isEnabled = true // auto enable when created from song
                )
                editingAlarm = newAlarm
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Alarms") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingAlarm = AlarmState() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Alarm")
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 88.dp) // space for FAB
        ) {
            if (alarms.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No Alarms set",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onCheckedChange = { isChecked ->
                            if (isChecked && alarm.songId.isBlank()) {
                                Toast.makeText(context, "Please select an alarm sound first.", Toast.LENGTH_SHORT).show()
                                return@AlarmCard
                            }
                            val newAlarms = alarms.map { if (it.id == alarm.id) alarm.copy(isEnabled = isChecked) else it }
                            alarms = newAlarms
                            AlarmManagerHelper.saveAlarms(context, newAlarms)
                        },
                        onClick = { editingAlarm = alarm }
                    )
                }
            }
        }
    }

    if (editingAlarm != null) {
        EditAlarmBottomSheet(
            initialAlarm = editingAlarm!!,
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
                val newAlarms = alarms.filter { it.id != editingAlarm!!.id }
                alarms = newAlarms
                AlarmManagerHelper.saveAlarms(context, newAlarms)
                editingAlarm = null
            },
            onOpenSearch = { showSearchSheet = true }
        )
    }

    if (showSearchSheet) {
        AlarmSongSearchSheet(
            onDismiss = { showSearchSheet = false },
            onSongSelected = { songItem ->
                // Update the currently editing alarm state
                editingAlarm = editingAlarm?.copy(
                    songId = songItem.id,
                    songTitle = songItem.title,
                    songArtist = songItem.artists.joinToString { it.name },
                    songThumbnail = songItem.thumbnail
                )
                // Ensure it's cached locally
                kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
                    database.transaction { insert(songItem.toMediaMetadata()) }
                }
            }
        )
    }
}

@Composable
fun AlarmCard(
    alarm: AlarmState,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hour)
        set(Calendar.MINUTE, alarm.minute)
    }
    val timeFormat = if (is24Hour) SimpleDateFormat("HH:mm", Locale.getDefault()) else SimpleDateFormat("h:mm", Locale.getDefault())
    val amPmFormat = SimpleDateFormat("a", Locale.getDefault())

    val daysText = if (alarm.days.isEmpty()) "Once" 
                   else if (alarm.days.size == 7) "Everyday" 
                   else listOf("M", "T", "W", "T", "F", "S", "S").filterIndexed { i, _ -> 
                       val dayVal = if (i == 6) Calendar.SUNDAY else i + 2
                       alarm.days.contains(dayVal) 
                   }.joinToString(", ")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                Text(
                    text = "$daysText • ${alarm.songTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onCheckedChange,
                enabled = alarm.songId.isNotBlank()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmBottomSheet(
    initialAlarm: AlarmState,
    onSave: (AlarmState) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var currentAlarm by remember { mutableStateOf(initialAlarm) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var showTimePicker by remember { mutableStateOf(false) }
    var isTimeInput by remember { mutableStateOf(false) }
    
    val timePickerState = rememberTimePickerState(
        initialHour = currentAlarm.hour,
        initialMinute = currentAlarm.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )

    if (showTimePicker) {
        TimePickerDialog(
            title = "Set Time",
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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Edit Alarm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                    Button(onClick = { 
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { 
                            onSave(currentAlarm.copy(isEnabled = currentAlarm.songId.isNotBlank())) 
                        } 
                    }) {
                        Text("Save")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Time 
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
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
                    fontSize = 72.sp,
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

            Spacer(Modifier.height(32.dp))

            // Days
            Text("Repeat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            val daysList = listOf("M" to Calendar.MONDAY, "T" to Calendar.TUESDAY, "W" to Calendar.WEDNESDAY, "T" to Calendar.THURSDAY, "F" to Calendar.FRIDAY, "S" to Calendar.SATURDAY, "S" to Calendar.SUNDAY)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysList.forEach { (label, dayValue) ->
                    val isSelected = currentAlarm.days.contains(dayValue)
                    val circleColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, label = "")
                    val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, label = "")
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .clickable {
                                val newDays = if (isSelected) currentAlarm.days - dayValue else currentAlarm.days + dayValue
                                currentAlarm = currentAlarm.copy(days = newDays)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Sound
            Text("Alarm Sound", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (currentAlarm.songId.isNotBlank()) {
                            currentAlarm = currentAlarm.copy(songId = "", songTitle = "No Alarm Sound", songArtist = null, songThumbnail = null, isEnabled = false)
                            Toast.makeText(context, "Alarm sound removed. Alarm turned off.", Toast.LENGTH_SHORT).show()
                        } else {
                            onOpenSearch()
                        }
                    }
            ) {
                if (currentAlarm.songId.isNotBlank()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = currentAlarm.songThumbnail,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(currentAlarm.songTitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            currentAlarm.songArtist?.let {
                                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Icon(painterResource(R.drawable.close), contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("No Alarm Sound", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Tap to select a track", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
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

    // Consumes ALL vertical overscroll to prevent the sheet from dragging and snapping back
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
