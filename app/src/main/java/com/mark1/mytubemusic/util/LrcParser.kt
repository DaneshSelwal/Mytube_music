package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()

        // ⚡ Bolt: Pre-allocate list capacity to avoid dynamic array reallocations during parsing
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            val length = line.length

            // ⚡ Bolt: Replace Regex with manual character parsing to avoid string allocations and pattern compilation overhead
            // LRC format: [mm:ss.xx] or [mm:ss.xxx]
            if (length >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                try {
                    val min = (line[1] - '0') * 10 + (line[2] - '0')
                    val sec = (line[4] - '0') * 10 + (line[5] - '0')
                    var ms = (line[7] - '0') * 100 + (line[8] - '0') * 10

                    val isThreeDigitMs = line[9] != ']'

                    val textStartIdx: Int
                    if (isThreeDigitMs && length >= 11 && line[10] == ']') {
                        ms += (line[9] - '0')
                        textStartIdx = 11
                    } else if (!isThreeDigitMs && line[9] == ']') {
                        textStartIdx = 10
                    } else {
                        continue
                    }

                    val timeMs = (min * 60000L) + (sec * 1000L) + ms

                    var start = textStartIdx
                    while (start < length && line[start] <= ' ') start++
                    var end = length - 1
                    while (end >= start && line[end] <= ' ') end--

                    if (start <= end) {
                        val text = line.substring(start, end + 1)
                        lyrics.add(LyricLine(timeMs, text))
                    }
                } catch (e: Exception) {
                    // Ignore malformed lines
                }
            }
        }

        return lyrics.sortedBy { it.startTimeMs }
    }
    
    fun getLyricsFromFile(path: String): List<LyricLine> {
        val file = File(path)
        if (file.exists()) {
            return parse(file.readText())
        }
        return emptyList()
    }
}
