@file:OptIn(ExperimentalMaterial3Api::class)

package com.cgens67.avidtune.ui.screens.settings

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cgens67.avidtune.NotificationPermissionPreference
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AiContentFilterEnabledKey
import com.cgens67.avidtune.constants.AiContentFilterIncludeModerateKey
import com.cgens67.avidtune.constants.ContentCountryKey
import com.cgens67.avidtune.constants.ContentLanguageKey
import com.cgens67.avidtune.constants.CountryCodeToName
import com.cgens67.avidtune.constants.DescriptionProviderOrderKey
import com.cgens67.avidtune.constants.EnableAppleMusicDescriptionsKey
import com.cgens67.avidtune.constants.EnableAvidDescriptionsKey
import com.cgens67.avidtune.constants.EnableAvidLyricsKey
import com.cgens67.avidtune.constants.EnableBetterLyricsKey
import com.cgens67.avidtune.constants.EnableBiniLyricsKey
import com.cgens67.avidtune.constants.EnableDescriptionsKey
import com.cgens67.avidtune.constants.EnableGeniusKey
import com.cgens67.avidtune.constants.EnableKugouKey
import com.cgens67.avidtune.constants.EnableLrcLibKey
import com.cgens67.avidtune.constants.EnableLrcRedKey
import com.cgens67.avidtune.constants.EnableLyricsPlusKey
import com.cgens67.avidtune.constants.EnableLyricsRomanizationKey
import com.cgens67.avidtune.constants.LyricsRomanizeCyrillicByLineKey
import com.cgens67.avidtune.constants.LyricsRomanizeLanguagesKey
import com.cgens67.avidtune.lyrics.LyricsRomanizer
import com.cgens67.avidtune.constants.EnableMusixmatchKey
import com.cgens67.avidtune.constants.EnableNetEaseKey
import com.cgens67.avidtune.constants.EnablePaxsenixKey
import com.cgens67.avidtune.constants.EnableSimpMusicKey
import com.cgens67.avidtune.constants.EnableUnisonKey
import com.cgens67.avidtune.constants.EnableWikipediaDescriptionsKey
import com.cgens67.avidtune.constants.EnableYouTubeMusicDescriptionsKey
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
import com.cgens67.avidtune.constants.ShareLinkFormat
import com.cgens67.avidtune.constants.ShareLinkFormatKey
import com.cgens67.avidtune.constants.TopSize
import com.cgens67.avidtune.ui.component.EditTextPreference
import com.cgens67.avidtune.ui.component.ListPreference
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SliderPreference
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.utils.AvidDescriptionProvider
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

    // Share link format
    val (shareFormat, onShareFormatChange) = rememberEnumPreference(
        key = ShareLinkFormatKey,
        defaultValue = ShareLinkFormat.AVIDTUNE
    )

    // Descriptions settings states
    val (enableDescriptions, onEnableDescriptionsChange) = rememberPreference(
        key = EnableDescriptionsKey,
        defaultValue = true
    )
    val (enableAvidDescriptions, onEnableAvidDescriptionsChange) = rememberPreference(
        key = EnableAvidDescriptionsKey,
        defaultValue = true
    )
    val (enableAppleMusicDescriptions, onEnableAppleMusicDescriptionsChange) = rememberPreference(
        key = EnableAppleMusicDescriptionsKey,
        defaultValue = true
    )
    val (enableWikipediaDescriptions, onEnableWikipediaDescriptionsChange) = rememberPreference(
        key = EnableWikipediaDescriptionsKey,
        defaultValue = true
    )
    val (enableYouTubeMusicDescriptions, onEnableYouTubeMusicDescriptionsChange) = rememberPreference(
        key = EnableYouTubeMusicDescriptionsKey,
        defaultValue = true
    )

    val defaultDescriptionOrder = AvidDescriptionProvider.defaultProviders
    val (descriptionOrderStr, onDescriptionOrderChange) = rememberPreference(
        DescriptionProviderOrderKey,
        defaultDescriptionOrder.joinToString(",")
    )
    val currentDescriptionOrder = remember(descriptionOrderStr) {
        descriptionOrderStr.split(",").filter { it.isNotBlank() }.let { saved ->
            val missing = defaultDescriptionOrder.filter { it !in saved }
            saved + missing
        }
    }
    var showDescriptionReorderDialog by remember { mutableStateOf(false) }

    // Lyrics provider states
    val (enableAvidLyrics, onEnableAvidLyricsChange) = rememberPreference(
        key = EnableAvidLyricsKey,
        defaultValue = true
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
    val (enableSimpMusic, onEnableSimpMusicChange) = rememberPreference(
        key = EnableSimpMusicKey,
        defaultValue = true
    )
    val (enablePaxsenix, onEnablePaxsenixChange) = rememberPreference(
        key = EnablePaxsenixKey,
        defaultValue = true
    )
    val (enableBiniLyrics, onEnableBiniLyricsChange) = rememberPreference(
        key = EnableBiniLyricsKey,
        defaultValue = true
    )
    val (enableLrcRed, onEnableLrcRedChange) = rememberPreference(
        key = EnableLrcRedKey,
        defaultValue = true
    )
    val (enableMusixmatch, onEnableMusixmatchChange) = rememberPreference(
        key = EnableMusixmatchKey,
        defaultValue = true
    )
    val (enableUnison, onEnableUnisonChange) = rememberPreference(
        key = EnableUnisonKey,
        defaultValue = true
    )
    val (enableLyricsPlus, onEnableLyricsPlusChange) = rememberPreference(
        key = EnableLyricsPlusKey,
        defaultValue = true
    )
    val (enableNetEase, onEnableNetEaseChange) = rememberPreference(
        key = EnableNetEaseKey,
        defaultValue = true
    )
    val (enableGenius, onEnableGeniusChange) = rememberPreference(
        key = EnableGeniusKey,
        defaultValue = true
    )
    val (enableLyricsRomanization, onEnableLyricsRomanizationChange) = rememberPreference(
        key = EnableLyricsRomanizationKey,
        defaultValue = false
    )
    val (romanizeCyrillicByLine, onRomanizeCyrillicByLineChange) = rememberPreference(
        key = LyricsRomanizeCyrillicByLineKey,
        defaultValue = false
    )
    val (romanizeLanguagesStr, onRomanizeLanguagesChange) = rememberPreference(
        key = LyricsRomanizeLanguagesKey,
        defaultValue = LyricsRomanizer.DEFAULT_ROMANIZE_LANGUAGES
    )
    val parsedRomanizeLanguages = remember(romanizeLanguagesStr) {
        LyricsRomanizer.parseLanguages(romanizeLanguagesStr)
    }
    var showRomanizeLanguagesSheet by remember { mutableStateOf(false) }

    val (aiContentFilterEnabled, onAiContentFilterEnabledChange) = rememberPreference(
        key = AiContentFilterEnabledKey,
        defaultValue = false
    )
    val (aiContentFilterIncludeModerate, onAiContentFilterIncludeModerateChange) = rememberPreference(
        key = AiContentFilterIncludeModerateKey,
        defaultValue = false
    )

    val defaultOrder = listOf(
        "AvidLyrics",
        "LyricsPlus",
        "Paxsenix",
        "BiniLyrics",
        "LrcRed",
        "BetterLyrics",
        "SimpMusic",
        "Musixmatch",
        "LrcLib",
        "Kugou",
        "NetEase",
        "Unison",
        "Genius",
        "YouTube Subtitle",
        "YouTube Music"
    )
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
            defaultOrder = defaultOrder,
            onDismiss = { showReorderDialog = false },
            onSave = { newOrder ->
                onProviderOrderChange(newOrder.joinToString(","))
                showReorderDialog = false
            }
        )
    }

    if (showDescriptionReorderDialog) {
        ReorderDescriptionProvidersBottomSheet(
            currentOrder = currentDescriptionOrder,
            defaultOrder = defaultDescriptionOrder,
            onDismiss = { showDescriptionReorderDialog = false },
            onSave = { newOrder ->
                onDescriptionOrderChange(newOrder.joinToString(","))
                showDescriptionReorderDialog = false
            }
        )
    }

    if (showRomanizeLanguagesSheet) {
        RomanizationLanguagesBottomSheet(
            languages = parsedRomanizeLanguages,
            onDismiss = { showRomanizeLanguagesSheet = false },
            onSave = { updated ->
                onRomanizeLanguagesChange(
                    updated.entries.joinToString(",") { (lang, enabled) -> "$lang:$enabled" }
                )
                showRomanizeLanguagesSheet = false
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
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.content_language)) },
                        icon = { Icon(painterResource(R.drawable.language), null) },
                        selectedValue = contentLanguage,
                        values = listOf(SYSTEM_DEFAULT) + LanguageCodeToName.keys.toList(),
                        valueText = {
                            LanguageCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                        },
                        onValueSelected = onContentLanguageChange,
                    )
                },
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.content_country)) },
                        icon = { Icon(painterResource(R.drawable.location_on), null) },
                        selectedValue = contentCountry,
                        values = listOf(SYSTEM_DEFAULT) + CountryCodeToName.keys.toList(),
                        valueText = {
                            CountryCodeToName.getOrElse(it) { stringResource(R.string.system_default) }
                        },
                        onValueSelected = onContentCountryChange,
                    )
                },

                // Share link format preference
                {
                    ListPreference(
                        title = { Text(stringResource(R.string.share_song_as)) },
                        icon = { Icon(painterResource(R.drawable.share), null) },
                        selectedValue = shareFormat,
                        values = listOf(
                            ShareLinkFormat.AVIDTUNE,
                            ShareLinkFormat.YOUTUBE_MUSIC,
                            ShareLinkFormat.ASK
                        ),
                        valueText = {
                            when (it) {
                                ShareLinkFormat.AVIDTUNE -> stringResource(R.string.share_format_avidtune)
                                ShareLinkFormat.YOUTUBE_MUSIC -> stringResource(R.string.share_format_youtube_music)
                                ShareLinkFormat.ASK -> stringResource(R.string.share_format_ask)
                            }
                        },
                        onValueSelected = onShareFormatChange
                    )
                },

                // Hide explicit content
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.hide_explicit)) },
                        icon = { Icon(painterResource(R.drawable.explicit), null) },
                        checked = hideExplicit,
                        onCheckedChange = onHideExplicitChange,
                    )
                },

                // Hide music videos
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.hide_music_videos)) },
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = hideMusicVideos,
                        onCheckedChange = onHideMusicVideosChange,
                    )
                },

                { NotificationPermissionPreference() },
            )
        )
        
        // AI Content Filter settings
        SettingsGeneralCategory(
            title = stringResource(R.string.ai_content_filter),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_ai_content_filter)) },
                        description = stringResource(R.string.ai_content_filter_desc),
                        icon = { Icon(painterResource(R.drawable.security), null) },
                        checked = aiContentFilterEnabled,
                        onCheckedChange = onAiContentFilterEnabledChange,
                    )
                },
                {
                    AnimatedVisibility(visible = aiContentFilterEnabled) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.include_moderate_confidence)) },
                                description = stringResource(R.string.include_moderate_confidence_desc),
                                icon = { Icon(painterResource(R.drawable.info), null) },
                                checked = aiContentFilterIncludeModerate,
                                onCheckedChange = onAiContentFilterIncludeModerateChange,
                            )
                        }
                    }
                }
            )
        )

        // Descriptions provider settings
        SettingsGeneralCategory(
            title = stringResource(R.string.description),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.descriptions_title)) },
                        description = stringResource(R.string.descriptions_desc),
                        icon = { Icon(painterResource(R.drawable.info), null) },
                        checked = enableDescriptions,
                        onCheckedChange = onEnableDescriptionsChange
                    )
                },
                {
                    AnimatedVisibility(visible = enableDescriptions) {
                        Column {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.provider_avid_descriptions)) },
                                description = stringResource(R.string.avid_descriptions_desc),
                                icon = { Icon(painterResource(R.drawable.info), null) },
                                checked = enableAvidDescriptions,
                                onCheckedChange = onEnableAvidDescriptionsChange
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.provider_apple_music)) },
                                description = stringResource(R.string.apple_music_desc),
                                icon = { Icon(painterResource(R.drawable.info), null) },
                                checked = enableAppleMusicDescriptions,
                                onCheckedChange = onEnableAppleMusicDescriptionsChange
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.provider_wikipedia)) },
                                description = stringResource(R.string.wikipedia_desc),
                                icon = { Icon(painterResource(R.drawable.language), null) },
                                checked = enableWikipediaDescriptions,
                                onCheckedChange = onEnableWikipediaDescriptionsChange
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            SwitchPreference(
                                title = { Text(stringResource(R.string.provider_youtube_music)) },
                                description = stringResource(R.string.ytm_desc),
                                icon = { Icon(painterResource(R.drawable.play), null) },
                                checked = enableYouTubeMusicDescriptions,
                                onCheckedChange = onEnableYouTubeMusicDescriptionsChange
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                            PreferenceEntry(
                                title = { Text(stringResource(R.string.description_provider_priority)) },
                                description = stringResource(R.string.description_provider_priority_desc),
                                icon = { Icon(painterResource(R.drawable.list), null) },
                                onClick = { showDescriptionReorderDialog = true }
                            )
                        }
                    }
                }
            )
        )

        // Proxy settings
        SettingsGeneralCategory(
            title = stringResource(R.string.proxy),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_proxy)) },
                        icon = { Icon(painterResource(R.drawable.wifi_proxy), null) },
                        checked = proxyEnabled,
                        onCheckedChange = onProxyEnabledChange,
                    )
                },
                {
                    if (proxyEnabled) {
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
                    }
                }
            )
        )

        // Lyrics settings
        SettingsGeneralCategory(
            title = stringResource(R.string.lyrics),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_avid_lyrics)) },
                        description = stringResource(R.string.enable_avid_lyrics_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableAvidLyrics,
                        onCheckedChange = onEnableAvidLyricsChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_lyrics_plus)) },
                        description = stringResource(R.string.enable_lyrics_plus_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableLyricsPlus,
                        onCheckedChange = onEnableLyricsPlusChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_better_lyrics)) },
                        description = stringResource(R.string.enable_better_lyrics_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableBetterLyrics,
                        onCheckedChange = onEnableBetterLyricsChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_simpmusic)) },
                        description = stringResource(R.string.enable_simpmusic_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableSimpMusic,
                        onCheckedChange = onEnableSimpMusicChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_paxsenix)) },
                        description = stringResource(R.string.enable_paxsenix_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enablePaxsenix,
                        onCheckedChange = onEnablePaxsenixChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_binilyrics)) },
                        description = stringResource(R.string.enable_binilyrics_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableBiniLyrics,
                        onCheckedChange = onEnableBiniLyricsChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_lrc_red)) },
                        description = stringResource(R.string.enable_lrc_red_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableLrcRed,
                        onCheckedChange = onEnableLrcRedChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_musixmatch)) },
                        description = stringResource(R.string.enable_musixmatch_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableMusixmatch,
                        onCheckedChange = onEnableMusixmatchChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_lrclib)) },
                        description = stringResource(R.string.enable_lrclib_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableLrclib,
                        onCheckedChange = onEnableLrclibChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_kugou)) },
                        description = stringResource(R.string.enable_kugou_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableKugou,
                        onCheckedChange = onEnableKugouChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_netease)) },
                        description = stringResource(R.string.enable_netease_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableNetEase,
                        onCheckedChange = onEnableNetEaseChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_unison)) },
                        description = stringResource(R.string.enable_unison_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableUnison,
                        onCheckedChange = onEnableUnisonChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_genius)) },
                        description = stringResource(R.string.enable_genius_desc),
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = enableGenius,
                        onCheckedChange = onEnableGeniusChange,
                    )
                },
                {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.lyrics_provider_priority)) },
                        description = stringResource(R.string.lyrics_provider_priority_desc),
                        icon = { Icon(painterResource(R.drawable.list), null) },
                        onClick = { showReorderDialog = true }
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.romanization_enable)) },
                        description = stringResource(R.string.romanization_enable_desc),
                        icon = { Icon(painterResource(R.drawable.translate), null) },
                        checked = enableLyricsRomanization,
                        onCheckedChange = onEnableLyricsRomanizationChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.romanization_cyrillic_per_line)) },
                        description = stringResource(R.string.romanization_cyrillic_per_line_desc),
                        icon = { Icon(painterResource(R.drawable.translate), null) },
                        checked = romanizeCyrillicByLine,
                        onCheckedChange = onRomanizeCyrillicByLineChange,
                    )
                },
                {
                    val enabledCount = parsedRomanizeLanguages.count { it.value }
                    val totalCount = parsedRomanizeLanguages.size
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.romanization_languages)) },
                        description = "${stringResource(R.string.romanization_languages_desc)} ($enabledCount/$totalCount)",
                        icon = { Icon(painterResource(R.drawable.language), null) },
                        onClick = { showRomanizeLanguagesSheet = true }
                    )
                }
            )
        )

        // Misc settings
        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {
                    EditTextPreference(
                        title = { Text(stringResource(R.string.top_length)) },
                        icon = { Icon(painterResource(R.drawable.trending_up), null) },
                        value = lengthTop,
                        isInputValid = { it.toIntOrNull()?.let { num -> num > 0 } == true },
                        onValueChange = onLengthTopChange,
                    )
                },
                {
                    ListPreference(
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
                    )
                },
                {
                    SliderPreference(
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
                    )
                },
            )
        )
    }
}

