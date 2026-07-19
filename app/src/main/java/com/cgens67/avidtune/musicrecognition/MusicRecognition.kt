package com.cgens67.avidtune.musicrecognition

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.*
import android.graphics.drawable.Icon
import android.media.*
import android.media.projection.*
import android.net.Uri
import android.os.*
import android.service.quicksettings.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.*
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.*
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import com.cgens67.avidtune.BuildConfig
import com.cgens67.avidtune.MainActivity
import com.cgens67.avidtune.R
import com.cgens67.avidtune.shazamkit.*
import com.cgens67.avidtune.utils.dataStore
import org.json.*
import timber.log.Timber
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

const val MusicRecognitionRoute = "music_recognition"
const val ACTION_MUSIC_RECOGNITION = "com.cgens67.avidtune.action.MUSIC_RECOGNITION"
const val MusicRecognitionAutoStartRequestKey = "music_recognition_auto_start_request"
fun NavHostController.openMusicRecognition(autoStartRequestId: Long = System.currentTimeMillis()) { if (currentDestination?.route != MusicRecognitionRoute && !popBackStack(MusicRecognitionRoute, false)) navigate(MusicRecognitionRoute) { launchSingleTop = true }; getBackStackEntry(MusicRecognitionRoute).savedStateHandle[MusicRecognitionAutoStartRequestKey] = autoStartRequestId }

data class RecognizedTrack(val trackId: String, val title: String, val artist: String, val album: String?, val coverArtUrl: String?, val coverArtHqUrl: String?, val genre: String?, val releaseDate: String?, val label: String?, val lyrics: List<String>, val shazamUrl: String?, val isrc: String?) { val searchQuery get() = "$title $artist".trim() }
data class RecognitionHistoryEntry(val trackId: String, val title: String, val artist: String, val album: String?, val coverArtUrl: String?, val coverArtHqUrl: String?, val genre: String?, val releaseDate: String?, val shazamUrl: String?, val isrc: String?, val recognizedAtEpochMillis: Long) { val stableKey get() = trackId.takeIf { it.isNotBlank() } ?: listOf(title, artist, isrc.orEmpty()).joinToString("|") { it.trim().lowercase() }; val searchQuery get() = "$title $artist".trim() }
data class BackgroundRecognitionSetting(val enabled: Boolean, val available: Boolean)
enum class RecognitionPhase { Listening, Processing }
sealed interface MusicRecognitionFailure { data object NoMatch : MusicRecognitionFailure; data object RecordingFailed : MusicRecognitionFailure; data object SignatureFailed : MusicRecognitionFailure; data object RecognitionFailed : MusicRecognitionFailure }
class MusicRecognitionException(val failure: MusicRecognitionFailure, cause: Throwable? = null) : Exception(cause)
internal enum class BackgroundRecognitionState { Idle, Listening, Processing }
internal object MusicRecognitionRuntimeState { @Volatile var state: BackgroundRecognitionState = BackgroundRecognitionState.Idle; fun update(state: BackgroundRecognitionState) { this.state = state } }

