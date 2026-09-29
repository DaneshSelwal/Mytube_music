## 2024-06-25 - Avoid String Allocations in Parsing Highly Predictable Formats
**Learning:** In `LrcParser`, using `Regex` and `substring()` for parsing LRC format timestamps `[mm:ss.xx]` inside a loop caused significant performance overhead and unnecessary memory allocations.
**Action:** Replace `Regex` with manual character checking (e.g., `line[index] == '['`) and compute integer values using primitive character arithmetic (e.g., `(line[index] - '0') * 10`). This simple change provides measurable performance gains in tight loops.
