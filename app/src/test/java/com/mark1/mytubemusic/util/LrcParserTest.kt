package com.mark1.mytubemusic.util

import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun getLyricsFromFile_missingFile_returnsEmptyList() {
        val nonExistentPath = "this_file_does_not_exist_12345.lrc"
        val result = LrcParser.getLyricsFromFile(nonExistentPath)

        assertTrue(
            "Expected an empty list for a non-existent file",
            result.isEmpty()
        )
    }
}
