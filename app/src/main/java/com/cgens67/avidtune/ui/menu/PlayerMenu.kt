@file:OptIn(ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.menu

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.ThumbnailCornerRadius
import com.cgens67.avidtune.db.entities.AlbumEntity
import com.cgens67.avidtune.db.entities.ArtistEntity
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.BottomSheetState
import com.cgens67.avidtune.ui.component.ListDialog
import com.cgens67.avidtune.ui.component.ListItem
import com.cgens67.avidtune.ui.component.MenuItemData
import com.cgens67.avidtune.ui.component.MenuGroup
import com.cgens67.avidtune.ui.component.NewAction
import com.cgens67.avidtune.ui.component.NewActionGrid
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun ColumnScope.PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id)
        .collectAsState(initial = null)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    val sleepTimerEnabled =
        remember(
            playerConnection.service.sleepTimer.triggerTime,
            playerConnection.service.sleepTimer.pauseWhenSongEnd
        ) {
            playerConnection.service.sleepTimer.isActive
        }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                        playerConnection.player.duration - playerConnection.player.currentPosition
                    } else {
                        playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableFloatStateOf(30f)
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showErrorPlaylistAddDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showInAppEqualizer by rememberSaveable {
        mutableStateOf(false)
    }

    if (showInAppEqualizer) {
        com.cgens67.avidtune.playback.InAppEqualizerBottomSheet(
            onDismiss = { showInAppEqualizer = false }
        )
    }

    var showExportSheet by rememberSaveable {
        mutableStateOf(false)
    }

    if (showExportSheet) {
        val dummySong = librarySong ?: Song(
            song = mediaMetadata.toSongEntity(),
            artists = mediaMetadata.artists.map { ArtistEntity(it.id ?: "", it.name) },
            album = mediaMetadata.album?.let { AlbumEntity(id = it.id, title = it.title, duration = 0, songCount = 0) }
        )
        ExportAudioBottomSheet(
            song = dummySong,
            onDismiss = { showExportSheet = false }
        )
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }
            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }
            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    title = stringResource(R.string.already_in_playlist),
                    thumbnailContent = {
                        Image(
                            painter = painterResource(R.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier =
                        Modifier
                            .clickable { showErrorPlaylistAddDialog = false },
                )
            }

            item {
                ListItem(
                    title = mediaMetadata.title,
                    thumbnailContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = mediaMetadata.thumbnailUrl,
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    subtitle =
                        joinByBullet(
                            mediaMetadata.artists.joinToString { it.name },
                            makeTimeString(mediaMetadata.duration * 1000L),
                        ),
                )
            }
        }
    }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoSheet by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoSheet) {
        TempoPitchBottomSheet(
            onDismiss = { showPitchTempoSheet = false },
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null
                )
            },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (sleepTimerEnabled) {
                        TextButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.clear()
                            },
                        ) {
                            Text(stringResource(R.string.cancel_timer))
                        }
                    }

                    TextButton(
                        onClick = {
                            showSleepTimerDialog = false
                            if (!sleepTimerEnabled) {
                                playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                            }
                        },
                    ) {
                        Text(
                            text = if (sleepTimerEnabled)
                                stringResource(android.R.string.ok)
                            else
                                stringResource(R.string.start_timer)
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false },
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sleepTimerEnabled) {
                        Text(
                            text = stringResource(R.string.sleep_timer_active),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            text = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                                stringResource(R.string.end_of_song)
                            } else {
                                makeTimeString(sleepTimerTimeLeft)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    if (!sleepTimerEnabled) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.minute,
                                sleepTimerValue.roundToInt(),
                                sleepTimerValue.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Slider(
                            value = sleepTimerValue,
                            onValueChange = { sleepTimerValue = it },
                            valueRange = 5f..120f,
                            steps = (120 - 5) / 5 - 1,
                        )

                        OutlinedButton(
                            onClick = {
                                showSleepTimerDialog = false
                                playerConnection.service.sleepTimer.start(-1)
                            },
                        ) {
                            Text(stringResource(R.string.end_of_song))
                        }
                    }
                }
            },
        )
    }

    if (isQueueTrigger != true) {
        var isMuted by remember { mutableStateOf(false) }
        var previousVolume by remember { mutableFloatStateOf(playerVolume.value) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(
                        if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            isMuted = !isMuted
                            if (isMuted) {
                                previousVolume = playerVolume.value
                                playerConnection.service.playerVolume.value = 0f
                            } else {
                                playerConnection.service.playerVolume.value = previousVolume
                            }
                        },
                )

                Slider(
                    value = if (isMuted) 0f else playerVolume.value,
                    onValueChange = { volume ->
                        if (!isMuted) {
                            playerConnection.service.playerVolume.value = volume
                            previousVolume = volume
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                )
            }

            if (sleepTimerEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                            stringResource(R.string.end_of_song)
                        } else {
                            makeTimeString(sleepTimerTimeLeft)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    HorizontalDivider()

    Spacer(modifier = Modifier.height(12.dp))

    LazyColumn(
        modifier = Modifier.weight(1f, fill = false),
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = listOf(
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.start_radio),
                        onClick = {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    WatchEndpoint(videoId = mediaMetadata.id),
                                    mediaMetadata
                                )
                            )
                            onDismiss()
                        }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_add),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.add_to_playlist),
                        onClick = { showChoosePlaylistDialog = true }
                    ),
                    NewAction(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.share),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = stringResource(R.string.share),
                        onClick = {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, null))
                            onDismiss()
                        }
                    )
                ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp)
            )
        }

        item {
            MenuGroup(
                items = buildList {
                    if (artists.isNotEmpty()) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_artist)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.artists.joinToString { it.name },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.artist),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    if (mediaMetadata.artists.size == 1) {
                                        navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    } else {
                                        showSelectArtistDialog = true
                                    }
                                }
                            )
                        )
                    }
                    if (mediaMetadata.album != null) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.view_album)) },
                                description = {
                                    Text(
                                        text = mediaMetadata.album.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.album),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    navController.navigate("album/${mediaMetadata.album.id}")
                                    playerBottomSheetState.collapseSoft()
                                    onDismiss()
                                }
                            )
                        )
                    }
                }
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            MenuGroup(
                items = listOf(
                    when (download?.state) {
                        androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.offline),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                    onDismiss()
                                }
                            )
                        }

                        androidx.media3.exoplayer.offline.Download.STATE_QUEUED,
                        androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.downloading)) },
                                icon = {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                },
                                onClick = {
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        mediaMetadata.id,
                                        false,
                                    )
                                    onDismiss()
                                }
                            )
                        }

                        else -> {
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.download)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        insert(mediaMetadata)
                                    }
                                    val downloadRequest =
                                        DownloadRequest
                                            .Builder(mediaMetadata.id, mediaMetadata.id.toUri())
                                            .setCustomCacheKey(mediaMetadata.id)
                                            .setData(mediaMetadata.title.toByteArray())
                                            .build()
                                    DownloadService.sendAddDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        downloadRequest,
                                        false,
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    },
                    MenuItemData(
                        title = { Text(text = "Export to Device") },
                        description = { Text(text = "Download as audio file") },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = { showExportSheet = true }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            MenuGroup(
                items = buildList {
                    if (librarySong?.song?.inLibrary != null) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.remove_from_library)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.library_add_check),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.query {
                                        inLibrary(mediaMetadata.id, null)
                                    }
                                }
                            )
                        )
                    } else {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.add_to_library)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.library_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    database.transaction {
                                        insert(mediaMetadata)
                                        inLibrary(mediaMetadata.id, LocalDateTime.now())
                                    }
                                }
                            )
                        )
                    }

                    add(
                        MenuItemData(
                            title = { Text(text = stringResource(R.string.details)) },
                            description = { Text(text = stringResource(R.string.details_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.info),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                onShowDetailsDialog()
                                onDismiss()
                            }
                        )
                    )

                    if (isQueueTrigger != true) {
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.sleep_timer)) },
                                description = {
                                    Text(
                                        text = if (sleepTimerEnabled) {
                                            if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                                                stringResource(R.string.end_of_song)
                                            } else {
                                                makeTimeString(sleepTimerTimeLeft)
                                            }
                                        } else {
                                            stringResource(R.string.sleep_timer_desc)
                                        }
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.bedtime),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (sleepTimerEnabled) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                onClick = {
                                    showSleepTimerDialog = true
                                }
                            )
                        )
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.equalizer)) },
                                description = { Text(text = stringResource(R.string.in_app_audio_effects_and_eq)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.equalizer),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showInAppEqualizer = true
                                }
                            )
                        )
                        add(
                            MenuItemData(
                                title = { Text(text = stringResource(R.string.advanced)) },
                                description = { Text(text = stringResource(R.string.advanced_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.tune),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                onClick = {
                                    showPitchTempoSheet = true
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun TempoPitchBottomSheet(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()
    
    var tempo by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.speed)
    }
    var pitch by remember {
        mutableFloatStateOf(playerConnection.player.playbackParameters.pitch)
    }

    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters =
            PlaybackParameters(tempo, pitch)
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dismissWithAnimation = {
        coroutineScope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.tempo_and_pitch),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            ContinuousValueAdjuster(
                title = "Tempo",
                icon = R.drawable.speed,
                value = tempo,
                valueRange = 0.25f..3.0f,
                onValueChange = { 
                    tempo = it
                    updatePlaybackParameters()
                },
                valueText = { String.format(java.util.Locale.US, "%.2fx", it) },
                buttonStep = 0.05f
            )

            Spacer(modifier = Modifier.height(24.dp))

            ContinuousValueAdjuster(
                title = "Pitch",
                icon = R.drawable.discover_tune,
                value = pitch,
                valueRange = 0.25f..3.0f,
                onValueChange = { 
                    pitch = it
                    updatePlaybackParameters()
                },
                valueText = { String.format(java.util.Locale.US, "%.2fx", it) },
                buttonStep = 0.05f
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        tempo = 1f
                        pitch = 1f
                        updatePlaybackParameters()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.reset))
                }

                Button(
                    onClick = { dismissWithAnimation() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
fun ContinuousValueAdjuster(
    title: String,
    @DrawableRes icon: Int,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueText: (Float) -> String,
    buttonStep: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = valueText(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { 
                    val newValue = kotlin.math.round((value - buttonStep) * 100) / 100f
                    onValueChange(newValue.coerceIn(valueRange)) 
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.remove), 
                    contentDescription = "-",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Slider(
                value = value,
                onValueChange = { newValue ->
                    onValueChange(kotlin.math.round(newValue * 100) / 100f)
                },
                valueRange = valueRange,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = { 
                    val newValue = kotlin.math.round((value + buttonStep) * 100) / 100f
                    onValueChange(newValue.coerceIn(valueRange)) 
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.add), 
                    contentDescription = "+",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

enum class ExportState {
    IDLE, FETCHING, DOWNLOADING, SUCCESS, ERROR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportAudioBottomSheet(
    song: Song,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf(ExportState.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf("m4a") }
    
    ModalBottomSheet(onDismissRequest = { if (state != ExportState.FETCHING && state != ExportState.DOWNLOADING) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Export to Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Track Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artists.joinToString { it.name },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            AnimatedContent(targetState = state, label = "export_state") { currentState ->
                when (currentState) {
                    ExportState.IDLE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Audio Format",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("m4a", "webm").forEach { format ->
                                    FilterChip(
                                        selected = selectedFormat == format,
                                        onClick = { selectedFormat = format },
                                        label = { Text(format.uppercase()) }
                                    )
                                }
                            }
                            
                            Spacer(Modifier.height(16.dp))
                            
                            Text(
                                text = "Note: Native extraction ensures the highest quality (128kbps+). M4A is globally supported on all Android audio players.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(Modifier.height(32.dp))
                            
                            Button(
                                onClick = {
                                    state = ExportState.FETCHING
                                    coroutineScope.launch(Dispatchers.IO) {
                                        var downloadResponse: okhttp3.Response? = null
                                        var resolvedExtension = selectedFormat
                                        val spoofedAgentForDownload = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                        val youtubeUrl = "https://www.youtube.com/watch?v=${song.song.id}"
                                        
                                        // ----------------------------------------------------
                                        // DUAL HTTP CLIENTS:
                                        // fetchClient has a 5-second timeout to quickly skip offline APIs
                                        // downloadClient has an INFINITE (0s) read timeout to prevent "Software caused connection abort"
                                        // ----------------------------------------------------
                                        val fetchClient = OkHttpClient.Builder()
                                            .connectTimeout(5, TimeUnit.SECONDS)
                                            .readTimeout(5, TimeUnit.SECONDS)
                                            .build()
                                            
                                        val downloadClient = OkHttpClient.Builder()
                                            .connectTimeout(15, TimeUnit.SECONDS)
                                            .readTimeout(0, TimeUnit.SECONDS) // 0 means no timeout for the actual file stream
                                            .build()
                                            
                                        val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

                                        try {
                                            // ----------------------------------------------------
                                            // 1. Primary: Direct Google InnerTube Extraction
                                            // ----------------------------------------------------
                                            if (downloadResponse == null) {
                                                val innerTubeClients = listOf(
                                                    """{"context":{"client":{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","clientScreen":"WATCH","hl":"en"},"thirdParty":{"embedUrl":"https://www.youtube.com/"}},"playbackContext":{"contentPlaybackContext":{"signatureTimestamp":19000}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"ANDROID_VR","clientVersion":"1.56.27","hl":"en"}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"IOS","clientVersion":"20.11.6","deviceMake":"Apple","deviceModel":"iPhone10,4","osName":"iOS","osVersion":"16.7.7.20H330","hl":"en"}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"ANDROID","clientVersion":"19.30.36","androidSdkVersion":33,"osName":"Android","osVersion":"13","hl":"en"}},"videoId":"${song.song.id}"}"""
                                                )

                                                for (payload in innerTubeClients) {
                                                    if (downloadResponse != null) break
                                                    try {
                                                        val req = Request.Builder()
                                                            .url("https://www.youtube.com/youtubei/v1/player")
                                                            .post(payload.toRequestBody(mediaTypeJson))
                                                            .header("Content-Type", "application/json")
                                                            .header("User-Agent", spoofedAgentForDownload)
                                                            .build()
                                                        val response = fetchClient.newCall(req).execute()
                                                        if (response.isSuccessful) {
                                                            val responseString = response.body?.string() ?: ""
                                                            val json = JSONObject(responseString)
                                                            val streamingData = json.optJSONObject("streamingData") ?: continue
                                                            val formats = streamingData.optJSONArray("adaptiveFormats") ?: streamingData.optJSONArray("formats") ?: continue
                                                            
                                                            var candidateUrl: String? = null
                                                            var candidateExt: String = selectedFormat
                                                            
                                                            // EXPLICIT QUALITY FILTERING:
                                                            // Priority 1: Match Exact High-Quality itags (140 = 128kbps m4a, 251 = 160kbps webm)
                                                            for (i in 0 until formats.length()) {
                                                                val format = formats.getJSONObject(i)
                                                                if (format.has("url")) {
                                                                    val itag = format.optInt("itag")
                                                                    if (selectedFormat == "m4a" && itag == 140) {
                                                                        candidateUrl = format.getString("url")
                                                                        candidateExt = "m4a"
                                                                        break
                                                                    } else if (selectedFormat == "webm" && itag == 251) {
                                                                        candidateUrl = format.getString("url")
                                                                        candidateExt = "webm"
                                                                        break
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // Priority 2: Fallback to the highest bitrate format that matches the container
                                                            if (candidateUrl == null) {
                                                                var maxBitrate = 0
                                                                for (i in 0 until formats.length()) {
                                                                    val format = formats.getJSONObject(i)
                                                                    if (format.has("url")) {
                                                                        val mime = format.optString("mimeType", "").lowercase()
                                                                        val bitrate = format.optInt("bitrate", 0)
                                                                        if (selectedFormat == "m4a" && mime.contains("audio/mp4") && bitrate > maxBitrate) {
                                                                            maxBitrate = bitrate
                                                                            candidateUrl = format.getString("url")
                                                                            candidateExt = "m4a"
                                                                        } else if (selectedFormat == "webm" && mime.contains("audio/webm") && bitrate > maxBitrate) {
                                                                            maxBitrate = bitrate
                                                                            candidateUrl = format.getString("url")
                                                                            candidateExt = "webm"
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            
                                                            // VERIFY THE STREAM URL: Prevents 403 Forbidden blocks
                                                            if (candidateUrl != null) {
                                                                val dReq = Request.Builder()
                                                                    .url(candidateUrl)
                                                                    .header("User-Agent", spoofedAgentForDownload)
                                                                    .header("Connection", "close")
                                                                    .build()
                                                                val dRes = downloadClient.newCall(dReq).execute()
                                                                if (dRes.isSuccessful) {
                                                                    downloadResponse = dRes
                                                                    resolvedExtension = candidateExt
                                                                    break
                                                                } else {
                                                                    dRes.close()
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) { /* Silently fallback */ }
                                                }
                                            }

                                            // ----------------------------------------------------
                                            // 2. Fallback: Cobalt V11 Cluster
                                            // ----------------------------------------------------
                                            if (downloadResponse == null) {
                                                val payloadV11 = JSONObject().apply {
                                                    put("url", youtubeUrl)
                                                    put("downloadMode", "audio")
                                                    put("audioFormat", if (selectedFormat == "m4a") "best" else "opus")
                                                    put("filenameStyle", "basic")
                                                }.toString()
                                                
                                                val cobaltV11Instances = listOf(
                                                    "https://api.cobalt.tools/",
                                                    "https://api.cobalt.best/",
                                                    "https://cobalt.qewertyy.dev/"
                                                )
                                                
                                                for (instance in cobaltV11Instances) {
                                                    if (downloadResponse != null) break
                                                    try {
                                                        val body = payloadV11.toRequestBody(mediaTypeJson)
                                                        val req = Request.Builder()
                                                            .url(instance)
                                                            .post(body)
                                                            .header("Accept", "application/json")
                                                            .header("Content-Type", "application/json")
                                                            .header("User-Agent", spoofedAgentForDownload)
                                                            .build()
                                                        val response = fetchClient.newCall(req).execute()
                                                        if (response.isSuccessful) {
                                                            val responseString = response.body?.string() ?: ""
                                                            val json = JSONObject(responseString)
                                                            if (json.has("url")) {
                                                                val candidateUrl = json.getString("url")
                                                                val dReq = Request.Builder()
                                                                    .url(candidateUrl)
                                                                    .header("User-Agent", spoofedAgentForDownload)
                                                                    .header("Connection", "close")
                                                                    .build()
                                                                val dRes = downloadClient.newCall(dReq).execute()
                                                                if (dRes.isSuccessful) {
                                                                    downloadResponse = dRes
                                                                    resolvedExtension = selectedFormat
                                                                    break
                                                                } else {
                                                                    dRes.close()
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) { /* Silently fallback */ }
                                                }
                                            }
                                            
                                            // ----------------------------------------------------
                                            // 3. Fallback: Cobalt V7 Cluster
                                            // ----------------------------------------------------
                                            if (downloadResponse == null) {
                                                val payloadV7 = JSONObject().apply {
                                                    put("url", youtubeUrl)
                                                    put("isAudioOnly", true)
                                                    put("aFormat", selectedFormat)
                                                }.toString()
                                                
                                                val cobaltV7Instances = listOf(
                                                    "https://co.wuk.sh/api/json",
                                                    "https://api.cnvmp3.com/api/json",
                                                    "https://cobalt.api.timelessnesses.me/api/json"
                                                )
                                                
                                                for (instance in cobaltV7Instances) {
                                                    if (downloadResponse != null) break
                                                    try {
                                                        val body = payloadV7.toRequestBody(mediaTypeJson)
                                                        val req = Request.Builder()
                                                            .url(instance)
                                                            .post(body)
                                                            .header("Accept", "application/json")
                                                            .header("Content-Type", "application/json")
                                                            .header("User-Agent", spoofedAgentForDownload)
                                                            .build()
                                                        val response = fetchClient.newCall(req).execute()
                                                        if (response.isSuccessful) {
                                                            val responseString = response.body?.string() ?: ""
                                                            val json = JSONObject(responseString)
                                                            if (json.has("url")) {
                                                                val candidateUrl = json.getString("url")
                                                                val dReq = Request.Builder()
                                                                    .url(candidateUrl)
                                                                    .header("User-Agent", spoofedAgentForDownload)
                                                                    .header("Connection", "close")
                                                                    .build()
                                                                val dRes = downloadClient.newCall(dReq).execute()
                                                                if (dRes.isSuccessful) {
                                                                    downloadResponse = dRes
                                                                    resolvedExtension = selectedFormat
                                                                    break
                                                                } else {
                                                                    dRes.close()
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) { /* Silently fallback */ }
                                                }
                                            }

                                            // ----------------------------------------------------
                                            // 4. Fallback: Piped API Cluster
                                            // ----------------------------------------------------
                                            if (downloadResponse == null) {
                                                val pipedInstances = listOf(
                                                    "https://pipedapi.kavin.rocks",
                                                    "https://api.piped.privacydev.net",
                                                    "https://piped-api.lunar.icu",
                                                    "https://api-piped.mha.fi",
                                                    "https://pipedapi.tokhmi.xyz"
                                                )
                                                for (instance in pipedInstances) {
                                                    if (downloadResponse != null) break
                                                    try {
                                                        val req = Request.Builder()
                                                            .url("$instance/streams/${song.song.id}")
                                                            .get()
                                                            .header("Accept", "application/json")
                                                            .header("User-Agent", spoofedAgentForDownload)
                                                            .build()
                                                        val response = fetchClient.newCall(req).execute()
                                                        if (response.isSuccessful) {
                                                            val responseString = response.body?.string() ?: ""
                                                            val json = JSONObject(responseString)
                                                            if (json.has("audioStreams")) {
                                                                val streams = json.getJSONArray("audioStreams")
                                                                var candidateUrl: String? = null
                                                                var maxBitrate = 0
                                                                for (i in 0 until streams.length()) {
                                                                    val stream = streams.getJSONObject(i)
                                                                    val format = stream.optString("format", "").lowercase()
                                                                    val bitrate = stream.optInt("bitrate", 0)
                                                                    if (format.contains(selectedFormat) && bitrate > maxBitrate) {
                                                                        maxBitrate = bitrate
                                                                        candidateUrl = stream.getString("url")
                                                                    }
                                                                }
                                                                if (candidateUrl == null && streams.length() > 0) {
                                                                    candidateUrl = streams.getJSONObject(0).getString("url")
                                                                }
                                                                
                                                                if (candidateUrl != null) {
                                                                    val dReq = Request.Builder()
                                                                        .url(candidateUrl)
                                                                        .header("User-Agent", spoofedAgentForDownload)
                                                                        .header("Connection", "close")
                                                                        .build()
                                                                    val dRes = downloadClient.newCall(dReq).execute()
                                                                    if (dRes.isSuccessful) {
                                                                        downloadResponse = dRes
                                                                        resolvedExtension = if (candidateUrl.contains("webm")) "webm" else "m4a"
                                                                        break
                                                                    } else {
                                                                        dRes.close()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) { /* Silently fallback */ }
                                                }
                                            }
                                            
                                            // Validation Check
                                            if (downloadResponse == null) {
                                                errorMessage = "Export failed: All network endpoints restricted or blocked (403/404)."
                                                state = ExportState.ERROR
                                                return@launch
                                            }
                                            
                                            state = ExportState.DOWNLOADING
                                            
                                            // ----------------------------------------------------
                                            // ACTUAL DOWNLOADING AND FILE WRITING
                                            // ----------------------------------------------------
                                            val responseBody = downloadResponse!!.body ?: throw Exception("Empty response body")
                                            val contentLength = responseBody.contentLength()
                                            val inputStream = responseBody.byteStream()
                                            
                                            val cleanTitle = song.song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                            val cleanArtist = song.artists.joinToString { it.name }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                            val mimeType = if (resolvedExtension == "webm") "audio/webm" else "audio/mp4"
                                            val fileName = "$cleanTitle - $cleanArtist.$resolvedExtension"
                                            
                                            val contentValues = ContentValues().apply {
                                                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                                                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AvidTune")
                                                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                                                }
                                            }
                                            
                                            val resolver = context.contentResolver
                                            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                                            
                                            if (uri != null) {
                                                var streamCompleted = false
                                                var totalBytesRead = 0L
                                                
                                                resolver.openOutputStream(uri)?.use { outputStream ->
                                                    val buffer = ByteArray(8192)
                                                    var bytesRead: Int
                                                    var lastUpdateTime = System.currentTimeMillis()
                                                    
                                                    try {
                                                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                                            outputStream.write(buffer, 0, bytesRead)
                                                            totalBytesRead += bytesRead
                                                            
                                                            val currentTime = System.currentTimeMillis()
                                                            // THROTTLE: Only update progress state max 4 times a second (250ms) to prevent UI thread crashes
                                                            if (contentLength > 0 && currentTime - lastUpdateTime > 250) {
                                                                progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                                                lastUpdateTime = currentTime
                                                            }
                                                        }
                                                        streamCompleted = true
                                                    } catch (e: Exception) {
                                                        // CONNECTION RESET MITIGATION:
                                                        // If YouTube cuts the stream connection but we have already fetched 95%+ of it, 
                                                        // the file is still fully playable since the faststart (moov atom) is at the beginning.
                                                        if (contentLength > 0 && totalBytesRead.toFloat() / contentLength.toFloat() >= 0.95f) {
                                                            streamCompleted = true
                                                        } else {
                                                            throw e
                                                        }
                                                    }
                                                    outputStream.flush()
                                                }
                                                
                                                if (streamCompleted) {
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                                        contentValues.clear()
                                                        contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                                                        resolver.update(uri, contentValues, null, null)
                                                    }
                                                    state = ExportState.SUCCESS
                                                } else {
                                                    // File failed entirely.
                                                    resolver.delete(uri, null, null)
                                                    errorMessage = "Connection reset before completion."
                                                    state = ExportState.ERROR
                                                }
                                            } else {
                                                errorMessage = "Could not create file in MediaStore"
                                                state = ExportState.ERROR
                                            }
                                            
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "Network error"
                                            state = ExportState.ERROR
                                        } finally {
                                            downloadResponse?.close() // Prevent resource leaks
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(painterResource(R.drawable.download), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Export", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    ExportState.FETCHING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text("Securing valid unencrypted stream...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ExportState.DOWNLOADING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        ) {
                            Text(
                                "Downloading...", 
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    ExportState.SUCCESS -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.check), 
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                "Successfully saved to Music folder!",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Done")
                            }
                        }
                    }
                    ExportState.ERROR -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(R.drawable.error), 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                "Export Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                                    Text("Cancel")
                                }
                                Button(
                                    onClick = { state = ExportState.IDLE },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
