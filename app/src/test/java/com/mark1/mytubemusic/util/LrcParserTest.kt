package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_standardLinesWithMilliseconds_returnsCorrectList() {
        val lrcContent = """
            [00:12.345] Line 1
            [01:23.456] Line 2
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(2, result.size)

        // 00 * 60 * 1000 + 12 * 1000 + 345 = 12345
        assertEquals(12345L, result[0].startTimeMs)
        assertEquals("Line 1", result[0].text)

        // 01 * 60 * 1000 + 23 * 1000 + 456 = 83456
        assertEquals(83456L, result[1].startTimeMs)
        assertEquals("Line 2", result[1].text)
    }

    @Test
    fun parse_linesWithCentiseconds_padsToMilliseconds() {
        val lrcContent = """
            [00:01.23] Line with centiseconds
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(1, result.size)

        // 00 * 60 * 1000 + 01 * 1000 + 230 = 1230
        assertEquals(1230L, result[0].startTimeMs)
        assertEquals("Line with centiseconds", result[0].text)
    }

    @Test
    fun parse_unsortedTimestamps_returnsSortedList() {
        val lrcContent = """
            [02:00.000] Second Line
            [01:00.000] First Line
            [03:00.000] Third Line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(3, result.size)

        assertEquals(60000L, result[0].startTimeMs)
        assertEquals("First Line", result[0].text)

        assertEquals(120000L, result[1].startTimeMs)
        assertEquals("Second Line", result[1].text)

        assertEquals(180000L, result[2].startTimeMs)
        assertEquals("Third Line", result[2].text)
    }

    @Test
    fun parse_invalidLinesAndEmptyText_ignoresThem() {
        val lrcContent = """
            [00:10.000] Valid Line
            [invalid] Invalid format
            [00:20.000]
            [00:30.000]
            [00:40.000] Another Valid Line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(2, result.size)

        assertEquals(10000L, result[0].startTimeMs)
        assertEquals("Valid Line", result[0].text)

        assertEquals(40000L, result[1].startTimeMs)
        assertEquals("Another Valid Line", result[1].text)
    }

    @Test
    fun parse_emptyString_returnsEmptyList() {
        val result = LrcParser.parse("")
        assertEquals(0, result.size)
    }
}
