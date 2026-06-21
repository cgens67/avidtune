package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cgens67.avidtune.NotificationPermissionPreference
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.ContentCountryKey
import com.cgens67.avidtune.constants.ContentLanguageKey
import com.cgens67.avidtune.constants.CountryCodeToName
import com.cgens67.avidtune.constants.EnableKugouKey
import com.cgens67.avidtune.constants.EnableLrcLibKey
import com.cgens67.avidtune.constants.EnableBetterLyricsKey
import com.cgens67.avidtune.constants.EnableLyricsPlusKey
import com.cgens67.avidtune.constants.EnablePaxsenixKey
import com.cgens67.avidtune.constants.HideExplicitKey
import com.cgens67.avidtune.constants.HideMusicVideosKey
import com.cgens67.avidtune.constants.HistoryDuration
import com.cgens67.avidtune.constants.LanguageCodeToName
import com.cgens67.avidtune.constants.LyricsProviderOrderKey
import com.cgens67.avidtune.constants.ProxyEnabledKey
import com.cgens67.avidtune.constants.ProxyTypeKey
import com.cgens67.avidtune.constants.ProxyUrlKey
import com.cgens67.avidtune.constants.QuickPicks
import com.cgens67.avidtune.constants.QuickPicksKey
import com.cgens67.avidtune.constants.SYSTEM_DEFAULT
import com.cgens67.avidtune.constants.TopSize
import com.cgens67.avidtune.ui.component.EditTextPreference
import com.cgens67.avidtune.ui.component.ListPreference
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SliderPreference
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.net.Proxy
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (contentLanguage, onContentLanguageChange) = rememberPreference(
        key = ContentLanguageKey,
        defaultValue = "system"
    )
    val (contentCountry, onContentCountryChange) = rememberPreference(
        key = ContentCountryKey,
        defaultValue = "system"
    )
    val (hideExplicit, onHideExplicitChange) = rememberPreference(
        key = HideExplicitKey,
        defaultValue = false
    )
    val (hideMusicVideos, onHideMusicVideosChange) = rememberPreference(
        key = HideMusicVideosKey,
        defaultValue = false
    )
    val (proxyEnabled, onProxyEnabledChange) = rememberPreference(
        key = ProxyEnabledKey,
        defaultValue = false
    )
    val (proxyType, onProxyTypeChange) = rememberEnumPreference(
        key = ProxyTypeKey,
        defaultValue = Proxy.Type.HTTP
    )
    val (proxyUrl, onProxyUrlChange) = rememberPreference(
        key = ProxyUrlKey,
        defaultValue = "host:port"
    )
    val (lengthTop, onLengthTopChange) = rememberPreference(
        key = TopSize,
        defaultValue = "50"
    )
    val (historyDuration, onHistoryDurationChange) = rememberPreference(
        key = HistoryDuration,
        defaultValue = 30f
    )
    val (quickPicks, onQuickPicksChange) = rememberEnumPreference(
        key = QuickPicksKey,
        defaultValue = QuickPicks.QUICK_PICKS
    )
    val (enableKugou, onEnableKugouChange) = rememberPreference(
        key = EnableKugouKey,
        defaultValue = true
    )
    val (enableLrclib, onEnableLrclibChange) = rememberPreference(
        key = EnableLrcLibKey,
        defaultValue = true
    )
    val (enableBetterLyrics, onEnableBetterLyricsChange) = rememberPreference(
        key = EnableBetterLyricsKey,
        defaultValue = true
    )
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(
        key = EnablePaxsenixKey,
        defaultValue = true
    )
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(
        key = EnableLyricsPlusKey,
        defaultValue = true
    )

    val defaultOrder = listOf("LyricsPlus", "Paxsenix", "BetterLyrics", "LrcLib", "Kugou", "YouTube Subtitle", "YouTube Music")
    val (providerOrderStr, onProviderOrderChange) = rememberPreference(LyricsProviderOrderKey, defaultOrder.joinToString(","))
    val currentOrder = remember(providerOrderStr) {
        providerOrderStr.split(",").filter { it.isNotBlank() }.let { saved ->
            val missing = defaultOrder.filter { it !in saved }
            saved + missing
        }
    }
    var showReorderDialog by remember { mutableStateOf(false) }

    if (showReorderDialog) {
        ReorderLyricsProvidersBottomSheet(
            currentOrder = currentOrder,
            onDismiss = { showReorderDialog = false },
            onSave = { newOrder ->
                onProviderOrderChange(newOrder.joinToString(","))
                showReorderDialog = false
            }
        )
    }

    SettingsPage(
        title = stringResource(R.string.content),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        // General settings
        SettingsGeneralCategory(
            title = stringResource(R.string.general),
            items = listOf(
                {ListPreference(
                    title = { Text(stringResource(R.string.content_language)) },
                    icon = { Icon(painterResource(R.drawable.language), null) },
                    selectedValue = contentLanguage,
                    values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                    valueText = {
                        LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentLanguageChange,
                )},
                {ListPreference(
                    title = { Text(stringResource(R.string.content_country)) },
                    icon = { Icon(painterResource(R.drawable.location_on), null) },
                    selectedValue = contentCountry,
                    values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                    valueText = {
                        CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                    },
                    onValueSelected = onContentCountryChange,
                )},

                // Hide explicit content
                {SwitchPreference(
                    title = { Text(stringResource(R.string.hide_explicit)) },
                    icon = { Icon(painterResource(R.drawable.explicit), null) },
                    checked = hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )},

                // Hide music videos
                {SwitchPreference(
                    title = { Text(stringResource(R.string.hide_music_videos)) },
                    icon = { Icon(painterResource(R.drawable.play), null) },
                    checked = hideMusicVideos,
                    onCheckedChange = onHideMusicVideosChange,
                )},

                {NotificationPermissionPreference()},
            )
        )

        // Proxy settings
        SettingsGeneralCategory(
            title = stringResource(R.string.proxy),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_proxy)) },
                    icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                    checked = proxyEnabled,
                    onCheckedChange = onProxyEnabledChange,
                )},
                {if (proxyEnabled) {
                    Column {
                        ListPreference(
                            title = { Text(stringResource(R.string.proxy_type)) },
                            selectedValue = proxyType,
                            values = listOf(Proxy.Type.HTTP, Proxy.Type.SOCKS),
                            valueText = { it.name },
                            onValueSelected = onProxyTypeChange,
                        )
                        EditTextPreference(
                            title = { Text(stringResource(R.string.proxy_url)) },
                            value = proxyUrl,
                            onValueChange = onProxyUrlChange,
                        )
                    }
                }}
            )
        )

        // Lyrics settings
        SettingsGeneralCategory(
            title = stringResource(R.string.lyrics),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_lyrics_plus)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableLyricsPlus,
                    onCheckedChange = onEnableLyricsPlusChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_better_lyrics)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableBetterLyrics,
                    onCheckedChange = onEnableBetterLyricsChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_paxsenix)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enablePaxsenix,
                    onCheckedChange = onEnablePaxsenixChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_lrclib)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableLrclib,
                    onCheckedChange = onEnableLrclibChange,
                )},
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_kugou)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = enableKugou,
                    onCheckedChange = onEnableKugouChange,
                )},
                {PreferenceEntry(
                    title = { Text(stringResource(R.string.lyrics_provider_priority)) },
                    description = stringResource(R.string.lyrics_provider_priority_desc),
                    icon = { Icon(painterResource(R.drawable.list), null) },
                    onClick = { showReorderDialog = true }
                )}
            )
        )

        // Misc settings
        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {EditTextPreference(
                    title = { Text(stringResource(R.string.top_length)) },
                    icon = { Icon(painterResource(R.drawable.trending_up), null) },
                    value = lengthTop,
                    isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                    onValueChange = onLengthTopChange,
                )},
                {ListPreference(
                    title = { Text(stringResource(R.string.set_quick_picks)) },
                    icon = { Icon(painterResource(R.drawable.home_outlined), null) },
                    selectedValue = quickPicks,
                    values = listOf(QuickPicks.QUICK_PICKS, QuickPicks.LAST_LISTEN),
                    valueText = {
                        when (it) {
                            QuickPicks.QUICK_PICKS -> stringResource(R.string.quick_picks)
                            QuickPicks.LAST_LISTEN -> stringResource(R.string.last_song_listened)
                        }
                    },
                    onValueSelected = onQuickPicksChange,
                )},
                {SliderPreference(
                    title = { Text(stringResource(R.string.history_duration)) },
                    icon = { Icon(painterResource(R.drawable.history), null) },
                    value = historyDuration,
                    onValueChange = onHistoryDurationChange,
                    dialogTitle = stringResource(R.string.history_duration),
                    valueText = {
                        Text(
                            text = pluralStringResource(
                                R.plurals.seconds,
                                it.roundToInt(),
                                it.roundToInt()
                            ),
                            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        )
                    }
                )},
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderLyricsProvidersBottomSheet(
    currentOrder: List<String>,
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
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
                        text = stringResource(R.string.provider_priority),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.lyrics_provider_priority_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

            HorizontalDivider()

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
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
                                // Priority Number Badge
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
                                
                                // Provider Name
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Drag Handle
                                Icon(
                                    painter = painterResource(R.drawable.drag_handle),
                                    contentDescription = "Drag",
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
        }
    }
}