class MusicRecognitionNotificationManager @Inject constructor(@ApplicationContext private val context: Context) {
    fun createChannel() = context.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("music_recognition", context.getString(R.string.music_recognition_notification_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply { description = context.getString(R.string.music_recognition_notification_channel_description); setShowBadge(true) })
    fun listening() = baseBuilder(context.getString(R.string.music_recognition_notification_listening_title), context.getString(R.string.music_recognition_notification_listening_text), R.string.music_recognition_notification_status_listening, true).setProgress(0, 0, true).addAction(R.drawable.close, context.getString(R.string.cancel), cancelIntent()).build()
    fun processing() = baseBuilder(context.getString(R.string.music_recognition_notification_processing_title), context.getString(R.string.music_recognition_notification_processing_text), R.string.music_recognition_notification_status_processing, false).setProgress(0, 0, true).addAction(R.drawable.close, context.getString(R.string.cancel), cancelIntent()).build()
    fun result(track: RecognizedTrack): Notification {
        val details = listOfNotNull(context.getString(R.string.music_recognition_notification_artist, track.artist), track.album?.takeIf{it.isNotBlank()}?.let{context.getString(R.string.music_recognition_notification_album, it)}, track.genre?.takeIf{it.isNotBlank()}?.let{context.getString(R.string.music_recognition_notification_genre, it)}, track.releaseDate?.takeIf{it.isNotBlank()}?.let{context.getString(R.string.music_recognition_notification_release, it)}, track.label?.takeIf{it.isNotBlank()}?.let{context.getString(R.string.music_recognition_notification_label, it)}, track.isrc?.takeIf{it.isNotBlank()}?.let{context.getString(R.string.music_recognition_notification_isrc, it)}).joinToString("\n")
        return baseBuilder(track.title, listOfNotNull(track.artist, track.album?.takeIf{it.isNotBlank()}).joinToString(" • "), R.string.music_recognition_notification_status_result, true).setContentIntent(resultIntent(track.shazamUrl)).setStyle(NotificationCompat.BigTextStyle().bigText(details)).setTimeoutAfter(300000L).build()
    }
    fun failure(failure: MusicRecognitionFailure): Notification {
        val title = when (failure) { MusicRecognitionFailure.NoMatch -> R.string.music_recognition_notification_no_match_title; MusicRecognitionFailure.RecordingFailed -> R.string.music_recognition_notification_recording_failed_title; MusicRecognitionFailure.SignatureFailed -> R.string.music_recognition_signature_failed; MusicRecognitionFailure.RecognitionFailed -> R.string.music_recognition_recognition_failed }
        val text = when (failure) { MusicRecognitionFailure.NoMatch -> R.string.music_recognition_notification_no_match_text; MusicRecognitionFailure.RecordingFailed -> R.string.music_recognition_notification_recording_failed_text; MusicRecognitionFailure.SignatureFailed -> R.string.music_recognition_notification_signature_failed_text; MusicRecognitionFailure.RecognitionFailed -> R.string.music_recognition_notification_recognition_failed_text }
        return baseBuilder(context.getString(title), context.getString(text), R.string.music_recognition_notification_status_result, true).setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(text))).setTimeoutAfter(300000L).build()
    }
    fun notify(notification: Notification) = NotificationManagerCompat.from(context).notify(9410, notification)
    fun cancel() = NotificationManagerCompat.from(context).cancel(9410)
    private fun baseBuilder(title: String, text: String, @StringRes status: Int, alert: Boolean) = NotificationCompat.Builder(context, "music_recognition").setSmallIcon(R.drawable.music_note).setContentTitle(title).setContentText(text).setSubText(context.getString(status)).setCategory(NotificationCompat.CATEGORY_STATUS).setPriority(NotificationCompat.PRIORITY_DEFAULT).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setAutoCancel(true).setOngoing(false).setSilent(false).setOnlyAlertOnce(!alert).setContentIntent(resultIntent(null))
    private fun resultIntent(shazamUrl: String?) = PendingIntent.getActivity(context, 9411, shazamUrl?.takeIf{it.isNotBlank()}?.let{ Intent(Intent.ACTION_VIEW, Uri.parse(it)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } ?: Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun cancelIntent() = PendingIntent.getService(context, 9412, BackgroundMusicRecognitionService.cancelIntent(context), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

@Singleton class MusicRecognitionRepository @Inject constructor(@ApplicationContext private val context: Context) {
    fun observeHistory(): Flow<List<RecognitionHistoryEntry>> = context.dataStore.data.map { p -> decode(p[stringPreferencesKey("musicRecognitionHistoryJson")]) }.catch { if (it is CancellationException) throw it; Timber.e(it); emit(emptyList()) }.flowOn(Dispatchers.IO)
    fun observeBackgroundRecognitionEnabled() = context.dataStore.data.map { it[booleanPreferencesKey("musicRecognitionBackgroundEnabled")] ?: true }.distinctUntilChanged().flowOn(Dispatchers.IO)
    suspend fun isBackgroundRecognitionEnabled() = withContext(Dispatchers.IO) { context.dataStore.data.first()[booleanPreferencesKey("musicRecognitionBackgroundEnabled")] ?: true }
    suspend fun setBackgroundRecognitionEnabled(enabled: Boolean) = withContext(Dispatchers.IO) { context.dataStore.edit { it[booleanPreferencesKey("musicRecognitionBackgroundEnabled")] = enabled } }
    suspend fun captureAudio() = withContext(Dispatchers.IO) { captureSamples(AudioRecord(MediaRecorder.AudioSource.MIC, 16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)), 4096) }
    suspend fun captureDevicePlayback(proj: MediaProjection) = withContext(Dispatchers.IO) { captureSamples(AudioRecord.Builder().setAudioFormat(AudioFormat.Builder().setSampleRate(16000).setChannelMask(AudioFormat.CHANNEL_IN_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build()).setBufferSizeInBytes(AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)).setAudioPlaybackCaptureConfig(AudioPlaybackCaptureConfiguration.Builder(proj).addMatchingUsage(AudioAttributes.USAGE_MEDIA).addMatchingUsage(AudioAttributes.USAGE_GAME).addMatchingUsage(AudioAttributes.USAGE_UNKNOWN).build()).build(), 4096) }
    suspend fun recognize(samples: ShortArray): Result<RecognizedTrack> {
        val sig = withContext(Dispatchers.Default) { ShazamSignatureGenerator().apply{feedPcm16Mono(samples)}.nextSignatureOrNull() } ?: return Result.failure(MusicRecognitionException(MusicRecognitionFailure.SignatureFailed))
        return withContext(Dispatchers.IO) { Shazam.recognize(sig.uri, sig.sampleDurationMs).fold(onSuccess = { Result.success(RecognizedTrack(it.trackId, it.title, it.artist, it.album, it.coverArtUrl, it.coverArtHqUrl, it.genre, it.releaseDate, it.label, it.lyrics.orEmpty(), it.shazamUrl, it.isrc)) }, onFailure = { if (it is CancellationException) throw it; Result.failure(MusicRecognitionException(if(it.message?.contains("no match", true)==true || it.message?.contains("404")==true) MusicRecognitionFailure.NoMatch else MusicRecognitionFailure.RecognitionFailed, it)) }) }
    }
    suspend fun saveToHistory(track: RecognizedTrack) {
        val entry = track.run { RecognitionHistoryEntry(trackId, title, artist, album, coverArtUrl, coverArtHqUrl, genre, releaseDate, shazamUrl, isrc, System.currentTimeMillis()) }
        withContext(Dispatchers.IO) { context.dataStore.edit { p -> val next = listOf(entry) + decode(p[stringPreferencesKey("musicRecognitionHistoryJson")]).filterNot { it.stableKey == entry.stableKey }; p[stringPreferencesKey("musicRecognitionHistoryJson")] = encode(next.take(50)) } }
    }
    private suspend fun captureSamples(rec: AudioRecord, bufSize: Int): ShortArray {
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); error("AudioRecord failed") }
        val out = ShortArray((4200L * 16000 / 1000L).toInt()); val buf = ShortArray(bufSize / 2)
        try { rec.startRecording(); var written = 0; while (written < out.size) { coroutineContext.ensureActive(); val read = rec.read(buf, 0, minOf(buf.size, out.size - written)); if (read < 0) error("Failed"); if (read == 0) continue; buf.copyInto(out, written, 0, read); written += read }; return out.copyOf(written) } finally { runCatching{rec.stop()}; rec.release() }
    }
    private fun decode(raw: String?) = runCatching { raw?.takeIf { it.isNotBlank() }?.let { JSONArray(it).let { arr -> List(arr.length()) { i -> arr.getJSONObject(i).let { o -> RecognitionHistoryEntry(o.optString("trackId"), o.optString("title"), o.optString("artist"), o.optString("album").takeIf{it.isNotBlank()}, o.optString("coverArtUrl").takeIf{it.isNotBlank()}, o.optString("coverArtHqUrl").takeIf{it.isNotBlank()}, o.optString("genre").takeIf{it.isNotBlank()}, o.optString("releaseDate").takeIf{it.isNotBlank()}, o.optString("shazamUrl").takeIf{it.isNotBlank()}, o.optString("isrc").takeIf{it.isNotBlank()}, o.optLong("recognizedAtEpochMillis")) } } } } ?: emptyList() }.getOrDefault(emptyList())
    private fun encode(entries: List<RecognitionHistoryEntry>) = JSONArray().apply { entries.forEach { e -> put(JSONObject().apply { put("trackId", e.trackId); put("title", e.title); put("artist", e.artist); put("album", e.album ?: ""); put("coverArtUrl", e.coverArtUrl ?: ""); put("coverArtHqUrl", e.coverArtHqUrl ?: ""); put("genre", e.genre ?: ""); put("releaseDate", e.releaseDate ?: ""); put("shazamUrl", e.shazamUrl ?: ""); put("isrc", e.isrc ?: ""); put("recognizedAtEpochMillis", e.recognizedAtEpochMillis) }) } }.toString()
}

