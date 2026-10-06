package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureTimeMillis

class LrcParserManualBenchmarkTest {

    fun parseManual(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = ArrayList<LyricLine>(lines.size)

        for (line in lines) {
            val startIdx = line.indexOf('[')
            val endIdx = line.indexOf(']', startIdx + 1)

            if (startIdx != -1 && endIdx != -1 && endIdx - startIdx >= 9 &&
                line[startIdx + 3] == ':' && line[startIdx + 6] == '.') {

                val m1 = line[startIdx + 1] - '0'
                val m2 = line[startIdx + 2] - '0'
                val s1 = line[startIdx + 4] - '0'
                val s2 = line[startIdx + 5] - '0'

                if (m1 in 0..9 && m2 in 0..9 && s1 in 0..9 && s2 in 0..9) {
                    val min = m1 * 10 + m2
                    val sec = s1 * 10 + s2

                    var ms = 0
                    val msLength = endIdx - (startIdx + 7)
                    var validMs = false

                    if (msLength == 2) {
                        val ms1 = line[startIdx + 7] - '0'
                        val ms2 = line[startIdx + 8] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9) {
                            ms = (ms1 * 10 + ms2) * 10
                            validMs = true
                        }
                    } else if (msLength == 3) {
                        val ms1 = line[startIdx + 7] - '0'
                        val ms2 = line[startIdx + 8] - '0'
                        val ms3 = line[startIdx + 9] - '0'
                        if (ms1 in 0..9 && ms2 in 0..9 && ms3 in 0..9) {
                            ms = ms1 * 100 + ms2 * 10 + ms3
                            validMs = true
                        }
                    }

                    if (validMs) {
                        val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                        val text = line.substring(endIdx + 1).trim()
                        if (text.isNotEmpty()) {
                            lyrics.add(LyricLine(timeInMs, text))
                        }
                    }
                }
            }
        }
        return lyrics.sortedBy { it.startTimeMs }
    }

    @Test
    fun testPerformance() {
        val lrc = StringBuilder()
        for (i in 0..1000) {
            val min = String.format("%02d", i % 60)
            val sec = String.format("%02d", (i * 2) % 60)
            val ms = String.format("%02d", i % 100)
            lrc.append("[$min:$sec.$ms] Line $i lyric text here\n")
        }
        val lrcContent = lrc.toString()

        // Warmup
        for (i in 0..10) {
            LrcParser.parse(lrcContent)
            parseManual(lrcContent)
        }

        val timeRegex = measureTimeMillis {
            for (i in 0..100) {
                LrcParser.parse(lrcContent)
            }
        }

        val timeManual = measureTimeMillis {
            for (i in 0..100) {
                parseManual(lrcContent)
            }
        }

        println("Regex time: $timeRegex ms")
        println("Manual time: $timeManual ms")

        // Correctness check
        val r1 = LrcParser.parse(lrcContent)
        val r2 = parseManual(lrcContent)
        assert(r1 == r2)
    }
}