@Composable
private fun getLyricsProviderDescription(providerName: String): String? {
    return when (providerName) {
        "AvidLyrics" -> stringResource(R.string.enable_avid_lyrics_desc)
        "LyricsPlus" -> stringResource(R.string.enable_lyrics_plus_desc)
        "BetterLyrics" -> stringResource(R.string.enable_better_lyrics_desc)
        "SimpMusic" -> stringResource(R.string.enable_simpmusic_desc)
        "Paxsenix" -> stringResource(R.string.enable_paxsenix_desc)
        "BiniLyrics" -> stringResource(R.string.enable_binilyrics_desc)
        "LrcRed" -> stringResource(R.string.enable_lrc_red_desc)
        "Musixmatch" -> stringResource(R.string.enable_musixmatch_desc)
        "LrcLib" -> stringResource(R.string.enable_lrclib_desc)
        "Kugou" -> stringResource(R.string.enable_kugou_desc)
        "NetEase" -> stringResource(R.string.enable_netease_desc)
        "Unison" -> stringResource(R.string.enable_unison_desc)
        "Genius" -> stringResource(R.string.enable_genius_desc)
        "YouTube Subtitle" -> stringResource(R.string.enable_yt_subtitle_desc)
        "YouTube Music" -> stringResource(R.string.enable_yt_music_lyrics_desc)
        else -> null
    }
}

