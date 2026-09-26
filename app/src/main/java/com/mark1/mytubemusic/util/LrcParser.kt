package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    // ⚡ Bolt: Removed Regex parsing in favor of manual index character parsing
    // to significantly reduce processing time and GC overhead during playback.
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = java.util.ArrayList<LyricLine>(lines.size) // Pre-allocate size
        
        for (line in lines) {
            var start = line.indexOf('[')
            while (start != -1 && start + 9 < line.length) {
                if (line[start + 3] == ':' && line[start + 6] == '.') {
                    val min1 = line[start + 1] - '0'
                    val min2 = line[start + 2] - '0'
                    val sec1 = line[start + 4] - '0'
                    val sec2 = line[start + 5] - '0'

                    if (min1 in 0..9 && min2 in 0..9 && sec1 in 0..9 && sec2 in 0..9) {
                        val min = min1 * 10 + min2
                        val sec = sec1 * 10 + sec2

                        var ms = -1
                        var textStart = 0

                        if (line[start + 9] == ']') {
                            val ms1 = line[start + 7] - '0'
                            val ms2 = line[start + 8] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9) {
                                ms = (ms1 * 10 + ms2) * 10
                                textStart = start + 10
                            }
                        } else if (start + 10 < line.length && line[start + 10] == ']') {
                            val ms1 = line[start + 7] - '0'
                            val ms2 = line[start + 8] - '0'
                            val ms3 = line[start + 9] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                                ms = ms1 * 100 + ms2 * 10 + ms3
                                textStart = start + 11
                            }
                        }

                        if (ms != -1) {
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                            var isTextEmpty = true
                            for (i in textStart until line.length) {
                                if (!line[i].isWhitespace()) {
                                    isTextEmpty = false
                                    break
                                }
                            }

                            if (!isTextEmpty) {
                                val text = line.substring(textStart).trim()
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                            break
                        }
                    }
                }
                start = line.indexOf('[', start + 1)
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
