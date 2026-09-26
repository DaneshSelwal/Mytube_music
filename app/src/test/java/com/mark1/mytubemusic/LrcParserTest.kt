package com.mark1.mytubemusic

import com.mark1.mytubemusic.util.LrcParser
import com.mark1.mytubemusic.util.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Test

class LrcParserTest {

    @Test
    fun parse_validLrcContent() {
        val lrcContent = """
            [01:23.45] First line
            [02:34.567] Second line with 3 digit ms
            [03:45.67] Third line
        """.trimIndent()

        val lyrics = LrcParser.parse(lrcContent)

        assertEquals(3, lyrics.size)

        // 1 min = 60000ms, 23 sec = 23000ms, 450ms -> 83450ms
        assertEquals(LyricLine(83450L, "First line"), lyrics[0])

        // 2 min = 120000ms, 34 sec = 34000ms, 567ms -> 154567ms
        assertEquals(LyricLine(154567L, "Second line with 3 digit ms"), lyrics[1])

        // 3 min = 180000ms, 45 sec = 45000ms, 670ms -> 225670ms
        assertEquals(LyricLine(225670L, "Third line"), lyrics[2])
    }

    @Test
    fun parse_ignoreInvalidLines() {
        val lrcContent = """
            Invalid line without timestamp
            [01:23.45] Valid line
            [aa:bb.cc] Invalid timestamp format
            [02:34.56]
            [03:45.67] Another valid line
        """.trimIndent()

        val lyrics = LrcParser.parse(lrcContent)

        assertEquals(2, lyrics.size)
        assertEquals(LyricLine(83450L, "Valid line"), lyrics[0])
        assertEquals(LyricLine(225670L, "Another valid line"), lyrics[1])
    }

    @Test
    fun parse_sortsByTimestamp() {
        val lrcContent = """
            [02:34.56] Second
            [01:23.45] First
            [03:45.67] Third
        """.trimIndent()

        val lyrics = LrcParser.parse(lrcContent)

        assertEquals(3, lyrics.size)
        assertEquals("First", lyrics[0].text)
        assertEquals("Second", lyrics[1].text)
        assertEquals("Third", lyrics[2].text)
    }

    @Test
    fun parse_handlesTextBeforeTimestamp() {
        val lrcContent = "Some extra text [01:23.45] Actual lyric line"
        val lyrics = LrcParser.parse(lrcContent)

        assertEquals(1, lyrics.size)
        assertEquals(LyricLine(83450L, "Actual lyric line"), lyrics[0])
    }
}
