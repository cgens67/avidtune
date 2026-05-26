package com.arturo254.opentune.lyrics

import android.text.format.DateUtils
import com.arturo254.opentune.ui.component.ANIMATE_SCROLL_DURATION

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.*)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val lines = lyrics.lines().map { it.trim() }
        val entries = mutableListOf<LyricsEntry>()
        
        var currentEntry: LyricsEntry? = null
        
        for (line in lines) {
            if (line.isEmpty()) continue
            
            if (line.startsWith("<") && line.endsWith(">") && currentEntry != null) {
                // Parse word-level timings
                val wordsData = line.substring(1, line.length - 1)
                val wordTokens = wordsData.split("|")
                val words = wordTokens.mapNotNull { token ->
                    val parts = token.split(":")
                    if (parts.size >= 3) {
                        val wordText = parts[0]
                        val start = parts[1].toDoubleOrNull() ?: 0.0
                        val end = parts[2].toDoubleOrNull() ?: 0.0
                        WordTimestamp(wordText, start, end, true)
                    } else null
                }
                if (words.isNotEmpty()) {
                    // Update previous entry with word data
                    entries.removeAt(entries.lastIndex)
                    val updated = currentEntry!!.copy(words = words)
                    entries.add(updated)
                    currentEntry = updated
                }
            } else {
                val matchResult = LINE_REGEX.matchEntire(line)
                if (matchResult != null) {
                    val times = matchResult.groupValues[1]
                    val rawText = matchResult.groupValues[3]
                    
                    val isBackground = rawText.startsWith("{bg}")
                    var text = if (isBackground) rawText.substring(4) else rawText
                    
                    var agent: String? = null
                    if (text.startsWith("{agent:")) {
                        val endIdx = text.indexOf("}")
                        if (endIdx != -1) {
                            agent = text.substring(7, endIdx)
                            text = text.substring(endIdx + 1)
                        }
                    }
                    
                    val timeMatchResults = TIME_REGEX.findAll(times)
                    timeMatchResults.forEach { timeMatchResult ->
                        val min = timeMatchResult.groupValues[1].toLong()
                        val sec = timeMatchResult.groupValues[2].toLong()
                        val milString = timeMatchResult.groupValues[3]
                        var mil = milString.toLong()
                        if (milString.length == 2) mil *= 10
                        
                        val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                        val entry = LyricsEntry(time, text, null, isBackground, agent)
                        entries.add(entry)
                        currentEntry = entry
                    }
                }
            }
        }
        return entries.sorted()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        for (index in lines.indices) {
            if (lines[index].time >= position + ANIMATE_SCROLL_DURATION) {
                return index - 1
            }
        }
        return lines.lastIndex
    }
}
