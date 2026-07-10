package com.cgens67.avidtune.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgens67.avidtune.LocalPlayerConnection
import com.cgens67.avidtune.constants.AppleMusicLyricsBlurKey
import com.cgens67.avidtune.constants.DisableBlurKey
import com.cgens67.avidtune.lyrics.LyricsEntry
import com.cgens67.avidtune.lyrics.WordTimestamp
import com.cgens67.avidtune.playback.PlayerConnection
import com.cgens67.avidtune.ui.screens.settings.LyricsPosition
import com.cgens67.avidtune.utils.rememberPreference
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsLine(
    entry: LyricsEntry,
    romanizedText: String? = null,
    isSynced: Boolean,
    isActive: Boolean,
    distanceFromCurrent: Int,
    lyricsTextPosition: LyricsPosition,
    textColor: Color,
    textSize: Float,
    lineSpacing: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isSelectionModeActive: Boolean,
    isAutoScrollActive: Boolean,
    animateLyrics: Boolean = true,
    lyricsOffset: Long = 0L,
    currentSkipSegments: List<Pair<Long, Long>> = emptyList(),
    sponsorBlockEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val playerConnection = LocalPlayerConnection.current ?: return

    val blurRadius by animateFloatAsState(
        targetValue = if (disableBlur || !appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive)
            0f
        else
            6f,
        animationSpec = if (animateLyrics) tween(durationMillis = 600) else snap(),
        label = "blur"
    )

    val animatedScale by animateFloatAsState(
        targetValue = when {
            !isSynced || isActive -> 1.05f
            distanceFromCurrent == 1 -> 1f
            else -> 0.95f
        },
        animationSpec = if (animateLyrics) tween(durationMillis = 400) else snap(),
        label = "scale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = when {
            !isSynced || (isSelectionModeActive && isSelected) -> 1f
            isActive -> 1f
            distanceFromCurrent == 1 -> 0.7f
            distanceFromCurrent == 2 -> 0.4f
            else -> 0.2f
        },
        animationSpec = if (animateLyrics) tween(durationMillis = 400) else snap(),
        label = "alpha"
    )

    val itemModifier = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .combinedClickable(
            enabled = true,
            onClick = onClick,
            onLongClick = onLongClick
        )
        .background(
            if (isSelected && isSelectionModeActive)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else Color.Transparent
        )
        .padding(horizontal = 24.dp, vertical = lineSpacing.dp)
        .graphicsLayer {
            this.alpha = animatedAlpha
            this.scaleX = animatedScale
            this.scaleY = animatedScale
        }
        .then(if (!disableBlur && blurRadius > 0.01f) Modifier.blur(blurRadius.dp) else Modifier)

    val agentAlignment = when {
        entry.agent == "v1" -> Alignment.Start
        entry.agent == "v2" -> Alignment.End
        entry.agent == "v1000" -> Alignment.CenterHorizontally
        entry.isBackground -> Alignment.CenterHorizontally
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    }

    val agentTextAlign = when {
        entry.agent == "v1" -> TextAlign.Left
        entry.agent == "v2" -> TextAlign.Right
        entry.agent == "v1000" -> TextAlign.Center
        entry.isBackground -> TextAlign.Center
        else -> when (lyricsTextPosition) {
            LyricsPosition.LEFT -> TextAlign.Left
            LyricsPosition.CENTER -> TextAlign.Center
            LyricsPosition.RIGHT -> TextAlign.Right
        }
    }

    Column(
        modifier = itemModifier,
        horizontalAlignment = agentAlignment
    ) {
        val inactiveAlpha = if (entry.isBackground) 0.08f else 0.2f
        val activeAlpha = 1f
        val focusedAlpha = if (entry.isBackground) 0.5f else 0.3f
        val targetAlpha = if (!isSynced || entry.isBackground || isActive) {
            activeAlpha
        } else if (isAutoScrollActive) {
            when (distanceFromCurrent) {
                0 -> focusedAlpha
                1 -> 0.2f; 2 -> 0.2f; 3 -> 0.15f; 4 -> 0.1f; else -> 0.08f
            }
        } else inactiveAlpha

        val lineAlpha by animateFloatAsState(targetAlpha, if (animateLyrics) tween(250) else snap(), label = "lyricsLineAlpha")
        val lineColor = textColor.copy(alpha = if (entry.isBackground) focusedAlpha else lineAlpha)

        val mainText = if (entry.isBackground) entry.text.removePrefix("(").removeSuffix(")") else entry.text

        val lyricStyle = TextStyle(
            fontSize = if (entry.isBackground) (textSize * 0.7f).sp else textSize.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = if (entry.isBackground) FontStyle.Italic else FontStyle.Normal,
            lineHeight = if (entry.isBackground) (textSize * 0.7f * 1.3f).sp else (textSize * 1.3f).sp,
            letterSpacing = (-0.5).sp,
            textAlign = agentTextAlign,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )

        val effectiveWords = if (entry.words?.isNotEmpty() == true) {
            entry.words
        } else if (mainText.isNotBlank()) {
            remember(mainText, entry.time) {
                val words = mainText.split(Regex("\\s+")).filter { it.isNotBlank() }
                val wordDurationSec = 0.18
                val wordStaggerSec = 0.03
                val startTimeSec = entry.time / 1000.0
                words.mapIndexed { idx, wordText ->
                    WordTimestamp(
                        text = wordText,
                        startTime = startTimeSec + (idx * wordStaggerSec),
                        endTime = startTimeSec + (idx * wordStaggerSec) + wordDurationSec,
                        hasTrailingSpace = idx < words.size - 1
                    )
                }
            }
        } else null

        val isTracking = isActive || distanceFromCurrent <= 3

        if (isSynced && effectiveWords != null && isTracking && mainText.isNotBlank()) {
            WordLevelLyrics(
                mainText = mainText,
                words = effectiveWords,
                isTracking = isTracking,
                lyricsOffset = lyricsOffset,
                playerConnection = playerConnection,
                lyricStyle = lyricStyle,
                lineColor = lineColor,
                expressiveAccent = textColor,
                isBackground = entry.isBackground,
                focusedAlpha = focusedAlpha,
                alignment = agentTextAlign,
                entryTime = entry.time,
                animateLyrics = animateLyrics,
                currentSkipSegments = currentSkipSegments,
                sponsorBlockEnabled = sponsorBlockEnabled
            )
        } else {
            if (isActive && isSynced) {
                val fillProgress = remember { Animatable(if (animateLyrics) 0f else 1f) }
                val pulseProgress = remember { Animatable(0f) }

                LaunchedEffect(entry.time, animateLyrics) {
                    fillProgress.snapTo(if (animateLyrics) 0f else 1f)
                    if (animateLyrics) {
                        fillProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 1200,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                }

                LaunchedEffect(animateLyrics) {
                    if (animateLyrics) {
                        while (true) {
                            pulseProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(
                                    durationMillis = 3000,
                                    easing = LinearEasing
                                )
                            )
                            pulseProgress.snapTo(0f)
                        }
                    } else {
                        pulseProgress.snapTo(0f)
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> Alignment.CenterStart
                        LyricsPosition.CENTER -> Alignment.Center
                        LyricsPosition.RIGHT -> Alignment.CenterEnd
                    }
                ) {
                    val fill = fillProgress.value
                    val pulse = pulseProgress.value
                    val pulseEffect = if (animateLyrics) (sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f) else 0f
                    val glowIntensity = (fill + pulseEffect).coerceIn(0f, 1.2f)

                    val glowBrush = Brush.horizontalGradient(
                        0.0f to textColor.copy(alpha = 0.3f),
                        (fill * 0.7f).coerceIn(0f, 1f) to textColor.copy(alpha = 0.9f),
                        fill to textColor,
                        (fill + 0.1f).coerceIn(0f, 1f) to textColor.copy(alpha = 0.7f),
                        1.0f to textColor.copy(alpha = if (fill >= 1f) 1f else 0.3f)
                    )

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                style = SpanStyle(
                                    shadow = androidx.compose.ui.graphics.Shadow(
                                        color = textColor.copy(alpha = 0.8f * glowIntensity),
                                        offset = Offset(0f, 0f),
                                        blurRadius = 28f * (1f + pulseEffect)
                                    ),
                                    brush = glowBrush
                                )
                            ) {
                                append(mainText)
                            }
                        },
                        style = lyricStyle,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = mainText,
                    style = lyricStyle.copy(color = lineColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (!romanizedText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = romanizedText,
                style = lyricStyle.copy(
                    fontSize = (textSize * 0.75f).sp,
                    lineHeight = (textSize * 0.75f * 1.3f).sp,
                    fontStyle = FontStyle.Italic
                ),
                color = lineColor.copy(alpha = lineAlpha * 0.8f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WordLevelLyrics(
    mainText: String,
    words: List<WordTimestamp>,
    isTracking: Boolean,
    lyricsOffset: Long,
    playerConnection: PlayerConnection,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    isBackground: Boolean,
    focusedAlpha: Float,
    alignment: TextAlign,
    entryTime: Long,
    animateLyrics: Boolean = true,
    currentSkipSegments: List<Pair<Long, Long>> = emptyList(),
    sponsorBlockEnabled: Boolean = false
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    
    var smoothPosition by remember { mutableLongStateOf(entryTime + lyricsOffset) }
    
    LaunchedEffect(isTracking, currentSkipSegments, sponsorBlockEnabled) {
        if (isTracking) {
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
                    val currentVideoPos = lastPlayerPos + (if (playerConnection.player.isPlaying) elapsed else 0)
                    
                    var sponsorBlockOffset = 0L
                    if (sponsorBlockEnabled) {
                        for (segment in currentSkipSegments) {
                            if (currentVideoPos >= segment.second) {
                                sponsorBlockOffset += (segment.second - segment.first)
                            } else if (currentVideoPos > segment.first) {
                                sponsorBlockOffset += (currentVideoPos - segment.first)
                            }
                        }
                    }
                    
                    smoothPosition = currentVideoPos - sponsorBlockOffset + lyricsOffset
                }
            }
        } else {
            val currentVideoPos = playerConnection.player.currentPosition
            var sponsorBlockOffset = 0L
            if (sponsorBlockEnabled) {
                for (segment in currentSkipSegments) {
                    if (currentVideoPos >= segment.second) {
                        sponsorBlockOffset += (segment.second - segment.first)
                    } else if (currentVideoPos > segment.first) {
                        sponsorBlockOffset += (currentVideoPos - segment.first)
                    }
                }
            }
            smoothPosition = currentVideoPos - sponsorBlockOffset + lyricsOffset
        }
    }

    val graphemeClusters = remember(mainText) { mainText.toGraphemeClusters() }
    val clusterCount = graphemeClusters.size
    val clusterCharOffsets = remember(mainText) {
        IntArray(clusterCount).also { offsets ->
            var charOffset = 0
            graphemeClusters.forEachIndexed { i, cluster ->
                offsets[i] = charOffset
                charOffset += cluster.length
            }
        }
    }

    val (effectiveWords, effectiveToOriginalIdx) = remember(words, isBackground) {
        words.flatMapIndexed { originalIdx, word ->
            val shouldSplit = word.text.contains('-') && word.text.length > 1 &&
                (!word.hasTrailingSpace || words.size == 1)
            if (shouldSplit) {
                val segments = mutableListOf<String>()
                var start = 0
                for (i in 0 until word.text.length) {
                    if (word.text[i] == '-') {
                        segments.add(word.text.substring(start, i + 1))
                        start = i + 1
                    }
                }
                if (start < word.text.length) {
                    segments.add(word.text.substring(start))
                }

                if (segments.size > 1) {
                    val totalDuration = word.endTime - word.startTime
                    val segmentDuration = totalDuration / segments.size
                    segments.mapIndexed { index, segmentText ->
                        WordTimestamp(
                            text = segmentText,
                            startTime = word.startTime + index * segmentDuration,
                            endTime = word.startTime + (index + 1) * segmentDuration,
                            hasTrailingSpace = if (index == segments.size - 1) word.hasTrailingSpace else false
                        ) to originalIdx
                    }
                } else listOf(word to originalIdx)
            } else listOf(word to originalIdx)
        }.let { data -> data.map { it.first } to data.map { it.second } }
    }

    val charToWordData = remember(mainText, effectiveWords, isBackground, graphemeClusters, clusterCharOffsets) {
        val wordIdxMap = IntArray(clusterCount) { -1 }
        val charInWordMap = IntArray(clusterCount)
        val wordLenMap = IntArray(clusterCount) { 1 }
        var currentPos = 0
        var clCursor = 0
        effectiveWords.forEachIndexed { wordIdx, word ->
            val rawWordText = word.text.let {
                if (isBackground) {
                    var t = it
                    if (wordIdx == 0) t = t.removePrefix("(")
                    if (wordIdx == effectiveWords.size - 1) t = t.removeSuffix(")")
                    t
                } else it
            }
            
            val cleanWordText = rawWordText.trim()
            if (cleanWordText.isNotEmpty()) {
                val indexInMain = mainText.indexOf(cleanWordText, currentPos)
                if (indexInMain != -1) {
                    val wordEndInMain = indexInMain + cleanWordText.length
                    while (clCursor < clusterCount && clusterCharOffsets[clCursor] < indexInMain) {
                        clCursor++
                    }
                    val wordClusterIndices = mutableListOf<Int>()
                    while (clCursor < clusterCount && clusterCharOffsets[clCursor] < wordEndInMain) {
                        wordClusterIndices.add(clCursor)
                        clCursor++
                    }
                    val wordClusterLen = wordClusterIndices.size
                    wordClusterIndices.forEachIndexed { posInWord, clIdx ->
                        wordIdxMap[clIdx] = wordIdx
                        charInWordMap[clIdx] = posInWord
                        wordLenMap[clIdx] = wordClusterLen
                    }
                    
                    if (clCursor < clusterCount && clusterCharOffsets[clCursor] == wordEndInMain && 
                        wordEndInMain < mainText.length && mainText[wordEndInMain] == ' ') {
                        val spaceClIdx = clCursor
                        wordIdxMap[spaceClIdx] = wordIdx
                        charInWordMap[spaceClIdx] = wordClusterLen
                        wordLenMap[spaceClIdx] = wordClusterLen + 1
                        clCursor++
                    }
                    currentPos = wordEndInMain
                }
            }
        }
        Triple(wordIdxMap, charInWordMap, wordLenMap)
    }

    val hyphenGroupData = remember(effectiveWords) {
        val map = mutableMapOf<Int, HyphenGroupWord>()
        var currentGroup = mutableListOf<Int>()
        effectiveWords.forEachIndexed { wordIdx, word ->
            currentGroup.add(wordIdx)
            if (!word.text.endsWith("-")) {
                if (currentGroup.size > 1) {
                    val groupSize = currentGroup.size
                    val groupStartMs = (effectiveWords[currentGroup.first()].startTime * 1000).toLong()
                    val groupEndMs = (word.endTime * 1000).toLong()
                    currentGroup.forEachIndexed { pos, idx ->
                        map[idx] = HyphenGroupWord(pos, groupSize, pos == groupSize - 1, groupStartMs, groupEndMs)
                    }
                }
                currentGroup = mutableListOf()
            }
        }
        map
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val maxWidthPx = constraints.maxWidth
        val layoutResult = remember(mainText, maxWidthPx, lyricStyle) {
            textMeasurer.measure(
                text = mainText,
                style = lyricStyle,
                constraints = Constraints(minWidth = maxWidthPx, maxWidth = maxWidthPx),
                softWrap = true
            )
        }
        
        val letterLayouts = remember(mainText, lyricStyle) {
            graphemeClusters.map { cluster -> textMeasurer.measure(cluster, lyricStyle) }
        }

        val charBoundsList = remember(layoutResult, clusterCharOffsets) {
            Array(clusterCount) { i -> layoutResult.getBoundingBox(clusterCharOffsets[i]) }
        }
        
        val lineIndices = remember(layoutResult, clusterCharOffsets) {
            IntArray(clusterCount) { i -> layoutResult.getLineForOffset(clusterCharOffsets[i]) }
        }
        
        val clusterBaselines = remember(letterLayouts) {
            FloatArray(clusterCount) { i -> letterLayouts[i].firstBaseline }
        }

        val fontSizePx = with(density) { lyricStyle.fontSize.toPx() }
        val glowPaint = remember(fontSizePx) {
            android.graphics.Paint().apply {
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                textSize = fontSizePx
            }
        }
        val cachedGlowFilters = remember { mutableMapOf<Int, android.graphics.BlurMaskFilter>() }
        
        val isRtlText = remember(mainText) { mainText.containsRtl() }
        
        val wordCount = effectiveWords.size
        val lineCount = layoutResult.lineCount
        val wordWobbles = remember(wordCount) { FloatArray(wordCount) }
        val wordCrescendos = remember(wordCount) { FloatArray(wordCount) }
        val wordProgs = remember(wordCount) { DoubleArray(wordCount) }
        val wordIsSung = remember(wordCount) { BooleanArray(wordCount) }
        val wordSungFactors = remember(wordCount) { FloatArray(wordCount) }
        
        val lineCurrentPushes = remember(lineCount) { FloatArray(lineCount) }
        val lineTotalPushes = remember(lineCount) { FloatArray(lineCount) }

        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height((layoutResult.size.height / density.density).dp)
            .graphicsLayer(
                clip = false
            )
        ) {
            if (mainText.isEmpty()) return@Canvas
            val currentSmoothPos = smoothPosition

            if (!isTracking) {
                drawText(layoutResult, color = lineColor)
                return@Canvas
            }

            val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData

            if (isRtlText) {
                drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))

                effectiveWords.indices.forEach { wIdx ->
                    val word = effectiveWords[wIdx]
                    val wStartMs = (word.startTime * 1000).toLong()
                    val wEndMs = (word.endTime * 1000).toLong()
                    val isWordSung = currentSmoothPos >= wStartMs
                    val isWordActive = currentSmoothPos in wStartMs..wEndMs
                    val sungFactor = if (isWordSung) 1f 
                                    else if (isWordActive) ((currentSmoothPos - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    else 0f
                    
                    var left = Float.MAX_VALUE
                    var right = Float.MIN_VALUE
                    var top = Float.MAX_VALUE
                    var bottom = Float.MIN_VALUE
                    var found = false

                    for (i in 0 until clusterCount) {
                        if (wordIdxMap[i] == wIdx) {
                            val bounds = charBoundsList[i]
                            left = minOf(left, bounds.left)
                            right = maxOf(right, bounds.right)
                            top = minOf(top, bounds.top)
                            bottom = maxOf(bottom, bounds.bottom)
                            found = true
                        }
                    }

                    if (found) {
                        if (isWordSung || (!animateLyrics && isWordActive)) {
                            clipRect(left = left, top = top, right = right, bottom = bottom) {
                                drawText(layoutResult, color = expressiveAccent)
                            }
                        } else if (isWordActive && sungFactor > 0f) {
                            clipRect(left = left, top = top, right = right, bottom = bottom) {
                                drawText(layoutResult, color = expressiveAccent.copy(alpha = focusedAlpha + (1f - focusedAlpha) * sungFactor))
                            }
                        }
                    }
                }
                return@Canvas
            }

            for (i in 0 until lineCount) {
                lineCurrentPushes[i] = 0f
                lineTotalPushes[i] = 0f
            }

            for (wordIdx in 0 until wordCount) {
                val word = effectiveWords[wordIdx]
                val startMs = (word.startTime * 1000).toLong()
                val endMs = (word.endTime * 1000).toLong()
                val dur = (endMs - startMs).coerceAtLeast(100L).toDouble()
                
                val timeSinceStart = (currentSmoothPos - startMs).toFloat()
                val wProg = timeSinceStart.toDouble() / dur
                wordProgs[wordIdx] = wProg
                
                val isSung = currentSmoothPos > endMs
                wordIsSung[wordIdx] = isSung
                
                val sungFactor = if (isSung) 1f else if (currentSmoothPos in startMs..endMs) (timeSinceStart / dur.toFloat()).coerceIn(0f, 1f) else 0f
                wordSungFactors[wordIdx] = sungFactor
                
                val wobble = if (animateLyrics) {
                    if (timeSinceStart in 0f..750f) {
                        if (timeSinceStart < 125f) timeSinceStart / 125f
                        else (1f - (timeSinceStart - 125f) / 625f).coerceAtLeast(0f)
                    } else 0f
                } else 0f
                wordWobbles[wordIdx] = wobble
                
                var crescendo = 0f
                val groupWord = hyphenGroupData[wordIdx]
                if (groupWord != null && animateLyrics) {
                    val p = sungFactor
                    val timeSinceEnd = (currentSmoothPos - groupWord.groupEndMs).toFloat()
                    val pOut = (timeSinceEnd / 600f).coerceIn(0f, 1f)
                    val peakScale = 0.06f
                    val decay = 3.5f
                    val freq = 5.0f
                    val baseScalePerSegment = 0.012f
                    
                    if (pOut > 0f) {
                        val baseAtEnd = groupWord.pos * baseScalePerSegment
                        val totalAtEnd = baseAtEnd + peakScale
                        crescendo = totalAtEnd * kotlin.math.exp(-decay * pOut).toFloat() * kotlin.math.cos(freq * pOut * kotlin.math.PI).toFloat() * (1f - pOut)
                    } else if (groupWord.isLast) {
                        val base = groupWord.pos * baseScalePerSegment
                        val springPart = peakScale * (1f - kotlin.math.exp(-decay * p).toFloat() * kotlin.math.cos(freq * p * kotlin.math.PI).toFloat() * (1f - p))
                        crescendo = base + springPart
                    } else {
                        val boost = if (p > 0f) 0.02f * (1f - p) else 0f
                        crescendo = (groupWord.pos * baseScalePerSegment) + boost
                    }
                }
                wordCrescendos[wordIdx] = crescendo
            }

            for (i in 0 until clusterCount) {
                val lineIdx = lineIndices[i]
                val charBounds = charBoundsList[i]
                val wordIdx = wordIdxMap[i]
                
                var charScaleX = 1f
                if (wordIdx != -1) {
                    val originalWordIdx = effectiveToOriginalIdx[wordIdx]
                    val wobble = if (originalWordIdx != -1 && animateLyrics) wordWobbles[originalWordIdx] else 0f
                    val crescendo = if (animateLyrics) wordCrescendos[wordIdx] else 0f
                    
                    val wProg = wordProgs[wordIdx]
                    val cInW = charInWordMap[i].toDouble()
                    val wLen = wordLenMap[i].toDouble()
                    val charLp = ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    
                    val isWordSung = wordIsSung[wordIdx]
                    val sungFactor = wordSungFactors[wordIdx]
                    
                    val nudgeScale = if (animateLyrics && !isWordSung && sungFactor > 0f) {
                        0.038f * kotlin.math.sin(charLp * kotlin.math.PI).toFloat() * kotlin.math.exp(-3f * charLp).toFloat()
                    } else 0f
                    
                    charScaleX += (wobble * 0.025f) + crescendo + (nudgeScale * 0.3f)
                }
                lineTotalPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
            }

            val wallTime = System.currentTimeMillis()

            for (i in 0 until clusterCount) {
                val lineIdx = lineIndices[i]
                val charBounds = charBoundsList[i]
                val wordIdx = wordIdxMap[i]
                
                val alignShift = when(alignment) {
                    TextAlign.Center -> -lineTotalPushes[lineIdx] / 2f
                    TextAlign.Right -> -lineTotalPushes[lineIdx]
                    else -> 0f
                }
                
                if (wordIdx != -1) {
                    val originalWordIdx = effectiveToOriginalIdx[wordIdx]
                    val wobble = if (originalWordIdx != -1 && animateLyrics) wordWobbles[originalWordIdx] else 0f
                    val crescendo = if (animateLyrics) wordCrescendos[wordIdx] else 0f
                    
                    val wProg = wordProgs[wordIdx]
                    val cInW = charInWordMap[i].toDouble()
                    val wLen = wordLenMap[i].toDouble()
                    val charLp = ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    
                    val isWordSung = wordIsSung[wordIdx]
                    val sungFactor = wordSungFactors[wordIdx]
                    
                    val shouldGlow = animateLyrics && !isWordSung && sungFactor > 0.001f
                    
                    val nudgeScale = if (animateLyrics && !isWordSung && sungFactor > 0f) {
                        0.038f * kotlin.math.sin(charLp * kotlin.math.PI).toFloat() * kotlin.math.exp(-3f * charLp).toFloat()
                    } else 0f
                    
                    val charScaleX2 = if (animateLyrics) 1f + wobble * 0.025f + crescendo + nudgeScale * 0.3f else 1f
                    val charScaleY2 = if (animateLyrics) 1f + wobble * 0.015f + crescendo + nudgeScale else 1f
                    
                    withTransform({
                        var waveOffset = 0f
                        val groupWord = hyphenGroupData[wordIdx]
                        if (groupWord != null && animateLyrics) {
                            val timeInGroup = (currentSmoothPos - groupWord.groupStartMs).toFloat()
                            val timeToGroupEnd = (groupWord.groupEndMs - currentSmoothPos).toFloat()
                            val waveFade = (timeInGroup / 200f).coerceIn(0f, 1f) * (timeToGroupEnd / 200f).coerceIn(0f, 1f)
                            if (waveFade > 0.01f) {
                                val waveSpeed = 0.006f
                                val waveHeight = 3.24f
                                val phaseOffset = i * 0.4f
                                waveOffset = kotlin.math.sin(wallTime * waveSpeed + phaseOffset).toFloat() * waveHeight * waveFade
                            }
                        }

                        translate(left = alignShift + lineCurrentPushes[lineIdx] + charBounds.left, top = charBounds.top + waveOffset)
                        scale(
                            charScaleX2,
                            charScaleY2,
                            pivot = Offset(charBounds.width / 2f, charBounds.height)
                        )
                    }) {
                        if (shouldGlow) {
                            val wordItem = effectiveWords[wordIdx]
                            val sMs = wordItem.startTime * 1000
                            val eMs = wordItem.endTime * 1000
                            val dur = eMs - sMs
                            val wordLenText = wordItem.text.length.coerceAtLeast(1)
                            val impactRatio = dur.toFloat() / wordLenText
                            val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                            val impactFactor = (((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fadeFactor
                            if (impactFactor > 0.01f) {
                                val glowAlpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)
                                val baseGlowRadius = density.run { 12.dp.toPx() } * impactFactor                                                                                    
                                
                                val intRadius = baseGlowRadius.toInt()
                                if (intRadius > 0) {
                                    drawIntoCanvas { canvas ->
                                        glowPaint.maskFilter = cachedGlowFilters.getOrPut(intRadius) {
                                            android.graphics.BlurMaskFilter(intRadius.toFloat(), android.graphics.BlurMaskFilter.Blur.NORMAL)
                                        }
                                        glowPaint.color = expressiveAccent.copy(alpha = glowAlpha).toArgb()
                                        canvas.nativeCanvas.drawText(graphemeClusters[i], 0f, clusterBaselines[i], glowPaint)
                                    }
                                }
                            }
                        }
                        
                        val isWordActive = currentSmoothPos in (effectiveWords[wordIdx].startTime * 1000).toLong()..(effectiveWords[wordIdx].endTime * 1000).toLong()
                        val baseAlpha = if (isWordSung || charLp > 0.99f || (!animateLyrics && isWordActive)) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                        drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = baseAlpha))
                        
                        if (animateLyrics && !isWordSung && charLp > 0f && charLp < 1f) {
                            val fXL = charBounds.width * charLp
                            val eW = (charBounds.width * 0.45f).coerceAtLeast(1f)
                            val sWL = (fXL - eW).coerceAtLeast(0f)
                            if (sWL > 0f) {
                                clipRect(left = 0f, top = 0f, right = sWL, bottom = charBounds.height) { drawText(letterLayouts[i], color = expressiveAccent) }
                            }
                            for (j in 0 until 12) {
                                val start = sWL + (j * eW / 12f)
                                val end = (sWL + ((j + 1) * eW / 12f) + 0.5f).coerceAtMost(fXL)
                                if (end > start) {
                                    clipRect(left = start, top = 0f, right = end, bottom = charBounds.height) { drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = 1f - (j + 0.5f) / 12f)) }
                                }
                            }
                        }
                    }
                    lineCurrentPushes[lineIdx] += charBounds.width * (charScaleX2 - 1f)
                } else {
                    withTransform({
                        translate(left = alignShift + lineCurrentPushes[lineIdx] + charBounds.left, top = charBounds.top)
                    }) {
                        drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = focusedAlpha))
                    }
                }
            }
        }
    }
}

private data class HyphenGroupWord(
    val pos: Int,
    val size: Int,
    val isLast: Boolean,
    val groupStartMs: Long,
    val groupEndMs: Long
)

private fun String.containsRtl(): Boolean {
    for (c in this) {
        val directionality = Character.getDirectionality(c).toInt()
        if (directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt() ||
            directionality == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt()
        ) {
            return true
        }
    }
    return false
}

private fun String.toGraphemeClusters(): List<String> {
    if (isEmpty()) return emptyList()
    val result = mutableListOf<String>()
    val it = java.text.BreakIterator.getCharacterInstance()
    it.setText(this)
    var start = it.first()
    var end = it.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(substring(start, end))
        start = end
        end = it.next()
    }
    return result
}
