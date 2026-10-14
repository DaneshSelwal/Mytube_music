./gradlew testDebugUnitTest --tests "com.mark1.mytubemusic.LrcParserBenchmarkTest" > build_output.log 2>&1
cat build_output.log | grep -A 5 "Original Regex Parser Time" || true
find app/build/reports/tests/testDebugUnitTest -type f -name "*.html" -exec cat {} + | grep -E "Original Regex Parser Time|Optimized Manual Parser Time" || true
