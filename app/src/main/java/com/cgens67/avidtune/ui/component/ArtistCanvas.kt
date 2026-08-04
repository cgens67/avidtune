package com.cgens67.avidtune.ui.component

import android.content.Context
import android.util.Base64
import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cgens67.avidtune.constants.ArtistCanvasProviderOrderKey
import com.cgens67.avidtune.constants.EnableAppleMusicCanvasKey
import com.cgens67.avidtune.constants.EnableAvidCanvasKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

interface ArtistCanvasProvider {
    val name: String
    fun isEnabled(context: Context): Boolean
    suspend fun getCanvas(artistName: String, storefront: String = "us"): String?
}

object AvidCanvasProvider : ArtistCanvasProvider {
    override val name = "AvidCanvas"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore.get(EnableAvidCanvasKey, true)

    private val client by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 5000
                connectTimeoutMillis = 5000
            }
            expectSuccess = false
        }
    }

    override suspend fun getCanvas(artistName: String, storefront: String): String? {
        return runCatching {
            val formattedName = artistName.lowercase().replace(Regex("[^a-z0-9]"), "_")
            if (formattedName.isBlank()) return@runCatching null

            val baseUrl = "https://raw.githubusercontent.com/cgens67/avidtune-canvas/main/canvas/$formattedName"

            // Check mp4 first
            val mp4Url = "$baseUrl.mp4"
            var response = client.head(mp4Url)
            if (response.status == HttpStatusCode.OK) return@runCatching mp4Url

            // Fallback to m3u8
            val m3u8Url = "$baseUrl.m3u8"
            response = client.head(m3u8Url)
            if (response.status == HttpStatusCode.OK) return@runCatching m3u8Url

            null
        }.getOrNull()
    }
}

object AppleMusicCanvasProvider : ArtistCanvasProvider {
    override val name = "Apple Music"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore.get(EnableAppleMusicCanvasKey, true)

