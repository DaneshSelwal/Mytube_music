package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // Pre-allocate list capacity based on number of lines to avoid array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            // Fast manual parsing instead of Regex to significantly reduce CPU overhead and object allocations.
            // Expected format: "[mm:ss.xx] text" or "[mm:ss.xxx] text"
            val startBracketIndex = trimmedLine.indexOf('[')
            if (startBracketIndex != -1) {
                val closeBracketIndex = trimmedLine.indexOf(']', startBracketIndex)
                if (closeBracketIndex != -1 && (closeBracketIndex - startBracketIndex) >= 9) {
                    try {
                        val colonIndex = trimmedLine.indexOf(':', startBracketIndex + 1)
                        val dotIndex = trimmedLine.indexOf('.', colonIndex)

                        if (colonIndex != -1 && dotIndex != -1 && dotIndex < closeBracketIndex) {
                            // Extract minutes, seconds, and milliseconds manually without allocating strings
                            var min = 0L
                            for (i in startBracketIndex + 1 until colonIndex) {
                                val c = trimmedLine[i]
                                if (c in '0'..'9') {
                                    min = min * 10 + (c - '0')
                                } else {
                                    throw NumberFormatException()
                                }
                            }

                            var sec = 0L
                            for (i in colonIndex + 1 until dotIndex) {
                                val c = trimmedLine[i]
                                if (c in '0'..'9') {
                                    sec = sec * 10 + (c - '0')
                                } else {
                                    throw NumberFormatException()
                                }
                            }

                            var ms = 0L
                            val msLength = closeBracketIndex - (dotIndex + 1)
                            var msMultiplier = 1L
                            if (msLength == 2) {
                                msMultiplier = 10L // Pad centiseconds
                            }

                            for (i in dotIndex + 1 until closeBracketIndex) {
                                val c = trimmedLine[i]
                                if (c in '0'..'9') {
                                    ms = ms * 10 + (c - '0')
                                } else {
                                    throw NumberFormatException()
                                }
                            }
                            ms *= msMultiplier

                            val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                            val text = trimmedLine.substring(closeBracketIndex + 1).trim()

                            if (text.isNotEmpty()) {
                                lyrics.add(LyricLine(timeInMs, text))
                            }
                        }
                    } catch (e: NumberFormatException) {
                        // Skip lines with invalid time format
                    }
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
