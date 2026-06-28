package com.cgens67.avidtune.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this

    // We use .findAll(...).lastOrNull() instead of .find(...) to avoid matching base64 video/playlist
    // IDs that accidentally contain "-w", "-h", or "-s" followed by digits (e.g., Hot Pink album cover).
    val wMatch = "([=-])w(\\d+)".toRegex().findAll(this).lastOrNull()
    val hMatch = "([=-])h(\\d+)".toRegex().findAll(this).lastOrNull()

    if (wMatch != null && hMatch != null) {
        val W = wMatch.groupValues[2].toIntOrNull() ?: 0
        val H = hMatch.groupValues[2].toIntOrNull() ?: 0
        var w = width
        var h = height
        if (W != 0 && H != 0) {
            if (w != null && h == null) h = (w.toFloat() / W * H).toInt()
            if (w == null && h != null) w = (h.toFloat() / H * W).toInt()
        } else {
            if (w == null) w = height
            if (h == null) h = width
        }

        var result = this
        // Replace only the last occurrence (the actual parameter) to prevent corrupting the URL hash
        result = result.replaceLast(wMatch.value, "${wMatch.groupValues[1]}w$w")
        result = result.replaceLast(hMatch.value, "${hMatch.groupValues[1]}h$h")
        return result
    }

    val sMatch = "([=-])s(\\d+)".toRegex().findAll(this).lastOrNull()
    if (sMatch != null) {
        return this.replaceLast(sMatch.value, "${sMatch.groupValues[1]}s${width ?: height}")
    }

    return this
}

private fun String.replaceLast(oldValue: String, newValue: String): String {
    val lastIndex = this.lastIndexOf(oldValue)
    if (lastIndex == -1) return this
    return this.substring(0, lastIndex) + newValue + this.substring(lastIndex + oldValue.length)
}
