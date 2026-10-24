package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        // Optimization: Pre-allocate capacity to avoid array resizing overhead
        val lyrics = ArrayList<LyricLine>(200)
        var startIdx = 0
        val len = lrcContent.length
        
        // Optimization: Manual string indexing prevents excessive object allocation
        // compared to split/lines() and Regex processing.
        while (startIdx < len) {
            var endIdx = lrcContent.indexOf('\n', startIdx)
            if (endIdx == -1) {
                endIdx = len
            }

            // Expected format: [mm:ss.xx] text... -> min length ~10 chars
            if (endIdx - startIdx > 10 && lrcContent[startIdx] == '[') {
                val closeIdx = lrcContent.indexOf(']', startIdx + 1)
                
                if (closeIdx != -1 && closeIdx < endIdx) {
                    try {
                        val min = lrcContent.substring(startIdx + 1, startIdx + 3).toLong()
                        val sec = lrcContent.substring(startIdx + 4, startIdx + 6).toLong()

                        val msStart = startIdx + 7
                        var msStr = lrcContent.substring(msStart, closeIdx)
                        if (msStr.length == 2) msStr += "0" // handle centiseconds
                        val ms = msStr.toLong()

                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                        val text = lrcContent.substring(closeIdx + 1, endIdx).trim()
                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                    } catch (e: Exception) {
                        // Ignore parsing errors for malformed lines
                    }
                }
            }
            startIdx = endIdx + 1
        }

        // Optimization: Use in-place sortBy to avoid creating a new list copy
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
