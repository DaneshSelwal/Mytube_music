package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {
    @Test
    fun testParse() {
        val lrc = """
            [01:23.45] Line 1
            [01:23.456] Line 2
            [00:00.00] Line 3
            Invalid line
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(3, parsed.size)
        assertEquals(0L, parsed[0].startTimeMs)
        assertEquals("Line 3", parsed[0].text)

        assertEquals(83450L, parsed[1].startTimeMs)
        assertEquals("Line 1", parsed[1].text)

        assertEquals(83456L, parsed[2].startTimeMs)
        assertEquals("Line 2", parsed[2].text)
    }
}
