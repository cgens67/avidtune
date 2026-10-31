package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.avidtune.utils.potoken.PoTokenGenerator
import okhttp3.OkHttpClient
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

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        val poToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken
        
        // Metrolist's InnerTubeX natively handles URL deciphering and client switching.
        var playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.WEB_REMIX, null, poToken).getOrNull()
        
        if (playerResponse?.playabilityStatus?.status == "UNPLAYABLE" || playerResponse?.playabilityStatus?.status == "LOGIN_REQUIRED") {
            playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.IOS).getOrNull()
        }

        if (playerResponse == null) {
            throw Exception("Failed to get player response")
        }

        if (playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                playerResponse.playabilityStatus.reason ?: "Unknown error",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        val audioConfig = playerResponse.playerConfig?.audioConfig
        val videoDetails = playerResponse.videoDetails
        val playbackTracking = playerResponse.playbackTracking
        
        val candidateFormats = findCandidateFormats(playerResponse, audioQuality, connectivityManager)
        val format = candidateFormats.firstOrNull { !it.url.isNullOrBlank() }
            ?: throw Exception("Could not find suitable format")
        
        val streamUrl = format.url!!
        val streamExpiresInSeconds = playerResponse.streamingData?.expiresInSeconds ?: throw Exception("Missing stream expire time")

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
        return YouTube.player(videoId, playlistId, YouTubeClient.WEB_REMIX, null, poToken)
    }

    suspend fun getAudioStreamsForExport(
        videoId: String,
        playlistId: String? = null
    ): Result<List<Pair<PlayerResponse.StreamingData.Format, String>>> = runCatching {
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        val poToken = PoTokenGenerator().getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken

        var playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.WEB_REMIX, null, poToken).getOrNull()
        if (playerResponse?.playabilityStatus?.status == "UNPLAYABLE" || playerResponse?.playabilityStatus?.status == "LOGIN_REQUIRED") {
            playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.IOS).getOrNull()
        }

        if (playerResponse?.playabilityStatus?.status != "OK") {
            throw Exception("Bad stream player response")
        }

        val audioFormats = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio } ?: emptyList()

        val validStreams = audioFormats.mapNotNull { format ->
            format.url?.let { url -> format to url }
        }

        if (validStreams.isEmpty()) {
            throw Exception("No valid audio streams found via InnerTube")
        }

        validStreams
    }

    private fun findCandidateFormats(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): List<PlayerResponse.StreamingData.Format> {
        val formats = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio && it.isOriginal }
            ?: return emptyList()

        val isMetered = connectivityManager.isActiveNetworkMetered

        return formats.sortedByDescending { format ->
            val qualityMultiplier = when (audioQuality) {
                AudioQuality.AUTO -> if (isMetered) -1 else 1
                AudioQuality.HIGH -> 1
                AudioQuality.LOW -> -1
            }
            (format.bitrate * qualityMultiplier) + (if (format.mimeType.startsWith("audio/webm")) 10240 else 0)
        }
    }
}