package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // Optimization: Pre-allocate capacity to prevent array reallocation
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        // Optimization: Use manual parsing instead of Regex to significantly reduce
        // CPU time and intermediate string allocations in parsing loops.
        for (line in lines) {
            val start = line.indexOf('[')
            if (start == -1) continue
            val end = line.indexOf(']', start)
            if (end == -1) continue

            var colonIdx = -1
            var dotIdx = -1
            for (i in start + 1 until end) {
                val c = line[i]
                if (c == ':') colonIdx = i
                else if (c == '.') dotIdx = i
            }

            if (colonIdx == -1) continue

            try {
                var min = 0L
                for (i in start + 1 until colonIdx) {
                    min = min * 10 + (line[i] - '0')
                }

                var sec = 0L
                val secEnd = if (dotIdx != -1) dotIdx else end
                for (i in colonIdx + 1 until secEnd) {
                    sec = sec * 10 + (line[i] - '0')
                }

                var ms = 0L
                if (dotIdx != -1) {
                    val msStr = line.substring(dotIdx + 1, end)
                    if (msStr.isNotEmpty()) {
                        ms = msStr.toLong()
                        if (msStr.length == 2) ms *= 10
                    }
                }
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                val text = line.substring(end + 1).trim()
                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timeInMs, text))
                }
            } catch (e: Exception) {
                // Ignore malformed lines
            }
        }
        // Optimization: Sort in-place instead of creating a new list
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
