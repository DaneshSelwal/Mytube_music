package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    /**
     * Optimized implementation using manual string parsing instead of Regex to significantly
     * reduce processing time and memory overhead. Also pre-allocates collection capacity.
     */
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            var startIndex = line.indexOf('[')
            var found = false
            while (startIndex != -1 && !found) {
                if (startIndex + 9 < line.length) {
                    val m10 = line[startIndex + 1] - '0'
                    val m1 = line[startIndex + 2] - '0'
                    val colon = line[startIndex + 3]
                    val s10 = line[startIndex + 4] - '0'
                    val s1 = line[startIndex + 5] - '0'
                    val dot = line[startIndex + 6]

                    if (colon == ':' && dot == '.' &&
                        m10 in 0..9 && m1 in 0..9 && s10 in 0..9 && s1 in 0..9) {

                        var endBracket = -1
                        var ms = 0L

                        if (line[startIndex + 9] == ']') {
                            val ms10 = line[startIndex + 7] - '0'
                            val ms1 = line[startIndex + 8] - '0'
                            if (ms10 in 0..9 && ms1 in 0..9) {
                                ms = (ms10 * 10 + ms1) * 10L
                                endBracket = startIndex + 9
                            }
                        } else if (startIndex + 10 < line.length && line[startIndex + 10] == ']') {
                            val ms100 = line[startIndex + 7] - '0'
                            val ms10 = line[startIndex + 8] - '0'
                            val ms1 = line[startIndex + 9] - '0'
                            if (ms100 in 0..9 && ms10 in 0..9 && ms1 in 0..9) {
                                ms = (ms100 * 100 + ms10 * 10 + ms1).toLong()
                                endBracket = startIndex + 10
                            }
                        }

                        if (endBracket != -1) {
                            val min = m10 * 10 + m1
                            val sec = s10 * 10 + s1
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                            val text = line.substring(endBracket + 1).trim()
                            if (text.isNotEmpty()) {
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                            found = true
                            break
                        }
                    }
                }
                startIndex = line.indexOf('[', startIndex + 1)
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
