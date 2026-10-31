package com.cgens67.avidtune.utils

import android.content.Context
import com.cgens67.avidtune.constants.EnableAppleMusicDescriptionsKey
import com.cgens67.avidtune.constants.EnableAvidDescriptionsKey
import com.cgens67.avidtune.constants.EnableDescriptionsKey
import com.cgens67.avidtune.constants.EnableWikipediaDescriptionsKey
import com.cgens67.avidtune.constants.EnableYouTubeMusicDescriptionsKey
import com.cgens67.avidtune.constants.DescriptionProviderOrderKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.first
import timber.log.Timber

object AvidDescriptionProvider {
    private const val GITHUB_USER = "cgens67"
    private const val GITHUB_REPO = "avidtune-descriptions"
    private const val GITHUB_BRANCH = "main"

    val defaultProviders = listOf("AvidDescriptions", "Apple Music", "Wikipedia", "YouTube Music")

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 6000
                connectTimeoutMillis = 4000
            }
            expectSuccess = false
        }
    }

    suspend fun isMasterEnabled(context: Context): Boolean =
        context.dataStore.data.first()[EnableDescriptionsKey] ?: true

    suspend fun isAvidEnabled(context: Context): Boolean =
        context.dataStore.data.first()[EnableAvidDescriptionsKey] ?: true

    suspend fun isAppleMusicEnabled(context: Context): Boolean =
        context.dataStore.data.first()[EnableAppleMusicDescriptionsKey] ?: true

    suspend fun isWikipediaEnabled(context: Context): Boolean =
        context.dataStore.data.first()[EnableWikipediaDescriptionsKey] ?: true

    suspend fun isYouTubeMusicEnabled(context: Context): Boolean =
        context.dataStore.data.first()[EnableYouTubeMusicDescriptionsKey] ?: true

    suspend fun getOrderedProviders(context: Context): List<String> {
        val saved = context.dataStore.data.first()[DescriptionProviderOrderKey] ?: return defaultProviders
        val savedList = saved.split(",").filter { it.isNotBlank() }
        val missing = defaultProviders.filter { it !in savedList }
        return savedList + missing
    }

    private fun normalize(str: String): String {
        val clean = str.substringBefore("(").substringBefore("[").trim()
        return clean.lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    /**
     * Resolves album description with the requested path hierarchy:
     * 1. albums/artists/{artist}/{playlistId}.txt (e.g. albums/artists/michael_jackson/OLAK5uy_l1U925dsiDi2DqlG-KCbODG6BaibpxbQE.txt)
     * 2. albums/artists/{artist}/{album_title}.txt (e.g. albums/artists/michael_jackson/thriller.txt)
     * 3. albums/artists/{artist}/{albumId}.txt
     * 4. albums/{playlistId}.txt
     * 5. albums/{album_title}.txt
     * 6. albums/{albumId}.txt
     */
    suspend fun getAlbumDescription(
        albumId: String,
        playlistId: String?,
        albumTitle: String,
        artistName: String?
    ): String? = runCatching {
        val paths = mutableListOf<String>()

        val normTitle = normalize(albumTitle)
        val fullTitle = albumTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')

        if (!artistName.isNullOrBlank()) {
            val normArtist = normalize(artistName)
            if (!playlistId.isNullOrBlank()) {
                paths.add("albums/artists/$normArtist/$playlistId.txt")
            }
            if (normTitle.isNotBlank()) {
                paths.add("albums/artists/$normArtist/$normTitle.txt")
            }
            if (fullTitle.isNotBlank() && fullTitle != normTitle) {
                paths.add("albums/artists/$normArtist/$fullTitle.txt")
            }
            if (albumId.isNotBlank()) {
                paths.add("albums/artists/$normArtist/$albumId.txt")
            }
        }

        // Global fallback without artist folder
        if (!playlistId.isNullOrBlank()) {
            paths.add("albums/$playlistId.txt")
        }
        if (normTitle.isNotBlank()) {
            paths.add("albums/$normTitle.txt")
        }
        if (albumId.isNotBlank()) {
            paths.add("albums/$albumId.txt")
        }

        for (path in paths.distinct()) {
            val url = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/$GITHUB_BRANCH/$path?t=${System.currentTimeMillis()}"
            try {
                val res = client.get(url)
                if (res.status == HttpStatusCode.OK) {
                    val body = res.bodyAsText().trim()
                    if (body.isNotBlank()) {
                        Timber.tag("AvidDescriptions").d("Found album description at $path")
                        return@runCatching body
                    }
                }
            } catch (_: Exception) {
            }
        }
        null
    }.getOrNull()

    /**
     * Resolves artist description with the requested path hierarchy:
     * 1. artists/{channelId}.txt (e.g. artists/UCoIOOL7QKuBhQHVKL8y7BEQ.txt)
     * 2. artists/{artistId}.txt
     * 3. artists/{artistName}.txt (e.g. artists/michael_jackson.txt)
     */
    suspend fun getArtistDescription(
        artistId: String,
        channelId: String?,
        artistName: String
    ): String? = runCatching {
        val paths = mutableListOf<String>()

        if (!channelId.isNullOrBlank()) {
            paths.add("artists/$channelId.txt")
        }
        if (artistId.isNotBlank()) {
            paths.add("artists/$artistId.txt")
        }
        val normArtist = normalize(artistName)
        if (normArtist.isNotBlank()) {
            paths.add("artists/$normArtist.txt")
        }

        for (path in paths.distinct()) {
            val url = "https://raw.githubusercontent.com/$GITHUB_USER/$GITHUB_REPO/$GITHUB_BRANCH/$path?t=${System.currentTimeMillis()}"
            try {
                val res = client.get(url)
                if (res.status == HttpStatusCode.OK) {
                    val body = res.bodyAsText().trim()
                    if (body.isNotBlank()) {
                        Timber.tag("AvidDescriptions").d("Found artist description at $path")
                        return@runCatching body
                    }
                }
            } catch (_: Exception) {
            }
        }
        null
    }.getOrNull()
}
