package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals

class LrcParserTest {

    @Test
    fun testParseValidFormatCentiseconds() {
        val lrc = "[01:23.45] Hello World\n[01:24.00] Goodbye"
        val result = LrcParser.parse(lrc)
        assertEquals(2, result.size)

        // 1*60*1000 + 23*1000 + 450 = 60000 + 23000 + 450 = 83450
        assertEquals(83450L, result[0].startTimeMs)
        assertEquals("Hello World", result[0].text)

        assertEquals(84000L, result[1].startTimeMs)
        assertEquals("Goodbye", result[1].text)
    }

    @Test
    fun testParseValidFormatMilliseconds() {
        val lrc = "[01:23.456] Hello Milliseconds\n[01:24.000] "
        val result = LrcParser.parse(lrc)

        // Line with empty text might be filtered out depending on behavior
        // Original code filters out empty text if it's completely blank
        assertEquals(1, result.size)

        assertEquals(83456L, result[0].startTimeMs)
        assertEquals("Hello Milliseconds", result[0].text)
    }

    @Test
    fun testFallbackRegex() {
        val lrc = "some text [01:23.45] Embedded tag\n[00:00.00]  "
        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals(83450L, result[0].startTimeMs)
        assertEquals("Embedded tag", result[0].text)
    }

    @Test
    fun benchmarkParse() {
        val sampleLrc = buildString {
            for (i in 0..5000) {
                val min = (i / 60).toString().padStart(2, '0')
                val sec = (i % 60).toString().padStart(2, '0')
                appendLine("[$min:$sec.00] This is a lyric line number $i")
            }
        }

        // Warmup
        for (i in 0..10) {
            LrcParser.parse(sampleLrc)
        }

        var totalTime = 0L
        for (i in 0..50) {
            totalTime += measureTimeMillis {
                LrcParser.parse(sampleLrc)
            }
        }
        println("Original parse time for 50 iterations (5000 lines each): $totalTime ms")
    }
}
