@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.cgens67.avidtune.ui.menu

import android.content.*
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import kotlinx.coroutines.launch

@Composable
fun MediaInfoBottomSheet(videoId: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss, 
        sheetState = sheetState, 
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) { 
        ShowMediaInfo(videoId) {
            scope.launch {
                sheetState.hide()
                onDismiss()
            }
        } 
    }
}

private enum class Tab(@StringRes val labelRes: Int) { Info(R.string.info_title), Details(R.string.details), Stats(R.string.media_info_numbers) }

@Composable
fun ColumnScope.ShowMediaInfo(videoId: String, onClose: () -> Unit) {
    val ctx = LocalContext.current; val db = LocalDatabase.current; val player = LocalPlayerConnection.current?.player
    val song by db.song(videoId).collectAsState(null); val format by db.format(videoId).collectAsState(null)
    var tab by rememberSaveable { mutableStateOf(Tab.Info) }
    val copyToClip = { t: String -> (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", t)); Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show() }
    
    val nestedScrollConnection = remember { object : NestedScrollConnection { override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = Offset(0f, available.y) } }

    LazyColumn(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .nestedScroll(nestedScrollConnection), 
        state = rememberLazyListState(), 
        contentPadding = PaddingValues(
            start = 16.dp, 
            top = 0.dp, 
            end = 16.dp, 
            bottom = 24.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        ), 
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { song?.thumbnailUrl?.let { AsyncImage(it, null, Modifier.size(88.dp).clip(MaterialTheme.shapes.large), contentScale = ContentScale.Crop) } ?: Surface(Modifier.size(88.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.onTertiaryContainer) } } } }; Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(stringResource(R.string.info_title), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge); Text(song?.title ?: videoId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: stringResource(R.string.unknown), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis); if (song == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularWavyProgressIndicator(Modifier.size(20.dp)); Text("${stringResource(R.string.please_wait)}...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; IconButton(onClose) { Icon(painterResource(R.drawable.close), stringResource(R.string.close)) } } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FilledTonalButton({ copyToClip(videoId) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.content_copy), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_copy)) }; OutlinedButton({ ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId") }, null)) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.share), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share)) } } }
        item { val facts = listOfNotNull(format?.mimeType?.substringBefore(';')?.takeIf { it.isNotBlank() }?.let { R.drawable.graphic_eq to it }, format?.bitrate?.takeIf { it > 0 }?.let { R.drawable.waves to "${it / 1000} Kbps" }, format?.contentLength?.takeIf { it > 0 }?.let { R.drawable.storage to Formatter.formatShortFileSize(ctx, it) }); if (facts.isNotEmpty()) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { facts.forEach { (i, t) -> AssistChip({ copyToClip(t) }, { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(painterResource(i), null) }, colors = AssistChipDefaults.assistChipColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)) } } }
        item { 
            val visTabs = Tab.entries.filter { it != Tab.Stats /* HIDDEN */ }
            // Wrapped the row inside a full Surface background to clean up the connected edges glitch entirely
            Surface(Modifier.fillMaxWidth().height(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(Modifier.fillMaxWidth()) { 
                    visTabs.forEach { t -> 
                        ToggleButton(
                            checked = tab == t, 
                            onCheckedChange = { if (it) tab = t }, 
                            modifier = Modifier.weight(1f).fillMaxHeight(), 
                            colors = ToggleButtonDefaults.toggleButtonColors(
                                containerColor = Color.Transparent, // Transparent clears up any background clashing
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant, 
                                checkedContainerColor = MaterialTheme.colorScheme.primaryContainer, 
                                checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) { 
                            Text(stringResource(t.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) 
                        } 
                    } 
                } 
            }
        }
        item {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { t ->
                Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val unk = stringResource(R.string.unknown); val wait = "${stringResource(R.string.please_wait)}..."
                    when (t) {
                        Tab.Info -> { CompactInfoGrid(listOf(stringResource(R.string.song_title) to (song?.title ?: unk), stringResource(R.string.artists) to (song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: unk), stringResource(R.string.media_id) to videoId), copyToClip); if (false) /* HIDDEN */ ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); OutlinedButton({ copyToClip(unk) }) { Icon(painterResource(R.drawable.content_copy), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.action_copy)) } }; Text(unk, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                        Tab.Details -> { val det = listOfNotNull(format?.itag?.let { stringResource(R.string.itag) to it.toString() }, format?.mimeType?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.mime_type) to it }, format?.codecs?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.codecs) to it }, format?.bitrate?.takeIf { it > 0 }?.let { stringResource(R.string.bitrate) to "${it / 1000} Kbps" }, format?.sampleRate?.takeIf { it > 0 }?.let { stringResource(R.string.sample_rate) to "$it Hz" }, format?.loudnessDb?.let { stringResource(R.string.loudness) to "$it dB" }, player?.volume?.let { stringResource(R.string.volume) to "${(it * 100).toInt()}%" }, format?.contentLength?.takeIf { it > 0 }?.let { stringResource(R.string.file_size) to Formatter.formatShortFileSize(ctx, it) }); if (det.isEmpty()) PendingCard(stringResource(R.string.details), wait) else CompactInfoGrid(det, copyToClip) }
                        Tab.Stats -> { if (false) /* HIDDEN */ Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf(stringResource(R.string.subscribers) to unk, stringResource(R.string.views) to unk, stringResource(R.string.likes) to unk, stringResource(R.string.dislikes) to unk).chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { (l, v) -> ElevatedCard(Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(l, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }
                    }
                }
            }
        }
    }
}

@Composable private fun CompactInfoGrid(items: List<Pair<String, String>>, onCopy: (String) -> Unit) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { items.chunked(2).forEach { r -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { r.forEach { (k, v) -> Column(Modifier.weight(1f).clip(MaterialTheme.shapes.small).clickable { onCopy(v) }) { Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(2.dp)); Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis) } }; if (r.size == 1) Spacer(Modifier.weight(1f)) } } } }
@Composable private fun PendingCard(t: String, m: String) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator(Modifier.size(40.dp)); Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(m, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
