package com.cgens67.avidtune.together

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.*
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.component.AvatarPreferenceManager
import com.cgens67.avidtune.ui.component.AvatarSelection
import com.cgens67.avidtune.ui.component.AvatarUtils
import com.cgens67.avidtune.ui.component.IconButton as AtIconButton
import com.cgens67.avidtune.ui.component.LocalMenuState
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberPreference
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO as ClientCIO
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// --- PREFERENCES ---
val TogetherAllowGuestsToAddTracksKey = booleanPreferencesKey("TogetherAllowGuestsToAddTracks")
val TogetherAllowGuestsToControlPlaybackKey = booleanPreferencesKey("TogetherAllowGuestsToControlPlayback")
val TogetherDefaultPortKey = intPreferencesKey("TogetherDefaultPort")
val TogetherDisplayNameKey = stringPreferencesKey("TogetherDisplayName")
val TogetherLastJoinLinkKey = stringPreferencesKey("TogetherLastJoinLink")
val TogetherRequireHostApprovalToJoinKey = booleanPreferencesKey("TogetherRequireHostApprovalToJoin")
val TogetherWelcomeShownKey = booleanPreferencesKey("TogetherWelcomeShown")

// --- MODELS & MESSAGES ---
@Serializable data class TogetherTrack(val id: String, val title: String, val artists: List<String> = emptyList(), val durationSec: Int = -1, val thumbnailUrl: String? = null)
@Serializable data class TogetherParticipant(val id: String, val name: String, val isHost: Boolean = false, val isPending: Boolean = false, val isConnected: Boolean = true, val avatar: String? = null)
@Serializable data class TogetherRoomSettings(val allowGuestsToAddTracks: Boolean = true, val allowGuestsToControlPlayback: Boolean = false, val requireHostApprovalToJoin: Boolean = false)
@Serializable data class TogetherRoomState(val sessionId: String, val hostId: String, val participants: List<TogetherParticipant> = emptyList(), val settings: TogetherRoomSettings = TogetherRoomSettings(), val queue: List<TogetherTrack> = emptyList(), val queueHash: String = "", val currentIndex: Int = 0, val isPlaying: Boolean = false, val positionMs: Long = 0L, val repeatMode: Int = 0, val shuffleEnabled: Boolean = false, val sentAtMs: Long = 0L)
@Serializable data class DiscoveredSession(val pin: String, val hostName: String, val joinInfo: TogetherJoinInfo)

@Serializable sealed class TogetherRole { @Serializable data object Host : TogetherRole(); @Serializable data object Guest : TogetherRole() }
sealed class TogetherSessionState { data object Idle : TogetherSessionState(); data class Hosting(val sessionId: String, val joinLink: String, val pin: String, val localAddressHint: String?, val port: Int, val settings: TogetherRoomSettings, val roomState: TogetherRoomState?) : TogetherSessionState(); data class Joining(val joinLink: String) : TogetherSessionState(); data class Joined(val role: TogetherRole, val sessionId: String, val selfParticipantId: String, val roomState: TogetherRoomState) : TogetherSessionState(); data class Error(val message: String, val recoverable: Boolean = true) : TogetherSessionState() }

const val TogetherProtocolVersion: Int = 1
@Serializable sealed interface TogetherMessage
@Serializable @SerialName("client_hello") data class ClientHello(val protocolVersion: Int, val sessionId: String, val sessionKey: String, val clientId: String, val displayName: String, val avatar: String? = null) : TogetherMessage
@Serializable @SerialName("server_welcome") data class ServerWelcome(val protocolVersion: Int, val sessionId: String, val participantId: String, val role: ServerRole, val isPending: Boolean, val settings: TogetherRoomSettings) : TogetherMessage
@Serializable @SerialName("room_state") data class RoomStateMessage(val state: TogetherRoomState) : TogetherMessage
@Serializable @SerialName("join_decision") data class JoinDecision(val sessionId: String, val participantId: String, val approved: Boolean) : TogetherMessage
@Serializable @SerialName("update_playback") data class UpdatePlayback(val positionMs: Long, val isPlaying: Boolean) : TogetherMessage
@Serializable @SerialName("update_track") data class UpdateTrack(val track: TogetherTrack) : TogetherMessage
@Serializable @SerialName("host_command") data class HostCommand(val command: String, val args: String = "") : TogetherMessage
@Serializable enum class ServerRole { HOST, GUEST }

// --- LINK & JSON ---
object TogetherJson { val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true; classDiscriminator = "type" } }
@Serializable data class TogetherJoinInfo(val host: String, val port: Int, val sessionId: String, val sessionKey: String) {
    fun toWebSocketUrl() = "ws://$host:$port/together"
    fun toDeepLink() = "AvidTune://together?host=$host&port=$port&sid=$sessionId&key=$sessionKey"
}
object TogetherLink {
    fun encode(info: TogetherJoinInfo) = info.toDeepLink()
    fun decode(raw: String): TogetherJoinInfo? {
        val trimmed = raw.trim().replace("\\s+".toRegex(), "")
        runCatching { URI(trimmed) }.getOrNull()?.let { uri ->
            if (uri.scheme?.lowercase() == "avidtune" && uri.authority?.lowercase() == "together") {
                val params = uri.rawQuery?.split("&")?.associate { val p = it.split("="); p[0] to URLDecoder.decode(p[1], "UTF-8") } ?: emptyMap()
                return TogetherJoinInfo(params["host"] ?: return null, params["port"]?.toIntOrNull() ?: return null, params["sid"] ?: return null, params["key"] ?: return null)
            }
        }
        val p = trimmed.split("|")
        if (p.size == 4) return TogetherJoinInfo(p[0], p[1].toIntOrNull() ?: return null, p[2], p[3])
        return null
    }
}

