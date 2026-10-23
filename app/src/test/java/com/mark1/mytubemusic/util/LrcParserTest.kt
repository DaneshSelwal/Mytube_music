package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LrcParserTest {

    @Test
    fun parse_validLrcContent_returnsParsedLines() {
        val lrcContent = """
            [00:10.50]First line
            [01:20.300]Second line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(2, result.size)

        // 00:10.50 = 10 sec + 500 ms = 10500 ms
        assertEquals(10500L, result[0].startTimeMs)
        assertEquals("First line", result[0].text)

        // 01:20.300 = 60000 + 20000 + 300 = 80300 ms
        assertEquals(80300L, result[1].startTimeMs)
        assertEquals("Second line", result[1].text)
    }

    @Test
    fun parse_ignoresInvalidLines() {
        val lrcContent = """
            [invalid format]
            just some text
            [00:10.50]Valid line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(1, result.size)
        assertEquals(10500L, result[0].startTimeMs)
        assertEquals("Valid line", result[0].text)
    }

    @Test
    fun parse_sortsLinesByTime() {
        val lrcContent = """
            [01:20.300]Second line
            [00:10.50]First line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(2, result.size)
        assertEquals("First line", result[0].text)
        assertEquals("Second line", result[1].text)
    }

    @Test
    fun parse_ignoresEmptyTextLines() {
        val lrcContent = """
            [00:10.50]
            [00:11.50]
            [00:12.50]Valid line
        """.trimIndent()

        val result = LrcParser.parse(lrcContent)

        assertEquals(1, result.size)
        assertEquals("Valid line", result[0].text)
    }

    @Test
    fun getLyricsFromFile_withValidFile_returnsLyrics() {
        val tempFile = File.createTempFile("test", ".lrc")
        tempFile.writeText("[00:10.50]First line")

        try {
            val result = LrcParser.getLyricsFromFile(tempFile.absolutePath)
            assertEquals(1, result.size)
            assertEquals("First line", result[0].text)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun getLyricsFromFile_withInvalidPath_returnsEmptyList() {
        val result = LrcParser.getLyricsFromFile("non_existent_file.lrc")
        assertTrue(result.isEmpty())
    }
}
