package com.cgens67.avidtune.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.AudioQualityKey
import com.cgens67.innertube.models.AccountInfo
import com.cgens67.avidtune.db.entities.Album
import com.cgens67.avidtune.db.entities.Artist
import com.cgens67.avidtune.db.entities.SongWithStats
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.viewmodels.InsightViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ======= STATE & MODELS =======

val bbh_bartle = FontFamily(Font(R.font.bartle_regular))

object WrappedConstants {
    val YEAR = Calendar.getInstance().get(Calendar.YEAR)
    val PLAYLIST_NAME = "AvidTune Insight $YEAR"
}

data class MessagePair(val range: LongRange, val tease: String, val reveal: String)

object WrappedRepository {
    private val messages = listOf(
        MessagePair(0L..999L, "I really hope you are not dissapointed...", "That's **%d minutes**. Just warming up?"),
        MessagePair(1000L..4999L, "It seems like you found us recently...", "And you dedicated **%d minutes** to the tunes."),
        MessagePair(5000L..14999L, "Music is definitely your thing.", "**%d minutes** is a solid soundtrack for your year."),
        MessagePair(15000L..39999L, "Do you ever take your headphones off?", "**%d minutes** suggests music is your oxygen."),
        MessagePair(40000L..Long.MAX_VALUE, "Are you... okay?", "You literally lived here for **%d minutes**.")
    )

    fun getMessage(minutes: Long): MessagePair {
        val possibleMessages = messages.filter { minutes in it.range }
        val chosenMessage = if (possibleMessages.isNotEmpty()) possibleMessages.random() 
        else MessagePair(0L..Long.MAX_VALUE, "Looks like we lost count!", "But you definitely listened to **%d minutes** of music.")
        
        return chosenMessage.copy(reveal = chosenMessage.reveal.format(minutes))
    }
}

sealed class WrappedScreenType {
    data object Welcome : WrappedScreenType()
    data object MinutesTease : WrappedScreenType()
    data object MinutesReveal : WrappedScreenType()
    data object TotalSongs : WrappedScreenType()
    data object TopSongReveal : WrappedScreenType()
    data object Top5Songs : WrappedScreenType()
    data object TotalAlbums : WrappedScreenType()
    data object TopAlbumReveal : WrappedScreenType()
    data object Top5Albums : WrappedScreenType()
    data object TotalArtists : WrappedScreenType()
    data object TopArtistReveal : WrappedScreenType()
    data object Top5Artists : WrappedScreenType()
    data object Playlist : WrappedScreenType()
    data object Conclusion : WrappedScreenType()
}

sealed class PlaylistCreationState {
    data object Idle : PlaylistCreationState()
    data object Creating : PlaylistCreationState()
    data object Success : PlaylistCreationState()
}

data class WrappedState(
    val accountInfo: AccountInfo? = null,
    val totalMinutes: Long = 0,
    val topSongs: List<SongWithStats> = emptyList(),
    val topArtists: List<Artist> = emptyList(),
    val top5Albums: List<Album> = emptyList(),
    val topAlbum: Album? = null,
    val uniqueSongCount: Int = 0,
    val uniqueArtistCount: Int = 0,
    val totalAlbums: Int = 0,
    val isDataReady: Boolean = false,
    val trackMap: Map<WrappedScreenType, String?> = emptyMap(),
    val playlistCreationState: PlaylistCreationState = PlaylistCreationState.Idle
)

// ======= AUDIO SERVICE =======

