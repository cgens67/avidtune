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
        // Replaced jsDelivr with GitHub Raw to avoid the 12-hour CDN cache on branches.
        // Appended a timestamp to completely bypass GitHub's 5-minute cache so edits show up instantly.
        val url = "https://raw.githubusercontent.com/$GITHUB_USERNAME/$GITHUB_REPO/$GITHUB_BRANCH/lyrics/$id.lrc?t=${System.currentTimeMillis()}"

        val response = client.get(url)
        if (response.status == HttpStatusCode.OK) {
            val body = response.bodyAsText()
            if (body.isNotBlank()) {
                body
            } else {
                throw IllegalStateException("Empty lyrics file")
            }
        } else {
            throw IllegalStateException("Failed to fetch from AvidLyrics: ${response.status}")
        }
    }
}
