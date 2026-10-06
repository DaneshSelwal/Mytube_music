package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `toFormattedDuration with 0 milliseconds returns 00-00`() {
        val duration = 0L
        assertEquals("00:00", duration.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration with less than a minute returns correctly`() {
        val duration = 59000L // 59 seconds
        assertEquals("00:59", duration.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration with exactly one minute returns 01-00`() {
        val duration = 60000L // 1 minute
        assertEquals("01:00", duration.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration with more than a minute returns correctly`() {
        val duration = 61000L // 1 minute and 1 second
        assertEquals("01:01", duration.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration with exactly an hour returns 60-00`() {
        val duration = 3600000L // 60 minutes
        assertEquals("60:00", duration.toFormattedDuration())
    }

    @Test
    fun `toFormattedDuration with more than an hour returns correctly`() {
        val duration = 3661000L // 61 minutes and 1 second
        assertEquals("61:01", duration.toFormattedDuration())
    }
}
