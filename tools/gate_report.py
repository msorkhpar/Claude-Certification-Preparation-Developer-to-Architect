#!/usr/bin/env python3
"""Judge one gate run from the files the runners left in .survey-out and write .survey-out/gate-report.json.

Called by tools/gate.sh; the gate's own echo is never evidence. For every practice and language in the range the report lists the
EXPECTED runs (starter, reference and each plant that tools/make_plants.py --list names) and the EXECUTED runs read from the
runner's own output (and the JUnit XML of Java and Kotlin), with the verdict of each; for every example and language the expected
test and output runs and what exists. expected != executed is a failure, and so is a language that produced no runs.

Languages: all four (python, typescript, java, kotlin) are required for every practice and example, except one whose Python and
TypeScript editions alone exist AND whose sources name the Agent SDK (the one stated exception, board D3).

usage: tools/gate_report.py report --modules RX [--examples RX] [--mode full|report-only] [--steps FILE] [--started EPOCH]
       tools/gate_report.py clean  --modules RX [--examples RX]       delete the outputs a run of the range will recreate
       tools/gate_report.py info   --modules RX [--examples RX]       print: practices, examples, jvm tasks, spec globs (for gate.sh)
"""
import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(Path(__file__).resolve().parent))
LANGS = ["python", "typescript", "java", "kotlin"]
OUT = ROOT / ".survey-out"
SDK_EXCEPTION = re.compile(r"agent sdk", re.I)


def _args():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("cmd", choices=["report", "clean", "info"])
    ap.add_argument("--modules", required=True)
    ap.add_argument("--examples")
    ap.add_argument("--mode", default="full")
    ap.add_argument("--steps")
    ap.add_argument("--started", type=float, default=0.0)
    return ap.parse_args()


A = _args()
os.environ["L2_MODULES"] = A.modules          # l2_practices reads the range from here, at import
import l2_practices  # noqa: E402
import grade_practices  # noqa: E402

EXAMPLES_RX = re.compile(A.examples if A.examples is not None else A.modules)
MODULES_RX = re.compile(A.modules)


def names_in_scope():
    """Module-named folders of course/, exercises/ and examples/ that the range covers."""
    dirs = []
    for top, rxs in (("course", (MODULES_RX, EXAMPLES_RX)), ("exercises", (MODULES_RX,)), ("examples", (EXAMPLES_RX,))):
        base = ROOT / top
        if base.is_dir():
            dirs += [str(p.relative_to(ROOT)) for p in sorted(base.iterdir()) if p.is_dir() and any(r.match(p.name) for r in rxs)]
    return dirs


def sdk_exception(folder):
    """True when a folder's own sources (statement, example files) name the Agent SDK."""
    for p in folder.rglob("*"):
        if p.is_file() and p.suffix in (".md", ".py", ".ts", ".json") and not any(x in p.parts for x in ("node_modules", ".gradle")) \
                and "wrong-" not in str(p) and SDK_EXCEPTION.search(p.read_text(errors="ignore")):
            return True
    return False


def expected_languages(folder, present):
    """(required languages, problem or None) for a practice or example folder; present = the languages it has."""
    present = [l for l in LANGS if l in present]
    if present == LANGS:
        return LANGS, None
    if present == ["python", "typescript"] and sdk_exception(folder):
        return present, None
    return LANGS, f"declares only {present}; all four are required unless the sources name the Agent SDK and python and typescript alone exist"


def plant_names():
    """{(practice, lang): [plant, ...]} exactly as tools/make_plants.py --list prints them."""
    r = subprocess.run([sys.executable, str(ROOT / "tools" / "make_plants.py"), "--modules", A.modules, "--list"], capture_output=True, text=True)
    names = {}
    for line in r.stdout.splitlines():
        parts = line.split()
        if len(parts) == 3:
            names.setdefault((parts[0], parts[1]), []).append(parts[2])
    return names


def examples():
    found = []
    for spec in sorted(ROOT.glob("examples/*/example.json")):
        if EXAMPLES_RX.match(spec.parent.name):
            found.append((spec.parent.name, list(json.loads(spec.read_text())["files"])))
    return found


def jvm_tasks():
    # cleanTest first: an up-to-date test task would not write the test summary the gate has just deleted
    return " ".join(f":{d}:{l}:{t}" for d, langs in examples() for l in ("java", "kotlin") if l in langs for t in ("cleanTest", "test", "runExample"))


