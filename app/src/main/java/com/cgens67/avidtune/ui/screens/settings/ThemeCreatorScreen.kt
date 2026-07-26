package com.cgens67.avidtune.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.cgens67.avidtune.LocalPlayerAwareWindowInsets
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.CustomThemeColorKey
import com.cgens67.avidtune.ui.theme.DefaultThemeColor
import com.cgens67.avidtune.ui.theme.ThemeSeedPalette
import com.cgens67.avidtune.ui.theme.ThemeSeedPaletteCodec
import com.cgens67.avidtune.utils.rememberPreference
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot

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
    var seedColor by remember { mutableStateOf(initialPalette.primary) }

    // Material 3 Dynamic Scheme Generation based on Seed
    val isDark = isSystemInDarkTheme()
    val generatedScheme = remember(seedColor, isDark) {
        SchemeTonalSpot(Hct.fromInt(seedColor.toArgb()), isDark, 0.0)
    }

    val primary = Color(generatedScheme.primary)
    val secondary = Color(generatedScheme.secondary)
    val tertiary = Color(generatedScheme.tertiary)
    val neutral = Color(generatedScheme.neutralVariant)
    val onPrimary = Color(generatedScheme.onPrimary)

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
                    Button(
                        onClick = {
                            val finalName = themeName.ifBlank { "Custom Theme" }
                            val palette = ThemeSeedPalette(seedColor, secondary, tertiary, neutral)
                            val json = ThemeSeedPaletteCodec.encodeForPreference(palette, finalName)
                            onCustomThemeColorChange(json)
                            Toast.makeText(context, "Theme Saved!", Toast.LENGTH_SHORT).show()
                            navController.navigateUp()
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
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

            // Explanation Text
            Text(
                text = "AvidTune uses Material You. Pick a single Seed Color, and the engine will automatically calculate the perfect matching Secondary, Tertiary, and Neutral colors.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Live Preview Area
            ThemeCreatorPreview(
                themeName = themeName,
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral,
                onPrimary = onPrimary
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            ColorEditor(
                color = seedColor,
                onColorChange = { seedColor = it },
                onRandomize = {
                    seedColor = Color(
                        red = (0..255).random() / 255f,
                        green = (0..255).random() / 255f,
                        blue = (0..255).random() / 255f
                    )
                }
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
    neutral: Color,
    onPrimary: Color
) {
    val isDark = isSystemInDarkTheme()
    
    val animatedPrimary by animateColorAsState(primary, tween(400, easing = FastOutSlowInEasing), label = "")
    val animatedSecondary by animateColorAsState(secondary, tween(400, easing = FastOutSlowInEasing), label = "")
    val animatedTertiary by animateColorAsState(tertiary, tween(400, easing = FastOutSlowInEasing), label = "")
    val animatedNeutral by animateColorAsState(neutral, tween(400, easing = FastOutSlowInEasing), label = "")

    val bgColor = if (isDark) Color(0xFF1C1C1E) else animatedTertiary.copy(alpha = 0.3f)
    val surfaceColor = if (isDark) animatedPrimary.copy(alpha = 0.15f) else Color.White
    val onSurfaceColor = if (isDark) Color.White else Color.Black

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(280.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gradientBrush = Brush.radialGradient(
                    colors = listOf(animatedPrimary.copy(alpha = 0.3f), Color.Transparent),
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
                        colors = CardDefaults.cardColors(containerColor = surfaceColor)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(animatedPrimary, animatedSecondary)))
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
                            tint = onPrimary,
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
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
                    text = themeName.ifBlank { "Preview" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ColorEditor(
    color: Color,
    onColorChange: (Color) -> Unit,
    onRandomize: () -> Unit
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
                        Text("Seed Color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(hexString, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
                
                IconButton(
                    onClick = onRandomize,
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                ) {
                    Icon(painterResource(R.drawable.shuffle), contentDescription = "Randomize", tint = MaterialTheme.colorScheme.onSecondaryContainer)
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
        }
    }
}

@Composable
private fun ColorSliderRow(label: String, value: Float, activeColor: Color, onValueChange: (Float) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
        Text(value.toInt().toString(), modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
