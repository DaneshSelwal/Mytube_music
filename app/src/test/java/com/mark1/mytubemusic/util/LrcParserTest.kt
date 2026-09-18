package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun testParse() {
        val sampleLrc = """
            [00:12.00]Line 1
            [01:23.45]Line 2
            [01:23.456]Line 3
            Some random text
            [invalid] text
            [00:00.00]
            [12:34.56]   Line 4
        """.trimIndent()

        val parsed = LrcParser.parse(sampleLrc)
        assertEquals(4, parsed.size)
        assertEquals(12000L, parsed[0].startTimeMs)
        assertEquals("Line 1", parsed[0].text)
        assertEquals(83450L, parsed[1].startTimeMs)
        assertEquals("Line 2", parsed[1].text)
        assertEquals(83456L, parsed[2].startTimeMs)
        assertEquals("Line 3", parsed[2].text)
        assertEquals(754560L, parsed[3].startTimeMs)
        assertEquals("Line 4", parsed[3].text)
    }
}
