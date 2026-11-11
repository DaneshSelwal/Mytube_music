package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Replaced Regex with manual parsing, pre-allocated capacity, and used in-place sorting to reduce allocations and execution time.
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            val startBracketIdx = line.indexOf('[')
            if (startBracketIdx == -1) continue
            val endBracketIdx = line.indexOf(']', startBracketIdx + 1)
            if (endBracketIdx == -1) continue

            if (endBracketIdx - startBracketIdx >= 9) {
                val colonIdx = line.indexOf(':', startBracketIdx)
                val dotIdx = line.indexOf('.', colonIdx)
                
                if (colonIdx != -1 && dotIdx != -1 && colonIdx < dotIdx && dotIdx < endBracketIdx) {
                    var isValid = true
                    var min = 0L
                    for (i in startBracketIdx + 1 until colonIdx) {
                        val c = line[i]
                        if (c.isDigit()) {
                            min = min * 10 + (c - '0')
                        } else {
                            isValid = false; break
                        }
                    }
                    if (!isValid) continue

                    var sec = 0L
                    for (i in colonIdx + 1 until dotIdx) {
                        val c = line[i]
                        if (c.isDigit()) {
                            sec = sec * 10 + (c - '0')
                        } else {
                            isValid = false; break
                        }
                    }
                    if (!isValid) continue

                    var ms = 0L
                    val msLen = endBracketIdx - dotIdx - 1
                    for (i in dotIdx + 1 until endBracketIdx) {
                        val c = line[i]
                        if (c.isDigit()) {
                            ms = ms * 10 + (c - '0')
                        } else {
                            isValid = false; break
                        }
                    }
                    if (!isValid) continue

                    if (msLen == 2) ms *= 10

                    val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                    val text = line.substring(endBracketIdx + 1).trim()
                    if (text.isNotEmpty()) {
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                }
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
