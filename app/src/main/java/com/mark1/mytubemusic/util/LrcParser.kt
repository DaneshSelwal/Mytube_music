package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate capacity to avoid backing array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            // ⚡ Bolt: Use manual string parsing instead of regex overhead for highly predictable LRC format
            val bracketOpen = line.indexOf('[')
            if (bracketOpen == -1) continue

            val colon = line.indexOf(':', bracketOpen + 1)
            if (colon == -1) continue

            val dot = line.indexOf('.', colon + 1)
            if (dot == -1) continue

            val bracketClose = line.indexOf(']', dot + 1)
            if (bracketClose == -1) continue

            var min = 0L
            var valid = true
            for (i in bracketOpen + 1 until colon) {
                val c = line[i]
                if (c in '0'..'9') min = min * 10 + (c.code - '0'.code).toLong()
                else { valid = false; break }
            }
            if (!valid) continue

            var sec = 0L
            for (i in colon + 1 until dot) {
                val c = line[i]
                if (c in '0'..'9') sec = sec * 10 + (c.code - '0'.code).toLong()
                else { valid = false; break }
            }
            if (!valid) continue

            var ms = 0L
            var msChars = 0
            for (i in dot + 1 until bracketClose) {
                val c = line[i]
                if (c in '0'..'9') {
                    ms = ms * 10 + (c.code - '0'.code).toLong()
                    msChars++
                } else { valid = false; break }
            }
            if (!valid) continue

            if (msChars == 2) ms *= 10

            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

            // ⚡ Bolt: Avoid trim() which creates new strings and doesn't remove BOM
            var textStart = bracketClose + 1
            while (textStart < line.length && line[textStart].isWhitespace()) {
                textStart++
            }
            var textEnd = line.length - 1
            while (textEnd >= textStart && line[textEnd].isWhitespace()) {
                textEnd--
            }
            if (textStart <= textEnd) {
                lyrics.add(LyricLine(timeInMs, line.substring(textStart, textEnd + 1)))
            }
        }
        // ⚡ Bolt: Apply fast sort in-place
        return lyrics.apply { sortBy { it.startTimeMs } }
    }
    
    fun getLyricsFromFile(path: String): List<LyricLine> {
        val file = File(path)
        if (file.exists()) {
            return parse(file.readText())
        }
        return emptyList()
    }
}
