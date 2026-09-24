package com.cgens67.avidtune.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
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
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.AlbumWithSongs
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.LocalAlbumRadio
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.SelectionSongMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.viewmodels.AlbumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun AlbumScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val scope = rememberCoroutineScope()

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    val playlistId by viewModel.playlistId.collectAsState()
    val albumWithSongs by viewModel.albumWithSongs.collectAsState()
    val otherVersions by viewModel.otherVersions.collectAsState()
    val albumDescription by viewModel.albumDescription.collectAsState()

    val wrappedSongs = albumWithSongs?.songs?.map { item -> ItemWrapper(item) }?.toMutableList()
    var selection by remember { mutableStateOf(false) }

    if (selection) {
        BackHandler { selection = false }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember { mutableIntStateOf(Download.STATE_STOPPED) }

    var isDescriptionLoading by remember { mutableStateOf(true) }

    LaunchedEffect(albumWithSongs?.album?.id) {
        if (albumWithSongs != null) {
            if (albumDescription == null) {
                isDescriptionLoading = true
                delay(8000)
                isDescriptionLoading = false
            } else {
                isDescriptionLoading = false
            }
        }
    }

    LaunchedEffect(albumDescription) {
        if (albumDescription != null) {
            isDescriptionLoading = false
        }
    }

    LaunchedEffect(albumWithSongs) {
        val songs = albumWithSongs?.songs?.map { it.id }
        if (songs.isNullOrEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it]?.state == Download.STATE_COMPLETED }) {
                    Download.STATE_COMPLETED
                } else if (songs.all {
                        downloads[it]?.state == Download.STATE_QUEUED ||
                                downloads[it]?.state == Download.STATE_DOWNLOADING ||
                                downloads[it]?.state == Download.STATE_COMPLETED
                    }
                ) {
                    Download.STATE_DOWNLOADING
                } else {
                    Download.STATE_STOPPED
                }
        }
    }

    val lazyListState = rememberLazyListState()
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

    val albumData = albumWithSongs
    val isCurrentAlbum = mediaMetadata?.album?.id == albumData?.album?.id

    val onPlayClick: () -> Unit = {
        albumData?.let {
            if (isPlaying && isCurrentAlbum) {
                playerConnection.player.pause()
            } else if (isCurrentAlbum) {
                playerConnection.player.play()
            } else {
                playerConnection.service.getAutomix(playlistId)
                playerConnection.playQueue(LocalAlbumRadio(it))
            }
        }
    }

    val onShuffleClick: () -> Unit = {
        albumData?.let {
            playerConnection.service.getAutomix(playlistId)
            playerConnection.playQueue(LocalAlbumRadio(it.copy(songs = it.songs.shuffled())))
        }
    }

    val onDownloadClick: () -> Unit = {
        albumData?.let {
            when (downloadState) {
                Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                    it.songs.forEach { song ->
                        DownloadService.sendRemoveDownload(
                            context,
                            ExoDownloadService::class.java,
                            song.id,
                            false,
                        )
                    }
                }
                else -> {
                    it.songs.forEach { song ->
                        val downloadRequest = DownloadRequest
                            .Builder(song.id, song.id.toUri())
                            .setCustomCacheKey(song.id)
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
        albumData?.let {
            database.query {
                update(it.album.toggleLike())
            }
        }
    }

    val onShareClick: () -> Unit = {
        albumData?.let {
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(
                    Intent.EXTRA_TEXT,
                    context.getString(
                        R.string.check_out_album_share,
                        it.album.title,
                        it.artists.joinToString { artist -> artist.name },
                        "https://music.youtube.com/playlist?list=${it.album.playlistId}"
                    )
                )
            }
            context.startActivity(Intent.createChooser(intent, null))
        }
    }

    val onMoreOptionsClick: () -> Unit = {
        albumData?.let {
            menuState.show {
                AlbumMenu(
                    originalAlbum = Album(it.album, it.artists),
                    navController = navController,
                    onDismiss = menuState::dismiss,
                )
            }
        }
    }

    val albumSongsAndOtherVersions: LazyListScope.() -> Unit = {
        if (!wrappedSongs.isNullOrEmpty()) {
            items(
                items = wrappedSongs,
                key = { song -> song.item.id },
            ) { songWrapper ->
                SongListItem(
                    song = songWrapper.item,
                    albumIndex = wrappedSongs.indexOf(songWrapper) + 1,
                    isActive = songWrapper.item.id == mediaMetadata?.id,
                    isPlaying = isPlaying,
                    showInLibraryIcon = true,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    SongMenu(
                                        originalSong = songWrapper.item,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                            )
                        }
                    },
                    isSelected = songWrapper.isSelected && selection,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (!selection) {
                                    if (songWrapper.item.id == mediaMetadata?.id) {
                                        playerConnection.player.togglePlayPause()
                                    } else {
                                        playerConnection.service.getAutomix(playlistId)
                                        playerConnection.playQueue(
                                            LocalAlbumRadio(
                                                albumData!!,
                                                startIndex = wrappedSongs.indexOf(songWrapper)
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
                        ),
                )
            }
        }

        if (otherVersions.isNotEmpty()) {
            item(key = "other_versions_title") {
                NavigationTitle(
                    title = stringResource(R.string.other_versions),
                    modifier = Modifier.animateItem()
                )
            }
            item(key = "other_versions_list") {
                LazyRow(
                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                ) {
                    items(
                        items = otherVersions.distinctBy { it.id },
                        key = { it.id },
                    ) { item ->
                        YouTubeGridItem(
                            item = item,
                            isActive = mediaMetadata?.album?.id == item.id,
                            isPlaying = isPlaying,
                            coroutineScope = scope,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = { navController.navigate("album/${item.id}") },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = menuState::dismiss,
                                            )
                                        }
                                    },
                                )
                                .animateItem(),
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (albumData != null && albumData.songs.isNotEmpty()) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Left Pane: Album presentation with fitted cover & quick actions
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                            .padding(
                                start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(layoutDirection) + 16.dp,
                                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 16.dp
                            )
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(56.dp))

                        // Compact Artwork
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                        ) {
                            AsyncImage(
                                model = albumData.album.thumbnailUrl?.resize(coverResolution.size, coverResolution.size),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        albumData.album.thumbnailUrl?.let {
                                            saveAlbumImageToGallery(
                                                context,
                                                it.resize(coverResolution.size, coverResolution.size),
                                                albumData.album.title
                                            )
                                        }
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

                        // Title
                        Text(
                            text = albumData.album.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp)
                        )

                        // Artists
                        Text(
                            text = buildAnnotatedString {
                                albumData.artists.fastForEachIndexed { index, artist ->
                                    val link = LinkAnnotation.Clickable(artist.id) {
                                        navController.navigate("artist/${artist.id}")
                                    }
                                    withLink(link) {
                                        append(artist.name)
                                    }
                                    if (index != albumData.artists.lastIndex) {
                                        append(", ")
                                    }
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp)
                        )

                        // Meta line
                        val totalDuration = albumData.songs.sumOf { it.song.duration }
                        val hours = totalDuration / 3600
                        val minutes = (totalDuration % 3600) / 60
                        val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                        Text(
                            text = listOfNotNull(
                                stringResource(R.string.album_text),
                                albumData.album.year?.toString(),
                                "${albumData.songs.size} tracks",
                                durationStr
                            ).joinToString(" • "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 4.dp)
                        )

                        // Action Play / Shuffle Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    painter = painterResource(if (isPlaying && isCurrentAlbum) R.drawable.pause else R.drawable.play),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isPlaying && isCurrentAlbum) stringResource(R.string.media3_controls_pause_description) else stringResource(R.string.play),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            FilledTonalButton(
                                onClick = onShuffleClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
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
                                .padding(horizontal = 16.dp)
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onLikeClick) {
                                Icon(
                                    painter = painterResource(
                                        if (albumData.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                    ),
                                    contentDescription = null,
                                    tint = if (albumData.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
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
                            IconButton(onClick = onShareClick) {
                                Icon(painterResource(R.drawable.share), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = onMoreOptionsClick) {
                                Icon(painterResource(R.drawable.more_vert), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Description
                        if (isDescriptionLoading) {
                            ShimmerHost(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Spacer(modifier = Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp)))
                                    Spacer(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp)))
                                }
                            }
                        } else {
                            var isDescExpanded by rememberSaveable { mutableStateOf(false) }
                            val staticDesc = "${albumData.album.title} is an album by ${albumData.artists.joinToString { it.name }}."
                            val desc = albumDescription ?: staticDesc
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                                    .clickable { isDescExpanded = !isDescExpanded }
                                    .animateContentSize(),
                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Pane: Track list & other versions
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = 56.dp,
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
                            end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(layoutDirection) + 16.dp
                        )
                    ) {
                        item(key = "tracklist_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.songs),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.n_song, albumData.songs.size, albumData.songs.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        albumSongsAndOtherVersions()
                    }
                }
            } else {
                // Portrait Layout (preserved)
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "album_header") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(Modifier.height(50.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 48.dp)
                            ) {
                                AsyncImage(
                                    model = albumData.album.thumbnailUrl?.resize(coverResolution.size, coverResolution.size),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                androidx.compose.material3.IconButton(
                                    onClick = {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            albumData.album.thumbnailUrl?.let {
                                                saveAlbumImageToGallery(
                                                    context,
                                                    it.resize(coverResolution.size, coverResolution.size),
                                                    albumData.album.title
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.download),
                                        contentDescription = "Guardar imagen en galería",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(32.dp))

                            Text(
                                text = albumData.album.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onLikeClick,
                                    shapes = ButtonDefaults.shapes(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (albumData.album.bookmarkedAt != null) R.drawable.favorite else R.drawable.favorite_border
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (albumData.album.bookmarkedAt != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.save),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                Button(
                                    onClick = onPlayClick,
                                    shapes = ButtonDefaults.shapes(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    contentPadding = PaddingValues(vertical = 12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (isPlaying && isCurrentAlbum) R.drawable.pause else R.drawable.play
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = if (isPlaying && isCurrentAlbum)
                                                stringResource(R.string.media3_controls_pause_description) else stringResource(R.string.play),
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                Surface(
                                    onClick = onShareClick,
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.share),
                                            contentDescription = stringResource(R.string.share),
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            Text(
                                text = buildString {
                                    append(stringResource(R.string.album_text))
                                    if (albumData.album.year != null) {
                                        append(" • ${albumData.album.year}")
                                    }
                                    append(" • ${albumData.songs.size} Tracks")
                                    val totalDuration = albumData.songs.sumOf { it.song.duration }
                                    val hours = totalDuration / 3600
                                    val minutes = (totalDuration % 3600) / 60
                                    if (hours > 0) {
                                        append(" • ${hours}h ${minutes}m")
                                    } else {
                                        append(" • ${minutes}m")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            if (isDescriptionLoading) {
                                ShimmerHost(modifier = Modifier.padding(horizontal = 32.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Spacer(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.fillMaxWidth(0.8f).height(14.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)))
                                        Spacer(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)))
                                    }
                                }
                            } else {
                                var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
                                val staticDescription = "${albumData.album.title} is an album by ${albumData.artists.joinToString { it.name }}${
                                    if (albumData.album.year != null) ", released in ${albumData.album.year}" else ""
                                }. This collection features ${albumData.songs.size} tracks showcasing their musical artistry."
                                val description = albumDescription ?: staticDescription

                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .padding(horizontal = 32.dp)
                                        .fillMaxWidth()
                                        .animateContentSize()
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { isDescriptionExpanded = !isDescriptionExpanded }
                                        ),
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.by_text))
                                    albumData.artists.fastForEachIndexed { index, artist ->
                                        val link = LinkAnnotation.Clickable(artist.id) {
                                            navController.navigate("artist/${artist.id}")
                                        }
                                        withLink(link) {
                                            append(artist.name)
                                        }
                                        if (index != albumData.artists.lastIndex) {
                                            append(", ")
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Start,
                                modifier = Modifier
                                    .padding(horizontal = 32.dp)
                                    .fillMaxWidth()
                            )

                            Spacer(Modifier.height(24.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                            ) {
                                ToggleButton(
                                    checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                                    onCheckedChange = { onDownloadClick() },
                                    modifier = Modifier.weight(1f),
                                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                ) {
                                    when (downloadState) {
                                        Download.STATE_COMPLETED -> {
                                            Icon(
                                                painter = painterResource(R.drawable.offline),
                                                contentDescription = stringResource(R.string.downloading),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Download.STATE_DOWNLOADING -> {
                                            CircularProgressIndicator(
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                painter = painterResource(R.drawable.download),
                                                contentDescription = stringResource(R.string.save),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(
                                        text = when (downloadState) {
                                            Download.STATE_COMPLETED -> stringResource(R.string.download)
                                            Download.STATE_DOWNLOADING -> stringResource(R.string.downloading)
                                            else -> stringResource(R.string.save)
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                ToggleButton(
                                    checked = false,
                                    onCheckedChange = { onShuffleClick() },
                                    modifier = Modifier.weight(1f),
                                    shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.shuffle),
                                        contentDescription = stringResource(R.string.shuffle),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.shuffle), style = MaterialTheme.typography.labelMedium)
                                }

                                ToggleButton(
                                    checked = false,
                                    onCheckedChange = { onMoreOptionsClick() },
                                    modifier = Modifier.weight(1f),
                                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = stringResource(R.string.more_options),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.more), style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    albumSongsAndOtherVersions()
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
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    Text(
                        text = pluralStringResource(R.plurals.n_song, count, count),
                        style = MaterialTheme.typography.titleLarge
                    )
                } else if (!transparentAppBar && !isLandscape) {
                    Text(
                        text = albumData?.album?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = if (transparentAppBar && !selection) MaterialTheme.colorScheme.surface.copy(alpha = 0.7f) else Color.Transparent,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (selection) {
                                selection = false
                            } else {
                                navController.navigateUp()
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (selection) R.drawable.close else R.drawable.arrow_back
                            ),
                            contentDescription = null
                        )
                    }
                }
            },
            actions = {
                if (selection) {
                    val count = wrappedSongs?.count { it.isSelected } ?: 0
                    IconButton(
                        onClick = {
                            if (count == wrappedSongs?.size) {
                                wrappedSongs.forEach { it.isSelected = false }
                            } else {
                                wrappedSongs?.forEach { it.isSelected = true }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (count == wrappedSongs?.size) R.drawable.deselect else R.drawable.select_all
                            ),
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            menuState.show {
                                SelectionSongMenu(
                                    songSelection = wrappedSongs?.filter { it.isSelected }!!
                                        .map { it.item },
                                    onDismiss = menuState::dismiss,
                                    clearAction = { selection = false }
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                }
            },
            colors = if (transparentAppBar && !selection) {
                TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            } else {
                TopAppBarDefaults.topAppBarColors()
            },
            scrollBehavior = scrollBehavior
        )
    }
}

suspend fun saveAlbumImageToGallery(context: Context, imageUrl: String, albumTitle: String) {
    try {
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            val displayName = "${albumTitle.replace(" ", "_")}.png"
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

            uri?.let {
                contentResolver.openOutputStream(it)?.use { outputStream ->
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
                "X",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
