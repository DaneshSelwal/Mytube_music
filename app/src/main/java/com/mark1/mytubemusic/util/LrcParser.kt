package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate capacity to prevent backing array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        // ⚡ Bolt: Replaced Regex with manual string parsing (indexOf and primitive math)
        // to avoid expensive Regex engine overhead and excessive String allocations
        for (line in lines) {
            var searchStart = 0
            while (searchStart < line.length) {
                val startIdx = line.indexOf('[', searchStart)
                if (startIdx == -1) break
                
                val endIdx = line.indexOf(']', startIdx)
                if (endIdx == -1) break

                val len = endIdx - startIdx - 1
                if ((len == 8 || len == 9) && line[startIdx + 3] == ':' && line[startIdx + 6] == '.') {
                    val m1 = line[startIdx + 1] - '0'
                    val m2 = line[startIdx + 2] - '0'
                    val s1 = line[startIdx + 4] - '0'
                    val s2 = line[startIdx + 5] - '0'
                    val ms1 = line[startIdx + 7] - '0'
                    val ms2 = line[startIdx + 8] - '0'

                    if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9 && ms1 in 0..9 && ms2 in 0..9) {
                        var isValid = true
                        var ms = ms1 * 100L + ms2 * 10L
                        if (len == 9) {
                            val ms3 = line[startIdx + 9] - '0'
                            if (ms3 in 0..9) {
                                ms += ms3
                            } else {
                                isValid = false
                            }
                        }

                        if (isValid) {
                            val min = m1 * 10L + m2
                            val sec = s1 * 10L + s2
                            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                            var textStart = endIdx + 1
                            while (textStart < line.length && line[textStart].isWhitespace()) {
                                textStart++
                            }
                            var textEnd = line.length - 1
                            while (textEnd >= textStart && line[textEnd].isWhitespace()) {
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

                searchStart = startIdx + 1
            }
        }
        return lyrics.apply { sortBy { it.startTimeMs } }
    }
    
    fun getLyricsFromFile(path: String): List<LyricLine> {
        val file = File(path)
        if (file.exists()) {
            return parse(file.readText())
        }
        return emptyList()
    }
}
