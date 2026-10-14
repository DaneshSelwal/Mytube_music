package com.mark1.mytubemusic

import org.junit.Test

data class LyricLine(
    val startTimeMs: Long,
    val text: String
)

object LrcParserOriginal {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        val lyrics = mutableListOf<LyricLine>()
        val timePattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

        for (line in lines) {
            val matchResult = timePattern.find(line)
            if (matchResult != null) {
                val min = matchResult.groupValues[1].toLong()
                val sec = matchResult.groupValues[2].toLong()
                var msStr = matchResult.groupValues[3]
                if (msStr.length == 2) msStr += "0" // handle centiseconds
                val ms = msStr.toLong()

                val timeInMs = (min * 60 * 1000) + (sec * 1000) + ms
                val text = line.substring(matchResult.range.last + 1).trim()
                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timeInMs, text))
                }
            }
        }
        return lyrics.sortedBy { it.startTimeMs }
    }
}

object LrcParserOptimized {
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = lrcContent.lines()
        // ⚡ Bolt: Pre-allocate list capacity to avoid backing array reallocations
        val lyrics = ArrayList<LyricLine>(lines.size)

        for (line in lines) {
            // ⚡ Bolt: Fast path check using indexOf to bypass BOM, avoiding regex and trim allocations
            val openIdx = line.indexOf('[')
            if (openIdx == -1 || line.length < openIdx + 10) continue
            if (line[openIdx + 3] != ':' || line[openIdx + 6] != '.') continue

            val closeIdx = line.indexOf(']', openIdx + 7)
            if (closeIdx == -1) continue

            try {
                // ⚡ Bolt: Primitive parsing avoids Regex and group allocation overhead
                val min = (line[openIdx + 1] - '0') * 10 + (line[openIdx + 2] - '0')
                val sec = (line[openIdx + 4] - '0') * 10 + (line[openIdx + 5] - '0')

                var ms = 0
                val msLen = closeIdx - (openIdx + 7)
                if (msLen == 2) {
                    ms = ((line[openIdx + 7] - '0') * 10 + (line[openIdx + 8] - '0')) * 10
                } else if (msLen == 3) {
                    ms = (line[openIdx + 7] - '0') * 100 + (line[openIdx + 8] - '0') * 10 + (line[openIdx + 9] - '0')
                } else {
                    continue // Unsupported ms format
                }

                val timeInMs = (min * 60 * 1000L) + (sec * 1000L) + ms
                val text = line.substring(closeIdx + 1).trim()

                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timeInMs, text))
                }
            } catch (e: Exception) {
                // Fallback or ignore malformed lines
            }
        }
        return lyrics.sortedBy { it.startTimeMs }
    }
}

class LrcParserBenchmarkTest {
    @Test
    fun benchmarkLrcParser() {
        val lrcContent = StringBuilder().apply {
            append("\uFEFF[00:00.00] BOM start\n")
            append(" [01:00.00] leading space\n")
            for (i in 1..1000) {
                append("[01:23.45] This is lyric line $i\n")
            }
            // Add some malformed lines to test robustness
            append("This is not a lyric line\n")
            append("[99:99.999] Test\n")
        }.toString()

        // Warmup
        for (i in 1..50) {
            LrcParserOriginal.parse(lrcContent)
            LrcParserOptimized.parse(lrcContent)
        }

        System.gc()
        Thread.sleep(100)

        val startOriginal = System.nanoTime()
        for (i in 1..200) {
            LrcParserOriginal.parse(lrcContent)
        }
        val endOriginal = System.nanoTime()

        System.gc()
        Thread.sleep(100)

        val startOptimized = System.nanoTime()
        for (i in 1..200) {
            LrcParserOptimized.parse(lrcContent)
        }
        val endOptimized = System.nanoTime()

        val durationOriginalMs = (endOriginal - startOriginal) / 1_000_000
        val durationOptimizedMs = (endOptimized - startOptimized) / 1_000_000

        println("=====================================================")
        println("Original Regex Parser Time: $durationOriginalMs ms")
        println("Optimized Manual Parser Time: $durationOptimizedMs ms")
        println("=====================================================")

        // Assert correctness
        val result1 = LrcParserOriginal.parse(lrcContent)
        val result2 = LrcParserOptimized.parse(lrcContent)
        assert(result1.size == result2.size) { "Sizes mismatch: ${result1.size} vs ${result2.size}" }
        for (i in result1.indices) {
            assert(result1[i] == result2[i]) { "Mismatch at $i: \nOrig: ${result1[i]} \nOpt:  ${result2[i]}" }
        }
    }
}