class WrappedAudioService(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var player: ExoPlayer? = null
    private var playbackJob: Job? = null

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private fun initPlayer() {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                volume = if (_isMuted.value) 0f else 1f
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Timber.tag("WrappedAudioService").e(error, "Player error")
                        playbackJob?.cancel()
                    }
                })
            }
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        player?.volume = if (_isMuted.value) 0f else 1f
    }

    private suspend fun prepareTrack(songId: String?) {
        initPlayer()
        val songUri = getSongUri(songId)
        withContext(Dispatchers.Main) {
            if (songUri != null) {
                val mediaItem = MediaItem.Builder()
                    .setUri(songUri)
                    .setMediaId(songId ?: "fallback")
                    .build()
                player?.setMediaItem(mediaItem)
                player?.prepare()
            }
        }
    }

    fun playTrack(songId: String?) {
        if (player?.currentMediaItem?.mediaId == songId) {
            if (player?.isPlaying == false) player?.play()
            return
        }
        playbackJob?.cancel()
        playbackJob = scope.launch {
            try {
                prepareTrack(songId)
                withContext(Dispatchers.Main) {
                    if (songId != null && songId != "2-p9DM2Xvsc") {
                        player?.seekTo(30_000)
                    } else {
                        player?.seekTo(0)
                    }
                    player?.play()
                    player?.volume = if (_isMuted.value) 0f else 1f
                }
            } catch (e: Exception) {
                Timber.tag("WrappedAudioService").e(e, "Error during playback preparation")
            }
        }
    }

    private suspend fun getSongUri(songId: String?): Uri? {
        if (songId == null) return null
        return try {
            val audioQualityStr = context.dataStore.get(AudioQualityKey) ?: AudioQuality.AUTO.name
            val audioQuality = AudioQuality.valueOf(audioQualityStr)
            val playbackData = withContext(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                ).getOrNull()
            }
            val streamUrl = playbackData?.streamUrl
            if (streamUrl.isNullOrBlank()) null else streamUrl.toUri()
        } catch (e: Exception) {
            null
        }
    }

    fun pause() = player?.pause()
    fun resume() = player?.play()
    fun release() {
        playbackJob?.cancel()
        player?.release()
        player = null
    }
}

// ======= MAIN COMPOSABLE =======

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InsightScreen(navController: NavController) {
    val viewModel: InsightViewModel = hiltViewModel()
    
    val onClose: () -> Unit = {
        navController.previousBackStackEntry?.savedStateHandle?.set("wrapped_seen", true)
        navController.popBackStack()
    }
    BackHandler(onBack = onClose)

    val messagePairSaver = Saver<MessagePair, List<Any>>(
        save = { listOf(it.range.first, it.range.last, it.tease, it.reveal) },
        restore = {
            MessagePair(
                range = (it[0] as Long)..(it[1] as Long),
                tease = it[2] as String,
                reveal = it[3] as String
            )
        }
    )
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val audioService = remember { WrappedAudioService(view.context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> audioService.pause()
                Lifecycle.Event.ON_RESUME -> audioService.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
            lifecycleOwner.lifecycle.removeObserver(observer)
            audioService.release()
        }
    }

    val screens = remember {
        listOf(
            WrappedScreenType.Welcome,
            WrappedScreenType.MinutesTease,
            WrappedScreenType.MinutesReveal,
            WrappedScreenType.TotalSongs,
            WrappedScreenType.TopSongReveal,
            WrappedScreenType.Top5Songs,
            WrappedScreenType.TotalAlbums,
            WrappedScreenType.TopAlbumReveal,
            WrappedScreenType.Top5Albums,
            WrappedScreenType.TotalArtists,
            WrappedScreenType.TopArtistReveal,
            WrappedScreenType.Top5Artists,
            WrappedScreenType.Playlist,
            WrappedScreenType.Conclusion
        )
    }
    
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val state by viewModel.state.collectAsState()
    val isMuted by audioService.isMuted.collectAsState()
    
    val messagePair = rememberSaveable(state.totalMinutes, saver = messagePairSaver) {
        WrappedRepository.getMessage(state.totalMinutes)
    }

    LaunchedEffect(Unit) {
        viewModel.prepare()
    }

    LaunchedEffect(pagerState, state.trackMap) {
        if (state.trackMap.isEmpty()) return@LaunchedEffect

        snapshotFlow { pagerState.currentPage }.distinctUntilChanged().collect { page ->
            val screen = screens.getOrNull(page)
            audioService.playTrack(state.trackMap[screen])
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { audioService.toggleMute() }) {
                        val icon = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        Icon(painterResource(icon), contentDescription = "Mute", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            WrappedBackground {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (screens[page]) {
                        is WrappedScreenType.Welcome -> WrappedIntro { scope.launch { pagerState.animateScrollToPage(page = 1) } }
                        is WrappedScreenType.MinutesTease -> WrappedMinutesTease(
                            messagePair = messagePair,
                            onNavigateForward = { scope.launch { pagerState.animateScrollToPage(page = 2) } },
                            isDataReady = state.isDataReady
                        )
                        is WrappedScreenType.MinutesReveal -> WrappedMinutesScreen(
                            messagePair = messagePair, 
                            totalMinutes = state.totalMinutes,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.MinutesReveal)
                        )
                        is WrappedScreenType.TotalSongs -> WrappedTotalSongsScreen(
                            uniqueSongCount = state.uniqueSongCount,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalSongs)
                        )
                        is WrappedScreenType.TopSongReveal -> WrappedTopSongScreen(
                            topSong = state.topSongs.firstOrNull(),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopSongReveal)
                        )
                        is WrappedScreenType.Top5Songs -> WrappedTop5SongsScreen(
                            topSongs = state.topSongs.take(5),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Songs)
                        )
                        is WrappedScreenType.TotalAlbums -> WrappedTotalAlbumsScreen(
                            uniqueAlbumCount = state.totalAlbums,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalAlbums)
                        )
                        is WrappedScreenType.TopAlbumReveal -> WrappedTopAlbumScreen(
                            topAlbum = state.topAlbum,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopAlbumReveal)
                        )
                        is WrappedScreenType.Top5Albums -> WrappedTop5AlbumsScreen(
                            topAlbums = state.top5Albums,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Albums)
                        )
                        is WrappedScreenType.TotalArtists -> WrappedTotalArtistsScreen(
                            uniqueArtistCount = state.uniqueArtistCount,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalArtists)
                        )
                        is WrappedScreenType.TopArtistReveal -> WrappedTopArtistScreen(
                            topArtist = state.topArtists.firstOrNull(),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopArtistReveal)
                        )
                        is WrappedScreenType.Top5Artists -> WrappedTop5ArtistsScreen(
                            topArtists = state.topArtists,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Artists)
                        )
                        is WrappedScreenType.Playlist -> PlaylistPage(
                            state = state,
                            onCreatePlaylist = { viewModel.createPlaylist("previewalbum") }
                        )
                        is WrappedScreenType.Conclusion -> ConclusionPage(onClose = onClose)
                    }
                }
            }
        }
    }
}

