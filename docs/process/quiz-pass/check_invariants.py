#!/usr/bin/env python3
"""Compare every quiz.json against a base commit: same item ids in the same order, same page, scope, key, select count and
option letters. Prints changed item ids per module and any broken invariant. usage: check_invariants.py <repo> <base> [prefix ...]"""
import json
import subprocess
import sys
from pathlib import Path

root, base, prefixes = Path(sys.argv[1]), sys.argv[2], sys.argv[3:]
bad, changed = [], {}
for f in sorted(root.glob("exercises/*/tests/quiz.json")):
    mod = f.parent.parent.name
    if prefixes and not any(mod.startswith(p) for p in prefixes):
        continue
    rel = f.relative_to(root).as_posix()
    old = json.loads(subprocess.run(["git", "-C", str(root), "show", f"{base}:{rel}"], capture_output=True, text=True, check=True).stdout)
    new = json.loads(f.read_text())
    o, n = old["quizzes"], new["quizzes"]
    if [q["id"] for q in o] != [q["id"] for q in n]:
        bad.append(f"{mod}: item ids or order changed")
        continue
    for a, b in zip(o, n):
        for k in ("page", "scope", "key", "select"):
            if a.get(k) != b.get(k):
                bad.append(f"{mod} {a['id']}: {k} {a.get(k)!r} -> {b.get(k)!r}")
        if sorted(a["options"]) != sorted(b["options"]):
            bad.append(f"{mod} {a['id']}: option letters changed")
        if a != b:
            changed.setdefault(mod, []).append(a["id"])
for mod, ids in changed.items():
    print(f"{mod}: {len(ids)} changed: {' '.join(ids)}")
print(f"total changed items: {sum(len(v) for v in changed.values())}")
for line in bad:
    print("INVARIANT BROKEN:", line)
sys.exit(1 if bad else 0)
