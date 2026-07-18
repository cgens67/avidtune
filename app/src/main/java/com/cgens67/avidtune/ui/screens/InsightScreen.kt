package com.cgens67.avidtune.ui.screens

import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
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
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.utils.rememberEnumPreference
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
}

data class MessagePair(val range: LongRange, val teaseRes: Int, val revealRes: Int)

object WrappedRepository {
    private val messages = listOf(
        MessagePair(0L..999L, R.string.insight_msg_tease_0, R.string.insight_msg_reveal_0),
        MessagePair(1000L..4999L, R.string.insight_msg_tease_1000, R.string.insight_msg_reveal_1000),
        MessagePair(5000L..14999L, R.string.insight_msg_tease_5000, R.string.insight_msg_reveal_5000),
        MessagePair(15000L..39999L, R.string.insight_msg_tease_15000, R.string.insight_msg_reveal_15000),
        MessagePair(40000L..Long.MAX_VALUE, R.string.insight_msg_tease_40000, R.string.insight_msg_reveal_40000)
    )

    fun getMessage(minutes: Long): MessagePair {
        val possibleMessages = messages.filter { minutes in it.range }
        return if (possibleMessages.isNotEmpty()) possibleMessages.random() 
        else MessagePair(0L..Long.MAX_VALUE, R.string.insight_msg_tease_default, R.string.insight_msg_reveal_default)
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

    private fun fadeOut(onDone: () -> Unit) {
        scope.launch {
            var vol = player?.volume ?: 1f
            val targetVol = if (_isMuted.value) 0f else 1f
            if (vol > 0f && targetVol > 0f) {
                while (vol > 0f) {
                    vol -= 0.1f
                    if (vol < 0f) vol = 0f
                    player?.volume = vol
                    delay(30)
                }
            }
            onDone()
        }
    }

    private fun fadeIn() {
        scope.launch {
            player?.volume = 0f
            var vol = 0f
            val targetVol = if (_isMuted.value) 0f else 1f
            if (targetVol > 0f) {
                while (vol < targetVol) {
                    vol += 0.05f
                    if (vol > targetVol) vol = targetVol
                    player?.volume = vol
                    delay(30)
                }
            }
        }
    }

    fun playTrack(songId: String?) {
        if (player?.currentMediaItem?.mediaId == songId) {
            if (player?.isPlaying == false) {
                player?.play()
                fadeIn()
            }
            return
        }

        val doPlay = {
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
                        player?.volume = 0f
                        player?.play()
                        fadeIn()
                    }
                } catch (e: Exception) {
                    Timber.tag("WrappedAudioService").e(e, "Error during playback preparation")
                }
            }
        }

        if (player?.isPlaying == true) {
            fadeOut { doPlay() }
        } else {
            doPlay()
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
        save = { listOf(it.range.first, it.range.last, it.teaseRes, it.revealRes) },
        restore = {
            MessagePair(
                range = (it[0] as Long)..(it[1] as Long),
                teaseRes = it[2] as Int,
                revealRes = it[3] as Int
            )
        }
    )
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val audioService = remember { WrappedAudioService(view.context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemDark) {
        if (darkTheme == DarkMode.AUTO) isSystemDark else darkTheme == DarkMode.ON
    }
    val textColor = if (useDarkTheme) Color.White else Color(0xFF121212)

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

    WrappedBackground(modifier = Modifier.fillMaxSize(), useDarkTheme = useDarkTheme) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back", tint = textColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = { audioService.toggleMute() }) {
                            val icon = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                            Icon(painterResource(icon), contentDescription = "Mute", tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (screens[page]) {
                        is WrappedScreenType.Welcome -> WrappedIntro(
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        ) { scope.launch { pagerState.animateScrollToPage(page = 1) } }
                        is WrappedScreenType.MinutesTease -> WrappedMinutesTease(
                            messagePair = messagePair,
                            onNavigateForward = { scope.launch { pagerState.animateScrollToPage(page = 2) } },
                            isDataReady = state.isDataReady,
                            textColor = textColor
                        )
                        is WrappedScreenType.MinutesReveal -> WrappedMinutesScreen(
                            messagePair = messagePair, 
                            totalMinutes = state.totalMinutes,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.MinutesReveal),
                            textColor = textColor
                        )
                        is WrappedScreenType.TotalSongs -> WrappedTotalSongsScreen(
                            uniqueSongCount = state.uniqueSongCount,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalSongs),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.TopSongReveal -> WrappedTopSongScreen(
                            topSong = state.topSongs.firstOrNull(),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopSongReveal),
                            textColor = textColor
                        )
                        is WrappedScreenType.Top5Songs -> WrappedTop5SongsScreen(
                            topSongs = state.topSongs.take(5),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Songs),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.TotalAlbums -> WrappedTotalAlbumsScreen(
                            uniqueAlbumCount = state.totalAlbums,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalAlbums),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.TopAlbumReveal -> WrappedTopAlbumScreen(
                            topAlbum = state.topAlbum,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopAlbumReveal),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.Top5Albums -> WrappedTop5AlbumsScreen(
                            topAlbums = state.top5Albums,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Albums),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.TotalArtists -> WrappedTotalArtistsScreen(
                            uniqueArtistCount = state.uniqueArtistCount,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TotalArtists),
                            textColor = textColor
                        )
                        is WrappedScreenType.TopArtistReveal -> WrappedTopArtistScreen(
                            topArtist = state.topArtists.firstOrNull(),
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.TopArtistReveal),
                            textColor = textColor
                        )
                        is WrappedScreenType.Top5Artists -> WrappedTop5ArtistsScreen(
                            topArtists = state.topArtists,
                            isVisible = pagerState.currentPage == screens.indexOf(WrappedScreenType.Top5Artists),
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.Playlist -> PlaylistPage(
                            state = state,
                            onCreatePlaylist = { viewModel.createPlaylist("previewalbum") },
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
                        is WrappedScreenType.Conclusion -> ConclusionPage(
                            onClose = onClose,
                            textColor = textColor,
                            useDarkTheme = useDarkTheme
                        )
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
fun AnimatedBackground(
    elementCount: Int = 20, 
    shapeTypes: List<ShapeType> = listOf(ShapeType.Circle),
    textColor: Color
) {
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
                ShapeType.Circle -> drawCircle(color = textColor.copy(alpha = element.alpha), radius = element.size, center = Offset(currentX * size.width, currentY * size.height))
                ShapeType.Rect -> drawRect(color = textColor.copy(alpha = element.alpha), topLeft = Offset(currentX * size.width, currentY * size.height), size = Size(element.size, element.size))
                ShapeType.Line -> drawLine(color = textColor.copy(alpha = element.alpha), start = Offset(currentX * size.width, currentY * size.height), end = Offset((currentX + 0.1f) * size.width, (currentY + 0.1f) * size.height), strokeWidth = 2f)
            }
        }
    }
}

