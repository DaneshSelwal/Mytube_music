## 2026-10-07 - LrcParser Manual Parsing
**Learning:** Parsing LRC files with `Regex` inside a loop can be a performance bottleneck due to the overhead of regular expression matching and string instantiation.
**Action:** Replace `Regex` with manual character index parsing and primitive math calculations for predictable formats (like LRC timestamps) to significantly reduce execution time (from ~2056ms to ~1557ms for 1000 lines).

## 2026-10-07 - LrcParser Manual Parsing
**Learning:** Parsing LRC files with `Regex` inside a loop can be a performance bottleneck due to the overhead of regular expression matching and string instantiation. Also `startsWith` can fail if there is a UTF-8 Byte Order Mark, using `indexOf` is safer.
**Action:** Replace `Regex` with manual character index parsing using `indexOf` and primitive math calculations for predictable formats (like LRC timestamps) to significantly reduce execution time.
