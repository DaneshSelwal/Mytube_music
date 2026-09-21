package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (i in lines.indices) {
            val line = lines[i]
            val len = line.length
            // Expected format: [mm:ss.xx] or [mm:ss.xxx]
            if (len >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                try {
                    val min = (line[1] - '0') * 10 + (line[2] - '0')
                    val sec = (line[4] - '0') * 10 + (line[5] - '0')

                    val closingBracketIndex: Int
                    val ms: Int

                    if (len > 9 && line[9] == ']') {
                        ms = (line[7] - '0') * 100 + (line[8] - '0') * 10
                        closingBracketIndex = 9
                    } else if (len > 10 && line[10] == ']') {
                        ms = (line[7] - '0') * 100 + (line[8] - '0') * 10 + (line[9] - '0')
                        closingBracketIndex = 10
                    } else {
                        continue
                    }

                    val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                    var startText = closingBracketIndex + 1
                    while (startText < len && line[startText] <= ' ') {
                        startText++
                    }

                    var endText = len - 1
                    while (endText >= startText && line[endText] <= ' ') {
                        endText--
                    }

                    if (startText <= endText) {
                        lyrics.add(LyricLine(timeInMs, line.substring(startText, endText + 1)))
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
