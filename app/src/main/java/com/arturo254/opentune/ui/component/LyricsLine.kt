package com.arturo254.opentune.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arturo254.opentune.lyrics.LyricsEntry
import com.arturo254.opentune.constants.AppleMusicLyricsBlurKey
import com.arturo254.opentune.ui.screens.settings.LyricsPosition
import com.arturo254.opentune.utils.rememberPreference
import kotlin.math.sin

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
    position: Long,
    modifier: Modifier = Modifier
) {
    val (appleMusicLyricsBlur) = rememberPreference(AppleMusicLyricsBlurKey, true)

    val blurRadius by animateFloatAsState(
        targetValue = if (!appleMusicLyricsBlur || !isAutoScrollActive || isActive || !isSynced || isSelectionModeActive)
            0f
        else
            6f,
        animationSpec = tween(durationMillis = 600),
        label = "blur"
    )

    val animatedScale by animateFloatAsState(
        targetValue = when {
            !isSynced || isActive -> 1.05f
            distanceFromCurrent == 1 -> 1f
            else -> 0.95f
        },
        animationSpec = tween(durationMillis = 400)
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = when {
            !isSynced || (isSelectionModeActive && isSelected) -> 1f
            isActive -> 1f
            distanceFromCurrent == 1 -> 0.7f
            distanceFromCurrent == 2 -> 0.4f
            else -> 0.2f
        },
        animationSpec = tween(durationMillis = 400)
    )

    val isBackground = entry.text.contains("{bg}")

    val cleanDisplayText = remember(entry.text) {
        entry.text
            .replace("{bg}", "")
            .replace(Regex("\\{agent:v\\d+\\}"), "")
            .trim()
    }

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

    val annotatedText = remember(entry, position, isActive, isSelectionModeActive, textColor) {
        buildAnnotatedString {
            val words = entry.words
            if (!words.isNullOrEmpty() && isSynced) {
                words.forEachIndexed { i, word ->
                    val isActiveWord = position >= word.startTime && position < word.endTime
                    val isPastWord = position >= word.endTime
                    
                    val wordColor = when {
                        !isActive -> textColor.copy(alpha = 0.5f)
                        isActiveWord -> textColor
                        isPastWord -> textColor.copy(alpha = 0.9f)
                        else -> textColor.copy(alpha = 0.35f)
                    }
                    
                    val wordWeight = when {
                        isActiveWord -> FontWeight.Black
                        isPastWord -> FontWeight.Bold
                        else -> FontWeight.Medium
                    }

                    val wordStyle = SpanStyle(
                        color = if (isBackground) wordColor.copy(alpha = wordColor.alpha * 0.7f) else wordColor,
                        fontWeight = wordWeight,
                        fontStyle = if (isBackground) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                    
                    withStyle(wordStyle) {
                        append(word.text)
                    }
                    if (i < words.lastIndex) {
                        append(" ")
                    }
                }
            } else {
                withStyle(
                    SpanStyle(
                        color = if (isBackground) textColor.copy(alpha = 0.7f) else textColor,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                        fontStyle = if (isBackground) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                    )
                ) {
                    append(cleanDisplayText)
                }
            }
        }
    }

    Column(
        modifier = itemModifier,
        horizontalAlignment = when (lyricsTextPosition) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }
    ) {
        if (isActive && isSynced) {
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
                val pulse = pulseProgress.value
                val pulseEffect = (sin(pulse * Math.PI.toFloat()) * 0.15f).coerceIn(0f, 0.15f)
                
                Text(
                    text = annotatedText,
                    fontSize = textSize.sp,
                    textAlign = when (lyricsTextPosition) {
                        LyricsPosition.LEFT -> TextAlign.Left
                        LyricsPosition.CENTER -> TextAlign.Center
                        LyricsPosition.RIGHT -> TextAlign.Right
                    },
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = textColor.copy(alpha = 0.4f),
                            offset = Offset(0f, 0f),
                            blurRadius = 14f * (1f + pulseEffect)
                        )
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Text(
                text = annotatedText,
                fontSize = textSize.sp,
                textAlign = when (lyricsTextPosition) {
                    LyricsPosition.LEFT -> TextAlign.Left
                    LyricsPosition.CENTER -> TextAlign.Center
                    LyricsPosition.RIGHT -> TextAlign.Right
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}