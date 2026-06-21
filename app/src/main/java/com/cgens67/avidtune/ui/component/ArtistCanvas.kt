package com.cgens67.avidtune.ui.component

import android.util.Base64
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ArtistCanvasProvider {
    // Updated fallback token from your friend's code
    private const val APPLE_MUSIC_TOKEN =
        "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiIsImtpZCI6IldlYlBsYXlLaWQifQ" +
        ".eyJpc3MiOiJBTVBXZWJQbGF5IiwiaWF0IjoxNzgxMDMyODU1LCJleHAiOjE3ODQw" +
        "NTY4NTUsInJvb3RfaHR0cHNfb3JpZ2luIjpbImFwcGxlLmNvbSJdfQ" +
        ".fiMFcJWkfSlxKP9NVA0UW9CbItD1Rge0SISuepz203XcpU762OqdCpU9M-YkmtKkjRmaIWtjsfGgqZPrlMonpA"

    private const val AMP_BASE_URL = "https://amp-api.music.apple.com"

    // Switched to OkHttp to prevent unhandled exceptions and crashes caused by Ktor's CIO engine on Android
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, String>()
    
    private var cachedToken: String? = null
    private var tokenExpiryMs: Long = 0L

    private suspend fun asyncGet(url: String, headers: Map<String, String> = emptyMap()): String {
        val requestBuilder = Request.Builder().url(url)
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }
        val request = requestBuilder.build()

        return suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            if (continuation.isActive) continuation.resumeWithException(IOException("HTTP ${it.code}"))
                        } else {
                            val bodyStr = it.body?.string() ?: ""
                            if (continuation.isActive) continuation.resume(bodyStr)
                        }
                    }
                }
            })
        }
    }

    private suspend fun getOrFetchToken(): String {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryMs - 60_000) {
            return cachedToken!!
        }

        return try {
            val html = asyncGet("https://music.apple.com/us/browse", mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))

            val scriptRegex = Regex("""/assets/index(?:-legacy)?[~-][a-zA-Z0-9_-]+\.js""")
            val scripts = scriptRegex.findAll(html).map { it.value }.distinct().toList()

            var fetchedToken: String? = null
            for (scriptPath in scripts) {
                val scriptUrl = "https://music.apple.com$scriptPath"
                val scriptText = try {
                    asyncGet(scriptUrl, mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    ))
                } catch (e: Exception) {
                    continue
                }

                val tokenRegex = Regex("""ey[a-zA-Z0-9_-]+\.ey[a-zA-Z0-9_-]+\.[a-zA-Z0-9_-]+""")
                val tokens = tokenRegex.findAll(scriptText).map { it.value }
                for (token in tokens) {
                    try {
                        val body = token.split(".")[1]
                        var base64 = body.replace("-", "+").replace("_", "/")
                        val pad = base64.length % 4
                        if (pad > 0) {
                            base64 += "=".repeat(4 - pad)
                        }
                        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            APPLE_MUSIC_TOKEN
        }
    }

    suspend fun getArtistCanvas(artistName: String, storefront: String = "us"): String? {
        if (artistName.isBlank()) return null
        val key = "$storefront|$artistName"
        cache[key]?.let { return it }

        return try {
            val token = getOrFetchToken()
            val encodedTerm = URLEncoder.encode(artistName, "UTF-8")
            val searchUrl = "$AMP_BASE_URL/v1/catalog/$storefront/search?term=$encodedTerm&types=artists&limit=3"
            
            val responseStr = asyncGet(searchUrl, mapOf(
                "Authorization" to "Bearer $token",
                "Origin" to "https://music.apple.com",
                "Referer" to "https://music.apple.com/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
            ))

            val root = JSONObject(responseStr)
            val results = root.optJSONObject("results")?.optJSONObject("artists")?.optJSONArray("data") ?: return null

            val scoredResults = mutableListOf<Pair<Int, JSONObject>>()
            for (i in 0 until results.length()) {
                val obj = results.optJSONObject(i) ?: continue
                val attributes = obj.optJSONObject("attributes") ?: continue
                val resultName = attributes.optString("name", "")
                
                if (!resultName.contains(artistName, ignoreCase = true) && 
                    !artistName.contains(resultName, ignoreCase = true)) continue
                
                var score = 0
                if (resultName.equals(artistName, ignoreCase = true)) score += 10
                else if (resultName.contains(artistName, ignoreCase = true) || artistName.contains(resultName, ignoreCase = true)) score += 5
                
                scoredResults.add(score to obj)
            }
            
            val sortedResults = scoredResults.sortedByDescending { it.first }
            
            for ((score, obj) in sortedResults) {
                if (score < 4) continue
                val artistId = obj.optString("id", "")
                if (artistId.isEmpty()) continue
                
                val artistUrl = "$AMP_BASE_URL/v1/catalog/$storefront/artists/$artistId?extend=editorialVideo,editorialArtwork"
                
                val artistResStr = try {
                    asyncGet(artistUrl, mapOf(
                        "Authorization" to "Bearer $token",
                        "Origin" to "https://music.apple.com",
                        "Referer" to "https://music.apple.com/",
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                    ))
                } catch (e: Exception) {
                    continue
                }

                val artistRoot = JSONObject(artistResStr)
                val attrs = artistRoot.optJSONArray("data")?.optJSONObject(0)?.optJSONObject("attributes")
                
                val ev = attrs?.optJSONObject("editorialVideo")
                if (ev != null) {
                    val videoUrl = extractEditorialVideoUrl(ev)
                    if (!videoUrl.isNullOrBlank()) {
                        cache[key] = videoUrl
                        return videoUrl
                    }
                }

                val ea = attrs?.optJSONObject("editorialArtwork")
                if (ea != null) {
                    val videoUrl = extractEditorialVideoUrl(ea)
                    if (!videoUrl.isNullOrBlank()) {
                        cache[key] = videoUrl
                        return videoUrl
                    }
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun extractEditorialVideoUrl(editorialData: JSONObject): String? {
        val preferredKeys = listOf("motionDetailRaw", "motionDetailTall", "motionDetailSquare", "motionSquareVideo1x1", "motionTallVideo3x4")
        for (k in preferredKeys) {
            val videoUrl = editorialData.optJSONObject(k)?.optString("video", null)
            if (!videoUrl.isNullOrBlank()) return videoUrl
        }
        // Deep Fallback: Loop through all keys if preferred ones don't match
        val keys = editorialData.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val videoUrl = editorialData.optJSONObject(k)?.optString("video", null)
            if (!videoUrl.isNullOrBlank()) return videoUrl
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
    val mediaSourceFactory =
        remember(okHttpClient) {
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(
                    context,
                    OkHttpDataSource.Factory(okHttpClient),
                ),
            )
        }
    
    // Crucial Fix: Create ExoPlayer without passing videoUrl as a key to prevent recreating the player
    // This allows the AndroidView's TextureView to stay correctly bound to the same ExoPlayer instance.
    val exoPlayer =
        remember {
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

    DisposableEffect(exoPlayer) {
        val listener =
            object : Player.Listener {
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
