package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.avidtune.App
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.PlaybackClientOrderKey
import com.cgens67.avidtune.utils.potoken.PoTokenGenerator
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
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
        val clientName: String = "",
    )

    val AVAILABLE_CLIENTS = mapOf(
        "ANDROID_NO_SDK" to YouTubeClient.ANDROID_NO_SDK,
        "VISIONOS" to YouTubeClient.VISIONOS,
        "ANDROID_CREATOR" to YouTubeClient.ANDROID_CREATOR,
        "ANDROID_VR_1_43_32" to YouTubeClient.ANDROID_VR_1_43_32,
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER" to YouTubeClient.TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        "WEB_REMIX" to YouTubeClient.WEB_REMIX,
        "TVHTML5" to YouTubeClient.TVHTML5,
        "TVHTML5_SIMPLY" to YouTubeClient.TVHTML5_SIMPLY,
        "WEB_CREATOR" to YouTubeClient.WEB_CREATOR,
        "MWEB" to YouTubeClient.MWEB,
        "WEB" to YouTubeClient.WEB,
        "IOS" to YouTubeClient.IOS,
        "IPADOS" to YouTubeClient.IPADOS,
        "MOBILE" to YouTubeClient.MOBILE,
        "ANDROID_VR_NO_AUTH" to YouTubeClient.ANDROID_VR_NO_AUTH,
        "ANDROID_VR_1_65_10" to YouTubeClient.ANDROID_VR_1_65_10,
        "ANDROID_VR_1_61_48" to YouTubeClient.ANDROID_VR_1_61_48
    )

    val DEFAULT_CLIENT_FALLBACK_CHAIN = listOf(
        "ANDROID_NO_SDK",
        "VISIONOS",
        "ANDROID_CREATOR",
        "ANDROID_VR_1_43_32",
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
        "WEB_REMIX",
        "TVHTML5",
        "TVHTML5_SIMPLY",
        "WEB_CREATOR",
        "MWEB",
        "WEB",
        "IOS",
        "IPADOS",
        "MOBILE",
        "ANDROID_VR_NO_AUTH",
        "ANDROID_VR_1_65_10",
        "ANDROID_VR_1_61_48"
    )

    fun getClientForName(name: String): YouTubeClient? = AVAILABLE_CLIENTS[name.trim().uppercase()]

    fun getClientFallbackChain(): List<Pair<String, YouTubeClient>> {
        val orderStr = runCatching { App.instance.dataStore[PlaybackClientOrderKey] }.getOrNull()
        if (orderStr.isNullOrBlank()) {
            return DEFAULT_CLIENT_FALLBACK_CHAIN.mapNotNull { key -> AVAILABLE_CLIENTS[key]?.let { key to it } }
        }
        val names = orderStr.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        val mapped = names.mapNotNull { key -> AVAILABLE_CLIENTS[key]?.let { key to it } }
        val missingKeys = AVAILABLE_CLIENTS.keys - names.toSet()
        val missing = missingKeys.mapNotNull { key -> AVAILABLE_CLIENTS[key]?.let { key to it } }
        return (mapped + missing).distinctBy { it.first }
    }

    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager?,
    ): Result<PlaybackData> = runCatching {
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData

        val webPoToken = PoTokenGenerator.getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken

        var successfulResponse: PlayerResponse? = null
        var chosenFormat: PlayerResponse.StreamingData.Format? = null
        var chosenStreamUrl: String? = null
        var chosenExpiresInSeconds: Int? = null
        var chosenClientName = ""

        val clientChain = getClientFallbackChain()

        for ((clientKey, client) in clientChain) {
            if (client.loginRequired && !isLoggedIn) {
                continue
            }

            val clientPoToken = if (clientKey == "WEB_REMIX" || clientKey == "WEB") {
                webPoToken
            } else {
                null
            }

            val clientNameFormatted = clientKey

            Timber.tag(logTag).d("Attempting player fetch with client: $clientNameFormatted for videoId: $videoId")
            val playerRes = YouTube.player(videoId, playlistId, client, signatureTimestamp, clientPoToken).getOrNull()

            if (playerRes == null) {
                Timber.tag(logTag).w("Client $clientNameFormatted returned null response, trying next client...")
                continue
            }

            val status = playerRes.playabilityStatus.status
            if (status != "OK") {
                Timber.tag(logTag).w("Client $clientNameFormatted unplayable: $status (${playerRes.playabilityStatus.reason}), trying next client...")
                continue
            }

            val candidateFormats = findCandidateFormats(playerRes, audioQuality, connectivityManager)
            for (candidate in candidateFormats) {
                val candidateUrl = findUrlOrNull(candidate, videoId)
                if (!candidateUrl.isNullOrBlank()) {
                    chosenFormat = candidate
                    chosenStreamUrl = candidateUrl
                    break
                }
            }

            if (chosenFormat != null && chosenStreamUrl != null) {
                successfulResponse = playerRes
                chosenExpiresInSeconds = playerRes.streamingData?.expiresInSeconds ?: 21600
                chosenClientName = clientNameFormatted
                Timber.tag(logTag).i("Successfully obtained valid stream using client: $chosenClientName")
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
            clientName = chosenClientName
        )
    }

    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        val sessionId = if (YouTube.cookie != null) YouTube.dataSyncId else YouTube.visitorData
        val poToken = PoTokenGenerator.getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken
        return YouTube.player(videoId, playlistId, client = YouTubeClient.WEB_REMIX, poToken = poToken)
    }

    suspend fun getAudioStreamsForExport(
        videoId: String,
        playlistId: String? = null
    ): Result<List<Pair<PlayerResponse.StreamingData.Format, String>>> = runCatching {
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        val webPoToken = PoTokenGenerator.getWebClientPoToken(videoId, sessionId ?: "")?.playerRequestPoToken

        val validStreams = mutableListOf<Pair<PlayerResponse.StreamingData.Format, String>>()
        val clientChain = getClientFallbackChain()

        for ((clientKey, client) in clientChain) {
            if (client.loginRequired && !isLoggedIn) continue
            val clientPoToken = if (clientKey == "WEB_REMIX" || clientKey == "WEB") webPoToken else null
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
        val formats = (playerResponse.streamingData?.adaptiveFormats.orEmpty() + 
                       playerResponse.streamingData?.formats.orEmpty())
            .filter { it.isAudio || it.audioSampleRate != null || (it.bitrate > 0 && it.width == null) }
            .ifEmpty { return emptyList() }

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