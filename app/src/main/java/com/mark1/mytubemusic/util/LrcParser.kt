package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    // ⚡ Bolt Optimization:
    // Replaced Regex and intermediate string allocations with manual character parsing.
    // Pre-allocated ArrayList capacity based on lines to avoid backing array reallocations.
    // Used sortBy for in-place sorting instead of sortedBy which creates a new list.
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            // Find the start of a timestamp
            val bracketIndex = line.indexOf('[')
            if (bracketIndex == -1) continue

            // Find the end of the timestamp
            val closeBracketIndex = line.indexOf(']', bracketIndex)
            // A valid timestamp like [00:00.00] is at least 10 chars long (including brackets),
            // but let's be safe and say at least 9 chars long for [0:00.00]
            if (closeBracketIndex == -1 || closeBracketIndex - bracketIndex < 9) continue

            val colonIndex = line.indexOf(':', bracketIndex)
            val dotIndex = line.indexOf('.', colonIndex)

            if (colonIndex == -1 || dotIndex == -1 || colonIndex > closeBracketIndex || dotIndex > closeBracketIndex) continue

            try {
                // Parse minutes
                var min = 0L
                for (i in bracketIndex + 1 until colonIndex) {
                    val c = line[i]
                    if (c in '0'..'9') min = min * 10 + (c - '0') else throw NumberFormatException()
                }

                // Parse seconds
                var sec = 0L
                for (i in colonIndex + 1 until dotIndex) {
                    val c = line[i]
                    if (c in '0'..'9') sec = sec * 10 + (c - '0') else throw NumberFormatException()
                }

                // Parse milliseconds
                var ms = 0L
                var msDigits = 0
                for (i in dotIndex + 1 until closeBracketIndex) {
                    val c = line[i]
                    if (c in '0'..'9') {
                        ms = ms * 10 + (c - '0')
                        msDigits++
                    } else throw NumberFormatException()
                }
                if (msDigits == 2) ms *= 10 // Handle centiseconds
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                // Trim trailing whitespace manually to avoid allocations
                var textStart = closeBracketIndex + 1
                while(textStart < line.length && line[textStart].isWhitespace()) {
                    textStart++
                }
                var textEnd = line.length - 1
                while(textEnd >= textStart && line[textEnd].isWhitespace()) {
                    textEnd--
                }

                if (textEnd >= textStart) {
                    lyrics.add(LyricLine(timeInMs, line.substring(textStart, textEnd + 1)))
                }
            } catch (e: Exception) {
                // Skip malformed lines
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
