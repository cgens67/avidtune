package com.cgens67.avidtune.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Locale

object TranslationHelper {
    private val client by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
        }
    }

    fun normalizeTargetLanguage(lang: String): String {
        val clean = lang.trim().replace('_', '-')
        val lower = clean.lowercase()
        return when {
            lower.startsWith("zh-tw") || lower.startsWith("zh-hk") || lower.startsWith("zh-hant") -> "zh-TW"
            lower.startsWith("zh") -> "zh-CN"
            lower.startsWith("pt-pt") -> "pt-PT"
            lower.startsWith("pt") -> "pt"
            lower == "he" || lower == "iw" -> "iw"
            lower == "id" || lower == "in" -> "id"
            lower.contains("-") -> lower.substringBefore("-")
            else -> lower
        }
    }

    fun getDefaultTargetLanguage(): String {
        val locale = Locale.getDefault()
        val code = locale.language.ifBlank { locale.toLanguageTag() }
        return normalizeTargetLanguage(code)
    }

    private suspend fun executeTranslateRequest(chunk: String, targetLang: String): String? {
        val endpoints = listOf(
            "https://translate.googleapis.com/translate_a/single",
            "https://translate.google.com/translate_a/single",
            "https://clients5.google.com/translate_a/t"
        )

        for (endpoint in endpoints) {
            val isClients5 = endpoint.contains("clients5")
            val clientParam = if (isClients5) "dict-chrome-ex" else "gtx"

            // 1. Try POST with parameters in URL query string and text chunk in body (handles long texts without 400/414)
            val postUrl = "$endpoint?client=$clientParam&sl=auto&tl=$targetLang&dt=t"
            val postResult = runCatching {
                val response = client.post(postUrl) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    setBody(FormDataContent(parameters {
                        append("q", chunk)
                    }))
                }
                if (response.status == HttpStatusCode.OK) {
                    response.bodyAsText()
                } else null
            }.getOrNull()

            if (!postResult.isNullOrBlank()) {
                val parsed = parseTranslationResponse(postResult)
                if (!parsed.isNullOrBlank()) {
                    return parsed
                }
            }

            // 2. Try submitForm where parameters are passed in form body
            val formResult = runCatching {
                val response = client.submitForm(
                    url = endpoint,
                    formParameters = parameters {
                        append("client", clientParam)
                        append("sl", "auto")
                        append("tl", targetLang)
                        append("dt", "t")
                        append("q", chunk)
                    }
                ) {
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
                if (response.status == HttpStatusCode.OK) {
                    response.bodyAsText()
                } else null
            }.getOrNull()

            if (!formResult.isNullOrBlank()) {
                val parsed = parseTranslationResponse(formResult)
                if (!parsed.isNullOrBlank()) {
                    return parsed
                }
            }

            // 3. Fallback to GET request
            val getResult = runCatching {
                val response = client.get(endpoint) {
                    parameter("client", clientParam)
                    parameter("sl", "auto")
                    parameter("tl", targetLang)
                    parameter("dt", "t")
                    parameter("q", chunk)
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
                if (response.status == HttpStatusCode.OK) {
                    response.bodyAsText()
                } else null
            }.getOrNull()

            if (!getResult.isNullOrBlank()) {
                val parsed = parseTranslationResponse(getResult)
                if (!parsed.isNullOrBlank()) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun parseTranslationResponse(response: String): String? {
        return runCatching {
            val jsonElement = Json.parseToJsonElement(response)
            if (jsonElement is JsonObject) {
                val sentences = jsonElement["sentences"] as? JsonArray
                if (sentences != null) {
                    val sb = StringBuilder()
                    for (sentence in sentences) {
                        val trans = (sentence as? JsonObject)?.get("trans")?.jsonPrimitive?.contentOrNull
                        if (!trans.isNullOrEmpty()) {
                            sb.append(trans)
                        }
                    }
                    if (sb.isNotEmpty()) return sb.toString()
                }
            }
            val jsonArray = jsonElement as? JsonArray ?: return null
            val translatedSegments = jsonArray.getOrNull(0) as? JsonArray ?: return null
            val sb = StringBuilder()
            for (segment in translatedSegments) {
                val segmentArray = segment as? JsonArray ?: continue
                val translatedSegment = (segmentArray.getOrNull(0) as? JsonPrimitive)?.contentOrNull
                if (!translatedSegment.isNullOrEmpty()) {
                    sb.append(translatedSegment)
                }
            }
            sb.toString()
        }.onFailure { Timber.e(it, "Failed to parse translation response") }.getOrNull()
    }

    suspend fun translate(text: String, targetLang: String = getDefaultTargetLanguage()): String? = runCatching {
        if (text.isBlank()) return null
        val normalizedTarget = normalizeTargetLanguage(targetLang)

        val lines = text.split("\n")
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()

        for (line in lines) {
            if (currentChunk.length + line.length > 1000) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
            }
            currentChunk.append(line).append("\n")
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }

        val resultBuilder = StringBuilder()
        for (chunk in chunks) {
            val parsedChunk = executeTranslateRequest(chunk, normalizedTarget) ?: return null
            resultBuilder.append(parsedChunk)
            if (!parsedChunk.endsWith("\n") && !resultBuilder.endsWith("\n")) {
                resultBuilder.append("\n")
            }
        }
        resultBuilder.toString().replace("\r", "").trimEnd()
    }.onFailure { Timber.e(it, "Failed to translate text") }.getOrNull()

    suspend fun romanize(lines: List<String>): List<String> = coroutineScope {
        val semaphore = Semaphore(5)
        lines.map { line ->
            async {
                if (line.isBlank()) return@async ""
                semaphore.withPermit {
                    runCatching {
                        val response = client.get("https://translate.googleapis.com/translate_a/single") {
                            parameter("client", "gtx")
                            parameter("sl", "auto")
                            parameter("tl", "en")
                            parameter("dt", "rm")
                            parameter("q", line)
                            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                        }
                        if (response.status != HttpStatusCode.OK) return@runCatching line
                        val responseText = response.bodyAsText()

                        val jsonArray = Json.parseToJsonElement(responseText) as? JsonArray ?: return@runCatching line
                        val rmSegments = jsonArray.getOrNull(0) as? JsonArray ?: return@runCatching line
                        var rmText = ""
                        for (segment in rmSegments) {
                            val segmentArray = segment as? JsonArray ?: continue
                            val text = (segmentArray.getOrNull(2) as? JsonPrimitive)?.contentOrNull
                                ?: (segmentArray.getOrNull(3) as? JsonPrimitive)?.contentOrNull

                            if (!text.isNullOrBlank() && text != "null") {
                                rmText += text
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
        val endpoints = listOf(
            "https://translate.googleapis.com/translate_a/single",
            "https://clients5.google.com/translate_a/t"
        )
        for (endpoint in endpoints) {
            val isClients5 = endpoint.contains("clients5")
            val clientParam = if (isClients5) "dict-chrome-ex" else "gtx"
            val response = runCatching {
                client.get(endpoint) {
                    parameter("client", clientParam)
                    parameter("sl", "auto")
                    parameter("tl", "en")
                    parameter("dt", "t")
                    parameter("q", sample)
                    header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                }
            }.getOrNull()
            if (response != null && response.status == HttpStatusCode.OK) {
                val responseBody = response.bodyAsText()
                val jsonElement = runCatching { Json.parseToJsonElement(responseBody) }.getOrNull()
                if (jsonElement is JsonObject) {
                    val src = jsonElement["src"]?.jsonPrimitive?.contentOrNull
                    if (!src.isNullOrBlank()) return src
                } else if (jsonElement is JsonArray) {
                    val lang = (jsonElement.getOrNull(2) as? JsonPrimitive)?.contentOrNull
                    if (!lang.isNullOrBlank()) return lang
                }
            }
        }
        null
    }.onFailure { Timber.e(it, "Failed to detect language") }.getOrNull()
}