// --- MANAGER ---
class TogetherManager(val scope: CoroutineScope, val player: ExoPlayer) {
    val sessionState = MutableStateFlow<TogetherSessionState>(TogetherSessionState.Idle)
    private var serverEngine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var clientSession: DefaultClientWebSocketSession? = null
    private val httpClient = HttpClient(ClientCIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000L
        }
    }
    private var roomSettings = TogetherRoomSettings()

    private val hostConnections = ConcurrentHashMap<String, io.ktor.websocket.DefaultWebSocketSession>()
    private val hostParticipants = ConcurrentHashMap<String, TogetherParticipant>()
    private var isHost = false
    private var broadcastJob: Job? = null
    
    // UDP Discovery features
    private var discoverySocket: DatagramSocket? = null
    private var discoveryJob: Job? = null
    private var currentPin: String = ""
    private var currentHostName: String = ""
    private var currentJoinInfo: TogetherJoinInfo? = null

    @Volatile private var isSyncing = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (isSyncing) return
            if (isHost) broadcastRoomState()
            else sendGuestUpdate()
        }
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (isSyncing) return
            if (isHost) broadcastRoomState()
            else sendGuestUpdate()
        }
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (isSyncing) return
            if (isHost) broadcastRoomState()
            else sendGuestTrackUpdate(mediaItem)
        }
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (isSyncing) return
            if (isHost && reason == Player.DISCONTINUITY_REASON_SEEK) {
                broadcastRoomState()
            } else if (!isHost && reason == Player.DISCONTINUITY_REASON_SEEK) {
                sendGuestUpdate()
            }
        }
    }

    init {
        player.addListener(playerListener)
    }

    private fun getBroadcastAddresses(): List<InetAddress> {
        val addresses = mutableListOf<InetAddress>()
        try {
            addresses.add(InetAddress.getByName("255.255.255.255"))
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) continue
                for (address in networkInterface.interfaceAddresses) {
                    address.broadcast?.let { addresses.add(it) }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return addresses
    }

    private fun startUdpDiscoveryServer() {
        discoveryJob?.cancel()
        discoverySocket?.close()
        discoveryJob = scope.launch(Dispatchers.IO) {
            try {
                discoverySocket = DatagramSocket(42118).apply { broadcast = true }
                val buffer = ByteArray(1024)
                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    discoverySocket?.receive(packet)
                    val msg = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    if (msg == "AVIDTUNE_DISCOVER_ALL" || msg == "AVIDTUNE_DISCOVER_PIN:$currentPin") {
                        val info = currentJoinInfo ?: continue
                        val replyInfo = DiscoveredSession(currentPin, currentHostName, info)
                        val replyStr = TogetherJson.json.encodeToString(DiscoveredSession.serializer(), replyInfo)
                        val replyData = replyStr.toByteArray(Charsets.UTF_8)
                        val replyPacket = DatagramPacket(replyData, replyData.size, packet.address, packet.port)
                        discoverySocket?.send(replyPacket)
                    }
                }
            } catch (e: Exception) {
                // Ignore socket closed exceptions
            }
        }
    }

    suspend fun discoverSessions(): List<DiscoveredSession> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DiscoveredSession>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 2000
            }
            val msg = "AVIDTUNE_DISCOVER_ALL".toByteArray(Charsets.UTF_8)
            for (address in getBroadcastAddresses()) {
                try { socket.send(DatagramPacket(msg, msg.size, address, 42118)) } catch(e: Exception){}
            }
            val buffer = ByteArray(2048)
            val end = System.currentTimeMillis() + 2000
            while (System.currentTimeMillis() < end) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val replyStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                    val session = TogetherJson.json.decodeFromString(DiscoveredSession.serializer(), replyStr)
                    if (results.none { it.pin == session.pin }) {
                        results.add(session)
                    }
                } catch (e: SocketTimeoutException) {
                    break
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
        } finally {
            socket?.close()
        }
        results
    }

    suspend fun resolvePin(pin: String): DiscoveredSession? = withContext(Dispatchers.IO) {
        var result: DiscoveredSession? = null
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = 2000
            }
            val msg = "AVIDTUNE_DISCOVER_PIN:$pin".toByteArray(Charsets.UTF_8)
            for (address in getBroadcastAddresses()) {
                try { socket.send(DatagramPacket(msg, msg.size, address, 42118)) } catch(e: Exception){}
            }
            val buffer = ByteArray(2048)
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val replyStr = String(packet.data, 0, packet.length, Charsets.UTF_8)
                result = TogetherJson.json.decodeFromString(DiscoveredSession.serializer(), replyStr)
            } catch (e: Exception) {}
        } finally {
            socket?.close()
        }
        result
    }

    private fun sendGuestUpdate() {
        if (isHost || clientSession == null) return
        scope.launch(Dispatchers.Main) {
            val currentState = sessionState.value as? TogetherSessionState.Joined ?: return@launch
            if (!currentState.roomState.settings.allowGuestsToControlPlayback) return@launch
            
            val msg = TogetherJson.json.encodeToString(
                TogetherMessage.serializer(),
                UpdatePlayback(
                    positionMs = player.currentPosition,
                    isPlaying = player.isPlaying
                )
            )
            withContext(Dispatchers.IO) {
                try { clientSession?.send(msg) } catch(e: Exception) {}
            }
        }
    }

    private fun sendGuestTrackUpdate(mediaItem: MediaItem?) {
        if (isHost || clientSession == null || mediaItem == null) return
        scope.launch(Dispatchers.Main) {
            val currentState = sessionState.value as? TogetherSessionState.Joined ?: return@launch
            if (!currentState.roomState.settings.allowGuestsToAddTracks) return@launch
            
            val customMeta = mediaItem.localConfiguration?.tag as? com.cgens67.avidtune.models.MediaMetadata
            val trackId = mediaItem.mediaId
            val trackTitle = customMeta?.title ?: mediaItem.mediaMetadata.title?.toString() ?: ""
            val trackArtists = customMeta?.artists?.map { it.name } ?: listOf(mediaItem.mediaMetadata.artist?.toString() ?: "")
            val trackArt = customMeta?.thumbnailUrl ?: mediaItem.mediaMetadata.artworkUri?.toString()
            val durationSec = customMeta?.duration ?: -1

            val track = TogetherTrack(trackId, trackTitle, trackArtists, durationSec, trackArt)
            
            val msg = TogetherJson.json.encodeToString(
                TogetherMessage.serializer(),
                UpdateTrack(track)
            )
            withContext(Dispatchers.IO) {
                try { clientSession?.send(msg) } catch(e: Exception) {}
            }
        }
    }

    // IMPORTANT: Call this ONLY from the Main thread because ExoPlayer is accessed here
    private fun getCurrentRoomState(sId: String): TogetherRoomState {
        val currentItem = player.currentMediaItem
        val customMeta = currentItem?.localConfiguration?.tag as? com.cgens67.avidtune.models.MediaMetadata
        
        val trackId = currentItem?.mediaId ?: ""
        val trackTitle = customMeta?.title ?: currentItem?.mediaMetadata?.title?.toString() ?: ""
        val trackArtists = customMeta?.artists?.map { it.name } ?: listOf(currentItem?.mediaMetadata?.artist?.toString() ?: "")
        val trackArt = customMeta?.thumbnailUrl ?: currentItem?.mediaMetadata?.artworkUri?.toString()
        val durationSec = customMeta?.duration ?: -1

        val currentTrack = TogetherTrack(
            id = trackId,
            title = trackTitle,
            artists = trackArtists,
            durationSec = durationSec,
            thumbnailUrl = trackArt
        )

        return TogetherRoomState(
            sessionId = sId,
            hostId = hostParticipants.values.firstOrNull { it.isHost }?.id ?: "host",
            participants = hostParticipants.values.toList(),
            settings = roomSettings,
            queue = listOf(currentTrack),
            queueHash = "",
            currentIndex = 0,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            sentAtMs = System.currentTimeMillis()
        )
    }

    private fun broadcastRoomState() {
        val currentState = sessionState.value
        if (currentState is TogetherSessionState.Hosting) {
            // Guarantee we jump to Main before reading the player state
            scope.launch(Dispatchers.Main) {
                try {
                    val roomState = getCurrentRoomState(currentState.sessionId)
                    sessionState.value = currentState.copy(roomState = roomState)

                    val msg = TogetherJson.json.encodeToString(TogetherMessage.serializer(), RoomStateMessage(roomState))
                    withContext(Dispatchers.IO) {
                        hostConnections.values.forEach { session ->
                            try {
                                session.send(msg)
                            } catch (e: Exception) {
                                // ignore
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun startPeriodicBroadcast() {
        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            while (isActive) {
                delay(3000)
                if (isHost && player.isPlaying) {
                    broadcastRoomState()
                }
            }
        }
    }

    fun startTogetherHost(port: Int, displayName: String, settings: TogetherRoomSettings, avatar: String? = null) {
        leaveTogether()
        isHost = true
        scope.launch(Dispatchers.IO) {
            try {
                roomSettings = settings
                val sId = UUID.randomUUID().toString()
                val sKey = UUID.randomUUID().toString()
                val hostIp = getIpAddress() ?: "127.0.0.1"
                
                currentPin = (100000..999999).random().toString()
                currentHostName = displayName
                currentJoinInfo = TogetherJoinInfo(hostIp, port, sId, sKey)

                hostParticipants.clear()
                hostConnections.clear()

                val myHostId = UUID.randomUUID().toString()
                hostParticipants[myHostId] = TogetherParticipant(
                    id = myHostId,
                    name = displayName,
                    isHost = true,
                    isConnected = true,
                    avatar = avatar
                )

                val engine = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                    install(io.ktor.server.websocket.WebSockets) {
                        pingPeriodMillis = 15000L
                        timeoutMillis = 15000L
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }
                    routing {
                        webSocket("/together") {
                            var pId: String? = null
                            try {
                                for (frame in incoming) {
                                    if (frame !is Frame.Text) continue
                                    val txt = frame.readText()
                                    val msg = try { 
                                        TogetherJson.json.decodeFromString<TogetherMessage>(txt) 
                                    } catch (e: Exception) { null }
                                    
                                    when (msg) {
                                        is ClientHello -> {
                                            val isPending = roomSettings.requireHostApprovalToJoin
                                            pId = UUID.randomUUID().toString()
                                            hostParticipants[pId] = TogetherParticipant(
                                                id = pId,
                                                name = msg.displayName,
                                                isHost = false,
                                                isConnected = true,
                                                isPending = isPending,
                                                avatar = msg.avatar
                                            )
                                            hostConnections[pId] = this@webSocket
                                            
                                            send(TogetherJson.json.encodeToString(
                                                TogetherMessage.serializer(), 
                                                ServerWelcome(
                                                    protocolVersion = TogetherProtocolVersion, 
                                                    sessionId = sId, 
                                                    participantId = pId, 
                                                    role = ServerRole.GUEST, 
                                                    isPending = isPending, 
                                                    settings = roomSettings
                                                )
                                            ))
                                            broadcastRoomState()
                                        }
                                        is UpdatePlayback -> {
                                            if (roomSettings.allowGuestsToControlPlayback) {
                                                val p = hostParticipants[pId]
                                                if (p != null && !p.isPending) {
                                                    withContext(Dispatchers.Main) {
                                                        isSyncing = true
                                                        if (msg.isPlaying && !player.isPlaying) player.play()
                                                        else if (!msg.isPlaying && player.isPlaying) player.pause()
                                                        
                                                        if (kotlin.math.abs(player.currentPosition - msg.positionMs) > 1000L) {
                                                            player.seekTo(msg.positionMs)
                                                        }
                                                        isSyncing = false
                                                    }
                                                    broadcastRoomState()
                                                }
                                            }
                                        }
                                        is UpdateTrack -> {
                                            if (roomSettings.allowGuestsToAddTracks) {
                                                val p = hostParticipants[pId]
                                                if (p != null && !p.isPending) {
                                                    withContext(Dispatchers.Main) {
                                                        isSyncing = true
                                                        val customMetadata = com.cgens67.avidtune.models.MediaMetadata(
                                                            id = msg.track.id,
                                                            title = msg.track.title,
                                                            artists = msg.track.artists.map { com.cgens67.avidtune.models.MediaMetadata.Artist(id = null, name = it) },
                                                            duration = msg.track.durationSec,
                                                            thumbnailUrl = msg.track.thumbnailUrl,
                                                            album = null,
                                                            explicit = false,
                                                            liked = false
                                                        )
                                                        val mediaItem = MediaItem.Builder()
                                                            .setMediaId(msg.track.id)
                                                            .setUri(msg.track.id)
                                                            .setCustomCacheKey(msg.track.id)
                                                            .setTag(customMetadata)
                                                            .setMediaMetadata(
                                                                androidx.media3.common.MediaMetadata.Builder()
                                                                    .setTitle(msg.track.title)
                                                                    .setArtist(msg.track.artists.joinToString(", "))
                                                                    .setArtworkUri(msg.track.thumbnailUrl?.let { android.net.Uri.parse(it) })
                                                                    .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                                                                    .build()
                                                            )
                                                            .build()
                                                        player.setMediaItem(mediaItem)
                                                        player.prepare()
                                                        player.play()
                                                        isSyncing = false
                                                    }
                                                    broadcastRoomState()
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                pId?.let {
                                    hostParticipants.remove(it)
                                    hostConnections.remove(it)
                                    broadcastRoomState()
                                }
                            }
                        }
                    }
                }
                engine.start(wait = false)
                serverEngine = engine

                val link = TogetherLink.encode(currentJoinInfo!!)
                startUdpDiscoveryServer()
                
                withContext(Dispatchers.Main) {
                    val rs = getCurrentRoomState(sId)
                    sessionState.value = TogetherSessionState.Hosting(sId, link, currentPin, hostIp, port, settings, rs)
                }
                startPeriodicBroadcast()
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    sessionState.value = TogetherSessionState.Error(e.message ?: "Failed to start server")
                }
            }
        }
    }

    fun joinTogether(inputLink: String, displayName: String, avatar: String? = null) {
        leaveTogether()
        isHost = false
        val joinJob = scope.launch(Dispatchers.IO) {
            val input = inputLink.trim()
            val info = TogetherLink.decode(input) ?: resolvePin(input)?.joinInfo
            if (info == null) {
                withContext(Dispatchers.Main) {
                    sessionState.value = TogetherSessionState.Error("Invalid link or PIN not found on LAN.")
                }
                return@launch
            }
            withContext(Dispatchers.Main) {
                sessionState.value = TogetherSessionState.Joining(inputLink)
            }
            try {
                httpClient.webSocket(info.toWebSocketUrl()) {
                    clientSession = this
                    val myClientId = UUID.randomUUID().toString()
                    send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), ClientHello(TogetherProtocolVersion, info.sessionId, info.sessionKey, myClientId, displayName, avatar)))
                    
                    var selfPId = "guest"

                    try {
                        for (frame in incoming) {
                            if (frame !is Frame.Text) continue
                            val msgText = frame.readText()
                            try {
                                when (val msg = TogetherJson.json.decodeFromString<TogetherMessage>(msgText)) {
                                    is ServerWelcome -> {
                                        selfPId = msg.participantId
                                    }
                                    is RoomStateMessage -> {
                                        val rs = msg.state
                                        withContext(Dispatchers.Main) {
                                            sessionState.value = TogetherSessionState.Joined(TogetherRole.Guest, info.sessionId, selfPId, rs)
                                            val isPending = rs.participants.find { it.id == selfPId }?.isPending == true
                                            if (!isPending) {
                                                syncPlayerToState(rs)
                                            }
                                        }
                                    }
                                    is HostCommand -> {
                                        if (msg.command == "KICK") {
                                            withContext(Dispatchers.Main) {
                                                sessionState.value = TogetherSessionState.Error("You have been disconnected from the session.")
                                            }
                                            close()
                                        }
                                    }
                                    is JoinDecision -> {
                                        if (!msg.approved) {
                                            withContext(Dispatchers.Main) {
                                                sessionState.value = TogetherSessionState.Error("Your request to join was denied.")
                                            }
                                            close()
                                        }
                                    }
                                    else -> {}
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Loop ended, connection closed by host/server
                    withContext(Dispatchers.Main) {
                        val curr = sessionState.value
                        if (curr is TogetherSessionState.Joined || curr is TogetherSessionState.Joining) {
                            sessionState.value = TogetherSessionState.Error("Connection closed by host")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    sessionState.value = TogetherSessionState.Error("Failed to connect: ${e.message}")
                }
            }
        }

        // Connection watchdog
        scope.launch {
            delay(10_000L)
            if (sessionState.value is TogetherSessionState.Joining) {
                joinJob.cancel()
                sessionState.value = TogetherSessionState.Error("Connection timed out. Please check the host's IP and port.")
            }
        }
    }

    private suspend fun syncPlayerToState(state: TogetherRoomState) = withContext(Dispatchers.Main) {
        val currentTrack = state.queue.getOrNull(state.currentIndex) ?: return@withContext

        val myTrackId = player.currentMediaItem?.mediaId
        if (myTrackId != currentTrack.id && currentTrack.id.isNotEmpty()) {
            isSyncing = true
            val customMetadata = com.cgens67.avidtune.models.MediaMetadata(
                id = currentTrack.id,
                title = currentTrack.title,
                artists = currentTrack.artists.map { com.cgens67.avidtune.models.MediaMetadata.Artist(id = null, name = it) },
                duration = currentTrack.durationSec,
                thumbnailUrl = currentTrack.thumbnailUrl,
                album = null,
                explicit = false,
                liked = false
            )
            
            val mediaItem = MediaItem.Builder()
                .setMediaId(currentTrack.id)
                .setUri(currentTrack.id)
                .setCustomCacheKey(currentTrack.id)
                .setTag(customMetadata)
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(currentTrack.title)
                        .setArtist(currentTrack.artists.joinToString(", "))
                        .setArtworkUri(currentTrack.thumbnailUrl?.let { android.net.Uri.parse(it) })
                        .setMediaType(androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC)
                        .build()
                )
                .build()
            player.setMediaItem(mediaItem)
            player.prepare()
            isSyncing = false
        }

        if (state.isPlaying && !player.isPlaying) {
            isSyncing = true
            player.play()
            isSyncing = false
        } else if (!state.isPlaying && player.isPlaying) {
            isSyncing = true
            player.pause()
            isSyncing = false
        }

        val expectedPosition = if (state.isPlaying && state.sentAtMs > 0) {
            state.positionMs + (System.currentTimeMillis() - state.sentAtMs)
        } else {
            state.positionMs
        }

        if (kotlin.math.abs(player.currentPosition - expectedPosition) > 2000L) {
            isSyncing = true
            player.seekTo(expectedPosition)
            isSyncing = false
        }
    }

    fun leaveTogether() {
        broadcastJob?.cancel()
        discoveryJob?.cancel()
        discoverySocket?.close()
        val engineToStop = serverEngine
        serverEngine = null
        val sessionToClose = clientSession
        clientSession = null
        
        sessionState.value = TogetherSessionState.Idle
        
        scope.launch(Dispatchers.IO) {
            try {
                engineToStop?.stop(100, 500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                sessionToClose?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateSettings(s: TogetherRoomSettings) { roomSettings = s }

    fun kickParticipant(pId: String) {
        if (!isHost) return
        scope.launch {
            val conn = hostConnections.remove(pId)
            hostParticipants.remove(pId)
            try {
                conn?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), HostCommand("KICK")))
                conn?.close()
            } catch(e: Exception) {}
            broadcastRoomState()
        }
    }

    fun banParticipant(pId: String) {
        if (!isHost) return
        kickParticipant(pId)
    }

    fun approveParticipant(pId: String, approved: Boolean) {
        if (!isHost) return
        scope.launch {
            if (approved) {
                val p = hostParticipants[pId]
                if (p != null) {
                    hostParticipants[pId] = p.copy(isPending = false)
                    val conn = hostConnections[pId]
                    try {
                        val sId = (sessionState.value as? TogetherSessionState.Hosting)?.sessionId ?: ""
                        conn?.send(TogetherJson.json.encodeToString(TogetherMessage.serializer(), JoinDecision(sId, pId, true)))
                    } catch(e: Exception) {}
                    broadcastRoomState()
                }
            } else {
                kickParticipant(pId)
            }
        }
    }

    private fun getIpAddress(): String? = java.net.NetworkInterface.getNetworkInterfaces().toList()
        .flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }?.hostAddress
}

// --- UI SCREEN ---

@Composable
private fun ParticipantAvatar(
    participant: TogetherParticipant,
    modifier: Modifier = Modifier,
    color: Color,
    textColor: Color,
    textStyle: androidx.compose.ui.text.TextStyle
) {
    if (participant.avatar != null) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(participant.avatar)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
            success = { SubcomposeAsyncImageContent() },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize().background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.name.take(1).uppercase(),
                        style = textStyle,
                        color = textColor
                    )
                }
            },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = participant.name.take(1).uppercase(),
                        style = textStyle,
                        color = textColor
                    )
                }
            }
        )
    } else {
        Box(
            modifier = modifier.clip(CircleShape).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = participant.name.take(1).uppercase(),
                style = textStyle,
                color = textColor
            )
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicTogetherScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    val handleBack: () -> Unit = {
        if (isVisible) {
            isVisible = false
            coroutineScope.launch {
                delay(350) // Match exit animation duration
                onBack()
            }
        }
    }

    BackHandler(enabled = isVisible) {
        handleBack()
    }

    val (welcomeShown, setWelcomeShown) = rememberPreference(TogetherWelcomeShownKey, false)
    var welcomeDismissedThisSession by rememberSaveable { mutableStateOf(false) }
    val showWelcome = !welcomeShown && !welcomeDismissedThisSession

    if (showWelcome) {
        WelcomeDialog(
            onGotIt = { dontShowAgain ->
                welcomeDismissedThisSession = true
                if (dontShowAgain) setWelcomeShown(true)
            },
            onDismiss = { welcomeDismissedThisSession = true },
        )
    }

    val (displayName, setDisplayName) = rememberPreference(
        TogetherDisplayNameKey,
        defaultValue = Build.MODEL?.takeIf { it.isNotBlank() } ?: context.getString(R.string.app_name),
    )
    
    val avatarManager = remember { AvatarPreferenceManager(context) }
    val currentAvatarSelection by avatarManager.getAvatarSelection.collectAsState(initial = AvatarSelection.Default)
    val currentAvatar = AvatarUtils.getAvatarSource(currentAvatarSelection)

    val (port, setPort) = rememberPreference(TogetherDefaultPortKey, defaultValue = 42117)
    val (allowAddTracks, setAllowAddTracksRaw) = rememberPreference(TogetherAllowGuestsToAddTracksKey, defaultValue = true)
    val (allowControlPlayback, setAllowControlPlaybackRaw) = rememberPreference(TogetherAllowGuestsToControlPlaybackKey, defaultValue = false)
    val (requireApproval, setRequireApprovalRaw) = rememberPreference(TogetherRequireHostApprovalToJoinKey, defaultValue = false)
    val (lastJoinLink, setLastJoinLink) = rememberPreference(TogetherLastJoinLinkKey, defaultValue = "")

    val sessionStateFlow = remember(playerConnection) { playerConnection?.service?.togetherSessionState ?: MutableStateFlow(TogetherSessionState.Idle) }
    val sessionState by sessionStateFlow.collectAsState()

    val isHosting = sessionState is TogetherSessionState.Hosting
    val isJoining = sessionState is TogetherSessionState.Joining
    val isIdle = sessionState is TogetherSessionState.Idle || sessionState is TogetherSessionState.Error
    val isHostRole = when (val state = sessionState) {
        is TogetherSessionState.Hosting -> true
        is TogetherSessionState.Joined  -> state.role is TogetherRole.Host
        else -> false
    }
    val isCreatingSessionLoading = (sessionState as? TogetherSessionState.Hosting)?.roomState == null && isHosting
    val isJoinedAsGuest = (sessionState as? TogetherSessionState.Joined)?.role is TogetherRole.Guest
    val isWaitingApproval = run {
        val joined = sessionState as? TogetherSessionState.Joined ?: return@run false
        joined.role is TogetherRole.Guest && joined.roomState.participants.firstOrNull { it.id == joined.selfParticipantId }?.isPending == true
    }
    val isJoinedAsAcceptedGuest = isJoinedAsGuest && !isWaitingApproval
    val disableJoinUi = isHostRole || isCreatingSessionLoading || isJoinedAsGuest

    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var showPortDialog by rememberSaveable { mutableStateOf(false) }
    var showJoinDialog by rememberSaveable { mutableStateOf(false) }

    val hostingLan = sessionState as? TogetherSessionState.Hosting
    val lanParticipants = hostingLan?.roomState?.participants.orEmpty()
    var confirmKickParticipantId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmBanParticipantId  by rememberSaveable { mutableStateOf<String?>(null) }
    val confirmKickName = lanParticipants.firstOrNull { it.id == confirmKickParticipantId }?.name
    val confirmBanName  = lanParticipants.firstOrNull { it.id == confirmBanParticipantId  }?.name

    var discoveredSessions by remember { mutableStateOf<List<DiscoveredSession>>(emptyList()) }

    LaunchedEffect(isIdle) {
        if (isIdle) {
            while(isActive) {
                val sessions = playerConnection?.service?.togetherManager?.discoverSessions() ?: emptyList()
                discoveredSessions = sessions
                delay(5000)
            }
        } else {
            discoveredSessions = emptyList()
        }
    }

    LaunchedEffect(disableJoinUi, isJoining, isHosting) {
        if (disableJoinUi || isJoining || isHosting) showJoinDialog = false
    }

    fun pushSettingsToActiveSession(addTracks: Boolean = allowAddTracks, controlPlayback: Boolean = allowControlPlayback, approval: Boolean = requireApproval) {
        if (isHosting) {
            playerConnection?.service?.updateTogetherSettings(TogetherRoomSettings(allowGuestsToAddTracks = addTracks, allowGuestsToControlPlayback = controlPlayback, requireHostApprovalToJoin = approval))
        }
    }

    val setAllowAddTracks: (Boolean) -> Unit = { v -> setAllowAddTracksRaw(v); pushSettingsToActiveSession(addTracks = v) }
    val setAllowControlPlayback: (Boolean) -> Unit = { v -> setAllowControlPlaybackRaw(v); pushSettingsToActiveSession(controlPlayback = v) }
    val setRequireApproval: (Boolean) -> Unit = { v -> setRequireApprovalRaw(v); pushSettingsToActiveSession(approval = v) }

    if (showNameDialog) {
        com.cgens67.avidtune.ui.component.TextFieldDialog(title = { Text(stringResource(R.string.together_display_name)) }, placeholder = { Text(stringResource(R.string.together_display_name_placeholder)) }, isInputValid = { it.trim().isNotBlank() }, onDone = { setDisplayName(it.trim()) }, onDismiss = { showNameDialog = false })
    }

    if (showPortDialog) {
        com.cgens67.avidtune.ui.component.TextFieldDialog(title = { Text(stringResource(R.string.together_port)) }, placeholder = { Text("42117") }, isInputValid = { it.trim().toIntOrNull() in 1..65535 }, onDone = { setPort(it.trim().toInt()) }, onDismiss = { showPortDialog = false })
    }

    var joinInput by rememberSaveable { mutableStateOf(lastJoinLink) }
    val canJoin = remember(joinInput) { joinInput.trim().isNotEmpty() }

    if (showJoinDialog) {
        CustomJoinDialog(
            initialValue = joinInput,
            onDismiss = { showJoinDialog = false },
            onJoin = { raw -> 
                val trimmed = raw.trim()
                joinInput = trimmed
                setLastJoinLink(trimmed)
                playerConnection?.service?.joinTogether(trimmed, displayName, currentAvatar)
                showJoinDialog = false 
            }
        )
    }

    if (confirmKickParticipantId != null) {
        AlertDialog(
            onDismissRequest = { confirmKickParticipantId = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.together_kick)) },
            text = { Text(stringResource(R.string.together_kick_confirm, confirmKickName ?: stringResource(R.string.unknown)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { Button(onClick = { val pid = confirmKickParticipantId ?: return@Button; confirmKickParticipantId = null; playerConnection?.service?.kickTogetherParticipant(pid) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.together_kick), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { confirmKickParticipantId = null }, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.dismiss)) } },
        )
    }

    if (confirmBanParticipantId != null) {
        AlertDialog(
            onDismissRequest = { confirmBanParticipantId = null },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.together_ban)) },
            text = { Text(stringResource(R.string.together_ban_confirm, confirmBanName ?: stringResource(R.string.unknown)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = { Button(onClick = { val pid = confirmBanParticipantId ?: return@Button; confirmBanParticipantId = null; playerConnection?.service?.banTogetherParticipant(pid) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.together_ban), fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { confirmBanParticipantId = null }, shape = RoundedCornerShape(16.dp)) { Text(stringResource(R.string.dismiss)) } },
        )
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300, easing = LinearEasing)),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(200, easing = LinearEasing)),
        modifier = Modifier.fillMaxSize().zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))

                StatusCard(state = sessionState, onCopyLink = { link -> clipboard.setText(AnnotatedString(link)); haptic.performHapticFeedback(HapticFeedbackType.LongPress); Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show() }, onShareLink = { link -> context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, link) }, null)) }, onLeave = { playerConnection?.service?.leaveTogether() }, modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 12.dp))

                if (hostingLan?.roomState != null && isHostRole) {
                    OnlineParticipantsCard(participants = hostingLan.roomState.participants, hostApprovalEnabled = hostingLan.settings.requireHostApprovalToJoin, onApprove = { pid, approved -> playerConnection?.service?.approveTogetherParticipant(pid, approved) }, onKick = { confirmKickParticipantId = it }, onBan  = { confirmBanParticipantId  = it }, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
                }

                if (!isJoinedAsGuest && !isHostRole) {
                    AnimatedVisibility(
                        visible = discoveredSessions.isNotEmpty(),
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
                    ) {
                        OngoingSessionsCard(
                            sessions = discoveredSessions,
                            onJoin = { session ->
                                val link = TogetherLink.encode(session.joinInfo)
                                joinInput = link
                                setLastJoinLink(link)
                                playerConnection?.service?.joinTogether(link, displayName, currentAvatar)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
                        )
                    }

                    HostSectionCard(displayName = displayName, port = port, allowAddTracks = allowAddTracks, allowControlPlayback = allowControlPlayback, requireApproval = requireApproval, onShowNameDialog = { showNameDialog = true }, onShowPortDialog = { showPortDialog = true }, onAllowAddTracksChange = setAllowAddTracks, onAllowControlPlaybackChange = setAllowControlPlayback, onRequireApprovalChange = setRequireApproval, isStartEnabled = !isCreatingSessionLoading && !isJoining && !isHosting && sessionState !is TogetherSessionState.Joined, isLoading = isCreatingSessionLoading, onStartSession = { playerConnection?.service?.startTogetherHost(port = port, displayName = displayName, settings = TogetherRoomSettings(allowGuestsToAddTracks = allowAddTracks, allowGuestsToControlPlayback = allowControlPlayback, requireHostApprovalToJoin = requireApproval), avatar = currentAvatar) }, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp))
                }

                if (!isHostRole) {
                    JoinSectionCard(joinInput = joinInput, onJoinInputChange = { joinInput = it }, canJoin = canJoin, disableJoinUi = disableJoinUi, isJoined = isJoinedAsAcceptedGuest, isWaitingApproval = isWaitingApproval, isJoining = isJoining, onShowJoinDialog = { showJoinDialog = true }, onPasteFromClipboard = { val text = clipboard.getText()?.text?.trim() ?: ""; if (text.isNotBlank()) { joinInput = text; haptic.performHapticFeedback(HapticFeedbackType.LongPress); setLastJoinLink(text); playerConnection?.service?.joinTogether(text, displayName, currentAvatar) } }, onJoin = { val trimmed = joinInput.trim(); setLastJoinLink(trimmed); playerConnection?.service?.joinTogether(trimmed, displayName, currentAvatar) }, modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp))
                }
            }

            TopAppBar(
                title = { Text(stringResource(R.string.music_together)) },
                navigationIcon = {
                    AtIconButton(onClick = handleBack, onLongClick = { handleBack(); navController.backToMain() }) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomJoinDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var isPinMode by remember { mutableStateOf(initialValue.length <= 6 && initialValue.all { it.isDigit() }) }
    var pinInput by remember { mutableStateOf(if (isPinMode) initialValue else "") }
    var linkInput by remember { mutableStateOf(if (!isPinMode) initialValue else "") }
    
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(if (isPinMode) R.string.together_join_via_pin else R.string.together_join_via_link),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = if (isPinMode) 0 else 1,
                    containerColor = Color.Transparent,
                    divider = { },
                    indicator = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    val pinBg = if (isPinMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    val linkBg = if (!isPinMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    
                    Tab(
                        selected = isPinMode,
                        onClick = { isPinMode = true },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(pinBg),
                        text = { Text(stringResource(R.string.together_pin), color = if (isPinMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                    Tab(
                        selected = !isPinMode,
                        onClick = { isPinMode = false },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(linkBg),
                        text = { Text(stringResource(R.string.together_link), color = if (!isPinMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) }
                    )
                }

                if (isPinMode) {
                    BasicTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (pinInput.length == 6) onJoin(pinInput) }),
                        modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                        decorationBox = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                                repeat(6) { index ->
                                    val char = pinInput.getOrNull(index)?.toString() ?: ""
                                    val isFocused = pinInput.length == index || (pinInput.length == 6 && index == 5)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.8f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = 2.dp,
                                                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = char,
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = linkInput,
                        onValueChange = { linkInput = it },
                        placeholder = { Text(stringResource(R.string.together_paste_link_placeholder)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (linkInput.isNotBlank()) onJoin(linkInput) }),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(if (isPinMode) pinInput else linkInput) },
                enabled = if (isPinMode) pinInput.length == 6 else linkInput.isNotBlank()
            ) {
                Text(stringResource(R.string.join))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
    
    LaunchedEffect(isPinMode) {
        delay(100)
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun OngoingSessionsCard(
    sessions: List<DiscoveredSession>,
    onJoin: (DiscoveredSession) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.wifi_proxy), contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.together_ongoing_sessions_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.together_ongoing_sessions_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            sessions.forEach { session ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onJoin(session) }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                            Text(session.hostName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.hostName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.together_local_network), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FilledTonalButton(onClick = { onJoin(session) }, contentPadding = PaddingValues(horizontal = 16.dp)) {
                            Text(stringResource(R.string.join), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HostSectionCard(displayName: String, port: Int, allowAddTracks: Boolean, allowControlPlayback: Boolean, requireApproval: Boolean, onShowNameDialog: () -> Unit, onShowPortDialog: () -> Unit, onAllowAddTracksChange: (Boolean) -> Unit, onAllowControlPlaybackChange: (Boolean) -> Unit, onRequireApprovalChange: (Boolean) -> Unit, isStartEnabled: Boolean, isLoading: Boolean, onStartSession: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f), MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.auto_awesome), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.together_host_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(stringResource(R.string.together_lan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.wifi_proxy), contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer); Text(stringResource(R.string.together_lan_badge), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer) } }
            }
            SettingsItemRow(icon = R.drawable.person, title = stringResource(R.string.together_display_name), subtitle = displayName, onClick = onShowNameDialog)
            SettingsDivider()
            SettingsItemRow(icon = R.drawable.link, title = stringResource(R.string.together_port), subtitle = port.toString(), onClick = onShowPortDialog)
            SettingsDivider()
            ToggleRow(icon = R.drawable.playlist_add, title = stringResource(R.string.together_allow_guests_add), checked = allowAddTracks, onCheckedChange = onAllowAddTracksChange)
            SettingsDivider()
            ToggleRow(icon = R.drawable.play, title = stringResource(R.string.together_allow_guests_control), checked = allowControlPlayback, onCheckedChange = onAllowControlPlaybackChange)
            SettingsDivider()
            ToggleRow(icon = R.drawable.lock, title = stringResource(R.string.together_require_approval), checked = requireApproval, onCheckedChange = onRequireApprovalChange)
            Spacer(Modifier.height(8.dp))
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "scale")
            Button(enabled = isStartEnabled, onClick = onStartSession, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 10.dp).scale(scale), shape = RoundedCornerShape(18.dp), interactionSource = interactionSource, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.loading), fontWeight = FontWeight.SemiBold) } else { Icon(painterResource(R.drawable.auto_awesome), null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.start_session), fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun JoinSectionCard(joinInput: String, onJoinInputChange: (String) -> Unit, canJoin: Boolean, disableJoinUi: Boolean, isJoined: Boolean, isWaitingApproval: Boolean, isJoining: Boolean, onShowJoinDialog: () -> Unit, onPasteFromClipboard: () -> Unit, onJoin: () -> Unit, modifier: Modifier = Modifier) {
    val parsedLink = remember(joinInput) { TogetherLink.decode(joinInput.trim()) }
    Card(modifier = modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f), MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.person), contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp)) }
                Column(modifier = Modifier.weight(1f)) { Text(stringResource(R.string.together_join_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(stringResource(R.string.join_session), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), modifier = Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(enabled = !disableJoinUi && !isJoining && !isJoined && !isWaitingApproval, onClick = onShowJoinDialog)) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painterResource(R.drawable.link), contentDescription = null, tint = if (canJoin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                        Text(text = if (joinInput.isBlank()) stringResource(R.string.together_enter_pin_or_link) else joinInput.trim(), style = MaterialTheme.typography.bodySmall.copy(fontFamily = if (joinInput.isNotBlank()) FontFamily.Monospace else FontFamily.Default), color = if (joinInput.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (joinInput.isNotBlank() && !disableJoinUi) { Icon(painterResource(R.drawable.close), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp).clip(CircleShape).clickable { onJoinInputChange("") }) }
                    }
                }
                if (!disableJoinUi && !isJoining && !isJoined && !isWaitingApproval) {
                    FilledTonalButton(onClick = onPasteFromClipboard, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) { Icon(painterResource(R.drawable.content_copy), contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.paste), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold) }
                }
            }
            AnimatedVisibility(visible = parsedLink != null && !isJoined && !isWaitingApproval, enter = fadeIn(tween(200)) + expandVertically(tween(250)), exit  = fadeOut(tween(150)) + shrinkVertically(tween(200))) {
                if (parsedLink != null) { Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(top = 4.dp)) { Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) { Text(text = "${parsedLink.host}:${parsedLink.port}", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }; Text(text = stringResource(R.string.together_session_id_format, parsedLink.sessionId.take(8)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.weight(1f)); Icon(painterResource(R.drawable.check), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) } } }
            }
            Spacer(Modifier.height(10.dp))
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "joinBtnScale")
            FilledTonalButton(enabled = canJoin && !disableJoinUi && !isJoining && !isJoined && !isWaitingApproval, onClick = onJoin, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 10.dp).scale(scale), shape = RoundedCornerShape(18.dp), interactionSource = interactionSource) {
                when {
                    isJoining -> { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.connecting), fontWeight = FontWeight.SemiBold) }
                    isWaitingApproval -> { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSecondaryContainer); Spacer(Modifier.width(10.dp)); Text(stringResource(R.string.together_waiting_approval), fontWeight = FontWeight.SemiBold) }
                    isJoined -> { Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.joined), fontWeight = FontWeight.SemiBold) }
                    else -> { Icon(painterResource(R.drawable.play), null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.join), fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: TogetherSessionState, onCopyLink: (String) -> Unit, onShareLink: (String) -> Unit, onLeave: () -> Unit, modifier: Modifier = Modifier) {
    val isActive = state !is TogetherSessionState.Idle
    val isError  = state is TogetherSessionState.Error
    val isWaitingApproval = run { val joined = state as? TogetherSessionState.Joined ?: return@run false; joined.role is TogetherRole.Guest && joined.roomState.participants.firstOrNull { it.id == joined.selfParticipantId }?.isPending == true }
    val statusColor = when { isError  -> MaterialTheme.colorScheme.error; isActive -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.onSurfaceVariant }
    Card(modifier = modifier.fillMaxWidth().animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(if (isActive && !isError) listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceContainerLow) else if (isError) listOf(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), MaterialTheme.colorScheme.surfaceContainerLow) else listOf(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.surfaceContainerLow))).padding(18.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(listOf(statusColor.copy(alpha = 0.18f), statusColor.copy(alpha = 0.08f)))), contentAlignment = Alignment.Center) { Icon(painterResource(if (isError) R.drawable.error else R.drawable.auto_awesome), contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp)) }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(R.string.together_status), style = MaterialTheme.typography.labelLarge, color = statusColor); if (isActive && !isError) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) } }
                        Text(text = when (state) { TogetherSessionState.Idle -> stringResource(R.string.together_idle); is TogetherSessionState.Hosting -> stringResource(R.string.together_hosting); is TogetherSessionState.Joining -> stringResource(R.string.together_joining); is TogetherSessionState.Joined -> if (isWaitingApproval) stringResource(R.string.together_waiting_approval) else stringResource(R.string.together_connected); is TogetherSessionState.Error -> stringResource(R.string.together_error_state); else -> "" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        
                        // Add track playing indicator inside the status card if active and not pending
                        if (isActive && !isError && !isWaitingApproval) {
                            val trackTitle = when (state) {
                                is TogetherSessionState.Hosting -> state.roomState?.queue?.getOrNull(state.roomState.currentIndex)?.title
                                is TogetherSessionState.Joined -> state.roomState.queue.getOrNull(state.roomState.currentIndex)?.title
                                else -> null
                            }
                            if (!trackTitle.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(R.drawable.music_note), null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text(trackTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    if (isActive) { FilledTonalButton(onClick = onLeave, shape = RoundedCornerShape(14.dp)) { Icon(painterResource(R.drawable.arrow_back), null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.leave), fontWeight = FontWeight.SemiBold) } }
                }
                when (state) {
                    is TogetherSessionState.Hosting -> { LanSessionLinkCard(link = state.joinLink, pin = state.pin, localAddressHint = state.localAddressHint, port = state.port, onCopy  = { onCopyLink(state.joinLink) }, onShare = { onShareLink(state.joinLink) }) }
                    is TogetherSessionState.Joined -> { if (!isWaitingApproval) { ParticipantsCard(participants = state.roomState.participants) } }
                    is TogetherSessionState.Error -> { Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) { Text(text = state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(14.dp)) } }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun LanSessionLinkCard(link: String, pin: String, localAddressHint: String?, port: Int, onCopy: () -> Unit, onShare: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.together_join_with_pin), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(pin, style = MaterialTheme.typography.displayMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 8.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            
            if (localAddressHint != null) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer) { Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(painterResource(R.drawable.wifi_proxy), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(13.dp)); Text(text = "$localAddressHint:$port", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) } }; Text(stringResource(R.string.session_link), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) } }
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f), modifier = Modifier.fillMaxWidth()) { Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp)) { Text(text = link, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.primary, maxLines = 2) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = onCopy, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.content_copy), contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.copy_link), fontWeight = FontWeight.SemiBold) }; FilledTonalButton(onClick = onShare, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.share), fontWeight = FontWeight.SemiBold) } }
        }
    }
}

