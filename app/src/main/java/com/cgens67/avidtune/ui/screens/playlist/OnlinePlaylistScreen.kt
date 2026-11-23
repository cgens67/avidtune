package com.cgens67.avidtune.ui.screens.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
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
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.db.entities.PlaylistEntity
import com.cgens67.avidtune.db.entities.PlaylistSongMap
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.PlayingIndicatorBox
import com.cgens67.avidtune.ui.component.VerticalFastScroller
import com.cgens67.avidtune.ui.menu.SelectionMediaMetadataMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.OnlinePlaylistViewModel
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun OnlinePlaylistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }
    val downloads by downloadUtil.downloads.collectAsState()

    LaunchedEffect(songs, downloads) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadState =
            if (songs.all { downloads[it.id]?.state == Download.STATE_COMPLETED }) {
                Download.STATE_COMPLETED
            } else if (songs.all {
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

    var showRemoveDownloadDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(
                        R.string.remove_download_playlist_confirm,
                        playlist?.title.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp)
                )
            },
            buttons = {
                TextButton(onClick = { showRemoveDownloadDialog = false }) {
                    Text(text = stringResource(android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            DownloadService.sendRemoveDownload(
                                context,
                                ExoDownloadService::class.java,
                                song.id,
                                false
                            )
                        }
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
                song.title.contains(query.text, ignoreCase = true) ||
                    song.artists.fastAny { it.name.contains(query.text, ignoreCase = true) }
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

    LaunchedEffect(playlist?.id) {
        if (playlist?.id != null) {
            lazyListState.scrollToItem(0)
        }
    }

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val isCurrentPlaylist = remember(mediaMetadata, songs) {
        val currentId = mediaMetadata?.id
        currentId != null && songs.any { it.id == currentId }
    }

    // Dynamic gradient mesh background extracted from playlist artwork
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val playlistThumbnail = playlist?.thumbnail ?: songs.firstOrNull()?.thumbnail

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
        val currentPlaylist = playlist
        if (currentPlaylist != null) {
            if (isPlaying && isCurrentPlaylist) {
                playerConnection.player.pause()
            } else if (isCurrentPlaylist) {
                playerConnection.player.play()
            } else {
                currentPlaylist.playEndpoint?.let {
                    playerConnection.playQueue(YouTubeQueue(it))
                } ?: if (songs.isNotEmpty()) {
                    playerConnection.playQueue(
                        ListQueue(
                            title = currentPlaylist.title,
                            items = songs.map { it.toMediaItem() }
                        )
                    )
                } else Unit
            }
        }
    }

    val onShuffleClick: () -> Unit = {
        val currentPlaylist = playlist
        if (currentPlaylist != null) {
            currentPlaylist.shuffleEndpoint?.let {
                playerConnection.playQueue(YouTubeQueue(it))
            } ?: if (songs.isNotEmpty()) {
                playerConnection.playQueue(
                    ListQueue(
                        title = currentPlaylist.title,
                        items = songs.shuffled().map { it.toMediaItem() }
                    )
                )
            } else Unit
        }
    }

    val onBookmarkClick: () -> Unit = {
        val currentPlaylist = playlist
        if (currentPlaylist != null && currentPlaylist.id != "LM") {
            if (dbPlaylist?.playlist == null) {
                database.transaction {
                    val playlistEntity = PlaylistEntity(
                        name = currentPlaylist.title,
                        browseId = currentPlaylist.id,
                        isEditable = currentPlaylist.isEditable,
                        playEndpointParams = currentPlaylist.playEndpoint?.params,
                        shuffleEndpointParams = currentPlaylist.shuffleEndpoint?.params,
                        radioEndpointParams = currentPlaylist.radioEndpoint?.params
                    ).toggleLike()
                    insert(playlistEntity)
                    songs.map(SongItem::toMediaMetadata)
                        .onEach(::insert)
                        .mapIndexed { index, song ->
                            PlaylistSongMap(
                                songId = song.id,
                                playlistId = playlistEntity.id,
                                position = index
                            )
                        }
                        .forEach(::insert)
                }
            } else {
                database.transaction {
                    update(dbPlaylist!!.playlist.toggleLike())
                }
            }
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
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
                            .setData(song.title.toByteArray())
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

    val onRadioClick: () -> Unit = {
        playlist?.radioEndpoint?.let { radioEndpoint ->
            playerConnection.playQueue(YouTubeQueue(radioEndpoint, isRadio = true))
        }
    }

    val onShareClick: () -> Unit = {
        playlist?.let { currentPlaylist ->
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, currentPlaylist.shareLink)
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    val onMoreOptionsClick: () -> Unit = {
        playlist?.let { currentPlaylist ->
            menuState.show {
                YouTubePlaylistMenu(
                    playlist = currentPlaylist,
                    songs = songs,
                    coroutineScope = coroutineScope,
                    onDismiss = menuState::dismiss,
                    selectAction = { selection = true },
                    canSelect = true,
                )
            }
        }
    }

    val playlistSongsList: LazyListScope.(PlaylistItem) -> Unit = { currentPlaylist ->
        if (!isSearching && wrappedSongs.isNotEmpty()) {
            item(key = "tracklist_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.songs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentPlaylist.songCountText
                            ?: pluralStringResource(R.plurals.n_song, songs.size, songs.size),
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
                        query = query.text,
                        title = stringResource(R.string.no_results_found),
                        subtitle = stringResource(R.string.try_another_term)
                    )
                }
            }
        } else if (wrappedSongs.isNotEmpty()) {
            itemsIndexed(
                items = wrappedSongs,
                key = { index, songWrapper -> "${songWrapper.item.id}_$index" },
            ) { index, songWrapper ->
                val song = songWrapper.item
                val isFirst = index == 0
                val isLast = index == wrappedSongs.size - 1
                val isActive = song.id == mediaMetadata?.id

                val dbSong by database.song(song.id).collectAsState(initial = null)
                val songDownload = downloads[song.id]

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
                        .alpha(if (hideExplicit && song.explicit) 0.3f else 1f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ListItemHeight)
                            .clip(cardShape)
                            .background(rowBackgroundColor)
                            .combinedClickable(
                                enabled = !hideExplicit || !song.explicit,
                                onClick = {
                                    if (!selection) {
                                        if (isActive) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.service.getAutomix(playlistId = currentPlaylist.id)
                                            playerConnection.playQueue(
                                                YouTubeQueue(
                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                    song.toMediaMetadata(),
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
                                    model = song.thumbnail.resize(200, 200),
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

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = song.title,
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
                                    if (dbSong?.song?.liked == true) {
                                        Icon(
                                            painter = painterResource(R.drawable.favorite),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (song.explicit) {
                                        Icon(
                                            painter = painterResource(R.drawable.explicit),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (dbSong?.song?.inLibrary != null) {
                                        Icon(
                                            painter = painterResource(R.drawable.library_add_check),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (songDownload?.state == Download.STATE_COMPLETED) {
                                        Icon(
                                            painter = painterResource(R.drawable.offline),
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }

                                    Text(
                                        text = joinByBullet(
                                            song.artists.joinToString { it.name },
                                            song.duration?.let { makeTimeString(it * 1000L) },
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    menuState.show {
                                        YouTubeSongMenu(
                                            song = song,
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

        val currentPlaylist = playlist
        if (currentPlaylist != null) {
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
                        OnlinePlaylistHeaderContent(
                            playlist = currentPlaylist,
                            songs = songs,
                            isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null,
                            artworkSize = 160.dp,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            coverResolution = coverResolution,
                            isPlaying = isPlaying,
                            isCurrentPlaylist = isCurrentPlaylist,
                            downloadState = downloadState,
                            navController = navController,
                            onPlayClick = onPlayClick,
                            onShuffleClick = onShuffleClick,
                            onBookmarkClick = onBookmarkClick,
                            onDownloadClick = onDownloadClick,
                            onRadioClick = onRadioClick,
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
                                OnlinePlaylistHeaderContent(
                                    playlist = currentPlaylist,
                                    songs = songs,
                                    isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null,
                                    artworkSize = 280.dp,
                                    thumbnailCornerRadius = thumbnailCornerRadius,
                                    coverResolution = coverResolution,
                                    isPlaying = isPlaying,
                                    isCurrentPlaylist = isCurrentPlaylist,
                                    downloadState = downloadState,
                                    navController = navController,
                                    onPlayClick = onPlayClick,
                                    onShuffleClick = onShuffleClick,
                                    onBookmarkClick = onBookmarkClick,
                                    onDownloadClick = onDownloadClick,
                                    onRadioClick = onRadioClick,
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
                                        SelectionMediaMetadataMenu(
                                            songSelection = wrappedSongs.filter { it.isSelected }
                                                .mapNotNull { it.item.toMediaItem().metadata },
                                            onDismiss = menuState::dismiss,
                                            clearAction = {
                                                selection = false
                                                wrappedSongs.forEach { it.isSelected = false }
                                            },
                                            currentItems = emptyList()
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

                        if (currentPlaylist != null) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                IconButton(onClick = onMoreOptionsClick) {
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OnlinePlaylistHeaderContent(
    playlist: PlaylistItem,
    songs: List<SongItem>,
    isBookmarked: Boolean,
    artworkSize: Dp,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    isPlaying: Boolean,
    isCurrentPlaylist: Boolean,
    downloadState: Int,
    navController: NavController,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onRadioClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val horizontalPadding = if (artworkSize <= 160.dp) 20.dp else 32.dp

    val totalDuration = remember(songs) { songs.sumOf { it.duration ?: 0 } }
    val hours = totalDuration / 3600
    val minutes = (totalDuration % 3600) / 60
    val durationStr = if (totalDuration > 0) {
        if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    } else null

    val artworkUrl = playlist.thumbnail ?: songs.firstOrNull()?.thumbnail

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
                IconButton(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            savePlaylistImageToGallery(
                                context,
                                artworkUrl.resize(coverResolution.size, coverResolution.size),
                                playlist.title
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
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(artworkSize * 0.4f)
                    )
                }
            }
        }

        // Title
        Text(
            text = playlist.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp)
        )

        // Author (clickable if ID available)
        playlist.author?.let { artist ->
            Text(
                text = buildAnnotatedString {
                    if (artist.id != null) {
                        val link = LinkAnnotation.Clickable(artist.id!!) {
                            navController.navigate("artist/${artist.id!!}")
                        }
                        withLink(link) {
                            append(artist.name)
                        }
                    } else {
                        append(artist.name)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .padding(top = 4.dp)
            )
        }

        // Meta line
        Text(
            text = listOfNotNull(
                stringResource(R.string.filter_playlists),
                playlist.songCountText ?: if (songs.isNotEmpty()) {
                    pluralStringResource(R.plurals.n_song, songs.size, songs.size)
                } else null,
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
            if (playlist.id != "LM") {
                IconButton(onClick = onBookmarkClick) {
                    Icon(
                        painter = painterResource(
                            if (isBookmarked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = null,
                        tint = if (isBookmarked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

            if (playlist.radioEndpoint != null) {
                IconButton(onClick = onRadioClick) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = stringResource(R.string.radio),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onShareClick) {
                Icon(
                    painter = painterResource(R.drawable.share),
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onMoreOptionsClick) {
                Icon(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}