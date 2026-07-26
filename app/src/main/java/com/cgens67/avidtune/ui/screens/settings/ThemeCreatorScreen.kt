package com.cgens67.avidtune.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.utils.dataStore
import com.cgens67.avidtune.utils.rememberPreference
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCreatorScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val context = LocalContext.current
    val (customThemeColor, onCustomThemeColorChange) = rememberPreference(
        CustomThemeColorKey,
        defaultValue = ThemePalettes.Default.id
    )

    // Load initial colors based on current selection (Custom JSON or Preset ID)
    val initialPalette = remember {
        val decoded = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)
        if (decoded != null) {
            decoded
        } else {
            val preset = ThemePalettes.findById(customThemeColor) 
                ?: ThemePalettes.findByPrimaryColor(customThemeColor) 
                ?: ThemePalettes.Default
            ThemeSeedPalette(preset.primary, preset.secondary, preset.tertiary, preset.neutral)
        }
    }

    val initialName = remember {
        ThemeSeedPaletteCodec.extractNameFromJsonOrNull(customThemeColor) ?: "Custom Theme"
    }

    var themeName by remember { mutableStateOf(initialName) }
    var primary by remember { mutableStateOf(initialPalette.primary) }
    var secondary by remember { mutableStateOf(initialPalette.secondary) }
    var tertiary by remember { mutableStateOf(initialPalette.tertiary) }
    var neutral by remember { mutableStateOf(initialPalette.neutral) }

    var autoHarmonize by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Primary, 1: Secondary, 2: Tertiary, 3: Neutral
    val isDark = isSystemInDarkTheme()

    // Auto-generate harmonious colors using Material 3 if enabled
    LaunchedEffect(primary, autoHarmonize) {
        if (autoHarmonize) {
            val scheme = SchemeTonalSpot(Hct.fromInt(primary.toArgb()), isDark, 0.0)
            secondary = Color(scheme.secondary)
            tertiary = Color(scheme.tertiary)
            neutral = Color(scheme.neutralVariant)
        }
    }

    val activeColor = when (selectedTab) {
        0 -> primary
        1 -> secondary
        2 -> tertiary
        else -> neutral
    }

    fun updateActiveColor(color: Color) {
        when (selectedTab) {
            0 -> primary = color
            1 -> secondary = color
            2 -> tertiary = color
            3 -> neutral = color
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Creator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val finalName = themeName.ifBlank { "Custom Theme" }
                        val palette = ThemeSeedPalette(primary, secondary, tertiary, neutral)
                        val json = ThemeSeedPaletteCodec.encodeForPreference(palette, finalName)
                        onCustomThemeColorChange(json)
                        Toast.makeText(context, "Theme Saved & Applied!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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

            // Preview Area
            ThemeCreatorPreview(
                themeName = themeName,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = themeName,
                onValueChange = { themeName = it },
                label = { Text("Theme Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-Harmonize Toggle
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { autoHarmonize = !autoHarmonize },
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Harmonize", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Automatically calculate secondary, tertiary, and neutral colors based on primary.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = autoHarmonize, onCheckedChange = { autoHarmonize = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Selector
            val tabs = listOf("Primary", "Secondary", "Tertiary", "Neutral")
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = {},
                edgePadding = 24.dp,
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    val isDisabled = autoHarmonize && index != 0 // Disable others if auto-harmonizing

                    Tab(
                        selected = isSelected,
                        onClick = { if (!isDisabled) selectedTab = index },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else -> Color.Transparent
                                }
                            ),
                        enabled = !isDisabled
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                isDisabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Color Picker for Active Color
            ColorEditor(
                color = activeColor,
                disabled = autoHarmonize && selectedTab != 0,
                onColorChange = { updateActiveColor(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeCreatorPreview(
    themeName: String,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    neutral: Color
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1C1C1E) else tertiary.copy(alpha = 0.3f)
    val onPrimaryColor = if (primary.luminance() > 0.5f) Color.Black else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(280.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.3f),
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
                        colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.15f))
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
                                    .background(Brush.linearGradient(listOf(primary, secondary)))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(neutral.copy(alpha = 0.3f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(primary)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = onPrimaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(
                        primary to 48.dp,
                        secondary to 36.dp,
                        tertiary to 28.dp
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
                    listOf(primary, secondary, neutral).forEach { color ->
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
                color = primary,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = themeName.ifBlank { "Preview" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimaryColor,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorEditor(
    color: Color,
    disabled: Boolean,
    onColorChange: (Color) -> Unit
) {
    // Hold HEX text locally to prevent cursor jumping when typing valid partial hexes
    var hexText by remember(color) { 
        mutableStateOf(String.format("#%06X", 0xFFFFFF and color.toArgb())) 
    }

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
            // Color Display & Hex Input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { newValue ->
                        hexText = newValue
                        if (newValue.length == 7 && newValue.startsWith("#")) {
                            try {
                                val parsed = Color(android.graphics.Color.parseColor(newValue))
                                onColorChange(parsed)
                            } catch (e: Exception) {
                                // Ignore invalid colors temporarily
                            }
                        }
                    },
                    label = { Text("HEX Code") },
                    enabled = !disabled,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // RGB Sliders using direct Color properties to prevent de-sync
            ColorSliderRow(
                label = "R", 
                value = color.red * 255f, 
                activeColor = Color.Red, 
                disabled = disabled,
                onValueChange = { onColorChange(Color(it / 255f, color.green, color.blue)) }
            )
            ColorSliderRow(
                label = "G", 
                value = color.green * 255f, 
                activeColor = Color.Green, 
                disabled = disabled,
                onValueChange = { onColorChange(Color(color.red, it / 255f, color.blue)) }
            )
            ColorSliderRow(
                label = "B", 
                value = color.blue * 255f, 
                activeColor = Color.Blue, 
                disabled = disabled,
                onValueChange = { onColorChange(Color(color.red, color.green, it / 255f)) }
            )
        }
    }
}

@Composable
private fun ColorSliderRow(
    label: String, 
    value: Float, 
    activeColor: Color, 
    disabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    val trackColor = if (disabled) MaterialTheme.colorScheme.surfaceVariant else activeColor
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.width(20.dp), 
            textAlign = TextAlign.Center,
            color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            enabled = !disabled,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = trackColor,
                thumbColor = trackColor,
                disabledActiveTrackColor = trackColor,
                disabledThumbColor = trackColor
            )
        )
        Text(
            text = value.toInt().toString(), 
            modifier = Modifier.width(36.dp), 
            textAlign = TextAlign.End,
            color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}
