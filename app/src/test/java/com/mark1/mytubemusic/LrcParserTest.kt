package com.mark1.mytubemusic.util

import org.junit.Test
import org.junit.Assert.assertEquals

class LrcParserTest {

    @Test
    fun testParse() {
        val lrcContent = """
            [00:12.00]Line 1
            [00:15.123]Line 2
            [invalid] ignored
            [01:00.50]  Line 3
            prefix [01:05.10]Line 4
        """.trimIndent()

        val lyrics = LrcParser.parse(lrcContent)

        assertEquals(4, lyrics.size)
        assertEquals(12000L, lyrics[0].startTimeMs)
        assertEquals("Line 1", lyrics[0].text)

        assertEquals(15123L, lyrics[1].startTimeMs)
        assertEquals("Line 2", lyrics[1].text)

        assertEquals(60500L, lyrics[2].startTimeMs)
        assertEquals("Line 3", lyrics[2].text)

        assertEquals(65100L, lyrics[3].startTimeMs)
        assertEquals("Line 4", lyrics[3].text)
    }
}
