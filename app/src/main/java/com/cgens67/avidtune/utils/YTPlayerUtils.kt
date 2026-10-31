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

    private val CLIENTS_TO_TRY = listOf(
        YouTubeClient.WEB_REMIX,
        YouTubeClient.IOS,
        YouTubeClient.TVHTML5,
        YouTubeClient.ANDROID
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
        
        var playerResponse: PlayerResponse? = null
        var successfulClient: YouTubeClient? = null

        // Try multiple clients until one returns playable streaming data with valid URLs
        for (client in CLIENTS_TO_TRY) {
            val response = YouTube.player(videoId, playlistId, client, null, poToken).getOrNull()
            if (response?.playabilityStatus?.status == "OK" && 
                (!response.streamingData?.adaptiveFormats.isNullOrEmpty() || !response.streamingData?.formats.isNullOrEmpty())) {
                playerResponse = response
                successfulClient = client
                Timber.tag(logTag).d("Successfully retrieved player response using client: ${client.clientName}")
                break
            }
        }

        if (playerResponse == null || playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                playerResponse?.playabilityStatus?.reason ?: "All fallback clients failed to retrieve a playable stream",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        val candidateFormats = findCandidateFormats(playerResponse, audioQuality, connectivityManager)
        val format = candidateFormats.firstOrNull { !it.url.isNullOrBlank() }
            ?: throw Exception("Could not find suitable format with a valid direct stream URL")
        
        PlaybackData(
            audioConfig = playerResponse.playerConfig?.audioConfig,
            videoDetails = playerResponse.videoDetails,
            playbackTracking = playerResponse.playbackTracking,
            format = format,
            streamUrl = format.url!!,
            streamExpiresInSeconds = playerResponse.streamingData?.expiresInSeconds ?: 43200,
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

        var playerResponse: PlayerResponse? = null
        for (client in CLIENTS_TO_TRY) {
            val response = YouTube.player(videoId, playlistId, client, null, poToken).getOrNull()
            if (response?.playabilityStatus?.status == "OK") {
                playerResponse = response
                break
            }
        }

        if (playerResponse?.playabilityStatus?.status != "OK") {
            throw Exception("Bad stream player response")
        }

        val audioFormats = (playerResponse.streamingData?.adaptiveFormats.orEmpty() + playerResponse.streamingData?.formats.orEmpty())
            .filter { it.isAudio }

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
        // Combine both adaptiveFormats and standard formats to ensure we never miss a stream URL
        val formats = (playerResponse.streamingData?.adaptiveFormats.orEmpty() + playerResponse.streamingData?.formats.orEmpty())
            .filter { it.isAudio }

        if (formats.isEmpty()) return emptyList()

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