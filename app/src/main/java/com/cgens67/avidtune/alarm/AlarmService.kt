package com.cgens67.avidtune.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes as AndroidAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.extractor.DefaultExtractorsFactory
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.AudioQualityKey
import com.cgens67.avidtune.db.MusicDatabase
import com.cgens67.avidtune.di.DownloadCache
import com.cgens67.avidtune.di.PlayerCache
import com.cgens67.avidtune.extensions.toEnum
import com.cgens67.avidtune.playback.AlternativeStreamClient
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.avidtune.utils.isInternetAvailable
import com.cgens67.innertube.YouTube
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class AlarmPlaybackStatus {
    LOADING_SONG,
    PLAYING_SONG,
    PLAYING_FALLBACK,
}

data class ActiveAlarmInfo(
    val alarm: AlarmState,
    val status: AlarmPlaybackStatus,
)

@AndroidEntryPoint
class AlarmService : Service() {

    @Inject
    lateinit var database: MusicDatabase

    @Inject
    @PlayerCache
    lateinit var playerCache: SimpleCache

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    private val binder = AlarmBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val songUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()

    private var wakeLock: PowerManager.WakeLock? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var connectivityManager: ConnectivityManager? = null

    private var exoPlayer: ExoPlayer? = null
    private var fallbackMediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private var currentAlarm: AlarmState? = null
    private var watchdogJob: Job? = null
    private var fadeInJob: Job? = null
    private var autoTimeoutJob: Job? = null

    inner class AlarmBinder : Binder() {
        val service: AlarmService get() = this@AlarmService
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        connectivityManager = getSystemService<ConnectivityManager>()
        ensureNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                handleStopAction()
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                handleSnoozeAction()
                return START_NOT_STICKY
            }

