package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    private val timePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = mutableListOf<LyricLine>()
        
        for (line in lines) {
            var parsed = false
            // Fast path for standard format [mm:ss.SS] or [mm:ss.SSS]
            if (line.length > 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val closeBracketIndex = line.indexOf(']', 7)
                if (closeBracketIndex in 9..10) {
                    try {
                        // Avoid subString allocations for primitive types when possible
                        val min = (line[1] - '0') * 10 + (line[2] - '0')
                        val sec = (line[4] - '0') * 10 + (line[5] - '0')

                        var ms = 0L
                        if (closeBracketIndex == 9) { // 2 digits
                             ms = ((line[7] - '0') * 10 + (line[8] - '0')) * 10L
                        } else if (closeBracketIndex == 10) { // 3 digits
                             ms = ((line[7] - '0') * 100 + (line[8] - '0') * 10 + (line[9] - '0')).toLong()
                        }

                        if (min in 0..99 && sec in 0..59 && ms >= 0) {
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                            val text = line.substring(closeBracketIndex + 1).trim()
                            if (text.isNotEmpty()) {
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                            parsed = true
                        }
                    } catch (e: Exception) {
                        // Ignore and fallback
                    }
                }
            }

            if (!parsed) {
                val matchResult = timePattern.find(line)
                if (matchResult != null) {
                    val min = matchResult.groupValues[1].toLong()
                    val sec = matchResult.groupValues[2].toLong()
                    var msStr = matchResult.groupValues[3]
                    if (msStr.length == 2) msStr += "0" // handle centiseconds
                    val ms = msStr.toLong()

                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                    val text = line.substring(matchResult.range.last + 1).trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
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
