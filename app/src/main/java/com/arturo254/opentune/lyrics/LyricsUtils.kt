package com.arturo254.opentune.lyrics

import android.text.format.DateUtils

object LyricsUtils {
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()
    val TAG_REGEX = "\\{(bg|agent:([^}]+))\\}".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val lines = lyrics.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<LyricsEntry>()
        
        for (line in lines) {
            if (line.startsWith("<") && line.endsWith(">") && result.isNotEmpty()) {
                // It's a word timing line, append to previous LyricsEntry
                val wordsData = line.substring(1, line.length - 1).split("|")
                val words = wordsData.mapNotNull { wordData ->
                    val parts = wordData.split(":")
                    if (parts.size >= 3) {
                        val text = parts.dropLast(2).joinToString(":")
                        val start = parts[parts.size - 2].toDoubleOrNull() ?: 0.0
                        val end = parts[parts.size - 1].toDoubleOrNull() ?: 0.0
                        WordTimestamp(text, start, end, true) // space handled below
                    } else null
                }
                
                val lastEntry = result.removeLast()
                val fixedWords = words.mapIndexed { index, w -> 
                    w.copy(hasTrailingSpace = index < words.lastIndex)
                }
                result.add(lastEntry.copy(words = fixedWords))
            } else {
                val timeMatchResults = TIME_REGEX.findAll(line)
                var text = line.replace(TIME_REGEX, "").trim()
                
                var isBackground = false
                var agent: String? = null
                
                val tags = TAG_REGEX.findAll(text)
                for (tag in tags) {
                    if (tag.groupValues[1] == "bg") isBackground = true
                    else if (tag.groupValues[1].startsWith("agent:")) {
                        agent = tag.groupValues[2]
                    }
                }
                text = text.replace(TAG_REGEX, "").trim()
                
                timeMatchResults.forEach { timeMatchResult ->
                    val min = timeMatchResult.groupValues[1].toLong()
                    val sec = timeMatchResult.groupValues[2].toLong()
                    val milString = timeMatchResult.groupValues[3]
                    var mil = milString.toLong()
                    if (milString.length == 2) mil *= 10
                    val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                    
                    result.add(LyricsEntry(time, text, emptyList(), isBackground, agent))
                }
            }
        }
        return result.sorted()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        val ANIMATE_SCROLL_DURATION = 300L
        for (index in lines.indices) {
            if (lines[index].time >= position + ANIMATE_SCROLL_DURATION) {
                return index - 1
            }
        }
        return lines.lastIndex
    }
}