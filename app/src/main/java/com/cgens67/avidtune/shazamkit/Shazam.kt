package com.cgens67.avidtune.shazamkit

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.CRC32
import kotlin.math.*
import kotlin.random.Random

data class ShazamSignature(val uri: String, val sampleDurationMs: Long)
data class RecognitionResult(val trackId: String, val title: String, val artist: String, val album: String?, val coverArtUrl: String?, val coverArtHqUrl: String?, val genre: String?, val releaseDate: String?, val label: String?, val lyrics: List<String>?, val shazamUrl: String?, val appleMusicUrl: String?, val spotifyUrl: String?, val isrc: String?, val youtubeVideoId: String? = null)

object Shazam {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeRequests = AtomicInteger(0)
    private var lastRequestTime = 0L
    private val requestMutex = Mutex()
    private val requestQueue = ConcurrentLinkedQueue<PendingRequest>()
    private val resultCache = ConcurrentHashMap<String, CachedResult>()
    private var nextRequestId = 0L
    private var isProcessingQueue = false
    private val userAgents = listOf("Dalvik/2.1.0 (Linux; U; Android 5.0.2; VS980 4G Build/LRX22G)", "Dalvik/1.6.0 (Linux; U; Android 4.4.2; SM-T210 Build/KOT49H)", "Dalvik/2.1.0 (Linux; U; Android 5.1.1; SM-P905V Build/LMY47X)", "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)", "Dalvik/2.1.0 (Linux; U; Android 5.0; SM-G900F Build/LRX21T)")
    private val timezones = listOf("Europe/Paris", "Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Tokyo", "Asia/Dubai")

    suspend fun recognize(signature: String, sampleDurationMs: Long): Result<RecognitionResult> {
        val cacheKey = signature.hashCode().toString()
        resultCache[cacheKey]?.let { if (System.currentTimeMillis() - it.timestamp <= 300000L) return Result.success(it.result) else resultCache.remove(cacheKey) }
        return requestMutex.withLock {
            if (requestQueue.size >= 50) return Result.failure(Exception("Request queue is full. Please wait."))
            val request = PendingRequest(nextRequestId++, signature, sampleDurationMs).also { requestQueue.offer(it) }
            if (!isProcessingQueue) { isProcessingQueue = true; processQueue() }
            request.awaitResult()
        }
    }

    private suspend fun processQueue() {
        while (true) {
            val request = requestQueue.poll() ?: break
            while (activeRequests.get() >= 2) delay(100)
            activeRequests.incrementAndGet()
            scope.launch {
                try {
                    var lastException: Exception? = null
                    for (attempt in 0 until 3) {
                        try {
                            val timeSinceLast = System.currentTimeMillis() - lastRequestTime
                            if (timeSinceLast < 1000L) delay(1000L - timeSinceLast)
                            lastRequestTime = System.currentTimeMillis()
                            val result = performRecognition(request.signature, request.sampleDurationMs)
                            resultCache[request.signature.hashCode().toString()] = CachedResult(System.currentTimeMillis(), result)
                            if (resultCache.size >= 100) resultCache.entries.removeIf { System.currentTimeMillis() - it.value.timestamp > 300000L }
                            request.completeWith(Result.success(result)); return@launch
                        } catch (e: Exception) {
                            lastException = e
                            if (e.message?.contains("429") == true || e.message?.contains("Too many requests", true) == true) {
                                if (attempt < 2) delay(2000L * (1 shl attempt)) else throw e
                            } else throw e
                        }
                    }
                    throw lastException ?: Exception("Recognition failed")
                } catch (e: Exception) { request.completeWith(Result.failure(e)) } finally { activeRequests.decrementAndGet() }
            }
        }
        isProcessingQueue = false
    }

