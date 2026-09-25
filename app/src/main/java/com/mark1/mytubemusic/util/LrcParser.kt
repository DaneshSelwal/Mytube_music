package com.mark1.mytubemusic.util

import java.io.File

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParser {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate list to avoid backing array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)
        
        for (line in lines) {
            // ⚡ Bolt: Fast path manual character index checks instead of Regex
            if (line.length > 10 && line[0] == '[' && line[3] == ':' && line[6] == '.' && (line[9] == ']' || (line.length > 10 && line[10] == ']'))) {
                try {
                    val min = (line[1] - '0') * 10 + (line[2] - '0')
                    val sec = (line[4] - '0') * 10 + (line[5] - '0')
                    val ms: Int
                    val endBracketIdx: Int
                    if (line[9] == ']') { // [mm:ss.xx] format
                        ms = ((line[7] - '0') * 10 + (line[8] - '0')) * 10
                        endBracketIdx = 9
                    } else if (line.length > 10 && line[10] == ']') { // [mm:ss.xxx] format
                        ms = (line[7] - '0') * 100 + (line[8] - '0') * 10 + (line[9] - '0')
                        endBracketIdx = 10
                    } else {
                        continue
                    }

                    val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                    val textStart = endBracketIdx + 1

                    // trim text manually to avoid extra string allocations
                    var textStartTrim = textStart
                    while (textStartTrim < line.length && line[textStartTrim].isWhitespace()) {
                        textStartTrim++
                    }
                    var textEndTrim = line.length - 1
                    while (textEndTrim >= textStartTrim && line[textEndTrim].isWhitespace()) {
                        textEndTrim--
                    }

                    if (textStartTrim <= textEndTrim) {
                        val text = line.substring(textStartTrim, textEndTrim + 1)
                        lyrics.add(LyricLine(timeInMs, text))
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors for individual lines
                }
            }
        }

        // Use apply and sort internally if needed, but returning sorted list
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
