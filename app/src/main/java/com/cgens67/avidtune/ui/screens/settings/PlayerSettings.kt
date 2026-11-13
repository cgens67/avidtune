@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.media3.common.Player
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AudioFadingKey
import com.cgens67.avidtune.constants.AudioNormalizationKey
import com.cgens67.avidtune.constants.AudioQuality
import com.cgens67.avidtune.constants.AudioQualityKey
import com.cgens67.avidtune.constants.AutoSkipNextOnErrorKey
import com.cgens67.avidtune.constants.PersistentQueueKey
import com.cgens67.avidtune.constants.PlaybackClientOrderKey
import com.cgens67.avidtune.constants.SeekIncrementKey
import com.cgens67.avidtune.constants.SkipSilenceKey
import com.cgens67.avidtune.constants.SponsorBlockEnabledKey
import com.cgens67.avidtune.constants.StopMusicOnTaskClearKey
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.component.EnumListPreference
import com.cgens67.avidtune.ui.component.ListPreference
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.utils.YTPlayerUtils
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (audioQuality, onAudioQualityChange) = rememberEnumPreference(
        AudioQualityKey,
        defaultValue = AudioQuality.AUTO
    )
    val (seekIncrement, onSeekIncrementChange) = rememberPreference(
        SeekIncrementKey,
        defaultValue = 5
    )
    val (persistentQueue, onPersistentQueueChange) = rememberPreference(
        PersistentQueueKey,
        defaultValue = true
    )
    val (skipSilence, onSkipSilenceChange) = rememberPreference(
        SkipSilenceKey,
        defaultValue = false
    )
    val (sponsorBlockEnabled, onSponsorBlockEnabledChange) = rememberPreference(
        SponsorBlockEnabledKey,
        defaultValue = true
    )
    val (audioFading, onAudioFadingChange) = rememberPreference(
        AudioFadingKey,
        defaultValue = false
    )
    val (audioNormalization, onAudioNormalizationChange) = rememberPreference(
        AudioNormalizationKey,
        defaultValue = true
    )
    val (autoSkipNextOnError, onAutoSkipNextOnErrorChange) = rememberPreference(
        AutoSkipNextOnErrorKey,
        defaultValue = false
    )
    val (stopMusicOnTaskClear, onStopMusicOnTaskClearChange) = rememberPreference(
        StopMusicOnTaskClearKey,
        defaultValue = false
    )

    val (showNerdStats, onShowNerdStatsChange) = rememberPreference(
        key = booleanPreferencesKey("dev_show_nerd_stats"),
        defaultValue = false
    )

    val defaultClientOrder = YTPlayerUtils.DEFAULT_CLIENT_FALLBACK_CHAIN
    val allClientKeys = YTPlayerUtils.AVAILABLE_CLIENTS.keys.toList()

    val (clientOrderStr, onClientOrderChange) = rememberPreference(
        PlaybackClientOrderKey,
        defaultClientOrder.joinToString(",")
    )

    val currentClientOrder = remember(clientOrderStr) {
        val saved = clientOrderStr.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }
        val validSaved = saved.filter { it in allClientKeys }
        val missing = allClientKeys.filter { it !in validSaved }
        (validSaved + missing).distinct()
    }
    var showClientReorderDialog by remember { mutableStateOf(false) }

    val playerConnection = LocalPlayerConnection.current

    if (showClientReorderDialog) {
        ReorderPlaybackClientsBottomSheet(
            currentOrder = currentClientOrder,
            defaultOrder = defaultClientOrder,
            onDismiss = { showClientReorderDialog = false },
            onSave = { newOrder ->
                onClientOrderChange(newOrder.joinToString(","))
                showClientReorderDialog = false
            }
        )
    }

    SettingsPage(
        title = stringResource(R.string.player_and_audio),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {
                    EnumListPreference(
                        title = { Text(stringResource(R.string.audio_quality)) },
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        selectedValue = audioQuality,
                        onValueSelected = onAudioQualityChange,
                        valueText = {
                            when (it) {
                                AudioQuality.AUTO -> stringResource(R.string.audio_quality_auto)
                                AudioQuality.HIGH -> stringResource(R.string.audio_quality_high)
                                AudioQuality.LOW -> stringResource(R.string.audio_quality_low)
                            }
                        }
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.playback_fallback_priority)) },
                        description = stringResource(R.string.playback_fallback_priority_desc),
                        icon = { Icon(painterResource(R.drawable.list), null) },
                        onClick = { showClientReorderDialog = true }
                    )
                },
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.double_tap_to_seek)) },
                        icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                        selectedValue = seekIncrement,
                        values = listOf(5, 10, 15, 30),
                        valueText = { pluralStringResource(R.plurals.seconds, it, it) },
                        onValueSelected = onSeekIncrementChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.skip_silence)) },
                        icon = { Icon(painterResource(R.drawable.fast_forward), null) },
                        checked = skipSilence,
                        onCheckedChange = onSkipSilenceChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_sponsorblock)) },
                        description = stringResource(R.string.enable_sponsorblock_desc),
                        icon = { Icon(painterResource(R.drawable.skip_next), null) },
                        checked = sponsorBlockEnabled,
                        onCheckedChange = onSponsorBlockEnabledChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.premium_audio_fading)) },
                        description = stringResource(R.string.premium_audio_fading_desc),
                        icon = { Icon(painterResource(R.drawable.graphic_eq), null) },
                        checked = audioFading,
                        onCheckedChange = onAudioFadingChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.audio_normalization)) },
                        icon = { Icon(painterResource(R.drawable.volume_up), null) },
                        checked = audioNormalization,
                        onCheckedChange = onAudioNormalizationChange
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.queue),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.persistent_queue)) },
                        description = stringResource(R.string.persistent_queue_desc),
                        icon = { Icon(painterResource(R.drawable.queue_music), null) },
                        checked = persistentQueue,
                        onCheckedChange = onPersistentQueueChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.auto_skip_next_on_error)) },
                        description = stringResource(R.string.auto_skip_next_on_error_desc),
                        icon = { Icon(painterResource(R.drawable.skip_next), null) },
                        checked = autoSkipNextOnError,
                        onCheckedChange = onAutoSkipNextOnErrorChange
                    )
                },
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.stop_music_on_task_clear)) },
                        icon = { Icon(painterResource(R.drawable.clear_all), null) },
                        checked = stopMusicOnTaskClear,
                        onCheckedChange = onStopMusicOnTaskClearChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.show_nerd_stats)) },
                        description = stringResource(R.string.show_nerd_stats_desc),
                        icon = { Icon(painterResource(R.drawable.info), null) },
                        checked = showNerdStats,
                        onCheckedChange = onShowNerdStatsChange
                    )
                }
            )
        )

        AnimatedVisibility(
            visible = showNerdStats && playerConnection != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                NerdStatsSection(playerConnection = playerConnection)
            }
        }
    }
}

