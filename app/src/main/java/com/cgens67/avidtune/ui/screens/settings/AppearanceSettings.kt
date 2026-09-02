@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.screens.settings

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.AppFontKey
import com.cgens67.avidtune.constants.AppTextSize
import com.cgens67.avidtune.constants.AppTextSizeKey
import com.cgens67.avidtune.constants.ArtistCanvasProviderOrderKey
import com.cgens67.avidtune.constants.ChipSortTypeKey
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.DefaultOpenTabKey
import com.cgens67.avidtune.constants.DynamicThemeKey
import com.cgens67.avidtune.constants.EnableAppleMusicCanvasKey
import com.cgens67.avidtune.constants.EnableArtistCanvasKey
import com.cgens67.avidtune.constants.EnableAvidCanvasKey
import com.cgens67.avidtune.constants.GridItemSize
import com.cgens67.avidtune.constants.GridItemsSizeKey
import com.cgens67.avidtune.constants.LibraryFilter
import com.cgens67.avidtune.constants.LyricsClickKey
import com.cgens67.avidtune.constants.LyricsTextPositionKey
import com.cgens67.avidtune.constants.MiniPlayerStyle
import com.cgens67.avidtune.constants.MiniPlayerStyleKey
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.PlayerButtonsStyle
import com.cgens67.avidtune.constants.PlayerButtonsStyleKey
import com.cgens67.avidtune.constants.PlayerTextAlignmentKey
import com.cgens67.avidtune.constants.PureBlackKey
import com.cgens67.avidtune.constants.SliderStyle
import com.cgens67.avidtune.constants.SliderStyleKey
import com.cgens67.avidtune.constants.SlimNavBarKey
import com.cgens67.avidtune.constants.SwipeThumbnailKey
import com.cgens67.avidtune.constants.UseSystemFontKey
import com.cgens67.avidtune.ui.component.AvatarSelector
import com.cgens67.avidtune.ui.component.DefaultDialog
import com.cgens67.avidtune.ui.component.EnumListPreference
import com.cgens67.avidtune.ui.component.IconButton
import com.cgens67.avidtune.ui.component.LanguagePreference
import com.cgens67.avidtune.ui.component.ListPreference
import com.cgens67.avidtune.ui.component.PlayerSliderTrack
import com.cgens67.avidtune.ui.component.PreferenceEntry
import com.cgens67.avidtune.ui.component.SettingsGeneralCategory
import com.cgens67.avidtune.ui.component.SettingsPage
import com.cgens67.avidtune.ui.component.SwitchPreference
import com.cgens67.avidtune.ui.component.ThumbnailCornerRadiusSelectorButton
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.ui.theme.googleSansBold
import com.cgens67.avidtune.ui.theme.sfProDisplayBold
import com.cgens67.avidtune.ui.theme.spaceGroteskBold
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import timber.log.Timber

