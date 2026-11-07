package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    // ⚡ Bolt: Replaced Regex string matching with manual character traversal to prevent heavy String allocations and
    // object instantiations inside hot loops for large LRC files. Pre-allocate list capacity to avoid reallocation.
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size) // ⚡ Bolt: pre-allocate to avoid backing array reallocations
        
        for (line in lines) {
            val start = line.indexOf('[')
            if (start == -1) continue

            val length = line.length - start
            if (length < 10) continue

            // Expected format: [mm:ss.xx] or [mm:ss.xxx]
            val m1 = line[start + 1] - '0'
            val m2 = line[start + 2] - '0'
            if (line[start + 3] != ':' || m1 < 0 || m1 > 9 || m2 < 0 || m2 > 9) continue

            val s1 = line[start + 4] - '0'
            val s2 = line[start + 5] - '0'
            if (line[start + 6] != '.' || s1 < 0 || s1 > 9 || s2 < 0 || s2 > 9) continue

            val ms1 = line[start + 7] - '0'
            val ms2 = line[start + 8] - '0'
            if (ms1 < 0 || ms1 > 9 || ms2 < 0 || ms2 > 9) continue

            var ms = ms1 * 100 + ms2 * 10
            var textStart = start + 10

            if (line[start + 9] == ']') {
                textStart = start + 10
            } else if (length >= 11) {
                val ms3 = line[start + 9] - '0'
                if (ms3 >= 0 && ms3 <= 9 && line[start + 10] == ']') {
                    ms += ms3
                    textStart = start + 11
                } else {
                    continue
                }
            } else {
                continue
            }

            val min = m1 * 10 + m2
            val sec = s1 * 10 + s2
            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

            val text = line.substring(textStart).trim()
            if (text.isNotEmpty()) {
                lyrics.add(LyricLine(timeInMs.toLong(), text))
            }
        }
        lyrics.sortBy { it.startTimeMs } // ⚡ Bolt: Use sortBy to mutate in-place rather than sortedBy which creates a new list
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
