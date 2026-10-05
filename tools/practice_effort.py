#!/usr/bin/env python3
"""How much code a learner has to write in each practice, per language.

The number is the lines of the reference solution minus the lines of the starter, counting only non-blank, non-comment
lines (a Python docstring and a /* */ block count as comments), summed over the files of the practice and never below
zero for one file. The starter is a scaffold: everything that is plumbing is already in it, so the number is the size
of the decision points the learner finishes.

usage: tools/practice_effort.py [THRESHOLD] [--modules REGEX] [--quiet]
  THRESHOLD        a practice breaks the limit when any language needs more than this many lines (default 30)
  --modules REGEX  only the modules whose folder name matches (default: the L2_MODULES environment variable, else all)
  --quiet          print only the practices over the threshold
Exit status: 0 when every practice is within the threshold, 1 when at least one is over it, 2 on a usage error.
"""
import argparse
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANGUAGES = ("python", "typescript", "java", "kotlin")
HASH_COMMENT = {".py", ".sh", ".yml", ".yaml", ".toml"}
SLASH_COMMENT = {".ts", ".tsx", ".js", ".java", ".kt", ".kts"}
CODE_SUFFIXES = HASH_COMMENT | SLASH_COMMENT | {".md", ".json", ".txt", ".xml"}


def code_lines(path):
    """The number of non-blank, non-comment lines of one file."""
    text = path.read_text(errors="replace")
    suffix = path.suffix
    if suffix == ".py":
        text = re.sub(r'""".*?"""', "", text, flags=re.S)
        text = re.sub(r"'''.*?'''", "", text, flags=re.S)
    if suffix in SLASH_COMMENT:
        text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    count = 0
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        if suffix in HASH_COMMENT and stripped.startswith("#"):
            continue
        if suffix in SLASH_COMMENT and stripped.startswith("//"):
            continue
        count += 1
    return count


def tree(folder):
    """{relative path: code lines} for the files under a folder."""
    return {str(p.relative_to(folder)): code_lines(p) for p in sorted(folder.rglob("*"))
            if p.is_file() and p.suffix in CODE_SUFFIXES and ".build" not in p.parts and "node_modules" not in p.parts}


def lines_to_write(language_dir):
    """Lines the learner writes in one language folder, or None when it has no starter and reference."""
    starter, reference = language_dir / "starter", language_dir / "reference"
    if not (starter.is_dir() and reference.is_dir()):
        return None
    given, solution = tree(starter), tree(reference)
    return sum(max(0, lines - given.get(name, 0)) for name, lines in solution.items())


def practices(modules):
    for module in sorted((ROOT / "exercises").iterdir()):
        if module.is_dir() and modules.search(module.name):
            for spec in sorted(module.glob("*/*/cases.json")):
                yield spec.parent


def main(argv):
    parser = argparse.ArgumentParser(description="lines a learner writes per practice and language")
    parser.add_argument("threshold", nargs="?", type=int, default=30)
    parser.add_argument("--modules", default=os.environ.get("L2_MODULES", ""))
    parser.add_argument("--quiet", action="store_true")
    args = parser.parse_args(argv)
    modules = re.compile(args.modules)
    over = 0
    total = 0
    print(f"{'practice':<72}" + "".join(f"{lang:>12}" for lang in LANGUAGES))
    for folder in practices(modules):
        counts = {lang: lines_to_write(folder / lang) for lang in LANGUAGES}
        total += 1
        broken = [lang for lang, n in counts.items() if n is not None and n > args.threshold]
        if broken:
            over += 1
        if args.quiet and not broken:
            continue
        cells = "".join(f"{('-' if n is None else n):>12}" for n in counts.values())
        print(f"{str(folder.relative_to(ROOT / 'exercises')):<72}{cells}" + (f"   OVER {args.threshold}: {', '.join(broken)}" if broken else ""))
    print(f"{total} practices, {over} over {args.threshold} lines")
    return 1 if over else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
