@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.menu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.cgens67.innertube.YouTube
import com.cgens67.innertube.models.MediaInfo

@Composable
fun MediaInfoBottomSheet(videoId: String, onDismiss: () -> Unit) = ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), contentWindowInsets = { WindowInsets(0,0,0,0) }) { ShowMediaInfo(videoId, onDismiss) }

private enum class Tab(@StringRes val label: Int) { Info(R.string.information), Details(R.string.details), Stats(R.string.numbers) }

@Composable
fun ShowMediaInfo(videoId: String, onClose: () -> Unit) {
    val ctx = LocalContext.current; val db = LocalDatabase.current; val player = LocalPlayerConnection.current?.player
    val song by db.song(videoId).collectAsState(null); val format by db.format(videoId).collectAsState(null)
    var info by remember(videoId) { mutableStateOf<MediaInfo?>(null) }; var tab by rememberSaveable { mutableStateOf(Tab.Info) }
    LaunchedEffect(videoId) { info = YouTube.getMediaInfo(videoId).getOrNull() }
    val copy = { t: String -> (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("", t)); Toast.makeText(ctx, R.string.copied, Toast.LENGTH_SHORT).show() }

    LazyColumn(Modifier.fillMaxWidth(), rememberLazyListState(), PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerHigh)) { Row(Modifier.padding(16.dp), Alignment.CenterVertically, Arrangement.spacedBy(16.dp)) { (song?.thumbnailUrl ?: info?.authorThumbnail)?.let { AsyncImage(it, null, Modifier.size(88.dp).clip(MaterialTheme.shapes.large), contentScale = ContentScale.Crop) } ?: Surface(Modifier.size(88.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Surface(Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) { Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(painterResource(R.drawable.music_note), null, tint = MaterialTheme.colorScheme.onTertiaryContainer) } } } }; Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(stringResource(R.string.information), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge); Text(song?.title ?: info?.title ?: videoId, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: info?.author ?: stringResource(R.string.unknown), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis); if (info == null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularWavyProgressIndicator(Modifier.size(20.dp)); Text(stringResource(R.string.please_wait), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; IconButton(onClose) { Icon(painterResource(R.drawable.close), stringResource(R.string.close)) } } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FilledTonalButton({ copy(videoId) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.copy), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.copy)) }; OutlinedButton({ ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=$videoId") }, null)) }, Modifier.weight(1f)) { Icon(painterResource(R.drawable.share), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.share)) } } }
        val facts = listOfNotNull(format?.mimeType?.substringBefore(';')?.takeIf { it.isNotBlank() }?.let { R.drawable.graphic_eq to it }, format?.bitrate?.takeIf { it > 0 }?.let { R.drawable.waves to "${it / 1000} Kbps" }, format?.contentLength?.takeIf { it > 0 }?.let { R.drawable.storage to Formatter.formatShortFileSize(ctx, it) }, info?.subscribers?.takeIf { it.isNotBlank() }?.let { R.drawable.person to it })
        if (facts.isNotEmpty()) item { FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { facts.forEach { (i, t) -> AssistChip({ copy(t) }, { Text(t, maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(painterResource(i), null) }, colors = AssistChipDefaults.assistChipColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)) } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) { Tab.entries.forEachIndexed { i, t -> ToggleButton(tab == t, { if (it) tab = t }, Modifier.weight(1f).height(52.dp), shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else if (i == 2) ButtonGroupDefaults.connectedTrailingButtonShapes() else ButtonGroupDefaults.connectedMiddleButtonShapes(), colors = ToggleButtonDefaults.toggleButtonColors(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)) { Text(stringResource(t.label), maxLines = 1, overflow = TextOverflow.Ellipsis) } } } }
        item {
            AnimatedContent(tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { t ->
                Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val unk = stringResource(R.string.unknown); val wait = stringResource(R.string.please_wait)
                    when (t) {
                        Tab.Info -> { InfoListCard(listOf(stringResource(R.string.song_title) to (song?.title ?: info?.title ?: unk), stringResource(R.string.song_artists) to (song?.artists?.takeIf { it.isNotEmpty() }?.joinToString { it.name } ?: info?.author ?: unk), stringResource(R.string.media_id) to videoId), copy); if (info == null) PendingCard(stringResource(R.string.description), wait) else ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.description), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); OutlinedButton({ info?.description?.takeIf { it.isNotBlank() }?.let(copy) }) { Icon(painterResource(R.drawable.copy), null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.copy)) } }; Text(info?.description?.takeIf { it.isNotBlank() } ?: unk, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
                        Tab.Details -> { val det = listOfNotNull(format?.itag?.let { "Itag" to it.toString() }, format?.mimeType?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.mime_type) to it }, format?.codecs?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.codecs) to it }, format?.bitrate?.takeIf { it > 0 }?.let { stringResource(R.string.bitrate) to "${it / 1000} Kbps" }, format?.sampleRate?.takeIf { it > 0 }?.let { stringResource(R.string.sample_rate) to "$it Hz" }, format?.loudnessDb?.let { stringResource(R.string.loudness) to "$it dB" }, player?.volume?.let { stringResource(R.string.volume) to "${(it * 100).toInt()}%" }, format?.contentLength?.takeIf { it > 0 }?.let { stringResource(R.string.file_size) to Formatter.formatShortFileSize(ctx, it) }); if (det.isEmpty()) PendingCard(stringResource(R.string.details), wait) else InfoListCard(det, copy) }
                        Tab.Stats -> { if (info == null) PendingCard(stringResource(R.string.numbers), wait) else Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { listOf(R.string.subscribers to (info?.subscribers ?: unk), R.string.views to (info?.viewCount?.let { "%,d".format(it) } ?: unk), R.string.likes to (info?.like?.let { "%,d".format(it) } ?: unk), R.string.dislikes to (info?.dislike?.let { "%,d".format(it) } ?: unk)).chunked(2).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { row.forEach { (l, v) -> ElevatedCard(Modifier.weight(1f), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(stringResource(l), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(v, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold) } } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } } } }
                    }
                }
            }
        }
    }
}

@Composable private fun InfoListCard(items: List<Pair<String, String>>, copy: (String) -> Unit) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth()) { items.forEachIndexed { i, (k, v) -> ListItem({ Text(k) }, Modifier.clickable { copy(v) }, headlineContent = { Text(v, maxLines = 2, overflow = TextOverflow.Ellipsis) }, trailingContent = { Icon(painterResource(R.drawable.copy), null) }, colors = ListItemDefaults.colors(Color.Transparent)); if (i < items.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp)) } } }
@Composable private fun PendingCard(t: String, m: String) = ElevatedCard(Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(MaterialTheme.colorScheme.surfaceContainerLow)) { Column(Modifier.fillMaxWidth().padding(20.dp), Alignment.CenterHorizontally, Arrangement.spacedBy(12.dp)) { LoadingIndicator(Modifier.size(40.dp)); Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text(m, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
