package com.cgens67.avidtune.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AnimateLyricsKey
import com.cgens67.avidtune.constants.AutoLoadMoreKey
import com.cgens67.avidtune.constants.CoverResolution
import com.cgens67.avidtune.constants.CoverResolutionKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.constants.MinimalPlayerDesignKey
import com.cgens67.avidtune.constants.SimilarContent
import com.cgens67.avidtune.ui.component.ListDialog
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (minimalPlayerDesign, onMinimalPlayerDesignChange) = rememberPreference(
        key = MinimalPlayerDesignKey,
        defaultValue = false
    )
    val (coverResolution, onCoverResolutionChange) = rememberEnumPreference(
        key = CoverResolutionKey,
        defaultValue = CoverResolution.RES_1080
    )
    val (disableBlur, onDisableBlurChange) = rememberPreference(
        key = DisableBlurKey,
        defaultValue = false
    )
    val (animateLyrics, onAnimateLyricsChange) = rememberPreference(
        AnimateLyricsKey,
        defaultValue = true
    )
    val (autoLoadMore, onAutoLoadMoreChange) = rememberPreference(
        AutoLoadMoreKey,
        defaultValue = true
    )
    val (similarContentEnabled, similarContentEnabledChange) = rememberPreference(
        key = SimilarContent,
        defaultValue = true
    )

    var showResolutionDialog by rememberSaveable { mutableStateOf(false) }

    if (showResolutionDialog) {
        ListDialog(
            onDismiss = { showResolutionDialog = false }
        ) {
            items(CoverResolution.entries.toList()) { res ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showResolutionDialog = false
                            onCoverResolutionChange(res)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    RadioButton(
                        selected = res == coverResolution,
                        onClick = null,
                    )
                    Text(
                        text = when (res) {
                            CoverResolution.RES_1200 -> stringResource(R.string.resolution_1200)
                            CoverResolution.RES_1080 -> stringResource(R.string.resolution_1080)
                            CoverResolution.RES_800 -> stringResource(R.string.resolution_800)
                            CoverResolution.RES_500 -> stringResource(R.string.resolution_500)
                            CoverResolution.RES_300 -> stringResource(R.string.resolution_300)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
    }

    SettingsPage(
        title = stringResource(R.string.performance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.minimal_player_design)) },
                        icon = { Icon(painterResource(R.drawable.play), null) },
                        checked = minimalPlayerDesign,
                        onCheckedChange = onMinimalPlayerDesignChange
                    )
                },
                {
                    val currentResText = when (coverResolution) {
                        CoverResolution.RES_1200 -> stringResource(R.string.resolution_1200)
                        CoverResolution.RES_1080 -> stringResource(R.string.resolution_1080)
                        CoverResolution.RES_800 -> stringResource(R.string.resolution_800)
                        CoverResolution.RES_500 -> stringResource(R.string.resolution_500)
                        CoverResolution.RES_300 -> stringResource(R.string.resolution_300)
                    }

                    PreferenceEntry(
                        title = { Text(stringResource(R.string.cover_resolution)) },
                        description = "${stringResource(R.string.cover_resolution_desc)}\n$currentResText",
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        onClick = { showResolutionDialog = true }
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.visual_effects),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.disable_blur_effects)) },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        description = stringResource(R.string.disable_blur_effects_desc),
                        checked = disableBlur,
                        onCheckedChange = onDisableBlurChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.animate_lyrics)) },
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        description = stringResource(R.string.animate_lyrics_desc),
                        checked = animateLyrics,
                        onCheckedChange = onAnimateLyricsChange
                    )
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.network),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.auto_load_more)) },
                        description = stringResource(R.string.auto_load_more_desc),
                        icon = { Icon(painterResource(R.drawable.playlist_add), null) },
                        checked = autoLoadMore,
                        onCheckedChange = onAutoLoadMoreChange
                    )
                },
                {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.enable_similar_content)) },
                        description = stringResource(R.string.similar_content_desc),
                        icon = { Icon(painterResource(R.drawable.similar), null) },
                        checked = similarContentEnabled,
                        onCheckedChange = similarContentEnabledChange
                    )
                }
            )
        )
    }
}
