package com.mark1.mytubemusic.util

import org.junit.Test
import org.junit.Assert.assertTrue

class LrcParserTest {
    @Test
    fun `getLyricsFromFile returns empty list for non-existent file`() {
        val result = LrcParser.getLyricsFromFile("dummy/path/to/nowhere.lrc")
        assertTrue(result.isEmpty())
    }
}
