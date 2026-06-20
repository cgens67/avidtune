package com.cgens67.avidtune.ui.component

import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.net.URLDecoder

object ArtistCanvasProvider {
    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 25_000
                socketTimeoutMillis = 25_000
            }
            expectSuccess = false
        }
    }

    private val cache = ConcurrentHashMap<String, String>()
    private var dynamicToken: String? = null

    // Dynamically fetches the Apple Music token from the main HTML meta tags
    private suspend fun getAppleMusicToken(): String? {
        dynamicToken?.let { return it }

        return try {
            Timber.d("Fetching Apple Music token from main page...")
            val mainPage = client.get("https://music.apple.com/us/home").bodyAsText()

            // Extract the URL-encoded JSON configuration from the meta tag
            val metaTagMatch = Regex("""<meta name="desktop-music-app/config/environment" content="([^"]+)"\s*/?>""").find(mainPage)
            
            if (metaTagMatch != null) {
                val urlEncodedJson = metaTagMatch.groupValues[1]
                val decodedJson = URLDecoder.decode(urlEncodedJson, "UTF-8")
                
                // Extract the token from the decoded JSON
                val tokenMatch = Regex(""""token"\s*:\s*"([^"]+)"""").find(decodedJson)
                if (tokenMatch != null) {
                    val token = tokenMatch.groupValues[1]
                    Timber.d("Successfully extracted token from meta tag")
                    dynamicToken = token
                    return token
                }
            }

            // Fallback: Just look for any valid looking JWT token in the page source
            val fallbackMatch = Regex("""(eyJh[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+)""").find(mainPage)
            if (fallbackMatch != null) {
                val token = fallbackMatch.groupValues[1]
                Timber.d("Successfully extracted token using fallback regex")
                dynamicToken = token
                return token
            }

            Timber.e("Could not find Apple Music token")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error fetching Apple Music token")
            null
        }
    }

    suspend fun getArtistCanvas(artistName: String, storefront: String = "us"): String? {
        if (artistName.isBlank()) return null
        val key = "$storefront|$artistName"
        cache[key]?.let { return it }

        return runCatching {
            val token = getAppleMusicToken() ?: return@runCatching null

            val searchUrl = "$AMP_BASE_URL/v1/catalog/$storefront/search"
            val response = client.get(searchUrl) {
                header("Authorization", "Bearer $token")
                header("Origin", "https://music.apple.com")
                header("Referer", "https://music.apple.com/")
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                parameter("term", artistName)
                parameter("types", "artists")
                parameter("limit", "3")
                parameter("l", "en-US")
            }
            
            if (response.status != HttpStatusCode.OK) {
                Timber.w("Artist search failed with status: ${response.status}")
                if (response.status == HttpStatusCode.Unauthorized) {
                    // Force a token refresh next time if it was rejected
                    dynamicToken = null 
                }
                return@runCatching null
            }

            val root = response.body<JsonObject>()
            val results = root["results"]?.jsonObject?.get("artists")?.jsonObject?.get("data")?.jsonArray ?: return@runCatching null

            for (item in results) {
                val obj = item.jsonObject
                val attributes = obj["attributes"]?.jsonObject ?: continue
                val resultName = attributes["name"]?.jsonPrimitive?.contentOrNull ?: ""

                if (resultName.equals(artistName, ignoreCase = true) || resultName.contains(artistName, ignoreCase = true) || artistName.contains(resultName, ignoreCase = true)) {
                    val artistId = obj["id"]?.jsonPrimitive?.contentOrNull ?: continue
                    val artistUrl = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId"
                    
                    val artistRes = client.get(artistUrl) {
                        header("Authorization", "Bearer $token")
                        header("Origin", "https://music.apple.com")
                        header("Referer", "https://music.apple.com/")
                        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        parameter("extend", "editorialVideo,editorialArtwork")
                        parameter("l", "en-US")
                    }
                    if (artistRes.status == HttpStatusCode.OK) {
                        val artistRoot = artistRes.body<JsonObject>()
                        val attrs = artistRoot["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("attributes")?.jsonObject
                        val ev = attrs?.get("editorialVideo")?.jsonObject ?: attrs?.get("editorialArtwork")?.jsonObject
                        
                        if (ev != null) {
                            val preferredKeys = listOf("motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionSquareVideo1x1", "motionTallVideo3x4")
                            for (k in preferredKeys) {
                                val videoUrl = ev[k]?.jsonObject?.get("video")?.jsonPrimitive?.contentOrNull
                                if (!videoUrl.isNullOrBlank()) {
                                    cache[key] = videoUrl
                                    Timber.d("Found Canvas video URL: $videoUrl")
                                    return@runCatching videoUrl
                                }
                            }
                        } else {
                            Timber.w("No editorial video/artwork found for $artistName")
                        }
                    }
                }
            }
            null
        }.onFailure {
            Timber.e(it, "Exception in getArtistCanvas")
        }.getOrNull()
    }
}

@Composable
fun ArtistVideo(
    videoUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var isVideoReady by remember(videoUrl) { mutableStateOf(false) }

    val okHttpClient = remember { OkHttpClient.Builder().build() }
    val mediaSourceFactory =
        remember(okHttpClient) {
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(
                    context,
                    OkHttpDataSource.Factory(okHttpClient),
                ),
            )
        }
    val exoPlayer =
        remember(videoUrl) {
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
                    playWhenReady = true
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                }
        }

    DisposableEffect(exoPlayer, videoUrl) {
        val listener =
            object : Player.Listener {
                override fun onRenderedFirstFrame() {
                    isVideoReady = true
                }
            }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(videoUrl, exoPlayer) {
        val normalized = videoUrl.trim()
        val mimeType =
            when {
                normalized.lowercase(Locale.ROOT).contains("m3u8") -> MimeTypes.APPLICATION_M3U8
                normalized.lowercase(Locale.ROOT).contains("mp4") -> MimeTypes.VIDEO_MP4
                else -> MimeTypes.APPLICATION_M3U8
            }

        val mediaItem =
            MediaItem.Builder()
                .setUri(normalized)
                .setMimeType(mimeType)
                .build()

        exoPlayer.stop()
        isVideoReady = false
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val videoAlpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "videoAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
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
            modifier = Modifier
                .matchParentSize()
                .alpha(videoAlpha),
        )
    }
}
