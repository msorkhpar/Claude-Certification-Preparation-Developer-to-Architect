#!/usr/bin/env python3
"""Fail when a starter, reference or lesson-example main file lacks the one-line logger declaration.

Checked, per language, the logic file the practice is named for (cases.json "name": gate -> gate.py, gate.ts, Gate.java, Gate.kt)
in <p>/<lang>/starter and <p>/<lang>/reference, and the main file each examples/*/example.json names. Data, tests, build files,
helpers and generated plants are not checked. The declaration is one line near the top of the file:
  Python      log = logging.getLogger(__name__)
  Java        private static final System.Logger LOG = System.getLogger(<Class>.class.getName());
  Kotlin      private val log = System.getLogger("<name>")
  TypeScript  const log = logger("<name>")        (import { logger } from "../logger.ts"; practices only)

usage: tools/check_logger.py [--modules REGEX]   (default: all modules; example folders match on their folder name)
Exit status: 0 when every checked file declares it, 1 and a list of the files otherwise.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DECL = {
    "python": re.compile(r"^log\s*=\s*logging\.getLogger\(__name__\)\s*$", re.M),
    "java": re.compile(r"^\s*private static final System\.Logger LOG = System\.getLogger\(\w+\.class\.getName\(\)\);\s*$", re.M),
    "kotlin": re.compile(r'^\s*private val log = System\.getLogger\("[^"]+"\)\s*$', re.M),
    "typescript": re.compile(r'^const log = logger\("[^"]+"\)\s*;?\s*$', re.M),
}
EXT = {"python": ".py", "typescript": ".ts", "java": ".java", "kotlin": ".kt"}


def main_file(folder, lang, name):
    """The logic file of a practice variant: the one whose name matches the practice name, else the only non-helper file."""
    want = name.replace("_", "").replace("-", "").lower()
    files = [f for f in sorted(folder.glob("*" + EXT[lang])) if f.is_file()]
    for f in files:
        if f.stem.replace("_", "").lower() == want:
            return f
    return files[0] if len(files) == 1 else None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--modules", default=".*")
    rx = re.compile(ap.parse_args().modules)
    bad, checked = [], 0
    for module in sorted((ROOT / "exercises").iterdir()):
        if not (module.is_dir() and rx.search(module.name)):
            continue
        for spec in sorted(module.glob("*/*/cases.json")):
            p = spec.parent
            name = json.loads(spec.read_text()).get("name", "")
            for lang in DECL:
                for variant in ("starter", "reference"):
                    folder = p / lang / variant
                    if not folder.is_dir():
                        continue
                    if not any(folder.glob("*" + EXT[lang])):
                        continue   # a configuration practice: the files to write are data, there is no source file to declare a logger in
                    f = main_file(folder, lang, name)
                    checked += 1
                    if f is None:
                        bad.append(f"{folder.relative_to(ROOT)}: no main file found for '{name}'")
                    elif not DECL[lang].search(f.read_text()):
                        bad.append(f"{f.relative_to(ROOT)}: no {lang} logger declaration")
    for spec in sorted((ROOT / "examples").glob("*/example.json")):
        if not rx.search(spec.parent.name):
            continue
        for lang, rel in json.loads(spec.read_text()).get("files", {}).items():
            f = spec.parent / lang / rel
            if lang in DECL and f.is_file():
                checked += 1
                if not DECL[lang].search(f.read_text()):
                    bad.append(f"{f.relative_to(ROOT)}: no {lang} logger declaration")
    for line in bad:
        print(line)
    print(f"check_logger: {checked} files checked, {len(bad)} without the declaration")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
