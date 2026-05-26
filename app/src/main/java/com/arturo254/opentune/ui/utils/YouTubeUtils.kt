package com.arturo254.opentune.ui.utils

fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    var result = this

    val wMatch = "([=-])w(\\d+)".toRegex().find(result)
    val hMatch = "([=-])h(\\d+)".toRegex().find(result)

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

        result = result.replace(wMatch.value, "${wMatch.groupValues[1]}w$w")
        result = result.replace(hMatch.value, "${hMatch.groupValues[1]}h$h")
        return result
    }

    val sMatch = "([=-])s(\\d+)".toRegex().find(result)
    if (sMatch != null) {
        return result.replace(sMatch.value, "${sMatch.groupValues[1]}s${width ?: height}")
    }

    return result
}
