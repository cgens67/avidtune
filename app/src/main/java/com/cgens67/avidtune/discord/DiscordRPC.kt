package com.cgens67.avidtune.discord

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import moe.rukamori.archivetune.utils.discordAlbumMusicUrl
import moe.rukamori.archivetune.utils.discordArtistMusicUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import com.cgens67.avidtune.db.entities.Song

// --- high-level Kizzy-compatible bridge ---
class DiscordRPC(
    private val context: Context,
    private val accessToken: String,
) {
    fun isRpcRunning(): Boolean = DiscordPresenceManager.isRunning()

    suspend fun updateSong(
        song: Song,
        currentPlaybackTimeMillis: Long,
        playbackSpeed: Float = 1.0f,
        useDetails: Boolean = false,
    ) = runCatching {
        DiscordPresenceManager.updateNow(
            context = context,
            token = accessToken,
            song = song,
            positionMs = currentPlaybackTimeMillis,
            isPaused = !PlayerConnection.instance?.player?.isPlaying!!,
            isMusicVideo = false
        )
    }

    suspend fun stopActivity() {
        DiscordPresenceManager.clearNow(context, accessToken)
    }

    suspend fun closeRPC() {
        DiscordPresenceManager.stop()
    }
}

object DiscordPresenceManager {
    private const val LOG_TAG = "DiscordPresenceManager"
    private const val IMAGE_RESOLUTION_TIMEOUT_MS = 8_000L
    private const val STOP_TIMEOUT_MS = 5_000L

    private val started = AtomicBoolean(false)
    private val updateGeneration = AtomicLong(0L)
    private val rpcMutex = Mutex()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var scope: CoroutineScope? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private var rpcInstance: DiscordRPC? = null
    private var rpcToken: String? = null

    private var consecutiveFailures = 0

    private val lastRpcStartTimeState = MutableStateFlow<Long?>(null)
    val lastRpcStartTimeFlow = lastRpcStartTimeState.asStateFlow()
    val lastRpcStartTime: Long? get() = lastRpcStartTimeState.value

    private val lastRpcEndTimeState = MutableStateFlow<Long?>(null)
    val lastRpcEndTimeFlow = lastRpcEndTimeState.asStateFlow()
    val lastRpcEndTime: Long? get() = lastRpcEndTimeState.value

    fun setLastRpcTimestamps(start: Long?, end: Long?) {
        lastRpcStartTimeState.value = start
        lastRpcEndTimeState.value = end
    }

