package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

private val timePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = mutableListOf<LyricLine>()
        
        for (line in lines) {
            var parsed = false
            // Fast path for standard LRC format: [mm:ss.xx] or [mm:ss.xxx]
            if (line.length >= 10 && line[0] == '[') {
                val closeBracketIndex = line.indexOf(']')
                if (closeBracketIndex in 9..10) {
                    if (line[3] == ':' && line[6] == '.') {
                        val m1 = line[1] - '0'
                        val m2 = line[2] - '0'
                        val s1 = line[4] - '0'
                        val s2 = line[5] - '0'

                        if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                            val min = m1 * 10L + m2
                            val sec = s1 * 10L + s2

                            val msLen = closeBracketIndex - 7
                            if (msLen == 2 || msLen == 3) {
                                val ms1 = line[7] - '0'
                                val ms2 = line[8] - '0'
                                if (ms1 in 0..9 && ms2 in 0..9) {
                                    var ms = ms1 * 100L + ms2 * 10L
                                    if (msLen == 3) {
                                        val ms3 = line[9] - '0'
                                        if (ms3 in 0..9) {
                                            ms = ms1 * 100L + ms2 * 10L + ms3
                                            parsed = true
                                        }
                                    } else {
                                        parsed = true
                                    }

                                    if (parsed) {
                                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                                        val text = line.substring(closeBracketIndex + 1).trim()
                                        if (text.isNotEmpty()) {
                                            lyrics.add(LyricLine(timeInMs, text))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Fallback for non-standard lines
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
