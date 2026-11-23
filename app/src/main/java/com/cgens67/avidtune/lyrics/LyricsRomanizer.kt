package com.cgens67.avidtune.lyrics

import com.cgens67.avidtune.App
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import meow.nyx.glyphmind.DictionaryPacks
import meow.nyx.glyphmind.LyricsRomanizer as GlyphmindRomanizer
import meow.nyx.glyphmind.WordTimestamp as GlyphmindWord
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File

object LyricsRomanizer {
    const val DEFAULT_ROMANIZE_LANGUAGES =
        "Japanese:true,Korean:true,Chinese:true,Arabic:true,Hindi:true,Punjabi:true,Thai:true,Russian:true,Ukrainian:true,Serbian:true,Bulgarian:true,Belarusian:true,Kyrgyz:true,Macedonian:true"

    private val rules = GlyphmindRomanizer()
    private val prepareMutex = Mutex()

    private val httpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 30_000
            }
        }
    }

    private val stored by lazy {
        val storageDir = File(App.instance.noBackupFilesDir, "glyphmind").absolutePath
        GlyphmindRomanizer(
            DictionaryPacks(storageDir) { pack ->
                httpClient
                    .prepareGet(pack.url) {
                        timeout {
                            requestTimeoutMillis = 60_000
                            socketTimeoutMillis = 30_000
                        }
                    }.execute { response ->
                        check(response.status.isSuccess()) { "Dictionary request failed: ${response.status}" }
                        checkNotNull(response.bodyAsBytesLimited(pack.size.toInt())) { "Dictionary exceeds pinned size" }
                    }
            },
        )
    }

    private fun engine(): GlyphmindRomanizer =
        try {
            stored
        } catch (_: Exception) {
            rules
        }

    fun parseLanguages(storedValue: String): Map<String, Boolean> {
        val result = parseRawLanguages(DEFAULT_ROMANIZE_LANGUAGES).toMutableMap()
        parseRawLanguages(storedValue).forEach { (language, enabled) ->
            if (language in result) {
                result[language] = enabled
            }
        }
        return result
    }

    fun enabledLanguageSet(storedValue: String): Set<String> =
        parseLanguages(storedValue).filterValues { it }.keys

    private fun parseRawLanguages(value: String): Map<String, Boolean> =
        value.split(',')
            .mapNotNull { part ->
                val pieces = part.split(':', limit = 2)
                val language = pieces.firstOrNull()?.trim()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                language to (pieces.getOrNull(1)?.toBooleanStrictOrNull() ?: true)
            }.toMap(LinkedHashMap())

    suspend fun prepareDictionariesIfNeeded(
        fullText: String,
        enabledLanguages: Set<String>,
    ): Boolean = withContext(Dispatchers.Default) {
        if (rules.requiredDictionaries(fullText, enabledLanguages).isEmpty()) {
            return@withContext false
        }
        prepareMutex.withLock {
            try {
                stored.prepareDictionariesIfNeeded(fullText, enabledLanguages).also { ready ->
                    if (ready) {
                        File(App.instance.noBackupFilesDir, "japanese-dictionary").deleteRecursively()
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.tag("LyricsRomanizer").w(error, "Glyphmind dictionary preparation failed")
                false
            }
        }
    }

    suspend fun romanize(
        fullText: String,
        line: String,
        enabledLanguages: Set<String>,
        cyrillicByLine: Boolean,
    ): String? = engine().romanize(fullText, line, enabledLanguages, cyrillicByLine)

    suspend fun romanizeWords(
        fullText: String,
        line: String,
        words: List<WordTimestamp>,
        enabledLanguages: Set<String>,
        cyrillicByLine: Boolean,
        isBackground: Boolean = false,
    ): Pair<String, List<WordTimestamp>>? =
        engine()
            .romanizeWords(
                fullText = fullText,
                line = line,
                words = words.map { it.toGlyphmind(isBackground) },
                enabledLanguages = enabledLanguages,
                cyrillicByLine = cyrillicByLine,
            )?.let { (text, romanized) ->
                text to words.mapIndexed { index, word -> word.copy(text = romanized[index].text) }
            }

    suspend fun romanizeLines(
        lines: List<String>,
        enabledLanguages: Set<String>,
        cyrillicByLine: Boolean,
    ): List<String> = withContext(Dispatchers.Default) {
        if (lines.isEmpty() || enabledLanguages.isEmpty()) {
            return@withContext List(lines.size) { "" }
        }
        val fullText = lines.joinToString("\n")
        prepareDictionariesIfNeeded(fullText, enabledLanguages)
        lines.map { line ->
            if (line.isBlank()) {
                ""
            } else {
                val romanized = romanize(fullText, line, enabledLanguages, cyrillicByLine)
                if (!romanized.isNullOrBlank() && romanized.trim() != line.trim()) {
                    romanized
                } else {
                    ""
                }
            }
        }
    }
}

private suspend fun HttpResponse.bodyAsBytesLimited(maxBytes: Int): ByteArray? {
    val declaredLength = headers[HttpHeaders.ContentLength]?.toLongOrNull()
    if (declaredLength != null && declaredLength > maxBytes) return null
    val channel = bodyAsChannel()
    val output = ByteArrayOutputStream(maxBytes.coerceAtMost(1024 * 1024))
    val buffer = ByteArray(8192)
    var total = 0L
    while (!channel.isClosedForRead) {
        val read = channel.readAvailable(buffer, 0, buffer.size)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        if (read > 0) output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

private fun WordTimestamp.toGlyphmind(isBackground: Boolean) =
    GlyphmindWord(text, startTime, endTime, hasTrailingSpace, isBackground, false)