package com.cgens67.avidtune.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AppFont
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.google.material.color.dynamiccolor.DynamicScheme
import com.google.material.color.hct.Hct
import com.google.material.color.scheme.SchemeTonalSpot
import com.google.material.color.score.Score

val DefaultThemeColor = Color(0xFF4285F4)

val sfProDisplayBold = FontFamily(Font(R.font.sfprodisplaybold))
val googleSansBold = FontFamily(Font(R.font.googlesansbold))
val spaceGroteskBold = FontFamily(Font(R.font.spacegroteskbold))

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AvidTuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    expressive: Boolean = true,
    themeColor: Color = DefaultThemeColor,
    appFont: AppFont = AppFont.SYSTEM,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colorScheme = remember(darkTheme, pureBlack, themeColor) {
        if (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (darkTheme) {
                dynamicDarkColorScheme(context).pureBlack(pureBlack, darkTheme)
            } else {
                dynamicLightColorScheme(context).pureBlack(false, darkTheme)
            }
        } else {
            SchemeTonalSpot(Hct.fromInt(themeColor.toArgb()), darkTheme, 0.0)
                .toColorScheme()
                .pureBlack(pureBlack, darkTheme)
        }
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
            // Adjust tracking for custom fonts which often look too loose with Roboto's default letter spacing
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

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = MaterialTheme.shapes,
        motionScheme = motionScheme,
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xFF4285F4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

object PlayerColorExtractor {
    val gradientCache = android.util.LruCache<String, List<Color>>(50)

    fun extractGradientColors(
        palette: Palette,
        fallbackColor: Int = Color(0xFF595959).toArgb()
    ): List<Color> {
        val extractedColors = palette.swatches
            .associate { it.rgb to it.population }

        val orderedColors = Score.score(extractedColors, 2, fallbackColor, true)
            .sortedByDescending { Color(it).luminance() }

        return if (orderedColors.size >= 2) {
            listOf(Color(orderedColors[0]), Color(orderedColors[1]))
        } else {
            listOf(Color(0xFF595959), Color(0xFF0D0D0D))
        }
    }
}

fun DynamicScheme.toColorScheme() =
    ColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(primary),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
    )

fun ColorScheme.pureBlack(apply: Boolean, isDarkTheme: Boolean) =
    if (apply && isDarkTheme) {
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

object PlayerSliderColors {
    @Composable
    fun getSliderColors(
        textButtonColor: Color,
        playerBackground: PlayerBackgroundStyle,
        useDarkTheme: Boolean
    ) = SliderDefaults.colors(
        activeTrackColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        },
        inactiveTrackColor = when {
            useDarkTheme -> Color.Gray.copy(alpha = 0.5f)
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        activeTickColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        },
        inactiveTickColor = Color.Gray,
        thumbColor = when (playerBackground) {
            PlayerBackgroundStyle.DEFAULT -> textButtonColor
            PlayerBackgroundStyle.BLUR -> Color.White
            PlayerBackgroundStyle.GRADIENT -> Color.White
            PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
            PlayerBackgroundStyle.LIVE_MESH -> Color.White
        }
    )
}
