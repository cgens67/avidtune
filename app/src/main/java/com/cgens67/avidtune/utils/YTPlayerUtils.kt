package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_TESTSUITE
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_UNPLUGGED
import com.cgens67.innertube.models.YouTubeClient.Companion.IOS
import com.cgens67.innertube.models.YouTubeClient.Companion.IOS_MUSIC
import com.cgens67.innertube.models.YouTubeClient.Companion.MWEB
import com.cgens67.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_SAFARI
import com.cgens67.avidtune.utils.potoken.PoTokenGenerator
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.cgens67.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX

    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        ANDROID_TESTSUITE,
        IOS_MUSIC,
        ANDROID_UNPLUGGED,
        ANDROID_MUSIC,
        MWEB,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        IOS,
        WEB_SAFARI,
        WEB_CREATOR,
        ANDROID_CREATOR
    )

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    /**
     * Custom player response intended to use for playback.
     * Metadata like audioConfig and videoDetails are from [MAIN_CLIENT].
     * Format & stream can be from [MAIN_CLIENT] or [STREAM_FALLBACK_CLIENTS].
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<PlaybackData> = runCatching {
        Timber.tag(logTag).d("Fetching player response for videoId: $videoId, playlistId: $playlistId")

        /**
         * This is required for some clients to get working streams however
         * it should not be forced for the [MAIN_CLIENT] because the response of the [MAIN_CLIENT]
         * is required even if the streams won't work from this client.
         * This is why it is allowed to be null.
         */
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).d("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId =
            if (isLoggedIn) {
                // signed in sessions use dataSyncId as identifier
                YouTube.dataSyncId
            } else {
                // signed out sessions use visitorData as identifier
                YouTube.visitorData
            } ?: ""
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        var audioConfig: PlayerResponse.PlayerConfig.AudioConfig? = null
        var videoDetails: PlayerResponse.VideoDetails? = null
        var playbackTracking: PlayerResponse.PlaybackTracking? = null
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        val clientsToTry = listOf(MAIN_CLIENT) + STREAM_FALLBACK_CLIENTS

        for (client in clientsToTry) {
            if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) continue

            Timber.tag(logTag).d("Attempting with client: ${client.clientName}")

            val poToken = if (client.useWebPoTokens) {
                poTokenGenerator.getWebClientPoToken(videoId, sessionId)?.playerRequestPoToken
            } else null

            val response = YouTube.player(videoId, playlistId, client, signatureTimestamp, poToken).getOrNull()

            if (response?.playabilityStatus?.status == "OK") {
                // Keep the metadata of the first working client we hit (usually MAIN_CLIENT)
                if (videoDetails == null) {
                    audioConfig = response.playerConfig?.audioConfig
                    videoDetails = response.videoDetails
                    playbackTracking = response.playbackTracking
                }

                val candidateFormat = findFormat(response, audioQuality, connectivityManager)
                if (candidateFormat != null) {
                    val candidateUrl = findUrlOrNull(candidateFormat, videoId)
                    if (candidateUrl != null) {
                        // Use it immediately if it's the last fallback, else validate it
                        if (client == clientsToTry.last() || validateStatus(candidateUrl)) {
                            format = candidateFormat
                            streamUrl = candidateUrl
                            streamExpiresInSeconds = response.streamingData?.expiresInSeconds
                            streamPlayerResponse = response
                            Timber.tag(logTag).d("Working stream found with client: ${client.clientName}")
                            break
                        } else {
                            Timber.tag(logTag).d("Stream URL validation failed for client: ${client.clientName}")
                        }
                    }
                }
            } else {
                Timber.tag(logTag).d("Player response status not OK: ${response?.playabilityStatus?.status}, reason: ${response?.playabilityStatus?.reason}")
            }
        }

        if (streamPlayerResponse == null || format == null || streamUrl == null || streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        Timber.tag(logTag).d("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")
        PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }

    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> = runCatching {
        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData ?: ""
        
        val mainPoToken = if (MAIN_CLIENT.useWebPoTokens) {
            poTokenGenerator.getWebClientPoToken(videoId, sessionId)?.playerRequestPoToken
        } else null

        Timber.tag(logTag).d("Fetching metadata-only player response for videoId: $videoId")
        
        var response = YouTube.player(videoId, playlistId, client = MAIN_CLIENT, poToken = mainPoToken).getOrNull()
        if (response == null || response.playabilityStatus.status != "OK") {
            response = YouTube.player(videoId, playlistId, client = YouTubeClient.ANDROID_TESTSUITE).getOrNull()
        }
        
        response ?: throw Exception("Failed to fetch metadata from any client")
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        Timber.tag(logTag).d("Finding format with audioQuality: $audioQuality, network metered: ${connectivityManager.isActiveNetworkMetered}")

        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        if (format != null) {
            Timber.tag(logTag).d("Selected format: ${format.mimeType}, bitrate: ${format.bitrate}")
        } else {
            Timber.tag(logTag).d("No suitable audio format found")
        }

        return format
    }

    /**
     * Checks if the stream url returns a successful status.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error during playback.
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            val requestBuilder = Request.Builder()
                .head()
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            Timber.tag(logTag).d("Stream URL validation result: ${if (isSuccessful) "Success" else "Failed"} (${response.code})")
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
            reportException(e)
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        Timber.tag(logTag).d("Getting signature timestamp for videoId: $videoId")
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).d("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
    }

    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        return NewPipeUtils.getStreamUrl(format, videoId)
            .onSuccess { Timber.tag(logTag).d("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull()
    }
}