@Composable
fun AppearanceSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val (dynamicTheme, onDynamicThemeChange) = rememberPreference(
        DynamicThemeKey,
        defaultValue = true
    )
    val (playerTextAlignment, onPlayerTextAlignmentChange) =
        rememberEnumPreference(
            PlayerTextAlignmentKey,
            defaultValue = PlayerTextAlignment.CENTER,
        )

    val (darkMode, onDarkModeChange) = rememberEnumPreference(
        DarkModeKey,
        defaultValue = DarkMode.AUTO
    )

    val (playerButtonsStyle, onPlayerButtonsStyleChange) = rememberEnumPreference(
        PlayerButtonsStyleKey,
        defaultValue = PlayerButtonsStyle.DEFAULT
    )
    val (playerBackground, onPlayerBackgroundChange) =
        rememberEnumPreference(
            PlayerBackgroundStyleKey,
            defaultValue = PlayerBackgroundStyle.DEFAULT,
        )
    val (pureBlack, onPureBlackChange) = rememberPreference(PureBlackKey, defaultValue = false)

    val (miniPlayerStyle, onMiniPlayerStyleChange) = rememberEnumPreference(
        key = MiniPlayerStyleKey,
        defaultValue = MiniPlayerStyle.DEFAULT
    )
    
    val (useSystemFont, _) = rememberPreference(UseSystemFontKey, defaultValue = false)
    val (appFontStr, onAppFontStrChange) = rememberPreference(AppFontKey, defaultValue = "")
    
    val appFont = remember(useSystemFont, appFontStr) {
        if (appFontStr.isNotEmpty()) {
            try { AppFont.valueOf(appFontStr) } catch(e: Exception) { AppFont.SYSTEM }
        } else {
            if (useSystemFont) AppFont.SYSTEM else AppFont.SF_PRO
        }
    }
    
    val onAppFontChange: (AppFont) -> Unit = { newFont ->
        onAppFontStrChange(newFont.name)
    }
    
    val (appTextSize, onAppTextSizeChange) = rememberEnumPreference(
        AppTextSizeKey,
        defaultValue = AppTextSize.SYSTEM
    )

    val (defaultOpenTab, onDefaultOpenTabChange) = rememberEnumPreference(
        DefaultOpenTabKey,
        defaultValue = NavigationTab.HOME
    )
    val (lyricsPosition, onLyricsPositionChange) = rememberEnumPreference(
        LyricsTextPositionKey,
        defaultValue = LyricsPosition.CENTER
    )
    val (lyricsClick, onLyricsClickChange) = rememberPreference(LyricsClickKey, defaultValue = true)
    val (sliderStyle, onSliderStyleChange) = rememberEnumPreference(
        SliderStyleKey,
        defaultValue = SliderStyle.SQUIGGLY
    )
    val (swipeThumbnail, onSwipeThumbnailChange) = rememberPreference(
        SwipeThumbnailKey,
        defaultValue = true
    )
    val (gridItemSize, onGridItemSizeChange) = rememberEnumPreference(
        GridItemsSizeKey,
        defaultValue = GridItemSize.BIG
    )

    val (enableArtistCanvas, onEnableArtistCanvasChange) = rememberPreference(
        EnableArtistCanvasKey,
        defaultValue = true
    )
    val (enableAppleMusicCanvas, onEnableAppleMusicCanvasChange) = rememberPreference(EnableAppleMusicCanvasKey, true)
    val (enableAvidCanvas, onEnableAvidCanvasChange) = rememberPreference(EnableAvidCanvasKey, true)
    
    val defaultCanvasOrder = listOf("AvidCanvas", "Apple Music")
    val (canvasProviderOrderStr, onCanvasProviderOrderChange) = rememberPreference(ArtistCanvasProviderOrderKey, defaultCanvasOrder.joinToString(","))
    
    val currentCanvasOrder = remember(canvasProviderOrderStr) {
        canvasProviderOrderStr.split(",").filter { it.isNotBlank() }.let { saved ->
            val missing = defaultCanvasOrder.filter { it !in saved }
            saved + missing
        }
    }
    var showCanvasReorderDialog by remember { mutableStateOf(false) }

    val (slimNav, onSlimNavChange) = rememberPreference(SlimNavBarKey, defaultValue = false)

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme =
        remember(darkMode, isSystemInDarkTheme) {
            if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
        }

    // Automatically disable pureBlack when switching to light mode
    LaunchedEffect(useDarkTheme) {
        if (!useDarkTheme && pureBlack) {
            onPureBlackChange(false)
        }
    }

    val (defaultChip, onDefaultChipChange) = rememberEnumPreference(
        key = ChipSortTypeKey,
        defaultValue = LibraryFilter.LIBRARY
    )

    var showSliderOptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSliderOptionDialog) {
        DefaultDialog(
            buttons = {
                TextButton(
                    onClick = { showSliderOptionDialog = false }
                ) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            },
            onDismiss = {
                showSliderOptionDialog = false
            }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.DEFAULT)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.default_),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SQUIGGLY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SQUIGGLY)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    SquigglySlider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(R.string.squiggly),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .aspectRatio(1f)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            1.dp,
                            if (sliderStyle == SliderStyle.SLIM) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            onSliderStyleChange(SliderStyle.SLIM)
                            showSliderOptionDialog = false
                        }
                        .padding(16.dp)
                ) {
                    var sliderValue by remember {
                        mutableFloatStateOf(0.5f)
                    }
                    Slider(
                        value = sliderValue,
                        valueRange = 0f..1f,
                        onValueChange = {
                            sliderValue = it
                        },
                        thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                        track = { sliderState ->
                            PlayerSliderTrack(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {}
                                )
                            }
                    )

                    Text(
                        text = stringResource(R.string.slim),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    if (showCanvasReorderDialog) {
        ReorderCanvasProvidersBottomSheet(
            currentOrder = currentCanvasOrder,
            onDismiss = { showCanvasReorderDialog = false },
            onSave = { newOrder ->
                onCanvasProviderOrderChange(newOrder.joinToString(","))
                com.cgens67.avidtune.ui.component.ArtistCanvasHelper.clearCache()
                showCanvasReorderDialog = false
            }
        )
    }

    SettingsPage(
        title = stringResource(R.string.appearance),
        navController = navController,
        scrollBehavior = scrollBehavior
    ) {
        SettingsGeneralCategory(
            title = stringResource(R.string.theme),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_dynamic_theme)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    checked = dynamicTheme,
                    onCheckedChange = onDynamicThemeChange,
                )},
                {AnimatedVisibility(visible = !dynamicTheme) {
                    PreferenceEntry(
                        title = { Text(stringResource(R.string.color_palette)) },
                        description = stringResource(R.string.choose_custom_color_theme),
                        icon = { Icon(painterResource(R.drawable.palette), null) },
                        onClick = { navController.navigate("settings/appearance/palette") }
                    )
                }},
                {EnumListPreference(
                    title = { Text(stringResource(R.string.dark_theme)) },
                    icon = { Icon(painterResource(R.drawable.dark_mode), null) },
                    selectedValue = darkMode,
                    onValueSelected = onDarkModeChange,
                    valueText = {
                        when (it) {
                            DarkMode.ON -> stringResource(R.string.dark_theme_on)
                            DarkMode.OFF -> stringResource(R.string.dark_theme_off)
                            DarkMode.AUTO -> stringResource(R.string.dark_theme_follow_system)
                        }
                    },
                )},
                {AnimatedVisibility(useDarkTheme) {
                    SwitchPreference(
                        title = { Text(stringResource(R.string.pure_black)) },
                        icon = { Icon(painterResource(R.drawable.contrast), null) },
                        checked = pureBlack && useDarkTheme,
                        onCheckedChange = { newValue ->
                            if (useDarkTheme) {
                                onPureBlackChange(newValue)
                            }
                        },
                        isEnabled = useDarkTheme
                    )
                }},
                {AppFontSelectorButton(
                    currentFont = appFont,
                    onFontSelected = { newFont ->
                        onAppFontChange(newFont)
                        coroutineScope.launch {
                            delay(100)
                            com.cgens67.avidtune.ui.component.LocaleManager.getInstance(context).restartApp(context)
                        }
                    }
                )},
                {EnumListPreference(
                    title = { Text(stringResource(R.string.app_text_size)) },
                    icon = { Icon(painterResource(R.drawable.format_align_left), null) },
                    selectedValue = appTextSize,
                    onValueSelected = { newValue ->
                        coroutineScope.launch {
                            context.dataStore.edit {
                                it[AppTextSizeKey] = newValue.name
                            }
                            com.cgens67.avidtune.ui.component.LocaleManager.getInstance(context).restartApp(context)
                        }
                    },
                    valueText = {
                        when (it) {
                            AppTextSize.SMALL -> stringResource(R.string.text_size_small)
                            AppTextSize.SYSTEM -> stringResource(R.string.text_size_system)
                            AppTextSize.MEDIUM -> stringResource(R.string.text_size_medium)
                            AppTextSize.LARGE -> stringResource(R.string.text_size_large)
                            AppTextSize.EXTRA_LARGE -> stringResource(R.string.text_size_extra_large)
                        }
                    },
                )}
            )
        )

        // Language preferences
        SettingsGeneralCategory(
            title = stringResource(R.string.app_language),
            items = listOf(
                { LanguagePreference() }
            )
        )

        val availableBackgroundStyles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            enumValues<PlayerBackgroundStyle>().toList()
        } else {
            enumValues<PlayerBackgroundStyle>().filter {
                it != PlayerBackgroundStyle.BLUR
            }
        }

        val safeSelectedValue = if (playerBackground == PlayerBackgroundStyle.BLUR &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) {
            PlayerBackgroundStyle.DEFAULT
        } else {
            playerBackground
        }

        SettingsGeneralCategory(
            title = stringResource(R.string.player),
            items = listOf(
                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_background_style)) },
                    icon = { Icon(painterResource(R.drawable.gradient), null) },
                    selectedValue = safeSelectedValue,
                    onValueSelected = onPlayerBackgroundChange,
                    valueText = {
                        when (it) {
                            PlayerBackgroundStyle.DEFAULT -> stringResource(R.string.follow_theme)
                            PlayerBackgroundStyle.GRADIENT -> stringResource(R.string.gradient)
                            PlayerBackgroundStyle.BLUR -> stringResource(R.string.player_background_blur)
                            PlayerBackgroundStyle.APPLE_MUSIC -> stringResource(R.string.apple_music)
                            PlayerBackgroundStyle.LIVE_MESH -> stringResource(R.string.live_mesh)
                        }
                    },
                    values = availableBackgroundStyles
                )},

                {EnumListPreference(
                    title = { Text("Mini-Player Style") },
                    icon = { Icon(painterResource(R.drawable.play), null) },
                    selectedValue = miniPlayerStyle,
                    onValueSelected = onMiniPlayerStyleChange,
                    valueText = {
                        when (it) {
                            MiniPlayerStyle.DEFAULT -> stringResource(R.string.default_style)
                            MiniPlayerStyle.APPLE -> "Apple Music"
                        }
                    },
                )},

                {ThumbnailCornerRadiusSelectorButton(
                    onRadiusSelected = { selectedRadius ->
                        Timber.tag("Thumbnail").d("Selected radio: $selectedRadius")
                    }
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_buttons_style)) },
                    icon = { Icon(painterResource(R.drawable.palette), null) },
                    selectedValue = playerButtonsStyle,
                    onValueSelected = onPlayerButtonsStyleChange,
                    valueText = {
                        when (it) {
                            PlayerButtonsStyle.DEFAULT -> stringResource(R.string.default_style)
                            PlayerButtonsStyle.PRIMARY -> stringResource(R.string.secondary_color_style)
                            PlayerButtonsStyle.TERTIARY -> stringResource(R.string.tertiary_color_style)
                        }
                    },
                )},

                {PreferenceEntry(
                    title = { Text(stringResource(R.string.player_slider_style)) },
                    description =
                        when (sliderStyle) {
                            SliderStyle.DEFAULT -> stringResource(R.string.default_)
                            SliderStyle.SQUIGGLY -> stringResource(R.string.squiggly)
                            SliderStyle.SLIM -> stringResource(R.string.slim)
                        },
                    icon = { Icon(painterResource(R.drawable.sliders), null) },
                    onClick = {
                        showSliderOptionDialog = true
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.enable_swipe_thumbnail)) },
                    icon = { Icon(painterResource(R.drawable.swipe), null) },
                    checked = swipeThumbnail,
                    onCheckedChange = onSwipeThumbnailChange,
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.player_text_alignment)) },
                    icon = {
                        Icon(
                            painter =
                                painterResource(
                                    when (playerTextAlignment) {
                                        PlayerTextAlignment.CENTER -> R.drawable.format_align_center
                                        PlayerTextAlignment.SIDED -> R.drawable.format_align_left
                                    },
                                ),
                            contentDescription = null,
                        )
                    },
                    selectedValue = playerTextAlignment,
                    onValueSelected = onPlayerTextAlignmentChange,
                    valueText = {
                        when (it) {
                            PlayerTextAlignment.SIDED -> stringResource(R.string.sided)
                            PlayerTextAlignment.CENTER -> stringResource(R.string.center)
                        }
                    },
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.lyrics_text_position)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    selectedValue = lyricsPosition,
                    onValueSelected = onLyricsPositionChange,
                    valueText = {
                        when (it) {
                            LyricsPosition.LEFT -> stringResource(R.string.left)
                            LyricsPosition.CENTER -> stringResource(R.string.center)
                            LyricsPosition.RIGHT -> stringResource(R.string.right)
                        }
                    },
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.lyrics_click_change)) },
                    icon = { Icon(painterResource(R.drawable.lyrics), null) },
                    checked = lyricsClick,
                    onCheckedChange = onLyricsClickChange,
                )}
            )
        )

        // Misc settings
        SettingsGeneralCategory(
            title = stringResource(R.string.misc),
            items = listOf(
                {SwitchPreference(
                    title = { Text(stringResource(R.string.turn_on_artist_canvas)) },
                    description = stringResource(R.string.turn_on_artist_canvas_desc),
                    icon = { Icon(painterResource(R.drawable.artist), null) },
                    checked = enableArtistCanvas,
                    onCheckedChange = onEnableArtistCanvasChange
                )},
                {AnimatedVisibility(visible = enableArtistCanvas) {
                    Column {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_avidcanvas_artist_canvas)) },
                            icon = { Icon(painterResource(R.drawable.artist), null) },
                            checked = enableAvidCanvas,
                            onCheckedChange = {
                                onEnableAvidCanvasChange(it)
                                com.cgens67.avidtune.ui.component.ArtistCanvasHelper.clearCache()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        SwitchPreference(
                            title = { Text(stringResource(R.string.enable_apple_music_artist_canvas)) },
                            icon = { Icon(painterResource(R.drawable.artist), null) },
                            checked = enableAppleMusicCanvas,
                            onCheckedChange = {
                                onEnableAppleMusicCanvasChange(it)
                                com.cgens67.avidtune.ui.component.ArtistCanvasHelper.clearCache()
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 76.dp, end = 20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                        PreferenceEntry(
                            title = { Text(stringResource(R.string.artist_canvas_provider_priority)) },
                            description = stringResource(R.string.artist_canvas_provider_priority_desc),
                            icon = { Icon(painterResource(R.drawable.list), null) },
                            onClick = { showCanvasReorderDialog = true }
                        )
                    }
                }},
                {EnumListPreference(
                    title = { Text(stringResource(R.string.default_open_tab)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    selectedValue = defaultOpenTab,
                    onValueSelected = onDefaultOpenTabChange,
                    valueText = {
                        when (it) {
                            NavigationTab.HOME -> stringResource(R.string.home)
                            NavigationTab.EXPLORE -> stringResource(R.string.explore)
                            NavigationTab.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                )},

                {ListPreference(
                    title = { Text(stringResource(R.string.default_lib_chips)) },
                    icon = { Icon(painterResource(R.drawable.tab), null) },
                    selectedValue = defaultChip,
                    values = listOf(
                        LibraryFilter.LIBRARY, LibraryFilter.PLAYLISTS, LibraryFilter.SONGS,
                        LibraryFilter.ALBUMS, LibraryFilter.ARTISTS
                    ),
                    valueText = {
                        when (it) {
                            LibraryFilter.SONGS -> stringResource(R.string.songs)
                            LibraryFilter.ARTISTS -> stringResource(R.string.artists)
                            LibraryFilter.ALBUMS -> stringResource(R.string.albums)
                            LibraryFilter.PLAYLISTS -> stringResource(R.string.playlists)
                            LibraryFilter.LIBRARY -> stringResource(R.string.filter_library)
                        }
                    },
                    onValueSelected = onDefaultChipChange,
                )},

                {SwitchPreference(
                    title = { Text(stringResource(R.string.slim_navbar)) },
                    icon = { Icon(painterResource(R.drawable.nav_bar), null) },
                    checked = slimNav,
                    onCheckedChange = onSlimNavChange
                )},

                {EnumListPreference(
                    title = { Text(stringResource(R.string.grid_cell_size)) },
                    icon = { Icon(painterResource(R.drawable.grid_view), null) },
                    selectedValue = gridItemSize,
                    onValueSelected = onGridItemSizeChange,
                    valueText = {
                        when (it) {
                            GridItemSize.SMALL -> stringResource(R.string.small)
                            GridItemSize.BIG -> stringResource(R.string.big)
                        }
                    },
                )},
            )
        )

        AvatarSelector(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun AppFontSelectorButton(
    currentFont: AppFont,
    onFontSelected: (AppFont) -> Unit,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    PreferenceEntry(
        title = { Text(stringResource(R.string.app_font)) },
        description = when (currentFont) {
            AppFont.SYSTEM -> stringResource(R.string.font_system)
            AppFont.SF_PRO -> stringResource(R.string.font_sf_pro)
            AppFont.GOOGLE_SANS -> stringResource(R.string.font_google_sans)
            AppFont.SPACE_GROTESK -> stringResource(R.string.font_space_grotesk)
        },
        icon = { Icon(painterResource(R.drawable.text_fields), null) },
        onClick = { showBottomSheet = true },
        modifier = modifier
    )

    if (showBottomSheet) {
        AppFontBottomSheet(
            selectedFont = currentFont,
            onFontSelected = onFontSelected,
            onDismiss = { showBottomSheet = false },
            sheetState = sheetState
        )
    }
}

@Composable
fun AppFontBottomSheet(
    selectedFont: AppFont,
    onFontSelected: (AppFont) -> Unit,
    onDismiss: () -> Unit,
    sheetState: androidx.compose.material3.SheetState
) {
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.app_font),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                val fonts = AppFont.entries.toList()
                items(fonts) { font ->
                    val isSelected = font == selectedFont
                    val fontFamily = when(font) {
                        AppFont.SYSTEM -> FontFamily.Default
                        AppFont.SF_PRO -> sfProDisplayBold
                        AppFont.GOOGLE_SANS -> googleSansBold
                        AppFont.SPACE_GROTESK -> spaceGroteskBold
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    onFontSelected(font)
                                    onDismiss()
                                }
                            }
                            .padding(horizontal = 24.dp, vertical = 14.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = when(font) {
                                AppFont.SYSTEM -> stringResource(R.string.font_system)
                                AppFont.SF_PRO -> stringResource(R.string.font_sf_pro)
                                AppFont.GOOGLE_SANS -> stringResource(R.string.font_google_sans)
                                AppFont.SPACE_GROTESK -> stringResource(R.string.font_space_grotesk)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontFamily = fontFamily,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReorderCanvasProvidersBottomSheet(
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
                        text = stringResource(R.string.artist_canvas_provider_priority),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.artist_canvas_provider_priority_desc),
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

enum class DarkMode {
    ON,
    OFF,
    AUTO,
}

enum class NavigationTab {
    HOME,
    EXPLORE,
    LIBRARY,
}

enum class LyricsPosition {
    LEFT,
    CENTER,
    RIGHT,
}

enum class PlayerTextAlignment {
    SIDED,
    CENTER,
}

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.*
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.together.TogetherRole
import com.cgens67.avidtune.together.TogetherSessionState
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.theme.PlayerColorExtractor
import com.cgens67.avidtune.ui.utils.resize
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
) {
    val pureBlack by rememberPreference(PureBlackKey, defaultValue = false)
    val style by rememberEnumPreference(MiniPlayerStyleKey, defaultValue = MiniPlayerStyle.DEFAULT)

    if (style == MiniPlayerStyle.APPLE) {
        AppleMiniPlayer(
            position = position,
            duration = duration,
            modifier = modifier,
            pureBlack = pureBlack
        )
    } else {
        DefaultMiniPlayer(
            position = position,
            duration = duration,
            modifier = modifier,
            pureBlack = pureBlack
        )
    }
}

@Composable
private fun DefaultMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val swipeSensitivity = 0.73f
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    
    val playerBackground by rememberEnumPreference(
        key = com.cgens67.avidtune.constants.PlayerBackgroundStyleKey,
        defaultValue = com.cgens67.avidtune.constants.PlayerBackgroundStyle.DEFAULT
    )
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    SwipeableMiniPlayerBox(
        modifier = modifier,
        swipeSensitivity = swipeSensitivity,
        swipeThumbnail = swipeThumbnail,
        playerConnection = playerConnection,
        layoutDirection = layoutDirection,
        coroutineScope = coroutineScope,
        pureBlack = pureBlack,
        useLegacyBackground = false
    ) { offsetX ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .clip(RoundedCornerShape(32.dp))
                .background(
                    color = if (playerBackground == com.cgens67.avidtune.constants.PlayerBackgroundStyle.LIVE_MESH) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                )
        ) {
            if (playerBackground == com.cgens67.avidtune.constants.PlayerBackgroundStyle.LIVE_MESH && mediaMetadata?.thumbnailUrl != null) {
                val infiniteTransition = rememberInfiniteTransition(label = "mini_mesh")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(60000, easing = LinearEasing)),
                    label = "mini_mesh_rot"
                )
                val saturationMatrix = remember { ColorMatrix().apply { setToSaturation(1.6f) } }
                
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(mediaMetadata?.thumbnailUrl)
                            .size(128, 128)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(saturationMatrix),
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(40.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                }
            }

            NewMiniPlayerContent(
                pureBlack = pureBlack,
                position = position,
                duration = duration,
                playerConnection = playerConnection,
                isLiveMesh = playerBackground == com.cgens67.avidtune.constants.PlayerBackgroundStyle.LIVE_MESH
            )
        }
    }
}

@Composable
fun SwipeableMiniPlayerBox(
    modifier: Modifier = Modifier,
    swipeSensitivity: Float,
    swipeThumbnail: Boolean,
    playerConnection: PlayerConnection,
    layoutDirection: LayoutDirection,
    coroutineScope: CoroutineScope,
    pureBlack: Boolean = false,
    useLegacyBackground: Boolean = false,
    content: @Composable (Float) -> Unit
) {
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
        return (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }
    val autoSwipeThreshold = calculateAutoSwipeThreshold(swipeSensitivity)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .let { baseModifier ->
                if (useLegacyBackground) {
                    baseModifier.background(
                        if (pureBlack) Color.Black
                        else MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    baseModifier.padding(horizontal = 12.dp)
                }
            }
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount =
                                    if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                val allowRight = adjustedDragAmount > 0 && canSkipPrevious
                                if (allowLeft || allowRight) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value

                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val shouldChangeSong = (
                                    kotlin.math.abs(currentOffset) > minDistanceThreshold &&
                                    velocity > velocityThreshold
                                ) || (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    val isRightSwipe = currentOffset > 0
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1

                                    if (isRightSwipe && canSkipPrevious) {
                                        playerConnection.seekToPrevious()
                                    } else if (!isRightSwipe && canSkipNext) {
                                        playerConnection.seekToNext()
                                    }
                                }

                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = animationSpec
                                    )
                                }
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            }
    ) {
        content(offsetXAnimatable.value)

        // Visual indicator
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun RowScope.MiniPlayerInfo(
    mediaMetadata: MediaMetadata,
    primaryTextColor: Color = MaterialTheme.colorScheme.onSurface,
    secondaryTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = mediaMetadata.title,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "title"
        ) { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }

        androidx.compose.animation.AnimatedContent(
            targetState = mediaMetadata.artists,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "artist"
        ) { artists ->
            Text(
                text = artists.joinToString { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.basicMarquee()
            )
        }
    }
}

