#!/usr/bin/env python3
"""Write cases.json for the practices of modules 30 to 35, 38 to 43 and 45 to 56 from one specification, and check that
every test the file names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
The cases.json of modules 6 and 13 to 29 is written by hand and has no entry here.
usage: tools/make_cases.py [--modules REGEX]
  --modules REGEX   only the practices whose module folder name matches (default: all), for example '^(3[6-9]|4[01])-'
"""
import argparse
import json
import re
import sys
from pathlib import Path

from modular_data import load

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# The data lives one file per module in tools/cases.d/<module folder>.py; a batch edits only its own module's file.
# practice dir -> {"name", "tests": {java, kotlin suite names}, "langs", "cases": [(id, kind, title)], "plants": {name: (caught_by, defect)}}
PRACTICES = load(ROOT / "tools" / "cases.d", "PRACTICES", {})


def camel(title):
    words = title.split()
    return words[0] + "".join(w[:1].upper() + w[1:] for w in words[1:])


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modules", default=".*")
    rx = re.compile(ap.parse_args().modules)
    selected = {p: s for p, s in PRACTICES.items() if rx.match(p.split("/")[1])}
    if not selected:
        sys.exit("no practice matches")
    problems = 0
    for practice, spec in selected.items():
        base = ROOT / practice
        cases, tests = {}, {}
        for cid, kind, title in spec["cases"]:
            cases[cid] = {"kind": kind, "python": f"test_{cid}_" + title.replace(" ", "_"), "typescript": f"{cid} {title}",
                          "java": f"{cid}_{camel(title)}", "kotlin": f"{cid}_{camel(title)}"}
        data = {"practice": practice.split(f"{X}/", 1)[1], "name": spec["name"], "tests": {"java": spec["suite"], "kotlin": spec["suite"]},
                "cases": {cid: {k: v for k, v in c.items() if k in ("kind",) + tuple(spec["langs"])} for cid, c in cases.items()},
                "plants": {n: {"caught_by": caught, "defect": defect} for n, (caught, defect) in spec["plants"].items()}}
        if spec["langs"] == ["python", "typescript"]:
            data.pop("tests")
        elif spec["langs"] != ["python", "typescript", "java", "kotlin"]:
            sys.exit(f"{practice}: unsupported languages {spec['langs']}")
        (base / "cases.json").write_text(json.dumps(data, indent=2) + "\n")
        for lang in spec["langs"]:
            found = {"python": next(base.glob("python/tests/*.py"), None), "typescript": next(base.glob("typescript/tests/*.test.ts"), None),
                     "java": next(base.glob("java/tests/*Test.java"), None), "kotlin": next(base.glob("kotlin/tests/*Test.kt"), None)}[lang]
            text = found.read_text() if found else ""
            for cid, c in cases.items():
                name = c[lang]
                ok = (f"def {name}(" in text) if lang == "python" else (f'test("{name}"' in text) if lang == "typescript" else (f"void {name}(" in text or f"fun {name}(" in text)
                if not ok:
                    print(f"{practice}/{lang}: test {name!r} not found")
                    problems += 1
        print(f"{practice}: cases.json written ({len(cases)} cases, {len(spec['plants'])} plants)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
