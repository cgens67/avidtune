package com.cgens67.avidtune.discord

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

private const val TAG = "GatewayClient"

object GatewayOp {
    const val DISPATCH = 0
    const val HEARTBEAT = 1
    const val IDENTIFY = 2
    const val PRESENCE_UPDATE = 3
    const val VOICE_STATE_UPDATE = 4
    const val RESUME = 6
    const val RECONNECT = 7
    const val INVALID_SESSION = 9
    const val HELLO = 10
    const val HEARTBEAT_ACK = 11
}

val NON_RESUMABLE_CLOSE_CODES: Set<Int> = setOf(4004, 4010, 4011, 4012, 4013, 4014)

object GatewayDefaults {
    const val API_BASE = "https://discord.com/api"
    const val GATEWAY_URL = "wss://gateway.discord.gg"
    const val GATEWAY_VERSION = 9
    const val USER_AGENT = "Discord Embedded/1.9.15780"
    const val HELLO_TIMEOUT_MS = 20_000L
}

object ActivityTypes {
    private val keys = listOf("PLAYING", "STREAMING", "LISTENING", "WATCHING", "CUSTOM", "COMPETING", "HANG")
    private val forward = keys.filter { it.isNotEmpty() }.mapIndexed { i, k -> k to i }.toMap()
    private val reverse = forward.entries.associate { (k, v) -> v to k }

    fun fromString(name: String): Int? = forward[name]
    fun fromInt(value: Int): String? = reverse[value]
}

object IntentsFlags {
    val FLAGS: Map<String, Int> = mapOf(
        "DIRECT_MESSAGES" to (1 shl 12),
        "PRIVATE_CHANNELS" to (1 shl 18),
        "CALLS" to (1 shl 19),
        "USER_RELATIONSHIPS" to (1 shl 22),
        "USER_PRESENCE" to (1 shl 23),
        "LOBBIES" to (1 shl 27),
        "LOBBY_DELETE" to (1 shl 28),
        "UNKNOWN_29" to (1 shl 29),
    )
}

object GatewayCapabilitiesFlags {
    val FLAGS: Map<String, Int> = mapOf(
        "DEDUPE_USER_OBJECTS" to (1 shl 4),
        "PRIORITIZED_READY_PAYLOAD" to (1 shl 5),
        "AUTO_CALL_CONNECT" to (1 shl 12),
        "AUTO_LOBBY_CONNECT" to (1 shl 16),
    )
}

data class GatewaySessionState(
    val sessionId: String,
    val seq: Int,
    val resumeGatewayUrl: String,
)

data class GatewayReadyEvent(
    val user: GatewayReadyUser,
    val sessionId: String,
    val resumeGatewayUrl: String,
) {
    companion object {
        fun fromJson(obj: JSONObject): GatewayReadyEvent {
            val userObj = obj.getJSONObject("user")
            return GatewayReadyEvent(
                user = GatewayReadyUser(
                    id = userObj.getString("id"),
                    username = userObj.getString("username"),
                    globalName = userObj.optString("global_name", null),
                ),
                sessionId = obj.getString("session_id"),
                resumeGatewayUrl = obj.getString("resume_gateway_url"),
            )
        }
    }
}

data class GatewayReadyUser(
    val id: String,
    val username: String,
    val globalName: String? = null,
)

data class GatewayCloseInfo(
    val code: Int,
    val reason: String,
    val resumable: Boolean,
    val session: GatewaySessionState?,
)

sealed class GatewayFrame {
    data class Text(val text: String) : GatewayFrame()
    data class Close(val code: Int, val reason: String) : GatewayFrame()
}

class GatewayClient {
    @Volatile private var httpClient: OkHttpClient? = null
    @Volatile private var wsSession: WebSocket? = null
    @Volatile private var processingJob: Job? = null
    private var heartbeatJob: Job? = null
    private var helloTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val incomingChannel = Channel<GatewayFrame>(Channel.UNLIMITED)

    private var lastAck = true
    private var lastHeartbeatAt = 0L
    private var ping = -1

    private var sessionState: GatewaySessionState? = null
    private var liveSeq = 0
    private var token = ""

    @Volatile private var closed = false
    @Volatile private var readyReceived = false
    private val connectionGeneration = AtomicLong(0L)
    @Volatile private var activeConnectionGeneration: Long = 0L

