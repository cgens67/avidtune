package com.cgens67.avidtune.ui.screens.insight

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

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
            val mediaItem = MediaItem.Builder()
                .setUri(songUri)
                .setMediaId(songId ?: "fallback")
                .build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
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

    private suspend fun getSongUri(songId: String?): Uri {
        val fallbackUri = "android.resource://${context.packageName}/${R.raw.click}".toUri()
        if (songId == null) return fallbackUri

        return try {
            val audioQuality = context.dataStore.get(AudioQualityKey).let {
                AudioQuality.valueOf(it?.name ?: AudioQuality.AUTO.name)
            }
            val playbackData = withContext(Dispatchers.IO) {
                YTPlayerUtils.playerResponseForPlayback(
                    videoId = songId,
                    audioQuality = audioQuality,
                    connectivityManager = connectivityManager,
                ).getOrNull()
            }
            val streamUrl = playbackData?.streamUrl
            if (streamUrl.isNullOrBlank()) fallbackUri else streamUrl.toUri()
        } catch (e: Exception) {
            fallbackUri
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
                        Icon(painterResource(R.drawable.arrow_back), "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { audioService.toggleMute() }) {
                        val icon = if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                        Icon(painterResource(icon), "Mute", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
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
                        messagePair = messagePair, totalMinutes = state.totalMinutes,
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
