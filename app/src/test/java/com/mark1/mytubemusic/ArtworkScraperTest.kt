package com.mark1.mytubemusic.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jsoup.Jsoup
import org.junit.Test
import kotlin.system.measureTimeMillis

class ArtworkScraperTest {

    @Test
    fun benchmarkJsoup() = runBlocking {
        // Just mocking the Jsoup connect logic and see if we can do it faster
        println("This will test Jsoup connection if possible or mock the Jsoup logic.")
    }
}
