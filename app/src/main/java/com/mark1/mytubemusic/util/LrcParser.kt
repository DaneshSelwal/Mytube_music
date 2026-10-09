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
            val start = line.indexOf('[')
            val colon = line.indexOf(':', start + 1)
            val dot = line.indexOf('.', colon + 1)
            val end = line.indexOf(']', dot + 1)

            if (start != -1 && colon != -1 && dot != -1 && end != -1) {
                try {
                    var min = 0L
                    for (i in (start + 1) until colon) {
                        val c = line[i]
                        if (c in '0'..'9') min = min * 10 + (c - '0') else throw Exception()
                    }
                    var sec = 0L
                    for (i in (colon + 1) until dot) {
                        val c = line[i]
                        if (c in '0'..'9') sec = sec * 10 + (c - '0') else throw Exception()
                    }
                    var ms = 0L
                    val msLen = end - dot - 1
                    for (i in (dot + 1) until end) {
                        val c = line[i]
                        if (c in '0'..'9') ms = ms * 10 + (c - '0') else throw Exception()
                    }
                    if (msLen == 2) ms *= 10

                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                    var textStart = end + 1
                    while (textStart < line.length && line[textStart].isWhitespace()) textStart++
                    var textEnd = line.length - 1
                    while (textEnd >= textStart && line[textEnd].isWhitespace()) textEnd--
                    if (textStart <= textEnd) {
                        lyrics.add(LyricLine(timeInMs, line.substring(textStart, textEnd + 1)))
                    }
                } catch (e: Exception) {
                    // Ignore malformed lines
                }
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
