package com.mark1.mytubemusic

import com.mark1.mytubemusic.util.YouTubeUrlParser
import org.junit.Assert.*
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun testExtractUrl() {
        val text = "Check out this song! https://music.youtube.com/watch?v=dQw4w9WgXcQ&feature=share"
        val extracted = YouTubeUrlParser.extractUrl(text)
        assertEquals("https://music.youtube.com/watch?v=dQw4w9WgXcQ&feature=share", extracted)
    }

    @Test
    fun testIsYouTubeUrl() {
        assertTrue(YouTubeUrlParser.isYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeUrlParser.isYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertTrue(YouTubeUrlParser.isYouTubeUrl("https://youtu.be/dQw4w9WgXcQ"))
        assertFalse(YouTubeUrlParser.isYouTubeUrl("https://google.com"))
    }

    @Test
    fun testExtractVideoId() {
        // Standard Watch link
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        
        // YouTube Music link
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ"))
        
        // Short links
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://youtu.be/dQw4w9WgXcQ?si=12345"))
        
        // Shorts link
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
        
        // Embed link
        assertEquals("dQw4w9WgXcQ", YouTubeUrlParser.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }
}
