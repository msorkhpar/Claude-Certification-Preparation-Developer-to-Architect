#!/usr/bin/env python3
"""Planted defects that MUST make the gate (or the hand-back validator) fail, each verified, plus the clean case that must pass.

usage: python3 tools/test_gate.py [--modules RX]      (default '^(41|62)-': two practices, two examples, all four languages)
It needs one finished FULL gate run of the range on the current HEAD (.survey-out/gate-report.json, mode full, pass). When there is
none it runs `tools/gate.sh --modules RX` first (heavy jobs through the slot, about half an hour for the default range). The defects
then act on the files and outputs of that run and re-judge with `tools/gate.sh --report-only`, which runs no container job; the one
defect that needs a real run (a starter that throws) re-runs just that one variant through the heavy slot. Every defect is applied by
an exact replacement that is checked to have changed the file, and is undone with git (or by moving files back) afterwards.
Exit status 0 only when every defect was caught and the clean case passed.
"""
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / ".survey-out"
REPORT = OUT / "gate-report.json"
KEEP = OUT / "gate-report.clean.json"
IMG = os.environ.get("RUNNER_IMAGE", "93d052f3fc87")
HEAVY = os.environ.get("HEAVY_SLOT", str(ROOT.parent / ".heavy-slot" / "run-heavy.sh"))
ENV = {**os.environ, "STUDYFORGE_NAMESPACE": "studyforge-local"}
MODULES = sys.argv[sys.argv.index("--modules") + 1] if "--modules" in sys.argv else "^(41|62)-"
results = []


def sh(*cmd, check=False):
    r = subprocess.run(cmd, cwd=ROOT, env=ENV, capture_output=True, text=True)
    return r.returncode, r.stdout + r.stderr


def gate(*extra):
    return sh("sh", "tools/gate.sh", "--modules", MODULES, *extra)


def report():
    return json.loads(REPORT.read_text())


def validate(path=REPORT):
    return sh(sys.executable, "tools/validate_handback.py", str(path))


def verdict(name, caught, detail):
    results.append((name, caught))
    print(f"{'CAUGHT ' if caught else 'MISSED '} {name}: {detail}", flush=True)


def replace_once(path, old, new):
    text = path.read_text()
    assert text.count(old) >= 1, f"{old!r} not found in {path}"
    changed = text.replace(old, new, 1)
    assert changed != text, "the plant did not change the file"
    path.write_text(changed)


def restore(*paths):
    sh("git", "checkout", "--", *[str(p) for p in paths])


def practices():
    sys.path.insert(0, str(ROOT / "tools"))
    os.environ["L2_MODULES"] = MODULES
    import l2_practices
    return [ROOT / p for p, _ in l2_practices.practices()]