    private fun addLifecycleObserverOnMain(observer: LifecycleEventObserver) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        } else {
            Handler(Looper.getMainLooper()).post {
                ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
            }
        }
    }

    private fun removeLifecycleObserverOnMain(observer: LifecycleEventObserver) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
        } else {
            Handler(Looper.getMainLooper()).post {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(observer)
            }
        }
    }

    private suspend fun getOrCreateRpc(context: Context, token: String): DiscordRPC {
        val activeToken = DiscordOAuthRepository.getValidAccessToken(context) ?: token
        if (rpcInstance == null || rpcToken != activeToken) {
            runCatching { rpcInstance?.stopActivity() }
            runCatching { rpcInstance?.closeRPC() }
            rpcInstance = DiscordRPC(context.applicationContext, activeToken)
            rpcToken = activeToken
        }
        return rpcInstance ?: error("Discord RPC instance was not created")
    }

    suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
        isMusicVideo: Boolean = false,
    ): Boolean = updatePresence(
        context = context,
        token = token,
        song = song,
        positionMs = positionMs,
        isPaused = isPaused,
        isMusicVideo = isMusicVideo,
        generation = updateGeneration.incrementAndGet(),
    )

    suspend fun clearNow(context: Context, token: String? = null): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        rpcMutex.withLock {
            try {
                clearPresenceLocked(appContext, token)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.tag(LOG_TAG).e(error, "clearNow failed")
                false
            }
        }
    }

    private suspend fun updatePresence(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
        isMusicVideo: Boolean = false,
        generation: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        rpcMutex.withLock {
            if (generation != updateGeneration.get()) return@withLock true

            try {
                val activeToken = DiscordOAuthRepository.getValidAccessToken(appContext) ?: token
                if (activeToken.isBlank()) return@withLock false

                if (song == null) {
                    val rpc = getOrCreateRpc(appContext, activeToken)
                    rpc.stopActivity()
                    setLastRpcTimestamps(null, null)
                    consecutiveFailures = 0
                    return@withLock true
                }

                runCatching {
                    withTimeout(IMAGE_RESOLUTION_TIMEOUT_MS) {
                        DiscordImageResolver.resolveImagesForSong(appContext, song, isMusicVideo)
                    }
                }

                if (generation != updateGeneration.get()) return@withLock true

                val rpc = getOrCreateRpc(appContext, activeToken)
                val result = rpc.updateSong(song, positionMs, isPaused = isPaused)
                if (result.isSuccess) {
                    consecutiveFailures = 0
                    updateLastTimestamps(song, positionMs, isPaused)
                    true
                } else {
                    consecutiveFailures++
                    false
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                consecutiveFailures++
                false
            }
        }
    }

    fun start(context: Context, token: String) {
        if (!started.getAndSet(true)) {
            consecutiveFailures = 0
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            lifecycleObserver = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) stop()
            }
            addLifecycleObserverOnMain(lifecycleObserver!!)
        }
        if (token.isNotBlank()) rpcToken = token
    }

    suspend fun updateNow(
        context: Context,
        token: String,
        song: Song?,
        positionMs: Long,
        isPaused: Boolean,
        isMusicVideo: Boolean = false,
    ): Boolean = updatePresence(context, token, song, positionMs, isPaused, isMusicVideo)

    fun setOnTransportInvalidated(listener: ((String) -> Unit)?) {
        DiscordSocialPresenceClient.setOnTransportInvalidated(listener)
    }

    private suspend fun clearPresenceLocked(context: Context, token: String? = null): Boolean {
        val existingRpc = rpcInstance
        if (existingRpc != null) {
            existingRpc.stopActivity()
            setLastRpcTimestamps(null, null)
            consecutiveFailures = 0
            return true
        }

        val activeToken = DiscordOAuthRepository.getValidAccessToken(context) ?: token.orEmpty()
        if (activeToken.isBlank()) return false

        val rpc = getOrCreateRpc(context, activeToken)
        rpc.stopActivity()
        setLastRpcTimestamps(null, null)
        consecutiveFailures = 0
        return true
    }

    fun stop() {
        if (!started.getAndSet(false)) return

        DiscordSocialPresenceClient.setOnTransportInvalidated(null)
        updateGeneration.incrementAndGet()
        scope?.cancel()
        scope = null

        lifecycleObserver?.let { removeLifecycleObserverOnMain(it) }
        lifecycleObserver = null

        val rpcToClose = rpcInstance
        rpcInstance = null
        rpcToken = null
        setLastRpcTimestamps(null, null)

        if (rpcToClose != null) {
            cleanupScope.launch {
                rpcMutex.withLock {
                    runCatching {
                        withTimeout(STOP_TIMEOUT_MS) {
                            rpcToClose.stopActivity()
                            rpcToClose.closeRPC()
                        }
                    }
                }
            }
        }
    }

    fun isRunning(): Boolean = started.get()

    private fun updateLastTimestamps(song: Song, positionMs: Long, isPaused: Boolean) {
        val durationMs = song.song.duration.takeIf { it > 0 }?.toLong()?.times(1000L)
        if (isPaused || durationMs == null) {
            setLastRpcTimestamps(null, null)
            return
        }
        val startMs = System.currentTimeMillis() - positionMs.coerceAtLeast(0L)
        setLastRpcTimestamps(startMs, startMs + durationMs)
    }
}

object DiscordAssetRegistrar {
    private const val TAG = "DiscordAssetRegistrar"
    private const val API_BASE = "https://discord.com/api/v10"
    private val client = OkHttpClient()
    private val cache = ConcurrentHashMap<String, String>()
    private val mutex = Mutex()

    suspend fun resolveImage(accessToken: String, imageUrl: String?): String? = withContext(Dispatchers.IO) {
        if (imageUrl == null) return@withContext null
        val parsed = parseImageType(imageUrl)
        when (parsed) {
            is ImageType.Snowflake, is ImageType.MpPrefix, is ImageType.DiscordCdn -> parsed.value
            is ImageType.ExternalUrl -> {
                cache[imageUrl]?.let { return@withContext "mp:$it" }
                val registered = registerExternal(accessToken, imageUrl) ?: return@withContext null
                cache[imageUrl] = registered
                "mp:$registered"
            }
            is ImageType.Raw -> parsed.value
        }
    }

