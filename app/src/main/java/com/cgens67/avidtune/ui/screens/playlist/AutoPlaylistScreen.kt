package com.cgens67.avidtune.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.cgens67.avidtune.constants.SongSortDescendingKey
import com.cgens67.avidtune.constants.SongSortType
import com.cgens67.avidtune.constants.SongSortTypeKey
import com.cgens67.avidtune.constants.YtmSyncKey
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.PlaylistEntity
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.EmptyPlaceholder
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.component.SortHeader
import com.cgens67.avidtune.ui.component.VerticalFastScroller
import com.cgens67.avidtune.ui.menu.AddToPlaylistDialog
import com.cgens67.avidtune.ui.menu.PlaylistMenu
import com.cgens67.avidtune.ui.menu.SelectionSongMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.AutoPlaylistViewModel
import com.cgens67.innertube.YouTube
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

enum class AutoPlaylistType {
    LIKE, DOWNLOAD, UPLOADED, OTHER
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AutoPlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AutoPlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val coroutineScope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val (sortType, onSortTypeChange) = rememberEnumPreference(
        SongSortTypeKey,
        SongSortType.CREATE_DATE
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        SongSortDescendingKey,
        true
    )

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    val playlistTitle = when (viewModel.playlist) {
        "liked" -> stringResource(R.string.liked)
        "uploaded" -> stringResource(R.string.uploaded_playlist)
        else -> stringResource(R.string.offline)
    }

    val playlistType = when (viewModel.playlist) {
        "liked" -> AutoPlaylistType.LIKE
        "downloaded" -> AutoPlaylistType.DOWNLOAD
        "uploaded" -> AutoPlaylistType.UPLOADED
        else -> AutoPlaylistType.OTHER
    }