@Composable
private fun MiniPlayerArtwork(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    isLiveMesh: Boolean = false
) {
    val progressColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.primary
    val progressTrackColor = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val imageBorderColor = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(47.dp)
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularWavyProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = progressColor,
                trackColor = progressTrackColor
            )
        } else {
            androidx.compose.material3.CircularWavyProgressIndicator(
                progress = { if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxSize(),
                color = progressColor,
                trackColor = progressTrackColor
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(37.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = imageBorderColor,
                    shape = CircleShape
                )
        ) {
            val thumbnailUrl = mediaMetadata?.thumbnailUrl
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerTransportButton(
    iconResId: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    containerColorPrimary: Color = MaterialTheme.colorScheme.surface,
    borderColorEnabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    borderColorDisabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
) {
    val containerColor =
        if (isPrimary) containerColorPrimary else Color.Transparent
    val borderColor =
        if (enabled) borderColorEnabled
        else borderColorDisabled
    val iconTint =
        if (enabled) iconColor
        else disabledIconColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .then(modifier)
            .size(if (isPrimary) 40.dp else 36.dp)
            .clip(CircleShape)
            .background(containerColor)
            .border(width = 1.dp, color = borderColor, shape = CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Icon(
            painter = painterResource(iconResId),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(if (isPrimary) 22.dp else 18.dp)
        )
    }
}

@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    playerConnection: PlayerConnection,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    disabledIconColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    containerColorPrimary: Color = MaterialTheme.colorScheme.surface,
    borderColorEnabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    borderColorDisabled: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_previous,
            contentDescription = null,
            onClick = { playerConnection.seekToPrevious() },
            enabled = canSkipPrevious,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp)
        ) {
            MiniPlayerTransportButton(
                iconResId = when {
                    playbackState == Player.STATE_ENDED -> R.drawable.replay
                    isPlaying -> R.drawable.pause
                    else -> R.drawable.play
                },
                contentDescription = stringResource(
                    if (playbackState == Player.STATE_ENDED || !isPlaying) R.string.play else R.string.play
                ).let {
                    if (isPlaying && playbackState != Player.STATE_ENDED) "Pause" else it
                },
                onClick = {
                    if (playbackState == Player.STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.togglePlayPause()
                    }
                },
                isPrimary = true,
                iconColor = iconColor,
                disabledIconColor = disabledIconColor,
                containerColorPrimary = containerColorPrimary,
                borderColorEnabled = borderColorEnabled,
                borderColorDisabled = borderColorDisabled
            )
        }

        MiniPlayerTransportButton(
            iconResId = R.drawable.skip_next,
            contentDescription = null,
            onClick = { playerConnection.seekToNext() },
            enabled = canSkipNext,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )
    }
}