class RecognizeMusicUseCase @Inject constructor(private val repo: MusicRecognitionRepository) {
    suspend operator fun invoke(onPhaseChanged: (RecognitionPhase) -> Unit) = recognize(repo::captureAudio, onPhaseChanged)
    suspend fun fromDevicePlayback(proj: MediaProjection, onPhaseChanged: (RecognitionPhase) -> Unit) = recognize({ repo.captureDevicePlayback(proj) }, onPhaseChanged)
    private suspend fun recognize(capture: suspend () -> ShortArray, onPhase: (RecognitionPhase) -> Unit): Result<RecognizedTrack> {
        onPhase(RecognitionPhase.Listening)
        val samples = try { capture() } catch (e: Exception) { if(e is CancellationException) throw e; return Result.failure(MusicRecognitionException(MusicRecognitionFailure.RecordingFailed, e)) }
        if (samples.isEmpty()) return Result.failure(MusicRecognitionException(MusicRecognitionFailure.RecordingFailed))
        onPhase(RecognitionPhase.Processing)
        val res = try { repo.recognize(samples) } catch (e: Exception) { if(e is CancellationException) throw e; Result.failure(MusicRecognitionException(MusicRecognitionFailure.RecognitionFailed, e)) }
        res.getOrNull()?.let { try { repo.saveToHistory(it) } catch (e: Exception) { if (e is CancellationException) throw e; Timber.e(e) } }; return res
    }
}
class ObserveRecognitionHistoryUseCase @Inject constructor(private val repo: MusicRecognitionRepository) { operator fun invoke() = repo.observeHistory() }
class FilterRecognitionHistoryUseCase @Inject constructor() { operator fun invoke(history: List<RecognitionHistoryEntry>, query: String) = query.trim().let { q -> if (q.isEmpty()) history else history.filter { listOfNotNull(it.title, it.artist, it.album, it.genre, it.releaseDate, it.isrc).any { v -> v.contains(q, true) } } } }
class ObserveBackgroundRecognitionSettingUseCase @Inject constructor(private val repo: MusicRecognitionRepository) { operator fun invoke() = repo.observeBackgroundRecognitionEnabled().map { BackgroundRecognitionSetting(it, true) }.distinctUntilChanged() }
class SetBackgroundRecognitionEnabledUseCase @Inject constructor(private val repo: MusicRecognitionRepository) { suspend operator fun invoke(enabled: Boolean) { repo.setBackgroundRecognitionEnabled(enabled) } }
class IsBackgroundRecognitionEnabledUseCase @Inject constructor(private val repo: MusicRecognitionRepository) { suspend operator fun invoke() = repo.isBackgroundRecognitionEnabled() }

