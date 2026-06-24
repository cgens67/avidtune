package com.cgens67.avidtune.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
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
    // OkHttp is much more robust for POST form requests to Google endpoints
    private val client = HttpClient(OkHttp)

    suspend fun translate(text: String, targetLang: String = Locale.getDefault().toLanguageTag()): String? = runCatching {
        if (text.isBlank()) return null
        
        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var currentChunk = java.lang.StringBuilder()
        
        for (line in lines) {
            // Using POST allows larger chunks, keeping sentences together
            if (currentChunk.length + line.length > 2000) {
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
            // Send as POST form data to completely bypass URL length limits
            val response = client.submitForm(
                url = "https://translate.googleapis.com/translate_a/single",
                formParameters = parameters {
                    append("client", "gtx")
                    append("sl", "auto")
                    append("tl", targetLang)
                    append("dt", "t")
                    append("q", chunk)
                }
            ).bodyAsText()

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
                        val response = client.submitForm(
                            url = "https://translate.googleapis.com/translate_a/single",
                            formParameters = parameters {
                                append("client", "gtx")
                                append("sl", "auto")
                                append("tl", "en")
                                append("dt", "rm") // rm requests transliteration/romanization
                                append("q", line)
                            }
                        ).bodyAsText()

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
        val response = client.submitForm(
            url = "https://translate.googleapis.com/translate_a/single",
            formParameters = parameters {
                append("client", "gtx")
                append("sl", "auto")
                append("tl", "en")
                append("dt", "t")
                append("q", sample)
            }
        ).bodyAsText()

        val jsonArray = Json.parseToJsonElement(response).jsonArray
        jsonArray[2].jsonPrimitive.content
    }.onFailure { Timber.e(it, "Failed to detect language") }.getOrNull()
}
