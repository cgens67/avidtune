package com.cgens67.avidtune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeMonochrome
import com.google.material.color.scheme.SchemeNeutral
import com.google.material.color.scheme.SchemeTonalSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val DefaultThemeColor = Color(0xFF4285F4)

val sfProDisplayBold = FontFamily(Font(R.font.sfprodisplaybold))
val googleSansBold = FontFamily(Font(R.font.googlesansbold))
val spaceGroteskBold = FontFamily(Font(R.font.spacegroteskbold))

enum class PaletteStyle { Monochrome, Neutral, TonalSpot }

private fun paletteStyleFor(seedColor: Color): PaletteStyle {
    val chroma = Hct.fromInt(seedColor.toArgb()).chroma
    return when {
        chroma < 4.0 -> PaletteStyle.Monochrome
        chroma < 12.0 -> PaletteStyle.Neutral
        else -> PaletteStyle.TonalSpot
    }
}

private fun materialKolorDynamicColorScheme(
    seedColor: Color,
    isDark: Boolean,
    contrastLevel: Double,
    style: PaletteStyle
): ColorScheme {
    val hct = Hct.fromInt(seedColor.toArgb())
    val scheme = when (style) {
        PaletteStyle.Monochrome -> SchemeMonochrome(hct, isDark, contrastLevel)
        PaletteStyle.Neutral -> SchemeNeutral(hct, isDark, contrastLevel)
        PaletteStyle.TonalSpot -> SchemeTonalSpot(hct, isDark, contrastLevel)
    }
    return scheme.toColorScheme()
}

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
): ColorScheme =
    mergedSeedColorScheme(
        primarySeed = palette.primary,
        secondarySeed = palette.secondary,
        tertiarySeed = palette.tertiary,
        neutralSeed = palette.neutral,
        isDark = isDark,
    )

