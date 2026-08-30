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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
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
                                
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Icon(
                                    painter = painterResource(R.drawable.drag_handle),
                                    contentDescription = null, // Drag handle, decoration only
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
