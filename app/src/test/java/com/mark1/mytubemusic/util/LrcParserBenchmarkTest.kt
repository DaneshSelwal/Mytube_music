package com.mark1.mytubemusic.util

import org.junit.Test
import kotlin.system.measureTimeMillis

class LrcParserBenchmarkTest {
    @Test
    fun testPerformance() {
        val lrc = StringBuilder()
        for (i in 0..1000) {
            val min = String.format("%02d", i % 60)
            val sec = String.format("%02d", (i * 2) % 60)
            val ms = String.format("%02d", i % 100)
            lrc.append("[$min:$sec.$ms] Line $i lyric text here\n")
        }
        val lrcContent = lrc.toString()

        // Warmup
        for (i in 0..10) {
            LrcParser.parse(lrcContent)
        }

        val time = measureTimeMillis {
            for (i in 0..100) {
                LrcParser.parse(lrcContent)
            }
        }
        println("Original time: $time ms")
    }
}
