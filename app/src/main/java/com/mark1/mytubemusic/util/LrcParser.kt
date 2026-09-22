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
            var startIndex = line.indexOf('[')
            while (startIndex != -1 && startIndex + 9 < line.length) {
                if (line[startIndex + 3] == ':' && line[startIndex + 6] == '.') {
                    val closePos = line.indexOf(']', startIndex + 7)
                    if (closePos != -1) {
                        val m1 = line[startIndex + 1]
                        val m2 = line[startIndex + 2]
                        val s1 = line[startIndex + 4]
                        val s2 = line[startIndex + 5]

                        if (m1 in '0'..'9' && m2 in '0'..'9' && s1 in '0'..'9' && s2 in '0'..'9') {
                            val msLen = closePos - (startIndex + 7)
                            var ms = -1L

                            if (msLen == 2) {
                                val ms1 = line[startIndex + 7]
                                val ms2 = line[startIndex + 8]
                                if (ms1 in '0'..'9' && ms2 in '0'..'9') {
                                    ms = (ms1 - '0') * 100L + (ms2 - '0') * 10L
                                }
                            } else if (msLen == 3) {
                                val ms1 = line[startIndex + 7]
                                val ms2 = line[startIndex + 8]
                                val ms3 = line[startIndex + 9]
                                if (ms1 in '0'..'9' && ms2 in '0'..'9' && ms3 in '0'..'9') {
                                    ms = (ms1 - '0') * 100L + (ms2 - '0') * 10L + (ms3 - '0').toLong()
                                }
                            }

                            if (ms != -1L) {
                                val min = (m1 - '0') * 10L + (m2 - '0')
                                val sec = (s1 - '0') * 10L + (s2 - '0')
                                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                                var textStart = closePos + 1
                                while (textStart < line.length && line[textStart] <= ' ') {
                                    textStart++
                                }
                                var textEnd = line.length - 1
                                while (textEnd >= textStart && line[textEnd] <= ' ') {
                                    textEnd--
                                }

                                if (textStart <= textEnd) {
                                    val text = line.substring(textStart, textEnd + 1)
                                    lyrics.add(LyricLine(timeInMs, text))
                                }
                                break
                            }
                        }
                    }
                }
                startIndex = line.indexOf('[', startIndex + 1)
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
