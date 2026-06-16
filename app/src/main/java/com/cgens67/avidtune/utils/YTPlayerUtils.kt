package com.cgens67.avidtune.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.cgens67.innertube.models.response.PlayerResponse
import com.cgens67.innertube.pages.NewPipeUtils
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.YouTubeClient
import com.cgens67.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.cgens67.innertube.models.YouTubeClient.Companion.IOS
import com.cgens67.innertube.models.YouTubeClient.Companion.MOBILE
import com.cgens67.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.cgens67.innertube.models.YouTubeClient.Companion.WEB_REMIX
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"

    private val httpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .build()

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
     * IOS and TVHTML5 are heavily prioritized to bypass JS extractor failing due to "Unexpected M" obfuscation.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        IOS,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        WEB,
        WEB_CREATOR
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
            }
        Timber.tag(logTag).d("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"}")

        val mainPlayerResponse =
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp).getOrThrow()
        
        val audioConfig = mainPlayerResponse.playerConfig?.audioConfig
        val videoDetails = mainPlayerResponse.videoDetails
        val playbackTracking = mainPlayerResponse.playbackTracking
        
        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        for (clientIndex in (-1 until STREAM_FALLBACK_CLIENTS.size)) {
            // reset for each client
            format = null
            streamUrl = null
            streamExpiresInSeconds = null

            // decide which client to use for streams and load its player response
            val client: YouTubeClient = if (clientIndex == -1) {
                MAIN_CLIENT
            } else {
                STREAM_FALLBACK_CLIENTS[clientIndex]
            }

            if (clientIndex != -1 && client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                continue
            }

            streamPlayerResponse = if (clientIndex == -1) {
                mainPlayerResponse
            } else {
                YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
            }

            // process current client response
            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                format = findFormat(streamPlayerResponse, audioQuality, connectivityManager)

                if (format == null) {
                    continue
                }

                streamUrl = findUrlOrNull(format, videoId)
                if (streamUrl == null) {
                    continue
                }

                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds
                if (streamExpiresInSeconds == null) {
                    continue
                }

                if (clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    /** skip [validateStatus] for last client */
                    break
                }

                if (validateStatus(streamUrl)) {
                    // working stream found
                    break
                }
            }
        }

        if (streamPlayerResponse == null) {
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null || format == null || streamUrl == null) {
            throw Exception("Could not resolve playable stream data")
        }

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
    ): Result<PlayerResponse> {
        return YouTube.player(videoId, playlistId, client = WEB_REMIX) // ANDROID_VR does not work with history
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): PlayerResponse.StreamingData.Format? {
        val format = playerResponse.streamingData?.adaptiveFormats
            ?.filter { it.isAudio }
            ?.maxByOrNull {
                it.bitrate * when (audioQuality) {
                    AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 1
                    AudioQuality.HIGH -> 1
                    AudioQuality.LOW -> -1
                } + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0) // prefer opus stream
            }

        return format
    }

    /**
     * Checks if the stream url returns a successful status and validates content-type.
     * If this returns true the url is likely to work.
     * If this returns false the url might cause an error (such as the "Unexpected M" HTML payload).
     */
    private fun validateStatus(url: String): Boolean {
        Timber.tag(logTag).d("Validating stream URL status")
        try {
            // Using a chunk request guarantees we retrieve stream headers without YouTube rejecting HEAD checks.
            val requestBuilder = Request.Builder()
                .get()
                .header("Range", "bytes=0-1")
                .url(url)
            val response = httpClient.newCall(requestBuilder.build()).execute()
            val isSuccessful = response.isSuccessful
            val contentType = response.header("Content-Type") ?: ""
            response.close()

            // If the stream returns an HTML page instead of an audio stream, it implies bot protection blocked it.
            if (isSuccessful && contentType.startsWith("text/html", ignoreCase = true)) {
                Timber.tag(logTag).w("Stream URL valid HTTP 200 but returned text/html (bot protection intercepted)")
                return false
            }
            if (response.code == 403 || response.code == 401) {
                return false
            }
            return isSuccessful
        } catch (e: Exception) {
            Timber.tag(logTag).e(e, "Stream URL validation failed with exception")
        }
        return false
    }

    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which suppresses exceptions safely
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        return NewPipeUtils.getSignatureTimestamp(videoId)
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
            }
            .getOrNull()
    }

    /**
     * Bypasses heavy [NewPipeUtils.getStreamUrl] evaluation completely if YouTube Native provides an available unencrypted link.
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String
    ): String? {
        Timber.tag(logTag).d("Finding stream URL for format: ${format.mimeType}")

        // If the format already has an unciphered direct URL (like from IOS/TV clients), we use it immediately.
        if (!format.url.isNullOrEmpty()) {
            Timber.tag(logTag).d("Direct URL available, bypassing signature decryption")
            return format.url
        }

        // If it requires decryption, attempt to decipher safely returning null if broken.
        val newPipeUrl = NewPipeUtils.getStreamUrl(format, videoId)
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL via NewPipe. Falling back to internal URL creation.")
            }
            .getOrNull()

        return newPipeUrl ?: com.cgens67.innertube.utils.createUrl(format.url, format.signatureCipher)
    }
}
