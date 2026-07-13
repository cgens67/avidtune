@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
package com.cgens67.avidtune.ui.menu

import android.content.*
import android.text.format.Formatter
import android.widget.Toast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R

@Composable
fun MediaInfoBottomSheet(videoId: String, onDismiss: () -> Unit) = ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), contentWindowInsets = { WindowInsets(0,0,0,0) }) { ShowMediaInfo(videoId, onDismiss) }

private enum class Tab(val label: String) { Info("Information"), Details("Details"), Stats("Numbers") }

@Composable
fun ColumnScope.ShowMediaInfo(videoId: String, onClose: () -> Unit) {
    val ctx = LocalContext.current; val db = LocalDatabase.current; val player = LocalPlayerConnection.current?.player
    val song by db.song(videoId).collectAsState(null); val format by db.format(videoId).collectAsState(null)
    var tab by rememberSaveable { mutableStateOf(Tab.Info) }
    val copyToClip = { t: String -> (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", t)); Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show() }
    
    val nestedScrollConnection = remember { object : NestedScrollConnection { override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource) = Offset(0f, available.y) } }

    LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().nestedScroll(nestedScrollConnection).navigationBarsPadding(), rememberLazyListState(), PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) { song?.thumbnailUrl?.let { AsyncImage(it, null, Modifier.size(88.dp).clip(MaterialTheme.shapes.large), contentScale = ContentScale.Crop) } ?: Surface(Modifier.size(88.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.onTertiaryContainer) } } } }; Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Information", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge); Text(song?.title ?: videoId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: "Unknown", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis); if (song == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularWavyProgressIndicator(Modifier.size(20.dp)); Text("Please wait...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; IconButton(onClose) { Icon(painterResource(R.drawable.close), "Close") } } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FilledTonalButton({ copyToClip(videoId) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.content_copy), null); Spacer(Modifier.width(8.dp)); Text("Copy") }; OutlinedButton({ ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId") }, null)) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.share), null); Spacer(Modifier.width(8.dp)); Text("Share") } } }
        item { val facts = listOfNotNull(format?.mimeType?.substringBefore(';')?.takeIf { it.isNotBlank() }?.let { R.drawable.graphic_eq to it }, format?.bitrate?.takeIf { it > 0 }?.let { R.drawable.waves to "${it / 1000} Kbps" }, format?.contentLength?.takeIf { it > 0 }?.let { R.drawable.storage to Formatter.formatShortFileSize(ctx, it) }); if (facts.isNotEmpty()) FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { facts.forEach { (i, t) -> AssistChip({ copyToClip(t) }, { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(painterResource(i), null) }, colors = AssistChipDefaults.assistChipColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)) } } }
        item { val visTabs = Tab.entries.filter { it != Tab.Stats /* HIDDEN */ }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) { visTabs.forEachIndexed { i, t -> ToggleButton(tab == t, { if (it) tab = t }, Modifier.weight(1f).height(52.dp), shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else if (i == visTabs.lastIndex) ButtonGroupDefaults.connectedTrailingButtonShapes() else ButtonGroupDefaults.connectedMiddleButtonShapes(), colors = ToggleButtonDefaults.toggleButtonColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)) { Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis) } } } }
        item {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { t ->
                Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val unk = "Unknown"; val wait = "Please wait..."
                    when (t) {
                        Tab.Info -> { InfoListCard(listOf("Song Title" to (song?.title ?: unk), "Artists" to (song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: unk), "Media ID" to videoId), copyToClip); if (false) /* HIDDEN */ ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); OutlinedButton({ copyToClip(unk) }) { Icon(painterResource(R.drawable.content_copy), null); Spacer(Modifier.width(8.dp)); Text("Copy") } }; Text(unk, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                        Tab.Details -> { val det = listOfNotNull(format?.itag?.let { "Itag" to it.toString() }, format?.mimeType?.takeIf { it.isNotBlank() }?.let { "MIME Type" to it }, format?.codecs?.takeIf { it.isNotBlank() }?.let { "Codecs" to it }, format?.bitrate?.takeIf { it > 0 }?.let { "Bitrate" to "${it / 1000} Kbps" }, format?.sampleRate?.takeIf { it > 0 }?.let { "Sample Rate" to "$it Hz" }, format?.loudnessDb?.let { "Loudness" to "$it dB" }, player?.volume?.let { "Volume" to "${(it * 100).toInt()}%" }, format?.contentLength?.takeIf { it > 0 }?.let { "File Size" to Formatter.formatShortFileSize(ctx, it) }); if (det.isEmpty()) PendingCard("Details", wait) else InfoListCard(det, copyToClip) }
                        Tab.Stats -> { if (false) /* HIDDEN */ Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf("Subscribers" to unk, "Views" to unk, "Likes" to unk, "Dislikes" to unk).chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { (l, v) -> ElevatedCard(Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(l, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }
                    }
                }
            }
        }
    }
}

@Composable private fun InfoListCard(items: List<Pair<String, String>>, onCopy: (String) -> Unit) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth()) { items.forEachIndexed { i, (k, v) -> ListItem(headlineContent = { Text(v, maxLines = 2, overflow = TextOverflow.Ellipsis) }, modifier = Modifier.clickable { onCopy(v) }, overlineContent = { Text(k) }, trailingContent = { Icon(painterResource(R.drawable.content_copy), null) }, colors = ListItemDefaults.colors(Color.Transparent)); if (i < items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp)) } } }
@Composable private fun PendingCard(t: String, m: String) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) { LoadingIndicator(Modifier.size(40.dp)); Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(m, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