@Composable
fun NewMiniPlayerContent(
    pureBlack: Boolean,
    position: Long,
    duration: Long,
    playerConnection: PlayerConnection,
    isLiveMesh: Boolean = false
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val isLoading = playbackState == Player.STATE_BUFFERING

    val primaryTextColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (isLiveMesh) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconColor = if (isLiveMesh) Color.White else MaterialTheme.colorScheme.onSurface
    val disabledIconColor = if (isLiveMesh) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val containerColorPrimary = if (isLiveMesh) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
    val borderColorEnabled = if (isLiveMesh) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val borderColorDisabled = if (isLiveMesh) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        MiniPlayerArtwork(
            mediaMetadata = mediaMetadata,
            position = position,
            duration = duration,
            isLoading = isLoading,
            isLiveMesh = isLiveMesh
        )

        Spacer(modifier = Modifier.width(5.dp))

        mediaMetadata?.let {
            MiniPlayerInfo(
                mediaMetadata = it,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        } ?: Spacer(Modifier.weight(1f))

        Spacer(modifier = Modifier.width(12.dp))

        MiniPlayerTransportControls(
            isPlaying = isPlaying,
            playbackState = playbackState,
            isLoading = isLoading,
            canSkipPrevious = canSkipPrevious,
            canSkipNext = canSkipNext,
            playerConnection = playerConnection,
            iconColor = iconColor,
            disabledIconColor = disabledIconColor,
            containerColorPrimary = containerColorPrimary,
            borderColorEnabled = borderColorEnabled,
            borderColorDisabled = borderColorDisabled
        )
    }
}

