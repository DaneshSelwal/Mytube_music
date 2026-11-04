package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size) // ⚡ Bolt: Pre-allocate collection size to avoid array resizing
        
        for (line in lines) {
            // ⚡ Bolt: Use manual character extraction instead of Regex to significantly reduce string allocations and memory overhead
            val trimLine = line.trimStart() // Handle optional leading whitespaces
            if (trimLine.length > 10 && trimLine[0] == '[' && trimLine[3] == ':' && trimLine[6] == '.' && trimLine.indexOf(']') in 9..10) {
                try {
                    // Ensure digits are actually digits before computing
                    if (!trimLine[1].isDigit() || !trimLine[2].isDigit() || !trimLine[4].isDigit() || !trimLine[5].isDigit()) {
                        continue
                    }

                    val min = (trimLine[1] - '0') * 10 + (trimLine[2] - '0')
                    val sec = (trimLine[4] - '0') * 10 + (trimLine[5] - '0')

                    val bracketIndex = trimLine.indexOf(']')
                    var ms = 0
                    if (bracketIndex == 9) {
                        // [00:00.00] format
                        if (!trimLine[7].isDigit() || !trimLine[8].isDigit()) continue
                        ms = (trimLine[7] - '0') * 10 + (trimLine[8] - '0')
                        ms *= 10 // convert centiseconds to milliseconds
                    } else if (bracketIndex == 10) {
                        // [00:00.000] format
                        if (!trimLine[7].isDigit() || !trimLine[8].isDigit() || !trimLine[9].isDigit()) continue
                        ms = (trimLine[7] - '0') * 100 + (trimLine[8] - '0') * 10 + (trimLine[9] - '0')
                    }

                    val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                    val text = trimLine.substring(bracketIndex + 1).trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                } catch (e: Exception) {
                    // Ignore parse errors for malformed lines
                }
            }
        }
        lyrics.sortBy { it.startTimeMs } // ⚡ Bolt: Use in-place sorting to avoid creating an intermediate copy
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
