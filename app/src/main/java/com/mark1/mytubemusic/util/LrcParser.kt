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
            val trimLine = line.trim()
            if (trimLine.length >= 10 && trimLine[0] == '[' && trimLine[3] == ':' && trimLine[6] == '.') {
                val min1 = trimLine[1] - '0'
                val min2 = trimLine[2] - '0'
                val sec1 = trimLine[4] - '0'
                val sec2 = trimLine[5] - '0'

                if (min1 in 0..9 && min2 in 0..9 && sec1 in 0..9 && sec2 in 0..9) {
                    val min = min1 * 10 + min2
                    val sec = sec1 * 10 + sec2

                    var ms = 0L
                    var textStartIndex = -1

                    if (trimLine[9] == ']') {
                        // [mm:ss.xx]
                        val ms1 = trimLine[7] - '0'
                        val ms2 = trimLine[8] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9) {
                            ms = (ms1 * 10 + ms2) * 10L
                            textStartIndex = 10
                        }
                    } else if (trimLine.length > 10 && trimLine[10] == ']') {
                        // [mm:ss.xxx]
                        val ms1 = trimLine[7] - '0'
                        val ms2 = trimLine[8] - '0'
                        val ms3 = trimLine[9] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                            ms = (ms1 * 100 + ms2 * 10 + ms3).toLong()
                            textStartIndex = 11
                        }
                    }

                    if (textStartIndex != -1) {
                        val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                        val text = trimLine.substring(textStartIndex).trim()
                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
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
