package com.cgens67.avidtune.ui.component

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cgens67.avidtune.R
import com.cgens67.avidtune.db.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class ExportState { IDLE, FETCHING, DOWNLOADING, SUCCESS, ERROR, QUALITY_UNAVAILABLE }

data class StreamInfo(
    val url: String, val ext: String, val bitrate: Int, val itag: Int, val source: String,
    var sizeBytes: Long = 0L, val frontendUrl: String? = null
)

fun formatBytes(bytes: Long) = if (bytes <= 0) "Unknown Size" else String.format(Locale.US, "%.2f MB", bytes / 1048576.0)

private const val SPOOFED_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

private fun OkHttpClient.fetchJsonObj(url: String, init: Request.Builder.() -> Unit = {}): JSONObject? = try {
    newCall(Request.Builder().url(url).header("User-Agent", SPOOFED_AGENT).apply(init).build()).execute().use {
        if (it.isSuccessful) JSONObject(it.body?.string() ?: "") else null
    }
} catch (e: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportDropdown(
    label: String, selected: String, options: List<String>, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, transform: (String) -> String = { it }
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = transform(selected), onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(), colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt -> DropdownMenuItem(text = { Text(transform(opt)) }, onClick = { onSelect(opt); expanded = false }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportAudioBottomSheet(song: Song, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val dismissWithAnimation = { coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() } }
    
    var state by remember { mutableStateOf(ExportState.IDLE) }
    var progress by remember { mutableFloatStateOf(0f) }
    var downloadedBytes by remember { mutableLongStateOf(0L) }
    var errorMessage by remember { mutableStateOf("") }
    
    var selectedFormat by remember { mutableStateOf("mp3") }
    var selectedQuality by remember { mutableStateOf("Default") }
    var selectedSource by remember { mutableStateOf("Auto") }
    
    var availableStreams by remember { mutableStateOf<List<StreamInfo>>(emptyList()) }
    var currentSource by remember { mutableStateOf("") }
    var currentTotalSize by remember { mutableLongStateOf(0L) }

    val startDownload = { stream: StreamInfo ->
        state = ExportState.DOWNLOADING
        coroutineScope.launch(Dispatchers.IO) {
            val downloadClient = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(0, TimeUnit.SECONDS).retryOnConnectionFailure(true).build()
            try {
                val dReq = Request.Builder().url(stream.url).header("User-Agent", SPOOFED_AGENT).header("Connection", "keep-alive").header("Accept", "*/*").apply {
                    stream.frontendUrl?.let { header("Origin", it); header("Referer", "$it/") }
                }.build()
                
                downloadClient.newCall(dReq).execute().use { dRes ->
                    if (!dRes.isSuccessful) {
                        errorMessage = "Stream access restricted (HTTP ${dRes.code})"
                        withContext(Dispatchers.Main) { state = ExportState.ERROR }
                        return@launch
                    }
                    
                    val body = dRes.body ?: throw Exception("Empty response body")
                    val contentLength = body.contentLength().also { if (it > 0) currentTotalSize = it }
                    val inputStream = body.byteStream()
                    
                    val cleanTitle = song.song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val cleanArtist = song.artists.joinToString { it.name }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                    val ext = if (stream.ext == "webm") "opus" else stream.ext
                    val mimeType = when (ext) { "opus", "ogg" -> "audio/ogg"; "mp3" -> "audio/mpeg"; else -> "audio/mp4" }
                    
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, "$cleanTitle - $cleanArtist.$ext")
                        put(MediaStore.Audio.Media.TITLE, song.song.title)
                        put(MediaStore.Audio.Media.ARTIST, song.artists.joinToString { it.name })
                        song.album?.title?.let { put(MediaStore.Audio.Media.ALBUM, it) }
                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AvidTune")
                            put(MediaStore.Audio.Media.IS_PENDING, 1)
                        }
                    }
                    
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    
                    if (uri != null) {
                        var streamCompleted = false
                        var totalBytesRead = 0L
                        
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var lastUpdateTime = System.currentTimeMillis()
                            
                            try {
                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    val currentTime = System.currentTimeMillis()
                                    if (currentTime - lastUpdateTime > 250) {
                                        downloadedBytes = totalBytesRead
                                        if (contentLength > 0) progress = totalBytesRead.toFloat() / contentLength.toFloat()
                                        lastUpdateTime = currentTime
                                    }
                                }
                                streamCompleted = true
                            } catch (e: Exception) {
                                if ((contentLength > 0 && totalBytesRead.toFloat() / contentLength >= 0.95f) || (contentLength <= 0 && totalBytesRead > 1048576)) streamCompleted = true
                                else throw e
                            }
                            outputStream.flush()
                        }
                        
                        if (streamCompleted && totalBytesRead > 0) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                contentValues.clear(); contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                                resolver.update(uri, contentValues, null, null)
                            }
                            withContext(Dispatchers.Main) { state = ExportState.SUCCESS }
                        } else {
                            resolver.delete(uri, null, null)
                            errorMessage = if (totalBytesRead == 0L) "Stream returned 0 bytes. Proxy node might be overloaded." else "Connection closed prematurely."
                            withContext(Dispatchers.Main) { state = ExportState.ERROR }
                        }
                    } else {
                        errorMessage = "Could not create file in MediaStore"
                        withContext(Dispatchers.Main) { state = ExportState.ERROR }
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Network error"
                withContext(Dispatchers.Main) { state = ExportState.ERROR }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = { if (state != ExportState.FETCHING && state != ExportState.DOWNLOADING) onDismiss() }, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.export_to_device), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(24.dp))
            
            if (state != ExportState.QUALITY_UNAVAILABLE) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = song.song.thumbnailUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(song.song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artists.joinToString { it.name }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            AnimatedContent(targetState = state, label = "export_state") { currentState ->
                when (currentState) {
                    ExportState.IDLE -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ExportDropdown(label = stringResource(R.string.format), selected = selectedFormat, options = listOf("mp3", "m4a", "opus"), onSelect = { selectedFormat = it }, modifier = Modifier.weight(1f), transform = { it.uppercase() })
                                ExportDropdown(label = stringResource(R.string.audio_quality), selected = selectedQuality, options = listOf("Default", "Highest", "320 kbps", "256 kbps", "128 kbps", "64 kbps", "Lowest"), onSelect = { selectedQuality = it }, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(16.dp))
                            ExportDropdown(label = stringResource(R.string.api_source), selected = selectedSource, options = listOf("Auto", "Cobalt API", "Google InnerTube", "Piped API"), onSelect = { selectedSource = it }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.export_note_mp3), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(32.dp))
                            Button(
                                onClick = {
                                    state = ExportState.FETCHING
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val fetchedStreams = mutableListOf<StreamInfo>()
                                        val fetchClient = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS).build()
                                        
                                        val tryCobalt = selectedSource == "Auto" || selectedSource == "Cobalt API" || selectedFormat == "mp3"
                                        val tryInnerTube = (selectedSource == "Auto" || selectedSource == "Google InnerTube") && selectedFormat != "mp3"
                                        val tryPiped = (selectedSource == "Auto" || selectedSource == "Piped API") && selectedFormat != "mp3"

                                        try {
                                            if (tryCobalt) {
                                                val bitrateStr = when (selectedQuality) { "Highest", "320 kbps" -> "320"; "256 kbps" -> "256"; "128 kbps", "Default" -> "128"; "Lowest", "64 kbps", "48 kbps" -> "64"; else -> "128" }
                                                val payload = JSONObject().apply {
                                                    put("url", "https://www.youtube.com/watch?v=${song.song.id}"); put("downloadMode", "audio")
                                                    put("audioFormat", if (selectedFormat == "opus" || selectedFormat == "mp3") selectedFormat else "best")
                                                    put("audioBitrate", bitrateStr); put("filenameStyle", "basic")
                                                }.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                                
                                                val cobaltV11Instances = listOf("https://rue-cobalt.xenon.zone/" to "https://cobalt.xenon.zone", "https://cobaltapi.kittycat.boo/" to "https://cobalt.kittycat.boo", "https://dog.kittycat.boo/" to "https://cobalt.kittycat.boo", "https://api.cobalt.liubquanti.click/" to "https://cobalt.liubquanti.click", "https://cobaltapi.cjs.nz/" to "https://cobalt.cjs.nz").shuffled()
                                                for ((apiUrl, frontend) in cobaltV11Instances) {
                                                    fetchClient.fetchJsonObj(apiUrl) { post(payload).header("Accept", "application/json").header("Content-Type", "application/json").header("Origin", frontend).header("Referer", "$frontend/") }?.optString("url")?.takeIf { it.isNotEmpty() }?.let {
                                                        fetchedStreams.add(StreamInfo(it, selectedFormat, bitrateStr.toInt(), 0, "Cobalt API (${frontend.removePrefix("https://")})", frontendUrl = frontend))
                                                        break
                                                    }
                                                }
                                            }

                                            if (tryInnerTube && fetchedStreams.isEmpty()) {
                                                val innerTubeClients = listOf(
                                                    """{"context":{"client":{"clientName":"TVHTML5_SIMPLY_EMBEDDED_PLAYER","clientVersion":"2.0","clientScreen":"WATCH","hl":"en"},"thirdParty":{"embedUrl":"https://www.youtube.com/"}},"playbackContext":{"contentPlaybackContext":{"signatureTimestamp":19000}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"ANDROID_VR","clientVersion":"1.56.27","hl":"en"}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"IOS","clientVersion":"20.11.6","deviceMake":"Apple","deviceModel":"iPhone10,4","osName":"iOS","osVersion":"16.7.7.20H330","hl":"en"}},"videoId":"${song.song.id}"}""",
                                                    """{"context":{"client":{"clientName":"ANDROID","clientVersion":"19.30.36","androidSdkVersion":33,"osName":"Android","osVersion":"13","hl":"en"}},"videoId":"${song.song.id}"}"""
                                                )
                                                for (payload in innerTubeClients) {
                                                    val res = fetchClient.fetchJsonObj("https://www.youtube.com/youtubei/v1/player") { post(payload.toRequestBody("application/json; charset=utf-8".toMediaType())).header("Content-Type", "application/json") }
                                                    val formats = res?.optJSONObject("streamingData")?.let { it.optJSONArray("adaptiveFormats") ?: it.optJSONArray("formats") } ?: continue
                                                    for (i in 0 until formats.length()) {
                                                        val f = formats.getJSONObject(i)
                                                        if (f.has("url") && f.optString("mimeType", "").lowercase().contains("audio")) {
                                                            val ext = if (f.optString("mimeType").contains("webm") || f.optString("mimeType").contains("opus")) "opus" else "m4a"
                                                            val itag = f.optInt("itag", 0)
                                                            val bitrate = (f.optInt("bitrate", 0).takeIf { it > 0 } ?: when(itag) { 140->128000; 251->160000; 139->48000; 249->50000; 250->70000; else->128000 }) / 1000
                                                            fetchedStreams.add(StreamInfo(f.getString("url"), ext, bitrate, itag, "Google InnerTube", f.optLong("contentLength", 0L)))
                                                        }
                                                    }
                                                    if (fetchedStreams.isNotEmpty()) break
                                                }
                                            }

                                            if (tryPiped && fetchedStreams.isEmpty()) {
                                                val pipedInstances = listOf("https://pipedapi.kavin.rocks", "https://api.piped.privacydev.net", "https://piped-api.lunar.icu", "https://api-piped.mha.fi", "https://pipedapi.tokhmi.xyz")
                                                for (instance in pipedInstances) {
                                                    val streams = fetchClient.fetchJsonObj("$instance/streams/${song.song.id}") { header("Accept", "application/json") }?.optJSONArray("audioStreams") ?: continue
                                                    for (i in 0 until streams.length()) {
                                                        val s = streams.getJSONObject(i)
                                                        val ext = if (s.optString("format", "").lowercase().let { it.contains("webm") || it.contains("opus") }) "opus" else "m4a"
                                                        fetchedStreams.add(StreamInfo(s.getString("url"), ext, s.optInt("bitrate", 0) / 1000, 0, "Piped API (${instance.removePrefix("https://")})", frontendUrl = instance))
                                                    }
                                                    if (fetchedStreams.isNotEmpty()) break
                                                }
                                            }
                                            
                                            if (fetchedStreams.isEmpty()) {
                                                withContext(Dispatchers.Main) {
                                                    errorMessage = "Export failed: ${if (selectedSource == "Auto") "All extraction networks" else selectedSource} restricted or unreachable."
                                                    state = ExportState.ERROR
                                                }
                                                return@launch
                                            }
                                            
                                            val uniqueStreams = fetchedStreams.distinctBy { "${it.bitrate}_${it.ext}" }.sortedByDescending { it.bitrate }
                                            val candidatePool = uniqueStreams.filter { it.ext == selectedFormat }.ifEmpty { uniqueStreams }
                                            
                                            val match = when (selectedQuality) {
                                                "Default" -> candidatePool.find { it.itag == 140 } ?: candidatePool.maxByOrNull { it.bitrate }
                                                "Highest" -> candidatePool.maxByOrNull { it.bitrate }
                                                "Lowest" -> candidatePool.minByOrNull { it.bitrate }
                                                else -> { val target = selectedQuality.substringBefore(" ").toIntOrNull() ?: 128; candidatePool.find { abs(it.bitrate - target) <= 24 } }
                                            }
                                            
                                            if (match != null) {
                                                if (match.source == "Google InnerTube") {
                                                    try {
                                                        val testReq = Request.Builder().url(match.url).head().header("User-Agent", SPOOFED_AGENT).header("Connection", "close").build()
                                                        fetchClient.newCall(testReq).execute().use { testRes ->
                                                            if (testRes.isSuccessful) {
                                                                match.sizeBytes = testRes.header("Content-Length")?.toLongOrNull() ?: match.sizeBytes
                                                                withContext(Dispatchers.Main) { currentSource = match.source; currentTotalSize = match.sizeBytes; startDownload(match) }
                                                            } else {
                                                                withContext(Dispatchers.Main) { availableStreams = candidatePool; state = ExportState.QUALITY_UNAVAILABLE }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        withContext(Dispatchers.Main) { availableStreams = candidatePool; state = ExportState.QUALITY_UNAVAILABLE }
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) { currentSource = match.source; currentTotalSize = match.sizeBytes; startDownload(match) }
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) { availableStreams = candidatePool; state = ExportState.QUALITY_UNAVAILABLE }
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) { errorMessage = e.message ?: "Network error"; state = ExportState.ERROR }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(painterResource(R.drawable.download), contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.export), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    ExportState.QUALITY_UNAVAILABLE -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(painterResource(R.drawable.info), null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text(stringResource(R.string.quality_unavailable), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.quality_unavailable_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                                items(availableStreams) { stream ->
                                    Card(
                                        onClick = { currentSource = stream.source; currentTotalSize = stream.sizeBytes; startDownload(stream) },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text("${stream.bitrate} kbps", fontWeight = FontWeight.Bold)
                                                Text(stream.ext.uppercase(), color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(stringResource(R.string.source_format, stream.source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(formatBytes(stream.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { dismissWithAnimation() }, modifier = Modifier.weight(1f)) { Text(stringResource(android.R.string.cancel)) }
                                Button(onClick = { state = ExportState.IDLE }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.retry)) }
                            }
                        }
                    }
                    ExportState.FETCHING -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.securing_stream), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    }
                    ExportState.DOWNLOADING -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                            Text(stringResource(R.string.downloading), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            if (currentTotalSize > 0) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            else LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.fetched_by, currentSource), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (currentTotalSize > 0) "${(progress * 100).toInt()}%" else stringResource(R.string.working), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            Text(if (currentTotalSize > 0) "${formatBytes((progress * currentTotalSize).toLong())} / ${formatBytes(currentTotalSize)}" else stringResource(R.string.downloaded_size, formatBytes(downloadedBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ExportState.SUCCESS -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Box(modifier = Modifier.size(64.dp).background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.check), null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                            }
                            Text(stringResource(R.string.success_saved), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            if (currentTotalSize > 0) Text(stringResource(R.string.size_format, formatBytes(currentTotalSize)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.fetched_by, currentSource), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { dismissWithAnimation() }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text(stringResource(R.string.done)) }
                        }
                    }
                    ExportState.ERROR -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Box(modifier = Modifier.size(64.dp).background(MaterialTheme.colorScheme.errorContainer, CircleShape), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.error), null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(32.dp))
                            }
                            Text(stringResource(R.string.export_failed), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                            Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { dismissWithAnimation() }, modifier = Modifier.weight(1f)) { Text(stringResource(android.R.string.cancel)) }
                                Button(onClick = { state = ExportState.IDLE }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.retry)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
