#!/usr/bin/env python3
"""Accept a gate report as a hand-back, or refuse it. Reads the report only; never believes anyone's words.

usage: tools/validate_handback.py [.survey-out/gate-report.json]
Exit 0 only when the report: exists; was written by a full gate run (not --report-only); is fresh (written after the last
commit that touched the range, and for the current HEAD of the range); has a clean range (nothing uncommitted); covers all four
languages with executed == expected > 0 (a language may be waived only when every item in scope is an Agent SDK exception, and
then only java and kotlin); has no step with a non-zero rc; has no findings; and says pass.
Prints one line the register can read: the verdict, the range, the commit and the per-language executed/expected runs.
"""
import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANGS = ["python", "typescript", "java", "kotlin"]


def git(*a):
    return subprocess.run(["git", "-C", str(ROOT), *a], capture_output=True, text=True).stdout.strip()


def validate(path):
    """(list of reasons it is refused, one-line summary)."""
    p = Path(path)
    if not p.exists():
        return [f"no report at {path}"], f"REFUSED no report at {path}"
    try:
        r = json.loads(p.read_text())
    except ValueError as e:
        return [f"report is not JSON: {e}"], "REFUSED unreadable report"
    why = []
    if r.get("mode") != "full":
        why.append(f"mode is {r.get('mode')!r}: only a full gate run counts")
    scope = r.get("scope_dirs", [])
    last = git("log", "-1", "--format=%ct", "--", *scope) if scope else ""
    last = int(last) if last else 0
    if r.get("generated_ts", 0) < last or r.get("range_last_commit_ts", 0) < last:
        why.append("stale: a commit touching the range is newer than the report")
    if git("status", "--porcelain", "--", *scope) if scope else "":
        why.append("the range has uncommitted changes")
    if not r.get("range_clean"):
        why.append("the report was written over an unclean range")
    if r.get("commit") != git("rev-parse", "HEAD"):
        why.append("the report is for another commit than HEAD")
    langs = r.get("languages", {})
    waived = set(r.get("languages_waived", []))
    if not waived <= {"java", "kotlin"}:
        why.append(f"only java and kotlin may be waived, not {sorted(waived - {'java', 'kotlin'})}")
    if r.get("scope_empty"):
        why.append("the range holds no practice and no example, so nothing was proved")
    for l in LANGS:
        t = langs.get(l)
        if t is None:
            why.append(f"{l}: missing from the report")
        elif l in waived:
            if t["expected"] or t["executed"]:
                why.append(f"{l}: waived but has runs")
        elif t["expected"] == 0 or t["executed"] == 0:
            why.append(f"{l}: no runs")
        elif t["executed"] != t["expected"]:
            why.append(f"{l}: executed {t['executed']} of {t['expected']} expected runs")
        elif t["failed"]:
            why.append(f"{l}: {t['failed']} run(s) with a wrong verdict")
    bad = [s["name"] for s in r.get("steps", []) if s["rc"] != 0]
    if bad:
        why.append(f"steps failed: {bad}")
    if r.get("findings") or r.get("grade_findings"):
        why.append(f"{r.get('grade_findings')} finding(s)")
    if not r.get("pass"):
        why.append("the report says fail")
    per = " ".join(f"{l}={langs.get(l, {}).get('executed', '?')}/{langs.get(l, {}).get('expected', '?')}" for l in LANGS)
    verdict = "ACCEPTED" if not why else "REFUSED (" + "; ".join(why) + ")"
    return why, f"{verdict} modules={r.get('modules')} commit={str(r.get('commit'))[:10]} runs {per} steps={len(r.get('steps', []))} findings={r.get('grade_findings')}"


def main():
    why, line = validate(sys.argv[1] if len(sys.argv) > 1 else str(ROOT / ".survey-out" / "gate-report.json"))
    print(line)
    return 1 if why else 0


if __name__ == "__main__":
    sys.exit(main())
