package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate collection size to avoid backing array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            // ⚡ Bolt: Use manual character index checks and primitive parsing instead of Regex to significantly reduce processing time and memory overhead.
            if (line.length > 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val closeBracketIndex = line.indexOf(']')
                if (closeBracketIndex > 6) {
                    try {
                        val min = line.substring(1, 3).toLong()
                        val sec = line.substring(4, 6).toLong()
                        var msStr = line.substring(7, closeBracketIndex)
                        if (msStr.length == 2) msStr += "0" // handle centiseconds
                        val ms = msStr.toLong()

                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                        val text = line.substring(closeBracketIndex + 1).trim()
                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                    } catch (e: NumberFormatException) {
                        // Ignore lines that don't match the format perfectly
                    }
                }
            }
        }
        // ⚡ Bolt: Use in-place sortBy to prevent unnecessary intermediate collection allocations during sorting
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
