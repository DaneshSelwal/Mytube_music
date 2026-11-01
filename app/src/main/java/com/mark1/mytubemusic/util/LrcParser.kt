package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate capacity to avoid array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            var start = 0
            while (true) {
                // ⚡ Bolt: Fast character index checks to avoid expensive Regex compilation and allocations
                start = line.indexOf('[', start)
                if (start == -1 || start >= line.length - 8) break
                
                if (line[start + 3] == ':' && line[start + 6] == '.') {
                    val m1 = line[start + 1] - '0'
                    val m2 = line[start + 2] - '0'
                    val s1 = line[start + 4] - '0'
                    val s2 = line[start + 5] - '0'

                    if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                        val min = m1 * 10 + m2
                        val sec = s1 * 10 + s2

                        var ms = -1
                        var endBracket = -1

                        if (line.length > start + 9 && line[start + 9] == ']') {
                            val ms1 = line[start + 7] - '0'
                            val ms2 = line[start + 8] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9) {
                                ms = ms1 * 100 + ms2 * 10
                                endBracket = start + 9
                            }
                        } else if (line.length > start + 10 && line[start + 10] == ']') {
                            val ms1 = line[start + 7] - '0'
                            val ms2 = line[start + 8] - '0'
                            val ms3 = line[start + 9] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                                ms = ms1 * 100 + ms2 * 10 + ms3
                                endBracket = start + 10
                            }
                        }

                        if (endBracket != -1) {
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                            // ⚡ Bolt: Manually walk over whitespace to extract string without allocating a new string with trim()
                            var textStart = endBracket + 1
                            while(textStart < line.length && line[textStart].isWhitespace()) {
                                textStart++
                            }
                            var textEnd = line.length - 1
                            while(textEnd >= textStart && line[textEnd].isWhitespace()) {
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
                start++
            }
        }
        // ⚡ Bolt: In-place sort to prevent unnecessary intermediate collection allocations
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
