package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureNanoTime
import org.junit.Assert.assertEquals

class LrcParserTest {
    @Test
    fun benchmarkLrcParser() {
        val lyrics = StringBuilder()
        for (i in 0..1000) {
            val min = (i / 60).toString().padStart(2, '0')
            val sec = (i % 60).toString().padStart(2, '0')
            lyrics.append("[$min:$sec.00] Line number $i\n")
        }
        val content = lyrics.toString()

        // Warmup
        for (i in 0..1000) {
            LrcParser.parse(content)
        }

        var totalTime = 0L
        for (i in 0..1000) {
            totalTime += measureNanoTime {
                LrcParser.parse(content)
            }
        }

        println("Average FAST parse time: ${totalTime / 1000 / 1_000_000.0} ms")
    }

    @Test
    fun testParseCorrectness() {
        val lrc = """
            [00:00.00] Line 1
            [00:01.50] Line 2
            [01:10.02] Line 3
            [01:10.123] Line 4
        """.trimIndent()
        val result = LrcParser.parse(lrc)
        assertEquals(4, result.size)
        assertEquals(0L, result[0].startTimeMs)
        assertEquals("Line 1", result[0].text)
        assertEquals(1500L, result[1].startTimeMs)
        assertEquals("Line 2", result[1].text)
        assertEquals(70020L, result[2].startTimeMs)
        assertEquals("Line 3", result[2].text)
        assertEquals(70123L, result[3].startTimeMs)
        assertEquals("Line 4", result[3].text)
    }
}