def clean():
    n = 0
    for practice, cases in l2_practices.practices():
        for lang in LANGS:
            for v in l2_practices.variants(cases):
                p = grade_practices.out_path(practice, lang, v)
                if p.exists():
                    p.unlink()
                    n += 1
    for d, _ in examples():
        for p in OUT.glob(f"ex-{d}-*"):
            p.unlink()
            n += 1
    for name in ("gate-report.json", "gate-steps.tsv"):
        if (OUT / name).exists():
            (OUT / name).unlink()
    print(f"removed {n} earlier output files of the range")


def fresh(path, started):
    return started <= 0 or path.stat().st_mtime >= started - 1


def run_record(lang, variant, practice, cases, started):
    p = grade_practices.out_path(practice, lang, variant)
    rec = {"variant": variant, "executed": False, "ok": False}
    if not p.exists():
        rec["problem"] = "no output file"
        return rec
    junit = grade_practices.junit_results(practice, lang, variant) if lang in ("java", "kotlin") else None
    if not fresh(p, started):
        rec["problem"] = "output file is older than this gate run"
        return rec
    failed, problems, rc = grade_practices.analyse(lang, p.read_text(), cases, junit)
    if rc is None:
        rec["problem"] = "the run never wrote its rc line"
        return rec
    ok, want = grade_practices.judge(variant, failed, problems, rc, cases)
    rec.update(executed=True, rc=rc, failed_cases=sorted(failed), ok=ok)
    if not ok:
        rec["problem"] = f"want {want}; problems {problems}"
        if variant not in ("reference", "starter"):
            rec["caught_by_expected"] = sorted(cases["plants"][variant]["caught_by"])
    return rec


def example_record(d, lang, started):
    rec = {"language": lang, "expected": ["test", "run"], "executed": [], "ok": True, "problems": []}
    tf, of = OUT / f"ex-{d}-{lang}-test.txt", OUT / f"ex-{d}-{lang}-out.txt"
    for kind, f in (("test", tf), ("run", of)):
        if not f.exists():
            rec["problems"].append(f"no {kind} output")
        elif not fresh(f, started):
            rec["problems"].append(f"{kind} output is older than this gate run")
        else:
            rec["executed"].append(kind)
    if "test" in rec["executed"]:
        txt = tf.read_text()
        if lang == "python":
            ok = bool(re.search(r"\d+ passed", txt)) and "failed" not in txt and "error" not in txt.lower()
        elif lang == "typescript":
            ok = bool(re.search(r"^ℹ fail 0", txt, re.M) and re.search(r"^ℹ pass [1-9]", txt, re.M))
        else:
            m = re.search(r"tests (\d+), passed (\d+), failed (\d+)", txt)
            ok = bool(m) and int(m.group(1)) > 0 and int(m.group(3)) == 0 and m.group(1) == m.group(2)
        if not ok:
            rec["problems"].append("tests did not all pass")
    if "run" in rec["executed"] and not of.read_text().strip():
        rec["problems"].append("the program printed nothing")
    rec["ok"] = not rec["problems"] and len(rec["executed"]) == 2
    return rec


def git(*args):
    return subprocess.run(["git", "-C", str(ROOT), *args], capture_output=True, text=True).stdout.strip()


