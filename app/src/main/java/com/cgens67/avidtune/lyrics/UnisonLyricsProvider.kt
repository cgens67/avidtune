package com.cgens67.avidtune.lyrics

import android.content.Context
import com.cgens67.avidtune.constants.EnableUnisonKey
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.get
import com.cgens67.music.betterlyrics.TTMLParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object UnisonLyricsProvider : LyricsProvider {
    override val name = "Unison"

    private const val BASE = "https://unison.boidu.dev/lyrics"
    private const val TTML = "ttml"
    private const val PLAIN = "plain"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 10000
                connectTimeoutMillis = 5000
                socketTimeoutMillis = 10000
            }
            expectSuccess = false
        }
    }

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableUnisonKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        duration: Int,
    ): Result<String> = runCatching {
        if (title.isBlank()) throw IllegalArgumentException("Title is empty")
        val durationSec = if (duration > 10000) duration / 1000 else duration

        val response = client.get(BASE) {
            parameter("song", title)
            parameter("artist", artist)
            if (durationSec > 0) parameter("duration", durationSec)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("Unison HTTP Error: ${response.status}")
        }

        val body = response.bodyAsText()
        val payload = json.decodeFromString<Response>(body)
        if (payload.success != true || payload.data == null) {
            throw IllegalStateException("No lyrics found on Unison")
        }

        parseEntry(payload.data)
            ?: throw IllegalStateException("Empty lyrics returned from Unison")
    }

    private fun parseEntry(data: Entry): String? {
        val text = data.lyrics?.takeIf { it.isNotBlank() } ?: return null
        return when {
            data.format.equals(TTML, ignoreCase = true) -> {
                val parsed = TTMLParser.parseTTML(text)
                if (parsed.isEmpty()) return null
                TTMLParser.toLRC(parsed).trim().takeIf { it.isNotBlank() }
            }
            data.syncType.equals(PLAIN, ignoreCase = true) -> {
                text.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
                    .takeIf { it.isNotBlank() }
            }
            else -> text.trim().takeIf { it.isNotBlank() }
        }
    }

    @Serializable
    private data class Response(
        val success: Boolean? = null,
        val data: Entry? = null,
    )

    @Serializable
    private data class Entry(
        val song: String? = null,
        val artist: String? = null,
        val album: String? = null,
        val lyrics: String? = null,
        val format: String? = null,
        val syncType: String? = null,
        val confidence: String? = null,
        val voteCount: Int? = null,
    )
}