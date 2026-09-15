@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package com.cgens67.avidtune.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.ui.component.IconButton as AppIconButton
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.ui.utils.backToMain
import com.cgens67.avidtune.utils.rememberPreference
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ThemePalette(
    val id: String,
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
    val onPrimary: Color = if (primary.luminance() > 0.5f) Color.Black else Color.White
)

object ThemePalettes {

    private var lightPalettes: List<ThemePalette>? = null
    private var darkPalettes: List<ThemePalette>? = null

    private fun generatePalette(id: String, name: String, primaryHex: Long, isDark: Boolean): ThemePalette {
        val primaryColor = Color(primaryHex)
        val scheme = SchemeTonalSpot(Hct.fromInt(primaryColor.toArgb()), isDark, 0.0)
        return ThemePalette(
            id = id,
            name = name,
            primary = primaryColor,
            secondary = Color(scheme.secondary),
            tertiary = Color(scheme.tertiary),
            neutral = Color(scheme.surfaceVariant),
            onPrimary = Color(scheme.onPrimary)
        )
    }

    fun getPalettes(isDark: Boolean): List<ThemePalette> {
        if (isDark && darkPalettes != null) return darkPalettes!!
        if (!isDark && lightPalettes != null) return lightPalettes!!

        val palettes = listOf(
            generatePalette("default", "Default", 0xFFED5564, isDark),
            generatePalette("ocean_blue", "Ocean Blue", 0xFF4A90D9, isDark),
            generatePalette("arctic_blue", "Arctic Blue", 0xFF00BFFF, isDark),
            generatePalette("midnight_navy", "Midnight Navy", 0xFF2C3E50, isDark),
            generatePalette("sky_blue", "Sky Blue", 0xFF87CEEB, isDark),
            generatePalette("cobalt_blue", "Cobalt Blue", 0xFF0047AB, isDark),
            generatePalette("electric_blue", "Electric Blue", 0xFF7DF9FF, isDark),
            generatePalette("emerald_green", "Emerald Green", 0xFF2ECC71, isDark),
            generatePalette("teal_wave", "Teal Wave", 0xFF1ABC9C, isDark),
            generatePalette("forest_green", "Forest Green", 0xFF228B22, isDark),
            generatePalette("spotify_green", "Spotify Green", 0xFF1DB954, isDark),
            generatePalette("mint_fresh", "Mint Fresh", 0xFF98FF98, isDark),
            generatePalette("olive_garden", "Olive Garden", 0xFF808000, isDark),
            generatePalette("sage_green", "Sage Green", 0xFF9CAF88, isDark),
            generatePalette("sunset_orange", "Sunset Orange", 0xFFE67E22, isDark),
            generatePalette("golden_hour", "Golden Hour", 0xFFF39C12, isDark),
            generatePalette("warm_amber", "Warm Amber", 0xFFFFBF00, isDark),
            generatePalette("tangerine_blast", "Tangerine Blast", 0xFFFF9800, isDark),
            generatePalette("peach", "Peach", 0xFFFFDAB9, isDark),
            generatePalette("mango", "Mango", 0xFFFF8243, isDark),
            generatePalette("royal_purple", "Royal Purple", 0xFF9B59B6, isDark),
            generatePalette("lavender_dream", "Lavender Dream", 0xFFB39DDB, isDark),
            generatePalette("grape_purple", "Grape Purple", 0xFF6B5B95, isDark),
            generatePalette("violet", "Violet", 0xFFEE82EE, isDark),
            generatePalette("amethyst", "Amethyst", 0xFF9966CC, isDark),
            generatePalette("ultra_violet", "Ultra Violet", 0xFF645394, isDark),
            generatePalette("cherry_blossom", "Cherry Blossom", 0xFFFFB7C5, isDark),
            generatePalette("rose_quartz", "Rose Quartz", 0xFFF7CAC9, isDark),
            generatePalette("magenta_pop", "Magenta Pop", 0xFFFF00FF, isDark),
            generatePalette("hot_pink", "Hot Pink", 0xFFFF69B4, isDark),
            generatePalette("blush", "Blush", 0xFFDE5D83, isDark),
            generatePalette("coral", "Coral", 0xFFFF7F50, isDark),
            generatePalette("bubblegum", "Bubblegum", 0xFFFFC1CC, isDark),
            generatePalette("crimson_red", "Crimson Red", 0xFFDC143C, isDark),
            generatePalette("youtube_red", "YouTube Red", 0xFFFF0000, isDark),
            generatePalette("wine_red", "Wine Red", 0xFF722F37, isDark),
            generatePalette("ruby_red", "Ruby Red", 0xFFE0115F, isDark),
            generatePalette("scarlet", "Scarlet", 0xFFFF2400, isDark),
            generatePalette("charcoal", "Charcoal", 0xFF36454F, isDark),
            generatePalette("silver", "Silver", 0xFFC0C0C0, isDark),
            generatePalette("slate", "Slate", 0xFF708090, isDark),
            generatePalette("graphite", "Graphite", 0xFF474747, isDark),
            generatePalette("terracotta", "Terracotta", 0xFFE2725B, isDark),
            generatePalette("coffee", "Coffee", 0xFF6F4E37, isDark),
            generatePalette("mocha", "Mocha", 0xFF967969, isDark),
            generatePalette("sand", "Sand", 0xFFC2B280, isDark),
            generatePalette("clay", "Clay", 0xFFB66A50, isDark),
            generatePalette("pastel_pink", "Pastel Pink", 0xFFFFD1DC, isDark),
            generatePalette("pastel_blue", "Pastel Blue", 0xFFAEC6CF, isDark),
            generatePalette("pastel_green", "Pastel Green", 0xFF77DD77, isDark),
            generatePalette("pastel_yellow", "Pastel Yellow", 0xFFFDFD96, isDark),
            generatePalette("pastel_purple", "Pastel Purple", 0xFFB19CD9, isDark),
            generatePalette("neon_green", "Neon Green", 0xFF39FF14, isDark),
            generatePalette("neon_pink", "Neon Pink", 0xFFFF10F0, isDark),
            generatePalette("neon_blue", "Neon Blue", 0xFF00F5FF, isDark),
            generatePalette("neon_orange", "Neon Orange", 0xFFFF5F1F, isDark),
            generatePalette("ocean", "Ocean", 0xFF006994, isDark),
            generatePalette("forest", "Forest", 0xFF0B3D0B, isDark),
            generatePalette("autumn", "Autumn", 0xFFD2691E, isDark),
            generatePalette("winter", "Winter", 0xFFADD8E6, isDark),
            generatePalette("spring", "Spring", 0xFF98FB98, isDark),
            generatePalette("summer", "Summer", 0xFFFFD700, isDark),
            generatePalette("twilight", "Twilight", 0xFF4B0082, isDark),
            generatePalette("aurora", "Aurora", 0xFF00FF7F, isDark),
            generatePalette("candy", "Candy", 0xFFFF69B4, isDark),
            generatePalette("rainbow", "Rainbow", 0xFFFF0000, isDark)
        )

        if (isDark) darkPalettes = palettes else lightPalettes = palettes
        return palettes
    }

