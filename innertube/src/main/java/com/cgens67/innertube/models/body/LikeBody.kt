package com.cgens67.innertube.models.body

import com.cgens67.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class LikeBody(
    val context: Context,
    val target: Target,
) {
    @Serializable
    data class Target(
        val videoId: String? = null,
        val playlistId: String? = null,
    ) {
        companion object {
            fun video(id: String) = Target(videoId = id)
            fun playlist(id: String) = Target(playlistId = id)
        }
    }
}