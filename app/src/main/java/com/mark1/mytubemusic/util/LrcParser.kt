package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // Pre-allocate list capacity to avoid reallocation overhead during parsing
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            // Fast path manual parsing for [MM:SS.xx] format.
            // This avoids Regex compilation and intermediate string/array allocations.
            val length = line.length
            if (length < 10) continue

            val startIdx = line.indexOf('[')
            if (startIdx == -1) continue

            val endIdx = line.indexOf(']', startIdx)
            if (endIdx == -1) continue

            // Extract the timestamp inside brackets e.g. "01:23.45"
            val timestamp = line.substring(startIdx + 1, endIdx)
            val colonIdx = timestamp.indexOf(':')
            val dotIdx = timestamp.indexOf('.', colonIdx)

            if (colonIdx != -1 && dotIdx != -1) {
                try {
                    val min = timestamp.substring(0, colonIdx).toLong()
                    val sec = timestamp.substring(colonIdx + 1, dotIdx).toLong()
                    var msStr = timestamp.substring(dotIdx + 1)
                    if (msStr.length == 2) msStr += "0" // handle centiseconds
                    val ms = msStr.toLong()

                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                    val text = line.substring(endIdx + 1).trim()

                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                } catch (e: NumberFormatException) {
                    // Ignore malformed timestamp lines
                }
            }
        }

        // Sort in-place to prevent unnecessary intermediate collection allocations
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
