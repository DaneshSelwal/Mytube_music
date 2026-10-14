import time
import re

def parse_regex(lrc_content):
    lines = lrc_content.splitlines()
    lyrics = []
    time_pattern = re.compile(r"\[(\d{2}):(\d{2})\.(\d{2,3})\]")

    for line in lines:
        match = time_pattern.search(line)
        if match:
            min = int(match.group(1))
            sec = int(match.group(2))
            ms_str = match.group(3)
            if len(ms_str) == 2: ms_str += "0"
            ms = int(ms_str)

            time_in_ms = (min * 60 * 1000) + (sec * 1000) + ms
            text = line[match.end():].strip()
            if text:
                lyrics.append((time_in_ms, text))
    return sorted(lyrics, key=lambda x: x[0])

def parse_manual(lrc_content):
    lines = lrc_content.splitlines()
    # PRE-ALLOCATION: We can guess the capacity based on lines length
    lyrics = [] # Cannot pre-allocate in python easily but in Kotlin we can ArrayList<LyricLine>(lines.size)

    for line in lines:
        if len(line) < 10 or line[0] != '[' or line[3] != ':' or line[6] != '.':
            continue

        try:
            # We assume format [mm:ss.xx] or [mm:ss.xxx]
            close_bracket = line.find(']', 7)
            if close_bracket == -1: continue

            min = int(line[1:3])
            sec = int(line[4:6])
            ms_str = line[7:close_bracket]
            if len(ms_str) == 2: ms_str += "0"
            ms = int(ms_str)

            time_in_ms = (min * 60 * 1000) + (sec * 1000) + ms

            # Substring without allocation
            text = line[close_bracket+1:].strip()
            if text:
                lyrics.append((time_in_ms, text))
        except ValueError:
            pass

    return sorted(lyrics, key=lambda x: x[0])

lrc = "\n".join(f"[01:23.45] This is lyric line {i}" for i in range(1, 1001))

# Warmup
for _ in range(50):
    parse_regex(lrc)
    parse_manual(lrc)

# Test Regex
start = time.time()
for _ in range(100):
    parse_regex(lrc)
end = time.time()
print(f"Regex Parser Time: {(end - start) * 1000:.2f} ms")

# Test Manual
start = time.time()
for _ in range(100):
    parse_manual(lrc)
end = time.time()
print(f"Manual Parser Time: {(end - start) * 1000:.2f} ms")