    fun findByPrimaryColor(colorHex: String, isDark: Boolean): ThemePalette? {
        return getPalettes(isDark).find { it.primary.toHexString() == colorHex }
    }

    fun findById(id: String, isDark: Boolean): ThemePalette? {
        return getPalettes(isDark).find { it.id == id }
    }
}

private fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

@Composable
fun CustomThemeBottomSheet(
    initialColor: Color,
    onDismiss: () -> Unit,
    onSave: (ThemeSeedPalette) -> Unit,
    onExport: (ThemeSeedPalette) -> Unit
) {
    var red by remember { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember { mutableFloatStateOf(initialColor.blue * 255f) }

    val currentColor = Color(red / 255f, green / 255f, blue / 255f)
    val hexString = String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.custom_theme),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Color Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(currentColor)
                    .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = hexString,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (currentColor.luminance() > 0.5f) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Sliders
            ColorSlider(label = stringResource(R.string.red_colon, red.toInt()), value = red, onValueChange = { red = it }, trackColor = Color.Red)
            ColorSlider(label = stringResource(R.string.green_colon, green.toInt()), value = green, onValueChange = { green = it }, trackColor = Color.Green)
            ColorSlider(label = stringResource(R.string.blue_colon, blue.toInt()), value = blue, onValueChange = { blue = it }, trackColor = Color.Blue)

            Spacer(modifier = Modifier.height(8.dp))

            // Actions: Reset, Random, Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    red = initialColor.red * 255f
                    green = initialColor.green * 255f
                    blue = initialColor.blue * 255f
                }) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(onClick = {
                    red = (0..255).random().toFloat()
                    green = (0..255).random().toFloat()
                    blue = (0..255).random().toFloat()
                }) {
                    Text(stringResource(R.string.random))
                }
                TextButton(onClick = {
                    val palette = ThemeSeedPalette(currentColor, currentColor, currentColor, currentColor)
                    onExport(palette)
                }) {
                    Text(stringResource(R.string.export))
                }
            }

            // Save & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            val palette = ThemeSeedPalette(currentColor, currentColor, currentColor, currentColor)
                            onSave(palette)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, onValueChange: (Float) -> Unit, trackColor: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(activeTrackColor = trackColor, thumbColor = trackColor)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(
        CustomThemeColorKey,
        defaultValue = "default"
    )

    val customThemeName = stringResource(R.string.custom_theme)
    val isDarkTheme = isSystemInDarkTheme()

    // Retrieve the saved seed color based on current preferences.
    val selectedPalette = remember(customThemeColor, customThemeName, isDarkTheme) {
        val custom = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)
            ?.let { ThemePalette(
                id = "custom_seed",
                name = customThemeName,
                primary = it.primary,
                secondary = it.secondary,
                tertiary = it.tertiary,
                neutral = it.neutral
            ) }

        custom
            ?: ThemePalettes.findById(customThemeColor, isDarkTheme)
            ?: ThemePalettes.findByPrimaryColor(customThemeColor, isDarkTheme)
            ?: ThemePalettes.getPalettes(isDarkTheme).first()
    }

    val savedPrimaryColor = selectedPalette.primary

    // The color currently being actively edited by the sliders.
    // Syncs back to savedPrimaryColor when customThemeColor changes externally.
    var editorColor by remember(savedPrimaryColor) { mutableStateOf(savedPrimaryColor) }

    // Derive active palette for the live preview card.
    val activePalette = remember(editorColor, customThemeColor, isDarkTheme) {
        if (editorColor == savedPrimaryColor) {
            selectedPalette
        } else {
            // Live preview while dragging sliders
            val scheme = SchemeTonalSpot(Hct.fromInt(editorColor.toArgb()), isDarkTheme, 0.0)
            ThemePalette(
                id = "custom_seed_preview",
                name = context.getString(R.string.preview),
                primary = Color(scheme.primary),
                secondary = Color(scheme.secondary),
                tertiary = Color(scheme.tertiary),
                neutral = Color(scheme.surfaceVariant),
                onPrimary = Color(scheme.onPrimary)
            )
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val scheme = SchemeTonalSpot(Hct.fromInt(activePalette.primary.toArgb()), isDarkTheme, 0.0)
                    val p = ThemeSeedPalette(
                        primary = activePalette.primary,
                        secondary = Color(scheme.secondary),
                        tertiary = Color(scheme.tertiary),
                        neutral = Color(scheme.surfaceVariant)
                    )
                    val json = ThemeSeedPaletteCodec.encodeForPreference(p, customThemeName)
                    outputStream.write(json.toByteArray())
                }
                withContext(Dispatchers.Main) { Toast.makeText(context, "Theme exported successfully", Toast.LENGTH_SHORT).show() }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(context, "Theme export failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty() }.getOrNull().orEmpty()
            }
            val imported = ThemeSeedPaletteCodec.decodeFromJson(text)
            if (imported != null) {
                val extractedName = ThemeSeedPaletteCodec.extractNameFromJsonOrNull(text) ?: customThemeName
                onCustomThemeColorChange(ThemeSeedPaletteCodec.encodeForPreference(imported, extractedName))
                Toast.makeText(context, context.getString(R.string.theme_imported_successfully), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.theme_import_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showCustomThemeSheet by rememberSaveable { mutableStateOf(false) }
    var paletteToExport by remember { mutableStateOf<ThemeSeedPalette?>(null) }

    if (showCustomThemeSheet) {
        CustomThemeBottomSheet(
            initialColor = activePalette.primary,
            onDismiss = { showCustomThemeSheet = false },
            onSave = { palette ->
                onCustomThemeColorChange(ThemeSeedPaletteCodec.encodeForPreference(palette, customThemeName))
                showCustomThemeSheet = false
            },
            onExport = { palette ->
                paletteToExport = palette
                exportLauncher.launch("custom_theme_${System.currentTimeMillis()}.json")
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.color_palette), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    AppIconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                        Icon(painterResource(R.drawable.restore), contentDescription = stringResource(R.string.import_theme))
                    }
                    IconButton(onClick = { exportLauncher.launch("custom_theme_${System.currentTimeMillis()}.json") }) {
                        Icon(painterResource(R.drawable.share), contentDescription = stringResource(R.string.export))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ThemePreviewCard(
                palette = activePalette,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PRESETS SECTION
            Text(
                text = stringResource(R.string.presets),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            ColorPaletteSelector(
                palettes = ThemePalettes.getPalettes(isDarkTheme),
                selectedPalette = activePalette,
                onPaletteSelected = { palette ->
                    onCustomThemeColorChange(palette.id)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // CUSTOM THEME SECTION
            Text(
                text = stringResource(R.string.custom_theme),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Text(
                text = stringResource(R.string.theme_creator_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val isCustomMode = customThemeColor.startsWith("{")
            val isApplyEnabled = editorColor != savedPrimaryColor || !isCustomMode

            ColorEditor(
                color = editorColor,
                onColorChange = { newSeed ->
                    editorColor = newSeed
                },
                onRandomize = {
                    val newSeed = Color(
                        red = (0..255).random() / 255f,
                        green = (0..255).random() / 255f,
                        blue = (0..255).random() / 255f
                    )
                    editorColor = newSeed
                },
                onApply = {
                    val scheme = SchemeTonalSpot(Hct.fromInt(editorColor.toArgb()), isDarkTheme, 0.0)
                    val palette = ThemeSeedPalette(
                        primary = editorColor,
                        secondary = Color(scheme.secondary),
                        tertiary = Color(scheme.tertiary),
                        neutral = Color(scheme.surfaceVariant)
                    )
                    onCustomThemeColorChange(ThemeSeedPaletteCodec.encodeForPreference(palette, customThemeName))
                    Toast.makeText(context, context.getString(R.string.theme_saved), Toast.LENGTH_SHORT).show()
                },
                isApplyEnabled = isApplyEnabled
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemePreviewCard(
    palette: ThemePalette,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedPrimary by animateColorAsState(
        targetValue = palette.primary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "primaryColor"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = palette.secondary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "secondaryColor"
    )
    val animatedTertiary by animateColorAsState(
        targetValue = palette.tertiary,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "tertiaryColor"
    )
    val animatedNeutral by animateColorAsState(
        targetValue = palette.neutral,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "neutralColor"
    )

    val backgroundColor = if (isDarkTheme) {
        Color(0xFF1C1C1E)
    } else {
        animatedTertiary.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.7f, size.height * 0.3f),
                    radius = size.width * 0.8f
                )
                drawRect(brush = gradientBrush)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Card(
                        modifier = Modifier
                            .width(140.dp)
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = animatedPrimary.copy(alpha = 0.15f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(animatedPrimary, animatedSecondary)
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(animatedNeutral.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(animatedPrimary)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(animatedPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = palette.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(
                        animatedPrimary to 48.dp,
                        animatedSecondary to 36.dp,
                        animatedTertiary to 28.dp
                    ).forEachIndexed { index, (color, size) ->
                        Box(
                            modifier = Modifier
                                .offset(x = (-12 * index).dp)
                                .size(size)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(animatedPrimary, animatedSecondary, animatedNeutral).forEach { color ->
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color.copy(alpha = 0.2f))
                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                        }
                    }
                }
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = animatedPrimary,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = palette.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorPaletteSelector(
    palettes: List<ThemePalette>,
    selectedPalette: ThemePalette,
    onPaletteSelected: (ThemePalette) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val selectedIndex = palettes.indexOfFirst { it.id == selectedPalette.id }.takeIf { it >= 0 } ?: 0

    val totalDots = (palettes.size + 3) / 4

    val currentPage by remember {
        derivedStateOf { listState.firstVisibleItemIndex / 4 }
    }

    var stableCurrentPage by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(currentPage) {
        kotlinx.coroutines.delay(50)
        stableCurrentPage = currentPage
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            listState.animateScrollToItem(
                index = selectedIndex,
                scrollOffset = -100
            )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(palettes, key = { it.id }) { palette ->
                PaletteCard(
                    palette = palette,
                    isSelected = palette.id == selectedPalette.id,
                    onClick = { onPaletteSelected(palette) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CarouselDotsIndicator(
            totalDots = totalDots,
            currentPage = stableCurrentPage,
            selectedColor = selectedPalette.primary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun CarouselDotsIndicator(
    totalDots: Int,
    currentPage: Int,
    selectedColor: Color,
    modifier: Modifier = Modifier
) {
    val fixedDotContainerSize = 10.dp

    Row(
        modifier = modifier.height(fixedDotContainerSize),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == currentPage

            val dotSize by animateDpAsState(
                targetValue = if (isSelected) 8.dp else 4.dp,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "dotSize"
            )

            val dotColor by animateColorAsState(
                targetValue = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                animationSpec = tween(durationMillis = 200),
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(fixedDotContainerSize),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun PaletteCard(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scaleAnimation"
    )

    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "borderAnimation"
    )

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) palette.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 300),
        label = "borderColorAnimation"
    )

    Card(
        modifier = Modifier
            .size(80.dp)
            .scale(scale)
            .border(borderWidth, animatedBorderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(56.dp)
            ) {
                val radius = size.minDimension / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawArc(
                    color = palette.primary,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                drawArc(
                    color = palette.primary.copy(alpha = 0.4f),
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )

                drawArc(
                    color = palette.primary.copy(alpha = 0.2f),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset.Zero,
                    size = size
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(2.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = palette.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorEditor(
    color: Color,
    onColorChange: (Color) -> Unit,
    onRandomize: () -> Unit,
    onApply: () -> Unit,
    isApplyEnabled: Boolean
) {
    var red by remember(color) { mutableFloatStateOf(color.red * 255f) }
    var green by remember(color) { mutableFloatStateOf(color.green * 255f) }
    var blue by remember(color) { mutableFloatStateOf(color.blue * 255f) }

    val hexString = String.format("#%06X", (0xFFFFFF and color.toArgb()))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    )
                    Column {
                        Text(stringResource(R.string.hex_color), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(hexString, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                IconButton(
                    onClick = onRandomize,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(painterResource(R.drawable.shuffle), contentDescription = stringResource(R.string.randomize), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ColorSliderRow(label = "R", value = red, activeColor = Color.Red, onValueChange = {
                red = it
                onColorChange(Color(red/255f, green/255f, blue/255f))
            })
            ColorSliderRow(label = "G", value = green, activeColor = Color.Green, onValueChange = {
                green = it
                onColorChange(Color(red/255f, green/255f, blue/255f))
            })
            ColorSliderRow(label = "B", value = blue, activeColor = Color.Blue, onValueChange = {
                blue = it
                onColorChange(Color(red/255f, green/255f, blue/255f))
            })

            Button(
                onClick = onApply,
                enabled = isApplyEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.apply))
            }
        }
    }
}

@Composable
private fun ColorSliderRow(label: String, value: Float, activeColor: Color, onValueChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = activeColor,
                thumbColor = activeColor
            )
        )
        Text(value.toInt().toString(), modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
    }
}

@Preview(showBackground = true)
@Composable
private fun PaletteCardPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            val palettes = ThemePalettes.getPalettes(false)
            PaletteCard(
                palette = palettes.find { it.id == "default" }!!,
                isSelected = true,
                onClick = {}
            )
            PaletteCard(
                palette = palettes.find { it.id == "ocean_blue" }!!,
                isSelected = false,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemePreviewCardPreview() {
    MaterialTheme {
        val palettes = ThemePalettes.getPalettes(false)
        ThemePreviewCard(
            palette = palettes.find { it.id == "emerald_green" }!!,
            isDarkTheme = false,
            modifier = Modifier.padding(24.dp)
        )
    }
}