private fun mergedSeedColorScheme(
    primarySeed: Color,
    secondarySeed: Color,
    tertiarySeed: Color,
    neutralSeed: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    style: PaletteStyle = paletteStyleFor(primarySeed),
): ColorScheme {
    val primaryScheme = materialKolorDynamicColorScheme(primarySeed, isDark, contrastLevel, style)
    val secondaryScheme = materialKolorDynamicColorScheme(secondarySeed, isDark, contrastLevel, paletteStyleFor(secondarySeed))
    val tertiaryScheme = materialKolorDynamicColorScheme(tertiarySeed, isDark, contrastLevel, paletteStyleFor(tertiarySeed))
    val neutralScheme = materialKolorDynamicColorScheme(neutralSeed, isDark, contrastLevel, paletteStyleFor(neutralSeed))

    return ColorScheme(
        primary = primaryScheme.primary,
        onPrimary = primaryScheme.onPrimary,
        primaryContainer = primaryScheme.primaryContainer,
        onPrimaryContainer = primaryScheme.onPrimaryContainer,
        inversePrimary = primaryScheme.inversePrimary,
        secondary = secondaryScheme.primary,
        onSecondary = secondaryScheme.onPrimary,
        secondaryContainer = secondaryScheme.primaryContainer,
        onSecondaryContainer = secondaryScheme.onPrimaryContainer,
        tertiary = tertiaryScheme.primary,
        onTertiary = tertiaryScheme.onPrimary,
        tertiaryContainer = tertiaryScheme.primaryContainer,
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer,
        background = neutralScheme.background,
        onBackground = neutralScheme.onBackground,
        surface = neutralScheme.surface,
        onSurface = neutralScheme.onSurface,
        surfaceVariant = neutralScheme.surfaceVariant,
        onSurfaceVariant = neutralScheme.onSurfaceVariant,
        inverseSurface = neutralScheme.inverseSurface,
        inverseOnSurface = neutralScheme.inverseOnSurface,
        surfaceBright = neutralScheme.surfaceBright,
        surfaceDim = neutralScheme.surfaceDim,
        surfaceContainer = neutralScheme.surfaceContainer,
        surfaceContainerLow = neutralScheme.surfaceContainerLow,
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest,
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh,
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest,
        outline = neutralScheme.outline,
        outlineVariant = neutralScheme.outlineVariant,
        error = primaryScheme.error,
        onError = primaryScheme.onError,
        errorContainer = primaryScheme.errorContainer,
        onErrorContainer = primaryScheme.onErrorContainer,
        scrim = neutralScheme.scrim,
        surfaceTint = primaryScheme.surfaceTint,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvidTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    seedPalette: ThemeSeedPalette? = null,
    appFont: AppFont = AppFont.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor =
        (seedPalette == null && themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val paletteStyle = remember(themeColor, seedPalette) {
        paletteStyleFor(seedPalette?.primary ?: themeColor)
    }

    val appColorScheme = remember(seedPalette, themeColor, darkTheme) {
        if (seedPalette != null) {
            exactPaletteColorScheme(
                palette = seedPalette,
                isDark = darkTheme,
            )
        } else {
            materialKolorDynamicColorScheme(
                seedColor = themeColor,
                isDark = darkTheme,
                contrastLevel = 0.0,
                style = paletteStyle,
            )
        }
    }

    val baseColorScheme = if (useSystemDynamicColor) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        appColorScheme
    }

    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
    }

    val motionScheme = if (expressive) {
        MotionScheme.expressive()
    } else {
        MotionScheme.standard()
    }

    val typography = remember(appFont) {
        val default = Typography()
        val customFontFamily = when(appFont) {
            AppFont.SYSTEM -> null
            AppFont.SF_PRO -> sfProDisplayBold
            AppFont.GOOGLE_SANS -> googleSansBold
            AppFont.SPACE_GROTESK -> spaceGroteskBold
        }

        if (customFontFamily == null) {
            default
        } else {
            val titleLetterSpacing = when (appFont) {
                AppFont.GOOGLE_SANS -> (-0.4).sp
                AppFont.SPACE_GROTESK -> (-0.5).sp
                AppFont.SF_PRO -> (-0.2).sp
                else -> 0.sp
            }
            
            val bodyLetterSpacing = when (appFont) {
                AppFont.GOOGLE_SANS -> 0.sp
                AppFont.SPACE_GROTESK -> (-0.2).sp
                AppFont.SF_PRO -> 0.sp
                else -> 0.sp
            }

            Typography(
                displayLarge = default.displayLarge.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                displayMedium = default.displayMedium.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                displaySmall = default.displaySmall.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                headlineLarge = default.headlineLarge.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                headlineMedium = default.headlineMedium.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                headlineSmall = default.headlineSmall.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                titleLarge = default.titleLarge.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                titleMedium = default.titleMedium.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                titleSmall = default.titleSmall.copy(fontFamily = customFontFamily, letterSpacing = titleLetterSpacing),
                bodyLarge = default.bodyLarge.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing),
                bodyMedium = default.bodyMedium.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing),
                bodySmall = default.bodySmall.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing),
                labelLarge = default.labelLarge.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing),
                labelMedium = default.labelMedium.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing),
                labelSmall = default.labelSmall.copy(fontFamily = customFontFamily, letterSpacing = bodyLetterSpacing)
            )
        }
    }

    CompositionLocalProvider(
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = MaterialTheme.shapes,
            motionScheme = motionScheme,
            content = content
        )
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = com.google.material.color.score.Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = com.google.material.color.score.Score.score(extractedColors, 2, 0xFF4285F4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

object PlayerColorExtractor {
    val gradientCache = android.util.LruCache<String, List<Color>>(50)

    suspend fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int
    ): List<Color> = withContext(Dispatchers.Default) {
        val allSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.lightVibrantSwatch,
            palette.darkVibrantSwatch,
            palette.dominantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch,
        ).distinctBy { it.rgb }

        val rankedSwatches = allSwatches.sortedByDescending { calculateColorWeight(it) }
        val availableColors = mutableListOf<Color>()

        fun addIfUnique(color: Color, saturationFactor: Float) {
            if (!isSimilarToAny(color, availableColors)) {
                availableColors.add(enhanceColorVividness(color, saturationFactor))
            }
        }

        for (swatch in rankedSwatches) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(swatch.rgb, hsv)
            val satFactor = if (hsv[1] > 0.3f) 1.25f else 1.05f
            addIfUnique(Color(swatch.rgb), satFactor)
            if (availableColors.size >= 6) break
        }

        val totalPopulation = allSwatches.sumOf { it.population }.coerceAtLeast(1)
        val weightedExtractedSaturation = allSwatches.sumOf { swatch ->
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(swatch.rgb, hsv)
            (hsv[1] * swatch.population).toDouble()
        }.toFloat() / totalPopulation.toFloat()

        val dominantColor = availableColors.firstOrNull() ?: Color(fallbackColor)
        val isGreyscaleImage = weightedExtractedSaturation < 0.22f || isNearGray(dominantColor)

        if (isGreyscaleImage) {
            availableColors.clear()
            val baseBrightness = allSwatches.maxByOrNull { it.population }?.let { swatch ->
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(swatch.rgb, hsv)
                hsv[2]
            } ?: 0.10f
            val greyStops = floatArrayOf(
                (baseBrightness * 1.2f).coerceIn(0.06f, 0.40f),
                (baseBrightness * 0.9f).coerceIn(0.04f, 0.28f),
                (baseBrightness * 0.6f).coerceIn(0.02f, 0.16f),
                (baseBrightness * 1.4f).coerceIn(0.08f, 0.44f),
                (baseBrightness * 0.7f).coerceIn(0.03f, 0.20f),
                (baseBrightness * 0.5f).coerceIn(0.01f, 0.12f),
            )
            while (availableColors.size < 6) {
                val v = greyStops[availableColors.size % greyStops.size]
                availableColors.add(Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, v))))
            }
            return@withContext availableColors
        }

        val fallbackSeed = Color(fallbackColor).takeUnless { isNearGray(it) }
            ?: palette.dominantSwatch?.let { Color(it.rgb) }?.takeUnless { isNearGray(it) }
            ?: Color.DarkGray

        val seed = availableColors.firstOrNull() ?: fallbackSeed
        val targets = listOf(25f, -25f, 55f, -55f, 120f, -120f, 180f, 150f, -150f)
        val valueTargets = floatArrayOf(0.82f, 0.74f, 0.68f, 0.6f, 0.86f, 0.7f)

        val baseCandidates = (availableColors.toList() + seed).distinct()
        var baseIndex = 0
        var targetIndex = 0
        while (availableColors.size < 6) {
            val baseColor = baseCandidates[baseIndex % baseCandidates.size]
            val hueShiftDegrees = targets[targetIndex % targets.size]
            val valueTarget = valueTargets[availableColors.size % valueTargets.size]
            val derived = tuneColorForMesh(
                hueShift(baseColor, hueShiftDegrees),
                saturationMin = 0.62f,
                saturationBoost = 1.08f,
                valueTarget = valueTarget,
                valueMin = 0.38f,
                valueMax = 0.9f,
            )
            if (!isSimilarToAny(derived, availableColors)) {
                availableColors.add(derived)
            }
            baseIndex++
            targetIndex++
            if (baseIndex > 40) break
        }

        if (availableColors.isEmpty()) {
            availableColors.add(tuneColorForMesh(fallbackSeed, 0.62f, 1.08f, 0.75f, 0.38f, 0.9f))
        }

        return@withContext availableColors
    }

    private fun enhanceColorVividness(color: Color, saturationFactor: Float = 1.4f): Color {
        val argb = color.toArgb()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(argb, hsv)
        hsv[1] = (hsv[1] * saturationFactor).coerceAtMost(1.0f)
        hsv[2] = (hsv[2] * 1.02f).coerceIn(0.32f, 0.88f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun calculateColorWeight(swatch: Palette.Swatch?): Float {
        if (swatch == null) return 0f
        val population = swatch.population.toFloat()
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(swatch.rgb, hsv)
        val saturation = hsv[1]
        val brightness = hsv[2]
        val vibrancyBonus = if (saturation > 0.3f && brightness in 0.2f..0.9f) 1.3f else 1.0f
        return population * vibrancyBonus
    }

    private fun isSimilarColor(color1: Color?, color2: Color?): Boolean {
        if (color1 == null || color2 == null) return false
        val hsv1 = FloatArray(3)
        val hsv2 = FloatArray(3)
        android.graphics.Color.colorToHSV(color1.toArgb(), hsv1)
        android.graphics.Color.colorToHSV(color2.toArgb(), hsv2)

        val hueDiffRaw = kotlin.math.abs(hsv1[0] - hsv2[0])
        val hueDiff = kotlin.math.min(hueDiffRaw, 360f - hueDiffRaw)
        val satDiff = kotlin.math.abs(hsv1[1] - hsv2[1])
        val valueDiff = kotlin.math.abs(hsv1[2] - hsv2[2])
        if (hueDiff < 12f && satDiff < 0.12f && valueDiff < 0.12f) return true

        val threshold = 28
        val r1 = (color1.red * 255).toInt()
        val g1 = (color1.green * 255).toInt()
        val b1 = (color1.blue * 255).toInt()
        val r2 = (color2.red * 255).toInt()
        val g2 = (color2.green * 255).toInt()
        val b2 = (color2.blue * 255).toInt()

        return kotlin.math.abs(r1 - r2) < threshold &&
            kotlin.math.abs(g1 - g2) < threshold &&
            kotlin.math.abs(b1 - b2) < threshold
    }

    private fun isSimilarToAny(color: Color, colors: List<Color>): Boolean = colors.any { isSimilarColor(color, it) }

    private fun hueShift(color: Color, degrees: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun tuneColorForMesh(
        color: Color,
        saturationMin: Float,
        saturationBoost: Float,
        valueTarget: Float,
        valueMin: Float,
        valueMax: Float,
    ): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[1] = (kotlin.math.max(hsv[1], saturationMin) * saturationBoost).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * 0.85f + valueTarget * 0.15f).coerceIn(valueMin, valueMax)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    private fun isNearGray(color: Color): Boolean {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        return hsv[1] < 0.15f || hsv[2] < 0.08f
    }
}

fun DynamicScheme.toColorScheme() = ColorScheme(
    primary = Color(this.primary),
    onPrimary = Color(this.onPrimary),
    primaryContainer = Color(this.primaryContainer),
    onPrimaryContainer = Color(this.onPrimaryContainer),
    inversePrimary = Color(this.inversePrimary),
    secondary = Color(this.secondary),
    onSecondary = Color(this.onSecondary),
    secondaryContainer = Color(this.secondaryContainer),
    onSecondaryContainer = Color(this.onSecondaryContainer),
    tertiary = Color(this.tertiary),
    onTertiary = Color(this.onTertiary),
    tertiaryContainer = Color(this.tertiaryContainer),
    onTertiaryContainer = Color(this.onTertiaryContainer),
    background = Color(this.background),
    onBackground = Color(this.onBackground),
    surface = Color(this.surface),
    onSurface = Color(this.onSurface),
    surfaceVariant = Color(this.surfaceVariant),
    onSurfaceVariant = Color(this.onSurfaceVariant),
    surfaceTint = Color(this.primary),
    inverseSurface = Color(this.inverseSurface),
    inverseOnSurface = Color(this.inverseOnSurface),
    error = Color(this.error),
    onError = Color(this.onError),
    errorContainer = Color(this.errorContainer),
    onErrorContainer = Color(this.onErrorContainer),
    outline = Color(this.outline),
    outlineVariant = Color(this.outlineVariant),
    scrim = Color(this.scrim),
    surfaceBright = Color(this.surfaceBright),
    surfaceDim = Color(this.surfaceDim),
    surfaceContainer = Color(this.surfaceContainer),
    surfaceContainerHigh = Color(this.surfaceContainerHigh),
    surfaceContainerHighest = Color(this.surfaceContainerHighest),
    surfaceContainerLow = Color(this.surfaceContainerLow),
    surfaceContainerLowest = Color(this.surfaceContainerLowest),
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) {
        copy(
            surface = Color.Black,
            background = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainerLowest = Color.Black,
        )
    } else {
        this
    }

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
