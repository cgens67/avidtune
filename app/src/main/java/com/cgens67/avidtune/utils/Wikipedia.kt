package com.cgens67.avidtune.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber
import io.ktor.client.plugins.ResponseException

/**
 * Utility for fetching summaries from Wikipedia via the REST API.
 */
object Wikipedia {
    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
            expectSuccess = true
        }
    }

    @Serializable
    private data class WikiSummary(val extract: String? = null, val type: String? = null)

    private suspend fun fetchPageSummary(title: String, lang: String): WikiSummary? = runCatching {
        val encodedTitle = title.replace(" ", "_").encodeURLQueryComponent()
        client.get("https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle")
            .body<WikiSummary>()
    }.onFailure {
        if (it is ResponseException && it.response.status == HttpStatusCode.NotFound) {
            Timber.d("No Wikipedia summary found for: $title")
        } else {
            Timber.w("Failed to fetch Wikipedia summary for: $title (${it.message})")
        }
    }.getOrNull()

    /**
     * Attempts to find the Wikipedia summary for a specific album.
     *
     * It uses a heuristic approach to find the correct page:
     * 1.  **Precise Query**: Tries "Album (Artist album)" or "Album (Artist)".
     * 2.  **Generic Query**: Tries "Album (album)" or just "Album".
     *
     * To avoid incorrect matches (e.g., getting a "Greatest Hits" page for a different artist),
     * generic results are validated to ensure they mention the artist's name.
     *
     * @param albumTitle The title of the album.
     * @param artistName The name of the artist (optional but recommended for accuracy).
     * @param lang The language code to fetch the summary in (e.g., "en", "es").
     * @return The extract/summary text if found, or null.
     */
    suspend fun fetchAlbumInfo(albumTitle: String, artistName: String?, lang: String = "en"): String? {
        // Localized album suffixes to improve non-English matches
        val albumSuffixes = listOf("album", "álbum", "EP", "mixtape", "음반", "アルバム", "专辑")

        if (artistName != null) {
            val preciseQueries = mutableListOf<String>()
            albumSuffixes.forEach { preciseQueries.add("$albumTitle ($artistName $it)") }
            preciseQueries.add("$albumTitle ($artistName)")
            
            for (query in preciseQueries) {
                val summary = fetchPageSummary(query, lang)
                if (summary != null && summary.type != "disambiguation" && !summary.extract.isNullOrBlank()) {
                    return summary.extract
                }
            }
        }

        val genericQueries = mutableListOf<String>()
        albumSuffixes.forEach { genericQueries.add("$albumTitle ($it)") }
        genericQueries.add(albumTitle)

        for (query in genericQueries) {
            val summary = fetchPageSummary(query, lang)
            if (summary != null && summary.type != "disambiguation" && !summary.extract.isNullOrBlank()) {
                // If we know the artist, ensure the summary actually mentions them.
                // This prevents "Greatest Hits" returning the wrong album.
                if (artistName != null) {
                    if (summary.extract.contains(artistName, ignoreCase = true)) {
                        return summary.extract
                    }
                } else {
                    return summary.extract
                }
            }
        }

        return null
    }

    suspend fun fetchPlaylistInfo(playlistTitle: String, lang: String = "en"): String? {
        val summary = fetchPageSummary(playlistTitle, lang)
        return if (summary?.type != "disambiguation") summary?.extract else null
    }
}
