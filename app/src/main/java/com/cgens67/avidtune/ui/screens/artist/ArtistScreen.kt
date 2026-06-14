package com.cgens67.avidtune.ui.screens.artist

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppBarHeight
import com.cgens67.avidtune.db.entities.ArtistEntity
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.togglePlayPause
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.ListQueue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.ui.component.AlbumGridItem
import com.cgens67.avidtune.ui.component.HideOnScrollFAB
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.NavigationTitle
import com.cgens67.avidtune.ui.component.SongListItem
import com.cgens67.avidtune.ui.component.YouTubeGridItem
import com.cgens67.avidtune.ui.component.YouTubeListItem
import com.cgens67.avidtune.ui.component.shimmer.ButtonPlaceholder
import com.cgens67.avidtune.ui.component.shimmer.ListItemPlaceHolder
import com.cgens67.avidtune.ui.component.shimmer.ShimmerHost
import com.cgens67.avidtune.ui.component.shimmer.TextPlaceholder
import com.cgens67.avidtune.ui.menu.AlbumMenu
import com.cgens67.avidtune.ui.menu.SongMenu
import com.cgens67.avidtune.ui.menu.YouTubeAlbumMenu
import com.cgens67.avidtune.ui.menu.YouTubeArtistMenu
import com.cgens67.avidtune.ui.menu.YouTubePlaylistMenu
import com.cgens67.avidtune.ui.menu.YouTubeSongMenu
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.ui.utils.fadingEdge
import com.cgens67.avidtune.ui.utils.isScrollingUp
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.viewmodels.ArtistViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ServiceCast")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
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
    val monthlyPlayCount by viewModel.monthlyPlayCount.collectAsState()
    val totalPlayCount by viewModel.totalPlayCount.collectAsState()

    val lazyListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLocal by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current

    // Calculate the offset value outside of the offset lambda
    val systemBarsTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
    val headerOffset = with(density) {
        -(systemBarsTopPadding + AppBarHeight).roundToPx()
    }

    val transparentAppBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 100
        }
    }

    LaunchedEffect(libraryArtist) {
        // always show local page for local artists. Show local page remote artist when offline
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier = Modifier
                            .offset {
                                IntOffset(x = 0, y = headerOffset)
                            }
                    ) {
                        // Artist Image Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1.1f),
                        ) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shimmer()
                                    .background(MaterialTheme.colorScheme.onSurface)
                                    .fadingEdge(
                                        top = systemBarsTopPadding + AppBarHeight,
                                        bottom = 200.dp,
                                    ),
                            )
                        }
                        // Artist Name and Controls Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Artist Name Placeholder
                            TextPlaceholder(
                                height = 36.dp,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .padding(bottom = 16.dp)
                            )

                            // Buttons Row Placeholder
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Subscribe Button Placeholder
                                ButtonPlaceholder(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .height(52.dp)
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Right side buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Radio Button Placeholder
                                    ButtonPlaceholder(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(52.dp)
                                    )

                                    // Shuffle Button Placeholder
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .shimmer()
                                            .background(
                                                MaterialTheme.colorScheme.onSurface,
                                                RoundedCornerShape(26.dp)
                                            )
                                    )
                                }
                            }
                        }
                        // Songs List Placeholder
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    var artistMonthlyListeners by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(artistName) {
                        if (artistName != null && !showLocal) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val encodedName = java.net.URLEncoder.encode(artistName, "UTF-8")
                                    val response = java.net.URL("https://api.deezer.com/search/artist?q=$encodedName").readText()
                                    val json = org.json.JSONObject(response)
                                    val data = json.optJSONArray("data")
                                    if (data != null && data.length() > 0) {
                                        val nbFan = data.getJSONObject(0).optInt("nb_fan", 0)
                                        if (nbFan > 0) {
                                            val estimatedListeners = nbFan * 12
                                            val formatted = when {
                                                estimatedListeners >= 1000000 -> String.format(java.util.Locale.US, "%.1fM", estimatedListeners / 1000000.0)
                                                estimatedListeners >= 1000 -> String.format(java.util.Locale.US, "%.1fK", estimatedListeners / 1000.0)
                                                else -> estimatedListeners.toString()
                                            }
                                            artistMonthlyListeners = formatted
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    Box {
                        // Artist Image with offset
                        if (thumbnail != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .offset {
                                        IntOffset(x = 0, y = headerOffset)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .fadingEdge(
                                            bottom = 200.dp,
                                        )
                                ) {
                                    AsyncImage(
                                        model = thumbnail.resize(1200, 1200),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Artist Name and Controls Section - positioned at bottom of image
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    top = if (thumbnail != null) {
                                        // Position content at the bottom part of the image
                                        // Using screen width to calculate aspect ratio height minus overlap
                                        context.resources.displayMetrics.widthPixels.let { screenWidth ->
                                            with(density) {
                                                ((screenWidth / 1.2f) - 144).toDp()
                                            }
                                        }
                                    } else {
                                        16.dp
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                // Artist Name
                                Text(
                                    text = artistName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 32.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                // Subscriber count and Streams badges
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    artistPage?.artist?.subscriberCountText?.let { subscribers ->
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
                                                text = subscribers,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (artistMonthlyListeners != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.graphic_eq),
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "$artistMonthlyListeners monthly listeners",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (monthlyPlayCount > 0) {
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
                                                text = "$monthlyPlayCount your monthly streams",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    if (totalPlayCount > 0 && totalPlayCount != monthlyPlayCount) {
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
                                                text = "$totalPlayCount your total streams",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // Description
                                var isDescriptionExpanded by rememberSaveable { mutableStateOf(false) }
                                val description = artistPage?.description

                                if (!showLocal && !description.isNullOrEmpty()) {
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .padding(bottom = 16.dp)
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

                                // Buttons Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        ButtonGroupDefaults.ConnectedSpaceBetween
                                    )
                                ) {
                                    // Subscribe Button
                                    ToggleButton(
                                        checked = libraryArtist?.artist?.bookmarkedAt != null,
                                        onCheckedChange = {
                                            database.transaction {
                                                val artist = libraryArtist?.artist
                                                if (artist != null) {
                                                    update(artist.toggleLike())
                                                } else {
                                                    artistPage?.artist?.let {
                                                        insert(
                                                            ArtistEntity(
                                                                id = it.id,
                                                                name = it.title,
                                                                channelId = it.channelId,
                                                                thumbnailUrl = it.thumbnail,
                                                            ).toggleLike()
                                                        )
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .semantics { role = Role.Button },
                                        shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                                        colors = ToggleButtonDefaults.toggleButtonColors(
                                            containerColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (libraryArtist?.artist?.bookmarkedAt != null)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                LocalContentColor.current
                                        )
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                    R.drawable.subscribed
                                                } else {
                                                    R.drawable.subscribe
                                                }
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                LocalContentColor.current
                                            }
                                        )
                                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                        Text(
                                            text = stringResource(
                                                if (libraryArtist?.artist?.bookmarkedAt != null) {
                                                    R.string.subscribed
                                                } else {
                                                    R.string.subscribe
                                                }
                                            ),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    // Radio Button
                                    if (!showLocal) {
                                        artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .semantics { role = Role.Button },
                                                shapes = ButtonGroupDefaults.connectedMiddleButtonShapes(),
                                                colors = ToggleButtonDefaults.toggleButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.radio),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    text = stringResource(R.string.radio),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    }

                                    // Shuffle Button
                                    if (!showLocal) {
                                        artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                                            ToggleButton(
                                                checked = false,
                                                onCheckedChange = {
                                                    playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .semantics { role = Role.Button },
                                                shapes = if (artistPage?.artist?.radioEndpoint != null) {
                                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                } else {
                                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                },
                                                colors = ToggleButtonDefaults.toggleButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.shuffle),
                                                    contentDescription = stringResource(R.string.shuffle),
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                                Text(
                                                    text = stringResource(R.string.shuffle),
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        }
                                    } else if (librarySongs.isNotEmpty()) {
                                        ToggleButton(
                                            checked = false,
                                            onCheckedChange = {
                                                val shuffledSongs = librarySongs.shuffled()
                                                if (shuffledSongs.isNotEmpty()) {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                            items = shuffledSongs.map { it.toMediaItem() }
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .semantics { role = Role.Button },
                                            shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                                            colors = ToggleButtonDefaults.toggleButtonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.shuffle),
                                                contentDescription = stringResource(R.string.shuffle),
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                                            Text(
                                                text = stringResource(R.string.shuffle),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            NavigationTitle(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    navController.navigate("artist/${viewModel.artistId}/songs")
                                }
                            )
                        }

                        itemsIndexed(
                            items = librarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" }
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
                                                        title = libraryArtist?.artist?.name ?: "Unknown Artist",
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
                    }

                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}") {
                                NavigationTitle(
                                    title = section.title,
                                    modifier = Modifier.animateItem(),
                                    onClick = section.moreEndpoint?.let {
                                        {
                                            navController.navigate(
                                                "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                            )
                                        }
                                    },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            itemsIndexed(
                                items = section.items.distinctBy { it.id },
                                key = { _, it -> "youtube_song_${it.id}" },
                            ) { index, song ->
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
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" },
                                    ) { item ->
                                        YouTubeGridItem(
                                            item = item,
                                            isActive = when (item) {
                                                is SongItem -> mediaMetadata?.id == item.id
                                                is AlbumItem -> mediaMetadata?.album?.id == item.id
                                                else -> false
                                            },
                                            isPlaying = isPlaying,
                                            coroutineScope = coroutineScope,
                                            thumbnailRatio = 1f, // Use square thumbnails for all items in horizontal scroll
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

                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                            is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                                        }
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            when (item) {
                                                                is SongItem ->
                                                                    YouTubeSongMenu(
                                                                        song = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is AlbumItem ->
                                                                    YouTubeAlbumMenu(
                                                                        albumItem = item,
                                                                        navController = navController,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is ArtistItem ->
                                                                    YouTubeArtistMenu(
                                                                        artist = item,
                                                                        onDismiss = menuState::dismiss,
                                                                    )

                                                                is PlaylistItem ->
                                                                    YouTubePlaylistMenu(
                                                                        playlist = item,
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
                }
            }
        }

        val isScrollingUp = lazyListState.isScrollingUp()
        val showLocalFab = librarySongs.isNotEmpty()

        // Library/Local Toggle FAB
        HideOnScrollFAB(
            visible = showLocalFab,
            lazyListState = lazyListState,
            icon = if (showLocal) R.drawable.language else R.drawable.library_music,
            onClick = {
                showLocal = showLocal.not()
                if (!showLocal && artistPage == null) {
                    // Do nothing here since fetchArtistsFromYTM is private.
                    // Instead, rely on LaunchedEffect triggering it if needed, or simply don't fetch again if already loading.
                }
            }
        )

        // Play All FAB
        val canPlayAll = (showLocal && librarySongs.isNotEmpty()) ||
                (!showLocal && artistPage?.sections?.any {
                    (it.items.firstOrNull() as? SongItem)?.album != null
                } == true)

        if (canPlayAll) {
            androidx.compose.animation.AnimatedVisibility(
                visible = isScrollingUp,
                enter = androidx.compose.animation.slideInVertically { it * 2 },
                exit = androidx.compose.animation.slideOutVertically { it * 2 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    // Add padding to position it above the other FAB (56dp height + 16dp padding + 8dp spacing)
                    // If the other FAB is visible.
                    .padding(bottom = if (showLocalFab) 64.dp else 0.dp)
            ) {
                val onPlayAllClick: () -> Unit = {
                    if (showLocal) {
                        if (librarySongs.isNotEmpty()) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                    items = librarySongs.map { it.toMediaItem() }
                                )
                            )
                        }
                    } else if (artistPage != null) {
                        val songSection = artistPage.sections.find { section ->
                            (section.items.firstOrNull() as? SongItem)?.album != null
                        }

                        val moreEndpoint = songSection?.moreEndpoint
                        if (moreEndpoint != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val result = YouTube.artistItems(moreEndpoint).getOrNull()
                                withContext(Dispatchers.Main) {
                                    if (result != null && result.items.isNotEmpty()) {
                                        val songs = result.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = artistPage.artist.title,
                                                items = songs
                                            )
                                        )
                                    } else {
                                        // Fallback to loaded items
                                        val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                                        if (songs.isNotEmpty()) {
                                            playerConnection.playQueue(
                                                ListQueue(
                                                    title = artistPage.artist.title,
                                                    items = songs
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (songSection != null) {
                            // Use loaded items if no more endpoint
                            val songs = songSection.items.filterIsInstance<SongItem>().map { it.toMediaItem() }
                            playerConnection.playQueue(
                                ListQueue(
                                    title = artistPage.artist.title,
                                    items = songs
                                )
                            )
                        } else {
                            // Fallback to shuffle endpoint (stripped) if no song section found
                            val shuffleEndpoint = artistPage.artist.shuffleEndpoint
                            if (shuffleEndpoint != null) {
                                val endpoint = if (shuffleEndpoint.playlistId != null) {
                                    WatchEndpoint(
                                        playlistId = shuffleEndpoint.playlistId,
                                        params = null, // Remove shuffle params to play in order
                                        videoId = null // Ensure videoId is null to start from beginning of playlist
                                    )
                                } else {
                                    shuffleEndpoint
                                }
                                playerConnection.playQueue(YouTubeQueue(endpoint))
                            }
                        }
                    }
                }

                if (showLocalFab) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .offset(x = (-4).dp), // Align center with standard FAB (56dp vs 48dp)
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                        )
                    }
                } else {
                    androidx.compose.material3.FloatingActionButton(
                        modifier = Modifier.padding(16.dp),
                        onClick = onPlayAllClick
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play All",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current)
                .align(Alignment.BottomCenter)
        )
    }

    // TopAppBar with dynamic transparency and standard design
    TopAppBar(
        title = {
            if (!transparentAppBar)
                Text(artistPage?.artist?.title.orEmpty())
        },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
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
                )
            }
        },
        colors = if (transparentAppBar) {
            TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    )
}
