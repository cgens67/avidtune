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
        
        // Use IOS client first as it returns direct, unencrypted stream URLs without needing signature ciphers
        var playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.IOS, null, poToken).getOrNull()
        
        // Fallback to WEB_REMIX if IOS is blocked
        if (playerResponse?.playabilityStatus?.status != "OK") {
            playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.WEB_REMIX, null, poToken).getOrNull()
        }

        if (playerResponse == null || playerResponse.playabilityStatus.status != "OK") {
            throw PlaybackException(
                playerResponse?.playabilityStatus?.reason ?: "Unplayable stream",
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        val candidateFormats = findCandidateFormats(playerResponse, audioQuality, connectivityManager)
        val format = candidateFormats.firstOrNull { !it.url.isNullOrBlank() }
            ?: throw Exception("Could not find suitable format")
        
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

        var playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.IOS, null, poToken).getOrNull()
        if (playerResponse?.playabilityStatus?.status != "OK") {
            playerResponse = YouTube.player(videoId, playlistId, YouTubeClient.WEB_REMIX, null, poToken).getOrNull()
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