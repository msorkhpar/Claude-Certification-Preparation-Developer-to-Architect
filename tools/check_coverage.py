#!/usr/bin/env python3
"""Coverage check: every exam-map topic has a module; every module names only defined codes.

Reads docs/EXAM-MAP.md (definitions) and docs/COURSE-OUTLINE.md (module tables).
Code X (beyond the exam blueprints) is valid when the map defines it; no module is required
per X. Exit 0 when both directions hold, 1 otherwise. Standard library only.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CODE = r"(?:DV[1-9]|A[1-9]\.[1-9]|A[1-9]|S[1-9]|P[1-9]|AS|X)"
CODE_RE = re.compile(r"\b" + CODE + r"\b")
RANGE_RE = re.compile(r"\b(" + CODE + r")\s+to\s+(" + CODE + r")\b")
LEAF_RE = re.compile(r"^(?:DV\d|A\d\.\d|S\d|P\d)$")


def defined_codes(map_text: str) -> set:
    codes = set()
    for line in map_text.splitlines():
        m = re.match(r"\|\s*((?:DV|A|P)\d)\s", line)  # domain rows
        if m:
            codes.add(m.group(1))
        if re.match(r"\|\s*X\s+Beyond", line):  # the "beyond the exam blueprints" row
            codes.add("X")
        if line.startswith("|") and re.match(r"\|\s*A\d\s", line):
            codes.update(re.findall(r"\b(A\d\.\d)\b", line))
    codes.update(re.findall(r"\b(S\d)\s+(?!to\b)[A-Za-z]", map_text))  # scenario list "S1 customer ..."
    codes.add("AS")
    return codes


def expand(a: str, b: str) -> list:
    pa, pb = re.match(r"([A-Z]+)(\d)", a), re.match(r"([A-Z]+)(\d)", b)
    if not (pa and pb and pa.group(1) == pb.group(1)):
        return [a, b]
    return [f"{pa.group(1)}{i}" for i in range(int(pa.group(2)), int(pb.group(2)) + 1)]


def modules(outline_text: str) -> dict:
    """module number -> (title, exams cell). Rows look like: | 12 | Title | Covers | Exams | Practice |"""
    out = {}
    for line in outline_text.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) >= 5 and cells[0].isdigit():
            out[int(cells[0])] = (cells[1], cells[3])
    return out


def check(map_text: str, outline_text: str) -> list:
    problems = []
    defined = defined_codes(map_text)
    mods = modules(outline_text)
    if not mods:
        return ["no modules found in the outline"]
    covered = set()
    for num, (title, cell) in sorted(mods.items()):
        ranged = set()
        for a, b in RANGE_RE.findall(cell):
            for c in expand(a, b):
                ranged.add(c)
                if c not in defined:
                    problems.append(f"module {num} ({title}): range names undefined code {c}")
        for c in CODE_RE.findall(cell):
            if c in defined:
                # a code inside a range is a mock-exam span and does not count as coverage
                if not (c in ranged and not re.search(r"(?<!to )\b" + re.escape(c) + r"\b(?! to)", cell)):
                    covered.add(c)
            else:
                problems.append(f"module {num} ({title}): undefined code {c}")
    for code in sorted(defined):
        if LEAF_RE.match(code) and code not in covered:
            problems.append(f"topic {code} has no module")
    return problems


def main() -> int:
    map_text = (ROOT / "docs" / "EXAM-MAP.md").read_text(encoding="utf-8")
    outline_text = (ROOT / "docs" / "COURSE-OUTLINE.md").read_text(encoding="utf-8")
    problems = check(map_text, outline_text)
    n_def = len([c for c in defined_codes(map_text) if LEAF_RE.match(c)])
    for p in problems:
        print("FAIL:", p)
    print(f"{n_def} topic codes, {len(modules(outline_text))} modules, {len(problems)} problems")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