    private const val APPLE_MUSIC_TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxMDMyODU1LCJleHAiOjE3ODQw" +
        "NTY4NTUsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".fiMFcJWkfSlxKP9NVA0UW9CbItD1Rge0SISuepz203XcpU762OqdCpU9M-YkmtKkjRmaIWtjsfGgqZPrlMonpA"

    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(jsonParser)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            expectSuccess = false
        }
    }

    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L

    private suspend fun getOrFetchToken(): String {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryMs - 60_000) {
            return cachedToken!!
        }

        return try {
            val html = client.get("https://music.apple.com/us/browse") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            }.bodyAsText()

            val scriptRegex = Regex("""/assets/index(?:-legacy)?[~-][a-zA-Z0-9_-]+\.js""")
            val scripts = scriptRegex.findAll(html).map { it.value }.distinct().toList()

            var fetchedToken: String? = null
            for (scriptPath in scripts) {
                val scriptUrl = "https://music.apple.com$scriptPath"
                val scriptText = client.get(scriptUrl) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }.bodyAsText()

                val tokenRegex = Regex("""ey[a-zA-Z0-9_-]+\.ey[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+""")
                val tokens = tokenRegex.findAll(scriptText).map { it.value }
                for (token in tokens) {
                    try {
                        val body = token.split(".")[1]
                        val decodedBytes = Base64.decode(body, Base64.URL_SAFE)
                        val decoded = String(decodedBytes, Charsets.UTF_8)
                        if (decoded.contains("iss") && decoded.contains("exp")) {
                            val expIndex = decoded.indexOf("\"exp\":")
                            if (expIndex != -1) {
                                val expValStr = decoded.substring(expIndex + 6).takeWhile { it.isDigit() }
                                val expSeconds = expValStr.toLongOrNull() ?: 0L
                                if (expSeconds * 1000 > now) {
                                    fetchedToken = token
                                    tokenExpiryMs = expSeconds * 1000
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore decoding issues
                    }
                }
                if (fetchedToken != null) break
            }

            if (fetchedToken != null) {
                cachedToken = fetchedToken
                fetchedToken
            } else {
                APPLE_MUSIC_TOKEN
            }
        } catch (e: Exception) {
            APPLE_MUSIC_TOKEN
        }
    }

    override suspend fun getCanvas(artistName: String, storefront: String): String? {
        if (artistName.isBlank()) return null
        return runCatching {
            val searchUrl = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(searchUrl) {
                header("Authorization", "Bearer ${getOrFetchToken()}")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", artistName)
                parameter("types", "artists")
                parameter("limit", "3")
            }
            if (response.status != HttpStatusCode.OK) return@runCatching null

            val responseText = response.bodyAsText()
            val root = jsonParser.parseToJsonElement(responseText) as? JsonObject ?: return@runCatching null
            val results = (root["results"] as? JsonObject)
                ?.get("artists")?.let { it as? JsonObject }
                ?.get("data")?.let { it as? JsonArray } ?: return@runCatching null

            val scoredResults = results.mapNotNull { item ->
                val obj = item as? JsonObject ?: return@mapNotNull null
                val attributes = obj["attributes"] as? JsonObject ?: return@mapNotNull null
                val resultName = (attributes["name"] as? JsonPrimitive)?.contentOrNull ?: ""

                if (!resultName.contains(artistName, ignoreCase = true) &&
                    !artistName.contains(resultName, ignoreCase = true)) return@mapNotNull null

                var score = 0
                if (resultName.equals(artistName, ignoreCase = true)) score += 10
                else if (resultName.contains(artistName, ignoreCase = true) || artistName.contains(resultName, ignoreCase = true)) score += 5

                score to obj
            }.sortedByDescending { it.first }

            for ((score, obj) in scoredResults) {
                if (score < 4) continue
                val artistId = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: continue
                val artistUrl = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId"
                val artistRes = client.get(artistUrl) {
                    header("Authorization", "Bearer ${getOrFetchToken()}")
                    header("Origin", "https://music.apple.com")
                    header("Referer", "https://music.apple.com/")
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    parameter("extend", "editorialVideo,editorialArtwork")
                }
                if (artistRes.status == HttpStatusCode.OK) {
                    val artistText = artistRes.bodyAsText()
                    val artistRoot = jsonParser.parseToJsonElement(artistText) as? JsonObject
                    val dataArray = artistRoot?.get("data") as? JsonArray
                    val firstData = dataArray?.firstOrNull() as? JsonObject
                    val attrs = firstData?.get("attributes") as? JsonObject

                    val ev = attrs?.get("editorialVideo") as? JsonObject
                    if (ev != null) {
                        val videoUrl = extractEditorialVideoUrl(ev)
                        if (!videoUrl.isNullOrBlank()) {
                            return@runCatching videoUrl
                        }
                    }

                    val ea = attrs?.get("editorialArtwork") as? JsonObject
                    if (ea != null) {
                        val videoUrl = extractEditorialVideoUrl(ea)
                        if (!videoUrl.isNullOrBlank()) {
                            return@runCatching videoUrl
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun extractEditorialVideoUrl(editorialData: JsonObject): String? {
        val preferredKeys = listOf("motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionSquareVideo1x1", "motionTallVideo3x4")
        for (key in preferredKeys) {
            val element = editorialData[key] as? JsonObject
            val videoUrl = (element?.get("video") as? JsonPrimitive)?.contentOrNull
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        for ((_, value) in editorialData) {
            val element = value as? JsonObject
            val videoUrl = (element?.get("video") as? JsonPrimitive)?.contentOrNull
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        return null
    }
}

object ArtistCanvasHelper {
    private val allProviders = listOf(AvidCanvasProvider, AppleMusicCanvasProvider)
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun getArtistCanvas(context: Context, artistName: String, storefront: String = "us"): String? {
        if (artistName.isBlank()) return null
        val cacheKey = "$storefront|$artistName"
        cache[cacheKey]?.let { return it }

        val orderStr = context.dataStore.data.first()[ArtistCanvasProviderOrderKey]
        val ordered = if (orderStr == null) {
            allProviders
        } else {
            val orderNames = orderStr.split(",")
            val orderedList = orderNames.mapNotNull { name -> allProviders.find { it.name == name } }
            val missing = allProviders.filter { it !in orderedList }
            orderedList + missing
        }

        for (provider in ordered) {
            if (provider.isEnabled(context)) {
                val canvasUrl = provider.getCanvas(artistName, storefront)
                if (!canvasUrl.isNullOrBlank()) {
                    cache[cacheKey] = canvasUrl
                    return canvasUrl
                }
            }
        }

        return null
    }
}

@Composable
fun ArtistVideo(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var isVideoReady by remember { mutableStateOf(false) }

    val okHttpClient = remember { OkHttpClient.Builder().build() }
    val mediaSourceFactory = remember(okHttpClient) {
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                OkHttpDataSource.Factory(okHttpClient),
            ),
        )
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false,
                )
                volume = 0f
                repeatMode = Player.REPEAT_MODE_ONE
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                isVideoReady = true
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUrl) {
        try {
            val normalized = videoUrl.trim()
            val mimeType = when {
                normalized.lowercase(Locale.ROOT).contains("m3u8") -> MimeTypes.APPLICATION_M3U8
                normalized.lowercase(Locale.ROOT).contains("mp4") -> MimeTypes.VIDEO_MP4
                else -> MimeTypes.APPLICATION_M3U8
            }

            val mediaItem = MediaItem.Builder()
                .setUri(normalized)
                .setMimeType(mimeType)
                .build()

            exoPlayer.stop()
            isVideoReady = false
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "videoAlpha"
    )

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable { onClick() },
    ) {
        AndroidView(
            factory = { viewContext ->
                android.widget.FrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)

                    val textureView = TextureView(viewContext).apply {
                        layoutParams = android.widget.FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, android.view.Gravity.CENTER)
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { _ ->
                // TextureView is already set in factory. Doing it here causes flickering.
            },
            modifier = Modifier
                .matchParentSize()
                .alpha(videoAlpha),
        )
    }
}
