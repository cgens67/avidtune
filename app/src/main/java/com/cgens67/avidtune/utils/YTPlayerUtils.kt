package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_NO_SDK
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.cgens67.innertube.models.YouTubeClient.Companion.IOS
import com.cgens67.innertube.models.YouTubeClient.Companion.MOBILE
import com.cgens67.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.cgens67.innertube.strategy.ContentAwareFallbackStrategy
import com.cgens67.innertube.strategy.ContentHints
import com.cgens67.avidtune.utils.potoken.PoTokenGenerator
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .build()
        
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    private val fallbackStrategy = ContentAwareFallbackStrategy()

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        
        // Use PoTokenGenerator to provide the token required by WEB_REMIX
        val poToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken
        
        Timber.tag(logTag).d("Attempting to get player response using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        var mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, poToken).getOrNull()

        if (mainPlayerResponse?.playabilityStatus?.status == "UNPLAYABLE") {
            mainPlayerResponse = YouTube.player(videoId, playlistId, ANDROID_NO_SDK, signatureTimestamp).getOrNull()
        }
        
        val hints = ContentHints(
            isExplicit = mainPlayerResponse?.playabilityStatus?.reason?.contains("explicit", true),
            isKidsContent = mainPlayerResponse?.playabilityStatus?.reason?.contains("kids", true),
            isLive = mainPlayerResponse?.videoDetails?.lengthSeconds == "0",
        )
        val STREAM_FALLBACK_CLIENTS = fallbackStrategy.resolveClients(hints).toTypedArray()

        if (mainPlayerResponse == null) {
            throw Exception("Failed to get main player response")
        }

        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            val client: YouTubeClient
            if (clientIndex == -1) {
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                client = STREAM_FALLBACK_CLIENTS[clientIndex]

                if (client.loginRequired && !isLoggedIn) {
                    continue
                }

                streamPlayerResponse =
                    YouTube.player(videoId, playlistId, client, signatureTimestamp, poToken).getOrNull()
            }

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                val tempStreamPlayerResponse = YouTube.newPipePlayer(videoId, streamPlayerResponse)
                streamPlayerResponse = tempStreamPlayerResponse ?: streamPlayerResponse

                format = findFormat(streamPlayerResponse, audioQuality, connectivityManager)

                if (format == null) continue
                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) continue

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) continue

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) break

                if (validateStatus(streamUrl)) break
            }
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                streamPlayerResponse.playabilityStatus.reason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) throw Exception("Missing stream expire time")
        if (format == null) throw Exception("Could not find format")
        if (streamUrl == null) throw Exception("Could not find stream url")

        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        val sessionId = if (YouTube.cookie != null) YouTube.dataSyncId else YouTube.visitorData
        val poToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken
        return YouTube.player(videoId, playlistId, client = WEB_REMIX, poToken = poToken)
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        return playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
            }
    }

    private fun validateStatus(url: String): Boolean {
        try {
            val requestBuilder = Request.Builder().head().url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            return response.isSuccessful
        } catch (e: Exception) {
            reportException(e)
        }
        return false
    }

    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                reportException(it)
            }
            .getOrNull()
    }
}