@Composable
fun WrappedBackground(
    modifier: Modifier = Modifier, 
    useDarkTheme: Boolean,
    content: @Composable BoxScope.() -> Unit = {}
) {
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

    val bgColor = if (useDarkTheme) Color(0xFF0F0620) else Color(0xFFF4F4F9)
    val blob1Colors = remember(useDarkTheme) { 
        if (useDarkTheme) listOf(Color(0xFF7C3AED).copy(alpha = 0.4f), Color.Transparent)
        else listOf(Color(0xFF7C3AED).copy(alpha = 0.15f), Color.Transparent)
    }
    val blob2Colors = remember(useDarkTheme) { 
        if (useDarkTheme) listOf(Color(0xFF06B6D4).copy(alpha = 0.3f), Color.Transparent)
        else listOf(Color(0xFF06B6D4).copy(alpha = 0.12f), Color.Transparent)
    }
    val blob3Colors = remember(useDarkTheme) { 
        if (useDarkTheme) listOf(Color(0xFFDB2777).copy(alpha = 0.2f), Color.Transparent)
        else listOf(Color(0xFFDB2777).copy(alpha = 0.1f), Color.Transparent)
    }
    val dotColor = if (useDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(bgColor)) {
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
                drawPoints(points = dotPoints, pointMode = PointMode.Points, color = dotColor, strokeWidth = 3f, cap = StrokeCap.Round)
            }
        }
        Box(modifier = Modifier.fillMaxSize()) { content() }
    }
}

