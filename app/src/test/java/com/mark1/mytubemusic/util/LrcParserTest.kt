package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParseStandardLrcFormat() {
        val lrcContent = """
            [00:12.00]Line 1
            [00:15.30]Line 2
            [01:00.50]Line 3
        """.trimIndent()

        val parsed = LrcParser.parse(lrcContent)

        assertEquals(3, parsed.size)

        assertEquals(12000L, parsed[0].startTimeMs)
        assertEquals("Line 1", parsed[0].text)

        assertEquals(15300L, parsed[1].startTimeMs)
        assertEquals("Line 2", parsed[1].text)

        assertEquals(60500L, parsed[2].startTimeMs)
        assertEquals("Line 3", parsed[2].text)
    }

    @Test
    fun testParseCentiseconds() {
        val lrcContent = """
            [00:01.05]Centiseconds
            [00:02.50]More centiseconds
            [00:03.123]Milliseconds
        """.trimIndent()

        val parsed = LrcParser.parse(lrcContent)

        assertEquals(3, parsed.size)

        assertEquals(1050L, parsed[0].startTimeMs)
        assertEquals("Centiseconds", parsed[0].text)

        assertEquals(2500L, parsed[1].startTimeMs)
        assertEquals("More centiseconds", parsed[1].text)

        assertEquals(3123L, parsed[2].startTimeMs)
        assertEquals("Milliseconds", parsed[2].text)
    }

    @Test
    fun testIgnoresMalformedAndEmptyLines() {
        val lrcContent = """
            [ar:Artist]
            [ti:Title]
            [00:10.00]
            Invalid line format
            [00:15.00] Valid line
            [00:20] Missing milliseconds
            [00:25.00]
        """.trimIndent()

        val parsed = LrcParser.parse(lrcContent)

        assertEquals(1, parsed.size)
        assertEquals(15000L, parsed[0].startTimeMs)
        assertEquals("Valid line", parsed[0].text)
    }

    @Test
    fun testSortsByTimestamp() {
        val lrcContent = """
            [01:00.00]Second
            [00:30.00]First
            [01:30.00]Third
        """.trimIndent()

        val parsed = LrcParser.parse(lrcContent)

        assertEquals(3, parsed.size)

        assertEquals(30000L, parsed[0].startTimeMs)
        assertEquals("First", parsed[0].text)

        assertEquals(60000L, parsed[1].startTimeMs)
        assertEquals("Second", parsed[1].text)

        assertEquals(90000L, parsed[2].startTimeMs)
        assertEquals("Third", parsed[2].text)
    }

    @Test
    fun testEmptyInput() {
        val parsed = LrcParser.parse("")
        assertTrue(parsed.isEmpty())

        val parsedWhitespaces = LrcParser.parse("   \n  ")
        assertTrue(parsedWhitespaces.isEmpty())
    }
}
