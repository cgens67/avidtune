package com.cgens67.avidtune.playback

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.STATE_READY
import androidx.media3.common.Timeline
import com.cgens67.avidtune.MusicWidget.Companion.ACTION_STATE_CHANGED
import com.cgens67.avidtune.MusicWidget.Companion.ACTION_UPDATE_PROGRESS
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AudioFadingKey
import com.cgens67.avidtune.constants.SeekIncrementKey
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.extensions.currentMetadata
import com.cgens67.avidtune.extensions.getCurrentQueueIndex
import com.cgens67.avidtune.extensions.getQueueWindows
import com.cgens67.avidtune.extensions.metadata
import com.cgens67.avidtune.playback.MusicService.MusicBinder
import com.cgens67.avidtune.playback.queues.Queue
import com.cgens67.avidtune.together.TogetherDisplayNameKey
import com.cgens67.avidtune.together.TogetherRole
import com.cgens67.avidtune.together.TogetherSessionState
import com.cgens67.avidtune.together.TogetherTrack
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.reportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerConnection(
    private val context: Context,
    binder: MusicBinder,
    val database: MusicDatabase,
    private val scope: CoroutineScope,
) : Player.Listener {

    companion object {
        private const val TAG = "PlayerConnection"
        private const val PROGRESS_UPDATE_INTERVAL = 1000L
        private const val WIDGET_UPDATE_DEBOUNCE = 100L

        @Volatile
        var instance: PlayerConnection? = null
            private set
    }

    val service = binder.service
    val player = service.player

    val sponsorBlockEnabled: StateFlow<Boolean> = service.sponsorBlockEnabled.asStateFlow()
    val currentSkipSegments: StateFlow<List<Pair<Long, Long>>> = service.currentSkipSegments.asStateFlow()
    val currentPlaybackClient: StateFlow<String?> = service.currentPlaybackClient.asStateFlow()

    private val _playbackState = MutableStateFlow(player.playbackState)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _playWhenReady = MutableStateFlow(player.playWhenReady)
    val playWhenReady: StateFlow<Boolean> = _playWhenReady.asStateFlow()

    val isPlaying = combine(playbackState, playWhenReady) { playbackState, playWhenReady ->
        playWhenReady && (playbackState == STATE_READY || playbackState == Player.STATE_BUFFERING)
    }.stateIn(
        scope,
        SharingStarted.Lazily,
        player.playWhenReady && player.playbackState == STATE_READY
    )

    private val _isConnected = MutableStateFlow(true)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.CONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _mediaMetadata = MutableStateFlow(player.currentMetadata)
    val mediaMetadata: StateFlow<com.cgens67.avidtune.models.MediaMetadata?> =
        _mediaMetadata.asStateFlow()

    val currentSong = mediaMetadata.flatMapLatest { metadata ->
        database.song(metadata?.id)
    }

    val currentLyrics = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.lyrics(mediaMetadata?.id)
    }

    val currentFormat = mediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

    private val _queueTitle = MutableStateFlow<String?>(service.queueTitle)
    val queueTitle: StateFlow<String?> = _queueTitle.asStateFlow()

    private val _queueWindows = MutableStateFlow<List<Timeline.Window>>(emptyList())
    val queueWindows: StateFlow<List<Timeline.Window>> = _queueWindows.asStateFlow()

    private val _currentMediaItemIndex = MutableStateFlow(-1)
    val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex.asStateFlow()

    private val _currentWindowIndex = MutableStateFlow(-1)
    val currentWindowIndex: StateFlow<Int> = _currentWindowIndex.asStateFlow()

    private val _shuffleModeEnabled = MutableStateFlow(false)
    val shuffleModeEnabled: StateFlow<Boolean> = _shuffleModeEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _canSkipPrevious = MutableStateFlow(true)
    val canSkipPrevious: StateFlow<Boolean> = _canSkipPrevious.asStateFlow()

    private val _canSkipNext = MutableStateFlow(true)
    val canSkipNext: StateFlow<Boolean> = _canSkipNext.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _bufferedPosition = MutableStateFlow(0L)
    val bufferedPosition: StateFlow<Long> = _bufferedPosition.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()

    private val _error = MutableStateFlow<PlaybackException?>(null)
    val error: StateFlow<PlaybackException?> = _error.asStateFlow()

    private val updateScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private var progressUpdateRunnable: Runnable? = null
    private val isUpdatingProgress = AtomicBoolean(false)
    private val widgetUpdateHandler = Handler(Looper.getMainLooper())
    private var pendingWidgetUpdate: Runnable? = null

    private var lastPlaybackState: Int = player.playbackState
    private var lastPlayWhenReady: Boolean = player.playWhenReady
    private var lastMediaItemIndex: Int = player.currentMediaItemIndex
    private var lastPosition: Long = 0L

    var seekIncrementMs: Long = 5000L
        private set

    private var audioFadingEnabled = false
    private var fadeJob: Job? = null

    val isGuestPlaybackRestricted: Boolean
        get() {
            val state = service.togetherSessionState.value
            return if (state is TogetherSessionState.Joined && state.role is TogetherRole.Guest) {
                !state.roomState.settings.allowGuestsToControlPlayback
            } else {
                false
            }
        }

    init {
        Log.d(TAG, "Initializing PlayerConnection")

        player.addListener(this)
        initializeStates()
        startProgressUpdates()

        scope.launch {
            context.dataStore.data.map { it[SeekIncrementKey] ?: 5 }.collect {
                seekIncrementMs = it * 1000L
            }
        }

        scope.launch {
            context.dataStore.data.map { it[AudioFadingKey] ?: false }.collect {
                audioFadingEnabled = it
            }
        }

        scope.launch {
            service.queueTitleFlow.collect { title ->
                _queueTitle.value = title
            }
        }

        scope.launch {
            service.togetherSessionState.collect { state ->
                if (state is TogetherSessionState.Hosting || state is TogetherSessionState.Joined) {
                    service.queueTitle = context.getString(R.string.music_together)
                    _queueTitle.value = service.queueTitle
                }
            }
        }

        instance = this

        player.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                handlePlayerEvents(player, events)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "Playback state changed to: $playbackState")
                updateConnectionState(playbackState)
            }
        })

        Log.d(TAG, "PlayerConnection initialized successfully")
    }

    private fun initializeStates() {
        try {
            _playbackState.value = player.playbackState
            _playWhenReady.value = player.playWhenReady
            _mediaMetadata.value = player.currentMetadata

            val togetherState = service.togetherSessionState.value
            if (togetherState is TogetherSessionState.Hosting || togetherState is TogetherSessionState.Joined) {
                service.queueTitle = context.getString(R.string.music_together)
            }
            _queueTitle.value = service.queueTitle

            _queueWindows.value = player.getQueueWindows()
            _currentWindowIndex.value = player.getCurrentQueueIndex()
            _currentMediaItemIndex.value = player.currentMediaItemIndex
            _shuffleModeEnabled.value = player.shuffleModeEnabled
            _repeatMode.value = player.repeatMode
            _currentPosition.value = player.currentPosition
            _duration.value = player.duration
            _bufferedPosition.value = player.bufferedPosition
            _volume.value = player.volume

            CoroutineScope(Dispatchers.IO).launch {
                updateLikeStatusForCurrentSong()
            }

            updateCanSkipPreviousAndNext()
            Log.d(TAG, "States initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing states", e)
            reportException(e)
        }
    }

    fun refreshLikeStatus() {
        updateScope.launch(Dispatchers.IO) {
            updateLikeStatusForCurrentSong()
        }
    }

    private fun handlePlayerEvents(player: Player, events: Player.Events) {
        var shouldUpdateWidget = false

        if (events.containsAny(
                Player.EVENT_PLAYBACK_STATE_CHANGED,
                Player.EVENT_PLAY_WHEN_READY_CHANGED
            )
        ) {
            shouldUpdateWidget = true
        }

        if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
            shouldUpdateWidget = true
        }

        if (events.contains(Player.EVENT_POSITION_DISCONTINUITY) ||
            events.contains(Player.EVENT_TIMELINE_CHANGED)
        ) {
            shouldUpdateWidget = true
        }

        if (shouldUpdateWidget) {
            scheduleWidgetUpdate()
        }
    }

    private fun updateConnectionState(playbackState: Int) {
        val newConnectionState = when (playbackState) {
            Player.STATE_IDLE -> ConnectionState.IDLE
            Player.STATE_BUFFERING -> ConnectionState.BUFFERING
            Player.STATE_READY -> ConnectionState.CONNECTED
            Player.STATE_ENDED -> ConnectionState.ENDED
            else -> ConnectionState.ERROR
        }

        _connectionState.value = newConnectionState
        _isConnected.value = newConnectionState == ConnectionState.CONNECTED ||
                newConnectionState == ConnectionState.BUFFERING
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()

        progressUpdateRunnable = object : Runnable {
            override fun run() {
                if (isUpdatingProgress.compareAndSet(false, true)) {
                    try {
                        updateProgressStates()

                        if (player.playWhenReady && player.playbackState == STATE_READY) {
                            progressUpdateHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL)
                        } else {
                            progressUpdateHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL * 2)
                        }
                    } finally {
                        isUpdatingProgress.set(false)
                    }
                }
            }
        }

        progressUpdateRunnable?.let {
            progressUpdateHandler.post(it)
        }
    }

    private fun stopProgressUpdates() {
        progressUpdateRunnable?.let { runnable ->
            progressUpdateHandler.removeCallbacks(runnable)
            progressUpdateRunnable = null
        }
    }

    private fun updateProgressStates() {
        try {
            val currentPos = player.currentPosition
            val totalDuration = player.duration
            val buffered = player.bufferedPosition

            if (kotlin.math.abs(currentPos - lastPosition) > 500L ||
                _duration.value != totalDuration
            ) {
                _currentPosition.value = currentPos
                _duration.value = totalDuration
                _bufferedPosition.value = buffered

                lastPosition = currentPos

                sendProgressUpdateBroadcast()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating progress states", e)
        }
    }

    private fun scheduleWidgetUpdate() {
        pendingWidgetUpdate?.let { widgetUpdateHandler.removeCallbacks(it) }

        pendingWidgetUpdate = Runnable {
            sendStateChangedBroadcast()
        }

        widgetUpdateHandler.postDelayed(pendingWidgetUpdate!!, WIDGET_UPDATE_DEBOUNCE)
    }

    fun playQueue(queue: Queue) {
        if (isGuestPlaybackRestricted) {
            handleGuestAttemptedQueue(queue)
            return
        }
        service.playQueue(queue)
    }

    private fun handleGuestAttemptedQueue(queue: Queue) {
        val state = service.togetherSessionState.value as? TogetherSessionState.Joined ?: return
        if (state.roomState.settings.allowGuestsToAddTracks) {
            scope.launch(Dispatchers.IO) {
                val initial = queue.getInitialStatus()
                val targetItem = initial.items.getOrNull(initial.mediaItemIndex) ?: initial.items.firstOrNull()
                val meta = targetItem?.metadata
                if (meta != null) {
                    val track = TogetherTrack(
                        id = meta.id,
                        title = meta.title,
                        artists = meta.artists.map { it.name },
                        durationSec = meta.duration,
                        thumbnailUrl = meta.thumbnailUrl
                    )
                    service.togetherManager.requestTrack(
                        track = track,
                        requesterId = state.selfParticipantId,
                        requesterName = service.dataStore.get(TogetherDisplayNameKey, "Guest")
                    )
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.together_submitted_as_request, meta.title),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.together_playback_controlled_by_host),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.together_playback_controlled_by_host),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun playNext(item: MediaItem) = playNext(listOf(item))

    fun playNext(items: List<MediaItem>) {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Log.d(TAG, "Adding ${items.size} items to play next")
            service.playNext(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding items to play next", e)
            reportException(e)
        }
    }

    fun addToQueue(item: MediaItem) = addToQueue(listOf(item))

    fun addToQueue(items: List<MediaItem>) {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Log.d(TAG, "Adding ${items.size} items to queue")
            service.addToQueue(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding items to queue", e)
            reportException(e)
        }
    }

    fun toggleLike() {
        try {
            Timber.tag(TAG).d("Toggling like for current track. Current state: ${_isLiked.value}")
            service.toggleLike()
            _isLiked.value = !_isLiked.value
            scheduleWidgetUpdate()
            Log.d(TAG, "Like toggled to: ${_isLiked.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling like", e)
            reportException(e)
        }
    }

    fun isCurrentSongLiked(): Boolean {
        return _isLiked.value
    }

    fun seekToNext() {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Log.d(TAG, "Seeking to next track")
            if (audioFadingEnabled && player.isPlaying) {
                fadeJob?.cancel()
                fadeJob = scope.launch {
                    val targetVol = service.playerVolume.value
                    val steps = 10
                    val delayMs = 150L / steps

                    for (i in steps downTo 1) {
                        player.volume = targetVol * (i.toFloat() / steps)
                        delay(delayMs)
                    }

                    if (player.hasNextMediaItem()) {
                        player.seekToNext()
                        player.prepare()
                        player.playWhenReady = true
                    }

                    for (i in 1..steps) {
                        player.volume = targetVol * (i.toFloat() / steps)
                        delay(delayMs)
                    }
                    player.volume = targetVol
                }
            } else {
                if (player.hasNextMediaItem()) {
                    player.seekToNext()
                    player.prepare()
                    player.playWhenReady = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to next", e)
            reportException(e)
        }
    }

    fun seekToPrevious() {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            Log.d(TAG, "Seeking to previous track")
            if (audioFadingEnabled && player.isPlaying) {
                fadeJob?.cancel()
                fadeJob = scope.launch {
                    val targetVol = service.playerVolume.value
                    val steps = 10
                    val delayMs = 150L / steps

                    for (i in steps downTo 1) {
                        player.volume = targetVol * (i.toFloat() / steps)
                        delay(delayMs)
                    }

                    if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                        player.seekToPrevious()
                        player.prepare()
                        player.playWhenReady = true
                    }

                    for (i in 1..steps) {
                        player.volume = targetVol * (i.toFloat() / steps)
                        delay(delayMs)
                    }
                    player.volume = targetVol
                }
            } else {
                if (player.hasPreviousMediaItem() || player.currentPosition > 3000) {
                    player.seekToPrevious()
                    player.prepare()
                    player.playWhenReady = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to previous", e)
            reportException(e)
        }
    }

    fun fastForward() {
        if (isGuestPlaybackRestricted) return
        try {
            player.seekTo((player.currentPosition + seekIncrementMs).coerceAtMost(player.duration))
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun rewind() {
        if (isGuestPlaybackRestricted) return
        try {
            player.seekTo((player.currentPosition - seekIncrementMs).coerceAtLeast(0L))
        } catch (e: Exception) {
            reportException(e)
        }
    }

    fun togglePlayPause() {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val newPlayWhenReady = !player.playWhenReady
            Log.d(TAG, "Toggling play/pause to: $newPlayWhenReady")

            if (audioFadingEnabled) {
                fadeJob?.cancel()
                fadeJob = scope.launch {
                    val targetVol = service.playerVolume.value
                    val steps = 10
                    val delayMs = 100L / steps

                    if (!newPlayWhenReady) {
                        for (i in steps downTo 1) {
                            player.volume = targetVol * (i.toFloat() / steps)
                            delay(delayMs)
                        }
                        player.playWhenReady = false
                        player.volume = targetVol
                    } else {
                        player.volume = 0f
                        if (player.playbackState == Player.STATE_IDLE) {
                            player.prepare()
                        }
                        player.playWhenReady = true
                        for (i in 1..steps) {
                            player.volume = targetVol * (i.toFloat() / steps)
                            delay(delayMs)
                        }
                        player.volume = targetVol
                    }
                }
            } else {
                if (newPlayWhenReady && player.playbackState == Player.STATE_IDLE) {
                    player.prepare()
                }
                player.playWhenReady = newPlayWhenReady
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling play/pause", e)
            reportException(e)
        }
    }

    fun toggleShuffle() {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val newShuffleMode = !player.shuffleModeEnabled
            Log.d(TAG, "Toggling shuffle to: $newShuffleMode")
            player.shuffleModeEnabled = newShuffleMode
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling shuffle", e)
            reportException(e)
        }
    }

    fun toggleReplayMode() {
        if (isGuestPlaybackRestricted) {
            Toast.makeText(context, context.getString(R.string.together_playback_controlled_by_host), Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val newRepeatMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                REPEAT_MODE_OFF
            } else {
                Player.REPEAT_MODE_ONE
            }
            Log.d(TAG, "Toggling repeat mode to: $newRepeatMode")
            player.repeatMode = newRepeatMode
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling repeat mode", e)
            reportException(e)
        }
    }

    fun seekTo(positionMs: Long) {
        if (isGuestPlaybackRestricted) return
        try {
            Log.d(TAG, "Seeking to position: ${positionMs}ms")
            player.seekTo(positionMs.coerceIn(0, player.duration))
        } catch (e: Exception) {
            Log.e(TAG, "Error seeking to position", e)
            reportException(e)
        }
    }

    fun setVolume(volume: Float) {
        try {
            val clampedVolume = volume.coerceIn(0.0f, 1.0f)
            Log.d(TAG, "Setting volume to: $clampedVolume")
            service.playerVolume.value = clampedVolume
            player.volume = clampedVolume
            _volume.value = clampedVolume
            if (clampedVolume > 0f && _isMuted.value) {
                _isMuted.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
            reportException(e)
        }
    }

    fun toggleMute() {
        try {
            val newMuteState = !_isMuted.value
            Log.d(TAG, "Toggling mute to: $newMuteState")

            if (newMuteState) {
                player.volume = 0.0f
            } else {
                val restored = service.playerVolume.value.takeIf { it > 0f } ?: _volume.value.takeIf { it > 0f } ?: 1.0f
                player.volume = restored
            }

            _isMuted.value = newMuteState
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling mute", e)
            reportException(e)
        }
    }

    override fun onPlaybackStateChanged(state: Int) {
        Log.d(TAG, "Playback state changed: $lastPlaybackState -> $state")
        _playbackState.value = state
        _error.value = player.playerError

        if (lastPlaybackState != state) {
            lastPlaybackState = state
            scheduleWidgetUpdate()
        }
    }

    override fun onPlayWhenReadyChanged(newPlayWhenReady: Boolean, reason: Int) {
        Log.d(
            TAG,
            "PlayWhenReady changed: $lastPlayWhenReady -> $newPlayWhenReady (reason: $reason)"
        )
        _playWhenReady.value = newPlayWhenReady

        if (lastPlayWhenReady != newPlayWhenReady) {
            lastPlayWhenReady = newPlayWhenReady
            scheduleWidgetUpdate()
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        Timber.tag(TAG).d("Media item transition: ${mediaItem?.mediaId} (reason: $reason)")
        _mediaMetadata.value = mediaItem?.metadata
        _currentMediaItemIndex.value = player.currentMediaItemIndex
        _currentWindowIndex.value = player.getCurrentQueueIndex()

        CoroutineScope(Dispatchers.IO).launch {
            updateLikeStatusForCurrentSong()
        }

        if (lastMediaItemIndex != player.currentMediaItemIndex) {
            lastMediaItemIndex = player.currentMediaItemIndex
            updateCanSkipPreviousAndNext()
            scheduleWidgetUpdate()
        }
    }

    private suspend fun updateLikeStatusForCurrentSong() {
        try {
            val currentSongId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                player.currentMediaItem?.mediaId
            }
            if (currentSongId != null) {
                val songWithInfo = database.song(currentSongId).first()
                _isLiked.value = songWithInfo?.song?.liked ?: false
            } else {
                _isLiked.value = false
            }
        } catch (e: Exception) {
            _isLiked.value = false
        }
    }

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        _queueWindows.value = player.getQueueWindows()
        val togetherState = service.togetherSessionState.value
        if (togetherState is TogetherSessionState.Hosting || togetherState is TogetherSessionState.Joined) {
            service.queueTitle = context.getString(R.string.music_together)
        }
        _queueTitle.value = service.queueTitle
        _currentMediaItemIndex.value = player.currentMediaItemIndex
        _currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
    }

    override fun onShuffleModeEnabledChanged(enabled: Boolean) {
        _shuffleModeEnabled.value = enabled
        _queueWindows.value = player.getQueueWindows()
        _currentWindowIndex.value = player.getCurrentQueueIndex()
        updateCanSkipPreviousAndNext()
        scheduleWidgetUpdate()
    }

    override fun onRepeatModeChanged(mode: Int) {
        _repeatMode.value = mode
        updateCanSkipPreviousAndNext()
        scheduleWidgetUpdate()
    }

    override fun onPlayerErrorChanged(playbackError: PlaybackException?) {
        if (playbackError != null) {
            reportException(playbackError)
        }
        _error.value = playbackError
        updateConnectionState(Player.STATE_IDLE)
    }

    override fun onVolumeChanged(volume: Float) {
        _volume.value = volume
        _isMuted.value = volume == 0.0f
    }

    private fun updateCanSkipPreviousAndNext() {
        try {
            if (!player.currentTimeline.isEmpty) {
                val window = player.currentTimeline.getWindow(
                    player.currentMediaItemIndex,
                    Timeline.Window()
                )

                val canPrevious = player.isCommandAvailable(COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) ||
                        !window.isLive ||
                        player.isCommandAvailable(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM) ||
                        player.currentPosition > 3000

                val canNext = (window.isLive && window.isDynamic) ||
                        player.isCommandAvailable(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)

                _canSkipPrevious.value = canPrevious
                _canSkipNext.value = canNext
            } else {
                _canSkipPrevious.value = false
                _canSkipNext.value = false
            }
        } catch (e: Exception) {
            _canSkipPrevious.value = false
            _canSkipNext.value = false
        }
    }

    private fun sendStateChangedBroadcast() {
        try {
            val intent = Intent(ACTION_STATE_CHANGED).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending state changed broadcast", e)
        }
    }

    private fun sendProgressUpdateBroadcast() {
        try {
            val intent = Intent(ACTION_UPDATE_PROGRESS).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending progress update broadcast", e)
        }
    }

    fun dispose() {
        stopProgressUpdates()
        pendingWidgetUpdate?.let { widgetUpdateHandler.removeCallbacks(it) }

        try {
            player.removeListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing player listener", e)
        }

        instance = null
    }

    enum class ConnectionState {
        IDLE,
        CONNECTING,
        CONNECTED,
        BUFFERING,
        ENDED,
        ERROR
    }
}