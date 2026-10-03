#!/usr/bin/env python3
"""Move each quiz key to a target letter by swapping two options (and relabelling the folded key), so key
letters are spread. Deterministic; idempotent once the targets are met.
usage: tools/balance_keys.py
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_quiz import ROOT  # noqa: E402

TARGETS = "cadbdbcaabdcbdac"


def relabel(par, mapping):
    return re.sub(r"(\*\*|\*)([a-d])\1", lambda m: m.group(1) + mapping.get(m.group(2), m.group(2)) + m.group(1), par)


def process(path, counter):
    md = path.read_text()
    out = []
    pos = 0
    for m in re.finditer(r"^## (Quiz|Module quiz|Mock exam)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        body = m.group(2)
        qblock, sep, keyblock = body.partition("<details>")
        qs = list(re.finditer(r"^(\d+)\. (.*?)(?=^\d+\. |\Z)", qblock, re.S | re.M))
        kps = list(re.finditer(r"^(\d+)\. (\*\*[a-d]\*\*.*?)(?=^\d+\. |\n</details>|\Z)", keyblock, re.S | re.M))
        new_q, new_k = qblock, keyblock
        for q, kp in zip(reversed(qs), reversed(kps)):
            idx = counter[0] + len(qs) - 1 - qs.index(q)
            target = TARGETS[idx % len(TARGETS)]
            key = re.match(r"\*\*([a-d])\*\*", kp.group(2)).group(1)
            if key == target:
                continue
            mapping = {key: target, target: key}
            lines = q.group(0).split("\n")
            opt_idx = {}
            for i, line in enumerate(lines):
                om = re.match(r"(\s*- \*\*)([a-d])(\*\*: .*)", line)
                if om:
                    opt_idx[om.group(2)] = i
            la, lb = opt_idx[key], opt_idx[target]
            ta = re.match(r"(\s*- \*\*)([a-d])(\*\*: .*)", lines[la])
            tb = re.match(r"(\s*- \*\*)([a-d])(\*\*: .*)", lines[lb])
            lines[la] = ta.group(1) + target + ta.group(3)
            lines[lb] = tb.group(1) + key + tb.group(3)
            lines[la], lines[lb] = lines[lb], lines[la]
            new_q = new_q.replace(q.group(0), "\n".join(lines))
            new_k = new_k.replace(kp.group(0), kp.group(1) + ". " + relabel(kp.group(2), mapping))
        counter[0] += len(qs)
        out.append((m.start(2), m.end(2), new_q + sep + new_k))
    for start, end, text in reversed(out):
        md = md[:start] + text + md[end:]
    path.write_text(md)


ONLY = sys.argv[1:]  # folder-name prefixes to balance; none means every module folder
for folder in sorted(p for p in (ROOT / "course").iterdir() if p.is_dir() and re.match(r"(0[1-9]|[12][0-9]|3[0-9]|4[01])-", p.name)):
    if ONLY and not any(folder.name.startswith(a) for a in ONLY):
        continue
    counter = [0]
    for page in sorted(folder.glob("*.md")):
        process(page, counter)
print("balanced")
