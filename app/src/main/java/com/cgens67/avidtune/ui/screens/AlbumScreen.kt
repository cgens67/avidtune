package com.cgens67.avidtune.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
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
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
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
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.ItemWrapper
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.viewmodels.AlbumViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private enum class AlbumReleaseType {
    ALBUM,
    SINGLE,
    EP
}

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
    val isTranslated by viewModel.isTranslated.collectAsState()
    val canTranslate by viewModel.canTranslate.collectAsState()
    val isExplicit by viewModel.isExplicit.collectAsState()

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
    val landscapeScrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val layoutDirection = LocalLayoutDirection.current

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    val albumData = albumWithSongs
    val isCurrentAlbum = mediaMetadata?.album?.id == albumData?.album?.id

    // Gradient colors extracted from album artwork
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val albumThumbnail = albumData?.album?.thumbnailUrl

    LaunchedEffect(albumThumbnail) {
        if (albumThumbnail != null) {
            val request = ImageRequest.Builder(context)
                .data(albumThumbnail)
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

                    val extractedColors = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColor
                    )
                    gradientColors = extractedColors
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        // Gradient background layer
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

                            // Smooth horizontal fade into surfaceColor across right boundary of left pane
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

                            // Smooth vertical fade towards bottom of left pane
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

        if (albumData != null && albumData.songs.isNotEmpty()) {
            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
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
                        AlbumHeaderContent(
                            albumData = albumData,
                            artworkSize = 160.dp,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            coverResolution = coverResolution,
                            isPlaying = isPlaying,
                            isCurrentAlbum = isCurrentAlbum,
                            downloadState = downloadState,
                            isDescriptionLoading = isDescriptionLoading,
                            albumDescription = albumDescription,
                            isTranslated = isTranslated,
                            canTranslate = canTranslate,
                            isExplicit = isExplicit,
                            onToggleTranslation = viewModel::toggleDescriptionTranslation,
                            navController = navController,
                            onPlayClick = onPlayClick,
                            onShuffleClick = onShuffleClick,
                            onLikeClick = onLikeClick,
                            onDownloadClick = onDownloadClick,
                            onShareClick = onShareClick,
                            onMoreOptionsClick = onMoreOptionsClick,
                            modifier = Modifier.background(Color.Transparent)
                        )
                    }

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
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(key = "album_header") {
                        AlbumHeaderContent(
                            albumData = albumData,
                            artworkSize = 280.dp,
                            thumbnailCornerRadius = thumbnailCornerRadius,
                            coverResolution = coverResolution,
                            isPlaying = isPlaying,
                            isCurrentAlbum = isCurrentAlbum,
                            downloadState = downloadState,
                            isDescriptionLoading = isDescriptionLoading,
                            albumDescription = albumDescription,
                            isTranslated = isTranslated,
                            canTranslate = canTranslate,
                            isExplicit = isExplicit,
                            onToggleTranslation = viewModel::toggleDescriptionTranslation,
                            navController = navController,
                            onPlayClick = onPlayClick,
                            onShuffleClick = onShuffleClick,
                            onLikeClick = onLikeClick,
                            onDownloadClick = onDownloadClick,
                            onShareClick = onShareClick,
                            onMoreOptionsClick = onMoreOptionsClick,
                            modifier = Modifier.background(Color.Transparent)
                        )
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
                            if (selection) {
                                selection = false
                            } else {
                                navController.navigateUp()
                            }
                        },
                        onLongClick = {
                            if (!selection) {
                                navController.backToMain()
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(
                                if (selection) R.drawable.close else R.drawable.arrow_back
                            ),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                } else {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                val link = albumData?.album?.playlistId?.let { "https://music.youtube.com/playlist?list=$it" }
                                    ?: albumData?.album?.id?.let { "https://music.youtube.com/browse/$it" }
                                if (link != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Album Link", link)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Icon(
                                painterResource(R.drawable.link),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun AlbumHeaderContent(
    albumData: AlbumWithSongs,
    artworkSize: Dp,
    thumbnailCornerRadius: Float,
    coverResolution: CoverResolution,
    isPlaying: Boolean,
    isCurrentAlbum: Boolean,
    downloadState: Int,
    isDescriptionLoading: Boolean,
    albumDescription: String?,
    isTranslated: Boolean,
    canTranslate: Boolean,
    isExplicit: Boolean,
    onToggleTranslation: () -> Unit,
    navController: NavController,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val horizontalPadding = if (artworkSize <= 160.dp) 20.dp else 32.dp

    val releaseType = remember(albumData) {
        val title = albumData.album.title.trim()
        val count = if (albumData.songs.isNotEmpty()) albumData.songs.size else albumData.album.songCount
        when {
            title.endsWith(" - Single", ignoreCase = true) ||
            title.endsWith(" (Single)", ignoreCase = true) ||
            title.endsWith("[Single]", ignoreCase = true) ||
            count == 1 -> AlbumReleaseType.SINGLE

            title.endsWith(" - EP", ignoreCase = true) ||
            title.endsWith(" (EP)", ignoreCase = true) ||
            title.endsWith("[EP]", ignoreCase = true) ||
            title.endsWith(" EP") ||
            count in 2..6 -> AlbumReleaseType.EP

            else -> AlbumReleaseType.ALBUM
        }
    }

    val effectiveIsExplicit = remember(isExplicit, albumData.album.title) {
        isExplicit || albumData.album.title.contains("explicit", ignoreCase = true)
    }

    val cleanAlbumTitle = remember(albumData.album.title, effectiveIsExplicit) {
        if (effectiveIsExplicit) {
            albumData.album.title
                .replace(Regex("""\s*[\(\[](Explicit|explicit)[\)\]]"""), "")
                .trim()
        } else {
            albumData.album.title
        }
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

        // Title with inline Explicit icon
        val inlineContentId = "explicitBadge"
        val inlineContent = remember(effectiveIsExplicit) {
            if (!effectiveIsExplicit) {
                emptyMap()
            } else {
                mapOf(
                    inlineContentId to InlineTextContent(
                        Placeholder(
                            width = 18.sp,
                            height = 18.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            visible = true
                        }
                        val scale by animateFloatAsState(
                            targetValue = if (visible) 1f else 0.3f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "explicitScale"
                        )
                        val alpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(350),
                            label = "explicitAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.explicit),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }
        }

        val annotatedTitle = remember(cleanAlbumTitle, effectiveIsExplicit) {
            buildAnnotatedString {
                append(cleanAlbumTitle)
                if (effectiveIsExplicit) {
                    append("\u00A0")
                    appendInlineContent(inlineContentId, "[E]")
                }
            }
        }

        Text(
            text = annotatedTitle,
            inlineContent = inlineContent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 12.dp)
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow))
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
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        // Meta line
        val totalDuration = albumData.songs.sumOf { it.song.duration }
        val hours = totalDuration / 3600
        val minutes = (totalDuration % 3600) / 60
        val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        val releaseTypeText = when (releaseType) {
            AlbumReleaseType.ALBUM -> stringResource(R.string.album_text)
            AlbumReleaseType.SINGLE -> stringResource(R.string.single_text)
            AlbumReleaseType.EP -> stringResource(R.string.ep_text)
        }

        Text(
            text = listOfNotNull(
                releaseTypeText,
                albumData.album.year?.toString(),
                pluralStringResource(R.plurals.n_song, albumData.songs.size, albumData.songs.size),
                durationStr
            ).joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .padding(top = 4.dp)
        )

        // Action Play / Shuffle Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                .padding(horizontal = horizontalPadding)
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
            ShimmerHost(modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Spacer(modifier = Modifier.fillMaxWidth().height(12.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp)))
                    Spacer(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp)))
                }
            }
        } else {
            var isDescExpanded by rememberSaveable { mutableStateOf(false) }
            val fallbackRes = when (releaseType) {
                AlbumReleaseType.ALBUM -> R.string.album_fallback_description
                AlbumReleaseType.SINGLE -> R.string.single_fallback_description
                AlbumReleaseType.EP -> R.string.ep_fallback_description
            }
            val staticDesc = stringResource(
                fallbackRes,
                cleanAlbumTitle,
                albumData.artists.joinToString { it.name }
            )
            val desc = albumDescription ?: staticDesc

            Text(
                text = desc,
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

            if (canTranslate || isTranslated) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = horizontalPadding)
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTranslation() }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.translate),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isTranslated) stringResource(R.string.show_original) else stringResource(R.string.Translate),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
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
