package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    private val timePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]") // ⚡ Bolt: Extract regex to prevent recompilation

    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size) // ⚡ Bolt: Pre-allocate capacity to avoid array resizing
        
        for (line in lines) {
            val length = line.length
            if (length < 11) continue

            // ⚡ Bolt: Fast path for standard [mm:ss.xx] format to bypass expensive Regex matching
            if (line[0] == '[' && line[3] == ':' && line[6] == '.' && line[9] == ']') {
                val m1 = line[1] - '0'
                val m2 = line[2] - '0'
                val s1 = line[4] - '0'
                val s2 = line[5] - '0'
                val ms1 = line[7] - '0'
                val ms2 = line[8] - '0'

                if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9 && ms1 in 0..9 && ms2 in 0..9) {
                    val min = m1 * 10L + m2
                    val sec = s1 * 10L + s2
                    val ms = ms1 * 100L + ms2 * 10L // Convert hundredths to milliseconds

                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                    var i = 10
                    while (i < length && line[i] <= ' ') i++ // ⚡ Bolt: Manual trimStart to reduce string allocations

                    if (i < length) {
                        lyrics.add(LyricLine(timeInMs, line.substring(i)))
                    }
                    continue
                }
            }

            // Fallback for non-standard formats (e.g., 3-digit milliseconds)
            val matchResult = timePattern.find(line)
            if (matchResult != null) {
                val min = matchResult.groupValues[1].toLong()
                val sec = matchResult.groupValues[2].toLong()
                var msStr = matchResult.groupValues[3]
                if (msStr.length == 2) msStr += "0" // handle centiseconds
                val ms = msStr.toLong()
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                val text = line.substring(matchResult.range.last + 1).trim()
                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timeInMs, text))
                }
            }
        }
        lyrics.sortBy { it.startTimeMs } // ⚡ Bolt: Sort in-place instead of creating a new list allocation
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