    suspend fun resolveImages(accessToken: String, largeImage: String?, smallImage: String?): Pair<String?, String?> = withContext(Dispatchers.IO) {
        mutex.withLock {
            val urlsToRegister = mutableListOf<Pair<String, String>>()
            val largeType = largeImage?.let { parseImageType(it) }
            val smallType = smallImage?.let { parseImageType(it) }

            if (largeType is ImageType.ExternalUrl && !cache.containsKey(largeImage)) urlsToRegister.add("large" to largeImage)
            if (smallType is ImageType.ExternalUrl && !cache.containsKey(smallImage)) urlsToRegister.add("small" to smallImage)

            if (urlsToRegister.isNotEmpty()) {
                val urls = urlsToRegister.map { it.second }
                try {
                    val results = registerExternalBatch(accessToken, urls)
                    for (i in results.indices) {
                        val registeredPath = results[i]
                        if (registeredPath != null) {
                            cache[urlsToRegister[i].second] = registeredPath
                        }
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Failed to register external assets")
                }
            }

            val resolvedLarge = when (largeType) {
                is ImageType.ExternalUrl -> cache[largeImage]?.let { "mp:$it" }
                else -> largeType?.value
            }
            val resolvedSmall = when (smallType) {
                is ImageType.ExternalUrl -> cache[smallImage]?.let { "mp:$it" }
                else -> smallType?.value
            }
            resolvedLarge to resolvedSmall
        }
    }

    fun clearCache() = cache.clear()

    private sealed class ImageType {
        abstract val value: String
        data class Snowflake(override val value: String) : ImageType()
        data class MpPrefix(override val value: String) : ImageType()
        data class DiscordCdn(override val value: String) : ImageType()
        data class ExternalUrl(override val value: String) : ImageType()
        data class Raw(override val value: String) : ImageType()
    }

    private fun parseImageType(image: String): ImageType {
        if (Regex("^[0-9]{17,19}$").matches(image)) return ImageType.Snowflake(image)
        if (listOf("mp:", "youtube:", "spotify:", "twitch:").any { image.startsWith(it) }) return ImageType.MpPrefix(image)
        if (image.startsWith("external/")) return ImageType.MpPrefix("mp:$image")
        val isValidUrl = try {
            val uri = URI(image)
            uri.scheme == "http" || uri.scheme == "https"
        } catch (_: Exception) { false }
        if (!isValidUrl) return ImageType.Raw(image)
        val isDiscordCdn = listOf("https://cdn.discordapp.com/", "http://cdn.discordapp.com/", "https://media.discordapp.net/", "http://media.discordapp.net/").any { image.startsWith(it) }
        if (isDiscordCdn) {
            val result = image.replace("https://cdn.discordapp.com/", "mp:")
                .replace("http://cdn.discordapp.com/", "mp:")
                .replace("https://media.discordapp.net/", "mp:")
                .replace("http://media.discordapp.net/", "mp:")
            return ImageType.DiscordCdn(result)
        }
        return ImageType.ExternalUrl(image)
    }

    private fun registerExternal(accessToken: String, imageUrl: String): String? {
        val json = JSONObject().apply { put("urls", JSONArray(listOf(imageUrl))) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$API_BASE/applications/${DiscordOAuthRepository.applicationId}/external-assets")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body).build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null
        val arr = JSONArray(responseBody)
        if (arr.length() == 0) return null
        return arr.getJSONObject(0).optString("external_asset_path", null)
    }

    private fun registerExternalBatch(accessToken: String, urls: List<String>): List<String?> {
        if (urls.isEmpty()) return emptyList()
        val json = JSONObject().apply { put("urls", JSONArray(urls)) }
        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$API_BASE/applications/${DiscordOAuthRepository.applicationId}/external-assets")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body).build()

        return try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return urls.map { null }
            if (!response.isSuccessful) return urls.map { null }
            val arr = JSONArray(responseBody)
            (0 until arr.length()).map { arr.getJSONObject(it).optString("external_asset_path", null) }
        } catch (e: Exception) { urls.map { null } }
    }
}

object DiscordImageResolver {
    fun buildImageUrl(imageType: String, customUrl: String, resolvedImages: Pair<String?, String?>, song: Song): String? {
        return when (imageType.lowercase()) {
            "thumbnail" -> song.song.thumbnailUrl
            "artist" -> song.artists.firstOrNull()?.thumbnailUrl
            "custom" -> customUrl.takeIf { it.isNotBlank() }
            else -> song.song.thumbnailUrl
        }
    }
    suspend fun resolveImagesForSong(context: Context, song: Song, isMusicVideo: Boolean = false): Pair<String?, String?> {
        val token = DiscordOAuthRepository.getValidAccessToken(context).orEmpty()
        return DiscordAssetRegistrar.resolveImages(token, song.song.thumbnailUrl, song.artists.firstOrNull()?.thumbnailUrl)
    }
    fun clearCache() = DiscordAssetRegistrar.clearCache()
}

internal fun Song.discordAlbumMusicUrl(): String? {
    return album?.playlistId?.takeIf { it.isNotBlank() }?.let { "https://music.youtube.com/playlist?list=$it" }
        ?: album?.id?.toYouTubeMusicAlbumUrl() ?: song.albumId.toYouTubeMusicAlbumUrl()
}

private fun String?.toYouTubeMusicAlbumUrl(): String? {
    val id = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (id.isLocalMediaId()) return null
    return if (id.startsWith("OLAK5uy_", ignoreCase = true)) "https://music.youtube.com/playlist?list=$id" else "https://music.youtube.com/browse/$id"
}

fun String.isLocalMediaId(): Boolean = startsWith("content://") || startsWith("file://")
fun String.isLocalArtistId(): Boolean = startsWith("LOCAL_ARTIST_") || startsWith("LA") || contains("privately_owned_artist", ignoreCase = true)
