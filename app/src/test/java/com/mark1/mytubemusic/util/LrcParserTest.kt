package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals

class LrcParserTest {
    @Test
    fun benchmarkParse() {
        val lrcContent = StringBuilder().apply {
            for (i in 1..1000) {
                append("[00:${(i%60).toString().padStart(2, '0')}.00] Line $i\n")
            }
        }.toString()

        // warmup
        for (i in 0..100) {
            LrcParser.parse(lrcContent)
        }

        val time = measureTimeMillis {
            for (i in 0..100) {
                LrcParser.parse(lrcContent)
            }
        }
        println("BASELINE_TIME: $time ms")
    }

    @Test
    fun testParse() {
        val lrcContent = """
            [00:10.00] Line 1
            [00:20.50] Line 2
            [01:00.000] Line 3
            [invalid] Invalid line
            [02:00.00]
            [03:00.00] Line 4
        """.trimIndent()
        val parsed = LrcParser.parse(lrcContent)

        assertEquals(4, parsed.size)
        assertEquals(10000, parsed[0].startTimeMs)
        assertEquals("Line 1", parsed[0].text)

        assertEquals(20500, parsed[1].startTimeMs)
        assertEquals("Line 2", parsed[1].text)

        assertEquals(60000, parsed[2].startTimeMs)
        assertEquals("Line 3", parsed[2].text)

        assertEquals(180000, parsed[3].startTimeMs)
        assertEquals("Line 4", parsed[3].text)
    }
}