    private fun performRecognition(signature: String, sampleDurationMs: Long): RecognitionResult {
        val ts = System.currentTimeMillis() / 1000
        val payload = JSONObject().apply {
            put("geolocation", JSONObject().put("altitude", Random.nextDouble() * 400 + 100).put("latitude", Random.nextDouble() * 180 - 90).put("longitude", Random.nextDouble() * 360 - 180))
            put("signature", JSONObject().put("samplems", sampleDurationMs).put("timestamp", ts).put("uri", signature))
            put("timestamp", ts).put("timezone", timezones.random())
        }
        val conn = URL("https://amp.shazam.com/discovery/v5/en/US/android/-/tag/${UUID.randomUUID().toString().uppercase()}/${UUID.randomUUID()}?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true&video=v3").openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = "POST"; doOutput = true; readTimeout = 30000; connectTimeout = 30000
            setRequestProperty("User-Agent", userAgents.random())
            setRequestProperty("Content-Language", "en_US")
            setRequestProperty("Content-Type", "application/json")
            outputStream.use { it.write(payload.toString().toByteArray()) }
        }
        when (conn.responseCode) {
            429 -> throw Exception("Too many requests")
            404 -> throw Exception("No match found")
            in 500..599 -> throw Exception("Shazam service temporarily unavailable")
            !in 200..299 -> throw Exception("Recognition failed (error ${conn.responseCode})")
        }
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        val track = json.optJSONObject("track") ?: throw Exception("No match found")
        val metadata = track.optJSONArray("sections")?.let { arr -> (0 until arr.length()).map { arr.getJSONObject(it) }.find { it.optString("type") == "SONG" }?.optJSONArray("metadata") }
        fun meta(key: String) = metadata?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.find { o -> o.optString("title") == key }?.optString("text") }
        val actions = track.optJSONObject("hub")?.optJSONArray("options")?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.find { o -> o.optString("providername").contains("apple", true) }?.optJSONArray("actions") }
        val youtubeUri = track.optJSONObject("hub")?.optJSONArray("options")?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.find { o -> o.optString("type").contains("video", true) }?.optJSONArray("actions") }?.optJSONObject(0)?.optString("uri")
        return RecognitionResult(
            trackId = track.optString("key").takeIf { it.isNotEmpty() } ?: json.optString("tagid"),
            title = track.optString("title"), artist = track.optString("subtitle"), album = meta("Album"), label = meta("Label"), releaseDate = meta("Released"),
            coverArtUrl = track.optJSONObject("images")?.optString("coverart"), coverArtHqUrl = track.optJSONObject("images")?.optString("coverarthq"),
            genre = track.optJSONObject("genres")?.optString("primary"), lyrics = track.optJSONArray("sections")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) }.find { it.optString("type") == "LYRICS" }?.optJSONArray("text")?.let { t -> List(t.length()) { t.getString(it) } } },
            shazamUrl = track.optString("url"), appleMusicUrl = actions?.optJSONObject(0)?.optString("uri"), spotifyUrl = track.optJSONObject("hub")?.optJSONArray("providers")?.let { (0 until it.length()).map { i -> it.getJSONObject(i) }.find { o -> o.optString("caption").contains("spotify", true) }?.optJSONArray("actions") }?.optJSONObject(0)?.optString("uri"),
            isrc = track.optString("isrc"), youtubeVideoId = youtubeUri?.substringAfterLast("v=", "")?.takeIf { it.isNotEmpty() } ?: youtubeUri?.substringAfterLast("/", "")?.takeIf { it.length == 11 }
        )
    }

    private class PendingRequest(val id: Long, val signature: String, val sampleDurationMs: Long) {
        private var result: Result<RecognitionResult>? = null; private var isCompleted = false
        suspend fun awaitResult(): Result<RecognitionResult> { while (!isCompleted) delay(50); return result ?: Result.failure(Exception("Result not received")) }
        fun completeWith(res: Result<RecognitionResult>) { result = res; isCompleted = true }
    }
    private data class CachedResult(val timestamp: Long, val result: RecognitionResult)
    fun getPendingRequestsCount() = requestQueue.size
    fun getActiveRequestsCount() = activeRequests.get()
    fun clearCache() = resultCache.clear()
    fun cancelPendingRequests() = requestQueue.clear()
    fun cleanup() { cancelPendingRequests(); clearCache() }
}

class ShazamSignatureGenerator(private val maxTimeSeconds: Double = 3.1, private val maxPeaks: Int = 255) {
    private val pending = IntArrayList()
    private var processedSamples = 0
    private val sampleRateHz = 16000
    private val fft = Fft(2048)
    private val window = DoubleArray(2048) { 0.5 - 0.5 * cos(2.0 * PI * (it + 1) / 2049) }
    private val realBuffer = DoubleArray(2048); private val imagBuffer = DoubleArray(2048)
    private val ringSamples = IntArray(2048)
    private var ringSamplePos = 0; private var fftPos = 0; private var fftWritten = 0
    private val fftOutputs = Array(256) { DoubleArray(1025) }
    private var spreadPos = 0; private var spreadWritten = 0
    private val spreadOutputs = Array(256) { DoubleArray(1025) }
    private var signatureNumberSamples = 0
    private val bandToPeaks = linkedMapOf<Int, MutableList<FrequencyPeak>>()

