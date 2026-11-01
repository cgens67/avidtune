package com.cgens67.innertube.pages

import com.cgens67.innertube.models.response.PlayerResponse

object NewPipeUtils {
    fun getSignatureTimestamp(videoId: String): Result<Int> =
        Result.failure(UnsupportedOperationException("NewPipe extractor not configured"))

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        Result.failure(UnsupportedOperationException("NewPipe extractor not configured"))
}