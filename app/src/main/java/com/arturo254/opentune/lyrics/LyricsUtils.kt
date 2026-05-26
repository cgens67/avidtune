package com.arturo254.opentune.lyrics

import android.text.format.DateUtils

object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()
    val WORD_TIMING_REGEX = "^<(.+)>$".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        val lines = lyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
        
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val matchResult = LINE_REGEX.matchEntire(line)
            
            if (matchResult != null) {
                val times = matchResult.groupValues[1]
                val textRaw = matchResult.groupValues[3]
                
                var isBackground = false
                var agent: String? = null
                var text = textRaw
                
                if (text.startsWith("{bg}")) {
                    isBackground = true
                    text = text.removePrefix("{bg}")
                }
                if (text.startsWith("{agent:")) {
                    val endIdx = text.indexOf("}")
                    if (endIdx != -1) {
                        agent = text.substring(7, endIdx)
                        text = text.substring(endIdx + 1)
                    }
                }
                
                var words: List<WordTimestamp>? = null
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    val wordMatch = WORD_TIMING_REGEX.matchEntire(nextLine)
                    if (wordMatch != null) {
                        val wordData = wordMatch.groupValues[1]
                        words = wordData.split("|").mapNotNull { wordToken ->
                            val parts = wordToken.split(":")
                            if (parts.size >= 3) {
                                val wordText = parts.dropLast(2).joinToString(":")
                                val start = parts[parts.size - 2].toDoubleOrNull() ?: 0.0
                                val end = parts[parts.size - 1].toDoubleOrNull() ?: 0.0
                                WordTimestamp(wordText, start, end)
                            } else null
                        }
                        i++ // Skip word timing line
                    }
                }

                val timeMatchResults = TIME_REGEX.findAll(times)
                for (timeMatchResult in timeMatchResults) {
                    val min = timeMatchResult.groupValues[1].toLong()
                    val sec = timeMatchResult.groupValues[2].toLong()
                    val milString = timeMatchResult.groupValues[3]
                    var mil = milString.toLong()
                    if (milString.length == 2) {
                        mil *= 10
                    }
                    val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                    result.add(LyricsEntry(time, text, words, isBackground, agent))
                }
            }
            i++
        }
        return result.sorted()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + com.arturo254.opentune.ui.component.ANIMATE_SCROLL_DURATION) {
                return index - 1
            }
        }
        return lines.lastIndex
    }
}
