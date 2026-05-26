package com.arturo254.opentune.lyrics

import android.text.format.DateUtils
import com.arturo254.opentune.ui.component.ANIMATE_SCROLL_DURATION

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.+)".toRegex()
    val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val lines = lyrics.lines()
        val entries = mutableListOf<LyricsEntry>()
        var lastEntry: LyricsEntry? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("<") && trimmed.endsWith(">") && lastEntry != null) {
                val wordContent = trimmed.substring(1, trimmed.length - 1)
                val words = parseWordTimings(wordContent)
                if (words.isNotEmpty()) {
                    entries.removeAt(entries.lastIndex)
                    val updatedEntry = lastEntry.copy(words = words)
                    entries.add(updatedEntry)
                    lastEntry = updatedEntry
                }
            } else {
                val parsed = parseLine(line)
                if (parsed.isNotEmpty()) {
                    entries.addAll(parsed)
                    lastEntry = parsed.last()
                }
            }
        }
        return entries.sorted()
    }

    private fun parseWordTimings(content: String): List<LyricsWord> {
        return content.split('|').mapNotNull { segment ->
            val parts = segment.split(':')
            if (parts.size >= 3) {
                val text = parts[0]
                val startSec = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val endSec = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                LyricsWord(
                    text = text,
                    startTime = (startSec * 1000).toLong(),
                    endTime = (endSec * 1000).toLong()
                )
            } else null
        }
    }

    fun parseLine(line: String): List<LyricsEntry> {
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return emptyList()
        val times = matchResult.groupValues[1]
        val text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text)
            }.toList()
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