package com.cgens67.innertube.models.response

import com.cgens67.innertube.models.ResponseContext
import com.cgens67.innertube.models.Thumbnails
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val responseContext: ResponseContext? = null,
    val playabilityStatus: PlayabilityStatus? = null,
    val playerConfig: PlayerConfig? = null,
    val streamingData: StreamingData? = null,
    val videoDetails: VideoDetails? = null,
    @SerialName("playbackTracking")
    val playbackTracking: PlaybackTracking? = null,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String,
        val reason: String? = null,
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig? = null,
    ) {
        @Serializable
        data class AudioConfig(
            val loudnessDb: Double? = null,
            val perceptualLoudnessDb: Double? = null,
        )
    }

    @Serializable
    data class StreamingData(
        val formats: List<Format>? = null,
        val adaptiveFormats: List<Format>? = null,
        val expiresInSeconds: String? = null,
    ) {
        @Serializable
        data class Format(
            val itag: Int,
            val url: String? = null,
            val mimeType: String,
            val bitrate: Int,
            val width: Int? = null,
            val height: Int? = null,
            val contentLength: String? = null,
            val quality: String? = null,
            val fps: Int? = null,
            val qualityLabel: String? = null,
            val averageBitrate: Int? = null,
            val audioQuality: String? = null,
            val approxDurationMs: String? = null,
            val audioSampleRate: String? = null,
            val audioChannels: Int? = null,
            val loudnessDb: Double? = null,
            val lastModified: String? = null,
            val signatureCipher: String? = null,
        ) {
            val isAudio: Boolean
                get() = width == null
        }
    }

    @Serializable
    data class VideoDetails(
        val videoId: String,
        val title: String,
        val author: String,
        val channelId: String,
        val lengthSeconds: String,
        val musicVideoType: String? = null,
        val viewCount: String? = null,
        val thumbnail: Thumbnails? = null,
    )

    @Serializable
    data class PlaybackTracking(
        @SerialName("videostatsPlaybackUrl")
        val videostatsPlaybackUrl: VideostatsPlaybackUrl? = null,
        @SerialName("videostatsWatchtimeUrl")
        val videostatsWatchtimeUrl: VideostatsWatchtimeUrl? = null,
        @SerialName("atrUrl")
        val atrUrl: AtrUrl? = null,
    ) {
        @Serializable
        data class VideostatsPlaybackUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
        @Serializable
        data class VideostatsWatchtimeUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
        @Serializable
        data class AtrUrl(
            @SerialName("baseUrl")
            val baseUrl: String?,
        )
    }
}
