#!/usr/bin/env python3
"""Read the outputs that tools/run_all_practices.sh left in .survey-out and judge the practice proof.

Per language: the reference passes every case; the starter fails every case; every planted wrong solution
fails ON AN ASSERTION (no compile error, no other exception) and fails the case(s) cases.json names for it.
Prints one line per variant and a summary; exit 1 on any finding.
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from grade_practices import jvm_failures, junit_results  # noqa: E402  (one reader for Gradle failures: the JUnit XML report)

ROOT = Path(__file__).resolve().parent.parent
PD = "exercises/06-prompting-fundamentals/unit-01/practice-1"
OUT = ROOT / ".survey-out"
CASES = json.loads((ROOT / PD / "cases.json").read_text())
LANGS = ["python", "typescript", "java", "kotlin"]
ALL = list(CASES["cases"])


def out_path(lang, variant):
    return OUT / f"{PD.replace('/', '_')}-{lang}-{variant}.txt"


def analyse(lang, text, variant=None):
    """Return (passed_ids, failed_ids, other_problems) for one run's output."""
    names = {lang_name: cid for cid, c in CASES["cases"].items() for lang_name in [c[lang]]}
    failed, problems = set(), []
    if lang == "python":
        for m in re.finditer(r"^FAILED \S+::(\w+)", text, re.M):
            failed.add(names.get(m.group(1), m.group(1)))
        kinds = re.findall(r"^tests/\S+:\d+: (\w+)\s*$", text, re.M)
        if len(kinds) != len(failed) or any(k != "AssertionError" for k in kinds):
            problems.append(f"failure types {kinds} are not all AssertionError")
        if re.search(r"^E\s+(ImportError|ModuleNotFoundError|SyntaxError)", text, re.M):
            problems.append("import or syntax error")
    elif lang == "typescript":
        section = text.split("✖ failing tests:", 1)[1] if "✖ failing tests:" in text else ""
        for m in re.finditer(r"^\s*✖ (.*?) \(\d[\d.]*ms\)", text, re.M):
            if m.group(1) in names:
                failed.add(names[m.group(1)])
        kinds = re.findall(r"^\s+(\w+Error)\b.*", section, re.M)
        top = [k for k in kinds if k != "AssertionError"]
        if len([k for k in kinds if k == "AssertionError"]) < len(failed) or top:
            problems.append(f"failure types {sorted(set(kinds))} are not all AssertionError")
    elif lang == "java":  # Gradle output, read like Kotlin's
        if re.search(r"error: |Execution failed for task ':compile(Test)?Java'", text):
            problems.append("compilation error")
        jvm_failures(text, "PromptBuilderTest", names, junit_results(PD, lang, variant), failed, problems)
        if "BUILD FAILED" in text and not failed and not problems:
            problems.append("the build failed without a failing test")
    else:
        if re.search(r"^e: ", text, re.M):
            problems.append("compilation error")
        jvm_failures(text, "PromptBuilderTest", names, junit_results(PD, lang, variant), failed, problems)
    return set(ALL) - failed, failed, problems


def main():
    findings = 0
    summary = []
    for lang in LANGS:
        for variant in ["starter", "reference"] + list(CASES["plants"]):
            p = out_path(lang, variant)
            if not p.exists():
                print(f"{lang} {variant}: no output at {p.name}")
                findings += 1
                continue
            passed, failed, problems = analyse(lang, p.read_text(), variant)
            if variant == "reference":
                ok = failed == set() and not problems and len(passed) == len(ALL)
                want = "all pass"
            elif variant == "starter":
                ok = failed == set(ALL) and not problems
                want = "all fail on assertions"
            else:
                caught = set(CASES["plants"][variant]["caught_by"])
                ok = caught <= failed and not problems
                want = f"fails {sorted(caught)} on an assertion"
            summary.append((lang, variant, len(passed), len(failed), ok))
            print(f"{lang:10} {variant:32} passed={len(passed)} failed={len(failed)} {sorted(failed)} {'ok' if ok else 'FINDING: want ' + want + ' ' + str(problems)}")
            findings += 0 if ok else 1
    for lang in LANGS:
        rows = [r for r in summary if r[0] == lang]
        plants = [r for r in rows if r[1].startswith("wrong-")]
        ref = [r for r in rows if r[1] == "reference"]
        st = [r for r in rows if r[1] == "starter"]
        print(f"{lang}: reference passes {ref[0][2]}/{len(ALL)}" if ref else f"{lang}: no reference run", end=", ")
        print(f"starter fails {st[0][3]}/{len(ALL)}" if st else "no starter run", end=", ")
        print(f"plants failing on assertions={sum(1 for r in plants if r[4])}/{len(CASES['plants'])}")
    for ex in ["01-language-model", "02-toy-tokenizer"]:
        for lang in ["python", "typescript"]:
            t = (OUT / f"ex-{ex}-{lang}-test.txt")
            txt = t.read_text() if t.exists() else ""
            ok = bool(re.search(r"\d+ passed", txt) and "failed" not in txt) if lang == "python" else bool(re.search(r"^ℹ fail 0", txt, re.M) and re.search(r"^ℹ pass [1-9]", txt, re.M))
            print(f"example {ex} {lang} tests: {'ok' if ok else 'FINDING'}")
            findings += 0 if ok else 1
    print("findings:", findings)
    return 1 if findings else 0


if __name__ == "__main__":
    sys.exit(main())
