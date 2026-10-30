package com.cgens67.avidtune.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp,
    sponsorSegments: List<Pair<Float, Float>> = emptyList(),
    sponsorColor: Color = Color(0xFF00E676).copy(alpha = 0.85f),
) {
    val inactiveTrackColor = colors.inactiveTrackColor
    val activeTrackColor = colors.activeTrackColor
    val inactiveTickColor = colors.inactiveTickColor
    val activeTickColor = colors.activeTickColor
    val valueRange = sliderState.valueRange
    Canvas(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        drawTrack(
            stepsToTickFractions(sliderState.steps),
            0f,
            calcFraction(
                valueRange.start,
                valueRange.endInclusive,
                sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
            ),
            inactiveTrackColor,
            activeTrackColor,
            inactiveTickColor,
            activeTickColor,
            trackHeight,
            sponsorSegments,
            sponsorColor
        )
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp,
    sponsorSegments: List<Pair<Float, Float>> = emptyList(),
    sponsorColor: Color = Color(0xFF00E676).copy(alpha = 0.85f),
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()

    drawLine(
        inactiveTrackColor,
        sliderStart,
        sliderEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )

    if (sponsorSegments.isNotEmpty()) {
        for ((startFrac, endFrac) in sponsorSegments) {
            val clampedStart = startFrac.coerceIn(0f, 1f)
            val clampedEnd = endFrac.coerceIn(0f, 1f)
            if (clampedEnd >= clampedStart) {
                val segX1 = sliderStart.x + (sliderEnd.x - sliderStart.x) * clampedStart
                val segX2 = sliderStart.x + (sliderEnd.x - sliderStart.x) * clampedEnd
                val minSpan = trackStrokeWidth.coerceAtLeast(3.dp.toPx())
                val drawSegX2 = if (abs(segX2 - segX1) < minSpan) {
                    if (isRtl) segX1 - minSpan else segX1 + minSpan
                } else segX2

                drawLine(
                    color = sponsorColor,
                    start = Offset(segX1, center.y),
                    end = Offset(drawSegX2, center.y),
                    strokeWidth = trackStrokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }

    val sliderValueEnd = Offset(
        sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        center.y
    )
    val sliderValueStart = Offset(
        sliderStart.x + (sliderEnd.x - sliderStart.x) * activeRangeStart,
        center.y
    )
    drawLine(
        activeTrackColor,
        sliderValueStart,
        sliderValueEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )

    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)

@Composable
fun SponsorBlockOverlay(
    sponsorSegments: List<Pair<Float, Float>>,
    trackHeight: Dp = 4.dp,
    thumbOffset: Dp = 10.dp,
    color: Color = Color(0xFF00E676).copy(alpha = 0.85f),
    modifier: Modifier = Modifier
) {
    if (sponsorSegments.isEmpty()) return
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .padding(horizontal = thumbOffset)
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderStart = if (isRtl) Offset(size.width, center.y) else Offset(0f, center.y)
        val sliderEnd = if (isRtl) Offset(0f, center.y) else Offset(size.width, center.y)
        for ((startFrac, endFrac) in sponsorSegments) {
            val clampedStart = startFrac.coerceIn(0f, 1f)
            val clampedEnd = endFrac.coerceIn(0f, 1f)
            if (clampedEnd >= clampedStart) {
                val segX1 = sliderStart.x + (sliderEnd.x - sliderStart.x) * clampedStart
                val segX2 = sliderStart.x + (sliderEnd.x - sliderStart.x) * clampedEnd
                val minSpan = trackHeight.toPx().coerceAtLeast(3.dp.toPx())
                val drawSegX2 = if (abs(segX2 - segX1) < minSpan) {
                    if (isRtl) segX1 - minSpan else segX1 + minSpan
                } else segX2

                drawLine(
                    color = color,
                    start = Offset(segX1, center.y),
                    end = Offset(drawSegX2, center.y),
                    strokeWidth = trackHeight.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
