package com.arturo254.opentune.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.arturo254.opentune.R
import com.arturo254.opentune.constants.AnimateLyricsKey
import com.arturo254.opentune.constants.AutoLoadMoreKey
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.MinimalPlayerDesignKey
import com.arturo254.opentune.constants.SimilarContent
import com.arturo254.opentune.ui.component.SettingsGeneralCategory
import com.arturo254.opentune.ui.component.SettingsPage
import com.arturo254.opentune.ui.component.SwitchPreference
import com.arturo254.opentune.utils.rememberPreference

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
                }
            )
        )

        SettingsGeneralCategory(
            title = stringResource(R.string.visual_effects),
            items = listOf(
                {
                    SwitchPreference(
                        title = { Text("Disable blur effects") },
                        icon = { Icon(painterResource(R.drawable.image), null) },
                        description = "Disables background blur to improve performance",
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