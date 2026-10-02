## 2024-05-24 - LrcParser Regex Overhead
**Learning:** Regex parsing inside a hot loop (like line-by-line LRC parsing) creates significant CPU overhead due to pattern matching and string allocations.
**Action:** When formats are highly predictable (like `[mm:ss.xx]`), use manual character indexing and math (`(char - '0') * 10`) instead of Regex and `substring()`. Pre-allocate collection sizes (`ArrayList(lines.size)`) to prevent backing array reallocations. Always keep a fallback to Regex for malformed lines.
