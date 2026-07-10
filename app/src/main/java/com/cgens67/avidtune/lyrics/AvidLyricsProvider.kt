package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableAvidLyricsKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode

object AvidLyricsProvider : LyricsProvider {
    override val name = "AvidLyrics"

    private const val GITHUB_USERNAME = "cgens67"
    private const val GITHUB_REPO = "avidtune-lyrics"
    private const val GITHUB_BRANCH = "main"

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 5000
            }
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableAvidLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        // Utilizing jsDelivr CDN to handle traffic and prevent GitHub raw rate limits
        val url = "https://cdn.jsdelivr.net/gh/$GITHUB_USERNAME/$GITHUB_REPO@$GITHUB_BRANCH/lyrics/$id.lrc"

        val response = client.get(url)
        if (response.status == HttpStatusCode.OK) {
            val body = response.bodyAsText()
            if (body.isNotBlank()) {
                body
            } else {
                throw IllegalStateException("Empty lyrics file")
            }
        } else {
            throw IllegalStateException("Failed to fetch from AvidLyrics CDN: ${response.status}")
        }
    }
}
