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
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (i in 0 until lines.size) {
            val line = lines[i]
            val len = line.length
            var parsed = false

            // Fast path: predictable format [mm:ss.xx] or [mm:ss.xxx]
            if (len >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                try {
                    val min = (line[1] - '0') * 10 + (line[2] - '0')
                    val sec = (line[4] - '0') * 10 + (line[5] - '0')

                    var ms = 0
                    var endIndex = 0

                    if (line[9] == ']') { // centiseconds
                        ms = (line[7] - '0') * 100 + (line[8] - '0') * 10
                        endIndex = 10
                    } else if (len > 10 && line[10] == ']') { // milliseconds
                        ms = (line[7] - '0') * 100 + (line[8] - '0') * 10 + (line[9] - '0')
                        endIndex = 11
                    }

                    if (endIndex > 0) {
                        val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                        var textStartIndex = endIndex
                        while (textStartIndex < len && line[textStartIndex].isWhitespace()) {
                            textStartIndex++
                        }

                        if (textStartIndex < len) {
                            val text = line.substring(textStartIndex).trimEnd()
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                        parsed = true
                    }
                } catch (e: Exception) {
                    // Fall through to regex
                }
            }

            // Slow path: fallback to regex for unusual formats
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
