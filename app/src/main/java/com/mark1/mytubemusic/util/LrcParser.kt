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
        
        for (line in lines) {
            val len = line.length
            if (len >= 10 && line[0] == '[' && line[3] == ':' && line[6] == '.') {
                val min0 = line[1] - '0'
                val min1 = line[2] - '0'
                val sec0 = line[4] - '0'
                val sec1 = line[5] - '0'
                
                if (min0 in 0..9 && min1 in 0..9 && sec0 in 0..9 && sec1 in 0..9) {
                    val min = (min0 * 10 + min1).toLong()
                    val sec = (sec0 * 10 + sec1).toLong()

                    var ms = 0L
                    var msLen = 0
                    var i = 7
                    while (i < len && line[i] in '0'..'9') {
                        ms = ms * 10 + (line[i] - '0')
                        msLen++
                        i++
                    }

                    if (msLen in 2..3 && i < len && line[i] == ']') {
                        if (msLen == 2) ms *= 10

                        val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms

                        var textStart = i + 1
                        while (textStart < len && line[textStart].isWhitespace()) {
                            textStart++
                        }

                        var textEnd = len - 1
                        while (textEnd >= textStart && line[textEnd].isWhitespace()) {
                            textEnd--
                        }

                        if (textEnd >= textStart) {
                            val text = line.substring(textStart, textEnd + 1)
                            lyrics.add(LyricLine(timeInMs, text))
                        }
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
