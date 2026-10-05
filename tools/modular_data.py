"""Load the per-module data files of make_plants.py (plants.d/) and make_cases.py (cases.d/).

One file per module, named after the module folder under exercises/ (for example 41-security-and-safety.py).
A file is plain Python run with the shared tables and helpers in scope (PLANTS and X and both() for plants,
PRACTICES and X for cases); names it defines stay local to that file. Python keeps the comments and the
exact-replacement strings (f-strings, escapes, shared helper variables) as they were written.

The loader refuses, with the file names in the message:
  - a file whose name is not an existing module folder, or two files for the same module number;
  - a file that sets a practice outside its own module;
  - the same practice set twice (in one file or in two).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"


class Table(dict):
    """The practice -> data table; setting a key twice is an error naming where it happened."""

    def __init__(self, owner):
        super().__init__()
        self.owner = owner  # practice -> file stem that set it
        self.current = None

    def __setitem__(self, key, value):
        if key in self.owner:
            sys.exit(f"{self.current}: practice {key!r} is already claimed by {self.owner[key]}")
        module = key.split("/")[1] if key.count("/") >= 1 else key
        if module != self.current:
            sys.exit(f"{self.current}: practice {key!r} belongs to module {module!r}; each file holds only its own module")
        self.owner[key] = self.current
        super().__setitem__(key, value)


def load(folder, table_name, helpers):
    """Run every <folder>/*.py in name order; return the filled table."""
    owner, table = {}, None
    table = Table(owner)
    files = sorted(Path(folder).glob("*.py"))
    seen = {}
    for f in files:
        stem = f.stem
        if not (ROOT / X / stem).is_dir():
            sys.exit(f"{f.name}: no module folder {X}/{stem}")
        num = re.match(r"\d+", stem)
        if num:
            if num.group() in seen:
                sys.exit(f"{f.name} and {seen[num.group()]} both claim module {num.group()}")
            seen[num.group()] = f.name
        table.current = stem
        scope = {"__name__": f"data_{stem}", "X": X, table_name: table, **helpers}
        exec(compile(f.read_text(), str(f), "exec"), scope)
    return table
