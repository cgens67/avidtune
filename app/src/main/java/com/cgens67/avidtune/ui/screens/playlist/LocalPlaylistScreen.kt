package com.cgens67.avidtune.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.PlaylistEditLockKey
import com.cgens67.avidtune.constants.PlaylistSongSortDescendingKey
import com.cgens67.avidtune.constants.PlaylistSongSortType
import com.cgens67.avidtune.constants.PlaylistSongSortTypeKey
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.PlaylistSong
import com.cgens67.avidtune.db.entities.PlaylistSongMap
import com.cgens67.avidtune.extensions.move
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.component.SortHeader
import com.cgens67.avidtune.ui.component.TextFieldDialog
import com.cgens67.avidtune.ui.component.VerticalFastScroller
import com.cgens67.avidtune.ui.menu.PlaylistMenu
import com.cgens67.avidtune.ui.menu.SelectionSongMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.deletePlaylistImage
import com.cgens67.avidtune.utils.getPlaylistImageUri
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.utils.saveCustomPlaylistImage
import com.cgens67.avidtune.viewmodels.LocalPlaylistViewModel
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.utils.completed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.time.LocalDateTime

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun LocalPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: LocalPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val mutableSongs = remember { mutableStateListOf<PlaylistSong>() }
    val playlistLength = remember(songs) {
        songs.fastSumBy { it.song.song.duration }
    }

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSongSortTypeKey,
        PlaylistSongSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSongSortDescendingKey,
        true
    )
    var locked by rememberPreference(PlaylistEditLockKey, defaultValue = true)

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    val filteredSongs = remember(songs, query.text) {
        if (query.text.isEmpty()) {
            songs
        } else {
            songs.filter { song ->
                song.song.song.title.contains(query.text, ignoreCase = true) ||
                    song.song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
            }
        }
    }

    val wrappedSongs = remember(filteredSongs) {
        filteredSongs.map { item -> ItemWrapper(item, initialSelected = false) }
    }
    var selection by remember { mutableStateOf(false) }

    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
            wrappedSongs.forEach { it.isSelected = false }
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    val editable: Boolean = playlist?.playlist?.isEditable == true

    LaunchedEffect(songs) {
        mutableSongs.apply {
            clear()
            addAll(songs)
        }
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it.song.id]?.state == Download.STATE_QUEUED ||
                            downloads[it.song.id]?.state == Download.STATE_DOWNLOADING ||
                            downloads[it.song.id]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    if (showEditDialog) {
        playlist?.playlist?.let { playlistEntity ->
            TextFieldDialog(
                icon = {
                    Icon(painter = painterResource(R.drawable.edit), contentDescription = null)
                },
                title = { Text(text = stringResource(R.string.edit_playlist)) },
                onDismiss = { showEditDialog = false },
                initialTextFieldValue = TextFieldValue(
                    playlistEntity.name,
                    TextRange(playlistEntity.name.length)
                ),
                onDone = { name ->
                    database.query {
                        update(
                            playlistEntity.copy(
                                name = name,
                                lastUpdateTime = LocalDateTime.now()
                            )
                        )
                    }
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        playlistEntity.browseId?.let { YouTube.renamePlaylist(it, name) }
                    }
                },
            )
        }
    }

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist?.playlist?.name.orEmpty()
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
                        if (!editable) {
                            database.transaction {
                                playlist?.id?.let { clearPlaylist(it) }
                            }
                        }
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
                                false
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.delete_playlist_confirm,
                        playlist?.playlist?.name.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showDeletePlaylistDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        database.query {
                            playlist?.let { delete(it.playlist) }
                        }
                        viewModel.viewModelScope.launch(Dispatchers.IO) {
                            playlist?.playlist?.browseId?.let { YouTube.deletePlaylist(it) }
                        }
                        navController.popBackStack()
                    }
                ) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        )
    }

    val lazyListState = rememberLazyListState()
    val landscapeScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val headerCount = when {
        isSearching -> 0
        isLandscape -> 1
        else -> 2
    }
    val currentHeaderCount by rememberUpdatedState(headerCount)

    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
    ) { from, to ->
        val offset = currentHeaderCount
        if (to.index >= offset && from.index >= offset) {
            val currentDragInfo = dragInfo
            dragInfo = if (currentDragInfo == null) {
                (from.index - offset) to (to.index - offset)
            } else {
                currentDragInfo.first to (to.index - offset)
            }
            val safeFrom = (from.index - offset).coerceIn(0, mutableSongs.lastIndex.coerceAtLeast(0))
            val safeTo = (to.index - offset).coerceIn(0, mutableSongs.lastIndex.coerceAtLeast(0))
            if (safeFrom != safeTo && mutableSongs.isNotEmpty()) {
                mutableSongs.move(safeFrom, safeTo)
            }
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging) {
            dragInfo?.let { (from, to) ->
                database.transaction {
                    move(viewModel.playlistId, from, to)
                }

                viewModel.playlist.value?.playlist?.browseId?.let { browseId ->
                    viewModel.viewModelScope.launch(Dispatchers.IO) {
                        val playlistSongMap = database.playlistSongMaps(viewModel.playlistId, 0)
                        val successorIndex = if (from > to) to else to + 1
                        val successorSetVideoId = playlistSongMap.getOrNull(successorIndex)?.setVideoId

                        playlistSongMap.getOrNull(from)?.setVideoId?.let { setVideoId ->
                            YouTube.moveSongPlaylist(
                                browseId,
                                setVideoId,
                                successorSetVideoId.toString()
                            )
                        }
                    }
                }
                dragInfo = null
            }
        }
    }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val currentPlaylist = playlist
    var customThumbnailUri by rememberSaveable(currentPlaylist?.id) {
        mutableStateOf<Uri?>(currentPlaylist?.let { getPlaylistImageUri(context, it.playlist.id) })
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val p = playlist ?: return@rememberLauncherForActivityResult
        uri?.let {
            saveCustomPlaylistImage(context, p.playlist.id, it)
            customThumbnailUri = getPlaylistImageUri(context, p.playlist.id) ?: it
        }
    }

    val isCurrentPlaylist = remember(mediaMetadata, songs) {
        val currentId = mediaMetadata?.id
        currentId != null && songs.any { it.song.id == currentId }
    }

    // Dynamic gradient mesh background extracted from artwork
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val playlistThumbnailModel: Any? = customThumbnailUri
        ?: currentPlaylist?.thumbnails?.firstOrNull()
        ?: songs.firstOrNull()?.song?.song?.thumbnailUrl

    LaunchedEffect(playlistThumbnailModel) {
        if (playlistThumbnailModel != null) {
            val request = ImageRequest.Builder(context)
                .data(playlistThumbnailModel)
                .size(100, 100)
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request).drawable
            }.getOrNull()

            val bitmap = (result as? BitmapDrawable)?.bitmap
            if (bitmap != null) {
                val palette = withContext(Dispatchers.Default) {
                    Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()
                }
                gradientColors = PlayerColorExtractor.extractGradientColors(
                    palette = palette,
                    fallbackColor = fallbackColor
                )
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val gradientAlpha by remember {
        derivedStateOf {
            if (isLandscape) {
                val offset = landscapeScrollState.value
                (1f - (offset / 500f)).coerceIn(0f, 1f)
            } else {
                if (lazyListState.firstVisibleItemIndex == 0) {
                    val offset = lazyListState.firstVisibleItemScrollOffset
                    (1f - (offset / 800f)).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }
    }

    val onPlayClick: () -> Unit = {
        val playlistData = playlist
        if (playlistData != null && songs.isNotEmpty()) {
            if (isPlaying && isCurrentPlaylist) {
                playerConnection.player.pause()
            } else if (isCurrentPlaylist) {
                playerConnection.player.play()
            } else {
                playerConnection.playQueue(
                    ListQueue(
                        title = playlistData.playlist.name,
                        items = songs.map { it.song.toMediaItem() }
                    )
                )
            }
        }
    }

    val onShuffleClick: () -> Unit = {
        val playlistData = playlist
        if (playlistData != null && songs.isNotEmpty()) {
            playerConnection.playQueue(
                ListQueue(
                    title = playlistData.playlist.name,
                    items = songs.shuffled().map { it.song.toMediaItem() }
                )
            )
        }
    }

    val onDownloadClick: () -> Unit = {
        if (songs.isNotEmpty()) {
            when (downloadState) {
                Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                    showRemoveDownloadDialog = true
                }
                else -> {
                    songs.forEach { song ->
                        val downloadRequest = DownloadRequest
                            .Builder(song.song.id, song.song.id.toUri())
                            .setCustomCacheKey(song.song.id)
                            .setData(song.song.song.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false
                        )
                    }
                }
            }
        }
    }

    val onSyncClick: () -> Unit = {
        val playlistData = playlist
        if (playlistData?.playlist?.browseId != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val playlistPage = YouTube.playlist(playlistData.playlist.browseId)
                    .completed()
                    .getOrNull() ?: return@launch
                database.transaction {
                    clearPlaylist(playlistData.id)
                    playlistPage.songs
                        .map(SongItem::toMediaMetadata)
                        .onEach(::insert)
                        .mapIndexed { position, song ->
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistData.id,
                                position = position,
                                setVideoId = song.setVideoId
                            )
                        }
                        .forEach(::insert)
                }
            }
            coroutineScope.launch(Dispatchers.Main) {
                snackbarHostState.showSnackbar(context.getString(R.string.playlist_synced))
            }
        }
    }

    val onShareClick: () -> Unit = {
        val shareLink = playlist?.playlist?.shareLink
        if (shareLink != null) {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareLink)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    val onMoreOptionsClick: () -> Unit = {
        playlist?.let { playlistData ->
            menuState.show {
                PlaylistMenu(
                    playlist = playlistData,
                    coroutineScope = coroutineScope,
                    onDismiss = menuState::dismiss,
                )
            }
        }
    }

    val useReorderableList = !isSearching && !selection && sortType == PlaylistSongSortType.CUSTOM && !locked && editable

    val playlistSongsList: LazyListScope.(Playlist) -> Unit = { playlistData ->
        if (!isSearching) {
            item(key = "tracklist_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SortHeader(
                        sortType = sortType,
                        sortDescending = sortDescending,
                        onSortTypeChange = onSortTypeChange,
                        onSortDescendingChange = onSortDescendingChange,
                        sortTypeText = { type ->
                            when (type) {
                                PlaylistSongSortType.CUSTOM -> R.string.sort_by_custom
                                PlaylistSongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                PlaylistSongSortType.NAME -> R.string.sort_by_name
                                PlaylistSongSortType.ARTIST -> R.string.sort_by_artist
                                PlaylistSongSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (editable && sortType == PlaylistSongSortType.CUSTOM) {
                            FilledIconToggleButton(
                                checked = locked,
                                onCheckedChange = { locked = it },
                                colors = IconButtonDefaults.filledIconToggleButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    painter = painterResource(if (locked) R.drawable.lock else R.drawable.lock_open),
                                    contentDescription = if (locked) "Locked" else "Unlocked"
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        if (isSearching && filteredSongs.isEmpty()) {
            item(key = "empty_search_results") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 64.dp, horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptySearchResultsView(
                        query = query.text,
                        title = stringResource(R.string.no_results_found),
                        subtitle = stringResource(R.string.try_another_term)
                    )
                }
            }
        } else if (useReorderableList) {
            itemsIndexed(
                items = mutableSongs,
                key = { _, song -> song.map.id },
            ) { index, song ->
                ReorderableItem(
                    state = reorderableState,
                    key = song.map.id,
                ) {
                    val currentItem by rememberUpdatedState(song)
                    val isFirst = index == 0
                    val isLast = index == mutableSongs.size - 1
                    val isActive = song.song.id == mediaMetadata?.id

                    LocalPlaylistSegmentedSongRow(
                        index = index,
                        playlistSong = currentItem,
                        playlistBrowseId = playlistData.playlist.browseId,
                        isFirst = isFirst,
                        isLast = isLast,
                        isActive = isActive,
                        isPlaying = isPlaying,
                        isSelected = false,
                        selection = false,
                        showDragHandle = true,
                        thumbnailCornerRadius = thumbnailCornerRadius,
                        dragModifier = Modifier.draggableHandle(),
                        onClick = {
                            if (isActive) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = playlistData.playlist.name,
                                        items = mutableSongs.map { it.song.toMediaItem() },
                                        startIndex = index,
                                    )
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            selection = true
                            wrappedSongs.forEach { it.isSelected = it.item.map.id == currentItem.map.id }
                        },
                        onMoreClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = currentItem.song,
                                    playlistSong = currentItem,
                                    playlistBrowseId = playlistData.playlist.browseId,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        }
                    )
                }
            }
        } else {
            itemsIndexed(
                items = wrappedSongs,
                key = { _, song -> song.item.map.id },
            ) { index, songWrapper ->
                val isFirst = index == 0
                val isLast = index == wrappedSongs.size - 1
                val isActive = songWrapper.item.song.id == mediaMetadata?.id

                LocalPlaylistSegmentedSongRow(
                    index = index,
                    playlistSong = songWrapper.item,
                    playlistBrowseId = playlistData.playlist.browseId,
                    isFirst = isFirst,
                    isLast = isLast,
                    isActive = isActive,
                    isPlaying = isPlaying,
                    isSelected = songWrapper.isSelected && selection,
                    selection = selection,
                    showDragHandle = false,
                    thumbnailCornerRadius = thumbnailCornerRadius,
                    dragModifier = Modifier,
                    onClick = {
                        if (!selection) {
                            if (isActive) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = playlistData.playlist.name,
                                        items = filteredSongs.map { it.song.toMediaItem() },
                                        startIndex = index,
                                    )
                                )
                            }
                        } else {
                            songWrapper.isSelected = !songWrapper.isSelected
                            if (wrappedSongs.none { it.isSelected }) {
                                selection = false
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (!selection) {
                            selection = true
                        }
                        wrappedSongs.forEach { it.isSelected = false }
                        songWrapper.isSelected = true
                    },
                    onMoreClick = {
                        menuState.show {
                            SongMenu(
                                originalSong = songWrapper.item.song,
                                playlistSong = songWrapper.item,
                                playlistBrowseId = playlistData.playlist.browseId,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // Dynamic Palette radial/mesh gradient background
        if (gradientColors.isNotEmpty() && gradientAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(if (isLandscape) 1f else 0.75f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawBehind {
                        val width = size.width
                        val height = size.height
                        val isLightMode = surfaceColor.luminance() > 0.5f

                        fun adaptColor(c: Color, alphaFactor: Float): Color {
                            val a = (gradientAlpha * alphaFactor).coerceIn(0f, 1f)
                            return if (isLightMode) {
                                if (c.luminance() < 0.25f) c.copy(alpha = a * 0.32f)
                                else c.copy(alpha = a * 0.55f)
                            } else {
                                c.copy(alpha = a)
                            }
                        }

                        if (isLandscape) {
                            val leftPaneWidth = width * 0.42f
                            val centerX = leftPaneWidth * 0.5f
                            val centerY = (140.dp.toPx()).coerceAtMost(height * 0.42f)
                            val radius = maxOf(height * 0.85f, leftPaneWidth * 0.95f)

                            if (gradientColors.size >= 3) {
                                val c0 = gradientColors[0]
                                val c1 = gradientColors[1]
                                val c2 = gradientColors[2]

                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c0, 0.75f),
                                            adaptColor(c0, 0.35f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX, centerY),
                                        radius = radius
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c1, 0.55f),
                                            adaptColor(c1, 0.20f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX * 0.4f, centerY * 0.65f),
                                        radius = radius * 0.8f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c2, 0.50f),
                                            adaptColor(c2, 0.18f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX * 1.5f, centerY * 1.35f),
                                        radius = radius * 0.85f
                                    )
                                )
                            } else if (gradientColors.isNotEmpty()) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(gradientColors[0], 0.65f),
                                            adaptColor(gradientColors[0], 0.25f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX, centerY),
                                        radius = radius
                                    )
                                )
                            }

                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.5f),
                                        surfaceColor,
                                        surfaceColor
                                    ),
                                    startX = leftPaneWidth * 0.65f,
                                    endX = leftPaneWidth * 1.05f
                                )
                            )

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.6f),
                                        surfaceColor
                                    ),
                                    startY = height * 0.62f,
                                    endY = height
                                )
                            )
                        } else {
                            val centerY = (160.dp.toPx()).coerceAtMost(height * 0.28f)
                            val radius = width * 0.75f

                            if (gradientColors.size >= 3) {
                                val c0 = gradientColors[0]
                                val c1 = gradientColors[1]
                                val c2 = gradientColors[2]
                                val c3 = gradientColors.getOrElse(3) { c0 }
                                val c4 = gradientColors.getOrElse(4) { c1 }

                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c0, 0.72f),
                                            adaptColor(c0, 0.40f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, centerY),
                                        radius = radius
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c1, 0.56f),
                                            adaptColor(c1, 0.30f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.15f, centerY * 1.5f),
                                        radius = width * 0.6f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c2, 0.52f),
                                            adaptColor(c2, 0.26f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.85f, centerY * 1.5f),
                                        radius = width * 0.65f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c3, 0.34f),
                                            adaptColor(c3, 0.18f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.35f, centerY * 2.1f),
                                        radius = width * 0.8f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c4, 0.28f),
                                            adaptColor(c4, 0.14f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.55f, centerY * 2.6f),
                                        radius = width * 0.95f
                                    )
                                )
                            } else if (gradientColors.isNotEmpty()) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(gradientColors[0], 0.60f),
                                            adaptColor(gradientColors[0], 0.30f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.5f, centerY),
                                        radius = width * 0.8f
                                    )
                                )
                            }

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.3f),
                                        surfaceColor.copy(alpha = 0.7f),
                                        surfaceColor
                                    ),
                                    startY = height * 0.35f,
                                    endY = height * 0.85f
                                )
                            )
                        }
                    }
            )
        }

        if (currentPlaylist != null) {
            if (currentPlaylist.songCount == 0 && currentPlaylist.playlist.remoteSongCount == 0 && songs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                        .padding(top = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.music_note),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.playlist_is_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (isLandscape && !isSearching) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .padding(
                                    start = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                                        .calculateStartPadding(layoutDirection) + 16.dp,
                                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                                        .calculateBottomPadding() + 16.dp
                                )
                                .verticalScroll(landscapeScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LocalPlaylistHeaderContent(
                                playlist = currentPlaylist,
                                songs = songs,
                                playlistLength = playlistLength,
                                customThumbnailUri = customThumbnailUri,
                                editable = editable,
                                artworkSize = 160.dp,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                coverResolution = coverResolution,
                                isPlaying = isPlaying,
                                isCurrentPlaylist = isCurrentPlaylist,
                                downloadState = downloadState,
                                onPickThumbnail = { imagePickerLauncher.launch("image/*") },
                                onRemoveThumbnail = {
                                    deletePlaylistImage(context, currentPlaylist.playlist.id)
                                    customThumbnailUri = null
                                },
                                onPlayClick = onPlayClick,
                                onShuffleClick = onShuffleClick,
                                onPrimaryActionClick = {
                                    if (editable) {
                                        showDeletePlaylistDialog = true
                                    } else {
                                        database.transaction {
                                            update(currentPlaylist.playlist.toggleLike())
                                        }
                                    }
                                },
                                onDownloadClick = onDownloadClick,
                                onAddToQueueClick = {
                                    playerConnection.addToQueue(songs.map { it.song.toMediaItem() })
                                },
                                onEditClick = { showEditDialog = true },
                                onSyncClick = onSyncClick,
                                onShareClick = onShareClick,
                                onMoreOptionsClick = onMoreOptionsClick,
                                modifier = Modifier.background(Color.Transparent)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                        ) {
                            VerticalFastScroller(
                                listState = lazyListState,
                                topContentPadding = 16.dp,
                                endContentPadding = 0.dp
                            ) {
                                LazyColumn(
                                    state = lazyListState,
                                    contentPadding = PaddingValues(
                                        top = 56.dp,
                                        bottom = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime)
                                            .asPaddingValues().calculateBottomPadding() + 24.dp,
                                        end = LocalPlayerAwareWindowInsets.current.asPaddingValues()
                                            .calculateEndPadding(layoutDirection) + 16.dp
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    playlistSongsList(currentPlaylist)
                                }
                            }
                        }
                    }
                } else {
                    VerticalFastScroller(
                        listState = lazyListState,
                        topContentPadding = 16.dp,
                        endContentPadding = 0.dp
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            contentPadding = LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime).asPaddingValues(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!isSearching) {
                                item(key = "playlist_header") {
                                    LocalPlaylistHeaderContent(
                                        playlist = currentPlaylist,
                                        songs = songs,
                                        playlistLength = playlistLength,
                                        customThumbnailUri = customThumbnailUri,
                                        editable = editable,
                                        artworkSize = 280.dp,
                                        thumbnailCornerRadius = thumbnailCornerRadius,
                                        coverResolution = coverResolution,
                                        isPlaying = isPlaying,
                                        isCurrentPlaylist = isCurrentPlaylist,
                                        downloadState = downloadState,
                                        onPickThumbnail = { imagePickerLauncher.launch("image/*") },
                                        onRemoveThumbnail = {
                                            deletePlaylistImage(context, currentPlaylist.playlist.id)
                                            customThumbnailUri = null
                                        },
                                        onPlayClick = onPlayClick,
                                        onShuffleClick = onShuffleClick,
                                        onPrimaryActionClick = {
                                            if (editable) {
                                                showDeletePlaylistDialog = true
                                            } else {
                                                database.transaction {
                                                    update(currentPlaylist.playlist.toggleLike())
                                                }
                                            }
                                        },
                                        onDownloadClick = onDownloadClick,
                                        onAddToQueueClick = {
                                            playerConnection.addToQueue(songs.map { it.song.toMediaItem() })
                                        },
                                        onEditClick = { showEditDialog = true },
                                        onSyncClick = onSyncClick,
                                        onShareClick = onShareClick,
                                        onMoreOptionsClick = onMoreOptionsClick,
                                        modifier = Modifier.background(Color.Transparent)
                                    )
                                }
                            }

                            playlistSongsList(currentPlaylist)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                    .padding(top = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        }

        // Top App Bar with floating pill styling
        TopAppBar(
            title = {
                if (selection) {
                    val count = wrappedSongs.count { it.isSelected }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.n_song, count, count),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                } else if (isSearching) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (query.text.isEmpty()) {
                                            Text(
                                                text = stringResource(R.string.search),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (query.text.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { query = TextFieldValue() }
                                )
                            }
                        }
                    }
                } else if (currentPlaylist?.songCount == 0 && currentPlaylist.playlist.remoteSongCount == 0 && songs.isEmpty()) {
                    Text(
                        text = currentPlaylist.playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    com.cgens67.avidtune.ui.component.IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                query = TextFieldValue()
                                focusManager.clearFocus()
                            } else if (selection) {
                                selection = false
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!selection && !isSearching) {
                                navController.backToMain()
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (selection || isSearching) R.drawable.close else R.drawable.arrow_back
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                val enterTransition = fadeIn(tween(350)) +
                    expandHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) +
                    scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))

                val exitTransition = fadeOut(tween(200)) +
                    shrinkHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) +
                    scaleOut(targetScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))

                AnimatedVisibility(
                    visible = selection,
                    enter = enterTransition,
                    exit = exitTransition
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val count = wrappedSongs.count { it.isSelected }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val selectAll = count != wrappedSongs.size
                                    wrappedSongs.forEach { it.isSelected = selectAll }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (count == wrappedSongs.size) R.drawable.deselect else R.drawable.select_all
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SelectionSongMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }
                                                .map { it.item.song },
                                            songPosition = wrappedSongs.filter { it.isSelected }
                                                .map { it.item.map },
                                            onDismiss = menuState::dismiss,
                                            clearAction = {
                                                selection = false
                                                wrappedSongs.forEach { it.isSelected = false }
                                            }
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !selection && !isSearching,
                    enter = enterTransition,
                    exit = exitTransition
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (songs.isNotEmpty()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                IconButton(onClick = { isSearching = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        currentPlaylist?.let { playlistData ->
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            PlaylistMenu(
                                                playlist = playlistData,
                                                coroutineScope = coroutineScope,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.union(WindowInsets.ime))
                .align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LocalPlaylistSegmentedSongRow(
    index: Int,
    playlistSong: PlaylistSong,
    playlistBrowseId: String?,
    isFirst: Boolean,
    isLast: Boolean,
    isActive: Boolean,
    isPlaying: Boolean,
    isSelected: Boolean,
    selection: Boolean,
    showDragHandle: Boolean,
    thumbnailCornerRadius: Float,
    dragModifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(
        topStart = if (isFirst) 20.dp else 4.dp,
        topEnd = if (isFirst) 20.dp else 4.dp,
        bottomStart = if (isLast) 20.dp else 4.dp,
        bottomEnd = if (isLast) 20.dp else 4.dp
    )

    val rowBackgroundColor = when {
        isSelected && selection -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        isActive -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainer
    }

    val song = playlistSong.song

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListItemHeight)
                .clip(cardShape)
                .background(rowBackgroundColor)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier
                        .height(24.dp)
                        .widthIn(min = 28.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(ListThumbnailSize)
                        .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                ) {
                    AsyncImage(
                        model = song.song.thumbnailUrl?.resize(200, 200),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    if (isActive) {
                        PlayingIndicatorBox(
                            isActive = true,
                            playWhenReady = isPlaying,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        )
                    }

                    if (isSelected && selection) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f))
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = song.song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (song.song.liked) {
                            Icon(
                                painter = painterResource(R.drawable.favorite),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (song.song.inLibrary != null) {
                            Icon(
                                painter = painterResource(R.drawable.library_add_check),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (song.song.title.contains("explicit", ignoreCase = true)) {
                            Icon(
                                painter = painterResource(R.drawable.explicit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            text = joinByBullet(
                                song.artists.joinToString { it.name },
                                makeTimeString(song.song.duration * 1000L)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onMoreClick) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showDragHandle) {
                    IconButton(
                        onClick = {},
                        modifier = dragModifier
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.drag_handle),
                            contentDescription = stringResource(R.string.drag_to_reorder),
                            tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (!isLast) {
            Spacer(modifier = Modifier.height(3.dp))
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LocalPlaylistHeaderContent(
    playlist: Playlist,
    songs: List<PlaylistSong>,
    playlistLength: Int,
    customThumbnailUri: Uri?,
    editable: Boolean,
    artworkSize: Dp,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    isPlaying: Boolean,
    isCurrentPlaylist: Boolean,
    downloadState: Int,
    onPickThumbnail: () -> Unit,
    onRemoveThumbnail: () -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPrimaryActionClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onEditClick: () -> Unit,
    onSyncClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val horizontalPadding = if (artworkSize <= 160.dp) 20.dp else 32.dp

    val hours = playlistLength / 3600
    val minutes = (playlistLength % 3600) / 60
    val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    val trackCount = if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
        playlist.playlist.remoteSongCount ?: 0
    } else {
        playlist.songCount
    }

    val fallbackArtworkUrl = playlist.thumbnails.firstOrNull()
        ?: songs.firstOrNull()?.song?.song?.thumbnailUrl

    val descriptionText = buildString {
        append(stringResource(R.string.playlist_description_start))
        append(" ")
        append(pluralStringResource(R.plurals.n_song, trackCount, trackCount))
        append(". ")

        if (hours > 0 && minutes > 0) {
            append(stringResource(R.string.playlist_duration_description))
            append(" $hours ")
            append(if (hours > 1) stringResource(R.string.hours) else stringResource(R.string.hour))
            append(" ")
            append(stringResource(R.string.and))
            append(" $minutes ")
            append(if (minutes > 1) stringResource(R.string.minutes) else stringResource(R.string.minute))
            append(" ")
            append(stringResource(R.string.of_music))
        } else if (hours > 0) {
            append(stringResource(R.string.playlist_duration_description))
            append(" $hours ")
            append(if (hours > 1) stringResource(R.string.hours) else stringResource(R.string.hour))
            append(" ")
            append(stringResource(R.string.of_music))
        } else if (minutes > 0) {
            append(stringResource(R.string.playlist_duration_description))
            append(" $minutes ")
            append(if (minutes > 1) stringResource(R.string.minutes) else stringResource(R.string.minute))
            append(" ")
            append(stringResource(R.string.of_music))
        }

        append(". ")
        append(stringResource(R.string.playlist_enjoy_message))
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // Artwork
        Box(
            modifier = Modifier
                .size(artworkSize)
                .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                .clickable(enabled = editable, onClick = onPickThumbnail)
        ) {
            when {
                customThumbnailUri != null -> {
                    AsyncImage(
                        model = customThumbnailUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                playlist.thumbnails.size == 1 -> {
                    AsyncImage(
                        model = playlist.thumbnails[0].resize(coverResolution.size, coverResolution.size),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                playlist.thumbnails.size > 1 -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        listOf(
                            Alignment.TopStart,
                            Alignment.TopEnd,
                            Alignment.BottomStart,
                            Alignment.BottomEnd,
                        ).fastForEachIndexed { index, alignment ->
                            AsyncImage(
                                model = playlist.thumbnails.getOrNull(index)
                                    ?.resize(coverResolution.size / 2, coverResolution.size / 2),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .align(alignment)
                                    .fillMaxSize(0.5f)
                            )
                        }
                    }
                }

                else -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.queue_music),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier = Modifier.size(artworkSize * 0.4f)
                        )
                    }
                }
            }

            if (editable) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (customThumbnailUri != null) {
                        IconButton(
                            onClick = onRemoveThumbnail,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f))
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = stringResource(R.string.remove_thumbnail),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onPickThumbnail,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = stringResource(R.string.edit_thumbnail),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else if (fallbackArtworkUrl != null) {
                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            savePlaylistImageToGallery(
                                context,
                                fallbackArtworkUrl.resize(coverResolution.size, coverResolution.size),
                                playlist.playlist.name
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.download),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Title
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp)
                .then(
                    if (editable) Modifier.clickable(onClick = onEditClick) else Modifier
                )
        )

        // Subtitle
        Text(
            text = stringResource(
                if (playlist.playlist.browseId != null) R.string.synced_songs
                else R.string.local_playlist
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        // Meta line
        Text(
            text = listOfNotNull(
                stringResource(R.string.filter_playlists),
                pluralStringResource(R.plurals.n_song, trackCount, trackCount),
                durationStr
            ).joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        // Action Play / Shuffle Buttons with Morphing Shape
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val playInteractionSource = remember { MutableInteractionSource() }
            val isPlayPressed by playInteractionSource.collectIsPressedAsState()
            val playCornerRadius by animateDpAsState(
                targetValue = if (isPlayPressed) 28.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "playCornerRadius"
            )

            Button(
                onClick = onPlayClick,
                interactionSource = playInteractionSource,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(playCornerRadius),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    painter = painterResource(if (isPlaying && isCurrentPlaylist) R.drawable.pause else R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isPlaying && isCurrentPlaylist) {
                        stringResource(R.string.media3_controls_pause_description)
                    } else {
                        stringResource(R.string.play)
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            val shuffleInteractionSource = remember { MutableInteractionSource() }
            val isShufflePressed by shuffleInteractionSource.collectIsPressedAsState()
            val shuffleCornerRadius by animateDpAsState(
                targetValue = if (isShufflePressed) 28.dp else 16.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "shuffleCornerRadius"
            )

            FilledTonalButton(
                onClick = onShuffleClick,
                interactionSource = shuffleInteractionSource,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(shuffleCornerRadius)
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.shuffle),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Icon Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrimaryActionClick) {
                Icon(
                    painter = painterResource(
                        when {
                            editable -> R.drawable.delete
                            playlist.playlist.bookmarkedAt != null -> R.drawable.favorite
                            else -> R.drawable.favorite_border
                        }
                    ),
                    contentDescription = if (editable) stringResource(R.string.delete) else stringResource(R.string.save),
                    tint = when {
                        !editable && playlist.playlist.bookmarkedAt != null -> MaterialTheme.colorScheme.error
                        editable -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            IconButton(onClick = onDownloadClick) {
                when (downloadState) {
                    Download.STATE_COMPLETED -> {
                        Icon(painterResource(R.drawable.offline), null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Download.STATE_DOWNLOADING -> {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                    else -> {
                        Icon(painterResource(R.drawable.download), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            IconButton(onClick = onAddToQueueClick) {
                Icon(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = stringResource(R.string.add_to_queue_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (editable) {
                IconButton(onClick = onEditClick) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (playlist.playlist.browseId != null) {
                IconButton(onClick = onSyncClick) {
                    Icon(
                        painter = painterResource(R.drawable.sync),
                        contentDescription = stringResource(R.string.sync),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (playlist.playlist.shareLink != null) {
                IconButton(onClick = onShareClick) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = stringResource(R.string.share),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Expandable Description Section
        var isDescExpanded by rememberSaveable { mutableStateOf(false) }
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clickable { isDescExpanded = !isDescExpanded }
                .animateContentSize(),
            maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.height(12.dp))
    }
}