@Composable
fun AutoResizingText(
    text: String, 
    modifier: Modifier = Modifier, 
    style: TextStyle, 
    maxLines: Int = 1,
    textAlign: TextAlign? = null
) {
    var scaledTextStyle by remember(style) { mutableStateOf(style) }
    var readyToDraw by remember(style) { mutableStateOf(false) }

    Text(
        text = text,
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = scaledTextStyle,
        maxLines = maxLines,
        textAlign = textAlign,
        softWrap = true,
        onTextLayout = { textLayoutResult ->
            var midWordBreak = false
            for (i in 0 until textLayoutResult.lineCount - 1) {
                val lineEnd = textLayoutResult.getLineEnd(i)
                if (lineEnd > 0 && lineEnd < text.length) {
                    val charBefore = text[lineEnd - 1]
                    val charAfter = text[lineEnd]
                    // If a line ends and neither character is whitespace, it forcefully split a word.
                    if (!charBefore.isWhitespace() && !charAfter.isWhitespace() && charBefore != '-') {
                        midWordBreak = true
                        break
                    }
                }
            }

            if ((textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount > maxLines || textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight || midWordBreak) && scaledTextStyle.fontSize.value > 10f) {
                scaledTextStyle = scaledTextStyle.copy(
                    fontSize = scaledTextStyle.fontSize * 0.95,
                    lineHeight = if (scaledTextStyle.lineHeight.isSp) scaledTextStyle.lineHeight * 0.95 else scaledTextStyle.lineHeight
                )
            } else {
                readyToDraw = true
            }
        }
    )
}

@Composable
fun FormattedText(text: String, modifier: Modifier = Modifier, style: TextStyle) {
    var scaledTextStyle by remember(style) { mutableStateOf(style) }
    var readyToDraw by remember(style) { mutableStateOf(false) }

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
        modifier = modifier.drawWithContent { if (readyToDraw) drawContent() },
        style = scaledTextStyle,
        softWrap = true,
        onTextLayout = { textLayoutResult ->
            var midWordBreak = false
            val rawText = annotatedString.text
            for (i in 0 until textLayoutResult.lineCount - 1) {
                val lineEnd = textLayoutResult.getLineEnd(i)
                if (lineEnd > 0 && lineEnd < rawText.length) {
                    val charBefore = rawText[lineEnd - 1]
                    val charAfter = rawText[lineEnd]
                    if (!charBefore.isWhitespace() && !charAfter.isWhitespace() && charBefore != '-') {
                        midWordBreak = true
                        break
                    }
                }
            }
            
            if ((textLayoutResult.hasVisualOverflow || textLayoutResult.didOverflowWidth || textLayoutResult.didOverflowHeight || midWordBreak) && scaledTextStyle.fontSize.value > 10f) {
                scaledTextStyle = scaledTextStyle.copy(
                    fontSize = scaledTextStyle.fontSize * 0.95,
                    lineHeight = if (scaledTextStyle.lineHeight.isSp) scaledTextStyle.lineHeight * 0.95 else scaledTextStyle.lineHeight
                )
            } else {
                readyToDraw = true
            }
        }
    )
}

// ======= WRAPPED PAGES =======