sealed interface MusicRecognitionScreenState { val history: RecognitionHistoryUiModel; @Immutable data class Loading(val phase: RecognitionPhaseUi, override val history: RecognitionHistoryUiModel) : MusicRecognitionScreenState; @Immutable data class Success(val track: RecognizedTrackUiModel, override val history: RecognitionHistoryUiModel) : MusicRecognitionScreenState; @Immutable data class Empty(override val history: RecognitionHistoryUiModel) : MusicRecognitionScreenState; @Immutable data class Error(val error: MusicRecognitionErrorUi, override val history: RecognitionHistoryUiModel) : MusicRecognitionScreenState }
enum class RecognitionPhaseUi { Listening, Processing }
enum class MusicRecognitionErrorUi { PermissionRequired, NoMatch, RecordingFailed, SignatureFailed, RecognitionFailed }
@Immutable data class RecognizedTrackUiModel(val title: String, val artist: String, val album: String?, val artworkUrl: String?, val metadata: String, val label: String?, val lyricsPreview: String?, val shazamUrl: String?, val isrc: String?, val searchQuery: String)
@Immutable data class RecognitionHistoryItemUiModel(val stableKey: String, val title: String, val artist: String, val artworkUrl: String?, val metadata: String, val recognizedAt: String, val shazamUrl: String?, val searchQuery: String)
@Immutable data class RecognitionHistoryUiModel(val items: List<RecognitionHistoryItemUiModel>)
@Immutable data class RecognitionHistorySheetUiState(val visible: Boolean, val query: String, val allItems: RecognitionHistoryUiModel, val filteredItems: RecognitionHistoryUiModel)
@Immutable data class MusicRecognitionSettingsUiState(val visible: Boolean, val backgroundRecognitionEnabled: Boolean, val backgroundRecognitionAvailable: Boolean)
sealed interface MusicRecognitionEvent { data object RequestMicrophonePermission : MusicRecognitionEvent; data object RecognitionStarted : MusicRecognitionEvent; data class Search(val query: String) : MusicRecognitionEvent; data class OpenUri(val uri: String) : MusicRecognitionEvent }

