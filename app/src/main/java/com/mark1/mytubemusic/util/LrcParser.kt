package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // Pre-allocate array list to avoid resizing overhead
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        // Manual parsing avoids Regex compilation and multiple object allocations, improving CPU time
        for (line in lines) {
            val bracketOpen = line.indexOf('[')
            if (bracketOpen == -1) continue

            val colon = line.indexOf(':', bracketOpen)
            if (colon == -1 || colon - bracketOpen != 3) continue

            val dot = line.indexOf('.', colon)
            if (dot == -1 || dot - colon != 3) continue

            val bracketClose = line.indexOf(']', dot)
            if (bracketClose == -1 || (bracketClose - dot != 3 && bracketClose - dot != 4)) continue

            try {
                // Parse timestamp primitively to save string parsing overhead
                val min = (line[bracketOpen + 1] - '0') * 10L + (line[bracketOpen + 2] - '0')
                val sec = (line[colon + 1] - '0') * 10L + (line[colon + 2] - '0')

                val ms = if (bracketClose - dot == 3) {
                    (line[dot + 1] - '0') * 100L + (line[dot + 2] - '0') * 10L
                } else {
                    (line[dot + 1] - '0') * 100L + (line[dot + 2] - '0') * 10L + (line[dot + 3] - '0')
                }
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                // Manual trim to avoid regex or string allocations
                var start = bracketClose + 1
                while (start < line.length && line[start].isWhitespace()) {
                    start++
                }
                var end = line.length - 1
                while (end >= start && line[end].isWhitespace()) {
                    end--
                }

                if (start <= end) {
                    val text = line.substring(start, end + 1)
                    lyrics.add(LyricLine(timeInMs, text))
                }
            } catch (e: Exception) {
                // Ignore improperly formatted line
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
