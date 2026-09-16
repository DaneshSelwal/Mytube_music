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
        
        // Optimizing by manually parsing indices to avoid expensive Regex engine in the tight loop
        for (line in lines) {
            val len = line.length
            if (len >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val min1 = line[1] - '0'
                val min2 = line[2] - '0'
                val sec1 = line[4] - '0'
                val sec2 = line[5] - '0'
                
                if (min1 in 0..9 && min2 in 0..9 && sec1 in 0..9 && sec2 in 0..9) {
                    val min = min1 * 10 + min2
                    val sec = sec1 * 10 + sec2

                    var ms = 0
                    var textStartIndex = 0

                    if (line[9] == ']') {
                        val ms1 = line[7] - '0'
                        val ms2 = line[8] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9) {
                            ms = ms1 * 100 + ms2 * 10
                            textStartIndex = 10
                        }
                    } else if (len >= 11 && line[10] == ']') {
                        val ms1 = line[7] - '0'
                        val ms2 = line[8] - '0'
                        val ms3 = line[9] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                            ms = ms1 * 100 + ms2 * 10 + ms3
                            textStartIndex = 11
                        }
                    }

                    if (textStartIndex > 0) {
                        val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                        var start = textStartIndex
                        while (start < len && line[start].isWhitespace()) {
                            start++
                        }
                        var end = len - 1
                        while (end >= start && line[end].isWhitespace()) {
                            end--
                        }
                        if (start <= end) {
                            lyrics.add(LyricLine(timeInMs, line.substring(start, end + 1)))
                        }
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
