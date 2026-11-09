package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    // ⚡ Bolt: Performance Optimization
    // Avoids Regex, substring(), and intermediate allocations to parse highly predictable LRC timestamp format.
    // Uses manual character parsing with index checks, primitive arithmetic, and pre-allocated capacity.
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            val startIdx = line.indexOf('[')
            if (startIdx == -1) continue

            // Expected format: [MM:SS.xx] or [MM:SS.xxx]
            // Minimum length check (e.g. "[00:00.00]" is 10 chars)
            if (line.length < startIdx + 10) continue

            if (line[startIdx + 3] != ':' || line[startIdx + 6] != '.') continue

            val m1 = line[startIdx + 1]
            val m2 = line[startIdx + 2]
            val s1 = line[startIdx + 4]
            val s2 = line[startIdx + 5]

            if (!m1.isDigit() || !m2.isDigit() || !s1.isDigit() || !s2.isDigit()) continue

            val min = (m1 - '0') * 10L + (m2 - '0')
            val sec = (s1 - '0') * 10L + (s2 - '0')

            val ms: Long
            val endIdx: Int

            if (line.length > startIdx + 9 && line[startIdx + 9] == ']') {
                // [MM:SS.xx]
                val ms1 = line[startIdx + 7]
                val ms2 = line[startIdx + 8]
                if (!ms1.isDigit() || !ms2.isDigit()) continue
                ms = (ms1 - '0') * 100L + (ms2 - '0') * 10L
                endIdx = startIdx + 9
            } else if (line.length > startIdx + 10 && line[startIdx + 10] == ']') {
                // [MM:SS.xxx]
                val ms1 = line[startIdx + 7]
                val ms2 = line[startIdx + 8]
                val ms3 = line[startIdx + 9]
                if (!ms1.isDigit() || !ms2.isDigit() || !ms3.isDigit()) continue
                ms = (ms1 - '0') * 100L + (ms2 - '0') * 10L + (ms3 - '0')
                endIdx = startIdx + 10
            } else {
                continue
            }

            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
            val text = line.substring(endIdx + 1).trim()

            if (text.isNotEmpty()) {
                lyrics.add(LyricLine(timeInMs, text))
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
