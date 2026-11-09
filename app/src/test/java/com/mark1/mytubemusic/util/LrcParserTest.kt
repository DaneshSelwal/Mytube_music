package com.mark1.mytubemusic.util

import org.junit.Test
import org.junit.Assert.*

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [00:12.34] Hello
            [01:02.345] World
            [invalid] Ignore this
            [00:10.00]First
            Some invalid line without brackets
            [0a:12.34] Not a digit
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)
        assertEquals(3, lyrics.size)
        assertEquals("First", lyrics[0].text)
        assertEquals(10000L, lyrics[0].startTimeMs)
        assertEquals("Hello", lyrics[1].text)
        assertEquals(12340L, lyrics[1].startTimeMs)
        assertEquals("World", lyrics[2].text)
        assertEquals(62345L, lyrics[2].startTimeMs)
    }
}