// ======= ANIMATED BACKGROUNDS =======

enum class ShapeType { Circle, Rect, Line }

private data class AnimatedElement(
    val shapeType: ShapeType,
    val initialX: Float, val initialY: Float,
    val targetX: Float, val targetY: Float,
    val size: Float, val alpha: Float, val duration: Int
)

@Composable
fun AnimatedBackground(elementCount: Int = 20, shapeTypes: List<ShapeType> = listOf(ShapeType.Circle)) {
    val random = remember { Random(System.currentTimeMillis()) }
    val elements = remember {
        List(elementCount) {
            val shapeType = shapeTypes.random(random)
            AnimatedElement(
                shapeType = shapeType,
                initialX = random.nextFloat(), initialY = random.nextFloat(),
                targetX = random.nextFloat(), targetY = random.nextFloat(),
                size = if (shapeType == ShapeType.Circle) random.nextFloat() * 15f + 5f else random.nextFloat() * 50f + 10f,
                alpha = random.nextFloat() * 0.3f + 0.1f,
                duration = random.nextInt(4000, 10000)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "anim_bg")
    val progressAnims = elements.mapIndexed { index, el ->
        infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(el.duration, easing = LinearEasing), RepeatMode.Reverse),
            label = "el_$index"
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        elements.forEachIndexed { index, element ->
            val progress = progressAnims[index].value
            val currentX = element.initialX + (element.targetX - element.initialX) * progress
            val currentY = element.initialY + (element.targetY - element.initialY) * progress

            when (element.shapeType) {
                ShapeType.Circle -> drawCircle(color = Color.White.copy(alpha = element.alpha), radius = element.size, center = Offset(currentX * size.width, currentY * size.height))
                ShapeType.Rect -> drawRect(color = Color.White.copy(alpha = element.alpha), topLeft = Offset(currentX * size.width, currentY * size.height), size = Size(element.size, element.size))
                ShapeType.Line -> drawLine(color = Color.White.copy(alpha = element.alpha), start = Offset(currentX * size.width, currentY * size.height), end = Offset((currentX + 0.1f) * size.width, (currentY + 0.1f) * size.height), strokeWidth = 2f)
            }
        }
    }
}

@Composable
fun WrappedBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "wrapped_bg")
    val blob1Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "blob1"
    )
    val blob2Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "blob2"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    val blob1Colors = remember { listOf(Color(0xFF7C3AED).copy(alpha = 0.4f), Color.Transparent) }
    val blob2Colors = remember { listOf(Color(0xFF06B6D4).copy(alpha = 0.3f), Color.Transparent) }
    val blob3Colors = remember { listOf(Color(0xFFDB2777).copy(alpha = 0.2f), Color.Transparent) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF0F0620))) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        val dotPoints = remember(widthPx, heightPx) {
            val points = ArrayList<Offset>()
            if (widthPx > 0 && heightPx > 0) {
                val dotSpacing = 30f
                for (x in 0..(widthPx / dotSpacing).toInt()) {
                    for (y in 0..(heightPx / dotSpacing).toInt()) {
                        points.add(Offset(x * dotSpacing, y * dotSpacing))
                    }
                }
            }
            points
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val b1X = w * 0.3f + sin(blob1Offset) * w * 0.2f
            val b1Y = h * 0.2f + cos(blob1Offset) * h * 0.1f
            drawCircle(brush = Brush.radialGradient(blob1Colors, Offset(b1X, b1Y), w * 0.8f * scale), radius = w * 0.8f * scale, center = Offset(b1X, b1Y))

            val b2X = w * 0.7f + cos(blob2Offset) * w * 0.2f
            val b2Y = h * 0.8f + sin(blob2Offset) * h * 0.1f
            drawCircle(brush = Brush.radialGradient(blob2Colors, Offset(b2X, b2Y), w * 0.9f * scale), radius = w * 0.9f * scale, center = Offset(b2X, b2Y))

            drawCircle(brush = Brush.radialGradient(blob3Colors, Offset(w * 0.1f, h * 0.9f), w * 0.6f), radius = w * 0.6f, center = Offset(w * 0.1f, h * 0.9f))

            if (dotPoints.isNotEmpty()) {
                drawPoints(points = dotPoints, pointMode = PointMode.Points, color = Color.White.copy(alpha = 0.05f), strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Composable
fun AnimatedDecorativeElement(modifier: Modifier = Modifier, isVisible: Boolean) {
    val rotation = remember { Animatable(0f) }
    val shapeType = remember { Random.nextInt(3) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(Random.nextLong(500))
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(Random.nextInt(1000, 3000)), RepeatMode.Restart)
            )
        }
    }
    Canvas(modifier = modifier.graphicsLayer { rotationZ = rotation.value }) {
        val strokeWidth = 2.dp.toPx()
        when (shapeType) {
            0 -> drawArc(color = Color.White.copy(alpha = 0.2f), startAngle = 0f, sweepAngle = 90f, useCenter = false, style = Stroke(strokeWidth))
            1 -> drawCircle(color = Color.White.copy(alpha = 0.2f), style = Stroke(strokeWidth))
            2 -> drawRect(color = Color.White.copy(alpha = 0.2f), style = Stroke(strokeWidth))
        }
    }
}

