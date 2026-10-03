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
            val startIndex = line.indexOf('[')
            if (startIndex == -1) continue

            if (line.length >= startIndex + 10 && line[startIndex + 3] == ':' && line[startIndex + 6] == '.') {
                val m1 = line[startIndex + 1] - '0'
                val m2 = line[startIndex + 2] - '0'
                val s1 = line[startIndex + 4] - '0'
                val s2 = line[startIndex + 5] - '0'
                
                if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                    val min = m1 * 10L + m2
                    val sec = s1 * 10L + s2

                    var ms: Long = 0
                    var endIndex = -1
                    if (line[startIndex + 9] == ']') {
                        val ms1 = line[startIndex + 7] - '0'
                        val ms2 = line[startIndex + 8] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9) {
                            ms = (ms1 * 10L + ms2) * 10L
                            endIndex = startIndex + 9
                        }
                    } else if (line.length >= startIndex + 11 && line[startIndex + 10] == ']') {
                        val ms1 = line[startIndex + 7] - '0'
                        val ms2 = line[startIndex + 8] - '0'
                        val ms3 = line[startIndex + 9] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                            ms = ms1 * 100L + ms2 * 10L + ms3
                            endIndex = startIndex + 10
                        }
                    }

                    if (endIndex != -1) {
                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                        val text = line.substring(endIndex + 1).trim()
                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                    }
                }
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