    val songs by viewModel.likedSongs.collectAsState(null)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }

    // Sync in background for liked songs
    LaunchedEffect(Unit) {
        if (ytmSync && playlistType == AutoPlaylistType.LIKE) {
            launch(Dispatchers.IO) {
                delay(300)
                viewModel.syncLikedSongs()
            }
        }
    }

    // Observe download states
    LaunchedEffect(songs) {
        val songList = songs
        if (songList.isNullOrEmpty()) return@LaunchedEffect

        downloadUtil.downloads.collect { downloads ->
            downloadState = if (songList.all { downloads[it.song.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songList.all {
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

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(R.string.remove_download_playlist_confirm, playlistTitle),
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
                        songs?.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.song.id,
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

    val filteredSongs = remember(songs, searchQuery.text) {
        val query = searchQuery.text.trim()
        if (query.isEmpty()) {
            songs.orEmpty()
        } else {
            songs.orEmpty().filter { song ->
                song.song.title.contains(query, ignoreCase = true) ||
                        song.artists.any { it.name.contains(query, ignoreCase = true) }
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
            searchQuery = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
            wrappedSongs.forEach { it.isSelected = false }
        }
    }

    val lazyListState = rememberLazyListState()
    val landscapeScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val isCurrentPlaylist = remember(mediaMetadata, songs) {
        val currentId = mediaMetadata?.id
        currentId != null && songs?.any { it.song.id == currentId } == true
    }

    // Dynamic gradient mesh background calculation extracted from artwork
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val playlistThumbnail = songs?.firstOrNull()?.song?.thumbnailUrl

    LaunchedEffect(playlistThumbnail) {
        if (playlistThumbnail != null) {
            val request = ImageRequest.Builder(context)
                .data(playlistThumbnail)
                .size(100, 100)
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request).drawable
            }.getOrNull()

            if (result != null) {
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
        val songList = songs
        if (!songList.isNullOrEmpty()) {
            if (isPlaying && isCurrentPlaylist) {
                playerConnection.player.pause()
            } else if (isCurrentPlaylist) {
                playerConnection.player.play()
            } else {
                playerConnection.playQueue(
                    ListQueue(
                        title = playlistTitle,
                        items = songList.map { it.toMediaItem() }
                    )
                )
            }
        }
    }

    val onShuffleClick: () -> Unit = {
        val songList = songs
        if (!songList.isNullOrEmpty()) {
            playerConnection.playQueue(
                ListQueue(
                    title = playlistTitle,
                    items = songList.shuffled().map { it.toMediaItem() }
                )
            )
        }
    }

    val onDownloadClick: () -> Unit = {
        val songList = songs
        if (!songList.isNullOrEmpty()) {
            when (downloadState) {
                Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                    showRemoveDownloadDialog = true
                }
                else -> {
                    songList.forEach { song ->
                        val downloadRequest = DownloadRequest
                            .Builder(song.song.id, song.song.id.toUri())
                            .setCustomCacheKey(song.song.id)
                            .setData(song.song.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                    }
                }
            }
        }
    }

    val onLikeClick: () -> Unit = {
        if (playlistType == AutoPlaylistType.LIKE) {
            viewModel.syncLikedSongs()
            Toast.makeText(context, R.string.playlist_synced, Toast.LENGTH_SHORT).show()
        }
    }

    var showAddToPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    AddToPlaylistDialog(
        isVisible = showAddToPlaylistDialog,
        onGetSong = { targetPlaylist ->
            val songIds = songs?.map { it.song.id }.orEmpty()
            coroutineScope.launch(Dispatchers.IO) {
                targetPlaylist.playlist.browseId?.let { browseId ->
                    songIds.forEach { songId ->
                        YouTube.addToPlaylist(browseId, songId)
                    }
                }
            }
            songIds
        },
        onDismiss = { showAddToPlaylistDialog = false }
    )

    val onAddToPlaylistClick: () -> Unit = {
        showAddToPlaylistDialog = true
    }

    val onMoreOptionsClick: () -> Unit = {
        val fakePlaylist = Playlist(
            playlist = PlaylistEntity(
                id = viewModel.playlist,
                name = playlistTitle
            ),
            songCount = songs?.size ?: 0,
            thumbnails = emptyList()
        )
        menuState.show {
            PlaylistMenu(
                playlist = fakePlaylist,
                coroutineScope = coroutineScope,
                onDismiss = menuState::dismiss,
                autoPlaylist = true,
                downloadPlaylist = playlistType == AutoPlaylistType.DOWNLOAD,
                songList = songs
            )
        }
    }

    val playlistSongsList: LazyListScope.() -> Unit = {
        if (!isSearching && wrappedSongs.isNotEmpty()) {
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
                                SongSortType.CREATE_DATE -> R.string.sort_by_create_date
                                SongSortType.NAME -> R.string.sort_by_name
                                SongSortType.ARTIST -> R.string.sort_by_artist
                                SongSortType.PLAY_TIME -> R.string.sort_by_play_time
                            }
                        }
                    )
                    Text(
                        text = pluralStringResource(R.plurals.n_song, songs?.size ?: 0, songs?.size ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        query = searchQuery.text,
                        title = stringResource(R.string.no_results_found),
                        subtitle = stringResource(R.string.try_another_term)
                    )
                }
            }
        } else if (wrappedSongs.isNotEmpty()) {
            itemsIndexed(
                items = wrappedSongs,
                key = { _, song -> song.item.song.id },
            ) { index, songWrapper ->
                val isFirst = index == 0
                val isLast = index == wrappedSongs.size - 1
                val isActive = songWrapper.item.song.id == mediaMetadata?.id

                val cardShape = RoundedCornerShape(
                    topStart = if (isFirst) 20.dp else 4.dp,
                    topEnd = if (isFirst) 20.dp else 4.dp,
                    bottomStart = if (isLast) 20.dp else 4.dp,
                    bottomEnd = if (isLast) 20.dp else 4.dp
                )

                val rowBackgroundColor = when {
                    songWrapper.isSelected && selection -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    isActive -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainer
                }

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
                                onClick = {
                                    if (!selection) {
                                        if (songWrapper.item.song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = playlistTitle,
                                                    items = filteredSongs.map { it.toMediaItem() },
                                                    startIndex = filteredSongs.indexOf(songWrapper.item)
                                                ),
                                            )
                                        }
                                    } else {
                                        songWrapper.isSelected = !songWrapper.isSelected
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
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            // Compact, modern pill badge for the rank number
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

                            // Song Thumbnail with Playing Indicator / Selection Badge
                            Box(
                                modifier = Modifier
                                    .size(ListThumbnailSize)
                                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                            ) {
                                AsyncImage(
                                    model = songWrapper.item.song.thumbnailUrl?.resize(200, 200),
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

                                if (songWrapper.isSelected && selection) {
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

                            // Song Title, Badges and Artists
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = songWrapper.item.song.title,
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
                                    if (songWrapper.item.song.liked) {
                                        Icon(
                                            painter = painterResource(R.drawable.favorite),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (songWrapper.item.song.inLibrary != null) {
                                        Icon(
                                            painter = painterResource(R.drawable.library_add_check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    val isSongExplicit = songWrapper.item.song.title.contains("explicit", ignoreCase = true)
                                    if (isSongExplicit) {
                                        Icon(
                                            painter = painterResource(R.drawable.explicit),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = joinByBullet(
                                            songWrapper.item.artists.joinToString { it.name },
                                            makeTimeString(songWrapper.item.song.duration * 1000L)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // More Options Button
                            IconButton(
                                onClick = {
                                    menuState.show {
                                        SongMenu(
                                            originalSong = songWrapper.item,
                                            navController = navController,
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.more_vert),
                                    contentDescription = null,
                                    tint = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // Gradient background layer matching AlbumScreen
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

        if (songs != null) {
            if (songs!!.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyPlaceholder(
                        icon = R.drawable.music_note,
                        text = stringResource(R.string.playlist_is_empty),
                    )
                }
            } else {
                if (isLandscape && !isSearching) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .padding(
                                    start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(layoutDirection) + 16.dp,
                                    bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp
                                )
                                .verticalScroll(landscapeScrollState),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AutoPlaylistHeaderContent(
                                playlistTitle = playlistTitle,
                                playlistType = playlistType,
                                songs = songs!!,
                                artworkSize = 160.dp,
                                thumbnailCornerRadius = thumbnailCornerRadius,
                                coverResolution = coverResolution,
                                isPlaying = isPlaying,
                                isCurrentPlaylist = isCurrentPlaylist,
                                downloadState = downloadState,
                                onPlayClick = onPlayClick,
                                onShuffleClick = onShuffleClick,
                                onLikeClick = onLikeClick,
                                onDownloadClick = onDownloadClick,
                                onAddToPlaylistClick = onAddToPlaylistClick,
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
                                        bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
                                        end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(layoutDirection) + 16.dp
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    playlistSongsList()
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
                            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (!isSearching) {
                                item(key = "playlist_header") {
                                    AutoPlaylistHeaderContent(
                                        playlistTitle = playlistTitle,
                                        playlistType = playlistType,
                                        songs = songs!!,
                                        artworkSize = 280.dp,
                                        thumbnailCornerRadius = thumbnailCornerRadius,
                                        coverResolution = coverResolution,
                                        isPlaying = isPlaying,
                                        isCurrentPlaylist = isCurrentPlaylist,
                                        downloadState = downloadState,
                                        onPlayClick = onPlayClick,
                                        onShuffleClick = onShuffleClick,
                                        onLikeClick = onLikeClick,
                                        onDownloadClick = onDownloadClick,
                                        onAddToPlaylistClick = onAddToPlaylistClick,
                                        onMoreOptionsClick = onMoreOptionsClick,
                                        modifier = Modifier.background(Color.Transparent)
                                    )
                                }
                            }

                            playlistSongsList()
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                ContainedLoadingIndicator()
            }
        }

        // Top App Bar
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
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
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
                                        if (searchQuery.text.isEmpty()) {
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

                            if (searchQuery.text.isNotEmpty()) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = stringResource(R.string.clear),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .clickable { searchQuery = TextFieldValue() }
                                )
                            }
                        }
                    }
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
                                searchQuery = TextFieldValue()
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
                                            songSelection = wrappedSongs.filter { it.isSelected }.map { it.item },
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

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val fakePlaylist = Playlist(
                                        playlist = PlaylistEntity(
                                            id = viewModel.playlist,
                                            name = playlistTitle
                                        ),
                                        songCount = songs?.size ?: 0,
                                        thumbnails = emptyList()
                                    )
                                    menuState.show {
                                        PlaylistMenu(
                                            playlist = fakePlaylist,
                                            coroutineScope = coroutineScope,
                                            onDismiss = menuState::dismiss,
                                            autoPlaylist = true,
                                            downloadPlaylist = playlistType == AutoPlaylistType.DOWNLOAD,
                                            songList = songs
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent,
                scrolledContainerColor = if (isSearching) MaterialTheme.colorScheme.surface else Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun EmptySearchResultsView(
    query: String = "",
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.no_results_found),
    subtitle: String = stringResource(R.string.try_another_term)
) {
    var visible by remember(query) { mutableStateOf(false) }
    LaunchedEffect(query) {
        visible = true
    }

    val iconScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emptyIconScale"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "emptyContentAlpha"
    )

    val contentOffsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "emptyContentOffsetY"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "emptyBob")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = contentAlpha
                translationY = contentOffsetY.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(iconScale)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.search),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer { translationY = floatOffset },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutoPlaylistHeaderContent(
    playlistTitle: String,
    playlistType: AutoPlaylistType,
    songs: List<Song>,
    artworkSize: Dp,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    isPlaying: Boolean,
    isCurrentPlaylist: Boolean,
    downloadState: Int,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val horizontalPadding = if (artworkSize <= 160.dp) 20.dp else 32.dp

    val totalDuration = remember(songs) { songs.sumOf { it.song.duration } }
    val hours = totalDuration / 3600
    val minutes = (totalDuration % 3600) / 60
    val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

    val artworkUrl = songs.firstOrNull()?.song?.thumbnailUrl

    val descriptionText = when (playlistType) {
        AutoPlaylistType.LIKE -> "Your favorite tracks all in one place. Songs you've liked across YouTube Music, automatically synced and ready to play."
        AutoPlaylistType.DOWNLOAD -> "All your downloaded songs available offline. Listen anytime, anywhere without an internet connection."
        AutoPlaylistType.UPLOADED -> "Your personal music collection. Songs you've uploaded to YouTube Music from your library."
        else -> "Your curated collection of songs, perfectly organized for your listening pleasure."
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
        ) {
            if (artworkUrl != null) {
                AsyncImage(
                    model = artworkUrl.resize(coverResolution.size, coverResolution.size),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        painter = painterResource(
                            when (playlistType) {
                                AutoPlaylistType.LIKE -> R.drawable.favorite
                                AutoPlaylistType.DOWNLOAD -> R.drawable.download
                                else -> R.drawable.queue_music
                            }
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(artworkSize * 0.4f)
                    )
                }
            }

            if (artworkUrl != null) {
                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            savePlaylistImageToGallery(
                                context,
                                artworkUrl.resize(coverResolution.size, coverResolution.size),
                                playlistTitle
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
        Row(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = playlistTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Subtitle line (Auto Playlist type)
        Text(
            text = stringResource(R.string.auto_playlist),
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
                pluralStringResource(R.plurals.n_song, songs.size, songs.size),
                durationStr
            ).joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        // Action Play / Shuffle Buttons with Morphing Shape (No push-in/scale)
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
                    text = if (isPlaying && isCurrentPlaylist) stringResource(R.string.media3_controls_pause_description) else stringResource(R.string.play),
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
            IconButton(onClick = onLikeClick) {
                Icon(
                    painter = painterResource(
                        if (playlistType == AutoPlaylistType.LIKE) R.drawable.favorite
                        else R.drawable.favorite_border
                    ),
                    contentDescription = null,
                    tint = if (playlistType == AutoPlaylistType.LIKE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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

            // Option B: Add to Playlist Action Button
            IconButton(onClick = onAddToPlaylistClick) {
                Icon(
                    painter = painterResource(R.drawable.playlist_add),
                    contentDescription = stringResource(R.string.add_to_playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoreOptionsClick) {
                Icon(painterResource(R.drawable.more_vert), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

suspend fun savePlaylistImageToGallery(context: Context, imageUrl: String, playlistTitle: String) {
    try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            val displayName = "${playlistTitle.replace(" ", "_")}.png"
            val mimeType = "image/png"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val contentResolver = context.contentResolver
            val uri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Save Picture Success",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    } catch (e: IOException) {
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Error saving image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
