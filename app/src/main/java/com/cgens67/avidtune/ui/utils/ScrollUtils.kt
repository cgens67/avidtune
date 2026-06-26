package com.cgens67.avidtune.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var isScrollingUp by remember { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousIndex = firstVisibleItemIndex
        var previousScrollOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (previousIndex != index) {
                    isScrollingUp = previousIndex > index
                } else if (previousScrollOffset != offset) {
                    isScrollingUp = previousScrollOffset > offset
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }
    return isScrollingUp
}

@Composable
fun LazyGridState.isScrollingUp(): Boolean {
    var isScrollingUp by remember { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousIndex = firstVisibleItemIndex
        var previousScrollOffset = firstVisibleItemScrollOffset
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (previousIndex != index) {
                    isScrollingUp = previousIndex > index
                } else if (previousScrollOffset != offset) {
                    isScrollingUp = previousScrollOffset > offset
                }
                previousIndex = index
                previousScrollOffset = offset
            }
    }
    return isScrollingUp
}

@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var isScrollingUp by remember { mutableStateOf(true) }
    LaunchedEffect(this) {
        var previousScrollOffset = value
        snapshotFlow { value }
            .collect { offset ->
                if (previousScrollOffset != offset) {
                    isScrollingUp = previousScrollOffset > offset
                }
                previousScrollOffset = offset
            }
    }
    return isScrollingUp
}
