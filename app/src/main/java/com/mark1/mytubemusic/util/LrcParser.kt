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
            val length = line.length
            if (length < 10) continue
            if (line[0] != '[') continue

            val colonIndex = line.indexOf(':')
            if (colonIndex != 3) continue

            val dotIndex = line.indexOf('.', colonIndex)
            if (dotIndex == -1 || dotIndex > colonIndex + 3) continue

            val bracketIndex = line.indexOf(']', dotIndex)
            if (bracketIndex == -1 || bracketIndex > dotIndex + 4) continue

            try {
                var min = 0L
                for (i in 1 until colonIndex) {
                    if (!line[i].isDigit()) throw NumberFormatException()
                    min = min * 10 + (line[i] - '0')
                }

                var sec = 0L
                for (i in colonIndex + 1 until dotIndex) {
                    if (!line[i].isDigit()) throw NumberFormatException()
                    sec = sec * 10 + (line[i] - '0')
                }

                var ms = 0L
                for (i in dotIndex + 1 until bracketIndex) {
                    if (!line[i].isDigit()) throw NumberFormatException()
                    ms = ms * 10 + (line[i] - '0')
                }

                if (bracketIndex - dotIndex - 1 == 2) {
                    ms *= 10
                }
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                var textStart = bracketIndex + 1
                while (textStart < length && line[textStart] == ' ') {
                    textStart++
                }

                var textEnd = length - 1
                while (textEnd >= textStart && line[textEnd] == ' ') {
                    textEnd--
                }

                if (textStart <= textEnd) {
                    lyrics.add(LyricLine(timeInMs, line.substring(textStart, textEnd + 1)))
                }
            } catch (e: Exception) {
                // Ignore parsing errors for individual lines
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