@HiltViewModel class MusicRecognitionViewModel @Inject constructor(handle: SavedStateHandle, obsHist: ObserveRecognitionHistoryUseCase, obsBg: ObserveBackgroundRecognitionSettingUseCase, private val filterHist: FilterRecognitionHistoryUseCase, private val recognize: RecognizeMusicUseCase, private val setBg: SetBackgroundRecognitionEnabledUseCase) : ViewModel() {
    private val emptyHist = RecognitionHistoryUiModel(emptyList()); private val _state = MutableStateFlow<MusicRecognitionScreenState>(MusicRecognitionScreenState.Empty(emptyHist)); val screenState = _state.asStateFlow()
    private val _vis = MutableStateFlow(false); private val _query = MutableStateFlow(""); private val _histUi = MutableStateFlow(emptyHist)
    private val histEntries = obsHist().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val _settings = MutableStateFlow(MusicRecognitionSettingsUiState(false, false, false)); val settingsState = _settings.asStateFlow()
    val historySheetState = combine(_vis, _query, histEntries, _histUi) { vis, q, ent, ui -> val keys = filterHist(ent, q).map{it.stableKey}.toSet(); RecognitionHistorySheetUiState(vis, q, ui, RecognitionHistoryUiModel(ui.items.filter { it.stableKey in keys })) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecognitionHistorySheetUiState(false, "", emptyHist, emptyHist))
    private val _events = Channel<MusicRecognitionEvent>(Channel.BUFFERED); val events = _events.receiveAsFlow(); private var job: Job? = null; private var bgJob: Job? = null; private var bgPersisted = false
    init { viewModelScope.launch { histEntries.collect { e -> val m = RecognitionHistoryUiModel(e.map { RecognitionHistoryItemUiModel(it.stableKey, it.title, it.artist, it.coverArtHqUrl ?: it.coverArtUrl, listOfNotNull(it.album, it.genre, it.releaseDate).filter{ s -> s.isNotBlank() }.joinToString(" • "), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it.recognizedAtEpochMillis)), it.shazamUrl, it.searchQuery) }); _histUi.value = m; update(m) } }; viewModelScope.launch { obsBg().collect { bgPersisted = it.enabled; _settings.value = _settings.value.copy(backgroundRecognitionEnabled = it.enabled, backgroundRecognitionAvailable = it.available) } }; viewModelScope.launch { handle.getStateFlow(MusicRecognitionAutoStartRequestKey, 0L).collect { if (it != 0L) { handle[MusicRecognitionAutoStartRequestKey] = 0L; _events.send(MusicRecognitionEvent.RequestMicrophonePermission) } } } }
    fun onListenRequested() { if (job?.isActive != true) _events.trySend(MusicRecognitionEvent.RequestMicrophonePermission) }
    fun onMicrophonePermissionResult(gr: Boolean) { if (!gr) _state.value = MusicRecognitionScreenState.Error(MusicRecognitionErrorUi.PermissionRequired, _histUi.value) else { job?.cancel(); job = viewModelScope.launch { _events.send(MusicRecognitionEvent.RecognitionStarted); recognize { _state.value = MusicRecognitionScreenState.Loading(if (it == RecognitionPhase.Listening) RecognitionPhaseUi.Listening else RecognitionPhaseUi.Processing, _histUi.value) }.fold(onSuccess = { _state.value = MusicRecognitionScreenState.Success(RecognizedTrackUiModel(it.title, it.artist, it.album, it.coverArtHqUrl ?: it.coverArtUrl, listOfNotNull(it.genre, it.releaseDate).filter{s->s.isNotBlank()}.joinToString(" • "), it.label, it.lyrics.take(6).takeIf{l->l.isNotEmpty()}?.joinToString("\n"), it.shazamUrl, it.isrc, it.searchQuery), _histUi.value) }, onFailure = { if (it is CancellationException) throw it; _state.value = MusicRecognitionScreenState.Error(when((it as? MusicRecognitionException)?.failure) { MusicRecognitionFailure.NoMatch -> MusicRecognitionErrorUi.NoMatch; MusicRecognitionFailure.RecordingFailed -> MusicRecognitionErrorUi.RecordingFailed; MusicRecognitionFailure.SignatureFailed -> MusicRecognitionErrorUi.SignatureFailed; else -> MusicRecognitionErrorUi.RecognitionFailed }, _histUi.value) }); job = null } } }
    fun onCancelRecognition() { job?.cancel(); job = null; _state.value = MusicRecognitionScreenState.Empty(_histUi.value) }
    fun onHistoryVisibilityChanged(vis: Boolean) { _vis.value = vis; if (!vis) _query.value = "" }
    fun onSettingsVisibilityChanged(vis: Boolean) { _settings.value = _settings.value.copy(visible = vis) }
    fun onBackgroundRecognitionEnabledChanged(en: Boolean) { if (_settings.value.backgroundRecognitionAvailable) { _settings.value = _settings.value.copy(backgroundRecognitionEnabled = en); bgJob?.cancel(); bgJob = viewModelScope.launch { runCatching { setBg(en) }.onFailure { if(it is CancellationException) throw it; _settings.value = _settings.value.copy(backgroundRecognitionEnabled = bgPersisted) } } } }
    fun onHistoryQueryChanged(q: String) { _query.value = q }
    fun onTrackSearchRequested(q: String) { q.trim().takeIf{it.isNotEmpty()}?.let { _vis.value = false; _query.value = ""; _events.trySend(MusicRecognitionEvent.Search(it)) } }
    fun onExternalUriRequested(u: String) { u.trim().takeIf{it.isNotEmpty()}?.let { _events.trySend(MusicRecognitionEvent.OpenUri(it)) } }
    private fun update(h: RecognitionHistoryUiModel) { _state.value = when(val s = _state.value) { is MusicRecognitionScreenState.Empty -> s.copy(history=h); is MusicRecognitionScreenState.Error -> s.copy(history=h); is MusicRecognitionScreenState.Loading -> s.copy(history=h); is MusicRecognitionScreenState.Success -> s.copy(history=h) } }
}

