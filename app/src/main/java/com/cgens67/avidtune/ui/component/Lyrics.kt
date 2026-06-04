package com.cgens67.avidtune.ui.component

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.cgens67.avidtune.LocalDatabase
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.R
import com.cgens67.avidtune.constants.AnimateLyricsKey
import com.cgens67.avidtune.constants.AppleMusicLyricsBlurKey
import com.cgens67.avidtune.constants.DarkModeKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.constants.LyricsClickKey
import com.cgens67.avidtune.constants.LyricsScrollKey
import com.cgens67.avidtune.constants.LyricsTextPositionKey
import com.cgens67.avidtune.constants.PlayerBackgroundStyle
import com.cgens67.avidtune.constants.PlayerBackgroundStyleKey
import com.cgens67.avidtune.constants.SliderStyle
import com.cgens67.avidtune.constants.SliderStyleKey
import com.cgens67.avidtune.constants.SwipeThumbnailKey
import com.cgens67.avidtune.db.entities.LyricsEntity
import com.cgens67.avidtune.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.cgens67.avidtune.lyrics.LyricsEntry
import com.cgens67.avidtune.lyrics.LyricsResult
import com.cgens67.avidtune.lyrics.LyricsUtils.findCurrentLineIndex
import com.cgens67.avidtune.lyrics.LyricsUtils.parseLyrics
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.menu.LyricsMenu
import com.cgens67.avidtune.ui.screens.settings.DarkMode
import com.cgens67.avidtune.ui.screens.settings.LyricsPosition
import com.cgens67.avidtune.ui.utils.fadingEdge
import com.cgens67.avidtune.utils.ComposeToImage
import com.cgens67.avidtune.utils.makeTimeString
import com.cgens67.avidtune.utils.rememberEnumPreference
import com.cgens67.avidtune.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.saket.squiggles.SquigglySlider
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.core.graphics.drawable.toBitmap
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.Typeface
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.Paint

