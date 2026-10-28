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
        
        // ⚡ Bolt: Fast string parsing for highly predictable LRC format.
        // Replaces slow Regex instantiation and `substring()` allocations with manual character checks and primitive math.
        for (line in lines) {
            val len = line.length
            if (len >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val closeIdx = line.indexOf(']', 7)
                if (closeIdx == 9 || closeIdx == 10) {
                    try {
                        val min = (line[1] - '0') * 10 + (line[2] - '0')
                        val sec = (line[4] - '0') * 10 + (line[5] - '0')
                        val ms = if (closeIdx == 9) {
                            (line[7] - '0') * 100 + (line[8] - '0') * 10
                        } else {
                            (line[7] - '0') * 100 + (line[8] - '0') * 10 + (line[9] - '0')
                        }

                        val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                        if (closeIdx + 1 < len) {
                            var startIdx = closeIdx + 1
                            while (startIdx < len && line[startIdx] <= ' ') startIdx++
                            var endIdx = len - 1
                            while (endIdx >= startIdx && line[endIdx] <= ' ') endIdx--

                            if (startIdx <= endIdx) {
                                val text = line.substring(startIdx, endIdx + 1)
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                        }
                    } catch (e: Exception) {
                        // ignore malformed lines
                    }
                }
            }
        }
        // ⚡ Bolt: In-place sort to prevent unnecessary intermediate collection allocations
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
