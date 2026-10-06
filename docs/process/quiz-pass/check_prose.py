#!/usr/bin/env python3
"""Compare every changed course page with a base commit outside its quiz sections. Lists lines added or removed in lesson prose.
usage: check_prose.py <repo> <base>"""
import re, subprocess, sys, difflib
root, base = sys.argv[1], sys.argv[2]
files = subprocess.run(["git", "-C", root, "diff", "--name-only", base, "--", "course"], capture_output=True, text=True).stdout.split()
QUIZ = re.compile(r"^## (Quiz|Module quiz|Mock exam)\n.*?(?=^## |\Z)", re.S | re.M)
total_add = total_del = 0
for f in files:
    old = subprocess.run(["git", "-C", root, "show", f"{base}:{f}"], capture_output=True, text=True).stdout
    new = open(f"{root}/{f}").read()
    o, n = QUIZ.sub("", old).splitlines(), QUIZ.sub("", new).splitlines()
    adds = dels = 0
    for line in difflib.unified_diff(o, n, lineterm="", n=0):
        if line.startswith(("+++", "---", "@@")):
            continue
        print(f"{f}: {line[:220]}")
        adds += line.startswith("+"); dels += line.startswith("-")
    total_add += adds; total_del += dels
print(f"pages changed: {len(files)}; prose lines added {total_add}, removed {total_del}")