    fun feedPcm16Mono(samples: ShortArray) = samples.forEach { pending.add(it.toInt()) }
    fun reset() { pending.clear(); processedSamples = 0; ringSamples.fill(0); ringSamplePos = 0; fftPos = 0; fftWritten = 0; spreadPos = 0; spreadWritten = 0; signatureNumberSamples = 0; bandToPeaks.clear() }

    fun nextSignatureOrNull(): ShazamSignature? {
        if (pending.size - processedSamples < 128) return null
        while (pending.size - processedSamples >= 128 && (signatureNumberSamples.toDouble() / sampleRateHz < maxTimeSeconds || bandToPeaks.values.sumOf { it.size } < maxPeaks)) {
            val end = processedSamples + 128
            for (pos in processedSamples until end step 128) {
                for (i in pos until pos + 128) { ringSamples[ringSamplePos++] = pending[i]; if (ringSamplePos == 2048) ringSamplePos = 0 }
                var p = ringSamplePos
                for (idx in 0 until 2048) { if (p == 2048) p = 0; realBuffer[idx] = ringSamples[p++] * window[idx]; imagBuffer[idx] = 0.0 }
                fft.fft(realBuffer, imagBuffer)
                for (k in 0..1024) { val v = (realBuffer[k] * realBuffer[k] + imagBuffer[k] * imagBuffer[k]) / 131072.0; fftOutputs[fftPos][k] = if (v <= 1e-10) 1e-10 else v }
                fftPos = (fftPos + 1) % 256; fftWritten++
                
                val origin = fftOutputs[(fftPos - 1).floorMod(256)]
                val originSpread = DoubleArray(1025).apply { for(i in 0..1021) this[i] = max(origin[i], max(origin[i + 1], origin[i + 2])); this[1022] = origin[1022]; this[1023] = origin[1023]; this[1024] = origin[1024] }
                val s1 = spreadOutputs[(spreadPos - 1).floorMod(256)]; val s2 = spreadOutputs[(spreadPos - 3).floorMod(256)]; val s3 = spreadOutputs[(spreadPos - 6).floorMod(256)]
                for (bin in 0..1024) { s1[bin] = max(originSpread[bin], s1[bin]); s2[bin] = max(s1[bin], s2[bin]); s3[bin] = max(s2[bin], s3[bin]) }
                System.arraycopy(originSpread, 0, spreadOutputs[spreadPos], 0, 1025)
                spreadPos = (spreadPos + 1) % 256; spreadWritten++

                if (spreadWritten >= 46) {
                    val fftMinus46 = fftOutputs[(fftPos - 46).floorMod(256)]
                    val spreadMinus49 = spreadOutputs[(spreadPos - 49).floorMod(256)]
                    for (bin in 10..1014) {
                        val energy = fftMinus46[bin]
                        if (energy >= (1.0 / 64.0) && energy >= spreadMinus49[bin - 1] && energy > intArrayOf(-10, -7, -4, -3, 1, 2, 5, 8).maxOf { spreadMinus49[bin + it] } && energy > intArrayOf(-53, -45, 165, 172, 179, 186, 193, 200, 214, 221, 228, 235, 242, 249).maxOf { spreadOutputs[(spreadPos + it).floorMod(256)][bin - 1] }) {
                            val peakMag = ln(max(1.0 / 64.0, energy)) * 1477.3 + 6144.0
                            val var1 = peakMag * 2.0 - (ln(max(1.0 / 64.0, fftMinus46[bin - 1])) * 1477.3 + 6144.0) - (ln(max(1.0 / 64.0, fftMinus46[bin + 1])) * 1477.3 + 6144.0)
                            if (var1 > 0.0) {
                                val freqHz = (bin * 64.0 + ((ln(max(1.0 / 64.0, fftMinus46[bin + 1])) * 1477.3 + 6144.0) - (ln(max(1.0 / 64.0, fftMinus46[bin - 1])) * 1477.3 + 6144.0)) * 32.0 / var1) * (sampleRateHz / 2.0 / 1024.0 / 64.0)
                                val band = when { freqHz in 250.0..520.0 -> 0; freqHz > 520.0 && freqHz <= 1450.0 -> 1; freqHz > 1450.0 && freqHz <= 3500.0 -> 2; freqHz > 3500.0 && freqHz <= 5500.0 -> 3; else -> null }
                                if (band != null) bandToPeaks.getOrPut(band) { mutableListOf() }.add(FrequencyPeak(spreadWritten - 46, peakMag.toInt(), (bin * 64.0 + ((ln(max(1.0 / 64.0, fftMinus46[bin + 1])) * 1477.3 + 6144.0) - (ln(max(1.0 / 64.0, fftMinus46[bin - 1])) * 1477.3 + 6144.0)) * 32.0 / var1).toInt()))
                            }
                        }
                    }
                }
            }
            processedSamples += 128
        }
        if (bandToPeaks.isEmpty()) return null
        val sig = ShazamSignature("data:audio/vnd.shazam.sig;base64," + Base64.getEncoder().encodeToString(ByteArrayOutputStream().apply {
            val cont = ByteArrayOutputStream().apply {
                bandToPeaks.toSortedMap().forEach { (band, peaks) ->
                    val pBytes = ByteArrayOutputStream().apply { var last = 0; peaks.forEach { val d = it.fftPassNumber - last; if(d >= 255) { write(0xFF); writeLittleInt(it.fftPassNumber); last = it.fftPassNumber }; write((if(d>=255) 0 else d).coerceAtLeast(0).coerceAtMost(254)); writeLittleShort(it.peakMagnitude); writeLittleShort(it.correctedPeakFrequencyBin); last = it.fftPassNumber } }
                    writeLittleInt(0x60030040 + band); writeLittleInt(pBytes.size()); write(pBytes.toByteArray()); repeat((-pBytes.size()).floorMod(4)) { write(0) }
                }
            }
            write(ByteBuffer.allocate(48).order(ByteOrder.LITTLE_ENDIAN).apply { putInt(0xCAFE2580.toInt()); putInt(0); putInt(cont.size() + 8); putInt(0x94119C00.toInt()); putInt(0); putInt(0); putInt(0); putInt(3 shl 27); putInt(0); putInt(0); putInt((signatureNumberSamples + sampleRateHz * 0.24).toInt()); putInt((15 shl 19) + 0x40000) }.array())
            writeLittleInt(0x40000000); writeLittleInt(cont.size() + 8); write(cont.toByteArray())
        }.toByteArray().also { bytes -> val crc = CRC32().apply { update(bytes, 8, bytes.size - 8) }.value.toInt(); ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(4, crc) }), (signatureNumberSamples * 1000L) / sampleRateHz)
        reset(); return sig
    }
    private data class FrequencyPeak(val fftPassNumber: Int, val peakMagnitude: Int, val correctedPeakFrequencyBin: Int)
}
private fun Int.floorMod(mod: Int) = (this % mod).let { if (it < 0) it + mod else it }
private class IntArrayList(initialCapacity: Int = 8192) { var array = IntArray(initialCapacity); var size = 0; operator fun get(index: Int) = array[index]; fun add(v: Int) { if (size == array.size) array = array.copyOf(size * 2); array[size++] = v }; fun clear() { size = 0 } }
private fun ByteArrayOutputStream.writeLittleInt(v: Int) { write(v and 0xFF); write((v ushr 8) and 0xFF); write((v ushr 16) and 0xFF); write((v ushr 24) and 0xFF) }
private fun ByteArrayOutputStream.writeLittleShort(v: Int) { write(v and 0xFF); write((v ushr 8) and 0xFF) }
private class Fft(private val n: Int) {
    private val cosTable = DoubleArray(n / 2) { cos(2.0 * PI * it / n) }; private val sinTable = DoubleArray(n / 2) { sin(2.0 * PI * it / n) }; private val bitReversal = IntArray(n) { i -> var x = i; var y = 0; repeat(Integer.numberOfTrailingZeros(n)) { y = (y shl 1) or (x and 1); x = x ushr 1 }; y }
    fun fft(real: DoubleArray, imag: DoubleArray) {
        for (i in 0 until n) { val j = bitReversal[i]; if (j > i) { val tr = real[i]; real[i] = real[j]; real[j] = tr; val ti = imag[i]; imag[i] = imag[j]; imag[j] = ti } }
        var len = 2
        while (len <= n) {
            for (i in 0 until n step len) { var k = 0; for (j in 0 until len / 2) { val i1 = i + j; val i2 = i1 + len / 2; val r2 = real[i2]; val im2 = imag[i2]; val tpre = r2 * cosTable[k] + im2 * sinTable[k]; val tpim = -r2 * sinTable[k] + im2 * cosTable[k]; real[i2] = real[i1] - tpre; imag[i2] = imag[i1] - tpim; real[i1] += tpre; imag[i1] += tpim; k += n / len } }
            len = len shl 1
        }
    }
}
