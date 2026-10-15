package com.mark1.mytubemusic.util

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class LrcParserTest {

    @Test
    fun testParseStandardFormat() {
        val lrc = "[01:23.456]Hello World"
        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        // 1 min * 60 * 1000 = 60000
        // 23 sec * 1000 = 23000
        // 456 ms = 456
        // Total: 83456
        assertEquals(83456L, result[0].startTimeMs)
        assertEquals("Hello World", result[0].text)
    }

    @Test
    fun testParseCentiseconds() {
        val lrc = "[01:23.45]Hello World"
        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        // 1 min = 60000
        // 23 sec = 23000
        // 45 cs = 450 ms
        // Total: 83450
        assertEquals(83450L, result[0].startTimeMs)
        assertEquals("Hello World", result[0].text)
    }

    @Test
    fun testParseMultipleLinesSorted() {
        val lrc = """
            [00:10.000]Line 2
            [00:05.000]Line 1
            [00:15.000]Line 3
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals("Line 1", result[0].text)
        assertEquals(5000L, result[0].startTimeMs)

        assertEquals("Line 2", result[1].text)
        assertEquals(10000L, result[1].startTimeMs)

        assertEquals("Line 3", result[2].text)
        assertEquals(15000L, result[2].startTimeMs)
    }

    @Test
    fun testParseEmptyLinesAndInvalidFormat() {
        val lrc = """
            [00:05.000]
            Some random text
            [invalid:time]Nope
            [00:10.000] Valid Line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        // Empty lines or invalid formats should be ignored
        assertEquals(1, result.size)
        assertEquals("Valid Line", result[0].text)
        assertEquals(10000L, result[0].startTimeMs)
    }
}
