## 2024-05-24 - LrcParser Regex Replacement
**Learning:** Parsing LRC files (which have highly predictable formatting) using `Regex` in a line-by-line loop is a significant bottleneck due to the creation of `MatchResult` objects and the general overhead of regex engines.
**Action:** Replaced `Regex` with manual index-based character matching and extraction, dropping parse times for 1,000 lines from ~1327 ms down to ~808 ms (~39% faster). I will look for other predictable string formats currently using Regex that could be swapped to index parsing.