@Composable
fun AutoResizingText(text: String, modifier: Modifier = Modifier, style: TextStyle) {
    var scaledTextStyle by remember { mutableStateOf(style) }
    var readyToDraw by remember { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = scaledTextStyle,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowWidth) {
                scaledTextStyle = scaledTextStyle.copy(fontSize = scaledTextStyle.fontSize * 0.9)
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun FormattedText(text: String, modifier: Modifier = Modifier, style: TextStyle) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split("(?=\\*\\*)|(?<=\\*\\*)".toRegex())
        var isBold = false
        for (part in parts) {
            if (part == "**") {
                isBold = !isBold
            } else {
                withStyle(SpanStyle(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)) { append(part) }
            }
        }
    }
    Text(
        text = annotatedString,
        modifier = modifier,
        style = style
    )
}

// ======= WRAPPED PAGES =======

@Composable
fun WrappedIntro(onNext: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); visible = true }

    WrappedBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Image(
                    painter = painterResource(R.drawable.avidtune),
                    contentDescription = null,
                    modifier = Modifier.size(100.dp).clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
            ) {
                val baseStyle = TextStyle(fontFamily = bbh_bartle, textAlign = TextAlign.Center, letterSpacing = 2.sp, fontSize = 50.sp)
                Box {
                    AutoResizingText(text = "AvidTune Insight", modifier = Modifier.padding(start = 2.dp, top = 2.dp), style = baseStyle.copy(color = Color.DarkGray))
                    AutoResizingText(text = "AvidTune Insight", modifier = Modifier.padding(start = 1.dp, top = 1.dp), style = baseStyle.copy(color = Color.Gray))
                    AutoResizingText(text = "AvidTune Insight", modifier = Modifier, style = baseStyle.copy(color = Color.White))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
            ) {
                Text(
                    text = "A look back at your year in music.",
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000)),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
        ) {
            Button(
                onClick = onNext,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Let's go!",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun WrappedMinutesTease(messagePair: MessagePair?, onNavigateForward: () -> Unit, isDataReady: Boolean) {
    LaunchedEffect(Unit) { delay(3500); onNavigateForward() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = messagePair != null && isDataReady,
            enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.9f, animationSpec = tween(1000))
        ) {
            Text(
                text = messagePair?.tease ?: "",
                modifier = Modifier.padding(horizontal = 24.dp),
                color = Color.White,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                fontFamily = bbh_bartle
            )
        }
    }
}

