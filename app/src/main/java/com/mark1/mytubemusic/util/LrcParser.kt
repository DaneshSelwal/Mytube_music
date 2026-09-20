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
                try {
                    val m1 = trimLine[1] - '0'
                    val m2 = trimLine[2] - '0'
                    val s1 = trimLine[4] - '0'
                    val s2 = trimLine[5] - '0'

                    if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                        val min = m1 * 10 + m2
                        val sec = s1 * 10 + s2

                        var ms = 0
                        var endIndex = 0
                        if (trimLine.length >= 10 && trimLine[9] == ']') {
                            val ms1 = trimLine[7] - '0'
                            val ms2 = trimLine[8] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9) {
                                ms = (ms1 * 10 + ms2) * 10
                                endIndex = 9
                            }
                        } else if (trimLine.length >= 11 && trimLine[10] == ']') {
                            val ms1 = trimLine[7] - '0'
                            val ms2 = trimLine[8] - '0'
                            val ms3 = trimLine[9] - '0'
                            if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                                ms = ms1 * 100 + ms2 * 10 + ms3
                                endIndex = 10
                            }
                        }

                        if (endIndex > 0) {
                            val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                            val text = trimLine.substring(endIndex + 1).trim()
                            if (text.isNotEmpty()) {
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                        }
                    }
                } catch (e: Exception) {
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
