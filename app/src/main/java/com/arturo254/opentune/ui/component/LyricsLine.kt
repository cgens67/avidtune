package com.arturo254.opentune.ui.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
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
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.constants.AppleMusicLyricsBlurKey
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.lyrics.WordTimestamp
import com.arturo254.opentune.ui.screens.settings.LyricsPosition
import com.arturo254.opentune.utils.rememberPreference
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LyricsLine(
    entry: LyricsEntry,
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
    modifier: Modifier = Modifier
) {
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)
    val playerConnection = LocalPlayerConnection.current ?: return

    val smoothPositionState = remember { mutableLongStateOf(entry.time) }
    val isTracking = isActive || distanceFromCurrent <= 3

    LaunchedEffect(isTracking) {
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
                    smoothPositionState.longValue = lastPlayerPos + (if (playerConnection.player.isPlaying) elapsed else 0L)
                }
            }
        } else {
            smoothPositionState.longValue = playerConnection.player.currentPosition
        }
    }

    val mainText = if (entry.isBackground) entry.text.removePrefix("(").removeSuffix(")") else entry.text

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

    val isLineCurrentlyPlaying by remember {
        derivedStateOf {
            if (effectiveWords != null && effectiveWords.isNotEmpty()) {
                val startMs = (effectiveWords.first().startTime * 1000).toLong()
                val endMs = (effectiveWords.last().endTime * 1000).toLong() + 300L
                smoothPositionState.longValue in startMs..endMs
            } else {
                false
            }
        }
    }

    val effectivelyActive = isActive || (isTracking && isLineCurrentlyPlaying)

    val blurRadius by animateFloatAsState(
        targetValue = if (!appleMusicLyricsBlur || !isAutoScrollActive || effectivelyActive || !isSynced || isSelectionModeActive)
            0f
        else
            6f,
        animationSpec = tween(durationMillis = 600),
        label = "blur"
    )

    val animatedScale by animateFloatAsState(
        targetValue = when {
            !isSynced || effectivelyActive -> 1.05f
            distanceFromCurrent == 1 -> 1f
            else -> 0.95f
        },
        animationSpec = tween(durationMillis = 400),
        label = "scale"
    )

    val inactiveAlpha = if (entry.isBackground) 0.15f else 0.2f
    val activeAlpha = 1f
    val focusedAlpha = if (entry.isBackground) 0.6f else 0.4f

    val targetAlpha = if (!isSynced) {
        activeAlpha
    } else if (effectivelyActive) {
        activeAlpha
    } else if (isAutoScrollActive) {
        when (distanceFromCurrent) {
            0 -> focusedAlpha
            1 -> if (entry.isBackground) 0.4f else 0.3f
            2 -> if (entry.isBackground) 0.25f else 0.2f
            3 -> 0.15f
            4 -> 0.1f
            else -> 0.08f
        }
    } else {
        inactiveAlpha
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 400),
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
        .blur(blurRadius.dp)

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
        val lineColor = textColor.copy(alpha = if (entry.isBackground && !effectivelyActive) focusedAlpha else targetAlpha)

        val lyricStyle = TextStyle(
            fontSize = if (entry.isBackground) (textSize * 0.7f).sp else textSize.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = if (entry.isBackground) FontStyle.Italic else FontStyle.Normal,
            lineHeight = if (entry.isBackground) (textSize * 0.7f * lineSpacing).sp else (textSize * lineSpacing).sp,
            letterSpacing = (-0.5).sp,
            textAlign = agentTextAlign,
            fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )

        if (isSynced && effectiveWords != null && isTracking && mainText.isNotBlank()) {
            WordLevelLyrics(
                mainText = mainText,
                words = effectiveWords,
                smoothPositionState = smoothPositionState,
                isTracking = isTracking,
                lyricStyle = lyricStyle,
                lineColor = lineColor,
                expressiveAccent = textColor,
                isBackground = entry.isBackground,
                focusedAlpha = focusedAlpha,
                alignment = agentTextAlign
            )
        } else {
            if (effectivelyActive && isSynced) {
                val fillProgress = remember { Animatable(0f) }
                val pulseProgress = remember { Animatable(0f) }

                LaunchedEffect(entry.time) {
                    fillProgress.snapTo(0f)
                    fillProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1200,
                            easing = FastOutSlowInEasing
                        )
                    )
                }

                LaunchedEffect(Unit) {
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
                    val pulseEffect = (sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
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
    }
}

