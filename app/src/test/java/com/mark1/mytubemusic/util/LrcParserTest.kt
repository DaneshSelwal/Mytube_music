package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [00:12.00]Line 1
            [01:05.123] Line 2
            [01:06.12] Line 3
            [02:00.00]
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertEquals(3, lyrics.size)
        assertEquals(12000L, lyrics[0].startTimeMs)
        assertEquals("Line 1", lyrics[0].text)

        assertEquals(65123L, lyrics[1].startTimeMs)
        assertEquals("Line 2", lyrics[1].text)

        assertEquals(66120L, lyrics[2].startTimeMs)
        assertEquals("Line 3", lyrics[2].text)
    }
}
