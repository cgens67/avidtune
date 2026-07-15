@file:OptIn(ExperimentalFoundationApi::class)

package com.cgens67.avidtune.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
import androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
import androidx.media3.exoplayer.offline.Download.STATE_QUEUED
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.AlbumItem
import com.cgens67.innertube.models.ArtistItem
import com.cgens67.innertube.models.PlaylistItem
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.YTItem
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalDownloadUtil
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.GridThumbnailHeight
import com.cgens67.avidtune.constants.ListItemHeight
import com.cgens67.avidtune.constants.ListThumbnailSize
import com.cgens67.avidtune.constants.SmallGridThumbnailHeight
import com.cgens67.avidtune.constants.ThumbnailCornerRadius
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.Playlist
import com.cgens67.avidtune.db.entities.Song
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.playback.queues.LocalAlbumRadio
import com.cgens67.avidtune.ui.theme.extractThemeColor
import com.cgens67.avidtune.utils.getPlaylistImageUri
import com.cgens67.avidtune.utils.joinByBullet
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
inline fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    noinline subtitle: (@Composable RowScope.() -> Unit)? = null,
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            if (isActive) {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                modifier
                    .height(ListItemHeight)
                    .padding(horizontal = 8.dp)
            },
    ) {
        Box(
            modifier = Modifier.padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            thumbnailContent()
        }
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .basicMarquee()
                        .fillMaxWidth(),
            )

            if (subtitle != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    subtitle()
                }
            }
        }

        trailingContent()
    }
}

@Composable
fun ListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String?,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable () -> Unit,
    trailingContent: @Composable RowScope.() -> Unit = {},
    isActive: Boolean = false,
) = ListItem(
    title = title,
    subtitle = {
        badges()

        if (!subtitle.isNullOrEmpty()) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    thumbnailContent = thumbnailContent,
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@Composable
fun GridItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    badges: @Composable RowScope.() -> Unit = {},
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    fillMaxWidth: Boolean = false,
) {
    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)

            },
    ) {
        BoxWithConstraints(
            contentAlignment =
                Alignment.Center,
            modifier =
                if (fillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.height(GridThumbnailHeight)
                }
                    .aspectRatio(thumbnailRatio)
                    .clip(RoundedCornerShape(27.dp)),
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start,
            modifier =
                Modifier
                    .basicMarquee()
                    .fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SmallGridItem(
    modifier: Modifier = Modifier,
    title: String,
    thumbnailContent: @Composable BoxWithConstraintsScope.() -> Unit,
    thumbnailShape: Shape,
    thumbnailRatio: Float = 1f,
    isArtist: Boolean? = false,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = if (isArtist == true) Alignment.CenterHorizontally else Alignment.Start,
        modifier =
            modifier
                .fillMaxHeight()
                .width(GridThumbnailHeight * thumbnailRatio)
                .padding(12.dp),
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .height(SmallGridThumbnailHeight)
                    .aspectRatio(thumbnailRatio)
                    .clip(RoundedCornerShape(8.dp))
        ) {
            thumbnailContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.width(SmallGridThumbnailHeight),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start,
                modifier =
                    Modifier
                        .basicMarquee()
                        .fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    showLikedIcon: Boolean = true,
    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    isSwipeable: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
    inSelectionMode: Boolean = false,
    onSelectionChange: ((Boolean) -> Unit)? = null,
) {
    val content: @Composable () -> Unit = {
        ListItem(
            title = song.song.title,
            subtitle = joinByBullet(
                song.artists.joinToString { it.name },
                makeTimeString(song.song.duration * 1000L)
            ),
            badges = badges,
            thumbnailContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ListThumbnailSize),
                ) {
                    if (albumIndex != null) {
                        AnimatedVisibility(
                            visible = !isActive,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                            exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            } else {
                                Text(
                                    text = albumIndex.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    } else {
                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .zIndex(1000f)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            }
                        }
                        AsyncImage(
                            model = song.song.thumbnailUrl,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                        )
                    }

                    PlayingIndicatorBox(
                        isActive = isActive,
                        playWhenReady = isPlaying,
                        color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    color =
                                        if (albumIndex != null) {
                                            Color.Transparent
                                        } else {
                                            Color.Black.copy(
                                                alpha = 0.4f,
                                            )
                                        },
                                    shape = RoundedCornerShape(ThumbnailCornerRadius),
                                ),
                    )
                }
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive,
        )
    }

    if (isSwipeable) {
        SwipeToSongBox(
            mediaItem = song.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@Composable
fun SongGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,

    showInLibraryIcon: Boolean = false,
    showDownloadIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        if (showLikedIcon && song.song.liked) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
        }
        if (showInLibraryIcon && song.song.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 2.dp)
            )
        }
        if (showDownloadIcon) {
            val download by LocalDownloadUtil.current.getDownload(song.id)
                .collectAsState(initial = null)
            when (download?.state) {
                STATE_COMPLETED -> Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 2.dp)
                )

                STATE_QUEUED, STATE_DOWNLOADING -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 2.dp)
                )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = song.song.title,
    subtitle = joinByBullet(
        song.artists.joinToString { it.name },
        makeTimeString(song.song.duration * 1000L)
    ),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
            modifier =
                Modifier
                    .align(Alignment.Center),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier
)

