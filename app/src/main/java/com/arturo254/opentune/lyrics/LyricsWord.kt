package com.arturo254.opentune.lyrics

data class LyricsWord(
    val text: String,
    val startTime: Long, // in ms
    val endTime: Long,   // in ms
)