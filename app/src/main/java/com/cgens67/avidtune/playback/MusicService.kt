@file:Suppress("DEPRECATION")
package com.cgens67.avidtune.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.cgens67.avidtune.MainActivity
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AudioNormalizationKey
import com.cgens67.avidtune.constants.AudioQualityKey
import com.cgens67.avidtune.constants.AutoLoadMoreKey
import com.cgens67.avidtune.constants.AutoSkipNextOnErrorKey
import com.cgens67.avidtune.constants.DisableLoadMoreWhenRepeatAllKey
import com.cgens67.avidtune.constants.DiscordTokenKey
import com.cgens67.avidtune.constants.DiscordUseDetailsKey
import com.cgens67.avidtune.constants.EnableDiscordRPCKey
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.HistoryDuration
import com.cgens67.avidtune.constants.MediaSessionConstants.CommandToggleLike
import com.cgens67.avidtune.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.cgens67.avidtune.constants.MediaSessionConstants.CommandToggleShuffle
import com.cgens67.avidtune.constants.PauseListenHistoryKey
import com.cgens67.avidtune.constants.PersistentQueueKey
import com.cgens67.avidtune.constants.PlayerVolumeKey
import com.cgens67.avidtune.constants.RepeatModeKey
import com.cgens67.avidtune.constants.SeekIncrementKey
import com.cgens67.avidtune.constants.ShowLyricsKey
import com.cgens67.avidtune.constants.SimilarContent
import com.cgens67.avidtune.constants.SkipSilenceKey
import com.cgens67.avidtune.constants.SponsorBlockEnabledKey
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.db.entities.Event
import com.cgens67.avidtune.db.entities.FormatEntity
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.db.entities.RelatedSongMap
import com.cgens67.avidtune.di.DownloadCache
import com.cgens67.avidtune.di.PlayerCache
import com.cgens67.avidtune.extensions.SilentHandler
import com.cgens67.avidtune.extensions.collect
import com.cgens67.avidtune.extensions.collectLatest
import com.cgens67.avidtune.extensions.currentMetadata
import com.cgens67.avidtune.extensions.findNextMediaItemById
import com.cgens67.avidtune.extensions.mediaItems
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.extensions.toMediaItem
import com.cgens67.avidtune.extensions.toQueue
import com.cgens67.avidtune.lyrics.LyricsHelper
import com.cgens67.avidtune.models.PersistPlayerState
import com.cgens67.avidtune.models.PersistQueue
import com.cgens67.avidtune.models.toMediaMetadata
import com.cgens67.avidtune.playback.queues.EmptyQueue
import com.cgens67.avidtune.playback.queues.Queue
import com.cgens67.avidtune.playback.queues.YouTubeQueue
import com.cgens67.avidtune.playback.queues.filterExplicit
import com.cgens67.avidtune.together.TogetherManager
import com.cgens67.avidtune.together.TogetherRoomSettings
import com.cgens67.avidtune.utils.CoilBitmapLoader
import com.cgens67.avidtune.utils.DiscordRPC
import com.cgens67.avidtune.utils.NetworkConnectivityObserver
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.enumPreference
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.reportException
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.SongItem
import com.cgens67.innertube.models.WatchEndpoint
import com.cgens67.jossredconnect.JossRedClient
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService(), Player.Listener, PlaybackStatsListener.Callback {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastAudioFocusState = AudioManager.AUDIOFOCUS_NONE
    private var wasPlayingBeforeAudioFocusLoss = false
    private var hasAudioFocus = false
    private var scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    val togetherManager by lazy { TogetherManager(scope, player) }
    val togetherSessionState get() = togetherManager.sessionState

    fun updateTogetherSettings(settings: TogetherRoomSettings) = togetherManager.updateSettings(settings)
    fun kickTogetherParticipant(pid: String) = togetherManager.kickParticipant(pid)
    fun banTogetherParticipant(pid: String) = togetherManager.banParticipant(pid)
    fun approveTogetherParticipant(pid: String, approved: Boolean) = togetherManager.approveParticipant(pid, approved)
    fun startTogetherHost(port: Int, displayName: String, settings: TogetherRoomSettings, avatar: String? = null) = togetherManager.startTogetherHost(port, displayName, settings, avatar)
    fun joinTogether(joinLink: String, displayName: String, avatar: String? = null) = togetherManager.joinTogether(joinLink, displayName, avatar)
    fun leaveTogether() = togetherManager.leaveTogether()

    private lateinit var connectivityManager: ConnectivityManager
    lateinit var connectivityObserver: NetworkConnectivityObserver
    val waitingForNetworkConnection = MutableStateFlow(false)
    private val isNetworkConnected = MutableStateFlow(false)

    private val audioQuality by enumPreference(
        this,
        AudioQualityKey,
        com.cgens67.avidtune.constants.AudioQuality.AUTO
    )

    private var currentQueue: Queue = EmptyQueue
    var queueTitle: String? = null
    val currentMediaMetadata = MutableStateFlow<com.cgens67.avidtune.models.MediaMetadata?>(null)

    private val currentSong = currentMediaMetadata
        .flatMapLatest { mediaMetadata ->
            database.song(mediaMetadata?.id)
        }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentFormat = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))
    val sponsorBlockEnabled = MutableStateFlow(true)
    val currentSkipSegments = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
    lateinit var sleepTimer: SleepTimer

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession
    private var isAudioEffectSessionOpened = false
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var discordRpc: DiscordRPC? = null
    private var lastPlaybackSpeed = 1.0f
    private var discordUpdateJob: Job? = null
    val automixItems = MutableStateFlow<List<MediaItem>>(emptyList())
    private var consecutivePlaybackErr = 0

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        val seekIncrementMs = dataStore.get(SeekIncrementKey, 5) * 1000L

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider(
                this,
                { NOTIFICATION_ID },
                CHANNEL_ID,
                R.string.music_player
            ).apply {
                setSmallIcon(R.drawable.avidtune_monochrome)
            }
        )

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createMediaSourceFactory())
            .setRenderersFactory(createRenderersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false,
            )
            .setSeekBackIncrementMs(seekIncrementMs)
            .setSeekForwardIncrementMs(seekIncrementMs)
            .build()
            .apply {
                addListener(this@MusicService)
                sleepTimer = SleepTimer(scope, this)
                addListener(sleepTimer)
                addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
            }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        setupAudioFocusRequest()

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }

        mediaSession = MediaLibrarySession.Builder(this, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            )
            .setBitmapLoader(CoilBitmapLoader(this, scope))
            .build()

        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            { controllerFuture.get() },
            MoreExecutors.directExecutor()
        )

        connectivityManager = getSystemService()!!
        connectivityObserver = NetworkConnectivityObserver(this)

        scope.launch {
            connectivityObserver.networkStatus.collect { isConnected ->
                isNetworkConnected.value = isConnected
                if (isConnected && waitingForNetworkConnection.value) {
                    waitingForNetworkConnection.value = false
                    if (player.currentMediaItem != null && player.playWhenReady) {
                        player.prepare()
                        player.play()
                    }
                }
            }
        }

        playerVolume.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.debounce(1000).collect(scope) { song ->
            updateNotification()
            if (song != null && player.playWhenReady && player.playbackState == Player.STATE_READY) {
                discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
            } else {
                discordRpc?.closeRPC()
            }
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged(),
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id).first() == null) {
                val lyricsResult = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyricsResult.lyrics,
                        )
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        var lastSkippedSegment: Pair<Long, Long>? = null

        dataStore.data
            .map { it[SponsorBlockEnabledKey] ?: true }
            .distinctUntilChanged()
            .collectLatest(scope) {
                sponsorBlockEnabled.value = it
            }

        currentMediaMetadata.distinctUntilChangedBy { it?.id }.collectLatest(scope) { metadata ->
            currentSkipSegments.value = emptyList()
            lastSkippedSegment = null
            // Check metadata.isVideo so SponsorBlock only skips segments on true Videos, not Songs
            if (metadata != null && sponsorBlockEnabled.value && metadata.isVideo) {
                val segments = withContext(Dispatchers.IO) {
                    com.cgens67.avidtune.models.SponsorBlock.getSkipSegments(metadata.id)
                }
                if (segments != null) {
                    currentSkipSegments.value = segments.filter {
                        it.actionType == "skip"
                    }.map {
                        (it.segment[0] * 1000).toLong() to (it.segment[1] * 1000).toLong()
                    }
                }
            }
        }

        scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(200)
                if (sponsorBlockEnabled.value && player.isPlaying && currentSkipSegments.value.isNotEmpty()) {
                    val currentPos = player.currentPosition
                    val segmentToSkip = currentSkipSegments.value.find { (start, end) ->
                        currentPos in start..(end - 100)
                    }
                    if (segmentToSkip != null && segmentToSkip != lastSkippedSegment) {
                        player.seekTo(segmentToSkip.second)
                        lastSkippedSegment = segmentToSkip
                        android.widget.Toast.makeText(this@MusicService, R.string.skipped_sponsor, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    if (lastSkippedSegment != null && currentPos !in (lastSkippedSegment!!.first - 2000)..(lastSkippedSegment!!.second + 2000)) {
                        lastSkippedSegment = null
                    }
                }
            }
        }

        combine(
            currentFormat,
            dataStore.data.map { it[AudioNormalizationKey] ?: true }.distinctUntilChanged(),
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            setupLoudnessEnhancer()
        }

        dataStore.data
            .map { it[DiscordTokenKey] to (it[EnableDiscordRPCKey] ?: true) }
            .debounce(300)
            .distinctUntilChanged()
            .collect(scope) { (key, enabled) ->
                if (discordRpc?.isRpcRunning() == true) {
                    discordRpc?.closeRPC()
                }
                discordRpc = null
                if (key != null && enabled) {
                    discordRpc = DiscordRPC(this, key)
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        currentSong.value?.let {
                            discordRpc?.updateSong(it, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                        }
                    }
                }
            }

        dataStore.data
            .map { it[DiscordUseDetailsKey] ?: false }
            .debounce(1000)
            .distinctUntilChanged()
            .collect(scope) { useDetails ->
                if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                    currentSong.value?.let { song ->
                        discordUpdateJob?.cancel()
                        discordUpdateJob = scope.launch {
                            delay(1000)
                            discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, useDetails)
                        }
                    }
                }
            }

        if (dataStore.get(PersistentQueueKey, true)) {
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                val restoredQueue = queue.toQueue()
                playQueue(
                    queue = restoredQueue,
                    playWhenReady = false,
                )
            }

            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistQueue
                    }
                }
            }.onSuccess { queue ->
                automixItems.value = queue.items.map {
                    it.toMediaItem()
                }
            }

            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).inputStream().use { fis ->
                    ObjectInputStream(fis).use { oos ->
                        oos.readObject() as PersistPlayerState
                    }
                }
            }.onSuccess { playerState ->
                scope.launch {
                    delay(1000)
                    player.repeatMode = playerState.repeatMode
                    player.shuffleModeEnabled = playerState.shuffleModeEnabled
                    player.volume = playerState.volume
                    if (playerState.currentMediaItemIndex < player.mediaItemCount) {
                        player.seekTo(playerState.currentMediaItemIndex, playerState.currentPosition)
                    }
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(30.seconds)
                if (dataStore.get(PersistentQueueKey, true)) {
                    saveQueueToDisk()
                }
            }
        }

        scope.launch {
            while (isActive) {
                delay(10.seconds)
                if (dataStore.get(PersistentQueueKey, true) && player.isPlaying) {
                    saveQueueToDisk()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAudioFocusRequest() {
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { focusChange ->
                handleAudioFocusChange(focusChange)
            }
            .setAcceptsDelayedFocusGain(true)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = false
                if (player.isPlaying) {
                    player.pause()
                }
                abandonAudioFocus()
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.pause()
                }
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasAudioFocus = false
                wasPlayingBeforeAudioFocusLoss = player.isPlaying
                if (player.isPlaying) {
                    player.volume = (playerVolume.value * 0.2f)
                }
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                hasAudioFocus = true
                if (wasPlayingBeforeAudioFocusLoss) {
                    player.play()
                    wasPlayingBeforeAudioFocusLoss = false
                }
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasAudioFocus = true
                player.volume = playerVolume.value
                lastAudioFocusState = focusChange
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        audioFocusRequest?.let { request ->
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            return hasAudioFocus
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocus() {
        if (hasAudioFocus) {
            audioFocusRequest?.let { request ->
                audioManager.abandonAudioFocusRequest(request)
                hasAudioFocus = false
            }
        }
    }

    fun hasAudioFocusForPlayback(): Boolean {
        return hasAudioFocus
    }

    private fun waitOnNetworkError() {
        waitingForNetworkConnection.value = true
    }

    private fun skipOnError() {
        consecutivePlaybackErr += 2
        val nextWindowIndex = player.nextMediaItemIndex
        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
            player.seekTo(nextWindowIndex, C.TIME_UNSET)
            player.prepare()
            player.play()
            return
        }
        player.pause()
        consecutivePlaybackErr = 0
    }

    private fun stopOnError() {
        player.pause()
    }

    private fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            if (currentSong.value?.song?.liked == true) {
                                R.string.action_remove_like
                            } else {
                                R.string.action_like
                            }
                        )
                    )
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    )
                    .setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        }
                    )
                    .setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (player.shuffleModeEnabled) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (player.shuffleModeEnabled) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build()
            )
        )
    }

    private suspend fun recoverSong(mediaId: String, playbackData: YTPlayerUtils.PlaybackData? = null) {
        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return

        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playbackData?.videoDetails ?: YTPlayerUtils.playerResponseForMetadata(mediaId).getOrNull()?.videoDetails)?.lengthSeconds?.toInt()
            ?: -1

        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint = YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map { RelatedSongMap(songId = mediaId, relatedSongId = it.id) }
                    .forEach(::insert)
            }
        }
    }

    fun playQueue(queue: Queue, playWhenReady: Boolean = true) {
        if (!scope.isActive) scope = CoroutineScope(Dispatchers.Main) + Job()
        currentQueue = queue
        queueTitle = null
        player.shuffleModeEnabled = false

        if (queue.preloadItem != null) {
            player.setMediaItem(queue.preloadItem!!.toMediaItem())
            player.prepare()
            player.playWhenReady = playWhenReady
        }

        scope.launch(SilentHandler) {
            val initialStatus = withContext(Dispatchers.IO) {
                queue.getInitialStatus().filterExplicit(dataStore.get(HideExplicitKey, false))
            }
            if (queue.preloadItem != null && player.playbackState == STATE_IDLE) return@launch
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            if (initialStatus.items.isEmpty()) return@launch

            if (queue.preloadItem != null) {
                player.addMediaItems(0, initialStatus.items.subList(0, initialStatus.mediaItemIndex))
                player.addMediaItems(initialStatus.items.subList(initialStatus.mediaItemIndex + 1, initialStatus.items.size))
            } else {
                player.setMediaItems(
                    initialStatus.items,
                    if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0,
                    initialStatus.position,
                )
                player.prepare()
                player.playWhenReady = playWhenReady
            }
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) {
            player.removeMediaItems(0, player.currentMediaItemIndex)
        }
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) {
            player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        }

        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(endpoint = WatchEndpoint(videoId = currentMediaMetadata.id))
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            player.addMediaItems(initialStatus.items.drop(1))
            currentQueue = radioQueue
        }
    }

    fun getAutomixAlbum(albumId: String) {
        scope.launch(SilentHandler) {
            YouTube.album(albumId).onSuccess {
                getAutomix(it.album.playlistId)
            }
        }
    }

    fun getAutomix(playlistId: String) {
        if (dataStore[SimilarContent] == true && !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)) {
            scope.launch(SilentHandler) {
                YouTube.next(WatchEndpoint(playlistId = playlistId)).onSuccess {
                    YouTube.next(WatchEndpoint(playlistId = it.endpoint.playlistId)).onSuccess { result ->
                        automixItems.value = result.items.map { song -> song.toMediaItem() }
                    }
                }
            }
        }
    }

    fun addToQueueAutomix(item: MediaItem, position: Int) {
        automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }
        addToQueue(listOf(item))
    }

    fun playNextAutomix(item: MediaItem, position: Int) {
        automixItems.value = automixItems.value.toMutableList().apply { removeAt(position) }
        playNext(listOf(item))
    }

    fun clearAutomix() {
        automixItems.value = emptyList()
    }

    fun playNext(items: List<MediaItem>) {
        if (player.mediaItemCount == 0 || player.playbackState == STATE_IDLE) {
            player.setMediaItems(items)
            player.prepare()
            player.play()
            return
        }
        
        val insertIndex = player.currentMediaItemIndex + 1
        val shuffleEnabled = player.shuffleModeEnabled
        player.addMediaItems(insertIndex, items)
        player.prepare()

        if (shuffleEnabled) {
            val timeline = player.currentTimeline
            if (!timeline.isEmpty) {
                val size = timeline.windowCount
                val currentIndex = player.currentMediaItemIndex
                val newIndices = (insertIndex until (insertIndex + items.size)).toSet()
                val orderAfter = mutableListOf<Int>()
                var idx = currentIndex
                while (true) {
                    idx = timeline.getNextWindowIndex(idx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (idx == C.INDEX_UNSET) break
                    if (idx != currentIndex) orderAfter.add(idx)
                }
                
                val prevList = mutableListOf<Int>()
                var pIdx = currentIndex
                while (true) {
                    pIdx = timeline.getPreviousWindowIndex(pIdx, Player.REPEAT_MODE_OFF, /*shuffleModeEnabled=*/true)
                    if (pIdx == C.INDEX_UNSET) break
                    if (pIdx != currentIndex) prevList.add(pIdx)
                }
                prevList.reverse()
                
                val existingOrder = (prevList + orderAfter).filter { it != currentIndex && it !in newIndices }
                val nextBlock = (insertIndex until (insertIndex + items.size)).toList()
                val finalOrder = IntArray(size)
                var pos = 0
                finalOrder[pos++] = currentIndex
                
                nextBlock.forEach { if (it in 0 until size) finalOrder[pos++] = it }
                existingOrder.forEach { if (pos < size) finalOrder[pos++] = it }
                
                if (pos < size) {
                    for (i in 0 until size) {
                        if (!finalOrder.contains(i)) {
                            finalOrder[pos++] = i
                            if (pos == size) break
                        }
                    }
                }
                player.setShuffleOrder(DefaultShuffleOrder(finalOrder, System.currentTimeMillis()))
            }
        }
    }

    fun addToQueue(items: List<MediaItem>) {
        player.addMediaItems(items)
        player.prepare()
    }

    private fun toggleLibrary() {
        database.transaction {
            val metadata = currentMediaMetadata.value ?: return@transaction
            val song = getSongById(metadata.id)
            if (song != null) {
                update(song.song.toggleLibrary())
            } else {
                insert(metadata) { it.toggleLibrary() }
            }
        }
    }

    fun toggleLike() {
        database.transaction {
            val metadata = currentMediaMetadata.value ?: return@transaction
            val song = getSongById(metadata.id)
            if (song != null) {
                update(song.song.toggleLike())
            } else {
                insert(metadata) { it.toggleLike() }
            }
        }
    }

    private fun setupLoudnessEnhancer() {
        val audioSessionId = player.audioSessionId
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) {
            Log.w(TAG, "setupLoudnessEnhancer: invalid audioSessionId ($audioSessionId), cannot create effect yet")
            return
        }
        
        if (loudnessEnhancer == null) {
            try {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
                Log.d(TAG, "LoudnessEnhancer created for sessionId=$audioSessionId")
            } catch (e: Exception) {
                reportException(e)
                loudnessEnhancer = null
                return
            }
        }
        
        scope.launch {
            try {
                val currentMediaId = withContext(Dispatchers.Main) {
                    player.currentMediaItem?.mediaId
                }
                val normalizeAudio = withContext(Dispatchers.IO) {
                    dataStore.data.map { it[AudioNormalizationKey] ?: true }.first()
                }
                
                if (normalizeAudio && currentMediaId != null) {
                    val format = withContext(Dispatchers.IO) {
                        database.format(currentMediaId).first()
                    }
                    val loudnessDb = format?.loudnessDb
                    withContext(Dispatchers.Main) {
                        if (loudnessDb != null) {
                            val targetGain = (-loudnessDb * 100).toInt()
                            val clampedGain = targetGain.coerceIn(MIN_GAIN_MB, MAX_GAIN_MB)
                            try {
                                loudnessEnhancer?.setTargetGain(clampedGain)
                                loudnessEnhancer?.enabled = true
                                Log.d(TAG, "LoudnessEnhancer gain applied: ${clampedGain} mB")
                            } catch (e: Exception) {
                                reportException(e)
                                releaseLoudnessEnhancer()
                            }
                        } else {
                            loudnessEnhancer?.enabled = false
                            Log.w(TAG, "setupLoudnessEnhancer: loudnessDb is null, enhancer disabled")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loudnessEnhancer?.enabled = false
                        Log.d(TAG, "setupLoudnessEnhancer: normalization disabled or mediaId unavailable")
                    }
                }
            } catch (e: Exception) {
                reportException(e)
                releaseLoudnessEnhancer()
            }
        }
    }

    private fun releaseLoudnessEnhancer() {
        try {
            loudnessEnhancer?.release()
            Log.d(TAG, "LoudnessEnhancer released")
        } catch (e: Exception) {
            reportException(e)
            Log.e(TAG, "Error releasing LoudnessEnhancer: ${e.message}")
        } finally {
            loudnessEnhancer = null
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        setupLoudnessEnhancer()
        com.cgens67.avidtune.playback.AudioEffectManager.init(player.audioSessionId, this)
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        releaseLoudnessEnhancer()
        com.cgens67.avidtune.playback.AudioEffectManager.release()
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        lastPlaybackSpeed = -1.0f
        setupLoudnessEnhancer()
        discordUpdateJob?.cancel()
        consecutivePlaybackErr = 0
        
        if (dataStore.get(AutoLoadMoreKey, true) &&
            reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT &&
            player.mediaItemCount - player.currentMediaItemIndex <= 5 &&
            currentQueue.hasNextPage() &&
            !(dataStore.get(DisableLoadMoreWhenRepeatAllKey, false) && player.repeatMode == REPEAT_MODE_ALL)
        ) {
            scope.launch(SilentHandler) {
                val mediaItems = currentQueue.nextPage().filterExplicit(dataStore.get(HideExplicitKey, false))
                if (player.playbackState != STATE_IDLE) {
                    player.addMediaItems(mediaItems.drop(1))
                }
            }
        }
        
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (dataStore.get(PersistentQueueKey, true) && playbackState != Player.STATE_BUFFERING) {
            scope.launch {
                delay(500)
                saveQueueToDisk()
            }
        }
        if (playbackState == Player.STATE_ENDED) {
            scope.launch {
                delay(1000)
                if (!player.isPlaying && player.mediaItemCount == 0) {
                    currentMediaMetadata.value = null
                }
            }
        }
    }

    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (playWhenReady) {
            setupLoudnessEnhancer()
        }
        scope.launch {
            delay(300)
            updateNotification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                val focusGranted = requestAudioFocus()
                if (focusGranted) {
                    openAudioEffectSession()
                }
            } else {
                closeAudioEffectSession()
                if (!player.playWhenReady || player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
                    abandonAudioFocus()
                }
            }
        }
        
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
            scope.launch {
                delay(200)
                updateNotification()
            }
        }
        
        if (events.containsAny(Player.EVENT_IS_PLAYING_CHANGED)) {
            if (player.isPlaying) {
                currentSong.value?.let { song ->
                    scope.launch {
                        discordRpc?.updateSong(song, player.currentPosition, player.playbackParameters.speed, dataStore.get(DiscordUseDetailsKey, false))
                    }
                }
            } else if (!events.containsAny(Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                scope.launch {
                    discordRpc?.stopActivity()
                }
            }
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        updateNotification()
        if (shuffleModeEnabled) {
            if (player.mediaItemCount == 0) return
            val shuffledIndices = IntArray(player.mediaItemCount) { it }
            shuffledIndices.shuffle()
            shuffledIndices[shuffledIndices.indexOf(player.currentMediaItemIndex)] = shuffledIndices[0]
            shuffledIndices[0] = player.currentMediaItemIndex
            player.setShuffleOrder(DefaultShuffleOrder(shuffledIndices, System.currentTimeMillis()))
        }
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
        if (dataStore.get(PersistentQueueKey, true)) {
            scope.launch {
                delay(300)
                saveQueueToDisk()
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log.e(TAG, "Player error: ${error.errorCodeName}, message: ${error.message}", error)
        val isConnectionError = (error.cause?.cause is PlaybackException) &&
            (error.cause?.cause as PlaybackException).errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
        if (!isNetworkConnected.value || isConnectionError) {
            waitOnNetworkError()
            return
        }
        if (dataStore.get(AutoSkipNextOnErrorKey, false)) {
            skipOnError()
        } else {
            stopOnError()
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                CacheDataSource.Factory()
                    .setCache(playerCache)
                    .setUpstreamDataSourceFactory(
                        DefaultDataSource.Factory(
                            this,
                            OkHttpDataSource.Factory(
                                OkHttpClient.Builder()
                                    .proxy(YouTube.proxy)
                                    .build(),
                            ),
                        ),
                    ),
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            if (downloadCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1) ||
                playerCache.isCached(mediaId, dataSpec.position, CHUNK_LENGTH)
            ) {
                scope.launch(Dispatchers.IO) {
                    recoverSong(mediaId)
                }
                return@Factory dataSpec
            }
            
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) {
                    recoverSong(mediaId)
                }
                return@Factory dataSpec.withUri(it.first.toUri())
            }
            
            val ytLogTag = "YouTube"
            try {
                val playbackData = runBlocking(Dispatchers.IO) {
                    YTPlayerUtils.playerResponseForPlayback(
                        mediaId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    )
                }.getOrElse { throwable ->
                    when (throwable) {
                        is PlaybackException -> throw throwable
                        is ConnectException, is UnknownHostException -> {
                            throw PlaybackException(
                                getString(R.string.error_no_internet),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED
                            )
                        }
                        is SocketTimeoutException -> {
                            throw PlaybackException(
                                getString(R.string.error_timeout),
                                throwable,
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                            )
                        }
                        else -> throw PlaybackException(
                            getString(R.string.error_unknown),
                            throwable,
                            PlaybackException.ERROR_CODE_REMOTE_ERROR
                        )
                    }
                }
                
                val format = playbackData.format
                database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = format.itag,
                            mimeType = format.mimeType.split(";")[0],
                            codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                            bitrate = format.bitrate,
                            sampleRate = format.audioSampleRate,
                            contentLength = format.contentLength!!,
                            loudnessDb = playbackData.audioConfig?.loudnessDb,
                            playbackUrl = playbackData.streamUrl
                        )
                    )
                }
                
                scope.launch(Dispatchers.IO) {
                    recoverSong(mediaId, playbackData)
                }
                
                val streamUrl = playbackData.streamUrl
                songUrlCache[mediaId] = streamUrl to System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                return@Factory dataSpec.withUri(streamUrl.toUri())
            } catch (e: Exception) {
                Timber.tag(ytLogTag).e(e, "YouTube playback error, trying JossRed as fallback")
                val useAlternativeSource = runBlocking {
                    dataStore.data.map { preferences ->
                        val JossRedMultimedia = booleanPreferencesKey("JossRedMultimedia")
                        preferences[JossRedMultimedia] ?: false
                    }.first()
                }
                
                if (!useAlternativeSource) {
                    throw e
                }
                
                val JRlogTag = "JossRed"
                try {
                    val alternativeUrl = runCatching {
                        runBlocking(Dispatchers.IO) {
                            withTimeout(5000) {
                                JossRedClient.getStreamingUrl(mediaId)
                            }
                        }
                    }.getOrNull()
                    
                    if (alternativeUrl != null) {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(2, TimeUnit.SECONDS)
                            .readTimeout(2, TimeUnit.SECONDS)
                            .build()
                        val request = Request.Builder()
                            .url(alternativeUrl)
                            .head()
                            .build()
                            
                        try {
                            val response = client.newCall(request).execute()
                            if (response.isSuccessful) {
                                Timber.tag(JRlogTag).i("Using JossRed URL as fallback: $alternativeUrl")
                                scope.launch(Dispatchers.IO) {
                                    recoverSong(mediaId)
                                }
                                return@Factory dataSpec.withUri(alternativeUrl.toUri())
                            } else {
                                Timber.tag(JRlogTag).w("JossRed URL unreachable (HTTP ${response.code}), throwing original error")
                                throw e
                            }
                        } catch (jrException: Exception) {
                            Timber.tag(JRlogTag).e(jrException, "Error verifying JossRed URL, throwing original error")
                            throw e
                        }
                    } else {
                        throw e
                    }
                } catch (jrException: Exception) {
                    when (jrException) {
                        is JossRedClient.JossRedException -> {
                            Timber.tag(JRlogTag).w("JossRed error: ${jrException.message}, throwing original error")
                        }
                        is TimeoutCancellationException -> {
                            Timber.tag(JRlogTag).w("JossRed timeout, throwing original error")
                        }
                        else -> {
                            Timber.tag(JRlogTag).e(jrException, "JossRed error, throwing original error")
                        }
                    }
                    throw e
                }
            }
        }
    }

    private fun createMediaSourceFactory() =
        DefaultMediaSourceFactory(
            createDataSourceFactory(),
            DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        )

    private fun createRenderersFactory() =
        object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ) = DefaultAudioSink.Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(2_000_000, 20_000, 256),
                        SonicAudioProcessor(),
                    ),
                ).build()
        }

    override fun onPlaybackStatsReady(eventTime: AnalyticsListener.EventTime, playbackStats: PlaybackStats) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        if (playbackStats.totalPlayTimeMs >= (dataStore[HistoryDuration]?.times(1000f) ?: 30000f) &&
            !dataStore.get(PauseListenHistoryKey, false)
        ) {
            database.query {
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs,
                        )
                    )
                } catch (_: SQLException) {
                }
            }
            val PauseRemoteListenHistoryKey = booleanPreferencesKey("pauseRemoteListenHistory")
            if (!dataStore.get(PauseRemoteListenHistoryKey, false)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val playbackUrl = database.format(mediaItem.mediaId).first()?.playbackUrl
                        ?: YTPlayerUtils.playerResponseForMetadata(mediaItem.mediaId, null)
                            .getOrNull()?.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    playbackUrl?.let {
                        YouTube.registerPlayback(null, playbackUrl).onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    private fun saveQueueToDisk() {
        if (player.playbackState == STATE_IDLE && player.mediaItemCount == 0) {
            filesDir.resolve(PERSISTENT_AUTOMIX_FILE).delete()
            filesDir.resolve(PERSISTENT_QUEUE_FILE).delete()
            filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).delete()
            return
        }
        try {
            val persistQueue = PersistQueue(
                title = queueTitle,
                items = player.mediaItems.mapNotNull { it.metadata },
                mediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                position = if (player.currentPosition >= 0) player.currentPosition else 0,
            )
            val persistAutomix = PersistQueue(
                title = "automix",
                items = automixItems.value.mapNotNull { it.metadata },
                mediaItemIndex = 0,
                position = 0,
            )
            val playerState = PersistPlayerState(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
                volume = player.volume,
                currentMediaItemIndex = player.currentMediaItemIndex.coerceAtLeast(0),
                currentPosition = if (player.currentPosition >= 0) player.currentPosition else 0,
                playWhenReady = player.playWhenReady,
                playbackState = player.playbackState
            )
            
            runCatching {
                filesDir.resolve(PERSISTENT_QUEUE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistQueue)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving queue to disk", it)
                reportException(it)
            }
            
            runCatching {
                filesDir.resolve(PERSISTENT_AUTOMIX_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(persistAutomix)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving automix to disk", it)
                reportException(it)
            }
            
            runCatching {
                filesDir.resolve(PERSISTENT_PLAYER_STATE_FILE).outputStream().use { fos ->
                    ObjectOutputStream(fos).use { oos ->
                        oos.writeObject(playerState)
                    }
                }
            }.onFailure {
                Log.e(TAG, "Error saving player state to disk", it)
                reportException(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveQueueToDisk", e)
            reportException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
        }
        if (discordRpc?.isRpcRunning() == true) {
            discordRpc?.closeRPC()
        }
        discordRpc = null
        abandonAudioFocus()
        releaseLoudnessEnhancer()
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession
    
    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"
        const val CHANNEL_ID = "music_channel_01"
        const val NOTIFICATION_ID = 888
        const val PERSISTENT_PLAYER_STATE_FILE = "persistent_player_state.data"
        const val MAX_CONSECUTIVE_ERR = 5
        const val CHUNK_LENGTH = 512 * 1024L
        const val PERSISTENT_QUEUE_FILE = "persistent_queue.data"
        const val PERSISTENT_AUTOMIX_FILE = "persistent_automix.data"
        private const val MAX_GAIN_MB = 800
        private const val MIN_GAIN_MB = -800
        private const val TAG = "MusicService"
    }
}