@Composable
fun SongSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = SmallGridItem(
    title = song.song.title,
    thumbnailContent = {
        AsyncImage(
            model = song.song.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !(isActive && isPlaying),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    modifier = modifier,
)

@Composable
fun ArtistListItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(CircleShape),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun ArtistGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = {
        if (artist.artist.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
    },
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = artist.artist.name,
    subtitle = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),

            )
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun ArtistSmallGridItem(
    artist: Artist,
    modifier: Modifier = Modifier,
) = SmallGridItem(
    title = artist.artist.name,
    thumbnailContent = {
        AsyncImage(
            model = artist.artist.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    },
    thumbnailShape = CircleShape,
    modifier = modifier,
    isArtist = true,
)

@Composable
fun AlbumListItem(
    album: Album,
    modifier: Modifier = Modifier,
    showLikedIcon: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                    downloads[it.id]?.state == STATE_DOWNLOADING ||
                                    downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (showLikedIcon && album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = album.album.title,
    subtitle =
        joinByBullet(
            album.artists.joinToString { it.name },
            pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
            album.album.year?.toString(),
        ),
    badges = badges,
    thumbnailContent = {
        val database = LocalDatabase.current
        val coroutineScope = rememberCoroutineScope()

        AsyncImage(
            model =
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(album.album.thumbnailUrl)
                    .allowHardware(false)
                    .build(),
            contentDescription = null,
            onState = { state ->
                if (album.album.themeColor == null && state is AsyncImagePainter.State.Success) {
                    coroutineScope.launch(Dispatchers.IO) {
                        state.result.drawable.toBitmapOrNull()?.extractThemeColor()?.toArgb()
                            ?.let { color ->
                                database.query {
                                    update(album.album.copy(themeColor = color))
                                }
                            }
                    }
                }
            },
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )

        PlayingIndicatorBox(
            isActive = isActive,
            playWhenReady = isPlaying,
            modifier =
                Modifier
                    .size(ListThumbnailSize)
                    .background(
                        color = Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(ThumbnailCornerRadius),
                    ),
        )
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun AlbumGridItem(
    album: Album,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val downloadUtil = LocalDownloadUtil.current
        var songs by remember {
            mutableStateOf(emptyList<Song>())
        }

        LaunchedEffect(Unit) {
            database.albumSongs(album.id).collect {
                songs = it
            }
        }

        var downloadState by remember {
            mutableStateOf(Download.STATE_STOPPED)
        }

        LaunchedEffect(songs) {
            if (songs.isEmpty()) return@LaunchedEffect
            downloadUtil.downloads.collect { downloads ->
                downloadState =
                    if (songs.all { downloads[it.id]?.state == STATE_COMPLETED }) {
                        STATE_COMPLETED
                    } else if (songs.all {
                            downloads[it.id]?.state == STATE_QUEUED ||
                                    downloads[it.id]?.state == STATE_DOWNLOADING ||
                                    downloads[it.id]?.state == STATE_COMPLETED
                        }
                    ) {
                        STATE_DOWNLOADING
                    } else {
                        Download.STATE_STOPPED
                    }
            }
        }

        if (album.album.bookmarkedAt != null) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }

        when (downloadState) {
            STATE_COMPLETED ->
                Icon(
                    painter = painterResource(R.drawable.offline),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(18.dp)
                            .padding(end = 2.dp),
                )

            STATE_DOWNLOADING ->
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier =
                        Modifier
                            .size(16.dp)
                            .padding(end = 2.dp),
                )

            else -> {}
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = GridItem(
    title = album.album.title,
    subtitle = album.artists.joinToString { it.name },
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = album.album.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
        ) {
            val database = LocalDatabase.current
            val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable {
                            coroutineScope.launch {
                                database.albumWithSongs(album.id).first()?.let { albumWithSongs ->
                                    playerConnection.playQueue(
                                        LocalAlbumRadio(albumWithSongs)
                                    )
                                }
                            }
                        },
            ) {
                Icon(
                    painter = painterResource(R.drawable.play),
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun AlbumSmallGridItem(
    song: Song,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
) = song.song.albumName?.let {
    SmallGridItem(
        title = it,
        thumbnailContent = {
            AsyncImage(
                model = song.song.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        },
        thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
        modifier = modifier,
    )
}

@Composable
fun PlaylistListItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {},
    autoPlaylist: Boolean = false,
) = ListItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    },
    thumbnailContent = {
        val painter =
            when (playlist.playlist.name) {
                stringResource(R.string.liked) -> R.drawable.favorite_border
                stringResource(R.string.offline) -> R.drawable.offline
                stringResource(R.string.cached_playlist) -> R.drawable.cached
                else -> {
                    if (autoPlaylist) {
                        R.drawable.trending_up
                    } else {
                        R.drawable.queue_music

                    }
                }
            }
        when (playlist.thumbnails.size) {
            0 ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Icon(
                        painter = painterResource(painter),
                        contentDescription = null,
                        modifier = Modifier
                            .size(ListThumbnailSize / 2)
                            .align(Alignment.Center)
                    )
                }

            1 ->
                AsyncImage(
                    model = playlist.thumbnails[0],
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                )

            else ->
                Box(
                    modifier =
                        Modifier
                            .size(ListThumbnailSize)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                ) {
                    listOf(
                        Alignment.TopStart,
                        Alignment.TopEnd,
                        Alignment.BottomStart,
                        Alignment.BottomEnd,
                    ).fastForEachIndexed { index, alignment ->
                        AsyncImage(
                            model = playlist.thumbnails.getOrNull(index),
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .align(alignment)
                                    .size(ListThumbnailSize / 2),
                        )
                    }
                }
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
)

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    badges: @Composable RowScope.() -> Unit = { },
    fillMaxWidth: Boolean = false,
    autoPlaylist: Boolean = false,
    context: Context // Agregamos el contexto para obtener la URI de la imagen
) = GridItem(
    title = playlist.playlist.name,
    subtitle = if (autoPlaylist) {
        ""
    } else {
        if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
            pluralStringResource(
                R.plurals.n_song,
                playlist.playlist.remoteSongCount,
                playlist.playlist.remoteSongCount
            )
        } else {
            pluralStringResource(R.plurals.n_song, playlist.songCount, playlist.songCount)
        }
    },
    badges = badges,
    thumbnailContent = {
        val thumbnailUri =
            getPlaylistImageUri(context, playlist.playlist.id) // Obtener URI de la miniatura

        if (thumbnailUri != null) {
            // Si la URI de la imagen existe, la mostramos
            AsyncImage(
                model = thumbnailUri,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
        } else {
            // Si no hay miniatura, mostrar la imagen predeterminada
            val painter =
                when (playlist.playlist.name) {
                    stringResource(R.string.liked) -> R.drawable.favorite_border
                    stringResource(R.string.offline) -> R.drawable.offline
                    stringResource(R.string.cached_playlist) -> R.drawable.cached
                    else -> {
                        if (autoPlaylist) {
                            R.drawable.trending_up
                        } else {
                            R.drawable.queue_music
                        }
                    }
                }
            val width = maxWidth
            val Libcarditem = 25.dp
            when (playlist.thumbnails.size) {
                0 ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .clip(RoundedCornerShape(Libcarditem))
                    ) {
                        Icon(
                            painter = painterResource(painter),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier =
                                Modifier
                                    .size(width / 2)
                                    .align(Alignment.Center),
                        )
                    }

                1 ->
                    AsyncImage(
                        model = playlist.thumbnails[0],
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    )

                else ->
                    Box(
                        modifier =
                            Modifier
                                .size(width)
                                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                    ) {
                        listOf(
                            Alignment.TopStart,
                            Alignment.TopEnd,
                            Alignment.BottomStart,
                            Alignment.BottomEnd,
                        ).fastForEachIndexed { index, alignment ->
                            AsyncImage(
                                model = playlist.thumbnails.getOrNull(index),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .align(alignment)
                                        .size(width / 2),
                            )
                        }
                    }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)


@Composable
fun MediaMetadataListItem(
    mediaMetadata: MediaMetadata,
    modifier: Modifier,
    isSelected: Boolean = false,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) = ListItem(
    title = mediaMetadata.title,
    subtitle =
        joinByBullet(
            mediaMetadata.artists.joinToString { it.name },
            makeTimeString(mediaMetadata.duration * 1000L),
        ),
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(ListThumbnailSize),
        ) {
            if (isSelected) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .zIndex(1000f)
                            .clip(RoundedCornerShape(ThumbnailCornerRadius))
                            .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.done),
                        modifier = Modifier.align(Alignment.Center),
                        contentDescription = null,
                    )
                }
            }

            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            PlayingIndicatorBox(
                isActive = isActive,
                playWhenReady = isPlaying,
                color = Color.White,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            )
        }
    },
    trailingContent = trailingContent,
    modifier = modifier,
    isActive = isActive,
)

@Composable
fun YouTubeListItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    albumIndex: Int? = null,
    isSelected: Boolean = false,
    isSwipeable: Boolean = true,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_QUEUED, STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val content: @Composable () -> Unit = {
        ListItem(
            title = item.title,
            subtitle =
                when (item) {
                    is SongItem -> joinByBullet(
                        item.artists.joinToString { it.name },
                        makeTimeString(item.duration?.times(1000L))
                    )

                    is AlbumItem -> joinByBullet(
                        item.artists?.joinToString { it.name },
                        item.year?.toString()
                    )

                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                },
            badges = badges,
            thumbnailContent = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(ListThumbnailSize),
                ) {
                    val thumbnailShape =
                        if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
                    if (albumIndex != null) {
                        AnimatedVisibility(
                            visible = !isActive,
                            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                            exit = shrinkOut(shrinkTowards = Alignment.Center) + fadeOut(),
                        ) {
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            } else {
                                Text(
                                    text = albumIndex.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    } else {
                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .zIndex(1000f)
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius))
                                        .background(Color.Black.copy(alpha = 0.5f)),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.done),
                                    modifier = Modifier.align(Alignment.Center),
                                    contentDescription = null,
                                )
                            }
                        }
                        AsyncImage(
                            model = item.thumbnail,
                            contentDescription = null,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clip(thumbnailShape),
                        )
                    }

                    PlayingIndicatorBox(
                        isActive = isActive,
                        playWhenReady = isPlaying,
                        color = if (albumIndex != null) MaterialTheme.colorScheme.onBackground else Color.White,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    color =
                                        if (albumIndex != null) {
                                            Color.Transparent
                                        } else {
                                            Color.Black.copy(
                                                alpha = 0.4f,
                                            )
                                        },
                                    shape = thumbnailShape,
                                ),
                    )
                }
            },
            trailingContent = trailingContent,
            modifier = modifier,
            isActive = isActive,
        )
    }

    if (item is SongItem && isSwipeable) {
        SwipeToSongBox(
            mediaItem = item.toMediaItem(),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        content()
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun YouTubeGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    badges: @Composable RowScope.() -> Unit = {
        val database = LocalDatabase.current
        val song by database.song(item.id).collectAsState(initial = null)
        val album by database.album(item.id).collectAsState(initial = null)

        if (item is SongItem &&
            song?.song?.liked == true ||
            item is AlbumItem &&
            album?.album?.bookmarkedAt != null
        ) {
            Icon(
                painter = painterResource(R.drawable.favorite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem && song?.song?.inLibrary != null) {
            Icon(
                painter = painterResource(R.drawable.library_add_check),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item.explicit) {
            Icon(
                painter = painterResource(R.drawable.explicit),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(18.dp)
                        .padding(end = 2.dp),
            )
        }
        if (item is SongItem) {
            val downloads by LocalDownloadUtil.current.downloads.collectAsState()
            when (downloads[item.id]?.state) {
                STATE_COMPLETED ->
                    Icon(
                        painter = painterResource(R.drawable.offline),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .size(18.dp)
                                .padding(end = 2.dp),
                    )

                STATE_DOWNLOADING ->
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .padding(end = 2.dp),
                    )

                else -> {}
            }
        }
    },
    thumbnailRatio: Float = if (item is SongItem) 16f / 9 else 1f,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) {
    val thumbnailShape =
        if (item is ArtistItem) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
    val thumbnailRatio = thumbnailRatio

    Column(
        modifier =
            if (fillMaxWidth) {
                modifier
                    .padding(12.dp)
                    .fillMaxWidth()
            } else {
                modifier
                    .padding(12.dp)
                    .width(GridThumbnailHeight * thumbnailRatio)
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .aspectRatio(thumbnailRatio)
                    .clip(thumbnailShape),
        ) {
            AsyncImage(
                model = item.thumbnail,
                contentDescription = null,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = item is AlbumItem && !isActive,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
            ) {
                val database = LocalDatabase.current
                val playerConnection = LocalPlayerConnection.current ?: return@AnimatedVisibility

                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable {
                                var playlistId = ""
                                coroutineScope?.launch(Dispatchers.IO) {
                                    var albumWithSongs = database.albumWithSongs(item.id).first()
                                    if (albumWithSongs?.songs.isNullOrEmpty()) {
                                        YouTube
                                            .album(item.id)
                                            .onSuccess { albumPage ->
                                                playlistId = albumPage.album.playlistId
                                                database.transaction {
                                                    insert(albumPage)
                                                }
                                                albumWithSongs =
                                                    database.albumWithSongs(item.id).first()
                                            }.onFailure {
                                                reportException(it)
                                            }
                                    }
                                    albumWithSongs?.let {
                                        withContext(Dispatchers.Main) {
                                            playerConnection.service.getAutomix(playlistId)
                                            playerConnection.playQueue(
                                                LocalAlbumRadio(it),
                                            )
                                        }
                                    }
                                }
                            },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = thumbnailShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = item is SongItem && !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (item is ArtistItem) TextAlign.Center else TextAlign.Start,
            modifier =
                Modifier
                    .basicMarquee(iterations = 3)
                    .fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            badges()

            val subtitle =
                when (item) {
                    is SongItem -> joinByBullet(
                        item.artists.joinToString { it.name },
                        makeTimeString(item.duration?.times(1000L))
                    )

                    is AlbumItem -> joinByBullet(
                        item.artists?.joinToString { it.name },
                        item.year?.toString()
                    )

                    is ArtistItem -> null
                    is PlaylistItem -> joinByBullet(item.author?.name, item.songCountText)
                }

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun YouTubeSmallGridItem(
    item: YTItem,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope? = null,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
) = SmallGridItem(
    title = item.title,
    thumbnailContent = {
        AsyncImage(
            model = item.thumbnail,
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        )
        if (item is SongItem) {
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = if (isPlaying) 0.4f else 0f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape =
        when (item) {
            is ArtistItem -> CircleShape
            else -> RoundedCornerShape(ThumbnailCornerRadius)
        },
    modifier = modifier,
    isArtist =
        when (item) {
            is ArtistItem -> true
            else -> false
        },
)

@Composable
fun LocalSongsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ThumbnailCornerRadius)),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(ThumbnailCornerRadius),
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !(isActive && isPlaying),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(8.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalArtistsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        ) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(500)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = CircleShape,
                            ),
                ) {
                    if (isPlaying) {
                        PlayingIndicator(
                            color = Color.White,
                            modifier = Modifier.height(24.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    },
    thumbnailShape = CircleShape,
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)

@Composable
fun LocalAlbumsGrid(
    title: String,
    subtitle: String,
    badges:
    @Composable()
    (RowScope.() -> Unit) = {},
    thumbnailUrl: String?,
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    fillMaxWidth: Boolean = false,
    modifier: Modifier,
) = GridItem(
    title = title,
    subtitle = subtitle,
    badges = badges,
    thumbnailContent = {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(500)),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.Black.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
            ) {
                if (isPlaying) {
                    PlayingIndicator(
                        color = Color.White,
                        modifier = Modifier.height(24.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    },
    thumbnailShape = RoundedCornerShape(ThumbnailCornerRadius),
    fillMaxWidth = fillMaxWidth,
    modifier = modifier,
)


// =================================================================================
// Appended Custom Spotlight Cards and Utilities (Archivetune additions adapted for Avidtune)
// =================================================================================

@Composable
fun LibraryPinnedCollectionTile(
    title: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier,
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors =
                        listOf(
                            accentColor.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                    ),
                ),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistThumbnail(
    thumbnails: List<String>,
    size: Dp,
    placeHolder: @Composable () -> Unit,
    shape: Shape,
) {
    when (thumbnails.size) {
        0 -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
                placeHolder()
            }
        }
        1 -> {
            AsyncImage(
                model = thumbnails[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(shape),
            )
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape),
            ) {
                listOf(
                    Alignment.TopStart,
                    Alignment.TopEnd,
                    Alignment.BottomStart,
                    Alignment.BottomEnd,
                ).fastForEachIndexed { index, alignment ->
                    AsyncImage(
                        model = thumbnails.getOrNull(index),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .align(alignment)
                            .size(size / 2),
                    )
                }
            }
        }
    }
}

@Composable
private fun playlistCountText(
    playlist: Playlist,
    autoPlaylist: Boolean,
): String =
    if (autoPlaylist) {
        ""
    } else if (playlist.songCount == 0 && playlist.playlist.remoteSongCount != null) {
        pluralStringResource(
            R.plurals.n_song,
            playlist.playlist.remoteSongCount,
            playlist.playlist.remoteSongCount,
        )
    } else {
        pluralStringResource(
            R.plurals.n_song,
            playlist.songCount,
            playlist.songCount,
        )
    }

@Composable
private fun playlistPlaceholderIcon(
    playlist: Playlist,
    autoPlaylist: Boolean,
): Int =
    when (playlist.playlist.name) {
        stringResource(R.string.liked) -> R.drawable.favorite_border
        stringResource(R.string.offline) -> R.drawable.offline
        stringResource(R.string.cached_playlist) -> R.drawable.cached
        else -> if (autoPlaylist) R.drawable.trending_up else R.drawable.queue_music
    }

@Composable
fun LibraryPlaylistFeatureCard(
    playlist: Playlist,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    autoPlaylist: Boolean = false,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitleText = playlistCountText(playlist = playlist, autoPlaylist = autoPlaylist)
    val thumbnailSize = 72.dp
    val thumbnailShape = RoundedCornerShape(18.dp)
    val context = LocalContext.current
    val primaryThumbnailUrl = playlist.thumbnails.getOrNull(0)
    var extractedGlowColor by remember(primaryThumbnailUrl) { mutableStateOf(Color.Transparent) }
    val glowColor by animateColorAsState(
        targetValue = extractedGlowColor,
        animationSpec = tween(400),
        label = "playlistItemGlow",
    )
    LaunchedEffect(primaryThumbnailUrl) {
        if (primaryThumbnailUrl == null) return@LaunchedEffect
        val bitmap =
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(primaryThumbnailUrl)
                    .size(128)
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).drawable?.toBitmapOrNull()
            }.getOrNull() ?: return@LaunchedEffect
        extractedGlowColor = withContext(Dispatchers.Default) { bitmap.extractThemeColor() }
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = shape,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(thumbnailSize)
                    .shadow(
                        elevation = 34.dp,
                        shape = thumbnailShape,
                        clip = false,
                        ambientColor = glowColor.copy(alpha = 0.82f),
                        spotColor = glowColor.copy(alpha = 0.96f),
                    ),
            ) {
                PlaylistThumbnail(
                    thumbnails = playlist.thumbnails,
                    size = thumbnailSize,
                    placeHolder = {
                        Icon(
                            painter = painterResource(playlistPlaceholderIcon(playlist, autoPlaylist)),
                            contentDescription = null,
                            tint = LocalContentColor.current.copy(alpha = 0.8f),
                            modifier = Modifier.size(thumbnailSize / 2),
                        )
                    },
                    shape = thumbnailShape,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = playlist.playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
fun LibraryAlbumSpotlightCard(
    album: Album,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    isActive: Boolean = false,
    isPlaying: Boolean = false,
    onPlay: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val subtitle =
        joinByBullet(
            album.artists.joinToString { it.name },
            pluralStringResource(R.plurals.n_song, album.album.songCount, album.album.songCount),
        )
    val context = LocalContext.current
    var extractedGlowColor by remember(album.album.thumbnailUrl) { mutableStateOf(Color.Transparent) }
    val glowColor by animateColorAsState(
        targetValue = extractedGlowColor,
        animationSpec = tween(400),
        label = "albumItemGlow",
    )
    LaunchedEffect(album.album.thumbnailUrl) {
        val url = album.album.thumbnailUrl ?: return@LaunchedEffect
        val bitmap =
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(128)
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).drawable?.toBitmapOrNull()
            }.getOrNull() ?: return@LaunchedEffect
        extractedGlowColor = withContext(Dispatchers.Default) { bitmap.extractThemeColor() }
    }

    Card(
        shape = shape,
        colors =
        CardDefaults.cardColors(
            containerColor =
            if (isActive) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 34.dp,
                        shape = RoundedCornerShape(18.dp),
                        clip = false,
                        ambientColor = glowColor.copy(alpha = 0.82f),
                        spotColor = glowColor.copy(alpha = 0.96f),
                    ),
            ) {
                AsyncImage(
                    model = album.album.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                )
                if (onPlay != null) {
                    AlbumPlayButton(
                        visible = !isActive,
                        onClick = onPlay,
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = album.album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                    if (isActive) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
fun LibraryArtistSpotlightCard(
    artist: Artist,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    trailingContent: @Composable RowScope.() -> Unit = {},
) {
    val context = LocalContext.current
    var extractedGlowColor by remember(artist.artist.thumbnailUrl) { mutableStateOf(Color.Transparent) }
    val glowColor by animateColorAsState(
        targetValue = extractedGlowColor,
        animationSpec = tween(400),
        label = "artistItemGlow",
    )
    LaunchedEffect(artist.artist.thumbnailUrl) {
        val url = artist.artist.thumbnailUrl ?: return@LaunchedEffect
        val bitmap =
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(128)
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).drawable?.toBitmapOrNull()
            }.getOrNull() ?: return@LaunchedEffect
        extractedGlowColor = withContext(Dispatchers.Default) { bitmap.extractThemeColor() }
    }
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Box(
                modifier =
                Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 34.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = glowColor.copy(alpha = 0.82f),
                        spotColor = glowColor.copy(alpha = 0.96f),
                    ),
            ) {
                AsyncImage(
                    model = artist.artist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = artist.artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                trailingContent()
            }
        }
    }
}

@Composable
fun SwipeToSongBox(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    contentBackgroundColor: Color? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val ctx = LocalContext.current
    val player = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    val offset = remember { mutableStateOf(0f) }
    val threshold = 300f
    val resolvedContentBackgroundColor = contentBackgroundColor ?: MaterialTheme.colorScheme.surface

    val dragState =
        rememberDraggableState { delta ->
            offset.value = (offset.value + delta).coerceIn(-threshold, threshold)
        }

    Box(
        modifier =
        modifier
            .fillMaxWidth()
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = {
                    when {
                        offset.value >= threshold -> {
                            player?.playNext(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.play_next, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        offset.value <= -threshold -> {
                            player?.addToQueue(listOf(mediaItem))
                            Toast.makeText(ctx, R.string.add_to_queue, Toast.LENGTH_SHORT).show()
                            reset(offset, scope)
                        }

                        else -> {
                            reset(offset, scope)
                        }
                    }
                },
            ),
    ) {
        if (offset.value != 0f) {
            val (iconRes, bg, tint, align) =
                if (offset.value > 0) {
                    Quadruple(
                        R.drawable.playlist_play,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.onSecondary,
                        Alignment.CenterStart,
                    )
                } else {
                    Quadruple(
                        R.drawable.queue_music,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        Alignment.CenterEnd,
                    )
                }

            Box(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.Center)
                    .background(bg),
                contentAlignment = align,
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    modifier =
                    Modifier
                        .padding(horizontal = 24.dp)
                        .size(30.dp)
                        .alpha(0.9f),
                    tint = tint,
                )
            }
        }

        Box(
            modifier =
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(resolvedContentBackgroundColor),
            content = content,
        )
    }
}

private fun reset(
    offset: MutableState<Float>,
    scope: CoroutineScope,
) {
    scope.launch {
        animate(
            initialValue = offset.value,
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300),
        ) { value, _ -> offset.value = value }
    }
}

data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)

@Composable
fun BoxScope.AlbumPlayButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(onClick = onClick),
        ) {
            Icon(
                painter = painterResource(R.drawable.play),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
