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
        
        for (line in lines) {
            val startIndex = line.indexOf('[')
            if (startIndex != -1 && line.length >= startIndex + 10) {
                val s = startIndex
                if (line[s+3] == ':' && line[s+6] == '.') {
                    try {
                        val min = (line[s+1] - '0') * 10 + (line[s+2] - '0')
                        val sec = (line[s+4] - '0') * 10 + (line[s+5] - '0')

                        var ms = 0
                        var bracketEnd = -1

                        if (line[s+9] == ']') {
                            ms = ((line[s+7] - '0') * 10 + (line[s+8] - '0')) * 10
                            bracketEnd = s+9
                        } else if (line.length >= s + 11 && line[s+10] == ']') {
                            ms = (line[s+7] - '0') * 100 + (line[s+8] - '0') * 10 + (line[s+9] - '0')
                            bracketEnd = s+10
                        }

                        if (bracketEnd != -1) {
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                            var textStart = bracketEnd + 1
                            val len = line.length
                            while (textStart < len && line[textStart] <= ' ') {
                                textStart++
                            }

                            var textEnd = len - 1
                            while (textEnd >= textStart && line[textEnd] <= ' ') {
                                textEnd--
                            }

                            if (textStart <= textEnd) {
                                lyrics.add(LyricLine(timeInMs, line.substring(textStart, textEnd + 1)))
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
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
