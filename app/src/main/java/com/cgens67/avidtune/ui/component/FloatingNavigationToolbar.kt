@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.cgens67.avidtune.ui.component

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cgens67.avidtune.R
import com.cgens67.avidtune.ui.screens.Screens
import kotlin.math.roundToInt

@Stable
class FloatingToolbarBackdropState internal constructor(
    val layer: GraphicsLayer,
) {
    var positionInWindow by mutableStateOf(Offset.Zero)
        internal set

    internal var isRecording = false
}

val LocalFloatingToolbarBackdrop = staticCompositionLocalOf<FloatingToolbarBackdropState?> { null }

@Composable
fun rememberFloatingToolbarBackdropState(): FloatingToolbarBackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { FloatingToolbarBackdropState(layer) }
}

fun Modifier.floatingToolbarBackdrop(
    state: FloatingToolbarBackdropState,
): Modifier =
    this
        .onGloballyPositioned { coordinates ->
            if (coordinates.isAttached) {
                val position = coordinates.positionInWindow()
                if (state.positionInWindow != position) {
                    state.positionInWindow = position
                }
            }
        }
        .drawWithContent {
            if (size.width >= 1f && size.height >= 1f) {
                state.isRecording = true
                try {
                    state.layer.record {
                        this@drawWithContent.drawContent()
                    }
                } finally {
                    state.isRecording = false
                }
                drawLayer(state.layer)
            } else {
                drawContent()
            }
        }

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    slimNav: Boolean = false,
    backdropLayer: GraphicsLayer? = null,
    backdropPositionInWindow: () -> Offset = { Offset.Zero },
    onFabClick: (() -> Unit)? = null,
    fabIconRes: Int? = null,
    fabContentDescription: String = "",
    onShuffleClick: (() -> Unit)? = null,
    shuffleIconRes: Int? = null,
    shuffleContentDescription: String = "",
    onMusicRecognitionClick: (() -> Unit)? = null,
    musicRecognitionContentDescription: String = "",
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
) {
    if (items.isEmpty()) return

    val localBackdrop = LocalFloatingToolbarBackdrop.current
    val resolvedBackdropLayer = backdropLayer ?: localBackdrop?.layer
    val resolvedBackdropPosition = if (backdropLayer != null) {
        backdropPositionInWindow
    } else {
        { localBackdrop?.positionInWindow ?: Offset.Zero }
    }
    val isBackdropRecording = {
        localBackdrop != null &&
            resolvedBackdropLayer === localBackdrop.layer &&
            localBackdrop.isRecording
    }

    val hasOverflowAction = onShuffleClick != null && shuffleIconRes != null
    val hasFabAction = onFabClick != null && fabIconRes != null
    val hasSideAction = hasOverflowAction || hasFabAction

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (hasSideAction) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassNavigationCapsule(
                    items = items,
                    pureBlack = pureBlack,
                    slimNav = slimNav,
                    backdropLayer = resolvedBackdropLayer,
                    backdropPositionInWindow = resolvedBackdropPosition,
                    isBackdropRecording = isBackdropRecording,
                    isSelected = isSelected,
                    onItemClick = onItemClick,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .widthIn(max = 340.dp),
                )

                if (hasOverflowAction) {
                    FloatingToolbarOverflowAction(
                        pureBlack = pureBlack,
                        onShuffleClick = onShuffleClick,
                        shuffleIconRes = shuffleIconRes,
                        shuffleContentDescription = shuffleContentDescription,
                        onMusicRecognitionClick = onMusicRecognitionClick,
                        musicRecognitionContentDescription = musicRecognitionContentDescription,
                    )
                } else if (hasFabAction) {
                    FloatingToolbarFabAction(
                        pureBlack = pureBlack,
                        onClick = onFabClick,
                        iconRes = fabIconRes,
                        contentDescription = fabContentDescription,
                    )
                }
            }
        } else {
            GlassNavigationCapsule(
                items = items,
                pureBlack = pureBlack,
                slimNav = slimNav,
                backdropLayer = resolvedBackdropLayer,
                backdropPositionInWindow = resolvedBackdropPosition,
                isBackdropRecording = isBackdropRecording,
                isSelected = isSelected,
                onItemClick = onItemClick,
                modifier = Modifier
                    .widthIn(max = if (items.size <= 2) 300.dp else 380.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GlassNavigationCapsule(
    items: List<Screens>,
    pureBlack: Boolean,
    slimNav: Boolean,
    backdropLayer: GraphicsLayer?,
    backdropPositionInWindow: () -> Offset,
    isBackdropRecording: () -> Boolean,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = pureBlack || MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val barHeight = if (slimNav) 56.dp else 64.dp
    val hasBackdropBlur =
        backdropLayer != null &&
            !backdropLayer.isReleased &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val fallbackSurfaceColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface

    val containerColor = when {
        hasBackdropBlur && pureBlack -> Color(0xFF080808).copy(alpha = 0.48f)
        hasBackdropBlur && isDark -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.42f)
        hasBackdropBlur -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.50f)
        pureBlack -> Color(0xFF0A0A0A)
        isDark -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.05f),
                Color.White.copy(alpha = 0.22f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.90f),
                Color.Black.copy(alpha = 0.08f),
                Color.Black.copy(alpha = 0.06f),
                Color.White.copy(alpha = 0.75f),
            ),
        )
    }

    val selectedIndex = items.indexOfFirst { isSelected(it) }
    val hasSelection = selectedIndex >= 0
    val targetIndex = selectedIndex.coerceAtLeast(0)

    val lastTargetIndex = remember { mutableIntStateOf(targetIndex) }
    val movingRightState = remember { mutableStateOf(true) }
    if (targetIndex != lastTargetIndex.intValue) {
        movingRightState.value = targetIndex > lastTargetIndex.intValue
        lastTargetIndex.intValue = targetIndex
    }
    val isMovingRight = movingRightState.value

    val fastSpring = spring<Float>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMedium,
    )
    val trailingSpring = spring<Float>(
        dampingRatio = 0.80f,
        stiffness = Spring.StiffnessMediumLow,
    )

    val animatedLeft by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = if (isMovingRight) trailingSpring else fastSpring,
        label = "navPillLeft",
    )
    val animatedRight by animateFloatAsState(
        targetValue = targetIndex.toFloat(),
        animationSpec = if (isMovingRight) fastSpring else trailingSpring,
        label = "navPillRight",
    )

    val interactionSources = remember(items.size) { List(items.size) { MutableInteractionSource() } }
    val pressedStates = interactionSources.map { it.collectIsPressedAsState().value }
    val isActivePressed = hasSelection && pressedStates.getOrElse(selectedIndex) { false }

    val activePillScale by animateFloatAsState(
        targetValue = if (isActivePressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "activePillPressScale",
    )

    val activePillColor by animateColorAsState(
        targetValue = when {
            isDark && isActivePressed -> Color.White.copy(alpha = 0.20f)
            isDark -> Color.White.copy(alpha = 0.14f)
            isActivePressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
        },
        animationSpec = tween(150),
        label = "activePillColor",
    )

    val activePillBorderColor = if (isDark) {
        Color.White.copy(alpha = if (isActivePressed) 0.22f else 0.12f)
    } else {
        Color.White.copy(alpha = if (isActivePressed) 0.65f else 0.40f)
    }

    var capsulePositionInWindow by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(barHeight)
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.55f else 0.20f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.65f else 0.25f),
            )
            .clip(CircleShape)
            .onGloballyPositioned { coordinates ->
                if (coordinates.isAttached) {
                    val position = coordinates.positionInWindow()
                    if (capsulePositionInWindow != position) {
                        capsulePositionInWindow = position
                    }
                }
            },
    ) {
        if (hasBackdropBlur && backdropLayer != null) {
            val blurRadiusPx = with(density) { 18.dp.toPx() }
            val bleedPx = with(density) { 36.dp.roundToPx() }
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .layout { measurable, constraints ->
                        val baseWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
                        val baseHeight = if (constraints.hasBoundedHeight) constraints.maxHeight else 0
                        val expandedWidth = (baseWidth + bleedPx * 2).coerceAtLeast(1)
                        val expandedHeight = (baseHeight + bleedPx * 2).coerceAtLeast(1)
                        val placeable = measurable.measure(
                            Constraints.fixed(expandedWidth, expandedHeight)
                        )
                        layout(baseWidth, baseHeight) {
                            placeable.place(-bleedPx, -bleedPx)
                        }
                    }
                    .graphicsLayer {
                        clip = true
                        if (blurRadiusPx > 0f) {
                            renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Clamp)
                        }
                    },
            ) {
                drawRect(color = fallbackSurfaceColor)
                if (!backdropLayer.isReleased && !isBackdropRecording()) {
                    val backdropOffset = backdropPositionInWindow()
                    val canvasOffsetX = capsulePositionInWindow.x - bleedPx
                    val canvasOffsetY = capsulePositionInWindow.y - bleedPx
                    clipRect {
                        translate(
                            left = backdropOffset.x - canvasOffsetX,
                            top = backdropOffset.y - canvasOffsetY,
                        ) {
                            drawLayer(backdropLayer)
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(containerColor)
                .border(width = 1.dp, brush = borderBrush, shape = CircleShape),
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
        ) {
            val totalWidthPx = with(density) { maxWidth.toPx() }
            val pillWidthPx = totalWidthPx / items.size

            if (hasSelection && pillWidthPx > 0f) {
                val startIndex = minOf(animatedLeft, animatedRight)
                val endIndex = maxOf(animatedLeft, animatedRight)
                val leftPx = (startIndex * pillWidthPx).coerceIn(0f, (totalWidthPx - pillWidthPx).coerceAtLeast(0f))
                val rightPx = ((endIndex + 1f) * pillWidthPx).coerceIn(leftPx + pillWidthPx, totalWidthPx)
                val widthPx = (rightPx - leftPx).coerceAtLeast(pillWidthPx)

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = leftPx.roundToInt(),
                                y = 0,
                            )
                        }
                        .width(with(density) { widthPx.toDp() })
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = activePillScale
                            scaleY = activePillScale
                        }
                        .clip(CircleShape)
                        .background(activePillColor)
                        .border(width = 0.5.dp, color = activePillBorderColor, shape = CircleShape),
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, screen ->
                    val selected = isSelected(screen)
                    GlassNavigationItem(
                        screen = screen,
                        selected = selected,
                        showLabel = !slimNav,
                        isDark = isDark,
                        interactionSource = interactionSources[index],
                        isPressed = pressedStates[index],
                        onClick = { onItemClick(screen, selected) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassNavigationItem(
    screen: Screens,
    selected: Boolean,
    showLabel: Boolean,
    isDark: Boolean,
    interactionSource: MutableInteractionSource,
    isPressed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            selected && isDark -> Color.White
            selected -> MaterialTheme.colorScheme.onSurface
            isPressed && isDark -> Color.White.copy(alpha = 0.88f)
            isPressed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
            isDark -> Color.White.copy(alpha = 0.68f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        },
        animationSpec = tween(180),
        label = "navItemContentColor",
    )

    val pressHighlightColor by animateColorAsState(
        targetValue = when {
            isPressed && !selected && isDark -> Color.White.copy(alpha = 0.10f)
            isPressed && !selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        animationSpec = tween(120),
        label = "navItemPressHighlight",
    )

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = 0.55f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navItemPressScale",
    )

    val iconPopScale = remember { Animatable(1f) }
    var wasSelected by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) {
        if (selected && !wasSelected) {
            iconPopScale.animateTo(
                targetValue = 1.15f,
                animationSpec = tween(90),
            )
            iconPopScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.52f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        } else if (!selected) {
            iconPopScale.animateTo(1f, tween(140))
        }
        wasSelected = selected
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(pressHighlightColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
                contentDescription = stringResource(screen.titleId),
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconPopScale.value),
            )

            if (showLabel) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = stringResource(screen.titleId),
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarOverflowAction(
    pureBlack: Boolean,
    onShuffleClick: (() -> Unit)?,
    shuffleIconRes: Int?,
    shuffleContentDescription: String,
    onMusicRecognitionClick: (() -> Unit)?,
    musicRecognitionContentDescription: String,
) {
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Box {
        FloatingToolbarDefaults.VibrantFloatingActionButton(
            onClick = { fabMenuExpanded = !fabMenuExpanded },
            containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
            contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
        ) {
            Icon(
                painter = painterResource(R.drawable.more_horiz),
                contentDescription =
                    shuffleContentDescription.ifEmpty {
                        stringResource(R.string.more)
                    },
            )
        }

        DropdownMenu(
            expanded = fabMenuExpanded,
            onDismissRequest = { fabMenuExpanded = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.search)) },
                onClick = {
                    fabMenuExpanded = false
                    onMusicRecognitionClick?.invoke()
                },
                leadingIcon = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                        contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription =
                                    musicRecognitionContentDescription.ifEmpty {
                                        stringResource(R.string.search)
                                    },
                            )
                        }
                    }
                },
                enabled = onMusicRecognitionClick != null,
                colors =
                    MenuDefaults.itemColors(
                        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledTextColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledLeadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    ),
            )

            if (onShuffleClick != null && shuffleIconRes != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.shuffle)) },
                    onClick = {
                        fabMenuExpanded = false
                        onShuffleClick()
                    },
                    leadingIcon = {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = floatingToolbarMenuIconContainerColor(pureBlack = pureBlack),
                            contentColor = floatingToolbarMenuIconContentColor(pureBlack = pureBlack),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(shuffleIconRes),
                                    contentDescription =
                                        shuffleContentDescription.ifEmpty {
                                            stringResource(R.string.shuffle)
                                        },
                                )
                            }
                        }
                    },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
                            leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FloatingToolbarFabAction(
    pureBlack: Boolean,
    onClick: (() -> Unit)?,
    iconRes: Int?,
    contentDescription: String,
) {
    if (onClick == null || iconRes == null) return

    FloatingToolbarDefaults.VibrantFloatingActionButton(
        onClick = onClick,
        containerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack),
        contentColor = floatingToolbarFabContentColor(pureBlack = pureBlack),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription =
                contentDescription.ifEmpty {
                    stringResource(R.string.create_playlist)
                },
        )
    }
}

@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer
}

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer
}

@Composable
private fun floatingToolbarMenuIconContainerColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun floatingToolbarMenuIconContentColor(pureBlack: Boolean): Color {
    return if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
}