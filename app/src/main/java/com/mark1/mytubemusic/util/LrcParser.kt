package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = mutableListOf<LyricLine>()
        val timePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")
        
        for (line in lines) {
            val matchResult = timePattern.find(line)
            if (matchResult != null) {
                val min = matchResult.groupValues[1].toLong()
                val sec = matchResult.groupValues[2].toLong()
                var msStr = matchResult.groupValues[3]
                if (msStr.length == 2) msStr += "0" // handle centiseconds
                val ms = msStr.toLong()
                
                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                val text = line.substring(matchResult.range.last + 1).trim()
                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timeInMs, text))
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
