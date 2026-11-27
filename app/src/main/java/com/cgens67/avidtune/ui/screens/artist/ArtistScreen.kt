package com.cgens67.avidtune.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.EnableArtistCanvasKey
import com.cgens67.avidtune.constants.EnableDescriptionsKey
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.ArtistEntity
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AppConfig
import com.cgens67.avidtune.ui.component.ArtistCanvasHelper
import com.cgens67.avidtune.ui.component.ArtistVideo
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.MenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.component.shimmer.TextPlaceholder
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.fadingEdge
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberPreference
import com.cgens67.avidtune.viewmodels.ArtistViewModel
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.EpisodeItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.PodcastItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.innertube.pages.ArtistPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ServiceCast")
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun ArtistScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsState()
    val librarySongs by viewModel.librarySongs.collectAsState()

    val (enableDescriptions) = rememberPreference(EnableDescriptionsKey, defaultValue = true)
    val artistDescription by viewModel.artistDescription.collectAsState()
    val descriptionProvider by viewModel.descriptionProvider.collectAsState()
    val isTranslated by viewModel.isTranslated.collectAsState()
    val canTranslate by viewModel.canTranslate.collectAsState()
    val isDescriptionLoading by viewModel.isDescriptionLoading.collectAsState()

    val totalPlayCount by remember(viewModel.artistId) { database.artistTotalPlayCount(viewModel.artistId) }.collectAsState(initial = 0)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val lazyListState = rememberLazyListState()
    val landscapeScrollState = rememberScrollState()

    var thumbnailCornerRadius by remember { mutableFloatStateOf(16f) }
    LaunchedEffect(Unit) {
        thumbnailCornerRadius = AppConfig.getThumbnailCornerRadius(context)
    }

    // Gradient colors for mesh background
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Get thumbnail URL
    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name ?: stringResource(R.string.unknown)

    val (enableArtistCanvas) = rememberPreference(EnableArtistCanvasKey, defaultValue = true)

    // Artist Canvas (always fetch 1:1 square version to avoid aspect-ratio distortion)
    var artistVideoUrl by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(artistName, enableArtistCanvas) {
        if (enableArtistCanvas && artistName.isNotBlank() && artistName != context.getString(R.string.unknown)) {
            artistVideoUrl = ArtistCanvasHelper.getArtistCanvas(
                context = context,
                artistName = artistName,
                preferSquare = true,
            )
        } else {
            artistVideoUrl = null
        }
    }

    // Skeleton loading state
    var isFetching by remember { mutableStateOf(true) }
    LaunchedEffect(artistPage) {
        if (artistPage != null) {
            isFetching = false
        }
    }
    LaunchedEffect(Unit) {
        delay(2500)
        isFetching = false
    }

    // Extract gradient colors from artist image
    LaunchedEffect(thumbnail) {
        if (thumbnail != null) {
            val request = ImageRequest.Builder(context)
                .data(thumbnail)
                .size(100, 100)
                .allowHardware(false)
                .build()

            val result = runCatching {
                context.imageLoader.execute(request).drawable
            }.getOrNull()

            if (result != null) {
                val bitmap = (result as? android.graphics.drawable.BitmapDrawable)?.bitmap
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

    // Calculate gradient opacity based on scroll position
    val gradientAlpha by remember {
        derivedStateOf {
            if (isLandscape) {
                (1f - (landscapeScrollState.value / 500f)).coerceIn(0f, 1f)
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

    val onSubscribeClick: () -> Unit = {
        val isSubscribing = libraryArtist?.artist?.bookmarkedAt == null
        database.transaction {
            val artist = libraryArtist?.artist
            if (artist != null) {
                update(artist.localToggleLike())
            } else {
                artistPage?.artist?.let {
                    insert(
                        ArtistEntity(
                            id = it.id,
                            name = it.title,
                            channelId = it.channelId,
                            thumbnailUrl = it.thumbnail,
                        ).localToggleLike()
                    )
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val artistId = artistPage?.artist?.id ?: libraryArtist?.artist?.id ?: return@launch
                val channelId = artistPage?.artist?.channelId ?: libraryArtist?.artist?.channelId
                val targetChannelId = channelId ?: YouTube.getChannelId(artistId)

                if (targetChannelId.isNotEmpty()) {
                    YouTube.subscribeChannel(targetChannelId, isSubscribing)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                    .fillMaxHeight(if (isLandscape) 1f else 0.65f)
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
                            val leftPaneWidth = width * 0.38f
                            val centerX = leftPaneWidth * 0.5f
                            val centerY = (140.dp.toPx()).coerceAtMost(height * 0.42f)
                            val radius = maxOf(height * 0.85f, leftPaneWidth * 1.15f)

                            if (gradientColors.size >= 3) {
                                val c0 = gradientColors[0]
                                val c1 = gradientColors[1]
                                val c2 = gradientColors[2]

                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c0, 0.78f),
                                            adaptColor(c0, 0.36f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX, centerY),
                                        radius = radius
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c1, 0.56f),
                                            adaptColor(c1, 0.22f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX * 0.4f, centerY * 0.65f),
                                        radius = radius * 0.8f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c2, 0.52f),
                                            adaptColor(c2, 0.18f),
                                            Color.Transparent
                                        ),
                                        center = Offset(centerX * 1.55f, centerY * 1.35f),
                                        radius = radius * 0.85f
                                    )
                                )
                            } else if (gradientColors.isNotEmpty()) {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(gradientColors[0], 0.68f),
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
                                        surfaceColor.copy(alpha = 0.55f),
                                        surfaceColor,
                                        surfaceColor
                                    ),
                                    startX = leftPaneWidth * 0.65f,
                                    endX = leftPaneWidth * 1.12f
                                )
                            )

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = 0.65f),
                                        surfaceColor
                                    ),
                                    startY = height * 0.65f,
                                    endY = height
                                )
                            )
                        } else {
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
                                        center = Offset(width * 0.5f, height * 0.2f),
                                        radius = width * 0.7f
                                    )
                                )
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            adaptColor(c1, 0.56f),
                                            adaptColor(c1, 0.30f),
                                            Color.Transparent
                                        ),
                                        center = Offset(width * 0.15f, height * 0.35f),
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
                                        center = Offset(width * 0.85f, height * 0.45f),
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
                                        center = Offset(width * 0.35f, height * 0.6f),
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
                                        center = Offset(width * 0.55f, height * 0.85f),
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
                                        center = Offset(width * 0.5f, height * 0.3f),
                                        radius = width * 0.8f
                                    )
                                )
                            }

                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        surfaceColor.copy(alpha = gradientAlpha * 0.22f),
                                        surfaceColor.copy(alpha = gradientAlpha * 0.55f),
                                        surfaceColor
                                    ),
                                    startY = height * 0.4f,
                                    endY = height
                                )
                            )
                        }
                    }
            )
        }

        if (isLandscape) {
            val statusBarTop = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
            val awareBottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()
            val isCompactHeight = configuration.screenHeightDp < 480
            val landscapeArtworkSize = if (isCompactHeight) 96.dp else 156.dp

            val topSongsSection = artistPage?.sections?.firstOrNull { s -> s.items.all { it is SongItem } }
            val jumpEntries = remember(librarySongs, artistPage) {
                buildList<Pair<String, Int>> {
                    var currentIndex = 0
                    if (librarySongs.isNotEmpty()) {
                        add("" to currentIndex)
                        currentIndex += 1
                    }
                    artistPage?.sections?.forEach { section ->
                        if (section.items.isNotEmpty()) {
                            add(section.title to currentIndex)
                            currentIndex += if (section === topSongsSection) {
                                1 + section.items.distinctBy { it.id }.chunked(2).size
                            } else {
                                2
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                    .padding(horizontal = 16.dp),
            ) {
                // Left Zone: Sculpted Artist Hero Card + Bio + Section Jump Rail
                Column(
                    modifier = Modifier
                        .weight(0.37f)
                        .fillMaxHeight()
                        .verticalScroll(landscapeScrollState)
                        .padding(
                            top = statusBarTop + 56.dp,
                            bottom = awareBottom + 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (isFetching && artistPage == null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ShimmerHost(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .size(landscapeArtworkSize)
                                            .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        TextPlaceholder(height = 24.dp, modifier = Modifier.fillMaxWidth(0.85f))
                                        TextPlaceholder(height = 20.dp, modifier = Modifier.fillMaxWidth(0.6f))
                                    }
                                }
                                Spacer(Modifier.height(14.dp))
                                Spacer(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(Modifier.height(10.dp))
                                TextPlaceholder(modifier = Modifier.fillMaxWidth())
                                TextPlaceholder(modifier = Modifier.fillMaxWidth(0.75f))
                            }
                        }
                    } else {
                        // Hero Identity Card
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.82f),
                            shape = RoundedCornerShape(22.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (isCompactHeight) {
                                    // Compact phone landscape: Artwork + Name/Stats side by side
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        if (thumbnail != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(landscapeArtworkSize)
                                                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                AsyncImage(
                                                    model = thumbnail.resize(1200, 1200),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                                artistVideoUrl?.let { url ->
                                                    ArtistVideo(
                                                        videoUrl = url,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = artistName,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            ArtistBadgesRow(
                                                subscriberCountText = artistPage?.subscriberCountText,
                                                monthlyListenerCount = artistPage?.monthlyListenerCount,
                                                totalPlayCount = totalPlayCount,
                                                centered = false,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                    }
                                } else {
                                    // Larger tablet landscape: Editorial showcase header
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        if (thumbnail != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(landscapeArtworkSize)
                                                    .clip(RoundedCornerShape(thumbnailCornerRadius.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            ) {
                                                AsyncImage(
                                                    model = thumbnail.resize(1200, 1200),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize(),
                                                )
                                                artistVideoUrl?.let { url ->
                                                    ArtistVideo(
                                                        videoUrl = url,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = artistName,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        ArtistBadgesRow(
                                            subscriberCountText = artistPage?.subscriberCountText,
                                            monthlyListenerCount = artistPage?.monthlyListenerCount,
                                            totalPlayCount = totalPlayCount,
                                            centered = true,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                ArtistActionButtons(
                                    isSubscribed = libraryArtist?.artist?.bookmarkedAt != null,
                                    radioEndpoint = artistPage?.artist?.radioEndpoint,
                                    shuffleEndpoint = artistPage?.artist?.shuffleEndpoint,
                                    compactTwoRow = true,
                                    onSubscribeClick = onSubscribeClick,
                                    onPlayQueue = playerConnection::playQueue,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        // Artist Bio Card
                        if (enableDescriptions && (isDescriptionLoading || !artistDescription.isNullOrBlank())) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ArtistDescriptionSection(
                                    isDescriptionLoading = isDescriptionLoading,
                                    artistDescription = artistDescription,
                                    descriptionProvider = descriptionProvider,
                                    isTranslated = isTranslated,
                                    canTranslate = canTranslate,
                                    onToggleTranslation = viewModel::toggleDescriptionTranslation,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                )
                            }
                        }

                        // Quick Section Jump Pills
                        if (jumpEntries.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.78f),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                ) {
                                    jumpEntries.forEach { (rawTitle, targetIdx) ->
                                        val label = if (rawTitle.isEmpty()) {
                                            stringResource(R.string.filter_library)
                                        } else {
                                            getTranslatedArtistSectionTitle(rawTitle)
                                        }
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            shape = CircleShape,
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    coroutineScope.launch {
                                                        lazyListState.animateScrollToItem(targetIdx)
                                                    }
                                                },
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right Zone: 2-Column Track Grids + FlowRow Discography Cards
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(0.63f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(
                        top = statusBarTop + 56.dp,
                        bottom = awareBottom + 24.dp,
                    ),
                ) {
                    if (isFetching && artistPage == null) {
                        item(key = "landscape_shimmer_list") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                ShimmerHost(modifier = Modifier.padding(12.dp)) {
                                    repeat(6) {
                                        ListItemPlaceHolder()
                                    }
                                }
                            }
                        }
                    } else {
                        artistLandscapeSectionsContent(
                            artistId = viewModel.artistId,
                            artistName = artistName,
                            artistPage = artistPage,
                            libraryArtist = libraryArtist,
                            librarySongs = librarySongs,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic,
                            coroutineScope = coroutineScope,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = lazyListState,
                contentPadding = LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            ) {
                if (isFetching && artistPage == null) {
                    item(key = "shimmer") {
                        ShimmerHost {
                            // Header Image
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                // Title
                                TextPlaceholder(
                                    height = 32.dp,
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .padding(bottom = 16.dp)
                                )

                                // Badges (Subscribers, etc.)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(bottom = 16.dp)
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .width(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(
                                        modifier = Modifier
                                            .height(28.dp)
                                            .width(100.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }

                                // Description
                                TextPlaceholder(modifier = Modifier.fillMaxWidth())
                                TextPlaceholder(modifier = Modifier.fillMaxWidth(0.8f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Spacer(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .weight(1f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .weight(1f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                    Spacer(
                                        modifier = Modifier
                                            .height(48.dp)
                                            .weight(1f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }
                            }

                            // List items
                            repeat(6) {
                                ListItemPlaceHolder()
                            }
                        }
                    }
                } else {
                    item(key = "header") {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (thumbnail != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .fadingEdge(bottom = 80.dp)
                                ) {
                                    AsyncImage(
                                        model = thumbnail.resize(1200, 1200),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )

                                    artistVideoUrl?.let { url ->
                                        ArtistVideo(
                                            videoUrl = url,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }

                            // Artist Name and Controls Section
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 16.dp)
                            ) {
                                // Artist Name
                                Text(
                                    text = artistName,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Subscriber count and Streams badges
                                ArtistBadgesRow(
                                    subscriberCountText = artistPage?.subscriberCountText,
                                    monthlyListenerCount = artistPage?.monthlyListenerCount,
                                    totalPlayCount = totalPlayCount,
                                    centered = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                )

                                // Description Section: Controlled by EnableDescriptionsKey
                                if (enableDescriptions) {
                                    ArtistDescriptionSection(
                                        isDescriptionLoading = isDescriptionLoading,
                                        artistDescription = artistDescription,
                                        descriptionProvider = descriptionProvider,
                                        isTranslated = isTranslated,
                                        canTranslate = canTranslate,
                                        onToggleTranslation = viewModel::toggleDescriptionTranslation,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // Buttons Row
                                ArtistActionButtons(
                                    isSubscribed = libraryArtist?.artist?.bookmarkedAt != null,
                                    radioEndpoint = artistPage?.artist?.radioEndpoint,
                                    shuffleEndpoint = artistPage?.artist?.shuffleEndpoint,
                                    compactTwoRow = false,
                                    onSubscribeClick = onSubscribeClick,
                                    onPlayQueue = playerConnection::playQueue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 16.dp)
                                )
                            }
                        }
                    }

                    artistSectionsContent(
                        artistId = viewModel.artistId,
                        artistName = artistName,
                        artistPage = artistPage,
                        libraryArtist = libraryArtist,
                        librarySongs = librarySongs,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        coroutineScope = coroutineScope,
                    )
                }
            }
        }

        // Top App Bar
        TopAppBar(
            title = { },
            navigationIcon = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    com.cgens67.avidtune.ui.component.IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = {
                            artistPage?.artist?.shareLink?.let { link ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Artist Link", link)
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ArtistBadgesRow(
    subscriberCountText: String?,
    monthlyListenerCount: String?,
    totalPlayCount: Int,
    centered: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = if (centered) {
            Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        } else {
            Arrangement.spacedBy(8.dp)
        }
    ) {
        subscriberCountText?.let { subscribers ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = subscribers.split(" ").firstOrNull() ?: subscribers,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        monthlyListenerCount?.let { monthlyListeners ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = monthlyListeners.split(" ").firstOrNull() ?: monthlyListeners,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (totalPlayCount > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.history),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = totalPlayCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ArtistDescriptionSection(
    isDescriptionLoading: Boolean,
    artistDescription: String?,
    descriptionProvider: String?,
    isTranslated: Boolean,
    canTranslate: Boolean,
    onToggleTranslation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isDescriptionLoading) {
        ShimmerHost(modifier = modifier.padding(bottom = 4.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                )
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                )
            }
        }
    } else if (!artistDescription.isNullOrBlank()) {
        var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
        val showActionsRow = (canTranslate || isTranslated) || (isDescriptionExpanded && !descriptionProvider.isNullOrBlank())

        Column(
            modifier = modifier.animateContentSize()
        ) {
            Text(
                text = artistDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isDescriptionExpanded = !isDescriptionExpanded }
                    ),
                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (showActionsRow) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (canTranslate || isTranslated) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleTranslation() }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.translate),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isTranslated) stringResource(R.string.show_original) else stringResource(R.string.Translate),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (isDescriptionExpanded && !descriptionProvider.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        when (descriptionProvider) {
                                            "Wikipedia" -> R.drawable.language
                                            else -> R.drawable.info
                                        }
                                    ),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = stringResource(R.string.fetched_by, descriptionProvider),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtistActionButtons(
    isSubscribed: Boolean,
    radioEndpoint: WatchEndpoint?,
    shuffleEndpoint: WatchEndpoint?,
    compactTwoRow: Boolean,
    onSubscribeClick: () -> Unit,
    onPlayQueue: (YouTubeQueue) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasRadio = radioEndpoint != null
    val hasShuffle = shuffleEndpoint != null

    val subscribeColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = if (isSubscribed) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (isSubscribed) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

    val secondaryColors = ToggleButtonDefaults.toggleButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (compactTwoRow) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleButton(
                checked = isSubscribed,
                onCheckedChange = { onSubscribeClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { role = Role.Button },
                shapes = ToggleButtonDefaults.shapes(),
                colors = subscribeColors
            ) {
                Icon(
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSubscribed) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isSubscribed) R.string.subscribed else R.string.subscribe
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (hasRadio || hasShuffle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    radioEndpoint?.let { endpoint ->
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { onPlayQueue(YouTubeQueue(endpoint, isRadio = true)) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.Button },
                            shapes = if (hasShuffle) {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            } else {
                                ToggleButtonDefaults.shapes()
                            },
                            colors = secondaryColors
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.radio),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.radio),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    shuffleEndpoint?.let { endpoint ->
                        ToggleButton(
                            checked = false,
                            onCheckedChange = { onPlayQueue(YouTubeQueue(endpoint)) },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.Button },
                            shapes = if (hasRadio) {
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            } else {
                                ToggleButtonDefaults.shapes()
                            },
                            colors = secondaryColors
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.shuffle),
                                contentDescription = stringResource(R.string.shuffle),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.shuffle),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        ) {
            ToggleButton(
                checked = isSubscribed,
                onCheckedChange = { onSubscribeClick() },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.Button },
                shapes = if (hasRadio || hasShuffle) {
                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                } else {
                    ToggleButtonDefaults.shapes()
                },
                colors = subscribeColors
            ) {
                Icon(
                    painter = painterResource(
                        if (isSubscribed) R.drawable.subscribed else R.drawable.subscribe
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSubscribed) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        if (isSubscribed) R.string.subscribed else R.string.subscribe
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            radioEndpoint?.let { endpoint ->
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onPlayQueue(YouTubeQueue(endpoint, isRadio = true)) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Button },
                    shapes = if (hasShuffle) {
                        ButtonGroupDefaults.connectedMiddleButtonShapes()
                    } else {
                        ButtonGroupDefaults.connectedTrailingButtonShapes()
                    },
                    colors = secondaryColors
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.radio),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            shuffleEndpoint?.let { endpoint ->
                ToggleButton(
                    checked = false,
                    onCheckedChange = { onPlayQueue(YouTubeQueue(endpoint)) },
                    modifier = Modifier
                        .weight(1f)
                        .semantics { role = Role.Button },
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                    colors = secondaryColors
                ) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.shuffle),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
private fun LazyListScope.artistLandscapeSectionsContent(
    artistId: String,
    artistName: String,
    artistPage: ArtistPage?,
    libraryArtist: Artist?,
    librarySongs: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope,
) {
    // 1. Library Songs Card (2-column grid of tracks)
    if (librarySongs.isNotEmpty()) {
        item(key = "landscape_library_card") {
            val previewSongs = librarySongs.take(6)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ArtistLandscapeSectionHeader(
                        title = stringResource(R.string.filter_library),
                        count = librarySongs.size,
                        onMoreClick = { navController.navigate("artist/$artistId/songs") },
                    )

                    previewSongs.chunked(2).forEachIndexed { rowIdx, pair ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            pair.forEachIndexed { colIdx, song ->
                                val songIndex = rowIdx * 2 + colIdx
                                val active = song.id == mediaMetadata?.id
                                Surface(
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    border = if (active) {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                                    } else {
                                        null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp)),
                                ) {
                                    SongListItem(
                                        song = song,
                                        showInLibraryIcon = true,
                                        isActive = active,
                                        isPlaying = isPlaying,
                                        trailingContent = {
                                            IconButton(
                                                onClick = {
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song,
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
                                        modifier = Modifier.combinedClickable(
                                            onClick = {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.player.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = libraryArtist?.artist?.name ?: artistName,
                                                            items = librarySongs.map { it.toMediaItem() },
                                                            startIndex = songIndex
                                                        )
                                                    )
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ),
                                    )
                                }
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (librarySongs.size > 6) {
                        Surface(
                            onClick = { navController.navigate("artist/$artistId/songs") },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.view_all),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // 2. YouTube Sections
    val topSongsSection = artistPage?.sections?.firstOrNull { s -> s.items.all { it is SongItem } }

    artistPage?.sections?.fastForEachIndexed { sectionIndex, section ->
        if (section.items.isNotEmpty()) {
            val distinctItems = section.items.distinctBy { it.id }

            if (section === topSongsSection) {
                // Top Songs Header
                item(key = "landscape_section_header_${sectionIndex}_${section.title}") {
                    ArtistLandscapeSectionHeader(
                        title = getTranslatedArtistSectionTitle(section.title),
                        count = distinctItems.size,
                        onMoreClick = section.moreEndpoint?.let {
                            {
                                navController.navigate(
                                    "artist/$artistId/items?browseId=${it.browseId}?params=${it.params}",
                                )
                            }
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }

                // Top Songs 2-column Track Cards
                val songPairs = distinctItems.chunked(2)
                itemsIndexed(
                    items = songPairs,
                    key = { pairIdx, _ -> "landscape_top_songs_row_${sectionIndex}_$pairIdx" },
                ) { _, pair ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                    ) {
                        pair.forEach { rawItem ->
                            val song = rawItem as SongItem
                            val active = mediaMetadata?.id == song.id
                            Surface(
                                color = if (active) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLow
                                },
                                shape = RoundedCornerShape(14.dp),
                                border = if (active) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
                                } else {
                                    null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp)),
                            ) {
                                YouTubeListItem(
                                    item = song,
                                    isActive = active,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
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
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.player.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = song.id),
                                                        song.toMediaMetadata()
                                                    ),
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                YouTubeSongMenu(
                                                    song = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else {
                // Discography / Videos / Related Artists Header
                item(key = "landscape_section_header_${sectionIndex}_${section.title}") {
                    ArtistLandscapeSectionHeader(
                        title = getTranslatedArtistSectionTitle(section.title),
                        count = distinctItems.size,
                        onMoreClick = section.moreEndpoint?.let {
                            {
                                navController.navigate(
                                    "artist/$artistId/items?browseId=${it.browseId}?params=${it.params}",
                                )
                            }
                        },
                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    )
                }

                // FlowRow Grid Showcase Card (no cramped horizontal nested scrolling)
                item(key = "landscape_section_flow_${sectionIndex}_${section.title}") {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                        ) {
                            distinctItems.forEach { item ->
                                YouTubeGridItem(
                                    item = item,
                                    isActive = when (item) {
                                        is SongItem -> mediaMetadata?.id == item.id
                                        is AlbumItem -> mediaMetadata?.album?.id == item.id
                                        is EpisodeItem -> mediaMetadata?.id == item.id
                                        else -> false
                                    },
                                    isPlaying = isPlaying,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier.combinedClickable(
                                        onClick = {
                                            when (item) {
                                                is SongItem ->
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = item.id),
                                                            item.toMediaMetadata()
                                                        ),
                                                    )
                                                is EpisodeItem ->
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            WatchEndpoint(videoId = item.id),
                                                            item.asSongItem().toMediaMetadata()
                                                        ),
                                                    )
                                                is AlbumItem -> navController.navigate("album/${item.id}")
                                                is ArtistItem -> navController.navigate("artist/${item.id}")
                                                is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                when (item) {
                                                    is SongItem -> YouTubeSongMenu(
                                                        song = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                    is AlbumItem -> YouTubeAlbumMenu(
                                                        albumItem = item,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                    is ArtistItem -> YouTubeArtistMenu(
                                                        artist = item,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                    is PlaylistItem -> YouTubePlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                    is EpisodeItem -> YouTubeSongMenu(
                                                        song = item.asSongItem(),
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                    is PodcastItem -> YouTubePlaylistMenu(
                                                        playlist = item.asPlaylistItem(),
                                                        coroutineScope = coroutineScope,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            }
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistLandscapeSectionHeader(
    title: String,
    count: Int,
    onMoreClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onMoreClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onMoreClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            shape = CircleShape,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (onMoreClick != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = CircleShape,
            ) {
                Icon(
                    painter = painterResource(R.drawable.navigate_next),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.artistSectionsContent(
    artistId: String,
    artistName: String,
    artistPage: ArtistPage?,
    libraryArtist: Artist?,
    librarySongs: List<Song>,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    coroutineScope: CoroutineScope,
) {
    // Local Songs Section
    if (librarySongs.isNotEmpty()) {
        item(key = "library_songs_header") {
            NavigationTitle(
                title = stringResource(R.string.filter_library),
                onClick = {
                    navController.navigate("artist/$artistId/songs")
                }
            )
        }

        itemsIndexed(
            items = librarySongs.take(5),
            key = { _, item -> "local_song_${item.id}" },
        ) { index, song ->
            SongListItem(
                song = song,
                showInLibraryIcon = true,
                isActive = song.id == mediaMetadata?.id,
                isPlaying = isPlaying,
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (song.id == mediaMetadata?.id) {
                                playerConnection.player.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = libraryArtist?.artist?.name ?: artistName,
                                        items = librarySongs.map { it.toMediaItem() },
                                        startIndex = index
                                    )
                                )
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                SongMenu(
                                    originalSong = song,
                                    navController = navController,
                                    onDismiss = menuState::dismiss,
                                )
                            }
                        },
                    )
                    .animateItem(),
            )
        }

        if (librarySongs.size > 5) {
            item(key = "library_songs_view_all") {
                Surface(
                    onClick = {
                        navController.navigate("artist/$artistId/songs")
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.view_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // YouTube/Remote content sections
    val topSongsSection = artistPage?.sections?.firstOrNull { s -> s.items.all { it is SongItem } }

    artistPage?.sections?.fastForEachIndexed { sectionIndex, section ->
        if (section.items.isNotEmpty()) {
            item(
                key = "youtube_section_header_${sectionIndex}_${section.title}",
            ) {
                NavigationTitle(
                    title = getTranslatedArtistSectionTitle(section.title),
                    onClick = section.moreEndpoint?.let {
                        {
                            navController.navigate(
                                "artist/$artistId/items?browseId=${it.browseId}?params=${it.params}",
                            )
                        }
                    },
                )
            }
        }

        if (section === topSongsSection) {
            items(
                items = section.items.distinctBy { it.id },
                key = { "youtube_song_${it.id}" },
            ) { song ->
                YouTubeListItem(
                    item = song as SongItem,
                    isActive = mediaMetadata?.id == song.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = song,
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
                    modifier = Modifier
                        .combinedClickable(
                            onClick = {
                                if (song.id == mediaMetadata?.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(
                                            WatchEndpoint(videoId = song.id),
                                            song.toMediaMetadata()
                                        ),
                                    )
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuState.show {
                                    YouTubeSongMenu(
                                        song = song,
                                        navController = navController,
                                        onDismiss = menuState::dismiss,
                                    )
                                }
                            },
                        )
                        .animateItem(),
                )
            }
        } else {
            item(
                key = "youtube_section_grid_${sectionIndex}_${section.title}",
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = section.items.distinctBy { it.id },
                        key = {
                            val type = when (it) {
                                is SongItem -> "song"
                                is AlbumItem -> "album"
                                is ArtistItem -> "artist"
                                is PlaylistItem -> "playlist"
                                is EpisodeItem -> "episode"
                                is PodcastItem -> "podcast"
                                else -> "item"
                            }
                            "youtube_${type}_${it.id}"
                        },
                    ) { item ->
                        YouTubeGridItem(
                            item = item,
                            isActive = when (item) {
                                is SongItem -> mediaMetadata?.id == item.id
                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                is EpisodeItem -> mediaMetadata?.id == item.id
                                else -> false
                            },
                            isPlaying = isPlaying,
                            coroutineScope = coroutineScope,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        when (item) {
                                            is SongItem ->
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = item.id),
                                                        item.toMediaMetadata()
                                                    ),
                                                )
                                            is EpisodeItem ->
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        WatchEndpoint(videoId = item.id),
                                                        item.asSongItem().toMediaMetadata()
                                                    ),
                                                )

                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                            is PodcastItem -> navController.navigate("online_playlist/${item.id}")
                                        }
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        menuState.show {
                                            when (item) {
                                                is SongItem -> YouTubeSongMenu(
                                                    song = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                                is AlbumItem -> YouTubeAlbumMenu(
                                                    albumItem = item,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                                is ArtistItem -> YouTubeArtistMenu(
                                                    artist = item,
                                                    onDismiss = menuState::dismiss,
                                                )
                                                is PlaylistItem -> YouTubePlaylistMenu(
                                                    playlist = item,
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                                is EpisodeItem -> YouTubeSongMenu(
                                                    song = item.asSongItem(),
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                                is PodcastItem -> YouTubePlaylistMenu(
                                                    playlist = item.asPlaylistItem(),
                                                    coroutineScope = coroutineScope,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
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

    // Bottom spacing
    item(key = "bottom_spacer") {
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun getTranslatedArtistSectionTitle(title: String): String {
    return when {
        title.equals("top songs", ignoreCase = true) -> stringResource(R.string.artist_top_songs)
        title.equals("albums", ignoreCase = true) -> stringResource(R.string.artist_albums)
        title.equals("singles & eps", ignoreCase = true) || title.equals("singles and eps", ignoreCase = true) -> stringResource(R.string.artist_singles_eps)
        title.equals("videos", ignoreCase = true) -> stringResource(R.string.artist_videos)
        title.equals("live performances", ignoreCase = true) -> stringResource(R.string.artist_live_performances)
        title.equals("featured on", ignoreCase = true) -> stringResource(R.string.artist_featured_on)
        title.equals("fans might also like", ignoreCase = true) || title.equals("fan might also like", ignoreCase = true) -> stringResource(R.string.artist_fans_might_also_like)
        title.startsWith("playlists by", ignoreCase = true) -> {
            val artistName = title.substringAfter("by", "").trim()
            if (artistName.isNotEmpty()) {
                stringResource(R.string.artist_playlists_by, artistName)
            } else {
                stringResource(R.string.artist_playlists)
            }
        }
        else -> title
    }
}