@Composable
private fun WordLevelLyrics(
    mainText: String,
    words: List<WordTimestamp>,
    smoothPositionState: State<Long>,
    isTracking: Boolean,
    lyricStyle: TextStyle,
    lineColor: Color,
    expressiveAccent: Color,
    isBackground: Boolean,
    focusedAlpha: Float,
    alignment: TextAlign
) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val glowPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
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
            val indexInMain = mainText.indexOf(rawWordText, currentPos)
            if (indexInMain != -1) {
                val wordEndInMain = indexInMain + rawWordText.length
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
        
        val isRtlText = remember(mainText) { mainText.containsRtl() }
        
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height((layoutResult.size.height / density.density).dp)
            .graphicsLayer(
                clip = false,
                compositingStrategy = CompositingStrategy.Offscreen,
            )
        ) {
            val smoothPosition = smoothPositionState.value
            if (mainText.isEmpty()) return@Canvas
            if (!isTracking) {
                drawText(layoutResult, color = lineColor)
            } else {
                if (isRtlText) {
                    val (wordIdxMap, _, _) = charToWordData
                    val wordFactors = effectiveWords.map { word ->
                        val wStartMs = (word.startTime * 1000).toLong()
                        val wEndMs = (word.endTime * 1000).toLong()
                        val isWordSung = smoothPosition > wEndMs
                        val isWordActive = smoothPosition in wStartMs..wEndMs
                        val sungFactor = if (isWordSung) 1f 
                                        else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                        else 0f
                        Triple(sungFactor, isWordSung, isWordActive)
                    }

                    drawText(layoutResult, color = lineColor.copy(alpha = focusedAlpha))

                    effectiveWords.indices.forEach { wIdx ->
                        val (sungFactor, isWordSung, isWordActive) = wordFactors[wIdx]
                        
                        var left = Float.MAX_VALUE
                        var right = Float.MIN_VALUE
                        var top = Float.MAX_VALUE
                        var bottom = Float.MIN_VALUE
                        var found = false

                        for (i in 0 until clusterCount) {
                            if (wordIdxMap[i] == wIdx) {
                                val charOffset = clusterCharOffsets[i]
                                val bounds = layoutResult.getBoundingBox(charOffset)
                                left = minOf(left, bounds.left)
                                right = maxOf(right, bounds.right)
                                top = minOf(top, bounds.top)
                                bottom = maxOf(bottom, bounds.bottom)
                                found = true
                            }
                        }

                        if (found) {
                            if (isWordSung) {
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

                val (wordIdxMap, charInWordMap, wordLenMap) = charToWordData
                val wordFactors = effectiveWords.map { word ->
                    val wStartMs = (word.startTime * 1000).toLong()
                    val wEndMs = (word.endTime * 1000).toLong()
                    val isWordSung = smoothPosition > wEndMs
                    val isWordActive = smoothPosition in wStartMs..wEndMs
                    val sungFactor = if (isWordSung) 1f 
                                    else if (isWordActive) ((smoothPosition - wStartMs).toFloat() / (wEndMs - wStartMs).coerceAtLeast(1)).coerceIn(0f, 1f)
                                    else 0f
                    Triple(sungFactor, word, isWordSung)
                }

                val wordWobbles = FloatArray(words.size)
                words.forEachIndexed { wordIdx, word ->
                    val startMs = (word.startTime * 1000).toLong()
                    val timeSinceStart = (smoothPosition - startMs).toFloat()
                    val wobble = if (timeSinceStart in 0f..750f) {
                        if (timeSinceStart < 125f) timeSinceStart / 125f
                        else (1f - (timeSinceStart - 125f) / 625f).coerceAtLeast(0f)
                    } else 0f
                    wordWobbles[wordIdx] = wobble
                }

                val lineCurrentPushes = FloatArray(layoutResult.lineCount)
                val lineTotalPushes = FloatArray(layoutResult.lineCount)
                
                for (i in 0 until clusterCount) {
                    val charOffset = clusterCharOffsets[i]
                    val lineIdx = layoutResult.getLineForOffset(charOffset)
                    val wordIdx = wordIdxMap[i]
                    val originalWordIdx = if (wordIdx != -1) effectiveToOriginalIdx[wordIdx] else -1
                    
                    val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)
                    val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                    
                    var crescendoDeltaX = 0f
                    val groupWord = if (wordIdx != -1) hyphenGroupData[wordIdx] else null
                    if (groupWord != null) {
                        val p = sungFactor
                        val timeSinceEnd = (smoothPosition - groupWord.groupEndMs).toFloat()
                        val exitDuration = 600f
                        val pOut = (timeSinceEnd / exitDuration).coerceIn(0f, 1f)
                        val peakScale = 0.06f
                        val decay = 2.5f
                        val freq = 10.0f
                        val baseScalePerSegment = 0.012f
                        if (pOut > 0f) {
                            val baseAtEnd = groupWord.pos * baseScalePerSegment
                            val totalAtEnd = baseAtEnd + peakScale
                            crescendoDeltaX = totalAtEnd * exp(-decay * pOut) * cos(freq * pOut * PI.toFloat()) * (1f - pOut)
                        } else if (groupWord.isLast) {
                            val base = groupWord.pos * baseScalePerSegment
                            val springPart = peakScale * (1f - exp(-decay * p) * cos(freq * p * PI.toFloat()) * (1f - p))
                            crescendoDeltaX = base + springPart
                        } else {
                            val boost = if (p > 0f) 0.02f * (1f - p) else 0f
                            crescendoDeltaX = (groupWord.pos * baseScalePerSegment) + boost
                        }
                    }

                    val charLp = if (wordItem != null) {
                        val sMs = wordItem.startTime * 1000
                        val dur = (wordItem.endTime * 1000 - wordItem.startTime * 1000).coerceAtLeast(100.0)
                        val wProg = (smoothPosition.toDouble() - sMs) / dur
                        val cInW = charInWordMap[i].toDouble()
                        val wLen = wordLenMap[i].toDouble()
                        ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    } else 0f

                    val nudgeScale = if (wordItem != null && !isWordSung && sungFactor > 0f) {
                        0.038f * sin(charLp * PI.toFloat()) * exp(-3f * charLp)
                    } else 0f

                    val charScaleX = 1f + (wobble * 0.025f) + crescendoDeltaX + (nudgeScale * 0.3f)
                    val charBounds = layoutResult.getBoundingBox(charOffset)
                    lineTotalPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
                }

                for (i in 0 until clusterCount) {
                    val charOffset = clusterCharOffsets[i]
                    val lineIdx = layoutResult.getLineForOffset(charOffset)
                    val charBounds = layoutResult.getBoundingBox(charOffset)
                    val wordIdx = wordIdxMap[i]
                    val originalWordIdx = if (wordIdx != -1) effectiveToOriginalIdx[wordIdx] else -1
                    
                    val alignShift = when(alignment) {
                        TextAlign.Center -> -lineTotalPushes[lineIdx] / 2f
                        TextAlign.Right -> -lineTotalPushes[lineIdx]
                        else -> 0f
                    }
                    
                    val (sungFactor, wordItem, isWordSung) = if (wordIdx != -1) wordFactors[wordIdx] else Triple(0f, null, false)
                    val wobble = if (originalWordIdx != -1) wordWobbles[originalWordIdx] else 0f
                    val wobbleX = wobble * 0.025f
                    val wobbleY = wobble * 0.015f
                    
                    val charLp = if (wordItem != null) {
                        val sMs = wordItem.startTime * 1000
                        val dur = (wordItem.endTime * 1000 - wordItem.startTime * 1000).coerceAtLeast(100.0)
                        val wProg = (smoothPosition.toDouble() - sMs) / dur
                        val cInW = charInWordMap[i].toDouble()
                        val wLen = wordLenMap[i].toDouble()
                        ((wProg - cInW / wLen) * wLen).coerceIn(0.0, 1.0).toFloat()
                    } else 0f

                    val shouldGlow = wordItem != null && !isWordSung && sungFactor > 0.001f

                    var crescendoDeltaX = 0f
                    var crescendoDeltaY = 0f
                    val groupWord = if (wordIdx != -1) hyphenGroupData[wordIdx] else null
                    if (groupWord != null) {
                        val p = sungFactor
                        val timeSinceEnd = (smoothPosition - groupWord.groupEndMs).toFloat()
                        val exitDuration = 600f
                        val pOut = (timeSinceEnd / exitDuration).coerceIn(0f, 1f)
                        val peakScale = 0.06f
                        val decay = 3.5f
                        val freq = 5.0f
                        val baseScalePerSegment = 0.012f
                        if (pOut > 0f) {
                            val baseAtEnd = groupWord.pos * baseScalePerSegment
                            val totalAtEnd = baseAtEnd + peakScale
                            val springOut = totalAtEnd * exp(-decay * pOut) * cos(freq * pOut * PI.toFloat()) * (1f - pOut)
                            crescendoDeltaX = springOut
                            crescendoDeltaY = springOut
                        } else if (groupWord.isLast) {
                            val base = groupWord.pos * baseScalePerSegment
                            val springPart = peakScale * (1f - exp(-decay * p) * cos(freq * p * PI.toFloat()) * (1f - p))
                            crescendoDeltaX = base + springPart
                            crescendoDeltaY = base + springPart
                        } else {
                            val boost = if (p > 0f) 0.02f * (1f - p) else 0f
                            val base = (groupWord.pos * baseScalePerSegment) + boost
                            crescendoDeltaX = base
                            crescendoDeltaY = base
                        }
                    }

                    val nudgeStrength = 0.038f
                    val nudgeScale = if (wordItem != null && !isWordSung && sungFactor > 0f) {
                        nudgeStrength * sin(charLp * PI.toFloat()) * exp(-3f * charLp)
                    } else 0f
                    
                    val charScaleX = 1f + wobbleX + crescendoDeltaX + nudgeScale * 0.3f
                    val charScaleY = 1f + wobbleY + crescendoDeltaY + nudgeScale

                    withTransform({
                        var waveOffset = 0f
                        if (groupWord != null) {
                            val wallTime = System.currentTimeMillis()
                            val adjSmoothPos = smoothPosition
                            val timeInGroup = (adjSmoothPos - groupWord.groupStartMs).toFloat()
                            val timeToGroupEnd = (groupWord.groupEndMs - adjSmoothPos).toFloat()
                            val waveFade = (timeInGroup / 200f).coerceIn(0f, 1f) * (timeToGroupEnd / 200f).coerceIn(0f, 1f)
                            if (waveFade > 0.01f) {
                                val waveSpeed = 0.006f
                                val waveHeight = 3.24f
                                val phaseOffset = i * 0.4f
                                waveOffset = sin(wallTime * waveSpeed + phaseOffset) * waveHeight * waveFade
                            }
                        }

                        translate(left = alignShift + lineCurrentPushes[lineIdx] + charBounds.left, top = charBounds.top + waveOffset)
                        if (wordIdx != -1) {
                            scale(
                                charScaleX,
                                charScaleY,
                                pivot = Offset(charBounds.width / 2f, charBounds.height)
                            )
                        }
                    }) {
                        if (shouldGlow) {
                            val sMs = wordItem.startTime * 1000
                            val eMs = wordItem.endTime * 1000
                            val dur = eMs - sMs
                            val wordLenText = wordItem.text.length.coerceAtLeast(1)
                            val impactRatio = dur.toFloat() / wordLenText
                            val fadeFactor = (sungFactor * 5f).coerceIn(0f, 1f) * ((1f - sungFactor) * 8f).coerceIn(0f, 1f)
                            val impactFactor = (((impactRatio - 100f) / 250f).coerceIn(0f, 1f) * 0.6f + ((dur.toFloat() - 300f) / 1500f).coerceIn(0f, 1f) * 0.4f).coerceIn(0f, 1f) * fadeFactor
                            if (impactFactor > 0.01f) {
                                val glowAlpha = (0.35f * impactFactor).coerceIn(0f, 0.4f)
                                val baseGlowRadius = 12.dp.toPx() * impactFactor                                                                                    
                                drawIntoCanvas { canvas ->
                                    glowPaint.maskFilter = BlurMaskFilter(baseGlowRadius, BlurMaskFilter.Blur.NORMAL)
                                    glowPaint.color = expressiveAccent.copy(alpha = glowAlpha).toArgb()
                                    glowPaint.textSize = lyricStyle.fontSize.toPx()
                                    glowPaint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                                    canvas.nativeCanvas.drawText(letterLayouts[i].layoutInput.text.text, 0f, letterLayouts[i].firstBaseline, glowPaint)
                                }
                            }
                        }
                        val baseAlpha = if (isWordSung || charLp > 0.99f) 1f else (focusedAlpha + (1f - focusedAlpha) * sungFactor)
                        drawText(letterLayouts[i], color = expressiveAccent.copy(alpha = if (wordIdx == -1) focusedAlpha else baseAlpha))
                        if (!isWordSung && charLp > 0f && charLp < 1f) {
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
                    lineCurrentPushes[lineIdx] += charBounds.width * (charScaleX - 1f)
                }
            }
        }
    }
}