package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        
        // Optimization: Pre-allocate capacity assuming mostly valid lyric lines
        // to avoid backing array reallocations in performance-critical parsing.
        val lyrics = ArrayList<LyricLine>(lines.size)

        // Optimization: Replace expensive Regex matching with manual string index parsing
        // and primitive mathematical conversions for the highly predictable [MM:SS.xxx] format.
        // This avoids memory allocations and significantly reduces processing time.
        for (line in lines) {
            val length = line.length
            if (length < 10) continue // Minimum length for "[00:00.00]" is 10

            val bracketIndex = line.indexOf('[')
            if (bracketIndex == -1) continue

            val endIndex = line.indexOf(']', bracketIndex)
            if (endIndex == -1 || endIndex < bracketIndex + 9) continue

            val colonIndex = line.indexOf(':', bracketIndex)
            if (colonIndex != bracketIndex + 3) continue

            val dotIndex = line.indexOf('.', colonIndex)
            if (dotIndex != colonIndex + 3) continue

            try {
                val m1 = line[bracketIndex + 1]
                val m2 = line[bracketIndex + 2]
                val s1 = line[colonIndex + 1]
                val s2 = line[colonIndex + 2]

                // Explicitly validate digits to prevent silently calculating garbage integers
                if (!m1.isDigit() || !m2.isDigit() || !s1.isDigit() || !s2.isDigit()) continue

                val min = (m1 - '0') * 10 + (m2 - '0')
                val sec = (s1 - '0') * 10 + (s2 - '0')
                
                val ms: Int
                if (endIndex == dotIndex + 3) {
                    // [MM:SS.xx] (centiseconds)
                    val ms1 = line[dotIndex + 1]
                    val ms2 = line[dotIndex + 2]
                    if (ms1.isDigit() && ms2.isDigit()) {
                        ms = (ms1 - '0') * 100 + (ms2 - '0') * 10
                    } else continue
                } else if (endIndex == dotIndex + 4) {
                    // [MM:SS.xxx] (milliseconds)
                    val ms1 = line[dotIndex + 1]
                    val ms2 = line[dotIndex + 2]
                    val ms3 = line[dotIndex + 3]
                    if (ms1.isDigit() && ms2.isDigit() && ms3.isDigit()) {
                        ms = (ms1 - '0') * 100 + (ms2 - '0') * 10 + (ms3 - '0')
                    } else continue
                } else {
                    continue
                }

                val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                val textIndex = endIndex + 1
                var startText = textIndex

                // Fast trim Start
                while (startText < length && line[startText] <= ' ') startText++

                if (startText < length) {
                    // Fast trim End
                    var endText = length - 1
                    while (endText > startText && line[endText] <= ' ') endText--
                    lyrics.add(LyricLine(timeInMs, line.substring(startText, endText + 1)))
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Optimization: Use in-place sortBy instead of sortedBy to avoid allocating a new list
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
