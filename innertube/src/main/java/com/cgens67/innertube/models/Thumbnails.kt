package com.cgens67.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Thumbnails(
    val thumbnails: List<Thumbnail>,
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int?,
    val height: Int?,
)

fun Thumbnails.isNotSquare(): Boolean {
    val last = thumbnails.lastOrNull() ?: return false
    val w = last.width?.toDouble() ?: return false
    val h = last.height?.toDouble() ?: return false
    if (w <= 0 || h <= 0) return false
    return w / h !in 0.85..1.15
}