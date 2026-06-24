package com.cgens67.avidtune.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Locale

object TranslationHelper {
    private val client = HttpClient(CIO)

    suspend fun translate(text: String, targetLang: String = Locale.getDefault().toLanguageTag()): String? = runCatching {
        if (text.isBlank()) return null
        
        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var currentChunk = java.lang.StringBuilder()
        
        for (line in lines) {
            // Keep chunks safely within the API's limit.
            // Reduced to 500 to account for URL encoding bloating non-ASCII strings drastically.
            if (currentChunk.length + line.length > 500) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = java.lang.StringBuilder()
                }
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        val resultBuilder = java.lang.StringBuilder()
        for (chunk in chunks) {
            val response = client.get("https://translate.googleapis.com/translate_a/single") {
                parameter("client", "gtx")
                parameter("sl", "auto")
                parameter("tl", targetLang)
                parameter("dt", "t")
                parameter("q", chunk)
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            }.bodyAsText()

            val jsonArray = Json.parseToJsonElement(response).jsonArray
            val translatedSegments = jsonArray[0].jsonArray
            for (segment in translatedSegments) {
                resultBuilder.append(segment.jsonArray[0].jsonPrimitive.content)
            }
        }
        resultBuilder.toString().trimEnd()
    }.onFailure { Timber.e(it, "Failed to translate text") }.getOrNull()

    suspend fun romanize(lines: List<String>): List<String> = coroutineScope {
        val semaphore = Semaphore(5) // Limit concurrent requests to prevent rate limiting
        lines.map { line ->
            async {
                if (line.isBlank()) return@async ""
                semaphore.withPermit {
                    runCatching {
                        val response = client.get("https://translate.googleapis.com/translate_a/single") {
                            parameter("client", "gtx")
                            parameter("sl", "auto")
                            parameter("tl", "en")
                            parameter("dt", "rm") // rm requests transliteration/romanization
                            parameter("q", line)
                            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                        }.bodyAsText()

                        val jsonArray = Json.parseToJsonElement(response).jsonArray
                        val rmSegments = jsonArray.getOrNull(0)?.jsonArray
                        var rmText = ""
                        if (rmSegments != null && rmSegments !is JsonNull) {
                            for (segment in rmSegments) {
                                val text = segment.jsonArray.getOrNull(2)?.let { if (it is JsonNull) null else it.jsonPrimitive.contentOrNull }
                                    ?: segment.jsonArray.getOrNull(3)?.let { if (it is JsonNull) null else it.jsonPrimitive.contentOrNull }
                                
                                if (text != null && text != "null") {
                                    rmText += text
                                }
                            }
                        }
                        if (rmText.isNotBlank() && rmText != "null") rmText.trim() else line
                    }.getOrDefault(line)
                }
            }
        }.awaitAll()
    }

    suspend fun detectLanguage(text: String): String? = runCatching {
        if (text.isBlank()) return null
        val sample = text.take(200)
        val response = client.get("https://translate.googleapis.com/translate_a/single") {
            parameter("client", "gtx")
            parameter("sl", "auto")
            parameter("tl", "en")
            parameter("dt", "t")
            parameter("q", sample)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        }.bodyAsText()

        val jsonArray = Json.parseToJsonElement(response).jsonArray
        jsonArray[2].jsonPrimitive.content
    }.onFailure { Timber.e(it, "Failed to detect language") }.getOrNull()
}
