package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size) // ⚡ Bolt: Pre-allocate capacity
        
        for (line in lines) {
            var startIdx = line.indexOf('[')
            while (startIdx != -1 && startIdx < line.length - 9) { // [mm:ss.xx] is at least 10 chars
                if (line[startIdx + 3] == ':' && line[startIdx + 6] == '.') {
                    val m1 = line[startIdx + 1] - '0'
                    val m2 = line[startIdx + 2] - '0'
                    val s1 = line[startIdx + 4] - '0'
                    val s2 = line[startIdx + 5] - '0'

                    if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                        val endIdx = line.indexOf(']', startIdx + 7)
                        if (endIdx != -1) {
                            val msLen = endIdx - (startIdx + 7)
                            if (msLen == 2 || msLen == 3) {
                                var ms = 0L
                                var validMs = true
                                for (i in startIdx + 7 until endIdx) {
                                    val digit = line[i] - '0'
                                    if (digit in 0..9) {
                                        ms = ms * 10 + digit
                                    } else {
                                        validMs = false
                                        break
                                    }
                                }

                                if (validMs) {
                                    if (msLen == 2) ms *= 10

                                    val min = m1 * 10L + m2
                                    val sec = s1 * 10L + s2
                                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                                    val text = line.substring(endIdx + 1).trim()
                                    if (text.isNotEmpty()) {
                                        lyrics.add(LyricLine(timeInMs, text))
                                    }
                                    break // Only process first valid timestamp per line to match original behavior
                                }
                            }
                        }
                    }
                }
                startIdx = line.indexOf('[', startIdx + 1)
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
