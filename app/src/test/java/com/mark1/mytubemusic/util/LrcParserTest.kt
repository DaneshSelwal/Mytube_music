package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun testParse() {
        val lrc = """
            [00:12.00] Line 1
            [00:15.50] Line 2
            [00:17.500] Line 3
            [01:00.00] Line 4
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)

        assertEquals(4, lyrics.size)
        assertEquals(12000L, lyrics[0].startTimeMs)
        assertEquals("Line 1", lyrics[0].text)

        assertEquals(15500L, lyrics[1].startTimeMs)
        assertEquals("Line 2", lyrics[1].text)

        assertEquals(17500L, lyrics[2].startTimeMs)
        assertEquals("Line 3", lyrics[2].text)

        assertEquals(60000L, lyrics[3].startTimeMs)
        assertEquals("Line 4", lyrics[3].text)
    }
}
