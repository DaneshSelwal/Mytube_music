package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate list to avoid array resizing overhead
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        // ⚡ Bolt: Replaced Regex with manual index and character parsing
        // This avoids creating MatchResult and Substring objects per line,
        // significantly reducing memory allocation and improving parsing speed.
        for (line in lines) {
            val startIndex = line.indexOf('[')
            val endIndex = line.indexOf(']')

            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                val colonIndex = line.indexOf(':', startIndex)
                val dotIndex = line.indexOf('.', colonIndex)
                
                if (colonIndex != -1 && dotIndex != -1 && colonIndex < endIndex && dotIndex < endIndex) {
                    try {
                        val min = parseLong(line, startIndex + 1, colonIndex)
                        val sec = parseLong(line, colonIndex + 1, dotIndex)
                        var ms = parseLong(line, dotIndex + 1, endIndex)

                        // Handle centiseconds (e.g. .23 instead of .230)
                        if (endIndex - (dotIndex + 1) == 2) {
                            ms *= 10
                        }

                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                        val text = line.substring(endIndex + 1).trim()

                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                    } catch (e: Exception) {
                        // Skip malformed time tags
                    }
                }
            }
        }
        // ⚡ Bolt: Use in-place sorting to avoid creating a new list
        lyrics.sortBy { it.startTimeMs }
        return lyrics
    }

    private fun parseLong(str: String, start: Int, end: Int): Long {
        var result = 0L
        for (i in start until end) {
            val char = str[i]
            if (char in '0'..'9') {
                result = result * 10 + (char - '0').toLong()
            } else {
                throw NumberFormatException()
            }
        }
        return result
    }
    
    fun getLyricsFromFile(path: String): List<LyricLine> {
        val file = File(path)
        if (file.exists()) {
            return parse(file.readText())
        }
        return emptyList()
    }
}
