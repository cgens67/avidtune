// app/src/main/java/com/arturo254/opentune/lyrics/LyricsEntry.kt
package com.arturo254.opentune.lyrics

data class WordTimestamp(
    val text: String,
    val startTime: Double,
    val endTime: Double,
    val hasTrailingSpace: Boolean = true
)

data class LyricsEntry(
    val time: Long,
    val text: String,
    val words: List<WordTimestamp>? = null,
    val isBackground: Boolean = false,
    val agent: String? = null
) : Comparable<LyricsEntry> {
    override fun compareTo(other: LyricsEntry): Int = (time - other.time).toInt()

    companion object {
        val HEAD_LYRICS_ENTRY = LyricsEntry(0L, "")
    }
}