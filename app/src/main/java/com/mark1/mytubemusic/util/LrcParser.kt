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
            val bracketStart = line.indexOf('[')
            if (bracketStart == -1) continue

            val colon = line.indexOf(':', bracketStart + 1)
            if (colon == -1) continue

            val dot = line.indexOf('.', colon + 1)
            if (dot == -1) continue

            val bracketEnd = line.indexOf(']', dot + 1)
            if (bracketEnd == -1) continue

            // Check length constraints: min=2, sec=2, ms=2 or 3
            if (colon - bracketStart - 1 != 2) continue
            if (dot - colon - 1 != 2) continue
            val msLen = bracketEnd - dot - 1
            if (msLen != 2 && msLen != 3) continue

            var min = 0L
            var sec = 0L
            var ms = 0L
            var isValid = true

            // Parse min
            for (i in bracketStart + 1 until colon) {
                val c = line[i]
                if (c in '0'..'9') {
                    min = min * 10 + (c - '0')
                } else {
                    isValid = false; break
                }
            }
            if (!isValid) continue

            // Parse sec
            for (i in colon + 1 until dot) {
                val c = line[i]
                if (c in '0'..'9') {
                    sec = sec * 10 + (c - '0')
                } else {
                    isValid = false; break
                }
            }
            if (!isValid) continue

            // Parse ms
            for (i in dot + 1 until bracketEnd) {
                val c = line[i]
                if (c in '0'..'9') {
                    ms = ms * 10 + (c - '0')
                } else {
                    isValid = false; break
                }
            }
            if (!isValid) continue

            if (msLen == 2) ms *= 10

            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

            var start = bracketEnd + 1
            var end = line.length - 1
            while (start <= end && line[start].isWhitespace()) start++
            while (end >= start && line[end].isWhitespace()) end--

            if (start <= end) {
                lyrics.add(LyricLine(timeInMs, line.substring(start, end + 1)))
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
