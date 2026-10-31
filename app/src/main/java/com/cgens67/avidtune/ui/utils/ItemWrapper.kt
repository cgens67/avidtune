package com.cgens67.avidtune.ui.utils

import androidx.compose.runtime.mutableStateOf

class ItemWrapper<T>(
    val item: T,
    initialSelected: Boolean = false,
) {
    private val _isSelected = mutableStateOf(initialSelected)

    var isSelected: Boolean
        get() = _isSelected.value
        set(value) {
            _isSelected.value = value
        }
}
