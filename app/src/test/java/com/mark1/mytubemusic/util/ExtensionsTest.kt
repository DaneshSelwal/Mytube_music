package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun toFormattedDuration_zero() {
        assertEquals("00:00", 0L.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_secondsOnly() {
        assertEquals("00:45", 45000L.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_minutesAndSeconds() {
        // 3 minutes and 15 seconds
        assertEquals("03:15", 195000L.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_exactMinutes() {
        // exactly 10 minutes
        assertEquals("10:00", 600000L.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_overAnHour() {
        // 65 minutes and 30 seconds
        assertEquals("65:30", 3930000L.toFormattedDuration())
    }
}
