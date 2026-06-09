package com.cgens67.avidtune.models

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

@Serializable
data class SponsorSegment(
    val segment: List<Float>,
    val category: String,
    val actionType: String
)

object SponsorBlock {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        expectSuccess = false
    }

    suspend fun getSkipSegments(videoId: String): List<SponsorSegment>? {
        return try {
            val response = client.get("https://sponsor.ajay.app/api/skipSegments") {
                parameter("videoID", videoId)
                parameter("categories", """["sponsor", "intro", "outro", "interaction", "selfpromo", "music_offtopic"]""")
            }
            if (response.status.value in 200..299) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch SponsorBlock segments")
            null
        }
    }
}
