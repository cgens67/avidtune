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
import androidx.compose.ui.geometry.Offset
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
            neutral = Color(scheme.surfaceVariant)
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
    val bgColor = if (isDark) Color(0xFF121212) else Color(0xFFF0F0F0)
    val surfaceColor = if (isDark) Color(0xFF1E1E1E) else Color.White
    val onSurfaceColor = if (isDark) Color.White else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(240.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Mock App Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = themeName.ifBlank { "Preview" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = onSurfaceColor
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(primary)
                )
            }

            // Mock Content Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(secondary))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Box(modifier = Modifier.width(100.dp).height(12.dp).clip(CircleShape).background(neutral))
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(neutral.copy(alpha = 0.5f)))
                        }
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))

                    // Mock Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tertiary", color = if (tertiary.luminance() > 0.5f) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Primary", color = if (primary.luminance() > 0.5f) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
