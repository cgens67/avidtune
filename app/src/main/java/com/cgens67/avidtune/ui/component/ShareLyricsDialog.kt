@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)

package com.cgens67.avidtune.ui.component

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private enum class CustomizerSection { PRESETS, CANVAS, STYLE, TYPOGRAPHY }

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
    var activeSection by remember { mutableStateOf(CustomizerSection.PRESETS) }
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

    // Extract dynamic palette from album cover
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
                                // Set first extracted vibrant color as active
                                val primaryCoverColor = colors.first()
                                customization = customization.copy(
                                    backgroundColor = primaryCoverColor,
                                    textColor = if (primaryCoverColor.isDark()) Color.White else Color.Black,
                                    secondaryTextColor = if (primaryCoverColor.isDark()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                    isDark = primaryCoverColor.isDark()
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
            // Top Bar
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

            // Mode Selector: Image vs Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
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
                    Icon(
                        painter = painterResource(R.drawable.image),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
                    Icon(
                        painter = painterResource(R.drawable.media3_icon_share),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
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
                        // Interactive Live Card Preview
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
                                        .scale(0.98f),
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

                        // Studio Section Selector (Pills)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeSection == CustomizerSection.PRESETS,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.PRESETS
                                },
                                label = { Text(stringResource(R.string.presets)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.STYLE,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.STYLE
                                },
                                label = { Text(stringResource(R.string.details)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.CANVAS,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.CANVAS
                                },
                                label = { Text(stringResource(R.string.player_background_style)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            FilterChip(
                                selected = activeSection == CustomizerSection.TYPOGRAPHY,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    activeSection = CustomizerSection.TYPOGRAPHY
                                },
                                label = { Text(stringResource(R.string.font_system)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        // Section Controls
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                when (activeSection) {
                                    CustomizerSection.PRESETS -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            if (paletteColors.isNotEmpty()) {
                                                Text(
                                                    text = stringResource(R.string.from_cover),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    items(paletteColors) { color ->
                                                        PaletteSwatch(
                                                            color = color,
                                                            isSelected = customization.backgroundColor == color,
                                                            onClick = {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                customization = customization.copy(
                                                                    backgroundColor = color,
                                                                    textColor = if (color.isDark()) Color.White else Color.Black,
                                                                    secondaryTextColor = if (color.isDark()) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                                                                    backgroundStyle = BackgroundStyle.SOLID,
                                                                    isDark = color.isDark()
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
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                items(colorPresets) { preset ->
                                                    BottomSheetColorCustomizationItem(
                                                        customization = preset.customization,
                                                        presetName = preset.name,
                                                        isSelected = customization.backgroundColor == preset.customization.backgroundColor &&
                                                                customization.backgroundStyle == preset.customization.backgroundStyle,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                                                    CoverArtStyle.ROUNDED to R.string.rounded,
                                                    CoverArtStyle.CIRCLE to R.string.circle,
                                                    CoverArtStyle.SQUARE to R.string.square
                                                ).forEach { (style, nameRes) ->
                                                    FilterChip(
                                                        selected = customization.coverArtStyle == style,
                                                        onClick = { customization = customization.copy(coverArtStyle = style) },
                                                        label = { Text(stringResource(nameRes), fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            Text(stringResource(R.string.lyrics), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    LyricsStyle.NORMAL to R.string.default_,
                                                    LyricsStyle.ITALIC to R.string.font_style_italic,
                                                    LyricsStyle.CONDENSED to R.string.font_style_condensed
                                                ).forEach { (style, nameRes) ->
                                                    FilterChip(
                                                        selected = customization.lyricsStyle == style,
                                                        onClick = { customization = customization.copy(lyricsStyle = style) },
                                                        label = { Text(stringResource(nameRes), fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveToggleRow(
                                                title = stringResource(R.string.accent_line),
                                                checked = customization.showAccentLine,
                                                onCheckedChange = {
                                                    customization = customization.copy(
                                                        showAccentLine = it,
                                                        accentColor = if (it && customization.accentColor == null) customization.textColor else customization.accentColor
                                                    )
                                                }
                                            )
                                        }
                                    }

                                    CustomizerSection.CANVAS -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text(stringResource(R.string.player_background_style), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    BackgroundStyle.SOLID to R.string.solid,
                                                    BackgroundStyle.GRADIENT to R.string.gradient,
                                                    BackgroundStyle.PATTERN to R.string.pattern
                                                ).forEach { (style, nameRes) ->
                                                    FilterChip(
                                                        selected = customization.backgroundStyle == style,
                                                        onClick = { customization = customization.copy(backgroundStyle = style) },
                                                        label = { Text(stringResource(nameRes), fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveSliderRow(
                                                label = stringResource(R.string.corner_radius),
                                                value = customization.cornerRadius,
                                                range = 0f..40f,
                                                onValueChange = { customization = customization.copy(cornerRadius = it) },
                                                format = "${customization.cornerRadius.toInt()}dp"
                                            )

                                            ExpressiveToggleRow(
                                                title = stringResource(R.string.border),
                                                checked = customization.borderEnabled,
                                                onCheckedChange = { customization = customization.copy(borderEnabled = it) }
                                            )
                                        }
                                    }

                                    CustomizerSection.TYPOGRAPHY -> {
                                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                            Text(stringResource(R.string.alignment), style = MaterialTheme.typography.labelMedium)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                listOf(
                                                    TextAlignment.LEFT to R.string.left,
                                                    TextAlignment.CENTER to R.string.center,
                                                    TextAlignment.RIGHT to R.string.right
                                                ).forEach { (align, nameRes) ->
                                                    FilterChip(
                                                        selected = customization.textAlignment == align,
                                                        onClick = { customization = customization.copy(textAlignment = align) },
                                                        label = { Text(stringResource(nameRes), fontSize = 12.sp) },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            ExpressiveToggleRow(
                                                title = stringResource(R.string.text_shadow),
                                                checked = customization.textShadowEnabled,
                                                onCheckedChange = { customization = customization.copy(textShadowEnabled = it) }
                                            )

                                            ExpressiveToggleRow(
                                                title = stringResource(R.string.app_logo),
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

            // Bottom Floating Action Buttons
            if (shareMode == ShareMode.IMAGE) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(0.7f).height(50.dp)
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
                            modifier = Modifier.weight(1.3f).height(50.dp)
                        ) {
                            if (isGenerating) {
                                CircularWavyProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.generating_image))
                            } else {
                                Icon(painterResource(R.drawable.media3_icon_share), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.share_lyrics_image), fontWeight = FontWeight.Bold)
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
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swatchScale"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
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
                tint = if (color.isDark()) Color.White else Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }
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