def main():
    code, out = sh("git", "status", "--porcelain", "--", "course", "exercises", "examples")
    if out.strip():
        sys.exit("the range must be committed before the gate's tests run:\n" + out)
    fresh_ok = False
    if REPORT.exists():
        r = report()
        fresh_ok = r.get("mode") == "full" and r.get("pass") and validate()[0] == 0
    if not fresh_ok:
        print("no passing full gate run for this HEAD: running it first", flush=True)
        code, out = gate()
        print(out[-1500:])
        if code != 0:
            sys.exit("the clean gate run failed, so the planted defects cannot be judged against it")
    shutil.copy(REPORT, KEEP)

    # ---- the clean case ----
    code, out = validate(KEEP)
    verdict("clean run is accepted", code == 0, out.strip()[:300])
    langs = json.loads(KEEP.read_text())["languages"]
    verdict("clean run executed all four languages", all(langs[l]["executed"] == langs[l]["expected"] > 0 for l in langs), str({l: langs[l]["executed"] for l in langs}))

    plist = practices()
    p62 = next(p for p in plist if p.parent.parent.name.startswith("62-")) if any(p.parent.parent.name.startswith("62-") for p in plist) else plist[-1]
    prefix = str(p62.relative_to(ROOT)).replace("/", "_")

    # ---- defect 1: Java editions skipped (no runs) ----
    moved = []
    for f in OUT.glob("exercises_*-java-*.txt"):
        f.rename(f.with_name(f.name + ".moved"))
        moved.append(f)
    code, out = gate("--report-only")
    r = report()
    ok = code != 0 and r["languages"]["java"]["executed"] == 0 and not r["pass"] and any("java" in x for x in r["findings"])
    verdict("Java editions skipped", ok, f"gate rc={code}, java executed {r['languages']['java']['executed']}/{r['languages']['java']['expected']}")
    verdict("Java editions skipped: validator refuses", validate()[0] != 0, validate()[1].strip()[:160])
    for f in moved:
        f.with_name(f.name + ".moved").rename(f)

    # ---- defect 1b: a practice with its Java edition missing from the tree ----
    src = p62 / "java"
    hidden = p62 / "java.hidden"
    src.rename(hidden)
    try:
        code, out = gate("--report-only")
        r = report()
        verdict("practice without a Java edition", code != 0 and any("all four are required" in x for x in r["findings"]), f"gate rc={code}")
    finally:
        hidden.rename(src)

    # ---- defect 2: a wrong caught_by ----
    cj = p62 / "cases.json"
    data = json.loads(cj.read_text())
    first = next(iter(data["plants"]))
    data["plants"][first]["caught_by"] = ["e9"] if data["plants"][first]["caught_by"] != ["e9"] else ["e1"]
    before = cj.read_text()
    cj.write_text(json.dumps(data, indent=2) + "\n")
    assert cj.read_text() != before, "the plant did not change cases.json"
    try:
        code, out = gate("--report-only")
        r = report()
        hit = [x for x in r["findings"] if "wrong-" in x and "fails" in x]
        verdict("wrong caught_by", code != 0 and bool(hit), f"gate rc={code}, e.g. {hit[0][:150] if hit else None}")
    finally:
        restore(cj)

    # ---- defect 3: a starter that throws instead of failing an assertion (a real run of that one variant) ----
    starter = p62 / "python" / "starter" / "extraction.py"
    replace_once(starter, "    return True\n\n\ndef _currency_ok", '    raise RuntimeError("starter blew up")\n\n\ndef _currency_ok')
    rel = str(p62.relative_to(ROOT))
    run = ["sh", HEAVY, "ccp-survey", "tools/l2_run_practice.sh", IMG, "python", rel, "starter"]
    try:
        code, out = sh(*run)
        code, out = gate("--report-only")
        r = report()
        hit = [x for x in r["findings"] if "python starter" in x]
        verdict("starter throws instead of failing an assertion", code != 0 and bool(hit), f"gate rc={code}, {hit[0][:160] if hit else None}")
    finally:
        restore(starter)
        sh(*run)
    sh("git", "checkout", "--", str(p62))

    # ---- defect 4: personal data in a page (an e-mail at a domain built here, never a real one) ----
    page = sorted((ROOT / "course").glob(f"{p62.parts[-4].split('-')[0]}-*/*.md"))[0]
    fake = "j" + "ane.roe" + "@" + "acme" + "-widgets" + ".com"
    replace_once(page, "\n", f"\nContact {fake} for questions.\n")
    try:
        code, out = gate("--report-only")
        r = report()
        verdict("personal data in a page", code != 0 and any(s["name"] == "personal-data" and s["rc"] != 0 for s in r["steps"]), f"gate rc={code}")
    finally:
        restore(page)

    # ---- defect 5: a report that must not be accepted as a hand-back ----
    gate("--report-only")
    verdict("a report-only report is refused", validate()[0] != 0, validate()[1].strip()[:120])
    stale = json.loads(KEEP.read_text())
    stale["generated_ts"] = 1
    tmp = OUT / "gate-report.stale.json"
    tmp.write_text(json.dumps(stale))
    verdict("a stale report is refused", validate(tmp)[0] != 0, validate(tmp)[1].strip()[:120])
    short = json.loads(KEEP.read_text())
    short["languages"]["java"]["executed"] = 0
    tmp.write_text(json.dumps(short))
    verdict("a report with no Java runs is refused", validate(tmp)[0] != 0, validate(tmp)[1].strip()[:120])
    verdict("a missing report is refused", validate(OUT / "no-such-report.json")[0] != 0, "")
    tmp.unlink()

    # leave the clean report in place and the tree as found
    shutil.copy(KEEP, REPORT)
    code, out = sh("git", "status", "--porcelain", "--", "course", "exercises", "examples")
    verdict("tree restored", not out.strip(), out.strip()[:200])
    bad = [n for n, c in results if not c]
    print(f"\n{len(results) - len(bad)}/{len(results)} checks held" + (f"; MISSED: {bad}" if bad else ""))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
