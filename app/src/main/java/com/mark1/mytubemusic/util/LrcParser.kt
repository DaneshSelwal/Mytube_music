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
            // Check for [mm:ss.xx] or [mm:ss.xxx]
            if (line.length >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val min0 = line[1] - '0'
                val min1 = line[2] - '0'
                val sec0 = line[4] - '0'
                val sec1 = line[5] - '0'
                
                if (min0 !in 0..9 || min1 !in 0..9 || sec0 !in 0..9 || sec1 !in 0..9) {
                    continue
                }

                val min = min0 * 10L + min1
                val sec = sec0 * 10L + sec1

                var ms = 0L
                var endBracketIndex = -1

                if (line[9] == ']') {
                    val ms0 = line[7] - '0'
                    val ms1 = line[8] - '0'
                    if (ms0 in 0..9 && ms1 in 0..9) {
                        ms = ms0 * 100L + ms1 * 10L
                        endBracketIndex = 9
                    }
                } else if (line.length > 10 && line[10] == ']') {
                    val ms0 = line[7] - '0'
                    val ms1 = line[8] - '0'
                    val ms2 = line[9] - '0'
                    if (ms0 in 0..9 && ms1 in 0..9 && ms2 in 0..9) {
                        ms = ms0 * 100L + ms1 * 10L + ms2
                        endBracketIndex = 10
                    }
                }

                if (endBracketIndex != -1) {
                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                    val text = line.substring(endBracketIndex + 1).trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
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