            ACTION_START_ALARM -> {
                val alarmId = intent.getStringExtra(AlarmManagerHelper.EXTRA_ALARM_ID)
                val alarm = alarmId?.let { AlarmManagerHelper.getAlarm(this, it) }
                if (alarm == null) {
                    startForeground(NOTIFICATION_ID, buildFallbackEmptyNotification())
                    stopSelf()
                    return START_NOT_STICKY
                }
                startRingingAlarm(alarm)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRingingAlarm(alarm: AlarmState) {
        stopAudioAndTimersOnly()
        currentAlarm = alarm

        acquireWakeLock()
        requestAlarmAudioFocus()

        val initialStatus = if (alarm.songId.isBlank()) {
            AlarmPlaybackStatus.PLAYING_FALLBACK
        } else {
            AlarmPlaybackStatus.LOADING_SONG
        }
        _activeAlarmState.value = ActiveAlarmInfo(alarm, initialStatus)

        // Must enter foreground immediately when started via startForegroundService.
        startForeground(NOTIFICATION_ID, buildAlarmNotification(alarm))

        if (alarm.vibrate) {
            startVibration()
        }

        // Safety auto-stop after 15 minutes so an unattended alarm cannot run indefinitely.
        autoTimeoutJob = serviceScope.launch {
            delay(15 * 60_000L)
            handleStopAction()
        }

        if (alarm.songId.isBlank()) {
            startFallbackRingtone(alarm)
            return
        }

        serviceScope.launch {
            val isCached = withContext(Dispatchers.IO) {
                isMediaCachedOrDownloaded(alarm.songId)
            }
            val hasNetwork = isInternetAvailable(this@AlarmService)

            if (!isCached && !hasNetwork) {
                Timber.w("No internet and track ${alarm.songId} is not cached; playing fallback alarm sound")
                startFallbackRingtone(alarm)
                return@launch
            }

            if (!isCached) {
                // Give ExoPlayer 2.5 seconds to resolve and buffer the stream cleanly;
                // if it takes longer, start fallback immediately so the user isn't left in silence,
                // then swap seamlessly once ExoPlayer reaches STATE_READY.
                watchdogJob = serviceScope.launch {
                    delay(2500L)
                    if (_activeAlarmState.value?.status != AlarmPlaybackStatus.PLAYING_SONG) {
                        Timber.w("Stream buffering took >2.5s; starting fallback until stream is ready")
                        startFallbackRingtone(alarm)
                    }
                }
            }

            prepareAndPlaySong(alarm)
        }
    }

    private fun prepareAndPlaySong(alarm: AlarmState) {
        releaseExoPlayer()

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(createAlarmMediaSourceFactory())
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ALARM)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                false // Audio focus managed manually with USAGE_ALARM
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()

        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = if (alarm.fadeInSeconds > 0) 0.05f else targetAlarmVolume(alarm)

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && player.playWhenReady) {
                    watchdogJob?.cancel()
                    stopFallbackRingtone()
                    _activeAlarmState.value = ActiveAlarmInfo(alarm, AlarmPlaybackStatus.PLAYING_SONG)
                    applyFadeIn(
                        targetVolume = targetAlarmVolume(alarm),
                        fadeSeconds = alarm.fadeInSeconds,
                        setVolume = { vol -> player.volume = vol }
                    )
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Timber.e(error, "Alarm ExoPlayer error for songId=${alarm.songId}; falling back to default alarm tone")
                watchdogJob?.cancel()
                releaseExoPlayer()
                startFallbackRingtone(alarm)
            }
        })

        exoPlayer = player

        val mediaItem = MediaItem.Builder()
            .setMediaId(alarm.songId)
            .setUri(alarm.songId)
            .setCustomCacheKey(alarm.songId)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    private fun isMediaCachedOrDownloaded(mediaId: String): Boolean {
        val downloadLen = ContentMetadata.getContentLength(downloadCache.getContentMetadata(mediaId))
        if (downloadLen > 0L && downloadCache.isCached(mediaId, 0L, downloadLen)) return true

        val playerLen = ContentMetadata.getContentLength(playerCache.getContentMetadata(mediaId))
        if (playerLen > 0L && playerCache.isCached(mediaId, 0L, playerLen)) return true

        return false
    }

    private fun createAlarmMediaSourceFactory(): DefaultMediaSourceFactory {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        val upstreamFactory = DefaultDataSource.Factory(
            this,
            OkHttpDataSource.Factory(
                OkHttpClient.Builder()
                    .proxy(YouTube.proxy)
                    .retryOnConnectionFailure(true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            ).setUserAgent(userAgent)
        )

        val playerCacheFactory = CacheDataSource.Factory()
            .setCache(playerCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val combinedCacheFactory = CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(playerCacheFactory)
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        val resolvingFactory = ResolvingDataSource.Factory(combinedCacheFactory) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            if (dataSpec.uri.scheme == "http" || dataSpec.uri.scheme == "https") {
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            if (isMediaCachedOrDownloaded(mediaId)) {
                return@Factory dataSpec
            }

            val audioQuality = dataStore[AudioQualityKey].toEnum(AudioQuality.AUTO)
            val playbackData = runCatching {
                runBlocking(Dispatchers.IO) {
                    YTPlayerUtils.playerResponseForPlayback(
                        videoId = mediaId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                    ).getOrNull()
                }
            }.getOrNull()

            if (playbackData != null) {
                val expiresAt = System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L)
                songUrlCache[mediaId] = playbackData.streamUrl to expiresAt
                return@Factory dataSpec.withUri(playbackData.streamUrl.toUri())
            }

            val altData = runCatching {
                runBlocking(Dispatchers.IO) {
                    AlternativeStreamClient.getStreamData(mediaId)
                }
            }.getOrNull() ?: throw IllegalStateException("Unable to resolve alarm stream for $mediaId")

            dataSpec.withUri(altData.streamUrl.toUri())
        }

        return DefaultMediaSourceFactory(
            resolvingFactory,
            DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
        ).setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(2))
    }

    private fun startFallbackRingtone(alarm: AlarmState) {
        if (fallbackMediaPlayer != null) return
        _activeAlarmState.value = ActiveAlarmInfo(alarm, AlarmPlaybackStatus.PLAYING_FALLBACK)

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        try {
            val mp = MediaPlayer().apply {
                setDataSource(this@AlarmService, uri)
                setAudioAttributes(
                    AndroidAudioAttributes.Builder()
                        .setUsage(AndroidAudioAttributes.USAGE_ALARM)
                        .setContentType(AndroidAudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                val initialVol = if (alarm.fadeInSeconds > 0) 0.05f else targetAlarmVolume(alarm)
                setVolume(initialVol, initialVol)
                setOnPreparedListener { preparedPlayer ->
                    preparedPlayer.start()
                    applyFadeIn(
                        targetVolume = targetAlarmVolume(alarm),
                        fadeSeconds = alarm.fadeInSeconds,
                        setVolume = { vol ->
                            runCatching { preparedPlayer.setVolume(vol, vol) }
                        }
                    )
                }
                setOnErrorListener { _, _, _ ->
                    true
                }
                prepareAsync()
            }
            fallbackMediaPlayer = mp
        } catch (e: Exception) {
            Timber.e(e, "Failed to start fallback ringtone")
        }
    }

    private fun stopFallbackRingtone() {
        try {
            fallbackMediaPlayer?.let { mp ->
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        } catch (e: Exception) {
            Timber.w(e, "Error stopping fallback MediaPlayer")
        } finally {
            fallbackMediaPlayer = null
        }
    }

    private fun applyFadeIn(
        targetVolume: Float,
        fadeSeconds: Int,
        setVolume: (Float) -> Unit,
    ) {
        fadeInJob?.cancel()
        if (fadeSeconds <= 0) {
            setVolume(targetVolume)
            return
        }
        fadeInJob = serviceScope.launch {
            val steps = (fadeSeconds * 10).coerceAtLeast(10)
            val stepDelay = (fadeSeconds * 1000L) / steps
            for (i in 1..steps) {
                if (!isActive) break
                val fraction = i.toFloat() / steps.toFloat()
                setVolume((0.05f + (targetVolume - 0.05f) * fraction).coerceIn(0.05f, targetVolume))
                delay(stepDelay)
            }
            setVolume(targetVolume)
        }
    }

    private fun targetAlarmVolume(alarm: AlarmState): Float =
        (alarm.volumePercent / 100f).coerceIn(0.1f, 1.0f)

    private fun startVibration() {
        stopVibration()
        val pattern = longArrayOf(0, 600, 400)
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib

        val effect = VibrationEffect.createWaveform(pattern, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attrs = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vib.vibrate(effect, attrs)
        } else {
            @Suppress("DEPRECATION")
            val attrs = AndroidAudioAttributes.Builder()
                .setUsage(AndroidAudioAttributes.USAGE_ALARM)
                .setContentType(AndroidAudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            @Suppress("DEPRECATION")
            vib.vibrate(effect, attrs)
        }
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    fun handleSnoozeAction() {
        val alarm = currentAlarm
        stopAudioAndTimersOnly()
        if (alarm != null) {
            AlarmManagerHelper.snoozeAlarm(this, alarm)
        }
        stopForegroundAndClear()
    }

    fun handleStopAction() {
        val alarm = currentAlarm
        stopAudioAndTimersOnly()
        if (alarm != null) {
            AlarmManagerHelper.markOneTimeAlarmDisabled(this, alarm.id)
        }
        stopForegroundAndClear()
    }

    private fun stopAudioAndTimersOnly() {
        watchdogJob?.cancel()
        watchdogJob = null
        fadeInJob?.cancel()
        fadeInJob = null
        autoTimeoutJob?.cancel()
        autoTimeoutJob = null

        releaseExoPlayer()
        stopFallbackRingtone()
        stopVibration()
        abandonAudioFocus()
        releaseWakeLock()
    }

    private fun stopForegroundAndClear() {
        currentAlarm = null
        _activeAlarmState.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun releaseExoPlayer() {
        runCatching {
            exoPlayer?.stop()
            exoPlayer?.release()
        }
        exoPlayer = null
    }

    private fun requestAlarmAudioFocus() {
        val am = audioManager ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AndroidAudioAttributes.Builder()
                    .setUsage(AndroidAudioAttributes.USAGE_ALARM)
                    .setContentType(AndroidAudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .build()
        audioFocusRequest = request
        am.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { req ->
            runCatching { audioManager?.abandonAudioFocusRequest(req) }
        }
        audioFocusRequest = null
    }

    private fun acquireWakeLock() {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AvidTune:AlarmServiceWakeLock"
        ).apply {
            acquire(20 * 60_000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildAlarmNotification(alarm: AlarmState): Notification {
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmManagerHelper.EXTRA_ALARM_ID, alarm.id)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            "activity:${alarm.id}".hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_SNOZE
        }
        val snoozePendingIntent = PendingIntent.getService(
            this,
            "snooze_btn:${alarm.id}".hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            "stop_btn:${alarm.id}".hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = alarm.label.ifBlank { getString(R.string.alarm_notification_title) }
        val text = if (alarm.songTitle.isNotBlank()) {
            listOfNotNull(alarm.songTitle, alarm.songArtist?.takeIf { it.isNotBlank() }).joinToString(" • ")
        } else {
            getString(R.string.alarm_notification_text)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.avidtune_monochrome)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, getString(R.string.alarm_snooze), snoozePendingIntent)
            .addAction(0, getString(R.string.alarm_stop), stopPendingIntent)
            .build()
    }

    private fun buildFallbackEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.avidtune_monochrome)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()
    }

    override fun onDestroy() {
        stopAudioAndTimersOnly()
        _activeAlarmState.value = null
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "alarm_playback_channel"
        private const val NOTIFICATION_ID = 7771

        const val ACTION_START_ALARM = "com.cgens67.avidtune.alarm.START"
        const val ACTION_STOP = "com.cgens67.avidtune.alarm.STOP"
        const val ACTION_SNOOZE = "com.cgens67.avidtune.alarm.SNOOZE"

        private val _activeAlarmState = MutableStateFlow<ActiveAlarmInfo?>(null)
        val activeAlarmState: StateFlow<ActiveAlarmInfo?> = _activeAlarmState.asStateFlow()
    }
}