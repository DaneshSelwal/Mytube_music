package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [00:12.00]Line 1
            [01:05.123]Line 2
            [02:10.50]Line 3
            invalid line
            [00:00.00]Line 0
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)

        assertEquals(4, lyrics.size)

        assertEquals(0L, lyrics[0].startTimeMs)
        assertEquals("Line 0", lyrics[0].text)

        assertEquals(12000L, lyrics[1].startTimeMs)
        assertEquals("Line 1", lyrics[1].text)

        assertEquals(65123L, lyrics[2].startTimeMs)
        assertEquals("Line 2", lyrics[2].text)

        assertEquals(130500L, lyrics[3].startTimeMs)
        assertEquals("Line 3", lyrics[3].text)
    }
}