    var onReady: ((GatewayReadyEvent) -> Unit)? = null
    var onClose: ((GatewayCloseInfo) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null
    var onDebug: ((String) -> Unit)? = null

    val latency: Int get() = ping

    fun isConnected(): Boolean = wsSession != null && !closed && processingJob?.isActive == true
    fun isReady(): Boolean = isConnected() && readyReceived

    suspend fun connect(accessToken: String) {
        if (wsSession != null) throw IllegalStateException("GatewayClient already connected")

        token = "Bearer $accessToken"
        sessionState = null
        liveSeq = 0
        closed = false
        readyReceived = false
        lastAck = true

        val url = "${GatewayDefaults.GATEWAY_URL}/?v=${GatewayDefaults.GATEWAY_VERSION}&encoding=json"
        val ready = CompletableDeferred<Unit>()

        debug("connecting $url")

        httpClient = OkHttpClient.Builder().build()

        val generation = connectionGeneration.incrementAndGet()
        activeConnectionGeneration = generation

        val request = Request.Builder().url(url).build()

        wsSession = httpClient!!.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    response.close()
                    if (!isActiveGeneration(generation)) {
                        webSocket.close(1000, "stale")
                        return
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    if (!isActiveGeneration(generation)) return
                    publishFrame(GatewayFrame.Text(text))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isActiveGeneration(generation)) return
                    publishFrame(GatewayFrame.Close(code, reason))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (!isActiveGeneration(generation)) return
                    publishFrame(GatewayFrame.Close(code, reason))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (!isActiveGeneration(generation)) return
                    response?.close()
                    onError?.invoke(t)
                    publishFrame(GatewayFrame.Close(response?.code ?: 4000, t.message ?: "failure"))
                }
            },
        )

        helloTimerJob = scope.launch {
            delay(GatewayDefaults.HELLO_TIMEOUT_MS)
            debug("HELLO timeout")
            forceClose(4009, "HELLO timeout")
            ready.completeExceptionally(Exception("HELLO timeout"))
        }

        processingJob = scope.launch {
            try {
                for (frame in incomingChannel) {
                    when (frame) {
                        is GatewayFrame.Text -> {
                            handleMessage(frame.text, ready)
                        }
                        is GatewayFrame.Close -> {
                            handleClose(frame.reason, frame.code)
                            if (!ready.isCompleted) {
                                ready.completeExceptionally(Exception("Gateway closed before ready"))
                            }
                            return@launch
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!ready.isCompleted) ready.completeExceptionally(e)
                onError?.invoke(e)
            }
        }

        ready.await()
    }

    fun sendPresenceUpdate(presenceJson: JSONObject): Boolean = sendFrame(GatewayOp.PRESENCE_UPDATE, presenceJson, requireReady = true)

    fun disconnect() {
        shutdownTransport(closeCode = 1000, closeReason = "Client disconnect", cancelProcessing = true)
    }

    private fun send(op: Int, d: Any?): Boolean = sendFrame(op, d, requireReady = false)

    private fun sendFrame(op: Int, d: Any?, requireReady: Boolean): Boolean {
        if (closed || wsSession == null || processingJob?.isActive != true) {
            debug("sendFrame skipped: not connected op=$op")
            return false
        }
        if (requireReady && !readyReceived) {
            debug("sendFrame skipped: gateway not ready op=$op")
            return false
        }

        val session = wsSession ?: return false
        return try {
            val sent = session.send(buildJsonString(op, d))
            if (!sent) {
                debug("sendFrame failed: WebSocket.send returned false op=$op")
            }
            sent
        } catch (e: Exception) {
            onError?.invoke(e)
            false
        }
    }

    private fun buildJsonString(op: Int, d: Any?): String {
        val json = JSONObject()
        json.put("op", op)
        when (d) {
            is JSONObject -> json.put("d", d)
            is JSONArray -> json.put("d", d)
            is Map<*, *> -> json.put("d", JSONObject(d as Map<*, *>))
            is Int -> json.put("d", d)
            is String -> json.put("d", d)
            is Boolean -> json.put("d", d)
            null -> json.put("d", JSONObject.NULL)
            else -> json.put("d", d.toString())
        }
        return json.toString()
    }

    private fun handleMessage(raw: String, ready: CompletableDeferred<Unit>) {
        try {
            val json = JSONObject(raw)
            val op = json.getInt("op")
            val d = json.opt("d")
            val s = if (json.has("s") && !json.isNull("s")) json.getInt("s") else null
            val t = json.optString("t", null)

            if (s != null && s > liveSeq) {
                liveSeq = s
                touchSession(seq = s)
            }

            when (op) {
                GatewayOp.HELLO -> {
                    helloTimerJob?.cancel()
                    helloTimerJob = null
                    val dObj = d as JSONObject
                    val interval = dObj.getInt("heartbeat_interval")
                    startHeartbeat(interval.toLong())
                    debug("HELLO received, heartbeat_interval=${interval}ms")
                    sendIdentify()
                }
                GatewayOp.HEARTBEAT_ACK -> {
                    lastAck = true
                    ping = (System.currentTimeMillis() - lastHeartbeatAt).toInt()
                    debug("heartbeat ack (${ping}ms)")
                }
                GatewayOp.HEARTBEAT -> {
                    debug("received server heartbeat")
                    sendHeartbeat(force = true)
                }
                GatewayOp.RECONNECT -> {
                    debug("server requested RECONNECT")
                    forceClose(4000, "server reconnect")
                }
                GatewayOp.INVALID_SESSION -> {
                    val resumable = if (d is Boolean) d else false
                    debug("INVALID_SESSION resumable=$resumable")
                    if (!resumable) {
                        sessionState = null
                        touchSession(sessionId = null, resumeGatewayUrl = null, seq = 0)
                    }
                    forceClose(if (resumable) 4000 else 1000, "invalid session")
                }
                GatewayOp.DISPATCH -> {
                    handleDispatch(t ?: "", d, s, ready)
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e)
        }
    }

    private fun handleDispatch(t: String, d: Any?, s: Int?, ready: CompletableDeferred<Unit>) {
        when (t) {
            "READY" -> {
                val obj = d as JSONObject
                val re = GatewayReadyEvent.fromJson(obj)
                debug("READY: user=${re.user.username} (${re.user.id}) session=${re.sessionId}")
                sessionState = GatewaySessionState(re.sessionId, liveSeq, re.resumeGatewayUrl)
                touchSession(re.sessionId, liveSeq, re.resumeGatewayUrl)
                readyReceived = true
                ready.complete(Unit)
                onReady?.invoke(re)
            }
            "RESUMED" -> {
                debug("RESUMED: session restored, seq=$liveSeq")
                touchSession(sessionState?.sessionId, liveSeq, sessionState?.resumeGatewayUrl)
                readyReceived = true
                ready.complete(Unit)
            }
            else -> {
                debug("dispatch $t")
            }
        }
    }

    private fun sendIdentify() {
        val capabilities = GatewayCapabilitiesFlags.FLAGS
        val intents = IntentsFlags.FLAGS

        val capsBitfield = capabilities.values.reduce { a, b -> a or b }
        val intentsBitfield = intents.values.reduce { a, b -> a or b }

        val d = JSONObject()
        d.put("capabilities", capsBitfield)
        d.put("intents", intentsBitfield)
        d.put("token", token)
        val properties = JSONObject()
        properties.put("os", "Android")
        properties.put("browser", "AvidTune")
        properties.put("device", "Android")
        properties.put("browser_user_agent", "AvidTune")
        properties.put("browser_version", "1.0")
        properties.put("client_version", "1.0")
        properties.put("client_build_number", 1)
        properties.put("native_build_number", 1)
        properties.put("release_channel", "unknown")
        d.put("properties", properties)
        debug("sending IDENTIFY")
        send(GatewayOp.IDENTIFY, d)
    }

    private fun startHeartbeat(intervalMs: Long) {
        stopHeartbeat()
        debug("heartbeat every ${intervalMs}ms")
        val firstDelay = (intervalMs * Random.nextDouble()).toLong()
        heartbeatJob = scope.launch {
            delay(firstDelay)
            if (wsSession != null) {
                sendHeartbeat()
                while (isActive) {
                    delay(intervalMs)
                    sendHeartbeat()
                }
            }
        }
    }

    private fun sendHeartbeat(force: Boolean = false) {
        if (!force && !lastAck) {
            debug("zombie connection; closing 4009")
            forceClose(4009, "heartbeat ack missed")
            return
        }
        lastAck = false
        lastHeartbeatAt = System.currentTimeMillis()
        val seq = if (liveSeq > 0) liveSeq else null
        send(GatewayOp.HEARTBEAT, seq)
        debug("heartbeat dispatched seq=$seq")
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun touchSession(sessionId: String? = null, seq: Int = liveSeq, resumeGatewayUrl: String? = null) {}

    private fun forceClose(code: Int, reason: String) {
        val session = wsSession
        if (session == null || !session.close(code, reason)) {
            publishFrame(GatewayFrame.Close(code, reason))
        }
    }

    private fun handleClose(reason: String, code: Int) {
        if (closed) return
        val fatal = NON_RESUMABLE_CLOSE_CODES.contains(code)
        val snapshot = sessionState?.copy(seq = liveSeq)
        if (fatal) sessionState = null

        shutdownTransport(closeCode = null, closeReason = null, cancelProcessing = false)

        onClose?.invoke(
            GatewayCloseInfo(
                code = code,
                reason = reason,
                resumable = !fatal && snapshot != null,
                session = snapshot,
            ),
        )
        debug("close code=$code reason=$reason resumable=${!fatal && snapshot != null}")
    }

    private fun shutdownTransport(closeCode: Int?, closeReason: String?, cancelProcessing: Boolean) {
        closed = true
        readyReceived = false
        stopHeartbeat()
        helloTimerJob?.cancel()
        helloTimerJob = null
        activeConnectionGeneration = connectionGeneration.incrementAndGet()

        val session = wsSession
        wsSession = null
        val client = httpClient
        httpClient = null

        if (cancelProcessing) {
            processingJob?.cancel()
            processingJob = null
        }

        if (closeCode != null) {
            runCatching { session?.close(closeCode, closeReason ?: "") }
        }
        runCatching { client?.dispatcher?.executorService?.shutdown() }
    }

    private fun isActiveGeneration(generation: Long): Boolean = generation == activeConnectionGeneration

    private fun publishFrame(frame: GatewayFrame) {
        val result = incomingChannel.trySend(frame)
        if (result.isFailure) {
            result.exceptionOrNull()?.let { onError?.invoke(it) }
        }
    }

    private fun debug(msg: String) {
        Timber.tag(TAG).v(msg)
        onDebug?.invoke(msg)
    }
}

data class DiscordPresenceButton(val label: String, val url: String)
data class DiscordPresenceAssets(val largeImage: String? = null, val largeText: String? = null, val largeUrl: String? = null, val smallImage: String? = null, val smallText: String? = null, val smallUrl: String? = null)
data class DiscordPresenceTimestamps(val startEpochSeconds: Long? = null, val endEpochSeconds: Long? = null)

data class DiscordPresenceActivity(
    val applicationId: Long,
    val name: String?,
    val type: DiscordActivityType,
    val details: String?,
    val state: String?,
    val detailsUrl: String? = null,
    val stateUrl: String? = null,
    val assets: DiscordPresenceAssets = DiscordPresenceAssets(),
    val buttons: List<DiscordPresenceButton> = emptyList(),
    val timestamps: DiscordPresenceTimestamps = DiscordPresenceTimestamps(),
    val statusDisplayType: DiscordStatusDisplayType = DiscordStatusDisplayType.State,
    val supportedPlatforms: Int = DiscordActivityPlatform.Android.bit,
    val onlineStatus: DiscordOnlineStatus = DiscordOnlineStatus.Online,
)

enum class DiscordActivityType(val nativeValue: Int) {
    Playing(0), Streaming(1), Listening(2), Watching(3), Competing(5);
    companion object {
        fun fromPreference(value: String): DiscordActivityType = when (value.uppercase()) {
            "PLAYING" -> Playing
            "STREAMING" -> Streaming
            "WATCHING" -> Watching
            "COMPETING" -> Competing
            else -> Listening
        }
    }
}

enum class DiscordStatusDisplayType(val nativeValue: Int) { Name(0), State(1), Details(2) }

enum class DiscordOnlineStatus(val nativeValue: Int) {
    Online(0), Idle(3), Dnd(4), Invisible(5), Streaming(6);
    companion object {
        fun fromPreference(value: String): DiscordOnlineStatus = when (value.lowercase()) {
            "idle" -> Idle
            "dnd" -> Dnd
            "invisible" -> Invisible
            "streaming" -> Streaming
            else -> Online
        }
    }
}

enum class DiscordActivityPlatform(val bit: Int, val wireValue: String) {
    Desktop(1, "desktop"), Xbox(2, "xbox"), Samsung(4, "samsung"), Ios(8, "ios"), Android(16, "android"), Embedded(32, "embedded"), Ps4(64, "ps4"), Ps5(128, "ps5");
    companion object {
        fun fromPreference(value: String): Int = when (value.lowercase()) {
            "desktop" -> Desktop.bit
            "xbox" -> Xbox.bit
            "samsung" -> Samsung.bit
            "ios" -> Ios.bit
            "web", "embedded" -> Embedded.bit
            "ps4" -> Ps4.bit
            "ps5" -> Ps5.bit
            else -> Android.bit
        }
        fun wireValueFromBit(bit: Int): String = entries.firstOrNull { it.bit == bit }?.wireValue ?: Android.wireValue
    }
}

object DiscordSocialPresenceClient {
    private const val TAG = "DiscordSocialPresenceClient"
    private const val MAX_SEND_ATTEMPTS = 2

    private val mutex = Mutex()
    private val callbackScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var gateway: GatewayClient? = null
    @Volatile private var activeToken: String? = null
    @Volatile private var transportInvalidatedListener: ((String) -> Unit)? = null

    val isStarted: Boolean get() = isConnectionUsable()

    private fun isConnectionUsable(): Boolean {
        val currentGateway = gateway
        val currentToken = activeToken
        return currentGateway != null && !currentToken.isNullOrBlank() && currentGateway.isReady()
    }

    fun setOnTransportInvalidated(listener: ((String) -> Unit)?) {
        transportInvalidatedListener = listener
    }

    suspend fun updatePresence(accessToken: String, activity: DiscordPresenceActivity): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accessToken.trim()
            if (token.isBlank()) return@withLock Result.failure(IllegalArgumentException("Discord access token is missing"))

            val presenceJson = buildPresencePayload(token, activity)
            var lastError: Throwable? = null

            repeat(MAX_SEND_ATTEMPTS) { attempt ->
                val connectResult = ensureConnected(token, force = attempt > 0)
                if (connectResult.isFailure) return@withLock connectResult

                val currentGateway = gateway
                val sent = if (currentGateway != null && currentGateway.isReady()) {
                    currentGateway.sendPresenceUpdate(presenceJson)
                } else {
                    false
                }

                if (sent) {
                    if (attempt > 0) {
                        Timber.tag(TAG).i("updatePresence: sent after reconnect attempt=%d", attempt)
                    }
                    return@withLock Result.success(Unit)
                }

                lastError = Exception("Failed to send presence update (attempt=$attempt)")
                Timber.tag(TAG).w("updatePresence: send failed, reconnecting attempt=%d", attempt)
                tearDownLocked("presence_send_failed_$attempt")
            }
            Result.failure(lastError ?: Exception("Failed to send presence update"))
        }
    }

    suspend fun clearPresence(accessToken: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val token = accessToken?.trim().orEmpty()
            val empty = JSONObject().apply {
                put("activities", JSONArray())
                put("afk", false)
                put("since", JSONObject.NULL)
                put("status", "online")
            }

            var lastError: Throwable? = null
            repeat(MAX_SEND_ATTEMPTS) { attempt ->
                val currentGateway = gateway
                if (currentGateway == null || !currentGateway.isReady()) {
                    if (token.isBlank()) return@withLock Result.failure(Exception("Not connected"))
                    val connectResult = ensureConnected(token, force = currentGateway != null || attempt > 0)
                    if (connectResult.isFailure) return@withLock connectResult
                }

                val activeGateway = gateway
                val sent = if (activeGateway != null && activeGateway.isReady()) {
                    activeGateway.sendPresenceUpdate(empty)
                } else {
                    false
                }

                if (sent) return@withLock Result.success(Unit)

                lastError = Exception("Failed to clear presence (attempt=$attempt)")
                Timber.tag(TAG).w("clearPresence: send failed, reconnecting attempt=%d", attempt)
                tearDownLocked("clear_send_failed_$attempt")
                if (token.isBlank()) return@withLock Result.failure(lastError!!)
            }
            Result.failure(lastError ?: Exception("Failed to clear presence"))
        }
    }

    suspend fun close(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            tearDownLocked("close")
            Result.success(Unit)
        }
    }

    private suspend fun ensureConnected(token: String, force: Boolean = false): Result<Unit> {
        val currentGateway = gateway
        val currentToken = activeToken
        if (!force && currentToken == token && currentGateway != null && currentGateway.isReady()) {
            return Result.success(Unit)
        }

        if (currentGateway != null) {
            Timber.tag(TAG).d("ensureConnected: reconnecting force=%s tokenMatch=%s ready=%s", force, currentToken == token, currentGateway.isReady())
        }

        tearDownLocked(if (force) "ensure_force" else "ensure_reconnect")

        val newGateway = GatewayClient()
        attachCallbacks(newGateway)

        return try {
            newGateway.connect(token)
            if (!newGateway.isReady()) {
                throw IllegalStateException("Gateway connected but not ready (isConnected=${newGateway.isConnected()}, isReady=${newGateway.isReady()})")
            }
            gateway = newGateway
            activeToken = token
            Timber.tag(TAG).i("ensureConnected: connected")
            Result.success(Unit)
        } catch (e: CancellationException) {
            cleanNewGateway(newGateway)
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "ensureConnected failed")
            cleanNewGateway(newGateway)
            Result.failure(e)
        }
    }

    private fun attachCallbacks(newGateway: GatewayClient) {
        newGateway.onClose = { info ->
            Timber.tag(TAG).w("gateway closed code=%d reason=%s resumable=%s", info.code, info.reason, info.resumable)
            invalidateGatewayAsync(newGateway, "gateway_closed_${info.code}")
        }
        newGateway.onError = { error -> Timber.tag(TAG).w(error, "gateway error") }
        newGateway.onReady = { Timber.tag(TAG).d("gateway ready") }
    }

    private fun invalidateGatewayAsync(expectedGateway: GatewayClient, reason: String) {
        callbackScope.launch {
            var invalidated = false
            mutex.withLock {
                if (gateway === expectedGateway) {
                    tearDownLocked(reason)
                    invalidated = true
                }
            }
            if (invalidated) notifyTransportInvalidated(reason)
        }
    }

    private fun notifyTransportInvalidated(reason: String) {
        runCatching { transportInvalidatedListener?.invoke(reason) }
            .onFailure { error -> Timber.tag(TAG).w(error, "transport invalidation listener failed") }
    }

    private fun cleanNewGateway(newGateway: GatewayClient) {
        newGateway.onClose = null
        newGateway.onError = null
        newGateway.onReady = null
        newGateway.onDebug = null
        runCatching { newGateway.disconnect() }
    }

    private fun tearDownLocked(reason: String) {
        val currentGateway = gateway
        if (currentGateway != null) {
            Timber.tag(TAG).d("tearDownLocked: %s", reason)
            currentGateway.onClose = null
            currentGateway.onError = null
            currentGateway.onReady = null
            currentGateway.onDebug = null
            runCatching { currentGateway.disconnect() }
        }
        gateway = null
        activeToken = null
    }

    private suspend fun buildPresencePayload(token: String, activity: DiscordPresenceActivity): JSONObject {
        val (resolvedLarge, resolvedSmall) = DiscordAssetRegistrar.resolveImages(token, activity.assets.largeImage, activity.assets.smallImage)
        val activityJson = JSONObject().apply {
            put("name", activity.name ?: "AvidTune")
            put("type", activity.type.nativeValue)
            activity.details?.let { put("details", it) }
            activity.state?.let { put("state", it) }
            put("application_id", activity.applicationId)
            
            val timestamps = JSONObject()
            activity.timestamps.startEpochSeconds?.let { timestamps.put("start", it * 1000L) }
            activity.timestamps.endEpochSeconds?.let { timestamps.put("end", it * 1000L) }
            if (timestamps.length() > 0) put("timestamps", timestamps)

            val assets = JSONObject()
            resolvedLarge?.let { assets.put("large_image", it) }
            activity.assets.largeText?.let { assets.put("large_text", it) }
            resolvedSmall?.let { assets.put("small_image", it) }
            activity.assets.smallText?.let { assets.put("small_text", it) }
            if (assets.length() > 0) put("assets", assets)

            if (activity.buttons.isNotEmpty()) {
                val buttonsArray = JSONArray()
                val metadataUrls = JSONArray()
                for (button in activity.buttons.take(2)) {
                    buttonsArray.put(button.label)
                    metadataUrls.put(button.url)
                }
                put("buttons", buttonsArray)
                put("metadata", JSONObject().apply { put("button_urls", metadataUrls) })
            }
            put("platform", DiscordActivityPlatform.wireValueFromBit(activity.supportedPlatforms))
        }
        return JSONObject().apply {
            put("activities", JSONArray().apply { put(activityJson) })
            put("afk", false)
            put("since", JSONObject.NULL)
            put("status", when (activity.onlineStatus) {
                DiscordOnlineStatus.Idle -> "idle"
                DiscordOnlineStatus.Dnd -> "dnd"
                else -> "online"
            })
        }
    }
}