@RequiresApi(Build.VERSION_CODES.M)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@SuppressLint("UnusedBoxWithConstraintsScope", "StringFormatInvalid",
    "LocalContextGetResourceValueCall"
)
@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    onNavigateBack: (() -> Unit)? = null,
    mediaMetadata: com.cgens67.avidtune.models.MediaMetadata? = null,
    onBackClick: () -> Unit = {},
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    backgroundAlpha: () -> Float = { 1f }
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = com.cgens67.avidtune.ui.component.LocalMenuState.current
    val density = LocalDensity.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    val database = LocalDatabase.current

    val isFullscreen = onNavigateBack != null
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT)
    val landscapeOffset = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val lyricsTextPosition by rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.CENTER)
    val changeLyrics by rememberPreference(LyricsClickKey, true)
    val scrollLyrics by rememberPreference(LyricsScrollKey, true)
    val animateLyrics by rememberPreference(AnimateLyricsKey, true)
    val disableBlur by rememberPreference(DisableBlurKey, false)
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)

    val currentMetadata = mediaMetadata ?: playerConnection.mediaMetadata.collectAsState().value
    val currentSongId = currentMetadata?.id

    var currentLineIndex by remember { mutableIntStateOf(-1) }
    var currentMainLineIndex by remember { mutableIntStateOf(-1) }
    var deferredCurrentMainLineIndex by remember(currentSongId) { mutableIntStateOf(0) }
    var previousMainLineIndex by remember(currentSongId) { mutableIntStateOf(0) }

    var lastPreviewTime by remember(currentSongId) { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var initialScrollDone by remember(currentSongId) { mutableStateOf(false) }
    var shouldScrollToFirstLine by remember(currentSongId) { mutableStateOf(true) }
    var isAppMinimized by rememberSaveable { mutableStateOf(false) }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }
    var cornerRadius by remember { mutableFloatStateOf(16f) }

    var isAutoScrollEnabled by rememberSaveable { mutableStateOf(true) }

    var isSelectionModeActive by remember(currentSongId) { mutableStateOf(false) }
    val selectedIndices = remember(currentSongId) { mutableStateListOf<Int>() }
    var showMaxSelectionToast by remember { mutableStateOf(false) }

    var showProgressDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var shareDialogData by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val lazyListState = rememberLazyListState()
    var isAnimating by remember { mutableStateOf(false) }
    val maxSelectionLimit = 5

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    var lyricsCache by remember { mutableStateOf<Map<String, LyricsEntity>>(emptyMap()) }
    var currentLyricsEntity by remember(currentSongId) {
        mutableStateOf<LyricsEntity?>(lyricsCache[currentSongId])
    }
    var isLoadingLyrics by remember(currentSongId) { mutableStateOf(false) }

    val rawLyricsEntity by playerConnection.currentLyrics.collectAsState(initial = null)
    
    val activeLyricsEntity = rawLyricsEntity ?: currentLyricsEntity
    
    val originalLyrics = remember(activeLyricsEntity) { 
        var text = activeLyricsEntity?.lyrics?.trim()
        if (text != null && text.startsWith("[provider:")) {
            text = text.substringAfter('\n').trim()
        }
        text
    }

    val lyricsOffsetMs = remember(activeLyricsEntity) {
        val raw = activeLyricsEntity?.lyrics.orEmpty()
        Regex("\\[offset:(-?\\d+)\\]").find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }
    
    val lyricsProviderName = remember(activeLyricsEntity) {
        val text = activeLyricsEntity?.lyrics?.trim()
        if (text != null && text.startsWith("[provider:")) {
            text.substringBefore('\n').trim().removePrefix("[provider:").removeSuffix("]")
        } else {
            null
        }
    }

    val lines = remember(originalLyrics, scope) {
        if (originalLyrics == null || originalLyrics == LYRICS_NOT_FOUND) {
            emptyList()
        } else if (originalLyrics.startsWith("[")) {
            val parsedLines = parseLyrics(originalLyrics)
            listOf(LyricsEntry.HEAD_LYRICS_ENTRY) + parsedLines
        } else {
            originalLyrics.lines().mapIndexed { index, line ->
                LyricsEntry(index * 100L, line)
            }
        }
    }

    var translatedLines by remember(currentSongId) { mutableStateOf<List<LyricsEntry>?>(null) }
    var showTranslated by remember(currentSongId) { mutableStateOf(false) }
    var showTranslatePrompt by remember(currentSongId) { mutableStateOf(false) }
    var translationPromptText by remember(currentSongId) { mutableStateOf("") }
    var isTranslating by remember(currentSongId) { mutableStateOf(false) }
    
    var showRomanized by remember(currentSongId) { mutableStateOf(false) }
    var romanizedLines by remember(currentSongId) { mutableStateOf<List<String>?>(null) }

    val displayedLines = if (showTranslated && translatedLines != null) translatedLines!! else lines

    val toggleTranslation = {
        if (showTranslated) {
            showTranslated = false
        } else if (translatedLines != null) {
            showTranslated = true
            showTranslatePrompt = false
        } else {
            scope.launch {
                isTranslating = true
                showTranslatePrompt = false
                
                val isZhConversion = translationPromptText == context.getString(R.string.translate_zh_tw_prompt)
                
                val itemsToTranslate = mutableListOf<String>()
                lines.forEach { entry ->
                    if (entry.text.isNotBlank()) {
                        itemsToTranslate.add(entry.text)
                        if (isZhConversion && entry.words != null) {
                            entry.words.forEach { word ->
                                itemsToTranslate.add(word.text)
                            }
                        }
                    }
                }
                
                val textToTranslate = itemsToTranslate.joinToString("\n")
                val translatedText = com.cgens67.avidtune.utils.TranslationHelper.translate(textToTranslate)
                
                if (translatedText != null) {
                    val translatedSplit = translatedText.split("\n")
                    val newEntries = lines.toMutableList()
                    
                    var transIdx = 0
                    for (i in newEntries.indices) {
                        val entry = newEntries[i]
                        if (entry.text.isNotBlank()) {
                            val newLineText = translatedSplit.getOrElse(transIdx++) { entry.text }
                            
                            val newWords = if (isZhConversion && entry.words != null) {
                                entry.words.map { word ->
                                    val newWordText = translatedSplit.getOrElse(transIdx++) { word.text }
                                    word.copy(text = newWordText)
                                }
                            } else {
                                null
                            }
                            
                            newEntries[i] = entry.copy(text = newLineText, words = newWords)
                        } else {
                            newEntries[i] = entry.copy(words = null)
                        }
                    }
                    
                    translatedLines = newEntries
                    showTranslated = true
                } else {
                    Toast.makeText(context, R.string.translation_failed, Toast.LENGTH_SHORT).show()
                }
                
                isTranslating = false
            }
        }
    }

    val toggleRomanization = {
        if (showRomanized) {
            showRomanized = false
        } else if (romanizedLines != null) {
            showRomanized = true
        } else {
            scope.launch {
                val linesToRomanize = lines.map { it.text }
                val romanizedResult = com.cgens67.avidtune.utils.TranslationHelper.romanize(linesToRomanize)

                // romanizedResult is a List<String> aligned with 'lines'
                val finalRomanizedLines = mutableListOf<String>()
                for (i in lines.indices) {
                    if (lines[i].text.isNotBlank()) {
                        finalRomanizedLines.add(romanizedResult.getOrElse(i) { "" })
                    } else {
                        finalRomanizedLines.add("")
                    }
                }
                romanizedLines = finalRomanizedLines
                showRomanized = true
            }
        }
    }

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by
    animateFloatAsState(
        targetValue = progress,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessVeryLow,
                visibilityThreshold = 1 / 1000f,
            ),
    )

    var position by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by rememberSaveable(playbackState) { mutableLongStateOf(playerConnection.player.duration) }

    val expressiveAccent = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.tertiary
    }
    val textColor = expressiveAccent

    val textBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.APPLE_MUSIC -> Color.White
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()

    LaunchedEffect(currentMetadata?.id, playerBackground) {
        if ((playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.APPLE_MUSIC) && currentMetadata?.thumbnailUrl != null) {
            val cachedColors = gradientColorsCache[currentMetadata.id]
            if (cachedColors != null) {
                gradientColors = cachedColors
                return@LaunchedEffect
            }

            withContext(Dispatchers.IO) {
                try {

                    val fallbackColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                    gradientColorsCache[currentMetadata.id] = fallbackColors
                    withContext(Dispatchers.Main) { gradientColors = fallbackColors }
                } catch (e: Exception) {
                    val fallbackColors = listOf(primaryColor, secondaryColor, tertiaryColor)
                    gradientColorsCache[currentMetadata.id] = fallbackColors
                    withContext(Dispatchers.Main) { gradientColors = fallbackColors }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    LaunchedEffect(currentSongId) {
        currentSongId?.let { songId ->
            if (lyricsCache.containsKey(songId)) {
                val cached = lyricsCache[songId]
                currentLyricsEntity = cached
                return@LaunchedEffect
            }

            isLoadingLyrics = true

            withContext(Dispatchers.IO) {
                try {
                    val existingLyrics = try {
                        database.getLyrics(songId)
                    } catch (e: Throwable) {
                        null
                    }

                    if (existingLyrics != null && existingLyrics.lyrics != LYRICS_NOT_FOUND) {
                        val newCache = lyricsCache.toMutableMap().apply {
                            put(songId, existingLyrics)
                        }
                        lyricsCache = newCache
                        currentLyricsEntity = existingLyrics

                        val text = existingLyrics.lyrics.trim()
                        if (!text.startsWith("[provider:")) {
                            // Old format, trigger a background upgrade
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val entryPoint = EntryPointAccessors.fromApplication(
                                        context.applicationContext,
                                        com.cgens67.avidtune.di.LyricsHelperEntryPoint::class.java
                                    )
                                    val lyricsHelper = entryPoint.lyricsHelper()
                                    val fetchedResult: LyricsResult? = currentMetadata?.let { lyricsHelper.getLyrics(it) }
                                    
                                    val fetchedLyrics = fetchedResult?.lyrics
                                    val pName = fetchedResult?.providerName

                                    if (!fetchedLyrics.isNullOrBlank() && fetchedLyrics != LYRICS_NOT_FOUND) {
                                        val textToSave = if (pName != null) "[provider:${pName}]\n$fetchedLyrics" else fetchedLyrics
                                        val upgradedEntity = LyricsEntity(songId, textToSave)
                                        try {
                                            database.query {
                                                upsert(upgradedEntity)
                                            }
                                        } catch (e: Throwable) {}

                                        // Update state to show provider credit
                                        currentLyricsEntity = upgradedEntity
                                        val upgradedCache = lyricsCache.toMutableMap().apply {
                                            put(songId, upgradedEntity)
                                        }
                                        lyricsCache = upgradedCache
                                    }
                                } catch (e: Throwable) {
                                    // Ignore errors during background upgrade
                                }
                            }
                        }
                    } else {
                        try {
                            val entryPoint = EntryPointAccessors.fromApplication(
                                context.applicationContext,
                                com.cgens67.avidtune.di.LyricsHelperEntryPoint::class.java
                            )
                            val lyricsHelper = entryPoint.lyricsHelper()
                            val fetchedResult: LyricsResult? = currentMetadata?.let { lyricsHelper.getLyrics(it) }
                            
                            val fetchedLyrics = fetchedResult?.lyrics
                            val pName = fetchedResult?.providerName

                            val entity = if (!fetchedLyrics.isNullOrBlank() && fetchedLyrics != LYRICS_NOT_FOUND) {
                                val textToSave = if (pName != null) "[provider:${pName}]\n$fetchedLyrics" else fetchedLyrics
                                LyricsEntity(songId, textToSave)
                            } else {
                                LyricsEntity(songId, LYRICS_NOT_FOUND)
                            }

                            try {
                                database.query {
                                    upsert(entity)
                                }
                            } catch (e: Throwable) {}

                            val newCache = lyricsCache.toMutableMap().apply {
                                put(songId, entity)
                            }
                            lyricsCache = newCache
                            currentLyricsEntity = entity
                        } catch (e: Throwable) {
                            val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                            val newCache = lyricsCache.toMutableMap().apply {
                                put(songId, errorEntity)
                            }
                            lyricsCache = newCache
                            currentLyricsEntity = errorEntity
                        }
                    }
                } catch (e: Exception) {
                    val errorEntity = LyricsEntity(songId, LYRICS_NOT_FOUND)
                    val newCache = lyricsCache.toMutableMap().apply {
                        put(songId, errorEntity)
                    }
                    lyricsCache = newCache
                    currentLyricsEntity = errorEntity
                } finally {
                    isLoadingLyrics = false
                }
            }
        }
    }

    LaunchedEffect(lines) {
        isSelectionModeActive = false
        selectedIndices.clear()
        currentLineIndex = -1
        currentMainLineIndex = -1
        deferredCurrentMainLineIndex = 0
        previousMainLineIndex = 0
        initialScrollDone = false
        shouldScrollToFirstLine = true
        isAutoScrollEnabled = true

        if (lines.isNotEmpty() && originalLyrics != LYRICS_NOT_FOUND) {
            val textOnly = lines.mapNotNull { it.text }.filter { it.isNotBlank() }.joinToString("\n").take(500)
            if (textOnly.isNotBlank()) {
                val detectedLang = com.cgens67.avidtune.utils.TranslationHelper.detectLanguage(textOnly)
                val systemLocale = java.util.Locale.getDefault()
                val systemLang = systemLocale.language.lowercase()
                val langTag = systemLocale.toLanguageTag().lowercase()
                
                val isSystemZhCn = langTag.contains("zh-cn") || langTag.contains("zh-hans") || (langTag == "zh")
                
                if (detectedLang != null && detectedLang != "und") {
                    val detectedLower = detectedLang.lowercase()
                    if (isSystemZhCn && (detectedLower == "zh-tw" || detectedLower == "zh-hant")) {
                        translationPromptText = context.getString(R.string.translate_zh_tw_prompt)
                        showTranslatePrompt = true
                    } else if (!detectedLower.startsWith(systemLang)) {
                        translationPromptText = context.getString(R.string.translate_lyrics_prompt)
                        showTranslatePrompt = true
                    }
                }
            }
        }
    }

    val isSynced = remember(originalLyrics) {
        !originalLyrics.isNullOrEmpty() && originalLyrics.startsWith("[")
    }

    BackHandler(enabled = isSelectionModeActive || isFullscreen) {
        when {
            isSelectionModeActive -> {
                isSelectionModeActive = false
                selectedIndices.clear()
            }
            isFullscreen -> onNavigateBack?.invoke()
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    isAutoScrollEnabled = false
                }
                if (!isSelectionModeActive) {
                    lastPreviewTime = System.currentTimeMillis()
                }
                return super.onPostScroll(consumed, available, source)
            }

            override suspend fun onPostFling(
                consumed: androidx.compose.ui.unit.Velocity,
                available: androidx.compose.ui.unit.Velocity
            ): androidx.compose.ui.unit.Velocity {
                isAutoScrollEnabled = false
                if (!isSelectionModeActive) {
                    lastPreviewTime = System.currentTimeMillis()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isFullscreen) {
            cornerRadius = 16f
        }
    }

    LaunchedEffect(playbackState) {
        if (isFullscreen && playbackState == Player.STATE_READY) {
            while (isActive) {
                delay(100)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    LaunchedEffect(showMaxSelectionToast) {
        if (showMaxSelectionToast) {
            Toast.makeText(
                context,
                context.getString(R.string.max_selection_limit, maxSelectionLimit),
                Toast.LENGTH_SHORT
            ).show()
            showMaxSelectionToast = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val visibleItemsInfo = lazyListState.layoutInfo.visibleItemsInfo
                val isCurrentLineVisible = visibleItemsInfo.any { it.index == currentMainLineIndex }
                if (isCurrentLineVisible) {
                    initialScrollDone = false
                }
                isAppMinimized = true
            } else if (event == Lifecycle.Event.ON_START) {
                isAppMinimized = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(originalLyrics, lyricsOffsetMs) {
        if (originalLyrics.isNullOrEmpty() || !originalLyrics.startsWith("[")) {
            currentLineIndex = -1
            currentMainLineIndex = -1
            return@LaunchedEffect
        }
        while (isActive) {
            delay(50)
            val sliderPos = sliderPositionProvider()
            isSeeking = sliderPos != null
            val rawIndex = findCurrentLineIndex(
                lines,
                (sliderPos ?: playerConnection.player.currentPosition) + lyricsOffsetMs
            )
            currentLineIndex = rawIndex

            var mainIdx = rawIndex
            // Navigate back to find the closest main line (ignoring background lines)
            while (mainIdx >= 0 && lines.getOrNull(mainIdx)?.isBackground == true) {
                mainIdx--
            }
            currentMainLineIndex = mainIdx
        }
    }

    LaunchedEffect(isSeeking, lastPreviewTime) {
        if (isSeeking) {
            lastPreviewTime = 0L
        } else if (lastPreviewTime != 0L) {
            delay(if (isFullscreen) 2.seconds else LyricsPreviewTime)
            lastPreviewTime = 0L
        }
    }

    suspend fun performSmoothPageScroll(targetIndex: Int, duration: Int = 1500) {
        if (isAnimating) return
        isAnimating = true
        try {
            val itemInfo = lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == targetIndex }
            if (itemInfo != null) {
                val viewportHeight = lazyListState.layoutInfo.viewportEndOffset -
                        lazyListState.layoutInfo.viewportStartOffset
                val center = lazyListState.layoutInfo.viewportStartOffset + (viewportHeight / 2)
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val offset = itemCenter - center
                if (kotlin.math.abs(offset) > 10) {
                    if (animateLyrics) {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = duration)
                        )
                    } else {
                        lazyListState.animateScrollBy(
                            value = offset.toFloat(),
                            animationSpec = tween(durationMillis = 1)
                        )
                    }
                }
            } else {
                lazyListState.scrollToItem(targetIndex)
            }
        } finally {
            isAnimating = false
        }
    }

    LaunchedEffect(currentMainLineIndex, lastPreviewTime, initialScrollDone, isAutoScrollEnabled) {
        if (!isSynced) return@LaunchedEffect

        if (currentMainLineIndex != -1) {
            deferredCurrentMainLineIndex = currentMainLineIndex
        }

        if (isAutoScrollEnabled) {
            if ((currentMainLineIndex == 0 && shouldScrollToFirstLine) || !initialScrollDone) {
                shouldScrollToFirstLine = false
                val initialCenterIndex = kotlin.math.max(0, currentMainLineIndex)
                performSmoothPageScroll(initialCenterIndex, 800)
                if (!isAppMinimized) {
                    initialScrollDone = true
                }
            } else if (currentMainLineIndex != -1) {
                if (isSeeking) {
                    val seekCenterIndex = kotlin.math.max(0, currentMainLineIndex - 1)
                    performSmoothPageScroll(seekCenterIndex, 500)
                } else if ((lastPreviewTime == 0L || currentMainLineIndex != previousMainLineIndex) && scrollLyrics) {
                    if (currentMainLineIndex != previousMainLineIndex) {
                        performSmoothPageScroll(currentMainLineIndex, 1500)
                    }
                }
            }
        }
        if (currentMainLineIndex > 0) {
            shouldScrollToFirstLine = true
        }
        previousMainLineIndex = currentMainLineIndex
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isFullscreen) MaterialTheme.colorScheme.background else Color.Transparent)
    ) {
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backgroundAlpha() }
            ) {
                when (playerBackground) {
                    PlayerBackgroundStyle.BLUR -> {
                        currentMetadata?.let { metadata ->
                            AsyncImage(
                                model = metadata.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (!disableBlur) it.blur(if (useDarkTheme) 150.dp else 100.dp) else it }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                        }
                    }
                    PlayerBackgroundStyle.GRADIENT -> {
                        if (gradientColors.isNotEmpty()) {
                            val gradientColorStops = if (gradientColors.size >= 3) {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.5f to gradientColors[1],
                                    1.0f to gradientColors[2]
                                )
                            } else {
                                arrayOf(
                                    0.0f to gradientColors[0],
                                    0.6f to gradientColors[0].copy(alpha = 0.7f),
                                    1.0f to Color.Black
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(colorStops = gradientColorStops))
                                    .background(Color.Black.copy(alpha = 0.2f))
                            )
                        }
                    }
                    PlayerBackgroundStyle.APPLE_MUSIC -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (gradientColors.isNotEmpty()) {
                                val color1 = gradientColors[0]
                                val color2 = gradientColors.getOrElse(1) { gradientColors[0].copy(alpha = 0.8f) }
                                val color3 = gradientColors.getOrElse(2) { gradientColors[0].copy(alpha = 0.6f) }

                                Canvas(modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (!disableBlur) it.blur(100.dp) else it }
                                ) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            listOf(color1, color2, color3)
                                        )
                                    )

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(color1, Color.Transparent),
                                            center = Offset(size.width * 0.2f, size.height * 0.2f),
                                            radius = size.width * 0.8f
                                        ),
                                        center = Offset(size.width * 0.2f, size.height * 0.2f),
                                        radius = size.width * 0.8f
                                    )

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(color2, Color.Transparent),
                                            center = Offset(size.width * 0.8f, size.height * 0.5f),
                                            radius = size.width * 0.7f
                                        ),
                                        center = Offset(size.width * 0.8f, size.height * 0.5f),
                                        radius = size.width * 0.7f
                                    )

                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(color3, Color.Transparent),
                                            center = Offset(size.width * 0.3f, size.height * 0.8f),
                                            radius = size.width * 0.9f
                                        ),
                                        center = Offset(size.width * 0.3f, size.height * 0.8f),
                                        radius = size.width * 0.9f
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                )
                            }
                        }
                    }
                    PlayerBackgroundStyle.DEFAULT -> {
                    }
                }

                if (playerBackground != PlayerBackgroundStyle.DEFAULT) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                BoxWithConstraints(
                    contentAlignment = Alignment.TopStart,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    val topPadding = with(LocalDensity.current) {
                        100.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    }

                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = topPadding,
                            bottom = if (isFullscreen) 180.dp else 0.dp,
                            start = 8.dp,
                            end = 8.dp
                        ),
                        modifier = Modifier
                            .fadingEdge(vertical = 32.dp)
                            .nestedScroll(nestedScrollConnection)
                    ) {
                        val displayedCurrentMainLineIndex =
                            if (isSeeking || isSelectionModeActive)
                                deferredCurrentMainLineIndex
                            else
                                currentMainLineIndex

                        if (isLoadingLyrics) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp),
                                    contentAlignment = when (lyricsTextPosition) {
                                        LyricsPosition.LEFT -> Alignment.CenterStart
                                        LyricsPosition.CENTER -> Alignment.Center
                                        LyricsPosition.RIGHT -> Alignment.CenterEnd
                                    }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        androidx.compose.material3.LoadingIndicator(
                                            color = expressiveAccent
                                        )
                                        Text(
                                            text = stringResource(R.string.loading_lyrics),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = expressiveAccent.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        else {
                            if (showTranslatePrompt && !isTranslating && !showTranslated) {
                                item(key = "translate_prompt") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.translate),
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = translationPromptText,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(
                                                        onClick = { showTranslatePrompt = false },
                                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                                    ) {
                                                        Text(
                                                            stringResource(R.string.dismiss),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                    Button(
                                                        onClick = { toggleTranslation() },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 16.dp)
                                                    ) {
                                                        Text(stringResource(R.string.Translate))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (isTranslating) {
                                item(key = "translating_indicator") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.material3.LoadingIndicator(
                                            color = expressiveAccent
                                        )
                                    }
                                }
                            }

                            itemsIndexed(
                                items = displayedLines,
                                key = { index, item -> "$index-${item.time}" }
                            ) { index, item ->
                                val isSelected = selectedIndices.contains(index)
                                
                                val isAssociatedBg = item.isBackground &&
                                        displayedCurrentMainLineIndex >= 0 &&
                                        index > displayedCurrentMainLineIndex &&
                                        displayedLines.subList(displayedCurrentMainLineIndex + 1, index + 1).all { it.isBackground }

                                val isActiveLine = (index == displayedCurrentMainLineIndex || isAssociatedBg) && isSynced
                                val distance = if (isActiveLine) 0 else kotlin.math.abs(index - displayedCurrentMainLineIndex)

                                val romText = if (showRomanized) romanizedLines?.getOrNull(index) else null

                                LyricsLine(
                                    entry = item,
                                    romanizedText = romText,
                                    isSynced = isSynced,
                                    isActive = isActiveLine,
                                    distanceFromCurrent = distance,
                                    lyricsTextPosition = lyricsTextPosition,
                                    textColor = expressiveAccent,
                                    textSize = 25f,
                                    lineSpacing = 4f,
                                    onClick = {
                                        if (isSelectionModeActive) {
                                            if (isSelected) {
                                                selectedIndices.remove(index)
                                                if (selectedIndices.isEmpty()) {
                                                    isSelectionModeActive = false
                                                }
                                            } else {
                                                if (selectedIndices.size < maxSelectionLimit) {
                                                    selectedIndices.add(index)
                                                } else {
                                                    showMaxSelectionToast = true
                                                }
                                            }
                                        } else if (isSynced && changeLyrics) {
                                            playerConnection.player.seekTo(item.time)
                                            scope.launch {
                                                performSmoothPageScroll(index, 1500)
                                            }
                                            lastPreviewTime = 0L
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionModeActive) {
                                            isSelectionModeActive = true
                                            selectedIndices.add(index)
                                        } else if (!isSelected && selectedIndices.size < maxSelectionLimit) {
                                            selectedIndices.add(index)
                                        } else if (!isSelected) {
                                            showMaxSelectionToast = true
                                        }
                                    },
                                    isSelected = isSelected,
                                    isSelectionModeActive = isSelectionModeActive,
                                    isAutoScrollActive = isAutoScrollEnabled,
                                    animateLyrics = animateLyrics,
                                    lyricsOffset = lyricsOffsetMs,
                                    modifier = Modifier
                                )

                                val nextItem = displayedLines.getOrNull(index + 1)
                                if (isSynced && nextItem != null && !item.isBackground && !nextItem.isBackground) {
                                    val itemEnd = item.words?.maxOfOrNull { (it.endTime * 1000).toLong() } ?: (item.time + 3000L)
                                    val gapDuration = nextItem.time - itemEnd

                                    if (gapDuration > 15000L) {
                                        GapIndicator(
                                            gapStart = itemEnd,
                                            gapEnd = nextItem.time,
                                            playerConnection = playerConnection,
                                            lyricsOffset = lyricsOffsetMs,
                                            color = expressiveAccent
                                        )
                                    }
                                }
                            }
                            
                            if (originalLyrics != LYRICS_NOT_FOUND && !lyricsProviderName.isNullOrBlank()) {
                                item(key = "provider_credit") {
                                    Text(
                                        text = stringResource(R.string.lyrics_provided_by, lyricsProviderName ?: ""),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 24.dp, bottom = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (originalLyrics == LYRICS_NOT_FOUND) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.8f)
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.music_note),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Text(
                                    text = stringResource(R.string.lyrics_not_found),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Las letras no están disponibles para esta canción",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {

                val coroutineScope = rememberCoroutineScope()
                val offsetXAnimatable = remember { Animatable(0f) }
                var dragStartTime by remember { mutableLongStateOf(0L) }
                var totalDragDistance by remember { mutableFloatStateOf(0f) }
                val layoutDirection = LocalLayoutDirection.current

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(swipeThumbnail) {
                                if (!swipeThumbnail) return@pointerInput
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        dragStartTime = System.currentTimeMillis()
                                        totalDragDistance = 0f
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetXAnimatable.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        val adjustedDragAmount = if (layoutDirection == LayoutDirection.Rtl)
                                            -dragAmount else dragAmount
                                        val allowLeft = adjustedDragAmount < 0 && canSkipNext
                                        val allowRight = adjustedDragAmount > 0 && canSkipPrevious

                                        if (allowLeft || allowRight) {
                                            totalDragDistance += adjustedDragAmount.absoluteValue
                                            coroutineScope.launch {
                                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val dragDuration = System.currentTimeMillis() - dragStartTime
                                        val velocity = if (dragDuration > 0)
                                            totalDragDistance / dragDuration else 0f
                                        val currentOffset = offsetXAnimatable.value

                                        val minDistanceThreshold = 50f
                                        val velocityThreshold = (0.73f * -8.25f) + 8.5f
                                        val autoSwipeThreshold = (600 / (1f + exp(-(-11.44748 * 0.73f + 9.04945)))).roundToInt()

                                        val shouldChangeSong = (
                                                currentOffset.absoluteValue > minDistanceThreshold &&
                                                        velocity > velocityThreshold
                                                ) || (currentOffset.absoluteValue > autoSwipeThreshold)

                                        if (shouldChangeSong) {
                                            val isRightSwipe = currentOffset > 0
                                            if (isRightSwipe && canSkipPrevious) {
                                                playerConnection.seekToPrevious()
                                            } else if (!isRightSwipe && canSkipNext) {
                                                playerConnection.seekToNext()
                                            }
                                        }

                                        coroutineScope.launch {
                                            offsetXAnimatable.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                                .fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            if (isPlaying) playerConnection.player.pause() else playerConnection.player.play()
                                        }
                                    }
                            ) {
                                currentMetadata?.let { metadata ->
                                    AsyncImage(
                                        model = metadata.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                val overlayAlphaState by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (isPlaying) 0.4f else 0.4f,
                                    label = "overlay_alpha",
                                    animationSpec = if (animateLyrics) spring() else snap()
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = overlayAlphaState))
                                )

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = playbackState == androidx.media3.common.Player.STATE_ENDED || !isPlaying || isPlaying,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                                                R.drawable.replay
                                            } else if (isPlaying) {
                                                R.drawable.pause
                                            } else {
                                                R.drawable.play
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                currentMetadata?.let { metadata ->
                                    Text(
                                        text = metadata.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = textBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (metadata.artists.isNotEmpty()) {
                                            metadata.artists.joinToString(", ") { it.name }
                                        } else {
                                            stringResource(R.string.unknown)
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp
                                        ),
                                        color = textBackgroundColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (offsetXAnimatable.value.absoluteValue > 20f) {
                            if (offsetXAnimatable.value > 0 && canSkipPrevious) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp)
                                        .alpha((offsetXAnimatable.value.absoluteValue / 100f).coerceIn(0.2f, 0.6f))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_previous),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (offsetXAnimatable.value < 0 && canSkipNext) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                        .alpha((offsetXAnimatable.value.absoluteValue / 100f).coerceIn(0.2f, 0.6f))
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.skip_next),
                                        contentDescription = null,
                                        tint = textBackgroundColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    playerConnection.toggleLike()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (currentSong?.song?.liked == true)
                                        R.drawable.favorite
                                    else R.drawable.favorite_border
                                ),
                                contentDescription = if (currentSong?.song?.liked == true) stringResource(R.string.remove_from_library) else stringResource(R.string.add_to_library),
                                tint = if (currentSong?.song?.liked == true)
                                    MaterialTheme.colorScheme.error
                                else
                                    textBackgroundColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clickable {
                                    currentMetadata?.let { metadata ->
                                        menuState.show {
                                            LyricsMenu(
                                                lyricsEntity = activeLyricsEntity,
                                                mediaMetadata = metadata,
                                                onDismiss = menuState::dismiss,
                                                isTranslated = showTranslated,
                                                onTranslateClick = { toggleTranslation() },
                                                isRomanized = showRomanized,
                                                onRomanizeClick = { toggleRomanization() }
                                            )
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_horiz),
                                contentDescription = stringResource(R.string.more_options),
                                tint = textBackgroundColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (sliderStyle) {
                    SliderStyle.DEFAULT -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                                thumbColor = textBackgroundColor
                            ),
                        )
                    }

                    SliderStyle.SQUIGGLY -> {
                        SquigglySlider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f),
                                thumbColor = textBackgroundColor
                            ),
                            squigglesSpec = SquigglySlider.SquigglesSpec(
                                amplitude = if (isPlaying && animateLyrics) (4.dp).coerceAtLeast(2.dp) else 0.dp,
                                strokeWidth = 3.dp,
                                wavelength = 36.dp,
                            ),
                        )
                    }

                    SliderStyle.SLIM -> {
                        Slider(
                            value = (sliderPosition ?: position).toFloat(),
                            valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                            onValueChange = { sliderPosition = it.toLong() },
                            onValueChangeFinished = {
                                sliderPosition?.let {
                                    playerConnection.player.seekTo(it)
                                    position = it
                                }
                                sliderPosition = null
                            },
                            thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                activeTrackColor = textBackgroundColor,
                                inactiveTrackColor = textBackgroundColor.copy(alpha = 0.3f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = makeTimeString(sliderPosition ?: position),
                        style = MaterialTheme.typography.labelMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Text(
                        text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = textBackgroundColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 220.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            performSmoothPageScroll(currentLineIndex, 1500)
                        }
                        isAutoScrollEnabled = true
                    }
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sync),
                        contentDescription = stringResource(R.string.auto_scroll),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.auto_scroll),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (isFullscreen && isSelectionModeActive) {
            AnimatedVisibility(
                visible = isSelectionModeActive,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 180.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .size(56.dp)
                            .clickable {
                                isSelectionModeActive = false
                                selectedIndices.clear()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.close),
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    if (selectedIndices.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                            tonalElevation = 4.dp,
                            modifier = Modifier
                                .clickable {
                                    val sortedIndices = selectedIndices.sorted()
                                    val selectedLyricsText = sortedIndices
                                        .mapNotNull { displayedLines.getOrNull(it)?.text }
                                        .joinToString("\n")

                                    if (selectedLyricsText.isNotBlank()) {
                                        shareDialogData = Triple(
                                            selectedLyricsText,
                                            currentMetadata?.title ?: "",
                                            currentMetadata?.artists?.joinToString { it.name } ?: ""
                                        )
                                        showShareDialog = true
                                    }
                                    isSelectionModeActive = false
                                    selectedIndices.clear()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.media3_icon_share),
                                    contentDescription = stringResource(R.string.share_selected),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = stringResource(R.string.share),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

    }

    if (showProgressDialog) {
        BasicAlertDialog(onDismissRequest = { /* No permitir cerrar */ }) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = stringResource(R.string.generating_image) + "\n" + stringResource(R.string.please_wait),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showShareDialog && shareDialogData != null) {
        ShareLyricsDialog(
            lyricsText = shareDialogData!!.first,
            songTitle = shareDialogData!!.second,
            artists = shareDialogData!!.third,
            mediaMetadata = currentMetadata,
            onDismiss = {
                showShareDialog = false
                shareDialogData = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GapIndicator(
    gapStart: Long,
    gapEnd: Long,
    playerConnection: PlayerConnection,
    lyricsOffset: Long,
    color: Color
) {
    var smoothPosition by remember { mutableLongStateOf(gapStart) }

    LaunchedEffect(Unit) {
        var lastPlayerPos = playerConnection.player.currentPosition
        var lastUpdateTime = System.currentTimeMillis()
        while (isActive) {
            withFrameMillis {
                val now = System.currentTimeMillis()
                val playerPos = playerConnection.player.currentPosition
                if (playerPos != lastPlayerPos) {
                    lastPlayerPos = playerPos
                    lastUpdateTime = now
                }
                val elapsed = now - lastUpdateTime
                smoothPosition = lastPlayerPos + lyricsOffset + (if (playerConnection.player.isPlaying) elapsed else 0)
            }
        }
    }

    val progress = ((smoothPosition - gapStart).toFloat() / (gapEnd - gapStart)).coerceIn(0f, 1f)
    val isVisible = smoothPosition in gapStart..(gapEnd - 1000L)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(1000)) + expandVertically(tween(1000)),
        exit = fadeOut(tween(1000)) + shrinkVertically(tween(1000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularWavyProgressIndicator(
                progress = { progress },
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareLyricsDialog(
    lyricsText: String,
    songTitle: String,
    artists: String,
    mediaMetadata: com.cgens67.avidtune.models.MediaMetadata?,
    onDismiss: () -> Unit,
    onShareAsImage: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(0.85f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.share_lyrics),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Share as text
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                val songLink =
                                    "https://music.youtube.com/watch?v=${mediaMetadata?.id}"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "\"$lyricsText\"\n\n$songTitle - $artists\n$songLink"
                                )
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    context.getString(R.string.share_lyrics)
                                )
                            )
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.media3_icon_share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.share_as_text),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Share as image
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onShareAsImage(lyricsText, songTitle, artists)
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.media3_icon_share),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.share_as_image),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Cancel button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    )
                }
            }
        }
    }
}


/**
 * Calculates the auto-swipe threshold based on swipe sensitivity.
 */
private fun calculateAutoSwipeThreshold(swipeSensitivity: Float): Int {
    return (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
}

// Preview time constant
val LyricsPreviewTime = 2.seconds
const val ANIMATE_SCROLL_DURATION = 300L