def report():
    started = A.started if A.mode == "full" else 0.0
    findings = []
    practices = []
    totals = {l: {"expected": 0, "executed": 0, "failed": 0} for l in LANGS}
    plants = plant_names()
    for practice, cases in l2_practices.practices():
        folder = ROOT / practice
        present = [l for l in LANGS if (folder / l).is_dir()]
        required, problem = expected_languages(folder, present)
        if problem:
            findings.append(f"{practice}: {problem}")
        entry = {"practice": practice, "languages_required": required, "languages": {}}
        declared = [l for l in LANGS if l in next(iter(cases["cases"].values()))]
        for lang in required:
            if lang not in declared:
                findings.append(f"{practice} {lang}: cases.json does not declare the language")
            want_plants = sorted(plants.get((practice, lang), []))
            if want_plants != sorted(cases["plants"]):
                findings.append(f"{practice} {lang}: plants from make_plants {want_plants} differ from cases.json {sorted(cases['plants'])}")
            expected = ["starter", "reference"] + want_plants
            runs = [run_record(lang, v, practice, cases, started) for v in expected]
            executed = [r for r in runs if r["executed"]]
            for r in runs:
                if not r["ok"]:
                    findings.append(f"{practice} {lang} {r['variant']}: {r.get('problem', 'failed')}")
            entry["languages"][lang] = {"expected": expected, "expected_count": len(expected), "executed_count": len(executed),
                                        "runs": runs}
            t = totals[lang]
            t["expected"] += len(expected)
            t["executed"] += len(executed)
            t["failed"] += sum(1 for r in runs if not r["ok"])
        practices.append(entry)
    exs = []
    for d, present in examples():
        required, problem = expected_languages(ROOT / "examples" / d, present)
        if problem:
            findings.append(f"example {d}: {problem}")
        entry = {"example": d, "languages_required": required, "languages": {}}
        for lang in required:
            if lang not in present:
                findings.append(f"example {d} {lang}: example.json does not declare the language")
            rec = example_record(d, lang, started)
            entry["languages"][lang] = rec
            for pr in rec["problems"]:
                findings.append(f"example {d} {lang}: {pr}")
            t = totals[lang]
            t["expected"] += 2
            t["executed"] += len(rec["executed"])
            t["failed"] += 0 if rec["ok"] else 1
        exs.append(entry)
    # a language that produced no runs
    in_scope = bool(practices or exs)
    waived = []
    if in_scope:
        for lang in LANGS:
            if totals[lang]["expected"] == 0:
                if lang in ("java", "kotlin") and all(lang not in e["languages_required"] for e in practices + exs):
                    waived.append(lang)
                else:
                    findings.append(f"{lang}: no runs at all")
            elif totals[lang]["executed"] == 0:
                findings.append(f"{lang}: expected {totals[lang]['expected']} runs, executed none")
    for lang in LANGS:
        if totals[lang]["executed"] != totals[lang]["expected"]:
            findings.append(f"{lang}: expected {totals[lang]['expected']} runs, executed {totals[lang]['executed']}")
    steps = []
    if A.steps and Path(A.steps).exists():
        for line in Path(A.steps).read_text().splitlines():
            name, rc, secs = line.split("\t")
            steps.append({"name": name, "rc": int(rc), "seconds": int(secs)})
            if int(rc) != 0:
                findings.append(f"step {name}: rc={rc}")
    scope = names_in_scope()
    last = git("log", "-1", "--format=%ct", "--", *scope) if scope else ""
    dirty = git("status", "--porcelain", "--", *scope).splitlines() if scope else []
    if A.mode == "full" and dirty:
        findings.append(f"the range has uncommitted changes ({len(dirty)}): commit, then gate")
    data = {"schema": 1, "mode": A.mode, "commit": git("rev-parse", "HEAD"), "modules": A.modules,
            "examples": A.examples if A.examples is not None else A.modules, "scope_dirs": scope,
            "range_last_commit_ts": int(last) if last else 0, "generated_ts": int(time.time()), "range_clean": not dirty,
            "scope_empty": not in_scope, "languages_waived": waived, "steps": steps, "practices": practices, "examples_runs": exs,
            "languages": totals, "grade_findings": len(findings), "findings": findings, "pass": not findings}
    OUT.mkdir(exist_ok=True)
    (OUT / "gate-report.json").write_text(json.dumps(data, indent=1) + "\n")
    for f in findings[:15]:
        print("FINDING:", f)
    if len(findings) > 15:
        print(f"... and {len(findings) - 15} more findings (all in .survey-out/gate-report.json)")
    print(f"gate report: {'PASS' if data['pass'] else 'FAIL'} findings={len(findings)} " +
          " ".join(f"{l}={totals[l]['executed']}/{totals[l]['expected']}" for l in LANGS))
    return 0 if data["pass"] else 1


if A.cmd == "clean":
    clean()
elif A.cmd == "info":
    print("PRACTICES", len(l2_practices.practices()))
    print("EXAMPLES", " ".join(d for d, _ in examples()))
    print("SPECS", " ".join(f"examples/{d}/example.json" for d, _ in examples()))
    print("JVMTASKS", jvm_tasks())
else:
    sys.exit(report())
