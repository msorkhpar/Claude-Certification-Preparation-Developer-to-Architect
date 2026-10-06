#!/usr/bin/env python3
"""Commit and push the consistent current state of the quiz quality pass.

Copies every changed module from each batch worktree into the main checkout, then stages only the modules whose quiz
sections and quiz.json agree (tools/check_quiz.py <prefix> reports ok), whose invariants against the base hold and whose
lesson prose is unchanged. A module caught mid-edit is left out of this snapshot and picked up by the next one.
usage: snapshot.py <main checkout> <base commit> <branch> [worktree ...]"""
import re
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
main, base, branch, worktrees = Path(sys.argv[1]).resolve(), sys.argv[2], sys.argv[3], [Path(w).resolve() for w in sys.argv[4:]]


def run(cmd, cwd):
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True)


def changed_modules(repo, against):
    out = run(["git", "diff", "--name-only", against, "--", "course", "exercises"], repo).stdout.split()
    out += run(["git", "ls-files", "--others", "--exclude-standard", "course", "exercises"], repo).stdout.split()
    return sorted({p.split("/")[1] for p in out if re.match(r"(course|exercises)/\d\d-", p)})


def module_ok(repo, module):
    prefix = module[:2]
    quiz = run([sys.executable, "tools/check_quiz.py", prefix], repo).stdout
    inv = run([sys.executable, str(HERE / "check_invariants.py"), str(repo), base, prefix], repo)
    return f"{module}: ok" in quiz and inv.returncode == 0


for wt in worktrees:
    for module in changed_modules(wt, base):
        if not module_ok(wt, module):
            print(f"skip {module} in {wt.name}: mid-edit")
            continue
        src, dst = wt / "course" / module, main / "course" / module
        for page in src.glob("*.md"):
            shutil.copy2(page, dst / page.name)
        shutil.copy2(wt / "exercises" / module / "tests" / "quiz.json", main / "exercises" / module / "tests" / "quiz.json")

prose = run([sys.executable, str(HERE / "check_prose.py"), str(main), base], main).stdout
touched_prose = {line.split("/")[1] for line in prose.splitlines() if line.startswith("course/")}
staged = []
for module in changed_modules(main, "HEAD"):
    if module in touched_prose:
        print(f"skip {module}: lesson prose differs from the base, needs a look")
        continue
    if module_ok(main, module):
        run(["git", "add", f"course/{module}", f"exercises/{module}/tests/quiz.json"], main)
        staged.append(module)
    else:
        print(f"skip {module} in main: mid-edit")
extra = [p for p in ("docs/process/QUIZ-QUALITY-PASS.md", "docs/process/QUIZ-POLISH.md", "docs/process/quiz-pass") if (main / p).exists()]
run(["git", "add", *extra], main)
if not run(["git", "diff", "--cached", "--quiet"], main).returncode:
    print("nothing to commit")
    sys.exit(0)
names = ", ".join(m[:2] for m in staged) or "process notes"
msg = (f"Quiz quality pass: work-in-progress snapshot ({names})\n\nModules whose quiz sections and quiz.json agree and whose "
       "invariants hold; modules still being edited follow in a later commit.\n\n"
       "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>\n"
       "Claude-Session: https://claude.ai/code/session_016gSg821AKSjnrpXS9MTMpN\n")
print(run(["git", "commit", "-q", "-m", msg], main).stdout)
for attempt in range(4):
    push = run(["git", "push", "-u", "origin", branch], main)
    if push.returncode == 0:
        print(f"pushed: {names}")
        break
    subprocess.run(["sleep", str(2 ** (attempt + 1))])
else:
    print("push failed:", push.stderr[-400:])