@Composable
fun WrappedIntro(textColor: Color, useDarkTheme: Boolean, onNext: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); visible = true }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    val shadowColor = if (useDarkTheme) Color.Black else Color.Gray
    val titleStyle = TextStyle(
        fontFamily = bbh_bartle,
        textAlign = TextAlign.Center,
        letterSpacing = 1.sp,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        color = textColor,
        shadow = Shadow(
            color = shadowColor,
            offset = Offset(6f, 6f),
            blurRadius = 0f
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        Image(
                            painter = painterResource(R.drawable.avidtune),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(textColor),
                            modifier = Modifier.size(100.dp).clip(CircleShape)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.9f), contentAlignment = Alignment.Center) {
                            AutoResizingText(
                                text = stringResource(R.string.insight_title), 
                                maxLines = 3, 
                                modifier = Modifier.fillMaxWidth(), 
                                style = titleStyle
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                    ) {
                        Text(
                            text = stringResource(R.string.insight_intro_subtitle),
                            color = textColor,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000))
                    ) {
                        Button(
                            onClick = onNext,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = textColor)
                        ) {
                            Text(
                                text = stringResource(R.string.insight_lets_go),
                                color = if (useDarkTheme) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    Image(
                        painter = painterResource(R.drawable.avidtune),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textColor),
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    Box(modifier = Modifier.fillMaxWidth(0.85f), contentAlignment = Alignment.Center) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_title), 
                            maxLines = 3, 
                            modifier = Modifier.fillMaxWidth(), 
                            style = titleStyle
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                ) {
                    Text(
                        text = stringResource(R.string.insight_intro_subtitle),
                        color = textColor,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000)),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp)
            ) {
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = textColor)
                ) {
                    Text(
                        text = stringResource(R.string.insight_lets_go),
                        color = if (useDarkTheme) Color.Black else Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedMinutesTease(messagePair: MessagePair?, onNavigateForward: () -> Unit, isDataReady: Boolean, textColor: Color) {
    LaunchedEffect(Unit) { delay(3500); onNavigateForward() }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.animation.AnimatedVisibility(
            visible = messagePair != null && isDataReady,
            enter = fadeIn(tween(1000)) + scaleIn(initialScale = 0.9f, animationSpec = tween(1000))
        ) {
            AutoResizingText(
                text = messagePair?.teaseRes?.let { stringResource(it) } ?: "",
                modifier = Modifier.fillMaxWidth(if (isLandscape) 0.9f else 0.85f),
                maxLines = 4,
                style = TextStyle(
                    color = textColor,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = bbh_bartle
                )
            )
        }
    }
}

@Composable
fun WrappedMinutesScreen(messagePair: MessagePair?, totalMinutes: Long, isVisible: Boolean, textColor: Color) {
    val animatedMinutes = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isVisible, totalMinutes) {
        if (isVisible && totalMinutes > 0) animatedMinutes.animateTo(targetValue = totalMinutes.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    FormattedText(
                        text = messagePair?.teaseRes?.let { stringResource(it) } ?: "",
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FormattedText(
                        text = messagePair?.revealRes?.let { stringResource(it, totalMinutes) } ?: "",
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    )
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FormattedText(
                    text = messagePair?.teaseRes?.let { stringResource(it) } ?: "",
                    modifier = Modifier.fillMaxWidth(0.85f),
                    style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
                    text = messagePair?.revealRes?.let { stringResource(it, totalMinutes) } ?: "",
                    modifier = Modifier.fillMaxWidth(0.85f),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
fun WrappedTotalSongsScreen(uniqueSongCount: Int, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    val animatedSongs = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isVisible, uniqueSongCount) {
        if (isVisible && uniqueSongCount > 0) animatedSongs.animateTo(targetValue = uniqueSongCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Line), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_total_songs_title),
                        modifier = Modifier.fillMaxWidth(0.9f),
                        maxLines = 3,
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.insight_total_songs_subtitle),
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    )
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AutoResizingText(
                    text = stringResource(R.string.insight_total_songs_title),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    maxLines = 3,
                    style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
                    text = stringResource(R.string.insight_total_songs_subtitle),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
fun WrappedTopSongScreen(topSong: SongWithStats?, isVisible: Boolean, textColor: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageSize = if (isLandscape) 140.dp else 200.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(topSong?.thumbnailUrl).build(),
                            contentDescription = null,
                            modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_song_title),
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp),
                            textAlign = TextAlign.Center,
                            maxLines = 3
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                    ) {
                        Text(
                            text = topSong?.title ?: stringResource(R.string.insight_no_data),
                            style = MaterialTheme.typography.headlineMedium.copy(color = textColor),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000))
                    ) {
                        Text(
                            text = stringResource(R.string.insight_listened_for_minutes, (topSong?.timeListened ?: 0) / 60000),
                            style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_song_title),
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp),
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(topSong?.thumbnailUrl).build(),
                        contentDescription = null,
                        modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                ) {
                    Text(
                        text = topSong?.title ?: stringResource(R.string.insight_no_data),
                        style = MaterialTheme.typography.headlineMedium.copy(color = textColor),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 1000)) + slideInVertically(tween(1000, 1000))
                ) {
                    Text(
                        text = stringResource(R.string.insight_listened_for_minutes, (topSong?.timeListened ?: 0) / 60000),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTop5SongsScreen(topSongs: List<SongWithStats>, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 25, shapeTypes = listOf(ShapeType.Rect), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_5_songs),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = TextStyle(fontSize = 28.sp, fontFamily = bbh_bartle, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1.5f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.Start) {
                    topSongs.forEachIndexed { index, song ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                        ) {
                            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${index + 1}", fontFamily = bbh_bartle, fontSize = 36.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.width(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                AsyncImage(model = song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = song.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_5_songs),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = TextStyle(fontSize = 28.sp, fontFamily = bbh_bartle, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    topSongs.forEachIndexed { index, song ->
                        androidx.compose.animation.AnimatedVisibility(
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
                                    color = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.width(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                AsyncImage(
                                    model = song.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        color = textColor,
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
}

@Composable
fun WrappedTotalAlbumsScreen(uniqueAlbumCount: Int, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    val animatedAlbums = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    var visible by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isVisible, uniqueAlbumCount) {
        if (isVisible) { visible = true; if (uniqueAlbumCount > 0) animatedAlbums.animateTo(targetValue = uniqueAlbumCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing)) }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_total_albums_title),
                            modifier = Modifier.fillMaxWidth(0.9f),
                            maxLines = 3,
                            style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                    ) {
                        Text(
                            text = stringResource(R.string.insight_total_albums_subtitle),
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        )
                    }
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_total_albums_title),
                        modifier = Modifier.fillMaxWidth(0.85f),
                        maxLines = 3,
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                ) {
                    Text(
                        text = stringResource(R.string.insight_total_albums_subtitle),
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTopAlbumScreen(topAlbum: Album?, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) visible = true }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageSize = if (isLandscape) 140.dp else 200.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Rect), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                    ) {
                        AsyncImage(
                            model = topAlbum?.album?.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_album_title),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                    ) {
                        Text(
                            text = topAlbum?.album?.title ?: stringResource(R.string.insight_no_data),
                            fontSize = 24.sp,
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
                    ) {
                        Text(
                            text = stringResource(R.string.insight_listened_for_minutes, (topAlbum?.timeListened ?: 0) / 60000),
                            fontSize = 16.sp,
                            color = textColor.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_album_title),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    AsyncImage(
                        model = topAlbum?.album?.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                ) {
                    Text(
                        text = topAlbum?.album?.title ?: stringResource(R.string.insight_no_data),
                        fontSize = 24.sp,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
                ) {
                    Text(
                        text = stringResource(R.string.insight_listened_for_minutes, (topAlbum?.timeListened ?: 0) / 60000),
                        fontSize = 16.sp,
                        color = textColor.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTop5AlbumsScreen(topAlbums: List<Album>, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_5_albums),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1.5f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.Start) {
                    topAlbums.forEachIndexed { index, album ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                        ) {
                            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${index + 1}", fontFamily = bbh_bartle, fontSize = 36.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.width(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                AsyncImage(model = album.album.thumbnailUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = album.album.title, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                    Text(text = stringResource(R.string.insight_minutes_short, (album.timeListened ?: 0) / 60000), color = textColor.copy(alpha = 0.7f), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_5_albums),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    topAlbums.forEachIndexed { index, album ->
                        androidx.compose.animation.AnimatedVisibility(
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
                                    color = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.width(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                AsyncImage(
                                    model = album.album.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = album.album.title,
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = stringResource(R.string.insight_minutes_short, (album.timeListened ?: 0) / 60000),
                                        color = textColor.copy(alpha = 0.7f),
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
}

@Composable
fun WrappedTotalArtistsScreen(uniqueArtistCount: Int, isVisible: Boolean, textColor: Color) {
    val animatedArtists = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    LaunchedEffect(isVisible, uniqueArtistCount) {
        if (isVisible && uniqueArtistCount > 0) animatedArtists.animateTo(targetValue = uniqueArtistCount.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Line), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_total_artists_title),
                        modifier = Modifier.fillMaxWidth(0.9f),
                        maxLines = 3,
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.insight_total_artists_subtitle),
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                    )
                }
                BoxWithConstraints(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AutoResizingText(
                    text = stringResource(R.string.insight_total_artists_title),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    maxLines = 3,
                    style = MaterialTheme.typography.headlineSmall.copy(color = textColor, textAlign = TextAlign.Center, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = MaterialTheme.typography.displayLarge.copy(color = textColor, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontFamily = bbh_bartle, drawStyle = Stroke(with(density) { 2.dp.toPx() }))
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
                    text = stringResource(R.string.insight_total_artists_subtitle),
                    modifier = Modifier.fillMaxWidth(0.85f),
                    style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                )
            }
        }
    }
}

@Composable
fun WrappedTopArtistScreen(topArtist: Artist?, isVisible: Boolean, textColor: Color) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) visible = true }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageSize = if (isLandscape) 140.dp else 200.dp
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Rect), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(topArtist?.artist?.thumbnailUrl).build(),
                            contentDescription = null,
                            modifier = Modifier.size(imageSize).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_artist_title),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                    ) {
                        Text(
                            text = topArtist?.artist?.name ?: stringResource(R.string.insight_no_data),
                            style = MaterialTheme.typography.headlineMedium.copy(color = textColor),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
                    ) {
                        Text(
                            text = stringResource(R.string.insight_listened_for_minutes, (topArtist?.timeListened ?: 0) / 60000),
                            style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_artist_title),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = MaterialTheme.typography.headlineSmall.copy(color = textColor, fontFamily = bbh_bartle, fontSize = 28.sp, lineHeight = 36.sp),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 400)) + slideInVertically(tween(1000, 400))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(topArtist?.artist?.thumbnailUrl).build(),
                        contentDescription = null,
                        modifier = Modifier.size(imageSize).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 600)) + slideInVertically(tween(1000, 600))
                ) {
                    Text(
                        text = topArtist?.artist?.name ?: stringResource(R.string.insight_no_data),
                        style = MaterialTheme.typography.headlineMedium.copy(color = textColor),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 800)) + slideInVertically(tween(1000, 800))
                ) {
                    Text(
                        text = stringResource(R.string.insight_listened_for_minutes, (topArtist?.timeListened ?: 0) / 60000),
                        style = MaterialTheme.typography.bodyLarge.copy(color = textColor.copy(alpha = 0.8f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTop5ArtistsScreen(topArtists: List<Artist>, isVisible: Boolean, textColor: Color, useDarkTheme: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) { if (isVisible) { delay(200); visible = true } }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 15, shapeTypes = listOf(ShapeType.Line), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                    ) {
                        AutoResizingText(
                            text = stringResource(R.string.insight_top_5_artists),
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(0.9f),
                            style = TextStyle(fontSize = 28.sp, fontFamily = bbh_bartle, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1.5f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.Start) {
                    topArtists.forEachIndexed { index, artist ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(600, 400 + (index * 200))) + slideInVertically(tween(600, 400 + (index * 200)))
                        ) {
                            Row(modifier = Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "${index + 1}", fontFamily = bbh_bartle, fontSize = 36.sp, color = textColor.copy(alpha = 0.8f), modifier = Modifier.width(40.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                AsyncImage(model = artist.artist.thumbnailUrl, contentDescription = null, modifier = Modifier.size(64.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(text = artist.artist.name, color = textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(text = stringResource(R.string.insight_minutes_short, (artist.timeListened ?: 0) / 60000), color = textColor.copy(alpha = 0.7f), fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(1000, 200)) + slideInVertically(tween(1000, 200))
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_top_5_artists),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        style = TextStyle(fontSize = 28.sp, fontFamily = bbh_bartle, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp)
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    topArtists.forEachIndexed { index, artist ->
                        androidx.compose.animation.AnimatedVisibility(
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
                                    color = textColor.copy(alpha = 0.8f),
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
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = stringResource(R.string.insight_minutes_short, (artist.timeListened ?: 0) / 60000),
                                        color = textColor.copy(alpha = 0.7f),
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
}

@Composable
fun PlaylistPage(state: WrappedState, onCreatePlaylist: () -> Unit, textColor: Color, useDarkTheme: Boolean) {
    val playlistCreationState = state.playlistCreationState
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(200); startAnimation = true }
    val contentAlpha by animateFloatAsState(targetValue = if (startAnimation) 1f else 0f, animationSpec = tween(800, 200), label = "alpha")
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val imageSize = if (isLandscape) 160.dp else 256.dp

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(shapeTypes = listOf(ShapeType.Circle), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(32.dp).alpha(contentAlpha), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.previewalbum),
                        contentDescription = null,
                        modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp))
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AutoResizingText(
                        text = stringResource(R.string.insight_playlist_ready),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.insight_playlist_name, WrappedConstants.YEAR),
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { if (playlistCreationState == PlaylistCreationState.Idle) onCreatePlaylist() },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = textColor),
                        modifier = Modifier.height(50.dp)
                    ) {
                        when (playlistCreationState) {
                            is PlaylistCreationState.Idle -> Text(stringResource(R.string.insight_add_to_library), style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold))
                            is PlaylistCreationState.Creating -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (useDarkTheme) Color.Black else Color.White, strokeWidth = 2.dp)
                            is PlaylistCreationState.Success -> Text(stringResource(R.string.insight_saved), style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp).alpha(contentAlpha),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AutoResizingText(
                    text = stringResource(R.string.insight_playlist_ready),
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(0.85f),
                    style = TextStyle(fontFamily = bbh_bartle, fontSize = 28.sp, color = textColor, textAlign = TextAlign.Center, lineHeight = 36.sp),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Image(
                    painter = painterResource(R.drawable.previewalbum),
                    contentDescription = null,
                    modifier = Modifier.size(imageSize).clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.insight_playlist_name, WrappedConstants.YEAR),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textColor),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = { if (playlistCreationState == PlaylistCreationState.Idle) onCreatePlaylist() },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = textColor),
                    modifier = Modifier.height(50.dp)
                ) {
                    when (playlistCreationState) {
                        is PlaylistCreationState.Idle -> Text(stringResource(R.string.insight_add_to_library), style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold))
                        is PlaylistCreationState.Creating -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = if (useDarkTheme) Color.Black else Color.White, strokeWidth = 2.dp)
                        is PlaylistCreationState.Success -> Text(stringResource(R.string.insight_saved), style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

@Composable
fun ConclusionPage(onClose: () -> Unit, textColor: Color, useDarkTheme: Boolean) {
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground(elementCount = 30, shapeTypes = listOf(ShapeType.Circle, ShapeType.Line), textColor = textColor)
        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.avidtune),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(textColor),
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.insight_thank_you),
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.insight_see_you),
                        style = TextStyle(fontSize = 16.sp, color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onClose,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = textColor)
                    ) {
                        Text(
                            text = stringResource(R.string.insight_close),
                            style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.avidtune),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(textColor),
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.insight_thank_you),
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.insight_see_you),
                    style = TextStyle(fontSize = 16.sp, color = Color.Gray),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                Button(
                    onClick = onClose,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = textColor)
                ) {
                    Text(
                        text = stringResource(R.string.insight_close),
                        style = TextStyle(color = if (useDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