@Composable
private fun getPlaybackClientDescription(clientName: String): String {
    return when (clientName.trim().uppercase()) {
        "ANDROID_NO_SDK" -> stringResource(R.string.client_desc_android_no_sdk)
        "VISIONOS" -> stringResource(R.string.client_desc_visionos)
        "ANDROID_CREATOR" -> stringResource(R.string.client_desc_android_creator)
        "ANDROID_VR_1_43_32" -> stringResource(R.string.client_desc_android_vr_1_43_32)
        "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> stringResource(R.string.client_desc_tv_embedded)
        "WEB_REMIX" -> stringResource(R.string.client_desc_web_remix)
        "TVHTML5" -> stringResource(R.string.client_desc_tvhtml5)
        "TVHTML5_SIMPLY" -> stringResource(R.string.client_desc_tvhtml5_simply)
        "WEB_CREATOR" -> stringResource(R.string.client_desc_web_creator)
        "MWEB" -> stringResource(R.string.client_desc_mweb)
        "WEB" -> stringResource(R.string.client_desc_web)
        "IOS" -> stringResource(R.string.client_desc_ios)
        "IPADOS" -> stringResource(R.string.client_desc_ipados)
        "MOBILE" -> stringResource(R.string.client_desc_android)
        "ANDROID_VR_1_65_10" -> stringResource(R.string.client_desc_android_vr_1_65_10)
        "ANDROID_VR_1_61_48" -> stringResource(R.string.client_desc_android_vr_1_61_48)
        "ANDROID_VR_NO_AUTH" -> stringResource(R.string.client_desc_android_vr_no_auth)
        "ANDROID_VR" -> stringResource(R.string.client_desc_android_vr)
        else -> YTPlayerUtils.AVAILABLE_CLIENTS[clientName]?.friendlyName
            ?: YTPlayerUtils.AVAILABLE_CLIENTS[clientName]?.clientName
            ?: "YouTube Client"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderPlaybackClientsBottomSheet(
    currentOrder: List<String>,
    defaultOrder: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val list = remember { currentOrder.toMutableStateList() }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val item = list.removeAt(from.index)
        list.add(to.index, item)
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return Offset(0f, available.y)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        text = stringResource(R.string.playback_fallback_priority),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.playback_fallback_priority_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        list.clear()
                        list.addAll(defaultOrder)
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.replay),
                        contentDescription = stringResource(R.string.reset),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 500.dp)
            ) {
                items(list, key = { it }) { item ->
                    ReorderableItem(reorderableState, key = item) { isDragging ->
                        val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
                        val index = list.indexOf(item)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            tonalElevation = elevation,
                            shadowElevation = elevation,
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            if (index == 0) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (index == 0) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val clientObj = YTPlayerUtils.AVAILABLE_CLIENTS[item]
                                    val displayName = clientObj?.friendlyName ?: item
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    val desc = getPlaybackClientDescription(item)
                                    if (desc.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    painter = painterResource(R.drawable.drag_handle),
                                    contentDescription = stringResource(R.string.drag_to_reorder),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .draggableHandle()
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onSave(list)
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}

@Composable
private fun NerdStatsSection(playerConnection: PlayerConnection?) {
    if (playerConnection == null) return

    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playbackClient by playerConnection.currentPlaybackClient.collectAsState()
    val player = playerConnection.player

    var bufferPercentage by remember { mutableIntStateOf(0) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var playbackPitch by remember { mutableFloatStateOf(1.0f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            bufferPercentage = player.bufferedPercentage
            bufferedPosition = player.bufferedPosition
            currentPosition = player.currentPosition
            playbackSpeed = player.playbackParameters.speed
            playbackPitch = player.playbackParameters.pitch
            delay(500)
        }
    }

    val cleanClientName = remember(playbackClient) {
        playbackClient?.let { raw ->
            if (raw.contains("clientName=")) {
                raw.substringAfter("clientName=").substringBefore(",").substringBefore(")")
            } else {
                raw
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.graphic_eq),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = stringResource(R.string.nerd_stats),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.nerd_stats_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (mediaMetadata != null) {
                NerdStatCard(
                    icon = R.drawable.music_note,
                    label = stringResource(R.string.track),
                    value = mediaMetadata?.title ?: stringResource(R.string.no_track_playing),
                    accentColor = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NerdStatChip(
                        icon = R.drawable.play,
                        label = stringResource(R.string.playback_client),
                        value = cleanClientName ?: stringResource(R.string.unknown),
                        modifier = Modifier.weight(1f),
                        valueColor = MaterialTheme.colorScheme.primary
                    )

                    NerdStatChip(
                        icon = R.drawable.graphic_eq,
                        label = stringResource(R.string.codec),
                        value = currentFormat?.mimeType?.substringAfter("/")?.uppercase()
                            ?: stringResource(R.string.unknown),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (currentFormat != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val bitrateKbps = currentFormat?.bitrate?.let { it / 1000 } ?: 0
                        NerdStatChip(
                            icon = R.drawable.graphic_eq,
                            label = stringResource(R.string.bitrate),
                            value = if (bitrateKbps > 0) stringResource(R.string.format_kbps, bitrateKbps) else stringResource(R.string.unknown),
                            modifier = Modifier.weight(1f)
                        )

                        val sampleRateKhz = currentFormat?.sampleRate?.let { (it / 1000.0).roundToInt() } ?: 0
                        NerdStatChip(
                            icon = R.drawable.graphic_eq,
                            label = stringResource(R.string.sample_rate),
                            value = if (sampleRateKhz > 0) stringResource(R.string.format_khz, sampleRateKhz) else stringResource(R.string.unknown),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        NerdStatChip(
                            icon = R.drawable.storage,
                            label = stringResource(R.string.size),
                            value = currentFormat?.contentLength?.let {
                                if (it > 0) stringResource(R.string.format_mb, String.format(Locale.US, "%.2f", it / 1024.0 / 1024.0))
                                else stringResource(R.string.unknown)
                            } ?: stringResource(R.string.unknown),
                            modifier = Modifier.weight(1f)
                        )

                        val pitchVal = String.format(Locale.US, "%.2fx", playbackPitch)
                        NerdStatChip(
                            icon = R.drawable.discover_tune,
                            label = "Pitch",
                            value = pitchVal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearWavyProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                            Text(
                                text = stringResource(R.string.loading_format_details),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val bufferDuration = ((bufferedPosition - currentPosition) / 1000.0).roundToInt()
                val bufferProgress by animateFloatAsState(
                    targetValue = bufferPercentage / 100f,
                    animationSpec = tween(300),
                    label = "buffer"
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.buffer_health),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.buffer_health_stats, bufferPercentage, bufferDuration),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    LinearWavyProgressIndicator(
                        progress = { bufferProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = when {
                            bufferPercentage > 70 -> Color(0xFF43B581)
                            bufferPercentage > 30 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val playbackStateText = when (player.playbackState) {
                        Player.STATE_IDLE -> stringResource(R.string.state_idle)
                        Player.STATE_BUFFERING -> stringResource(R.string.state_buffering)
                        Player.STATE_READY -> stringResource(R.string.state_ready)
                        Player.STATE_ENDED -> stringResource(R.string.state_ended)
                        else -> stringResource(R.string.unknown)
                    }

                    val stateColor = when (player.playbackState) {
                        Player.STATE_READY -> Color(0xFF43B581)
                        Player.STATE_BUFFERING -> MaterialTheme.colorScheme.tertiary
                        Player.STATE_IDLE -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.error
                    }

                    NerdStatChip(
                        icon = R.drawable.info,
                        label = stringResource(R.string.state),
                        value = playbackStateText,
                        modifier = Modifier.weight(1f),
                        valueColor = stateColor
                    )

                    NerdStatChip(
                        icon = R.drawable.graphic_eq,
                        label = stringResource(R.string.speed),
                        value = stringResource(R.string.format_speed, playbackSpeed),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.token),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.media_id),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = mediaMetadata?.id?.take(16)?.plus("...") ?: stringResource(R.string.n_a),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.music_note),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .alpha(0.5f),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.no_track_playing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NerdStatCard(
    icon: Int,
    label: String,
    value: String,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = accentColor.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                }
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun NerdStatChip(
    icon: Int,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}