package com.mark1.mytubemusic.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun toFormattedDuration_zeroMilliseconds_returnsZeroMinutesZeroSeconds() {
        val duration = 0L
        assertEquals("00:00", duration.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_lessThanOneMinute_returnsZeroMinutesAndSeconds() {
        val duration = 45000L // 45 seconds
        assertEquals("00:45", duration.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_exactMinute_returnsMinuteAndZeroSeconds() {
        val duration = 60000L // 1 minute
        assertEquals("01:00", duration.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_minutesAndSeconds_returnsFormattedString() {
        val duration = 125000L // 2 minutes and 5 seconds
        assertEquals("02:05", duration.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_moreThanTenMinutes_returnsFormattedString() {
        val duration = 645000L // 10 minutes and 45 seconds
        assertEquals("10:45", duration.toFormattedDuration())
    }

    @Test
    fun toFormattedDuration_moreThanOneHour_returnsFormattedString() {
        val duration = 3665000L // 61 minutes and 5 seconds
        assertEquals("61:05", duration.toFormattedDuration())
    }
}