@Composable
private fun getDescriptionProviderDescription(providerName: String): String? {
    return when (providerName) {
        "AvidDescriptions" -> stringResource(R.string.avid_descriptions_desc)
        "Apple Music" -> stringResource(R.string.apple_music_desc)
        "Wikipedia" -> stringResource(R.string.wikipedia_desc)
        "YouTube Music" -> stringResource(R.string.ytm_desc)
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderDescriptionProvidersBottomSheet(
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
                        text = stringResource(R.string.description_provider_priority),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.description_provider_priority_desc),
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
                                    val displayName = when (item) {
                                        "AvidDescriptions" -> stringResource(R.string.provider_avid_descriptions)
                                        "Apple Music" -> stringResource(R.string.provider_apple_music)
                                        "Wikipedia" -> stringResource(R.string.provider_wikipedia)
                                        "YouTube Music" -> stringResource(R.string.provider_youtube_music)
                                        else -> item
                                    }
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    val desc = getDescriptionProviderDescription(item)
                                    if (!desc.isNullOrBlank()) {
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
private fun romanizationLanguageLabel(language: String): String =
    when (language) {
        "Japanese" -> stringResource(R.string.romanization_lang_japanese)
        "Korean" -> stringResource(R.string.romanization_lang_korean)
        "Chinese" -> stringResource(R.string.romanization_lang_chinese)
        "Arabic" -> stringResource(R.string.romanization_lang_arabic)
        "Hindi" -> stringResource(R.string.romanization_lang_hindi)
        "Punjabi" -> stringResource(R.string.romanization_lang_punjabi)
        "Thai" -> stringResource(R.string.romanization_lang_thai)
        "Russian" -> stringResource(R.string.romanization_lang_russian)
        "Ukrainian" -> stringResource(R.string.romanization_lang_ukrainian)
        "Serbian" -> stringResource(R.string.romanization_lang_serbian)
        "Bulgarian" -> stringResource(R.string.romanization_lang_bulgarian)
        "Belarusian" -> stringResource(R.string.romanization_lang_belarusian)
        "Kyrgyz" -> stringResource(R.string.romanization_lang_kyrgyz)
        "Macedonian" -> stringResource(R.string.romanization_lang_macedonian)
        else -> language
    }

private fun romanizationLanguageMeta(language: String): Pair<String, String> =
    when (language) {
        "Japanese" -> "あ" to "日本語"
        "Korean" -> "가" to "한국어"
        "Chinese" -> "中" to "中文"
        "Arabic" -> "ع" to "العربية"
        "Hindi" -> "अ" to "हिन्दी"
        "Punjabi" -> "ਅ" to "ਪੰਜਾਬੀ"
        "Thai" -> "ก" to "ภาษาไทย"
        "Russian" -> "Я" to "Русский"
        "Ukrainian" -> "Ї" to "Українська"
        "Serbian" -> "Ђ" to "Српски"
        "Bulgarian" -> "Ж" to "Български"
        "Belarusian" -> "Ў" to "Беларуская"
        "Kyrgyz" -> "Ң" to "Кыргызча"
        "Macedonian" -> "Ѓ" to "Македонски"
        else -> language.take(1).uppercase() to language
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RomanizationLanguagesBottomSheet(
    languages: Map<String, Boolean>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Boolean>) -> Unit
) {
    var selectedMap by remember(languages) { mutableStateOf(languages) }
    var searchQuery by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val maxSheetHeight = (configuration.screenHeightDp * if (isLandscape) 0.85f else 0.78f).dp

    val enabledCount = selectedMap.values.count { it }
    val totalCount = selectedMap.size
    val allEnabled = totalCount > 0 && enabledCount == totalCount

    val allEntries = selectedMap.entries.toList()
    val filteredEntries = allEntries.filter { (lang, _) ->
        if (searchQuery.isBlank()) {
            true
        } else {
            val q = searchQuery.trim()
            val (_, nativeName) = romanizationLanguageMeta(lang)
            lang.contains(q, ignoreCase = true) || nativeName.contains(q, ignoreCase = true)
        }
    }

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .nestedScroll(nestedScrollConnection)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = if (isLandscape) 14.dp else 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.translate),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.romanization_languages),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.romanization_languages_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$enabledCount/$totalCount",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Surface(
                    onClick = {
                        val target = !allEnabled
                        selectedMap = selectedMap.mapValues { target }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(if (allEnabled) R.drawable.deselect else R.drawable.select_all),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Search Bar
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.search),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Language List
            if (filteredEntries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .padding(vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.no_results_found),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.try_another_term),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 3.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    if (isLandscape) {
                        items(
                            items = filteredEntries.chunked(2),
                            key = { row -> row.joinToString("_") { it.key } }
                        ) { rowEntries ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowEntries.forEach { (language, checked) ->
                                    RomanizationLanguageCard(
                                        language = language,
                                        checked = checked,
                                        shape = RoundedCornerShape(18.dp),
                                        compact = true,
                                        onToggle = { enabled ->
                                            selectedMap = selectedMap.toMutableMap().apply {
                                                this[language] = enabled
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowEntries.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = filteredEntries,
                            key = { _, item -> item.key }
                        ) { index, (language, checked) ->
                            val isFirst = index == 0
                            val isLast = index == filteredEntries.lastIndex
                            val shape = RoundedCornerShape(
                                topStart = if (isFirst) 20.dp else 6.dp,
                                topEnd = if (isFirst) 20.dp else 6.dp,
                                bottomStart = if (isLast) 20.dp else 6.dp,
                                bottomEnd = if (isLast) 20.dp else 6.dp
                            )
                            RomanizationLanguageCard(
                                language = language,
                                checked = checked,
                                shape = shape,
                                compact = false,
                                onToggle = { enabled ->
                                    selectedMap = selectedMap.toMutableMap().apply {
                                        this[language] = enabled
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Footer Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                            onSave(selectedMap)
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RomanizationLanguageCard(
    language: String,
    checked: Boolean,
    shape: RoundedCornerShape,
    compact: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val (glyph, nativeName) = remember(language) { romanizationLanguageMeta(language) }

    val cardColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        animationSpec = tween(200),
        label = "romanization_card_bg"
    )
    val glyphBgColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = tween(200),
        label = "romanization_glyph_bg"
    )
    val glyphTextColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "romanization_glyph_fg"
    )

    Surface(
        onClick = { onToggle(!checked) },
        shape = shape,
        color = cardColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(glyphBgColor)
            ) {
                Text(
                    text = glyph,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = glyphTextColor
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = romanizationLanguageLabel(language),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = nativeName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                thumbContent = {
                    Icon(
                        painter = painterResource(id = if (checked) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                        tint = if (checked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedIconColor = MaterialTheme.colorScheme.surface,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReorderLyricsProvidersBottomSheet(
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
                        text = stringResource(R.string.lyrics_provider_priority),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.lyrics_provider_priority_desc),
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
                                    val displayName = when (item) {
                                        "AvidLyrics" -> stringResource(R.string.provider_avid_lyrics)
                                        "LyricsPlus" -> stringResource(R.string.provider_lyrics_plus)
                                        "BetterLyrics" -> stringResource(R.string.provider_better_lyrics)
                                        "SimpMusic" -> stringResource(R.string.provider_simpmusic)
                                        "Paxsenix" -> stringResource(R.string.provider_paxsenix)
                                        "BiniLyrics" -> stringResource(R.string.provider_binilyrics)
                                        "LrcRed" -> stringResource(R.string.provider_lrc_red)
                                        "Musixmatch" -> stringResource(R.string.provider_musixmatch)
                                        "LrcLib" -> stringResource(R.string.provider_lrclib)
                                        "Kugou" -> stringResource(R.string.provider_kugou)
                                        "NetEase" -> stringResource(R.string.provider_netease)
                                        "Unison" -> stringResource(R.string.provider_unison)
                                        "Genius" -> stringResource(R.string.provider_genius)
                                        "YouTube Subtitle" -> stringResource(R.string.provider_youtube_subtitle)
                                        "YouTube Music" -> stringResource(R.string.provider_youtube_music)
                                        else -> item
                                    }
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.SemiBold
                                    )
                                    val desc = getLyricsProviderDescription(item)
                                    if (!desc.isNullOrBlank()) {
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