@AndroidEntryPoint class BackgroundMusicRecognitionService : Service() {
    @Inject lateinit var recognize: RecognizeMusicUseCase; @Inject lateinit var notif: MusicRecognitionNotificationManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate); private var job: Job? = null; private var proj: MediaProjection? = null; private var cb: MediaProjection.Callback? = null
    override fun onCreate() { super.onCreate(); notif.createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { when (intent?.action) { "CANCEL" -> cancel(); "PLAYBACK" -> play(intent); "MIC" -> mic(); else -> stopSelf(startId) }; return START_NOT_STICKY }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { job?.cancel(); release(); MusicRecognitionRuntimeState.update(BackgroundRecognitionState.Idle); refresh(); scope.cancel(); super.onDestroy() }
    private fun play(intent: Intent) {
        if (job?.isActive == true) return
        val code = intent.getIntExtra("CODE", Activity.RESULT_CANCELED); val data = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra("DATA", Intent::class.java) else intent.getParcelableExtra("DATA")
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return }
        startFg(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        proj = try { (getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(code, data) } catch(e: Exception) { publish(MusicRecognitionFailure.RecordingFailed); return } ?: return publish(MusicRecognitionFailure.RecordingFailed)
        cb = object : MediaProjection.Callback() { override fun onStop() { job?.cancel(CancellationException()) } }; proj!!.registerCallback(cb!!, Handler(Looper.getMainLooper()))
        job = scope.launch(start = CoroutineStart.LAZY) { runRec { recognize.fromDevicePlayback(proj!!) { p -> onPhase(p) } } }; job?.start()
    }
    private fun mic() { if (job?.isActive == true) return; startFg(if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0); job = scope.launch { runRec { recognize { onPhase(it) } } } }
    private fun startFg(type: Int) { MusicRecognitionRuntimeState.update(BackgroundRecognitionState.Listening); refresh(); ServiceCompat.startForeground(this, 9410, notif.listening(), type) }
    private suspend fun runRec(r: suspend () -> Result<RecognizedTrack>) { try { r().fold(onSuccess = { finishFg(); notif.notify(notif.result(it)) }, onFailure = { if (it is CancellationException) throw it; finishFg(); notif.notify(notif.failure((it as? MusicRecognitionException)?.failure ?: MusicRecognitionFailure.RecognitionFailed)) }) } catch (e: CancellationException) { throw e } catch (e: Exception) { finishFg(); notif.notify(notif.failure(MusicRecognitionFailure.RecognitionFailed)) } finally { release(); job = null; MusicRecognitionRuntimeState.update(BackgroundRecognitionState.Idle); refresh(); stopSelf() } }
    private fun onPhase(p: RecognitionPhase) { MusicRecognitionRuntimeState.update(if (p == RecognitionPhase.Listening) BackgroundRecognitionState.Listening else BackgroundRecognitionState.Processing); notif.notify(if (p == RecognitionPhase.Listening) notif.listening() else notif.processing()); refresh() }
    private fun cancel() { job?.cancel(); job = null; release(); MusicRecognitionRuntimeState.update(BackgroundRecognitionState.Idle); refresh(); stopForeground(STOP_FOREGROUND_REMOVE); notif.cancel(); stopSelf() }
    private fun publish(f: MusicRecognitionFailure) { finishFg(); notif.notify(notif.failure(f)); MusicRecognitionRuntimeState.update(BackgroundRecognitionState.Idle); refresh(); stopSelf() }
    private fun finishFg() = stopForeground(STOP_FOREGROUND_DETACH)
    private fun release() { proj?.let { p -> cb?.let(p::unregisterCallback); cb = null; proj = null; runCatching(p::stop) } }
    private fun refresh() = TileService.requestListeningState(this, ComponentName(this, MusicRecognitionTileService::class.java))
    companion object { fun devicePlaybackIntent(ctx: Context, code: Int, data: Intent) = Intent(ctx, BackgroundMusicRecognitionService::class.java).apply { action = "PLAYBACK"; putExtra("CODE", code); putExtra("DATA", data) }; fun microphoneIntent(ctx: Context) = Intent(ctx, BackgroundMusicRecognitionService::class.java).apply { action = "MIC" }; fun cancelIntent(ctx: Context) = Intent(ctx, BackgroundMusicRecognitionService::class.java).apply { action = "CANCEL" } }
}

class MusicRecognitionCaptureActivity : ComponentActivity() {
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) capture() else { finish(); overridePendingTransition(0, 0) } }
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { if (it.resultCode == Activity.RESULT_OK && it.data != null) ContextCompat.startForegroundService(this, BackgroundMusicRecognitionService.devicePlaybackIntent(this, it.resultCode, it.data!!)); finish(); overridePendingTransition(0, 0) }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); if (savedInstanceState == null) { val missing = buildList { if (ContextCompat.checkSelfPermission(this@MusicRecognitionCaptureActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.RECORD_AUDIO); if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MusicRecognitionCaptureActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) add(Manifest.permission.POST_NOTIFICATIONS) }; if (missing.isEmpty()) capture() else permLauncher.launch(missing.toTypedArray()) } }
    private fun capture() { if (Build.VERSION.SDK_INT < 29) { ContextCompat.startForegroundService(this, BackgroundMusicRecognitionService.microphoneIntent(this)); finish(); overridePendingTransition(0, 0) } else captureLauncher.launch((getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()) }
}