@Composable
private fun OnlineParticipantsCard(
    participants: List<TogetherParticipant>,
    hostApprovalEnabled: Boolean,
    onApprove: (String, Boolean) -> Unit,
    onKick: (String) -> Unit,
    onBan: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.together_participants),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.together_connected_count, participants.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            participants.forEachIndexed { index, participant ->
                key(participant.id) {
                    val accent = if (participant.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    
                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Avatar
                            ParticipantAvatar(
                                participant = participant,
                                modifier = Modifier.size(48.dp),
                                color = accent.copy(alpha = 0.15f),
                                textColor = accent,
                                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            // Name & Role
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    participant.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        participant.isHost -> stringResource(R.string.together_role_host)
                                        participant.isPending && hostApprovalEnabled -> stringResource(R.string.together_pending_approval)
                                        else -> stringResource(R.string.together_role_guest)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Actions
                            if (!participant.isHost) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (participant.isPending && hostApprovalEnabled) {
                                        Surface(
                                            onClick = { onApprove(participant.id, true) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        }
                                        Surface(
                                            onClick = { onApprove(participant.id, false) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.close), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    } else {
                                        Surface(
                                            onClick = { onKick(participant.id) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.remove), null, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Surface(
                                            onClick = { onBan(participant.id) },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(painterResource(R.drawable.close), null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantsCard(participants: List<TogetherParticipant>) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.person),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    stringResource(R.string.together_participants),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                participants.forEach { participant ->
                    val isHost = participant.isHost
                    val bgColor = if (isHost) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    val contentColor = if (isHost) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = bgColor,
                        contentColor = contentColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParticipantAvatar(
                                participant = participant,
                                modifier = Modifier.size(24.dp),
                                color = contentColor.copy(alpha = 0.2f),
                                textColor = contentColor,
                                textStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = participant.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isHost) {
                                Icon(
                                    painterResource(R.drawable.auto_awesome),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = contentColor
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
private fun WelcomeDialog(onGotIt: (Boolean) -> Unit, onDismiss: () -> Unit) {
    var dontShowAgain by rememberSaveable { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss, 
        shape = RoundedCornerShape(28.dp), 
        containerColor = MaterialTheme.colorScheme.surface, 
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { 
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))), contentAlignment = Alignment.Center) { 
                    Icon(painterResource(R.drawable.auto_awesome), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) 
                } 
                Text(stringResource(R.string.together_welcome_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) 
            } 
        }, 
        text = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                Text(stringResource(R.string.together_welcome_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), modifier = Modifier.fillMaxWidth()) { 
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { 
                        InstructionRow(R.drawable.auto_awesome, MaterialTheme.colorScheme.primary, stringResource(R.string.together_welcome_host_title), stringResource(R.string.together_welcome_host_body)) 
                        InstructionRow(R.drawable.link, MaterialTheme.colorScheme.tertiary, stringResource(R.string.together_welcome_join_title), stringResource(R.string.together_welcome_join_body)) 
                        InstructionRow(R.drawable.lock, MaterialTheme.colorScheme.secondary, stringResource(R.string.together_welcome_permissions_title), stringResource(R.string.together_welcome_permissions_body)) 
                    } 
                } 
                CheckboxRow(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it }, label = stringResource(R.string.together_dont_show_again)) 
            } 
        }, 
        confirmButton = { 
            Button(onClick = { onGotIt(dontShowAgain) }, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { 
                Icon(painterResource(R.drawable.check), null, modifier = Modifier.size(18.dp)) 
                Spacer(Modifier.width(8.dp)) 
                Text(stringResource(R.string.got_it), fontWeight = FontWeight.SemiBold) 
            } 
        }
    )
}

@Composable
private fun SettingsItemRow(
    icon: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun ToggleRow(
    icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun InstructionRow(
    icon: Int,
    tint: Color,
    title: String,
    body: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CheckboxRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
