#!/usr/bin/env python3
"""List the Level 2 practices (folders that hold a cases.json under exercises/<module>/) for the runner and the grader.
usage: tools/l2_practices.py list      one line per practice: <practice dir> <variant> <variant> ...
"""
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
# L2_MODULES narrows the run to one batch, for example L2_MODULES='^(1[89]|2[0-3])-'
MODULES = re.compile(os.environ.get("L2_MODULES", r"^(1[2-9]|2[0-9])-"))


def practices():
    """[(dir relative to the repository root, parsed cases.json)] in module order."""
    found = []
    for module in sorted((ROOT / "exercises").iterdir()):
        if module.is_dir() and MODULES.match(module.name):
            for spec in sorted(module.glob("*/*/cases.json")):
                found.append((str(spec.parent.relative_to(ROOT)), json.loads(spec.read_text())))
    return found


def variants(cases):
    return ["starter", "reference"] + list(cases["plants"])


if __name__ == "__main__":
    if sys.argv[1:] == ["list"]:
        for path, cases in practices():
            print(path, *variants(cases))
    else:
        sys.exit(__doc__)