@Composable
fun MiniMediaInfo(
    mediaMetadata: com.cgens67.avidtune.models.MediaMetadata,
    error: androidx.media3.common.PlaybackException?,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.padding(6.dp)) {
            AsyncImage(
                model = mediaMetadata.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(ThumbnailCornerRadius)),
            )
            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(ThumbnailCornerRadius),
                        ),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppleMiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    pureBlack: Boolean
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }
    val playerBackground by rememberEnumPreference(PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)
    val disableBlur by rememberPreference(DisableBlurKey, false)

    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()

    val sessionState by playerConnection.service.togetherSessionState.collectAsState()
    val isListenTogetherGuest = sessionState is TogetherSessionState.Joined &&
        (sessionState as TogetherSessionState.Joined).role is TogetherRole.Guest

    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest
    val swipeSensitivity = 0.73f
    
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isTabletLandscape = remember(configuration.screenWidthDp, configuration.orientation) {
        configuration.screenWidthDp >= 600 && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val animationSpec = remember { spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow) }
    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val fallbackColorArgb = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(mediaMetadata, playerBackground) {
        if (playerBackground == PlayerBackgroundStyle.GRADIENT ||
            playerBackground == PlayerBackgroundStyle.APPLE_MUSIC ||
            playerBackground == PlayerBackgroundStyle.LIVE_MESH
        ) {
            withContext(Dispatchers.IO) {
                val result = runCatching {
                    context.imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(mediaMetadata?.thumbnailUrl)
                            .allowHardware(false)
                            .build()
                    ).drawable as? android.graphics.drawable.BitmapDrawable
                }.getOrNull()

                result?.bitmap?.let { bitmap ->
                    val palette = androidx.palette.graphics.Palette.from(bitmap)
                        .maximumColorCount(8)
                        .resizeBitmapArea(100 * 100)
                        .generate()

                    val extracted = PlayerColorExtractor.extractGradientColors(
                        palette = palette,
                        fallbackColor = fallbackColorArgb
                    )
                    withContext(Dispatchers.Main) {
                        gradientColors = extracted
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    val backgroundColor = if (pureBlack && useDarkTheme) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val isDynamicBackground = playerBackground != PlayerBackgroundStyle.DEFAULT
    
    val primaryColor = if (isDynamicBackground) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = if (isDynamicBackground) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (isDynamicBackground) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MiniPlayerHeight)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 12.dp)
            .let { baseModifier ->
                if (swipeThumbnail) {
                    baseModifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartTime = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch { offsetXAnimatable.animateTo(0f, animationSpec) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDragAmount = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val canSkipPreviousLocal = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNextLocal = playerConnection.player.nextMediaItemIndex != -1
                                val tryingToSwipeRight = adjustedDragAmount > 0
                                val tryingToSwipeLeft = adjustedDragAmount < 0
                                val allowLeft = tryingToSwipeLeft && canSkipNextLocal
                                val allowRight = tryingToSwipeRight && canSkipPreviousLocal

                                val canReturnToCenter = (tryingToSwipeRight && !canSkipPreviousLocal && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNextLocal && offsetXAnimatable.value > 0)

                                if (allowLeft || allowRight || canReturnToCenter) {
                                    totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                    coroutineScope.launch {
                                        offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                    }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartTime
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetXAnimatable.value
                                val minDistanceThreshold = 50f
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                val canSkipPreviousLocal = playerConnection.player.previousMediaItemIndex != -1
                                val canSkipNextLocal = playerConnection.player.nextMediaItemIndex != -1

                                val shouldChangeSong = (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                    (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                if (shouldChangeSong) {
                                    if (currentOffset > 0 && canSkipPreviousLocal) {
                                        playerConnection.seekToPrevious()
                                    } else if (currentOffset <= 0 && canSkipNextLocal) {
                                        playerConnection.seekToNext()
                                    }
                                }
                                coroutineScope.launch {
                                    offsetXAnimatable.animateTo(0f, animationSpec)
                                }
                            }
                        )
                    }
                } else baseModifier
            }
    ) {
        Box(
            modifier = Modifier
                .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                .height(64.dp)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .clip(RoundedCornerShape(8.dp))
                .background(color = backgroundColor)
        ) {
            // Background Layers
            if (isDynamicBackground) {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(40.dp) else it }
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                            }
                        }
                        PlayerBackgroundStyle.GRADIENT -> {
                            if (gradientColors.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(gradientColors)).background(Color.Black.copy(0.2f)))
                            }
                        }
                        PlayerBackgroundStyle.APPLE_MUSIC -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(48.dp) else it }
                                )
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                            }
                        }
                        PlayerBackgroundStyle.LIVE_MESH -> {
                            mediaMetadata?.thumbnailUrl?.let { url ->
                                val infiniteTransition = rememberInfiniteTransition(label = "mesh_transition")
                                val rotation by infiniteTransition.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "rotation")
                                val satMatrix = remember { ColorMatrix().apply { setToSaturation(1.6f) } }
                                Box(modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(url.resize(100, 100)).allowHardware(false).build(),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        colorFilter = ColorFilter.colorMatrix(satMatrix),
                                        modifier = Modifier.fillMaxSize().let { if(!disableBlur) it.blur(40.dp) else it }.graphicsLayer { rotationZ = rotation }
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.3f)))
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }

            // Bottom Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter)
                    .drawWithContent {
                        val progress = if (duration > 0) position.toFloat() / duration else 0f
                        val trackColor = outlineColor.copy(alpha = 0.2f)
                        drawRect(trackColor)
                        drawRect(primaryColor, size = Size(size.width * progress.coerceIn(0f, 1f), size.height))
                    }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                // Cookie 4-Sided Thumbnail
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(MaterialShapes.Cookie4Sided.toShape())
                        .background(color = outlineColor.copy(alpha = 0.2f))
                ) {
                    mediaMetadata?.let { metadata ->
                        AsyncImage(
                            model = metadata.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = onSurfaceColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song info - title and artist
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    val error by playerConnection.error.collectAsState()
                    
                    mediaMetadata?.let { metadata ->
                        Text(
                            text = metadata.title,
                            color = onSurfaceColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (metadata.explicit) {
                                Icon(
                                    painter = painterResource(R.drawable.explicit),
                                    contentDescription = null,
                                    tint = onSurfaceColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp).padding(end = 2.dp)
                                )
                            }
                            if (metadata.artists.any { it.name.isNotBlank() }) {
                                Text(
                                    text = metadata.artists.joinToString { it.name },
                                    color = onSurfaceColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                                )
                            }
                        }

                        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                            Text(
                                text = stringResource(R.string.error_playing),
                                color = errorColor,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Play/Pause Button
                val isPlaying by playerConnection.isPlaying.collectAsState()
                val isMuted by playerConnection.isMuted.collectAsState()
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (isListenTogetherGuest) {
                                playerConnection.toggleMute()
                                return@clickable
                            }
                            if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.togglePlayPause()
                            }
                        }
                ) {
                    Icon(
                        painter = painterResource(
                            when {
                                isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                isPlaying -> R.drawable.pause 
                                else -> R.drawable.play
                            }
                        ),
                        contentDescription = null,
                        tint = onSurfaceColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Next Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canSkipNext && !isListenTogetherGuest) {
                            playerConnection.seekToNext()
                        }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = "Next",
                        tint = if (canSkipNext && !isListenTogetherGuest) onSurfaceColor else onSurfaceColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Visual swipe indicators (Left/Right skip hints)
        if (offsetXAnimatable.value.absoluteValue > 50f) {
            Box(
                modifier = Modifier
                    .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next
                    ),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(
                        alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
