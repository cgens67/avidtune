package com.cgens67.avidtune.ui.screens.settings

import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.ui.theme.AvidTuneTheme
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.utils.rememberPreference

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

    val initialPalette = remember {
        ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)
            ?: ThemeSeedPalette(DefaultThemeColor, DefaultThemeColor, DefaultThemeColor, DefaultThemeColor)
    }
    val initialName = remember {
        ThemeSeedPaletteCodec.extractNameFromJsonOrNull(customThemeColor) ?: "Custom Theme"
    }

    var themeName by remember { mutableStateOf(initialName) }
    var primary by remember { mutableStateOf(initialPalette.primary) }
    var secondary by remember { mutableStateOf(initialPalette.secondary) }
    var tertiary by remember { mutableStateOf(initialPalette.tertiary) }
    var neutral by remember { mutableStateOf(initialPalette.neutral) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Primary, 1: Secondary, 2: Tertiary, 3: Neutral

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

    val livePalette = remember(primary, secondary, tertiary, neutral) {
        ThemeSeedPalette(primary, secondary, tertiary, neutral)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_theme), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val finalName = if (themeName.isBlank()) "Custom Theme" else themeName
                        val json = ThemeSeedPaletteCodec.encodeForPreference(livePalette, finalName)
                        onCustomThemeColorChange(json)
                        Toast.makeText(context, "Theme Saved!", Toast.LENGTH_SHORT).show()
                        navController.navigateUp()
                    }) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
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

            // Real Live Material 3 Preview Rendering
            AvidTuneTheme(
                darkTheme = isSystemInDarkTheme(),
                customSeedPalette = livePalette
            ) {
                ThemeCreatorLivePreview(themeName = themeName)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = themeName,
                onValueChange = { themeName = it },
                label = { Text(stringResource(R.string.custom_theme)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Tabs
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
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .padding(horizontal = 4.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color Editor Controls for Active Role
            ColorEditor(
                color = activeColor,
                onColorChange = { updateActiveColor(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeCreatorLivePreview(
    themeName: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(260.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (themeName.isBlank()) "Preview" else themeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            // Content Preview Container using Material 3 Surface and Container roles
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSecondaryContainer)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Buttons displaying Primary, Secondary, and Tertiary Roles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Tertiary",
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Secondary",
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Primary",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
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
    onColorChange: (Color) -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                Column {
                    Text("Hex Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = hexString,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            ColorSliderRow(
                label = "R",
                value = red,
                activeColor = Color.Red,
                onValueChange = {
                    red = it
                    onColorChange(Color(red / 255f, green / 255f, blue / 255f))
                }
            )
            ColorSliderRow(
                label = "G",
                value = green,
                activeColor = Color.Green,
                onValueChange = {
                    green = it
                    onColorChange(Color(red / 255f, green / 255f, blue / 255f))
                }
            )
            ColorSliderRow(
                label = "B",
                value = blue,
                activeColor = Color.Blue,
                onValueChange = {
                    blue = it
                    onColorChange(Color(red / 255f, green / 255f, blue / 255f))
                }
            )
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
