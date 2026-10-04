#!/usr/bin/env python3
"""Read the outputs that tools/l2_run_all.sh left in .survey-out and judge the Level 2 practice proofs.

Per practice and language: the reference passes every case; the starter fails every case; every planted wrong
solution fails ON AN ASSERTION (no compile error, no other exception) and fails the case(s) cases.json names for it.
Also checks the Level 2 examples' test runs. Prints one line per variant and a summary; exit 1 on any finding.
usage: tools/grade_practices.py
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from l2_practices import ROOT, practices, variants  # noqa: E402

OUT = ROOT / ".survey-out"
LANGS = ["python", "typescript", "java", "kotlin"]


def out_path(practice, lang, variant):
    return OUT / f"{practice.replace('/', '_')}-{lang}-{variant}.txt"


ASSERTION_TYPES = ("AssertionFailedError", "AssertionError", "ComparisonFailure", "MultipleFailuresError")


def junit_results(practice, lang, variant):
    """{test name: failure type or None} read from the JUnit XML report Gradle wrote for one Java or Kotlin variant, or None.

    Gradle's short console format prints no exception line for some assertion failures (a multi-line assertEquals),
    so the failure type is read from the report: <failure type=...> is an assertion, <error type=...> is any other exception."""
    folder = ROOT / practice / f".build-{lang}" / variant / "test-results" / "test"
    reports = sorted(folder.glob("TEST-*.xml"))
    if not reports:
        return None
    results = {}
    for report in reports:
        for case in ET.parse(report).getroot().iter("testcase"):
            bad = case.find("failure") if case.find("failure") is not None else case.find("error")
            results[case.get("name").removesuffix("()")] = None if bad is None else (bad.tag, bad.get("type") or "")
    return results


def jvm_failures(text, suite, names, junit, failed, problems):
    """Add the failed case ids and the non-assertion failures of one Java or Kotlin run (report first, console as a fallback)."""
    if junit is not None:
        for name, bad in junit.items():
            if bad is not None:
                failed.add(names.get(name, name))
                if bad[0] != "failure" or not bad[1].endswith(ASSERTION_TYPES):
                    problems.append(f"{name}: {bad[1] or bad[0]}")
        return
    for m in re.finditer(suite + r" > (\w+)\(\) FAILED\n\s+(\S+)", text):
        failed.add(names.get(m.group(1), m.group(1)))
        if "AssertionFailedError" not in m.group(2):
            problems.append(f"{m.group(1)}: {m.group(2)}")


def analyse(lang, text, cases, junit=None):
    """Return (failed case ids, other problems, rc) for one run's output; junit is junit_results() for java and kotlin."""
    names = {c[lang]: cid for cid, c in cases["cases"].items()}
    failed, problems = set(), []
    rc_match = re.search(r"^rc=(\d+)\s*$", text, re.M)
    rc = int(rc_match.group(1)) if rc_match else None
    if lang == "python":
        for m in re.finditer(r"^FAILED \S+::(\w+)", text, re.M):
            failed.add(names.get(m.group(1), m.group(1)))
        kinds = re.findall(r"^tests/\S+:\d+: (\w+)\s*$", text, re.M)
        if len(kinds) != len(failed) or any(k != "AssertionError" for k in kinds):
            problems.append(f"failure types {kinds} are not all AssertionError")
        if re.search(r"^E\s+(ImportError|ModuleNotFoundError|SyntaxError)|error[s]? during collection|ERROR collecting", text, re.M):
            problems.append("import, syntax or collection error")
    elif lang == "typescript":
        section = text.split("✖ failing tests:", 1)[1] if "✖ failing tests:" in text else ""
        for m in re.finditer(r"^\s*✖ (.*?) \(\d[\d.]*ms\)", text.split("✖ failing tests:", 1)[0], re.M):
            failed.add(names.get(m.group(1), m.group(1)))
        kinds = re.findall(r"^  (\w*Error)\b", section, re.M)
        if len(kinds) != len(failed) or any(k != "AssertionError" for k in kinds):
            problems.append(f"failure types {sorted(set(kinds))} are not all AssertionError (or a failure has none)")
    elif lang == "java":  # Gradle output, read like Kotlin's
        if re.search(r"error: |Execution failed for task ':compileJava'|Execution failed for task ':compileTestJava'", text):
            problems.append("compilation error")
        suite = cases["tests"]["java"]
        jvm_failures(text, suite, names, junit, failed, problems)
        if "BUILD FAILED" in text and not failed and not problems:
            problems.append("the build failed without a failing test")
    else:
        if re.search(r"^e: ", text, re.M):
            problems.append("compilation error")
        suite = cases["tests"]["kotlin"]
        jvm_failures(text, suite, names, junit, failed, problems)
        if "FAILED" in text and "BUILD FAILED" in text and not failed and not problems:
            problems.append("the build failed without a failing test")
    return failed, problems, rc


def main():
    findings = 0
    summary = {}
    for practice, cases in practices():
        allids = list(cases["cases"])
        for lang in [l for l in LANGS if l in next(iter(cases["cases"].values()))]:
            for variant in variants(cases):
                p = out_path(practice, lang, variant)
                if not p.exists():
                    print(f"{practice} {lang} {variant}: no output at {p.name}")
                    findings += 1
                    continue
                failed, problems, rc = analyse(lang, p.read_text(), cases, junit_results(practice, lang, variant) if lang in ("java", "kotlin") else None)
                if variant == "reference":
                    ok = not failed and not problems and rc == 0
                    want = "all pass with rc=0"
                elif variant == "starter":
                    ok = failed == set(allids) and not problems and rc not in (0, None)
                    want = "all fail on assertions"
                else:
                    caught = set(cases["plants"][variant]["caught_by"])
                    ok = caught <= failed and not problems and rc not in (0, None)
                    want = f"fails {sorted(caught)} on an assertion"
                summary.setdefault((practice, lang), []).append((variant, len(allids) - len(failed), len(failed), ok))
                print(f"{practice.split('/')[0]:42} {lang:10} {variant:28} failed={sorted(failed)} rc={rc} {'ok' if ok else 'FINDING: want ' + want + ' ' + str(problems)}")
                findings += 0 if ok else 1
    print()
    for (practice, lang), rows in summary.items():
        plants = [r for r in rows if r[0].startswith("wrong-")]
        ref = next(r for r in rows if r[0] == "reference")
        st = next(r for r in rows if r[0] == "starter")
        total = ref[1] + ref[2]
        print(f"{practice.split('/')[0]:42} {lang:10} reference passes {ref[1]}/{total}, starter fails {st[2]}/{total}, "
              f"plants failing on assertions {sum(1 for r in plants if r[3])}/{len(plants)}")
    for spec in sorted(ROOT.glob("examples/*/example.json")):
        d = spec.parent.name
        if os.environ.get("L2_EXAMPLES") and not re.match(os.environ["L2_EXAMPLES"], d):
            continue
        for lang in ("python", "typescript"):
            t = OUT / f"ex-{d}-{lang}-test.txt"
            txt = t.read_text() if t.exists() else ""
            if lang == "python":
                ok = bool(re.search(r"\d+ passed", txt)) and "failed" not in txt and "error" not in txt.lower()
            else:
                ok = bool(re.search(r"^ℹ fail 0", txt, re.M) and re.search(r"^ℹ pass [1-9]", txt, re.M))
            print(f"example {d} {lang} tests: {'ok' if ok else 'FINDING'}")
            findings += 0 if ok else 1
    print("findings:", findings)
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
