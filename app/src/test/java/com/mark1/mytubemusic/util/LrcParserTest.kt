package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [01:23.45] This is a lyric line
            [02:34.567] Another line with 3 digits
            Invalid line without bracket
            [00:00.00]
            [99:59.99] Last line
        """.trimIndent()

        val lyrics = LrcParser.parse(lrc)

        assertEquals(3, lyrics.size)

        assertEquals(83450L, lyrics[0].startTimeMs)
        assertEquals("This is a lyric line", lyrics[0].text)

        assertEquals(154567L, lyrics[1].startTimeMs)
        assertEquals("Another line with 3 digits", lyrics[1].text)

        assertEquals(5999990L, lyrics[2].startTimeMs)
        assertEquals("Last line", lyrics[2].text)
    }
}
