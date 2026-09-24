
package com.cgens67.avidtune.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.playback.ExoDownloadService
import com.cgens67.avidtune.playback.queues.LocalAlbumRadio
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
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
import androidx.compose.ui.platform.LocalLayoutDirection

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
    var selection by remember {
        mutableStateOf(false)
    }

    if (selection) {
        BackHandler {
            selection = false
        }
    }

    val downloadUtil = LocalDownloadUtil.current
    var downloadState by remember {
        mutableStateOf(Download.STATE_STOPPED)
    }

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

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 200
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    val albumHeader = @Composable {
        val albumData = albumWithSongs!!
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp)) // Space for top app bar

            // Album Artwork - Large and centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isLandscape) 32.dp else 48.dp)
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

                // Botón de descarga superpuesto
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

            Spacer(Modifier.height(if (isLandscape) 24.dp else 32.dp))

            // Album Title
            Text(
                text = albumData.album.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(Modifier.height(if (isLandscape) 16.dp else 24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save/Like Button
                Button(
                    onClick = {
                        database.query {
                            update(albumData.album.toggleLike())
                        }
                    },
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
                                if (albumData.album.bookmarkedAt != null) {
                                    R.drawable.favorite
                                } else {
                                    R.drawable.favorite_border
                                }
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (albumData.album.bookmarkedAt != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (albumData.album.bookmarkedAt != null) stringResource(R.string.save) else stringResource(R.string.save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Play Button
                Button(
                    onClick = {
                        if (isPlaying && mediaMetadata?.album?.id == albumData.album.id) {
                            playerConnection.player.pause()
                        } else if (mediaMetadata?.album?.id == albumData.album.id) {
                            playerConnection.player.play()
                        } else {
                            playerConnection.service.getAutomix(playlistId)
                            playerConnection.playQueue(
                                LocalAlbumRadio(albumData),
                            )
                        }
                    },
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
                                if (isPlaying && mediaMetadata?.album?.id == albumData.album.id)
                                    R.drawable.pause
                                else
                                    R.drawable.play
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isPlaying && mediaMetadata?.album?.id == albumData.album.id)
                                stringResource(R.string.media3_controls_pause_description) else stringResource(R.string.play),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                // Share Button
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.check_out_album_share, albumData.album.title, albumData.artists.joinToString { it.name }, "https://music.youtube.com/playlist?list=${albumData.album.playlistId}"))
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
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

            // Album Info
            Text(
                text = buildString {
                    append(stringResource(R.string.album_text))
                    if (albumData.album.year != null) {
                        append(" • ${albumData.album.year}")
                    }
                    append(" • ${albumData.songs.size} Tracks")
                    val totalDuration = albumData.songs.sumOf { it.song.duration ?: 0 }
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

            // Album Description
            if (isDescriptionLoading) {
                ShimmerHost(modifier = Modifier.padding(horizontal = 32.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
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

            // Artist Names (clickable)
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

            // Additional action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            ) {

                ToggleButton(
                    checked = downloadState == Download.STATE_COMPLETED || downloadState == Download.STATE_DOWNLOADING,
                    onCheckedChange = {
                        when (downloadState) {
                            Download.STATE_COMPLETED, Download.STATE_DOWNLOADING -> {
                                albumData.songs.forEach { song ->
                                    DownloadService.sendRemoveDownload(
                                        context,
                                        ExoDownloadService::class.java,
                                        song.id,
                                        false,
                                    )
                                }
                            }
                            else -> {
                                albumData.songs.forEach { song ->
                                    val downloadRequest =
                                        DownloadRequest
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
                    },
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

                // Shuffle Button
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        playerConnection.service.getAutomix(playlistId)
                        playerConnection.playQueue(
                            LocalAlbumRadio(albumData.copy(songs = albumData.songs.shuffled())),
                        )
                    },
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

                // More Options Button
                ToggleButton(
                    checked = false,
                    onCheckedChange = {
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = Album(
                                    albumData.album,
                                    albumData.artists
                                ),
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
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

    val albumSongsAndOtherVersions: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        // Songs List
        if (wrappedSongs != null && wrappedSongs.isNotEmpty()) {
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
                    modifier =
                        Modifier
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
                                                    albumWithSongs!!,
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
                                    wrappedSongs.forEach {
                                        it.isSelected = false
                                    } // Clear previous selections
                                    songWrapper.isSelected = true // Select the current item
                                },
                            ),
                )
            }
        }

        // Other Versions Section
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
        if (albumWithSongs != null && albumWithSongs!!.songs.isNotEmpty()) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                            .padding(
                                start = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateStartPadding(layoutDirection),
                                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
                            )
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        albumHeader()
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(
                            top = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding() + 50.dp,
                            bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                            end = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateEndPadding(layoutDirection)
                        )
                    ) {
                        albumSongsAndOtherVersions()
                    }
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "album_header") {
                        albumHeader()
                    }
                    albumSongsAndOtherVersions()
                }
            }
        } else {
            // Loading indicator
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
                } else {
                    Text(
                        text = albumWithSongs?.album?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
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
        // Cargar la imagen usando Coil
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .build()

        val drawable = context.imageLoader.execute(request).drawable

        if (drawable != null) {
            val bitmap = drawable.toBitmap()

            // Usar el título del álbum para el nombre del archivo
            val displayName = "${albumTitle.replace(" ", "_")}.png"
            val mimeType = "image/png" // Tipo MIME para PNG

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

                // Mostrar un mensaje de éxito
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
--- START OF FILE app/src/main/java/com/cgens67/avidtune/ui/screens/ExploreScreen.kt ---

package com.cgens67.avidtune.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenresList by viewModel.moodAndGenres.collectAsState()
    val lazyGridState = rememberLazyGridState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        LazyVerticalGrid(
            state = lazyGridState,
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(
                top = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding() + 8.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (moodAndGenresList == null) {
                items(12) {
                    ShimmerHost {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.6f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }

            moodAndGenresList?.forEach { moodAndGenres ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = getTranslatedExploreSectionTitle(moodAndGenres.title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                    )
                }

                items(moodAndGenres.items) { item ->
                    MoodAndGenresCard(
                        title = item.title,
                        stripeColor = item.stripeColor,
                        onClick = {
                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MoodAndGenresCard(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    // Convert API's generic long value to Jetpack Compose Color
    val baseColor = if (stripeColor != 0L) Color(stripeColor) else MaterialTheme.colorScheme.primary

    // Create a beautiful linear gradient to give life to the card
    val gradient = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = if (isDark) 0.6f else 0.85f),
            baseColor.copy(alpha = if (isDark) 0.2f else 0.4f)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.6f) // Modern tile aspect ratio
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
        ) {
            // A subtle, oversized background decorative icon
            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 12.dp, y = 16.dp)
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = -20f
                        alpha = 0.25f
                    },
                tint = Color.White
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun getTranslatedExploreSectionTitle(title: String): String {
    return when (title.lowercase()) {
        "moods & moments" -> stringResource(R.string.moods_and_moments)
        "genres" -> stringResource(R.string.genres)
        else -> title
    }
}
--- START OF FILE app/src/main/java/com/cgens67/avidtune/ui/screens/HistoryScreen.kt ---

package com.cgens67.avidtune.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cgens67.innertube.utils.parseCookieString
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.HistorySource
import com.cgens67.avidtune.constants.InnerTubeCookieKey
import com.cgens67.avidtune.db.entities.EventWithSong
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.ChipsRow
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.menu.SelectionMediaMetadataMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.DateAgo
import com.cgens67.avidtune.viewmodels.HistoryViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    var selection by remember {
        mutableStateOf(false)
    }

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
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (selection) {
        BackHandler {
            selection = false
        }
    }

    val historySource by viewModel.historySource.collectAsState()
    val events by viewModel.events.collectAsState()
    val historyPage by viewModel.historyPage

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    fun dateAgoToString(dateAgo: DateAgo): String {
        return when (dateAgo) {
            DateAgo.Today -> context.getString(R.string.today)
            DateAgo.Yesterday -> context.getString(R.string.yesterday)
            DateAgo.ThisWeek -> context.getString(R.string.this_week)
            DateAgo.LastWeek -> context.getString(R.string.last_week)
            is DateAgo.Other -> dateAgo.date.format(DateTimeFormatter.ofPattern("yyyy/MM"))
        }
    }

    class WrappedHistoryItem(val item: EventWithSong) {
        var isSelected by mutableStateOf(false)
    }

    val filteredEvents = remember(events, query) {
        if (query.text.isEmpty()) {
            events
        } else {
            events.mapValues { (_, songs) ->
                songs.filter { event ->
                    event.song.song.title.contains(query.text, ignoreCase = true) ||
                            event.song.artists.any {
                                it.name.contains(
                                    query.text,
                                    ignoreCase = true
                                )
                            }
                }
            }.filterValues { it.isNotEmpty() }
        }
    }

    val filteredRemoteContent = remember(historyPage, query) {
        if (query.text.isEmpty()) {
            historyPage?.sections
        } else {
            historyPage?.sections?.map { section ->
                section.copy(
                    songs = section.songs.filter { song ->
                        song.title.contains(query.text, ignoreCase = true) ||
                                song.artists.any { it.name.contains(query.text, ignoreCase = true) }
                    }
                )
            }?.filter { it.songs.isNotEmpty() }
        }
    }

    val wrappedItems = remember(filteredEvents) {
        filteredEvents.flatMap { it.value }.map { WrappedHistoryItem(it) }
    }.toMutableStateList()

    val lazyListState = rememberLazyListState()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        ) {
            item {
                ChipsRow(
                    chips = if (isLoggedIn) listOf(
                        HistorySource.LOCAL to stringResource(R.string.local_history),
                        HistorySource.REMOTE to stringResource(R.string.remote_history),
                    ) else {
                        listOf(HistorySource.LOCAL to stringResource(R.string.local_history))
                    },
                    currentValue = historySource,
                    onValueUpdate = {
                        viewModel.historySource.value = it
                        if (it == HistorySource.REMOTE) {
                            viewModel.fetchRemoteHistory()
                        }
                    }
                )
            }

            if (historySource == HistorySource.REMOTE && isLoggedIn) {
                filteredRemoteContent?.forEach { section ->
                    stickyHeader {
                        NavigationTitle(
                            title = section.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                        )
                    }

                    items(
                        items = section.songs,
                        key = { "${section.title}_${it.id}" }
                    ) { song ->
                        YouTubeListItem(
                            item = song,
                            isActive = song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (song.id == mediaMetadata?.id) {
                                            playerConnection.player.togglePlayPause()
                                        } else {
                                            playerConnection.playQueue(
                                                YouTubeQueue.radio(song.toMediaMetadata())
                                            )
                                        }
                                    },
                                    onLongClick = {
                                        menuState.show {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = menuState::dismiss
                                            )
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            } else {
                filteredEvents.forEach { (dateAgo, events) ->
                    stickyHeader {
                        NavigationTitle(
                            title = dateAgoToString(dateAgo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface)
                        )
                    }

                    itemsIndexed(
                        items = wrappedItems,
                    ) { index, wrappedItem ->
                        val event = wrappedItem.item
                        SongListItem(
                            song = event.song,
                            isActive = event.song.id == mediaMetadata?.id,
                            isPlaying = isPlaying,
                            showInLibraryIcon = true,
                            isSelected = wrappedItem.isSelected && selection,

                            trailingContent = {
                                IconButton(
                                    onClick = {
                                        if (!selection) {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = event.song,
                                                    event = event.event,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.more_vert),
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (!selection) {
                                            if (event.song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    ListQueue(
                                                        title = dateAgoToString(dateAgo),
                                                        items = wrappedItems.map { it.item.song.toMediaItem() },
                                                        startIndex = index
                                                    )
                                                )
                                            }
                                        } else {
                                            wrappedItem.isSelected = !wrappedItem.isSelected
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!selection) {
                                            selection = true
                                            wrappedItems.forEach { it.isSelected = false }
                                            wrappedItem.isSelected = true
                                        }
                                    }
                                )
                                .animateItem()
                        )
                    }
                }
            }
        }

        HideOnScrollFAB(
            visible = if (historySource == HistorySource.REMOTE) {
                filteredRemoteContent?.any { it.songs.isNotEmpty() } == true
            } else {
                wrappedItems.isNotEmpty()
            },
            lazyListState = lazyListState,
            icon = R.drawable.shuffle,
            onClick = {
                if (historySource == HistorySource.REMOTE && historyPage != null) {
                    val songs = filteredRemoteContent?.flatMap { it.songs } ?: emptyList()
                    if (songs.isNotEmpty()) {
                        playerConnection.playQueue(
                            ListQueue(
                                title = context.getString(R.string.history),
                                items = songs.map { it.toMediaItem() }.shuffled()
                            )
                        )
                    }
                } else {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.history),
                            items = wrappedItems.map { it.item.song.toMediaItem() }.shuffled()
                        )
                    )
                }
            }
        )
    }

    TopAppBar(
        title = {
            if (selection) {
                val count = wrappedItems.count { it.isSelected }
                Text(
                    text = pluralStringResource(R.plurals.n_song, count, count),
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (isSearching) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            } else {
                Text(stringResource(R.string.history))
            }
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    when {
                        isSearching -> {
                            isSearching = false
                            query = TextFieldValue()
                        }

                        selection -> {
                            selection = false
                        }

                        else -> {
                            navController.navigateUp()
                        }
                    }
                },
                onLongClick = {
                    if (!isSearching && !selection) {
                        navController.backToMain()
                    }
                }
            ) {
                Icon(
                    painter = painterResource(
                        if (selection) R.drawable.close else R.drawable.arrow_back
                    ),
                    contentDescription = null
                )
            }
        },
        actions = {
            if (selection) {
                val count = wrappedItems.count { it.isSelected }
                IconButton(
                    onClick = {
                        if (count == wrappedItems.size) {
                            wrappedItems.forEach { it.isSelected = false }
                        } else {
                            wrappedItems.forEach { it.isSelected = true }
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(
                            if (count == wrappedItems.size) R.drawable.deselect else R.drawable.select_all
                        ),
                        contentDescription = null
                    )
                }
                IconButton(
                    onClick = {
                        menuState.show {
                            SelectionMediaMetadataMenu(
                                songSelection = wrappedItems
                                    .filter { it.isSelected }
                                    .map { it.item.song.toMediaItem().metadata!! },
                                onDismiss = menuState::dismiss,
                                clearAction = { selection = false },
                                currentItems = emptyList()
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = null
                    )
                }
            } else if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null
                    )
                }
            }
        }
    )
}
--- START OF FILE app/src/main/java/com/cgens67/avidtune/ui/screens/HomeScreen.kt ---

@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class,
)

package com.cgens67.avidtune.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.models.YTItem
import com.cgens67.innertube.utils.parseCookieString
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AccountNameKey
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.constants.GridThumbnailHeight
import com.cgens67.avidtune.constants.InnerTubeCookieKey
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.SwipeThumbnailKey
import com.cgens67.avidtune.constants.ThumbnailCornerRadius
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.LocalItem
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.LocalAlbumRadio
import com.cgens67.avidtune.playback.queues.YouTubeAlbumRadio
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AlbumGridItem
import com.cgens67.avidtune.ui.component.ArtistGridItem
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.MenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongGridItem
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.shimmer.GridItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.component.shimmer.TextPlaceholder
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.ArtistMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.SnapLayoutInfoProvider
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.isScrollingUp
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val similarRecommendations by viewModel.similarRecommendations.collectAsState()
    val accountPlaylists by viewModel.accountPlaylists.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by rememberPreference(AccountNameKey, "")
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null

    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val color1 = MaterialTheme.colorScheme.primary
    val color2 = MaterialTheme.colorScheme.secondary
    val color3 = MaterialTheme.colorScheme.tertiary
    val color4 = MaterialTheme.colorScheme.primaryContainer
    val color5 = MaterialTheme.colorScheme.secondaryContainer
    val surfaceColor = MaterialTheme.colorScheme.surface

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> SongGridItem(
                song = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (it.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                            else playerConnection.playQueue(YouTubeQueue.radio(it.toMediaMetadata()))
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { SongMenu(originalSong = it, navController = navController, onDismiss = menuState::dismiss) }
                        }
                    ),
                isActive = it.id == mediaMetadata?.id,
                isPlaying = isPlaying,
            )
            is Album -> AlbumGridItem(
                album = it,
                isActive = it.id == mediaMetadata?.album?.id,
                isPlaying = isPlaying,
                coroutineScope = scope,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("album/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { AlbumMenu(originalAlbum = it, navController = navController, onDismiss = menuState::dismiss) }
                        }
                    )
            )
            is Artist -> ArtistGridItem(
                artist = it,
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { navController.navigate("artist/${it.id}") },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show { ArtistMenu(originalArtist = it, coroutineScope = scope, onDismiss = menuState::dismiss) }
                        }
                    ),
            )
            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        YouTubeGridItem(
            item = item,
            isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
            isPlaying = isPlaying,
            coroutineScope = scope,
            thumbnailRatio = 1f,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        when (item) {
                            is SongItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.toMediaMetadata()))
                            is AlbumItem -> navController.navigate("album/${item.id}")
                            is ArtistItem -> navController.navigate("artist/${item.id}")
                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                            is EpisodeItem -> playerConnection.playQueue(YouTubeQueue(item.endpoint ?: WatchEndpoint(videoId = item.id), item.asSongItem().toMediaMetadata()))
                            is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> YouTubeSongMenu(song = item, navController = navController, onDismiss = menuState::dismiss)
                                is AlbumItem -> YouTubeAlbumMenu(albumItem = item, navController = navController, onDismiss = menuState::dismiss)
                                is ArtistItem -> YouTubeArtistMenu(artist = item, onDismiss = menuState::dismiss)
                                is PlaylistItem -> YouTubePlaylistMenu(playlist = item, coroutineScope = scope, onDismiss = menuState::dismiss)
                                is EpisodeItem -> YouTubeSongMenu(song = item.asSongItem(), navController = navController, onDismiss = menuState::dismiss)
                                is PodcastItem -> YouTubePlaylistMenu(playlist = item.asPlaylistItem(), coroutineScope = scope, onDismiss = menuState::dismiss)
                            }
                        }
                    }
                )
        )
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(state = pullRefreshState, isRefreshing = isRefreshing, onRefresh = viewModel::refresh),
        contentAlignment = Alignment.TopStart
    ) {
        if (!disableBlur) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxSize(0.7f)
                    .align(Alignment.TopCenter)
                    .zIndex(-1f)
                    .drawWithCache {
                        val width = this.size.width
                        val height = this.size.height

                        val brush1 = Brush.radialGradient(colors = listOf(color1.copy(0.38f), color1.copy(0.24f), color1.copy(0.14f), color1.copy(0.06f), Color.Transparent), center = Offset(width * 0.15f, height * 0.1f), radius = width * 0.55f)
                        val brush2 = Brush.radialGradient(colors = listOf(color2.copy(0.34f), color2.copy(0.2f), color2.copy(0.11f), color2.copy(0.05f), Color.Transparent), center = Offset(width * 0.85f, height * 0.2f), radius = width * 0.65f)
                        val brush3 = Brush.radialGradient(colors = listOf(color3.copy(0.3f), color3.copy(0.17f), color3.copy(0.09f), color3.copy(0.04f), Color.Transparent), center = Offset(width * 0.3f, height * 0.45f), radius = width * 0.6f)
                        val brush4 = Brush.radialGradient(colors = listOf(color4.copy(0.26f), color4.copy(0.14f), color4.copy(0.08f), color4.copy(0.03f), Color.Transparent), center = Offset(width * 0.7f, height * 0.5f), radius = width * 0.7f)
                        val brush5 = Brush.radialGradient(colors = listOf(color5.copy(0.22f), color5.copy(0.12f), color5.copy(0.06f), color5.copy(0.02f), Color.Transparent), center = Offset(width * 0.5f, height * 0.75f), radius = width * 0.8f)
                        val overlayBrush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, surfaceColor.copy(0.22f), surfaceColor.copy(0.55f), surfaceColor), startY = height * 0.4f, endY = height)

                        onDrawBehind {
                            drawRect(brush1); drawRect(brush2); drawRect(brush3); drawRect(brush4); drawRect(brush5); drawRect(overlayBrush)
                        }
                    }
            )
        }

        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

        val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
            SnapLayoutInfoProvider(lazyGridState = forgottenFavoritesLazyGridState, positionInLayout = { layoutSize, itemSize -> (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f) })
        }

        LazyColumn(
            state = lazylistState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
        ) {
            item {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .animateItem(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(12.dp))

                    FilterChip(
                        selected = false,
                        onClick = { navController.navigate("apple_music_trending") },
                        label = { Text(stringResource(R.string.trending)) },
                        leadingIcon = { Icon(painterResource(R.drawable.apple), null, modifier = Modifier.size(18.dp).offset(y = (-0.9).dp)) },
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    )

                    Spacer(Modifier.width(8.dp))

                    val chips = buildList {
                        add("history" to stringResource(R.string.history))
                        add("stats" to stringResource(R.string.stats))
                        add("liked" to stringResource(R.string.liked))
                        add("downloads" to stringResource(R.string.offline))
                        if (isLoggedIn) add("account" to stringResource(R.string.account))
                    }

                    chips.forEach { (value, label) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                when (value) {
                                    "history" -> navController.navigate("history")
                                    "stats" -> navController.navigate("stats")
                                    "liked" -> navController.navigate("auto_playlist/liked")
                                    "downloads" -> navController.navigate("auto_playlist/downloaded")
                                    "account" -> if (isLoggedIn) navController.navigate("account")
                                }
                            },
                            label = { Text(label) },
                            shape = RoundedCornerShape(16.dp),
                            border = null,
                            colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }

            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                item {
                    QuickPicksSection(quickPicks = quickPicks, mediaMetadata = mediaMetadata, isPlaying = isPlaying, navController = navController, playerConnection = playerConnection, menuState = menuState, haptic = haptic, modifier = Modifier.animateItem())
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                item { NavigationTitle(title = stringResource(R.string.keep_listening), modifier = Modifier.animateItem()) }
                item {
                    val rows = min(2, keepListening.size)
                    LazyHorizontalGrid(
                        state = rememberLazyGridState(),
                        rows = GridCells.Fixed(rows),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((GridThumbnailHeight + with(LocalDensity.current) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() * 2 + MaterialTheme.typography.bodyMedium.lineHeight.toDp() * 2 }) * rows)
                            .animateItem()
                    ) { items(keepListening) { localGridItem(it) } }
                }
            }

            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                item {
                    NavigationTitle(
                        label = stringResource(R.string.your_ytb_playlists),
                        title = accountName,
                        thumbnail = {
                            if (url != null) {
                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(url).diskCachePolicy(CachePolicy.ENABLED).diskCacheKey(url).crossfade(true).build(), placeholder = painterResource(R.drawable.person), error = painterResource(R.drawable.person), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(ListThumbnailSize).clip(CircleShape))
                            } else {
                                Icon(painterResource(R.drawable.person), null, modifier = Modifier.size(ListThumbnailSize))
                            }
                        },
                        onClick = { navController.navigate("account") },
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = Modifier.animateItem()) {
                        items(accountPlaylists) { item -> ytGridItem(item) }
                    }
                }
            }

            similarRecommendations?.forEach { recommendation ->
                item {
                    val thumbnailUrl = recommendation.title.thumbnailUrl
                    NavigationTitle(
                        label = stringResource(R.string.similar_to_caps),
                        title = recommendation.title.title,
                        thumbnail = if (thumbnailUrl != null) { { AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(if (recommendation.title is Artist) CircleShape else RoundedCornerShape(ThumbnailCornerRadius))) } } else null,
                        onClick = {
                            when (val itemTitle = recommendation.title) {
                                is Song -> navController.navigate("album/${itemTitle.album!!.id}")
                                is Album -> navController.navigate("album/${itemTitle.id}")
                                is Artist -> navController.navigate("artist/${itemTitle.id}")
                                is Playlist -> {}
                            }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = Modifier.animateItem()) {
                        items(recommendation.items) { item -> ytGridItem(item) }
                    }
                }
            }

            homePage?.sections?.filter { !it.title.equals("New releases", ignoreCase = true) }?.forEach { section ->
                item {
                    val thumbnailUrl = section.thumbnail
                    NavigationTitle(
                        title = getTranslatedHomeSectionTitle(section.title),
                        label = section.label?.let { getTranslatedHomeSectionTitle(it) },
                        thumbnail = if (thumbnailUrl != null) { { AsyncImage(model = thumbnailUrl, contentDescription = null, modifier = Modifier.size(ListThumbnailSize).clip(if (section.endpoint?.isArtistEndpoint == true) CircleShape else RoundedCornerShape(ThumbnailCornerRadius))) } } else null,
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = Modifier.animateItem()) {
                        items(section.items) { item -> ytGridItem(item) }
                    }
                }
            }

            explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                item { NavigationTitle(title = stringResource(R.string.new_release_albums), onClick = { navController.navigate("new_release") }, modifier = Modifier.animateItem()) }
                item {
                    LazyRow(contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(), modifier = Modifier.animateItem()) {
                        items(newReleaseAlbums) { album ->
                            YouTubeGridItem(
                                item = album,
                                isActive = mediaMetadata?.album?.id == album.id,
                                isPlaying = isPlaying,
                                coroutineScope = scope,
                                modifier = Modifier.combinedClickable(
                                    onClick = { navController.navigate("album/${album.id}") },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { YouTubeAlbumMenu(albumItem = album, navController = navController, onDismiss = menuState::dismiss) } }
                                ).animateItem()
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    ShimmerHost(modifier = Modifier.animateItem()) {
                        TextPlaceholder(height = 36.dp, modifier = Modifier.padding(12.dp).width(250.dp))
                        LazyRow { items(4) { GridItemPlaceHolder() } }
                    }
                }
            }

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                item { NavigationTitle(title = stringResource(R.string.forgotten_favorites), modifier = Modifier.animateItem()) }
                item {
                    val rows = min(4, forgottenFavorites.size)
                    LazyHorizontalGrid(
                        state = forgottenFavoritesLazyGridState,
                        rows = GridCells.Fixed(rows),
                        flingBehavior = rememberSnapFlingBehavior(forgottenFavoritesSnapLayoutInfoProvider),
                        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                        modifier = Modifier.fillMaxWidth().height(ListItemHeight * rows).animateItem()
                    ) {
                        items(forgottenFavorites) { originalSong ->
                            val song by database.song(originalSong.id).collectAsState(initial = originalSong)
                            SongListItem(
                                song = song!!,
                                showInLibraryIcon = true,
                                isActive = song!!.id == mediaMetadata?.id,
                                isPlaying = isPlaying,
                                modifier = Modifier.width(horizontalLazyGridItemWidth).combinedClickable(
                                    onClick = { if (song!!.id == mediaMetadata?.id) playerConnection.player.togglePlayPause() else playerConnection.playQueue(YouTubeQueue.radio(song!!.toMediaMetadata())) },
                                    onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); menuState.show { SongMenu(originalSong = song!!, navController = navController, onDismiss = menuState::dismiss) } }
                                )
                            )
                        }
                    }
                }
            }
        }

        val isFabVisible = (allLocalItems.isNotEmpty() || allYtItems.isNotEmpty()) && lazylistState.isScrollingUp()

        AnimatedVisibility(
            visible = isFabVisible,
            enter = scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                    fadeIn(tween(300, easing = LinearOutSlowInEasing)) +
                    slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it },
            exit = scaleOut(tween(400, easing = FastOutSlowInEasing), targetScale = 0.7f) +
                   fadeOut(tween(300, easing = LinearOutSlowInEasing)) +
                   slideOutVertically(tween(400, easing = FastOutSlowInEasing)) { it },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
        ) {
            FloatingActionButton(
                modifier = Modifier.padding(16.dp),
                onClick = {
                    val local = when {
                        allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                        allLocalItems.isNotEmpty() -> true
                        else -> false
                    }
                    scope.launch(Dispatchers.Main) {
                        if (local) {
                            when (val luckyItem = allLocalItems.random()) {
                                is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is Album -> {
                                    val albumWithSongs = withContext(Dispatchers.IO) { database.albumWithSongs(luckyItem.id).first() }
                                    albumWithSongs?.let { playerConnection.playQueue(LocalAlbumRadio(it)) }
                                }
                                is Artist -> {}
                                is Playlist -> {}
                            }
                        } else {
                            when (val luckyItem = allYtItems.random()) {
                                is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                                is ArtistItem -> luckyItem.radioEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                                is PlaylistItem -> luckyItem.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                                is EpisodeItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.asSongItem().toMediaMetadata()))
                                is PodcastItem -> luckyItem.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                            }
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    painter = painterResource(R.drawable.shuffle),
                    contentDescription = null
                )
            }
        }

        Box(Modifier.align(Alignment.TopCenter)) {
            PullToRefreshDefaults.Indicator(state = pullRefreshState, isRefreshing = isRefreshing, modifier = Modifier.padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun QuickPicksSection(
    quickPicks: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val distinctQuickPicks = remember(quickPicks) { quickPicks.distinctBy { it.id } }
    val (coverResolution) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )

    HorizontalCenteredHeroCarousel(
        state = rememberCarouselState { distinctQuickPicks.size },
        maxItemWidth = 250.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth().height(290.dp)
    ) { index ->
        val song = distinctQuickPicks[index]
        val isActive = song.id == mediaMetadata?.id

        Box(
            modifier = Modifier
                .fillMaxSize()
                .maskClip(MaterialTheme.shapes.extraLarge)
                .maskBorder(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.extraLarge)
                .combinedClickable(
                    onClick = {
                        if (isActive) playerConnection.player.togglePlayPause()
                        else playerConnection.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show { SongMenu(originalSong = song, navController = navController, onDismiss = menuState::dismiss) }
                    }
                )
        ) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(song.song.thumbnailUrl?.resize(coverResolution.size, coverResolution.size)).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)))))
            if (isActive && isPlaying) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.volume_up), null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                Text(text = song.song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = song.artists.joinToString { it.name }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun getTranslatedHomeSectionTitle(title: String): String {
    return when {
        title.equals("Albums for you", ignoreCase = true) -> stringResource(R.string.albums_for_you)
        title.equals("New releases", ignoreCase = true) -> stringResource(R.string.new_releases)
        title.equals("SIMILAR TO", ignoreCase = true) -> stringResource(R.string.similar_to_caps)
        title.equals("Featured playlists for you", ignoreCase = true) -> stringResource(R.string.featured_playlists_for_you)
        title.equals("Fresh finds, old favorites", ignoreCase = true) -> stringResource(R.string.fresh_finds_old_favorites)
        title.equals("From your library", ignoreCase = true) -> stringResource(R.string.from_your_library)
        title.equals("From the community", ignoreCase = true) -> stringResource(R.string.from_the_community)
        title.equals("Forgotten favorites", ignoreCase = true) -> stringResource(R.string.forgotten_favorites)
        title.equals("Music videos for you", ignoreCase = true) -> stringResource(R.string.music_videos_for_you)
        title.equals("Today's hits", ignoreCase = true) -> stringResource(R.string.todays_hits)
        title.equals("Mixed for you", ignoreCase = true) -> stringResource(R.string.mixed_for_you)
        title.equals("Live performances", ignoreCase = true) -> stringResource(R.string.artist_live_performances)
        title.equals("Your daily discover", ignoreCase = true) -> stringResource(R.string.your_daily_discover)
        else -> title
    }
}
