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
        // Clean the title to strip out typical YouTube suffixes like "(Official Video)" or "[Audio]"
        val baseTitle = title.substringBefore("(").substringBefore("[").trim()
        val cleanTitle = baseTitle.replace(Regex("[^a-zA-Z0-9 ]"), "").trim()
        
        val titleUnderscores = cleanTitle.lowercase().replace(Regex("\\s+"), "_")
        val titleNoSpaces = cleanTitle.lowercase().replace(Regex("\\s+"), "")
        val rawTitleUnderscores = title.replace(" ", "_")

        // Build a fallback priority list of possible paths
        // Exact YT video ID uses lyrics/ directory
        val possiblePaths = mutableListOf("lyrics/$id.lrc")
        
        // Title fallbacks use lyrics/sorted/ directory
        val fallbackNames = listOfNotNull(
            titleUnderscores.takeIf { it.isNotBlank() },
            titleNoSpaces.takeIf { it.isNotBlank() },
            rawTitleUnderscores.takeIf { it.isNotBlank() }
        ).distinct()
        
        for (name in fallbackNames) {
            possiblePaths.add("lyrics/sorted/$name.lrc")
        }

        for (path in possiblePaths) {
            // Replaced jsDelivr with GitHub Raw to avoid the 12-hour CDN cache on branches.
            // Appended a timestamp to completely bypass GitHub's 5-minute cache so edits show up instantly.
            val url = "https://raw.githubusercontent.com/$GITHUB_USERNAME/$GITHUB_REPO/$GITHUB_BRANCH/$path?t=${System.currentTimeMillis()}"

            try {
                val response = client.get(url)
                if (response.status == HttpStatusCode.OK) {
                    val body = response.bodyAsText()
                    if (body.isNotBlank()) {
                        return@runCatching body
                    }
                }
            } catch (e: Exception) {
                // Ignore network errors for an individual attempt, try the next one
            }
        }
        
        throw IllegalStateException("Failed to fetch from AvidLyrics: No matching lyrics found")
    }
}
