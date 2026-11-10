package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals

class LrcParserPerfTest {

    object LrcParserOptimized {
        fun parse(lrcContent: String): List<LyricLine> {
            val lines = lrcContent.lines()
            val lyrics = ArrayList<LyricLine>(lines.size)

            for (line in lines) {
                val length = line.length
                if (length < 10) continue

                val bracketIndex = line.indexOf('[')
                if (bracketIndex == -1) continue

                val endIndex = line.indexOf(']', bracketIndex)
                if (endIndex == -1 || endIndex < bracketIndex + 9) continue

                val colonIndex = line.indexOf(':', bracketIndex)
                if (colonIndex != bracketIndex + 3) continue

                val dotIndex = line.indexOf('.', colonIndex)
                if (dotIndex != colonIndex + 3) continue

                try {
                    val m1 = line[bracketIndex + 1]
                    val m2 = line[bracketIndex + 2]
                    val s1 = line[colonIndex + 1]
                    val s2 = line[colonIndex + 2]

                    if (!m1.isDigit() || !m2.isDigit() || !s1.isDigit() || !s2.isDigit()) continue

                    val min = (m1 - '0') * 10 + (m2 - '0')
                    val sec = (s1 - '0') * 10 + (s2 - '0')

                    val ms: Int
                    if (endIndex == dotIndex + 3) {
                        val ms1 = line[dotIndex + 1]
                        val ms2 = line[dotIndex + 2]
                        if (ms1.isDigit() && ms2.isDigit()) {
                            ms = (ms1 - '0') * 100 + (ms2 - '0') * 10
                        } else continue
                    } else if (endIndex == dotIndex + 4) {
                        val ms1 = line[dotIndex + 1]
                        val ms2 = line[dotIndex + 2]
                        val ms3 = line[dotIndex + 3]
                        if (ms1.isDigit() && ms2.isDigit() && ms3.isDigit()) {
                            ms = (ms1 - '0') * 100 + (ms2 - '0') * 10 + (ms3 - '0')
                        } else continue
                    } else {
                        continue
                    }

                    val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms

                    val textIndex = endIndex + 1
                    var startText = textIndex
                    while (startText < length && line[startText] <= ' ') startText++

                    if (startText < length) {
                        var endText = length - 1
                        while (endText > startText && line[endText] <= ' ') endText--
                        lyrics.add(LyricLine(timeInMs, line.substring(startText, endText + 1)))
                    }
                } catch (e: Exception) {
                    continue
                }
            }
            lyrics.sortBy { it.startTimeMs }
            return lyrics
        }
    }


    @Test
    fun testPerformance() {
        val lrc = """
            [00:12.00]Line 1
            [00:15.30]Line 2
            [00:20.123]Line 3
            [01:00.00]Line 4
        """.trimIndent()

        // Generate large lrc
        val sb = StringBuilder()
        for (i in 0..1000) {
            sb.append(lrc).append("\n")
        }
        val largeLrc = sb.toString()

        val parsed1 = LrcParser.parse(largeLrc)
        val parsed2 = LrcParserOptimized.parse(largeLrc)

        assertEquals(parsed1.size, parsed2.size)
        for (i in parsed1.indices) {
            assertEquals(parsed1[i].startTimeMs, parsed2[i].startTimeMs)
            assertEquals(parsed1[i].text, parsed2[i].text)
        }

        // Warmup
        for (i in 0..10) {
            LrcParser.parse(largeLrc)
            LrcParserOptimized.parse(largeLrc)
        }

        val time1 = measureTimeMillis {
            for (i in 0..100) {
                LrcParser.parse(largeLrc)
            }
        }

        val time2 = measureTimeMillis {
            for (i in 0..100) {
                LrcParserOptimized.parse(largeLrc)
            }
        }

        println("Original time: ${time1}ms")
        println("Optimized time: ${time2}ms")
    }
}
