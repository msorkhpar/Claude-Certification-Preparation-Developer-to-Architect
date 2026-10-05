#!/usr/bin/env python3
"""Materialise the planted wrong solutions of every practice from their reference solutions.

A plant is never committed as files. Each plant is an ordered list of exact replacements against the
practice's reference (tools/plants.d/, one file per module); this tool copies the reference into
<language>/<plant name>/ and applies the replacements. It fails when a pattern is missing or occurs more
than once in the text it is applied to, when a plant equals the reference, or when the plants of a
language differ from the ones the practice's cases.json names. The generated plant folders are git-ignored.

usage: tools/make_plants.py [--modules REGEX] [--out DIR] [--list]
  --modules REGEX   only the practices whose module folder name matches (default: all), for example '^(1[89]|2[0-3])-'
  --out DIR         write <DIR>/<practice>/<language>/<plant>/ instead of next to the reference
  --list            print one line per plant (practice, language, plant) and write nothing
"""
import argparse
import json
import re
import shutil
import sys
from pathlib import Path

from modular_data import load

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"



def both(file, plants):
    """The same edits for the Python, TypeScript, Java and Kotlin folders: a project set-up is the same files in all four."""
    return {lang: (file, plants) for lang in ("python", "typescript", "java", "kotlin")}


# practice dir -> language -> (main file, {plant: [(old, new), ...]}); a plant may instead map several files to their lists.
# The data lives one file per module in tools/plants.d/<module folder>.py; a batch edits only its own module's file.
PLANTS = load(ROOT / "tools" / "plants.d", "PLANTS", {"both": both})


def selected(modules):
    rx = re.compile(modules)
    return {p: v for p, v in PLANTS.items() if rx.match(p.split("/")[1])}


def apply(practice, lang, name, edits, fname, ref):
    """Text of every file the plant changes: {file: new text}; exits on a missing or ambiguous pattern."""
    by_file = edits if isinstance(edits, dict) else {fname: edits}
    changed = {}
    for target, pairs in by_file.items():
        original = (ref / target).read_text()
        text = original
        for n, (old, new) in enumerate(pairs, 1):
            hits = text.count(old)
            if hits != 1:
                sys.exit(f"{practice}/{lang}/{name}: replacement {n} of {target}: pattern found {hits} times (need exactly 1): {old!r}")
            text = text.replace(old, new)
        if text == original:
            sys.exit(f"{practice}/{lang}/{name}: plant equals the reference in {target}")
        changed[target] = text
    return changed


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--modules", default=".*")
    ap.add_argument("--out")
    ap.add_argument("--list", action="store_true")
    args = ap.parse_args()
    plants = selected(args.modules)
    if not plants:
        sys.exit(f"no practice matches {args.modules!r}")
    made = 0
    for practice, langs in plants.items():
        cases_file = ROOT / practice / "cases.json"
        for lang, (fname, specs) in langs.items():
            if cases_file.exists():
                named = sorted(json.loads(cases_file.read_text())["plants"])
                if sorted(specs) != named:
                    sys.exit(f"{practice}/{lang}: plants {sorted(specs)} differ from cases.json {named}")
            ref = ROOT / practice / lang / "reference"
            for name, edits in specs.items():
                if args.list:
                    print(practice, lang, name)
                    continue
                changed = apply(practice, lang, name, edits, fname, ref)
                base = (Path(args.out) / practice if args.out else ROOT / practice) / lang
                dest = base / name
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(ref, dest)
                for target, text in changed.items():
                    (dest / target).write_text(text)
                made += 1
    if not args.list:
        print(f"made {made} planted wrong solutions")


if __name__ == "__main__":
    main()
