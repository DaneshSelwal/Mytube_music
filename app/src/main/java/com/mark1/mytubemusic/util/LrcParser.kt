package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate capacity when possible to avoid array resizing
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (i in lines.indices) {
            val line = lines[i]
            // ⚡ Bolt: Replace expensive Regex with allocation-free manual char index parsing
            // Expected format: [mm:ss.xx] or [mm:ss.xxx]
            val openIdx = line.indexOf('[')
            if (openIdx == -1 || line.length - openIdx < 10 || line[openIdx + 3] != ':') continue

            try {
                val minTens = line[openIdx + 1]
                val minOnes = line[openIdx + 2]
                val secTens = line[openIdx + 4]
                val secOnes = line[openIdx + 5]

                // Ensure they are actually digits
                if (!minTens.isDigit() || !minOnes.isDigit() || !secTens.isDigit() || !secOnes.isDigit()) continue

                // Parse minute and second directly by char indexing
                val minute = (minTens - '0') * 10 + (minOnes - '0')
                val sec = (secTens - '0') * 10 + (secOnes - '0')

                val closeIdx = line.indexOf(']', openIdx + 7)
                if (closeIdx == -1) continue

                val msLength = closeIdx - (openIdx + 7)
                val ms = when (msLength) {
                    2 -> {
                        val m1 = line[openIdx + 7]
                        val m2 = line[openIdx + 8]
                        if (!m1.isDigit() || !m2.isDigit()) continue
                        ((m1 - '0') * 10 + (m2 - '0')) * 10
                    }
                    3 -> {
                        val m1 = line[openIdx + 7]
                        val m2 = line[openIdx + 8]
                        val m3 = line[openIdx + 9]
                        if (!m1.isDigit() || !m2.isDigit() || !m3.isDigit()) continue
                        (m1 - '0') * 100 + (m2 - '0') * 10 + (m3 - '0')
                    }
                    else -> continue
                }

                val timeInMs = (minute * 60 * 1000L) + (sec * 1000L) + ms
                
                val textIdx = closeIdx + 1
                if (textIdx < line.length) {
                    // ⚡ Bolt: safe indexOf check and manual trim avoids BOM parsing errors
                    val text = line.substring(textIdx).trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                }
            } catch (e: Exception) {
                // Ignore malformed lines
                continue
            }
        }
        // ⚡ Bolt: Sort in place to avoid creating a new list allocation
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
