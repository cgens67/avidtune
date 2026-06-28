package com.cgens67.avidtune.ui.utils

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay(),
): TopAppBarScrollBehavior =
    AppBarScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll,
    )

@ExperimentalMaterial3Api
class AppBarScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true },
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = false
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!canScroll()) return Offset.Zero
                
                // If the limit hasn't been set or if the app bar is pinned (limit >= 0f), 
                // do not consume any scroll. Let the list scroll normally.
                if (state.heightOffsetLimit == -Float.MAX_VALUE || state.heightOffsetLimit >= 0f) {
                    return Offset.Zero
                }
                
                val prevHeightOffset = state.heightOffset
                state.heightOffset += available.y
                return if (prevHeightOffset != state.heightOffset) {
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y
                
                if (state.heightOffsetLimit != -Float.MAX_VALUE && state.heightOffsetLimit < 0f) {
                    if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) {
                        if (consumed.y == 0f && available.y > 0f) {
                            state.contentOffset = 0f
                        }
                    }
                    state.heightOffset += consumed.y
                }
                return Offset.Zero
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
suspend fun TopAppBarState.resetHeightOffset() {
    if (heightOffset != 0f) {
        animate(
            initialValue = heightOffset,
            targetValue = 0f,
        ) { value, _ ->
            heightOffset = value
        }
    }
}
