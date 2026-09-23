package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        // Fast manual string splitting instead of String.lines() which uses regex internally
        val lyrics = ArrayList<LyricLine>(256) // ⚡ Bolt: Preallocate capacity
        
        var start = 0
        val lrcLength = lrcContent.length

        while (start < lrcLength) {
            var end = start
            while (end < lrcLength && lrcContent[end] != '\n' && lrcContent[end] != '\r') {
                end++
            }

            // Process line from `start` to `end`
            var lineStart = start
            // Trim leading whitespaces
            while (lineStart < end && lrcContent[lineStart].isWhitespace()) {
                lineStart++
            }

            val lineLength = end - lineStart
            if (lineLength >= 10 && lrcContent[lineStart] == '[') {
                var i = lineStart + 1
                var min = 0L
                while (i < end && lrcContent[i] in '0'..'9') {
                    min = min * 10 + (lrcContent[i] - '0')
                    i++
                }
                if (i < end && lrcContent[i] == ':') {
                    i++
                    var sec = 0L
                    while (i < end && lrcContent[i] in '0'..'9') {
                        sec = sec * 10 + (lrcContent[i] - '0')
                        i++
                    }
                    if (i < end && lrcContent[i] == '.') {
                        i++
                        var ms = 0L
                        var msDigits = 0
                        while (i < end && lrcContent[i] in '0'..'9') {
                            ms = ms * 10 + (lrcContent[i] - '0')
                            msDigits++
                            i++
                        }
                        if (i < end && lrcContent[i] == ']') {
                            // Adjust ms to always be based on 3 digits (milliseconds)
                            if (msDigits == 2) ms *= 10
                            else if (msDigits == 1) ms *= 100

                            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                            i++

                            // trim start
                            while (i < end && lrcContent[i].isWhitespace()) {
                                i++
                            }

                            // trim end
                            var textEnd = end - 1
                            while (textEnd >= i && lrcContent[textEnd].isWhitespace()) {
                                textEnd--
                            }

                            if (textEnd >= i) {
                                val text = lrcContent.substring(i, textEnd + 1)
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                        }
                    }
                }
            }

            start = end
            // Skip newline chars
            while (start < lrcLength && (lrcContent[start] == '\n' || lrcContent[start] == '\r')) {
                start++
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
