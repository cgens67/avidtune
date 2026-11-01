package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
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

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    // Primary order of clients to try when bot checks or stream errors occur
    private val CLIENT_FALLBACK_CHAIN = listOf(
        YouTubeClient.WEB_REMIX,
        YouTubeClient.IOS,
        YouTubeClient.ANDROID_VR_1_65_10,
        YouTubeClient.TVHTML5
    )

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData

        // Web PoToken is only valid for Web clients
        val webPoToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken

        var successfulResponse: PlayerResponse? = null
        var chosenFormat: PlayerResponse.StreamingData.Format? = null
        var chosenStreamUrl: String? = null
        var chosenExpiresInSeconds: Int? = null

        for (client in CLIENT_FALLBACK_CHAIN) {
            if (client.loginRequired && !isLoggedIn) {
                continue
            }

            // Only pass webPoToken to WEB_REMIX / WEB. Non-web clients must receive null.
            val clientPoToken = if (client == YouTubeClient.WEB_REMIX || client == YouTubeClient.WEB) {
                webPoToken
            } else {
                null
            }

            Timber.tag(logTag).d("Attempting player fetch with client: ${client.name} for videoId: $videoId")
            val playerRes = YouTube.player(videoId, playlistId, client, signatureTimestamp, clientPoToken).getOrNull()

            if (playerRes == null) {
                Timber.tag(logTag).w("Client ${client.name} returned null response, trying next client...")
                continue
            }

            val status = playerRes.playabilityStatus.status
            if (status != "OK") {
                Timber.tag(logTag).w("Client ${client.name} unplayable: $status (${playerRes.playabilityStatus.reason}), trying next client...")
                continue
            }

            // Attempt to resolve streams
            val candidateFormats = findCandidateFormats(playerRes, audioQuality, connectivityManager)
            for (candidate in candidateFormats) {
                val candidateUrl = findUrlOrNull(candidate, videoId)
                if (candidateUrl != null && validateStatus(candidateUrl)) {
                    chosenFormat = candidate
                    chosenStreamUrl = candidateUrl
                    break
                }
            }

            if (chosenFormat != null && chosenStreamUrl != null) {
                successfulResponse = playerRes
                chosenExpiresInSeconds = playerRes.streamingData?.expiresInSeconds ?: 21600
                Timber.tag(logTag).i("Successfully obtained valid stream using client: ${client.name}")
                break
            }
        }

        if (successfulResponse == null || chosenFormat == null || chosenStreamUrl == null) {
            throw PlaybackException(
                "Unable to obtain playback stream from any client (bot detection or restrictions)",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        PlaybackData(
            audioConfig = successfulResponse.playerConfig?.audioConfig,
            videoDetails = successfulResponse.videoDetails,
            playbackTracking = successfulResponse.playbackTracking,
            format = chosenFormat,
            streamUrl = chosenStreamUrl,
            streamExpiresInSeconds = chosenExpiresInSeconds ?: 21600,
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        val sessionId = if (YouTube.cookie != null) YouTube.dataSyncId else YouTube.visitorData
        val poToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken
        return YouTube.player(videoId, playlistId, client = YouTubeClient.WEB_REMIX, poToken = poToken)
    }

    suspend fun getAudioStreamsForExport(
        videoId: String,
        playlistId: String? = null
    ): Result<List<Pair<PlayerResponse.StreamingData.Format, String>>> = runCatching {
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        val webPoToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken

        val validStreams = mutableListOf<Pair<PlayerResponse.StreamingData.Format, String>>()

        for (client in CLIENT_FALLBACK_CHAIN) {
            if (client.loginRequired && !isLoggedIn) continue
            val clientPoToken = if (client == YouTubeClient.WEB_REMIX || client == YouTubeClient.WEB) webPoToken else null
            val response = YouTube.player(videoId, playlistId, client, signatureTimestamp, clientPoToken).getOrNull()

            if (response?.playabilityStatus?.status == "OK") {
                val audioFormats = response.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio } ?: emptyList()

                for (format in audioFormats) {
                    val url = findUrlOrNull(format, videoId) ?: continue
                    if (validStreams.none { it.first.itag == format.itag }) {
                        validStreams.add(format to url)
                    }
                }

                if (validStreams.isNotEmpty()) break
            }
        }

        if (validStreams.isEmpty()) {
            throw Exception("No valid audio streams found across all clients")
        }

        validStreams
    }

    private fun findCandidateFormats(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager?,
    ): List<PlayerResponse.StreamingData.Format> {
        val formats = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?: return emptyList()

        val isMetered = connectivityManager?.isActiveNetworkMetered ?: false

        return formats.sortedByDescending { format ->
            val qualityMultiplier = when (audioQuality) {
                AudioQuality.AUTO -> if (isMetered) -1 else 1
                AudioQuality.HIGH -> 1
                AudioQuality.LOW -> -1
            }
            (format.bitrate * qualityMultiplier) + (if (format.mimeType.startsWith("audio/webm")) 10240 else 0)
        }
    }

    private fun validateStatus(url: String): Boolean {
        return try {
            val requestBuilder = Request.Builder().head().url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    private fun getSignatureTimestampOrNull(videoId: String): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        if (!format.url.isNullOrBlank()) {
            return format.url
        }
        return NewPipeUtils.getStreamUrl(format, videoId).getOrNull()
    }
}