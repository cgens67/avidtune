@file:OptIn(ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.menu

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.ThumbnailCornerRadius
import com.cgens67.avidtune.db.entities.PlaylistSongMap
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.MenuItemData
import com.cgens67.avidtune.ui.component.MenuGroup
import com.cgens67.avidtune.ui.component.NewAction
import com.cgens67.avidtune.ui.component.NewActionGrid
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<Timeline.Window> = emptyList(),
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    // Fallback to currently playing track if opened with an empty list
    val effectiveSongs = remember(songSelection, playerConnection.mediaMetadata) {
        if (songSelection.isNotEmpty()) songSelection
        else listOfNotNull(playerConnection.mediaMetadata.value)
    }

    if (effectiveSongs.isEmpty()) {
        onDismiss()
        return
    }

    val isSingleSong = effectiveSongs.size == 1
    val singleSong = effectiveSongs.first()
    val dbSong by database.song(singleSong.id).collectAsState(initial = null)
    val isLiked = dbSong?.song?.liked ?: singleSong.liked

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    LaunchedEffect(effectiveSongs) {
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (effectiveSongs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (effectiveSongs.all {
                    downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                }
            ) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                effectiveSongs.forEach { songItem ->
                    database.transaction { insert(songItem) }
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, songItem.id)
                    }
                }
            }
            effectiveSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        if (isSingleSong) singleSong.title else "${effectiveSongs.size} songs"
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        effectiveSongs.forEach { songItem ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                songItem.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    // Header: Preview when 1 song, or Badge row when multiple
    if (isSingleSong) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = singleSong.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = singleSong.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = joinByBullet(
                        singleSong.artists.joinToString { it.name },
                        if (singleSong.duration > 0) makeTimeString(singleSong.duration * 1000L) else null
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    database.transaction {
                        val songInDb = getSongById(singleSong.id)
                        if (songInDb != null) {
                            update(songInDb.song.toggleLike())
                        } else {
                            insert(singleSong) { it.toggleLike() }
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (isLiked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.select_all),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.n_song, effectiveSongs.size, effectiveSongs.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // Expressive Action Grid
        item {
            NewActionGrid(
                actions = buildList {
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.play),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = if (isSingleSong) singleSong.title else "Selection",
                                        items = effectiveSongs.map { it.toMediaItem() },
                                    ),
                                )
                                clearAction()
                            }
                        )
                    )

                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = if (isSingleSong) singleSong.title else "Selection",
                                        items = effectiveSongs.shuffled().map { it.toMediaItem() },
                                    ),
                                )
                                clearAction()
                            }
                        )
                    )

                    if (isSingleSong) {
                        add(
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
                                    onDismiss()
                                    playerConnection.playQueue(YouTubeQueue.radio(singleSong))
                                    clearAction()
                                }
                            )
                        )
                    }

                    add(
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
                        )
                    )

                    add(
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
                                onDismiss()
                                val shareText = if (isSingleSong) {
                                    "https://music.youtube.com/watch?v=${singleSong.id}"
                                } else {
                                    effectiveSongs.joinToString("\n") {
                                        "${it.title} - ${it.artists.joinToString { a -> a.name }}: https://music.youtube.com/watch?v=${it.id}"
                                    }
                                }
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
            )
        }

        // Group 1: Queue Playback Additions
        item {
            MenuGroup(
                items = listOf(
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.play_next)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onDismiss()
                            playerConnection.playNext(effectiveSongs.map { it.toMediaItem() })
                            clearAction()
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_queue)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue(effectiveSongs.map { it.toMediaItem() })
                            clearAction()
                        }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Group 2: Queue Removal (if applicable)
        if (currentItems.isNotEmpty()) {
            item {
                MenuGroup(
                    items = listOf(
                        MenuItemData(
                            title = {
                                Text(
                                    text = if (isSingleSong) stringResource(R.string.remove_from_queue)
                                    else stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDismiss()
                                var i = 0
                                val sorted = currentItems.sortedBy { it.firstPeriodIndex }
                                sorted.forEach { cur ->
                                    if (playerConnection.player.availableCommands.contains(Player.COMMAND_CHANGE_MEDIA_ITEMS)) {
                                        playerConnection.player.removeMediaItem(cur.firstPeriodIndex - i++)
                                    }
                                }
                                clearAction()
                            }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Group 3: Downloads & Favorites
        item {
            MenuGroup(
                items = buildList {
                    // Download Option
                    add(
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
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
                                        effectiveSongs.forEach { songItem ->
                                            database.transaction { insert(songItem) }
                                            val downloadRequest = DownloadRequest.Builder(songItem.id, songItem.id.toUri())
                                                .setCustomCacheKey(songItem.id)
                                                .setData(songItem.title.toByteArray())
                                                .build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false,
                                            )
                                        }
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    )

                    // Favorite / Like All Option
                    if (!isSingleSong) {
                        val allLiked = effectiveSongs.all { it.liked }
                        add(
                            MenuItemData(
                                title = {
                                    Text(
                                        text = if (allLiked) stringResource(R.string.dislike_all)
                                        else stringResource(R.string.like_all)
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(if (allLiked) R.drawable.favorite else R.drawable.favorite_border),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (allLiked) MaterialTheme.colorScheme.error else LocalContentColor.current
                                    )
                                },
                                onClick = {
                                    database.query {
                                        if (allLiked) {
                                            effectiveSongs.forEach { songItem ->
                                                update(songItem.toSongEntity().toggleLike())
                                            }
                                        } else {
                                            effectiveSongs.filter { !it.liked }.forEach { songItem ->
                                                update(songItem.toSongEntity().toggleLike())
                                            }
                                        }
                                    }
                                    onDismiss()
                                    clearAction()
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return

    val effectiveSongs = remember(songSelection) { songSelection }
    if (effectiveSongs.isEmpty()) {
        onDismiss()
        return
    }

    val isSingleSong = effectiveSongs.size == 1
    val singleSong = effectiveSongs.first()

    val allInLibrary = remember(effectiveSongs) {
        effectiveSongs.all { it.song.inLibrary != null }
    }

    val allLiked = remember(effectiveSongs) {
        effectiveSongs.all { it.song.liked }
    }

    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(effectiveSongs) {
        downloadUtil.downloads.collect { downloads ->
            downloadState = if (effectiveSongs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (effectiveSongs.all {
                    downloads[it.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.id]?.state == Download.STATE_COMPLETED
                }
            ) {
                Download.STATE_DOWNLOADING
            } else {
                Download.STATE_STOPPED
            }
        }
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            coroutineScope.launch(Dispatchers.IO) {
                effectiveSongs.forEach { songItem ->
                    playlist.playlist.browseId?.let { browseId ->
                        YouTube.addToPlaylist(browseId, songItem.id)
                    }
                }
            }
            effectiveSongs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        if (isSingleSong) singleSong.song.title else "${effectiveSongs.size} songs"
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        effectiveSongs.forEach { songItem ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                songItem.song.id,
                                false,
                            )
                        }
                    },
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    // Header Preview
    if (isSingleSong) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = singleSong.song.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = singleSong.song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = joinByBullet(
                        singleSong.artists.joinToString { it.name },
                        if (singleSong.song.duration > 0) makeTimeString(singleSong.song.duration * 1000L) else null
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = {
                    database.query {
                        update(singleSong.song.toggleLike())
                    }
                }
            ) {
                Icon(
                    painter = painterResource(if (singleSong.song.liked) R.drawable.favorite else R.drawable.favorite_border),
                    tint = if (singleSong.song.liked) MaterialTheme.colorScheme.error else LocalContentColor.current,
                    contentDescription = null,
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.select_all),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.n_song, effectiveSongs.size, effectiveSongs.size),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.selected),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(modifier = Modifier.height(8.dp))

    LazyColumn(
        contentPadding = PaddingValues(
            start = 0.dp,
            top = 0.dp,
            end = 0.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        item {
            NewActionGrid(
                actions = buildList {
                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.play),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = if (isSingleSong) singleSong.song.title else "Selection",
                                        items = effectiveSongs.map { it.toMediaItem() },
                                    ),
                                )
                                clearAction()
                            }
                        )
                    )

                    add(
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.shuffle),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            text = stringResource(R.string.shuffle),
                            onClick = {
                                onDismiss()
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = if (isSingleSong) singleSong.song.title else "Selection",
                                        items = effectiveSongs.shuffled().map { it.toMediaItem() },
                                    ),
                                )
                                clearAction()
                            }
                        )
                    )

                    if (isSingleSong) {
                        add(
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
                                    onDismiss()
                                    playerConnection.playQueue(YouTubeQueue.radio(singleSong.toMediaMetadata()))
                                    clearAction()
                                }
                            )
                        )
                    }

                    add(
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
                        )
                    )

                    add(
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
                                onDismiss()
                                val shareText = if (isSingleSong) {
                                    "https://music.youtube.com/watch?v=${singleSong.id}"
                                } else {
                                    effectiveSongs.joinToString("\n") {
                                        "${it.song.title} - ${it.artists.joinToString { a -> a.name }}: https://music.youtube.com/watch?v=${it.id}"
                                    }
                                }
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp)
            )
        }

        // Group 1: Playback
        item {
            MenuGroup(
                items = listOf(
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.play_next)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.playlist_play),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onDismiss()
                            playerConnection.playNext(effectiveSongs.map { it.toMediaItem() })
                            clearAction()
                        }
                    ),
                    MenuItemData(
                        title = { Text(text = stringResource(R.string.add_to_queue)) },
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            onDismiss()
                            playerConnection.addToQueue(effectiveSongs.map { it.toMediaItem() })
                            clearAction()
                        }
                    )
                )
            )
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // Group 2: Playlist Position Removal (if in playlist)
        if (!songPosition.isNullOrEmpty()) {
            item {
                MenuGroup(
                    items = listOf(
                        MenuItemData(
                            title = {
                                Text(
                                    text = stringResource(R.string.delete),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.delete),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                onDismiss()
                                var i = 0
                                database.query {
                                    songPosition.forEach { cur ->
                                        move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                                        delete(cur.copy(position = Int.MAX_VALUE))
                                        i++
                                    }
                                }
                                clearAction()
                            }
                        )
                    )
                )
            }

            item { Spacer(modifier = Modifier.height(10.dp)) }
        }

        // Group 3: Library & Downloads
        item {
            MenuGroup(
                items = buildList {
                    // Library Toggle
                    add(
                        MenuItemData(
                            title = {
                                Text(
                                    text = if (allInLibrary) stringResource(R.string.remove_from_library)
                                    else stringResource(R.string.add_to_library)
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (allInLibrary) R.drawable.library_add_check
                                        else R.drawable.library_add
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = {
                                database.transaction {
                                    if (allInLibrary) {
                                        effectiveSongs.forEach { songItem ->
                                            inLibrary(songItem.id, null)
                                        }
                                    } else {
                                        effectiveSongs.forEach { songItem ->
                                            insert(songItem.toMediaMetadata())
                                            inLibrary(songItem.id, LocalDateTime.now())
                                        }
                                    }
                                }
                                onDismiss()
                                clearAction()
                            }
                        )
                    )

                    // Download Toggle
                    add(
                        when (downloadState) {
                            Download.STATE_COMPLETED -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.remove_download)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
                                )
                            }
                            Download.STATE_QUEUED, Download.STATE_DOWNLOADING -> {
                                MenuItemData(
                                    title = { Text(text = stringResource(R.string.downloading)) },
                                    icon = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    },
                                    onClick = { showRemoveDownloadDialog = true }
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
                                        effectiveSongs.forEach { songItem ->
                                            val downloadRequest = DownloadRequest.Builder(songItem.id, songItem.id.toUri())
                                                .setCustomCacheKey(songItem.id)
                                                .setData(songItem.song.title.toByteArray())
                                                .build()
                                            DownloadService.sendAddDownload(
                                                context,
                                                ExoDownloadService::class.java,
                                                downloadRequest,
                                                false,
                                            )
                                        }
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    )

                    // Like All / Dislike All
                    if (!isSingleSong) {
                        add(
                            MenuItemData(
                                title = {
                                    Text(
                                        text = if (allLiked) stringResource(R.string.dislike_all)
                                        else stringResource(R.string.like_all)
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter = painterResource(if (allLiked) R.drawable.favorite else R.drawable.favorite_border),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = if (allLiked) MaterialTheme.colorScheme.error else LocalContentColor.current
                                    )
                                },
                                onClick = {
                                    database.query {
                                        effectiveSongs.forEach { songItem ->
                                            if ((!allLiked && !songItem.song.liked) || allLiked) {
                                                update(songItem.song.toggleLike())
                                            }
                                        }
                                    }
                                    onDismiss()
                                    clearAction()
                                }
                            )
                        )
                    }
                }
            )
        }
    }
}
