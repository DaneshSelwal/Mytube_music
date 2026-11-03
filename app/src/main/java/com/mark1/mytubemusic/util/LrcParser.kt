package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate list capacity to avoid array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            val bracketIdx = line.indexOf('[')
            if (bracketIdx == -1) continue

            // ⚡ Bolt: Use manual index checks and primitive math instead of Regex for significant performance gains
            if (line.length >= bracketIdx + 10 && line[bracketIdx + 3] == ':' && line[bracketIdx + 6] == '.') {
                try {
                    val min = (line[bracketIdx + 1] - '0') * 10 + (line[bracketIdx + 2] - '0')
                    val sec = (line[bracketIdx + 4] - '0') * 10 + (line[bracketIdx + 5] - '0')

                    val closeBracketIdx = line.indexOf(']', bracketIdx + 7)
                    if (closeBracketIdx == -1) continue

                    val ms: Int
                    if (closeBracketIdx == bracketIdx + 9) { // [mm:ss.xx]
                        ms = (line[bracketIdx + 7] - '0') * 100 + (line[bracketIdx + 8] - '0') * 10
                    } else if (closeBracketIdx == bracketIdx + 10) { // [mm:ss.xxx]
                        ms = (line[bracketIdx + 7] - '0') * 100 + (line[bracketIdx + 8] - '0') * 10 + (line[bracketIdx + 9] - '0')
                    } else {
                        continue
                    }

                    val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                    // ⚡ Bolt: Optimize string extraction to prevent allocations
                    var textStart = closeBracketIdx + 1
                    while (textStart < line.length && line[textStart].isWhitespace()) {
                        textStart++
                    }
                    var textEnd = line.length - 1
                    while (textEnd >= textStart && line[textEnd].isWhitespace()) {
                        textEnd--
                    }

                    if (textStart <= textEnd) {
                        val text = line.substring(textStart, textEnd + 1)
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                } catch (e: Exception) {
                    // Ignore malformed lines
                }
            }
        }
        // ⚡ Bolt: Use in-place sortBy to avoid creating a new list
        lyrics.sortBy { it.startTimeMs }
        return lyrics
    }
    
    fun getLyricsFromFile(path: String): List<LyricLine> {
        val file = File(path)
        if (file.exists()) {
            return parse(file.readText())
        }
        return emptyList()
    }
}
