package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse valid standard format with milliseconds`() {
        val lrc = "[01:23.456]Hello World"
        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        // 1 min = 60000ms
        // 23 sec = 23000ms
        // 456 ms = 456ms
        // Total = 83456
        assertEquals(83456L, result[0].startTimeMs)
        assertEquals("Hello World", result[0].text)
    }

    @Test
    fun `parse valid format with centiseconds`() {
        val lrc = "[01:23.45]Hello World"
        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        // 1 min = 60000ms
        // 23 sec = 23000ms
        // 45 cs = 450 ms
        // Total = 83450
        assertEquals(83450L, result[0].startTimeMs)
        assertEquals("Hello World", result[0].text)
    }

    @Test
    fun `parse and sort by start time`() {
        val lrc = """
            [01:00.00]Second Line
            [00:30.00]First Line
            [01:30.00]Third Line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals("First Line", result[0].text)
        assertEquals("Second Line", result[1].text)
        assertEquals("Third Line", result[2].text)
    }

    @Test
    fun `ignore lines with no timings`() {
        val lrc = """
            [ti:Title]
            [ar:Artist]
            [00:30.00]First Line
            Some metadata or empty line without brackets
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals("First Line", result[0].text)
        assertEquals(30000L, result[0].startTimeMs)
    }

    @Test
    fun `ignore empty lyrics`() {
        val lrc = """
            [00:30.00]
            [01:00.00]
            [01:30.00]Valid Line
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals("Valid Line", result[0].text)
        assertEquals(90000L, result[0].startTimeMs)
    }
}
