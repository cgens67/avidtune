package com.cgens67.avidtune.ui.menu

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.ListDialog
import com.cgens67.avidtune.ui.component.MenuItemData
import com.cgens67.avidtune.ui.component.MenuGroup
import com.cgens67.avidtune.ui.component.NewAction
import com.cgens67.avidtune.ui.component.NewActionGrid
import com.cgens67.avidtune.ui.component.TextFieldDialog
import com.cgens67.avidtune.viewmodels.LyricsMenuViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LyricsMenu(
    lyricsEntity: LyricsEntity?,
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
    onLyricsUpdated: () -> Unit = {},
    viewModel: LyricsMenuViewModel = hiltViewModel(),
    isTranslated: Boolean = false,
    onTranslateClick: () -> Unit = {},
    isRomanized: Boolean = false,
    onRomanizeClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = com.cgens67.avidtune.LocalPlayerConnection.current ?: return

    var showEditDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showSyncDialog by rememberSaveable {
        mutableStateOf(false)
    }
    
    var showOffsetDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var syncOffsetValue by remember { 
        mutableFloatStateOf(0f) 
    }

    LaunchedEffect(showOffsetDialog) {
        if (showOffsetDialog) {
            val rawText = lyricsEntity?.lyrics.orEmpty()
            syncOffsetValue = Regex("\\[offset:(-?\\d+)\\]").find(rawText)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        }
    }

    if (showOffsetDialog) {
        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            title = { Text(stringResource(R.string.lyrics_sync_offset)) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (syncOffsetValue > 0) "+" else ""}${syncOffsetValue.toLong()} ms",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(onClick = { syncOffsetValue = (syncOffsetValue - 50f).coerceAtLeast(-5000f) }) {
                            Icon(painterResource(R.drawable.remove), contentDescription = "-")
                        }
                        Slider(
                            value = syncOffsetValue,
                            onValueChange = { syncOffsetValue = it },
                            valueRange = -5000f..5000f,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { syncOffsetValue = (syncOffsetValue + 50f).coerceAtMost(5000f) }) {
                            Icon(painterResource(R.drawable.add), contentDescription = "+")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.sync_offset_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showOffsetDialog = false
                        val rawText = lyricsEntity?.lyrics.orEmpty()
                        val textWithoutOffset = rawText.replace(Regex("\\[offset:-?\\d+\\]\\n?"), "")
                        
                        val newOffsetTag = if (syncOffsetValue == 0f) "" else "[offset:${syncOffsetValue.toLong()}]\n"
                        val finalLyrics = if (textWithoutOffset.startsWith("[provider:")) {
                            val lines = textWithoutOffset.lines()
                            val providerLine = lines.first()
                            val rest = lines.drop(1).joinToString("\n")
                            "$providerLine\n$newOffsetTag$rest"
                        } else {
                            "$newOffsetTag$textWithoutOffset"
                        }
                        
                        database.query {
                            upsert(LyricsEntity(id = mediaMetadata.id, lyrics = finalLyrics.trimStart('\n')))
                        }
                        onLyricsUpdated()
                        onDismiss()
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { syncOffsetValue = 0f }) {
                        Text(stringResource(R.string.reset))
                    }
                    TextButton(onClick = { showOffsetDialog = false }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        )
    }

    if (showEditDialog) {
        val rawText = lyricsEntity?.lyrics.orEmpty()
        val cleanText = if (rawText.startsWith("[provider:")) {
            rawText.substringAfter('\n')
        } else {
            rawText
        }
        
        TextFieldDialog(
            onDismiss = { showEditDialog = false },
            icon = { Icon(painterResource(R.drawable.edit), contentDescription = null) },
            title = { Text(text = mediaMetadata.title) },
            initialTextFieldValue = TextFieldValue(cleanText),
            singleLine = false,
            onDone = { newLyrics ->
                val oldTag = if (rawText.startsWith("[provider:")) {
                    rawText.substringBefore('\n') + "\n"
                } else {
                    ""
                }
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = oldTag + newLyrics,
                        ),
                    )
                }
                onLyricsUpdated()
                onDismiss()
            },
        )
    }

    if (showSyncDialog) {
        SyncLyricsDialog(
            lyricsText = lyricsEntity?.lyrics.orEmpty(),
            playerConnection = playerConnection,
            onDismiss = { showSyncDialog = false },
            onSave = { syncedLyrics ->
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = syncedLyrics,
                        ),
                    )
                }
                onLyricsUpdated()
                showSyncDialog = false
                onDismiss()
            }
        )
    }

    var showSearchDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showSearchResultDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val searchMediaMetadata = remember(showSearchDialog) { mediaMetadata }
    
    val (titleField, onTitleFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadata.title,
                ),
            )
        }
    val (artistField, onArtistFieldChange) =
        rememberSaveable(showSearchDialog, stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = mediaMetadata.artists.joinToString { it.name },
                ),
            )
        }

    if (showSearchDialog) {
        DefaultDialog(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            onDismiss = { showSearchDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.search_lyrics)) },
            buttons = {
                TextButton(
                    onClick = { showSearchDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        showSearchDialog = false
                        onDismiss()
                        try {
                            context.startActivity(
                                Intent(Intent.ACTION_WEB_SEARCH).apply {
                                    putExtra(
                                        SearchManager.QUERY,
                                        "${artistField.text} ${titleField.text} lyrics"
                                    )
                                },
                            )
                        } catch (_: Exception) {
                        }
                    },
                ) {
                    Text(stringResource(R.string.search_online))
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        viewModel.search(
                            searchMediaMetadata.id,
                            titleField.text,
                            artistField.text,
                            searchMediaMetadata.duration
                        )
                        showSearchResultDialog = true
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        ) {
            OutlinedTextField(
                value = titleField,
                onValueChange = onTitleFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_title)) },
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = artistField,
                onValueChange = onArtistFieldChange,
                singleLine = true,
                label = { Text(stringResource(R.string.song_artists)) },
            )
        }
    }

    if (showSearchResultDialog) {
        val results by viewModel.results.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()

        var expandedItemIndex by rememberSaveable {
            mutableStateOf(-1)
        }

        ListDialog(
            onDismiss = { showSearchResultDialog = false },
        ) {
            itemsIndexed(results) { index, result ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.cancelSearch()
                                database.query {
                                    val newLyrics = "[provider:${result.providerName}]\n${result.lyrics}"
                                    upsert(
                                        LyricsEntity(
                                            id = searchMediaMetadata.id,
                                            lyrics = newLyrics,
                                        ),
                                    )
                                }
                                onLyricsUpdated()
                                showSearchResultDialog = false
                                onDismiss()
                            }
                            .padding(12.dp)
                            .animateContentSize(),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = result.lyrics,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (index == expandedItemIndex) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = result.providerName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                maxLines = 1,
                            )
                            if (result.lyrics.startsWith("[")) {
                                Icon(
                                    painter = painterResource(R.drawable.sync),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier =
                                        Modifier
                                            .padding(start = 4.dp)
                                            .size(18.dp),
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            expandedItemIndex = if (expandedItemIndex == index) -1 else index
                        },
                    ) {
                        Icon(
                            painter = painterResource(if (index == expandedItemIndex) R.drawable.expand_less else R.drawable.expand_more),
                            contentDescription = null,
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.material3.LoadingIndicator()
                    }
                }
            }

            if (!isLoading && results.isEmpty()) {
                item {
                    Text(
                        text = context.getString(R.string.lyrics_not_found),
                        textAlign = TextAlign.Center,
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Header con información de la canción
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = mediaMetadata.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = mediaMetadata.artists.joinToString { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // Grid de acciones principales
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.tune),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.sync_offset),
                        onClick = { showOffsetDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.edit),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.edit),
                        onClick = { showEditDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.history),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = "Sync Lyrics",
                        onClick = { showSyncDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.cached),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.refetch),
                        onClick = {
                            viewModel.refetchLyrics(mediaMetadata, lyricsEntity)
                            onLyricsUpdated()
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.search),
                        onClick = { showSearchDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.translate),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(if (isTranslated) R.string.show_original else R.string.Translate),
                        onClick = { 
                            onTranslateClick()
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.translate), // Text format icon
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(if (isRomanized) R.string.hide_romanized else R.string.romanize),
                        onClick = { 
                            onRomanizeClick()
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncLyricsDialog(
    lyricsText: String,
    playerConnection: PlayerConnection,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val plainLines = remember(lyricsText) {
        val clean = if (lyricsText.startsWith("[provider:")) {
            lyricsText.substringAfter('\n')
        } else {
            lyricsText
        }
        clean.lines().map { it.replace(Regex("\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "").trim() }.filter { it.isNotEmpty() }
    }

    val timestamps = remember { mutableStateMapOf<Int, Long>() }
    var currentIndex by remember { mutableIntStateOf(0) }

    var currentPosition by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    LaunchedEffect(playerConnection.playbackState.collectAsState().value, playerConnection.isPlaying.collectAsState().value) {
        while (isActive) {
            currentPosition = playerConnection.player.currentPosition
            delay(50)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex) {
        if (currentIndex in plainLines.indices) {
            listState.animateScrollToItem(maxOf(0, currentIndex - 2))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Sync Lyrics") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(painterResource(R.drawable.close), contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val providerLine = if (lyricsText.startsWith("[provider:")) {
                                lyricsText.substringBefore('\n') + "\n"
                            } else ""

                            val syncedText = plainLines.mapIndexed { index, line ->
                                val time = timestamps[index]
                                if (time != null) {
                                    val min = time / 60000
                                    val sec = (time % 60000) / 1000
                                    val ms = (time % 1000) / 10
                                    String.format(java.util.Locale.US, "[%02d:%02d.%02d]%s", min, sec, ms, line)
                                } else {
                                    line
                                }
                            }.joinToString("\n")

                            onSave(providerLine + syncedText.trimStart('\n'))
                        }) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Lines list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    itemsIndexed(plainLines) { index, line ->
                        val isCurrent = index == currentIndex
                        val isSynced = timestamps.containsKey(index)
                        val time = timestamps[index]

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSynced) {
                                        playerConnection.player.seekTo(time!!)
                                    } else {
                                        currentIndex = index
                                    }
                                }
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isSynced) {
                                    val min = time!! / 60000
                                    val sec = (time % 60000) / 1000
                                    val ms = (time % 1000) / 10
                                    String.format(java.util.Locale.US, "[%02d:%02d.%02d]", min, sec, ms)
                                } else "[--:--.--]",
                                color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(80.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = line.ifBlank { "..." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                        else if (isSynced) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Controls
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues())
                            .padding(16.dp)
                    ) {
                        // Current time
                        Text(
                            text = com.cgens67.avidtune.utils.makeTimeString(currentPosition),
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Undo
                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        timestamps.remove(currentIndex)
                                    }
                                }
                            ) {
                                Icon(painterResource(R.drawable.skip_previous), contentDescription = "Undo")
                            }

                            // Play/Pause
                            val isPlaying by playerConnection.isPlaying.collectAsState()
                            IconButton(
                                onClick = { playerConnection.togglePlayPause() },
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(
                                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                    contentDescription = "Play/Pause",
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Spacer to balance the layout
                            Spacer(modifier = Modifier.size(48.dp))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Huge Sync Button
                        Button(
                            onClick = {
                                if (currentIndex < plainLines.size) {
                                    timestamps[currentIndex] = currentPosition
                                    currentIndex++
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            enabled = currentIndex < plainLines.size
                        ) {
                            Text("Sync Next Line", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