@AndroidEntryPoint class MusicRecognitionTileActionActivity : ComponentActivity() {
    @Inject lateinit var isBg: IsBackgroundRecognitionEnabledUseCase
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); lifecycleScope.launch { startActivity(if (try { isBg() } catch (e: Exception) { false }) Intent(this@MusicRecognitionTileActionActivity, MusicRecognitionCaptureActivity::class.java) else Intent(this@MusicRecognitionTileActionActivity, MainActivity::class.java).apply { action = ACTION_MUSIC_RECOGNITION; addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }); finish(); overridePendingTransition(0, 0) } }
}

class MusicRecognitionTileService : TileService() {
    override fun onStartListening() { super.onStartListening(); qsTile?.apply { state = if (MusicRecognitionRuntimeState.state == BackgroundRecognitionState.Idle) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE; label = getString(R.string.music_recognition); if (Build.VERSION.SDK_INT >= 29) subtitle = getString(when (MusicRecognitionRuntimeState.state) { BackgroundRecognitionState.Idle -> R.string.music_recognition_tap_to_listen; BackgroundRecognitionState.Listening -> R.string.music_recognition_listening; BackgroundRecognitionState.Processing -> R.string.music_recognition_processing }); icon = Icon.createWithResource(this@MusicRecognitionTileService, R.drawable.music_note); updateTile() } }
    override fun onClick() { super.onClick(); if (MusicRecognitionRuntimeState.state != BackgroundRecognitionState.Idle) startService(BackgroundMusicRecognitionService.cancelIntent(this)) else { val i = Intent(this, MusicRecognitionTileActionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP); if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)) else startActivityAndCollapse(i) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicRecognitionScreen(
    navController: NavHostController,
    viewModel: MusicRecognitionViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_recognition)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is MusicRecognitionScreenState.Empty -> {
                    Button(onClick = { viewModel.onListenRequested() }) {
                        Text(stringResource(R.string.music_recognition_tap_to_listen))
                    }
                }
                is MusicRecognitionScreenState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(if (s.phase == RecognitionPhaseUi.Listening) stringResource(R.string.music_recognition_listening) else stringResource(R.string.music_recognition_processing))
                    }
                }
                is MusicRecognitionScreenState.Success -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.track.title, style = MaterialTheme.typography.titleLarge)
                        Text(s.track.artist, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.onListenRequested() }) {
                            Text(stringResource(R.string.music_recognition_tap_to_listen))
                        }
                    }
                }
                is MusicRecognitionScreenState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(s.error.name, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.onListenRequested() }) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }
    }
}