@Composable
fun WrappedMinutesScreen(messagePair: MessagePair?, totalMinutes: Long, isVisible: Boolean) {
    val animatedMinutes = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(isVisible, totalMinutes) {
        if (isVisible && totalMinutes > 0) animatedMinutes.animateTo(targetValue = totalMinutes.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            FormattedText(
                text = messagePair?.tease ?: "",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
                val textStyle = remember(totalMinutes, maxWidth) {
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(totalMinutes.toString(), style).size.width
                    while (textWidth > constraints.maxWidth) { style = style.copy(fontSize = style.fontSize * 0.95f); textWidth = textMeasurer.measure(totalMinutes.toString(), style).size.width }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(
                    text = animatedMinutes.value.toInt().toString(),
                    style = textStyle,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            FormattedText(
                text = messagePair?.reveal ?: "",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
fun WrappedTotalSongsScreen(uniqueSongCount: Int, isVisible: Boolean) {
    val animatedSongs = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(isVisible, uniqueSongCount) {
        if (isVisible && uniqueSongCount > 0) animatedSongs.animateTo(targetValue = uniqueSongCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Line))
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Songs you\nlistened to",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
                val textStyle = remember(uniqueSongCount, maxWidth) {
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(uniqueSongCount.toString(), style).size.width
                    while (textWidth > constraints.maxWidth) { style = style.copy(fontSize = style.fontSize * 0.95f); textWidth = textMeasurer.measure(uniqueSongCount.toString(), style).size.width }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(
                    text = animatedSongs.value.toInt().toString(),
                    style = textStyle,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "That's a lot of melodies.",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
fun WrappedTopSongScreen(topSong: SongWithStats?, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "But your absolute\nfavorite was...",
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(topSong?.thumbnailUrl).build(),
                    contentDescription = null,
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
            ) {
                Text(
                    text = topSong?.title ?: "No Data",
                    style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000))
            ) {
                Text(
                    text = "Listened for ${(topSong?.timeListened ?: 0) / 60000} minutes",
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f)),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WrappedTop5SongsScreen(topSongs: List<SongWithStats>, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 25, shapeTypes = listOf(ShapeType.Rect))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "Your Top 5 Songs",
                    fontSize = 40.sp,
                    fontFamily = bbh_bartle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.Start) {
                topSongs.forEachIndexed { index, song ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontFamily = bbh_bartle,
                                fontSize = 36.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            AsyncImage(
                                model = song.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(3.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
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
fun WrappedTotalAlbumsScreen(uniqueAlbumCount: Int, isVisible: Boolean) {
    val animatedAlbums = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible, uniqueAlbumCount) {
        if (isVisible) { visible = true; if (uniqueAlbumCount > 0) animatedAlbums.animateTo(targetValue = uniqueAlbumCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing)) }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "Albums you\ndiscovered",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
                val textStyle = remember(uniqueAlbumCount, maxWidth) {
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(uniqueAlbumCount.toString(), style).size.width
                    while (textWidth > constraints.maxWidth) { style = style.copy(fontSize = style.fontSize * 0.95f); textWidth = textMeasurer.measure(uniqueAlbumCount.toString(), style).size.width }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(
                    text = animatedAlbums.value.toInt().toString(),
                    style = textStyle,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
            ) {
                Text(
                    text = "That's a lot of skips and repeats.",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
fun WrappedTopAlbumScreen(topAlbum: Album?, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) visible = true }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Rect))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "But there was one album\nthat ruled them all.",
                    style = TextStyle(fontFamily = bbh_bartle, fontSize = 40.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 48.sp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
            ) {
                AsyncImage(
                    model = topAlbum?.album?.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.size(200.dp).clip(RoundedCornerShape(3.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
            ) {
                Text(
                    text = topAlbum?.album?.title ?: "No Data",
                    fontSize = 24.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
            ) {
                Text(
                    text = "Listened for ${(topAlbum?.timeListened ?: 0) / 60000} minutes",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WrappedTop5AlbumsScreen(topAlbums: List<Album>, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "Your Top 5\nAlbums",
                    style = TextStyle(fontFamily = bbh_bartle, fontSize = 48.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 56.sp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.Start) {
                topAlbums.forEachIndexed { index, album ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontFamily = bbh_bartle,
                                fontSize = 36.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            AsyncImage(
                                model = album.album.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(3.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = album.album.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${(album.timeListened ?: 0) / 60000} mins",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
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
fun WrappedTotalArtistsScreen(uniqueArtistCount: Int, isVisible: Boolean) {
    val animatedArtists = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(isVisible, uniqueArtistCount) {
        if (isVisible && uniqueArtistCount > 0) animatedArtists.animateTo(targetValue = uniqueArtistCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Artists you\nlistened to",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White, textAlign = TextAlign.Center)
            )
            Spacer(modifier = Modifier.height(32.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                val density = LocalDensity.current
                val baseStyle = MaterialTheme.typography.displayLarge.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
                val textStyle = remember(uniqueArtistCount, maxWidth) {
                    var style = baseStyle.copy(fontSize = 96.sp)
                    var textWidth = textMeasurer.measure(uniqueArtistCount.toString(), style).size.width
                    while (textWidth > constraints.maxWidth) { style = style.copy(fontSize = style.fontSize * 0.95f); textWidth = textMeasurer.measure(uniqueArtistCount.toString(), style).size.width }
                    style.copy(lineHeight = style.fontSize * 1.08f)
                }
                Text(
                    text = animatedArtists.value.toInt().toString(),
                    style = textStyle,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your music taste is quite diverse.",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
            )
        }
    }
}

@Composable
fun WrappedTopArtistScreen(topArtist: Artist?, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) visible = true }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
        ) {
            Text(
                text = "Your Top Artist",
                style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(topArtist?.artist?.thumbnailUrl).build(),
                contentDescription = null,
                modifier = Modifier.size(200.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
        ) {
            Text(
                text = topArtist?.artist?.name ?: "No Data",
                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
        ) {
            Text(
                text = "Listened for ${(topArtist?.timeListened ?: 0) / 60000} minutes",
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(alpha = 0.8f)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun WrappedTop5ArtistsScreen(topArtists: List<Artist>, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 15, shapeTypes = listOf(ShapeType.Line))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
            ) {
                Text(
                    text = "Top 5 Artists",
                    fontSize = 40.sp,
                    fontFamily = bbh_bartle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 44.sp
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Column(horizontalAlignment = Alignment.Start) {
                topArtists.forEachIndexed { index, artist ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontFamily = bbh_bartle,
                                fontSize = 36.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.width(40.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            AsyncImage(
                                model = artist.artist.thumbnailUrl,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = artist.artist.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${(artist.timeListened ?: 0) / 60000} mins",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
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
fun PlaylistPage(state: WrappedState, onCreatePlaylist: () -> Unit) {
    val playlistCreationState = state.playlistCreationState
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); startAnimation = true }
    val contentAlpha by animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(800, 200), label = "alpha")

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle))
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp).alpha(contentAlpha),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AutoResizingText(
                text = "Your Insight Playlist\nis ready.",
                style = TextStyle(fontFamily = bbh_bartle, fontSize = 40.sp, color = Color.White, textAlign = TextAlign.Center, lineHeight = 48.sp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(R.drawable.previewalbum),
                contentDescription = null,
                modifier = Modifier.size(256.dp).clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "AvidTune Insight ${WrappedConstants.YEAR}",
                style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = { if (playlistCreationState == PlaylistCreationState.Idle) onCreatePlaylist() },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.height(50.dp)
            ) {
                when (playlistCreationState) {
                    is PlaylistCreationState.Idle -> Text("Add to Library", style = TextStyle(color = Color.Black, fontWeight = FontWeight.Bold))
                    is PlaylistCreationState.Creating -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black, strokeWidth = 2.dp)
                    is PlaylistCreationState.Success -> Text("Saved!", style = TextStyle(color = Color.Black, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun ConclusionPage(onClose: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 30, shapeTypes = listOf(ShapeType.Circle, ShapeType.Line))
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.avidtune),
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Thank you for listening",
                style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "See you next time!",
                style = TextStyle(fontSize = 16.sp, color = Color.Gray)
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onClose,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    text = "Close Insight",
                    style = TextStyle(color = Color.Black, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
