#!/usr/bin/env python3
"""Compare what the Java and Kotlin editions of each example printed with what the Python edition printed, and check their test summaries.

Reads .survey-out/ex-<example dir>-<lang>-out.txt and ...-test.txt (written by tools/l2_run_all.sh and tools/l2_run_jvm_examples.sh).
Every Java and Kotlin output must equal the Python output, or the example must be listed in EXPLAINED with the reason the printed text differs
(the meaning is the same). An entry whose output turns out identical is a finding too, so the list cannot go stale.
usage: tools/compare_example_outputs.py [dir prefix ...]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / ".survey-out"
EXPLAINED = {
    "13-raw-http-vs-sdk": "the SDK names its own request headers and its error class (RateLimitException in the Java SDK)",
    "15-sdk-retries-and-errors": "error class names of the Java SDK (RateLimitException, InternalServerException, AnthropicIoException ...)",
    "16-bounded-concurrency": "error class name of the Java SDK (RateLimitException)",
    "17-streaming-events": "error class name of the Java SDK for an error event in a stream (SseException)",
    "33-streamable-http-mrtr": "the JVM MCP SDKs speak the 2025-11-25 revision, like the TypeScript edition; the Python edition speaks 2026-07-28",
}
SAME_AS_TYPESCRIPT = {"33-streamable-http-mrtr"}   # compared with the TypeScript output instead of the Python one


def main(prefixes):
    problems = 0
    rows = []
    for spec in sorted(ROOT.glob("examples/*/example.json")):
        d = spec.parent.name
        files = json.loads(spec.read_text())["files"]
        if prefixes and not any(d.startswith(p) for p in prefixes):
            continue
        for lang in ("java", "kotlin"):
            if lang not in files:
                continue
            got_f, test_f = OUT / f"ex-{d}-{lang}-out.txt", OUT / f"ex-{d}-{lang}-test.txt"
            if not got_f.exists() or not test_f.exists():
                print(f"{d} {lang}: no recorded output or test summary")
                problems += 1
                continue
            m = re.search(r"tests (\d+), passed (\d+), failed (\d+)", test_f.read_text())
            if not m or int(m.group(1)) == 0 or int(m.group(3)) != 0 or m.group(1) != m.group(2):
                print(f"{d} {lang}: tests not all passing ({test_f.read_text().strip()})")
                problems += 1
                continue
            ref_lang = "typescript" if d in SAME_AS_TYPESCRIPT else "python"
            ref_f = OUT / f"ex-{d}-{ref_lang}-out.txt"
            if not ref_f.exists():
                print(f"{d}: no {ref_lang} output to compare with")
                problems += 1
                continue
            same = got_f.read_text().rstrip("\n") == ref_f.read_text().rstrip("\n")
            if d in SAME_AS_TYPESCRIPT:
                verdict = f"same as {ref_lang}" if same else f"DIFFERENT from {ref_lang}"
                problems += 0 if same else 1
            elif same and d in EXPLAINED:
                verdict = "identical, but listed as different"
                problems += 1
            elif same:
                verdict = "identical"
            elif d in EXPLAINED:
                verdict = f"differs, explained: {EXPLAINED[d]}"
            else:
                verdict = "DIFFERENT and not explained"
                problems += 1
            rows.append(f"{d} {lang}: tests {m.group(2)}/{m.group(1)}; output {verdict}")
    print("\n".join(rows))
    print("example outputs:", "ok" if not problems else f"{problems} finding(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
