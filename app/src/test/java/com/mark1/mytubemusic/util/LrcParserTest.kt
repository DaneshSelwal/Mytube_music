package com.mark1.mytubemusic.util

import org.junit.Test
import org.junit.Assert.assertEquals

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [00:12.00]Line 1
            [01:05.123]Line 2
            [01:10.50]  Line 3
        """.trimIndent()
        val parsed = LrcParser.parse(lrc)
        assertEquals(3, parsed.size)
        assertEquals(12000L, parsed[0].startTimeMs)
        assertEquals("Line 1", parsed[0].text)

        assertEquals(65123L, parsed[1].startTimeMs)
        assertEquals("Line 2", parsed[1].text)

        assertEquals(70500L, parsed[2].startTimeMs)
        assertEquals("Line 3", parsed[2].text)
    }

    @Test
    fun testParsePerformance() {
        val lrcBuilder = StringBuilder()
        for (i in 0 until 10000) {
            val min = String.format("%02d", i / 60)
            val sec = String.format("%02d", i % 60)
            lrcBuilder.append("[$min:$sec.000] Line $i\n")
        }
        val lrc = lrcBuilder.toString()

        val start = System.currentTimeMillis()
        LrcParser.parse(lrc)
        val end = System.currentTimeMillis()
        println("Optimized parse took ${end - start} ms")
    }
}
