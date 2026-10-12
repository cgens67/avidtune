@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import com.cgens67.avidtune.R
import com.cgens67.avidtune.models.MediaMetadata
import com.cgens67.avidtune.utils.ComposeToImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private enum class ShareMode { IMAGE, TEXT }
private enum class CustomizerSection { THEMES, STYLE, CANVAS, TYPOGRAPHY }

private fun Color.isDarkColor(): Boolean = this.luminance() < 0.5f

private fun createGradientFrom(color: Color): List<Color> {
    val isDark = color.isDarkColor()
    val factor = if (isDark) 1.5f else 0.75f
    val c2 = Color(
        red = (color.red * factor + if (isDark) 0.1f else 0f).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor + if (isDark) 0.15f else 0f).coerceIn(0f, 1f)
    )
    val c3 = Color(
        red = (color.red * if (isDark) 0.45f else 0.4f).coerceIn(0f, 1f),
        green = (color.green * if (isDark) 0.45f else 0.45f).coerceIn(0f, 1f),
        blue = (color.blue * if (isDark) 0.55f else 0.55f).coerceIn(0f, 1f)
    )
    return listOf(color, c2, c3)
}

@Composable
fun ShareLyricsDialog(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var shareMode by remember { mutableStateOf(ShareMode.IMAGE) }
    var activeSection by remember { mutableStateOf(CustomizerSection.THEMES) }
    var isGenerating by remember { mutableStateOf(false) }

    val coverUrl = mediaMetadata?.thumbnailUrl
    val paletteColors = remember { mutableStateListOf<Color>() }

    var customization by remember {
        mutableStateOf(
            colorPresets.first().customization.copy(
                cornerRadius = 24f,
                padding = 24f,
                fontStyle = FontStyle.EXTRA_BOLD
            )
        )
    }

    LaunchedEffect(coverUrl) {
        if (!coverUrl.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val loader = ImageLoader(context)
                    val req = ImageRequest.Builder(context).data(coverUrl).allowHardware(false).build()
                    val drawable = loader.execute(req).drawable
                    if (drawable != null) {
                        val bitmap = drawable.toBitmap()
                        val palette = Palette.from(bitmap).generate()
                        val colors = listOfNotNull(
                            palette.getLightVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getDarkVibrantColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getLightMutedColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() },
                            palette.getMutedColor(Color.Black.toArgb()).takeIf { it != Color.Black.toArgb() }
                        ).map { Color(it) }.distinct()

                        if (colors.isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                paletteColors.clear()
                                paletteColors.addAll(colors)
                                val primaryCoverColor = colors.first()
                                customization = customization.copy(
                                    backgroundColor = primaryCoverColor,
                                    textColor = if (primaryCoverColor.isDarkColor()) Color.White else Color.Black,
                                    secondaryTextColor = if (primaryCoverColor.isDarkColor()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                    isDark = primaryCoverColor.isDarkColor()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            ) {}
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    onClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.cancel),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Segmented Mode Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                ToggleButton(
                    checked = shareMode == ShareMode.IMAGE,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareMode = ShareMode.IMAGE
                    },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedLeadingButtonShapes()
                ) {
                    Icon(painterResource(R.drawable.image), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_as_image), fontWeight = FontWeight.SemiBold)
                }

                ToggleButton(
                    checked = shareMode == ShareMode.TEXT,
                    onCheckedChange = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        shareMode = ShareMode.TEXT
                    },
                    modifier = Modifier.weight(1f),
                    shapes = ButtonGroupDefaults.connectedTrailingButtonShapes()
                ) {
                    Icon(painterResource(R.drawable.media3_icon_share), null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.share_as_text), fontWeight = FontWeight.SemiBold)
                }
            }

            AnimatedContent(
                targetState = shareMode,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                            expandVertically(spring(stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(
                            fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                    shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                        )
                },
                label = "shareModeContent",
                modifier = Modifier.weight(1f)
            ) { mode ->
                if (mode == ShareMode.TEXT) {
                    TextQuoteMode(
                        lyricsText = lyricsText,
                        songTitle = songTitle,
                        artists = artists,
                        mediaMetadata = mediaMetadata,
                        onDismiss = onDismiss
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Live Card Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (mediaMetadata != null) {
                                val screenWidth = configuration.screenWidthDp.dp
                                val previewSize = min(screenWidth.value - 64f, 320f).dp

                                Box(
                                    modifier = Modifier
                                        .size(previewSize)
                                        .clip(RoundedCornerShape(customization.cornerRadius.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    LyricsImageCardPreview(
                                        lyricText = lyricsText,
                                        mediaMetadata = mediaMetadata,
                                        customization = customization,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Horizontally scrollable category pills
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeSection == CustomizerSection.THEMES,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.THEMES
                                },
                                label = { Text(stringResource(R.string.presets), maxLines = 1) },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.palette), null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.STYLE,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.STYLE
                                },
                                label = { Text(stringResource(R.string.details), maxLines = 1) },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.tune), null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.CANVAS,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.CANVAS
                                },
                                label = { Text("Canvas", maxLines = 1) },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.gradient), null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.TYPOGRAPHY,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.TYPOGRAPHY
                                },
                                label = { Text(stringResource(R.string.font_system), maxLines = 1) },
                                leadingIcon = {
                                    Icon(painterResource(R.drawable.text_fields), null, modifier = Modifier.size(16.dp))
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        // Customizer Options Container
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                when (activeSection) {
                                    CustomizerSection.THEMES -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            if (paletteColors.isNotEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.from_cover),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                                ) {
                                                    items(paletteColors) { color ->
                                                        PaletteSwatch(
                                                            color = color,
                                                            isSelected = customization.backgroundColor == color && customization.backgroundStyle != BackgroundStyle.GRADIENT,
                                                            onClick = {
                                                                val newGrad = if (customization.backgroundStyle == BackgroundStyle.GRADIENT) createGradientFrom(color) else null
                                                                customization = customization.copy(
                                                                    backgroundColor = color,
                                                                    gradientColors = newGrad,
                                                                    textColor = if (color.isDarkColor()) Color.White else Color.Black,
                                                                    secondaryTextColor = if (color.isDarkColor()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                                                    isDark = color.isDarkColor()
                                                                )
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = stringResource(R.string.color_palette),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                contentPadding = PaddingValues(horizontal = 2.dp)
                                            ) {
                                                items(colorPresets) { preset ->
                                                    BottomSheetColorCustomizationItem(
                                                        customization = preset.customization,
                                                        presetName = preset.name,
                                                        isSelected = customization.backgroundColor == preset.customization.backgroundColor &&
                                                                customization.backgroundStyle == preset.customization.backgroundStyle,
                                                        onClick = {
                                                            customization = preset.customization.copy(
                                                                cornerRadius = customization.cornerRadius,
                                                                padding = customization.padding
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    CustomizerSection.STYLE -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text(stringResource(R.string.cover_art), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    CoverArtStyle.ROUNDED to "Rounded",
                                                    CoverArtStyle.CIRCLE to "Circle",
                                                    CoverArtStyle.SQUARE to "Square"
                                                ).forEach { (style, name) ->
                                                    FilterChip(
                                                        selected = customization.coverArtStyle == style,
                                                        onClick = { customization = customization.copy(coverArtStyle = style) },
                                                        label = { Text(name, fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            Text(stringResource(R.string.lyrics), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    LyricsStyle.NORMAL to "Normal",
                                                    LyricsStyle.ITALIC to "Italic",
                                                    LyricsStyle.CONDENSED to "Condensed"
                                                ).forEach { (style, name) ->
                                                    FilterChip(
                                                        selected = customization.lyricsStyle == style,
                                                        onClick = { customization = customization.copy(lyricsStyle = style) },
                                                        label = { Text(name, fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveToggleRow(
                                                title = "Show Accent Line",
                                                checked = customization.showAccentLine,
                                                onCheckedChange = {
                                                    customization = customization.copy(
                                                        showAccentLine = it,
                                                        accentColor = if (it && customization.accentColor == null) customization.textColor else customization.accentColor
                                                    )
                                                }
                                            )

                                            ExpressiveSliderRow(
                                                label = "Line Spacing",
                                                value = customization.lyricsLineSpacing,
                                                range = 1.0f..1.8f,
                                                onValueChange = { customization = customization.copy(lyricsLineSpacing = it) },
                                                format = String.format("%.1f", customization.lyricsLineSpacing)
                                            )
                                        }
                                    }

                                    CustomizerSection.CANVAS -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text(stringResource(R.string.player_background_style), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    BackgroundStyle.SOLID to "Solid",
                                                    BackgroundStyle.GRADIENT to "Gradient",
                                                    BackgroundStyle.PATTERN to "Pattern"
                                                ).forEach { (style, name) ->
                                                    FilterChip(
                                                        selected = customization.backgroundStyle == style,
                                                        onClick = {
                                                            val grad = if (style == BackgroundStyle.GRADIENT) {
                                                                customization.gradientColors?.takeIf { it.size >= 2 } ?: createGradientFrom(customization.backgroundColor)
                                                            } else null

                                                            customization = customization.copy(
                                                                backgroundStyle = style,
                                                                gradientColors = grad,
                                                                patternOpacity = if (style == BackgroundStyle.PATTERN) 0.18f else customization.patternOpacity
                                                            )
                                                        },
                                                        label = { Text(name, fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveSliderRow(
                                                label = "Corner Radius",
                                                value = customization.cornerRadius,
                                                range = 0f..40f,
                                                onValueChange = { customization = customization.copy(cornerRadius = it) },
                                                format = "${customization.cornerRadius.toInt()}dp"
                                            )

                                            ExpressiveSliderRow(
                                                label = "Padding",
                                                value = customization.padding,
                                                range = 14f..48f,
                                                onValueChange = { customization = customization.copy(padding = it) },
                                                format = "${customization.padding.toInt()}dp"
                                            )

                                            ExpressiveToggleRow(
                                                title = "Border",
                                                checked = customization.borderEnabled,
                                                onCheckedChange = {
                                                    val autoBorderColor = if (customization.backgroundColor.isDarkColor()) {
                                                        Color.White.copy(alpha = 0.6f)
                                                    } else {
                                                        Color.Black.copy(alpha = 0.6f)
                                                    }
                                                    customization = customization.copy(
                                                        borderEnabled = it,
                                                        borderColor = autoBorderColor,
                                                        borderWidth = if (customization.borderWidth <= 0f) 2f else customization.borderWidth
                                                    )
                                                }
                                            )

                                            AnimatedVisibility(visible = customization.borderEnabled) {
                                                ExpressiveSliderRow(
                                                    label = "Border Width",
                                                    value = customization.borderWidth,
                                                    range = 1f..8f,
                                                    onValueChange = { customization = customization.copy(borderWidth = it) },
                                                    format = "${customization.borderWidth.toInt()}dp"
                                                )
                                            }
                                        }
                                    }

                                    CustomizerSection.TYPOGRAPHY -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text("Alignment", style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    TextAlignment.LEFT to stringResource(R.string.left),
                                                    TextAlignment.CENTER to stringResource(R.string.center),
                                                    TextAlignment.RIGHT to stringResource(R.string.right)
                                                ).forEach { (align, name) ->
                                                    FilterChip(
                                                        selected = customization.textAlignment == align,
                                                        onClick = { customization = customization.copy(textAlignment = align) },
                                                        label = { Text(name, fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveToggleRow(
                                                title = "Text Shadow",
                                                checked = customization.textShadowEnabled,
                                                onCheckedChange = { customization = customization.copy(textShadowEnabled = it) }
                                            )

                                            ExpressiveToggleRow(
                                                title = "Show Cover Art",
                                                checked = customization.showCoverArt,
                                                onCheckedChange = { customization = customization.copy(showCoverArt = it) }
                                            )

                                            ExpressiveToggleRow(
                                                title = "App Logo",
                                                checked = customization.showLogo,
                                                onCheckedChange = { customization = customization.copy(showLogo = it) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Bottom Actions Bar
            if (shareMode == ShareMode.IMAGE) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }

                        Button(
                            onClick = {
                                if (isGenerating) return@Button
                                isGenerating = true
                                coroutineScope.launch {
                                    try {
                                        val screenWidth = configuration.screenWidthDp
                                        val screenHeight = configuration.screenHeightDp

                                        val image = ComposeToImage.createLyricsImage(
                                            context = context,
                                            coverArtUrl = coverUrl,
                                            songTitle = songTitle,
                                            artistName = artists,
                                            lyrics = lyricsText,
                                            width = (screenWidth * density.density).toInt(),
                                            height = (screenHeight * density.density).toInt(),
                                            backgroundColor = customization.backgroundColor.toArgb(),
                                            textColor = customization.textColor.toArgb(),
                                            secondaryTextColor = customization.secondaryTextColor.toArgb(),
                                            showCoverArt = customization.showCoverArt,
                                            showLogo = customization.showLogo,
                                            backgroundStyle = customization.backgroundStyle.name,
                                            gradientColors = customization.gradientColors?.map { it.toArgb() }?.toIntArray(),
                                            fontStyle = customization.fontStyle.name,
                                            logoPosition = customization.logoPosition.name,
                                            cornerRadius = customization.cornerRadius,
                                            patternOpacity = customization.patternOpacity,
                                            textAlignment = customization.textAlignment.name,
                                            padding = customization.padding,
                                            showArtistName = customization.showArtistName,
                                            showSongTitle = customization.showSongTitle,
                                            textShadowEnabled = customization.textShadowEnabled,
                                            borderEnabled = customization.borderEnabled,
                                            borderColor = customization.borderColor.toArgb(),
                                            borderWidth = customization.borderWidth,
                                            logoSize = customization.logoSize.name,
                                            coverArtStyle = customization.coverArtStyle.name,
                                            lyricsStyle = customization.lyricsStyle.name,
                                            showAccentLine = customization.showAccentLine,
                                            accentColor = customization.accentColor?.toArgb(),
                                            spacingBetweenElements = customization.spacingBetweenElements,
                                            lyricsLineSpacing = customization.lyricsLineSpacing
                                        )

                                        val timestamp = System.currentTimeMillis()
                                        val safeSongTitle = songTitle.replace("[^a-zA-Z0-9\\s]".toRegex(), "").trim().replace("\\s+".toRegex(), "_")
                                        val filename = "lyrics_${safeSongTitle}_$timestamp"

                                        val uri = ComposeToImage.saveBitmapAsFile(context, image, filename)

                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }

                                        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics_image)))
                                        sheetState.hide()
                                        onDismiss()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isGenerating = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            if (isGenerating) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.generating_image), maxLines = 1)
                            } else {
                                Icon(painterResource(R.drawable.media3_icon_share), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.share_as_image),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
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
private fun TextQuoteMode(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "“$lyricsText”",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )

                Column(modifier = Modifier.padding(top = 20.dp)) {
                    Text(
                        text = songTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = artists,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Lyrics", "\"$lyricsText\"\n\n$songTitle - $artists"))
                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(painterResource(R.drawable.content_copy), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_copy))
            }

            Button(
                onClick = {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        val songLink = "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                        putExtra(Intent.EXTRA_TEXT, "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_lyrics)))
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(painterResource(R.drawable.media3_icon_share), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share))
            }
        }
    }
}

@Composable
private fun PaletteSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val circleSize by animateDpAsState(
        targetValue = if (isSelected) 44.dp else 36.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "swatchSize"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .zIndex(if (isSelected) 1f else 0f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(color)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    tint = if (color.isDarkColor()) Color.White else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun BottomSheetColorCustomizationItem(
    customization: ImageCustomization,
    presetName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val swatchSize by animateDpAsState(
        targetValue = if (isSelected) 48.dp else 42.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "presetSize"
    )

    Column(
        modifier = modifier
            .width(64.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .zIndex(if (isSelected) 1f else 0f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(swatchSize)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        brush = if (customization.backgroundStyle == BackgroundStyle.GRADIENT && customization.gradientColors != null) {
                            Brush.linearGradient(customization.gradientColors)
                        } else {
                            Brush.linearGradient(listOf(customization.backgroundColor, customization.backgroundColor))
                        }
                    )
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    color = customization.textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = presetName,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp)
        )
    }
}

@Composable
private fun ExpressiveSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    format: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(format, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range
        )
    }
}

@Composable
private fun ExpressiveToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
