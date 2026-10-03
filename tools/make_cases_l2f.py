#!/usr/bin/env python3
"""Write cases.json for the practices of modules 42 and 43 from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/make_cases_l2e.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
X = "exercises"

# practice dir -> {"name", "tests": {java, kotlin suite names}, "langs", "cases": [(id, kind, title)], "plants": {name: (caught_by, defect)}}
PRACTICES = {}

# --- PRACTICES BELOW ---

PRACTICES[f"{X}/42-evaluation/unit-01/practice-1"] = {
    "name": "harness", "suite": "HarnessTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run grades every case with its own check and reports the pass rate"),
        ("e1", "edge", "an exact check ignores case and spacing but nothing else"),
        ("e2", "edge", "a json field check needs a json object with the field and the same typed value"),
        ("e3", "edge", "a judge check sends the rubric prompt and accepts only a bare score at the threshold"),
        ("e4", "edge", "a model that fails on one case does not stop the run"),
        ("e5", "edge", "tags report their own rates and success criteria judge each dimension"),
        ("e6", "edge", "a regression run names what broke what was fixed and what went missing"),
        ("e7", "edge", "repeated runs expose flaky cases and a case passes only if every run does"),
    ],
    "plants": {
        "wrong-regex-full-match": (["m1"], "requires the whole output to match the regular expression instead of searching it"),
        "wrong-exact-case-sensitive": (["e1"], "compares exact answers without lower-casing them"),
        "wrong-exact-contains": (["e1"], "accepts an output that merely contains the expected answer"),
        "wrong-json-loose-type": (["e2"], "compares the field and the expected value as text, so the string 3 equals the number 3"),
        "wrong-json-fence-ok": (["e2"], "strips a code fence before parsing, so fenced output passes"),
        "wrong-judge-first-digit": (["e3"], "takes the first digit it finds in the judge's reply instead of requiring a bare score"),
        "wrong-judge-strict-threshold": (["e3"], "requires a score above the threshold instead of at it"),
        "wrong-model-error-passes": (["e4"], "counts a run in which the model raised an error as a pass"),
        "wrong-tag-total": (["e5"], "counts a case in its tag total only when it passed"),
        "wrong-meets-at-or-below": (["e5"], "fails the overall criterion when the pass rate equals the minimum"),
        "wrong-meets-missing-tag-ok": (["e5"], "lets a criterion pass when no case carries its tag"),
        "wrong-compare-rate-only": (["e6"], "calls a run fine whenever the pass rate did not fall, even with a regression"),
        "wrong-compare-removed-ignored": (["e6"], "ignores cases that disappeared from the set"),
        "wrong-flaky-any-run": (["e7"], "passes a case when any of its repeated runs passes"),
    },
}

PRACTICES[f"{X}/43-debugging-claude-applications/unit-01/practice-1"] = {
    "name": "diagnose", "suite": "DiagnoseTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "each documented http error maps to a type an origin and a recovery"),
        ("e1", "edge", "a 429 is a rate limit or a spend cap and other statuses fall back by class"),
        ("e2", "edge", "a successful response can still fail by its stop reason"),
        ("e3", "edge", "an empty end turn is the integration when text followed the tool result and the model otherwise"),
        ("e4", "edge", "a parse failure is the integration when a json object is in the text and the model when not"),
        ("e5", "edge", "tool failures split into a model that called a missing tool and our tool that raised"),
        ("e6", "edge", "the first failure names the cause and a later good response marks it recovered"),
        ("e7", "edge", "a dropped connection has no status and belongs to the service side"),
    ],
    "plants": {
        "wrong-retry-400": (["m1"], "retries a malformed request with back-off instead of fixing it"),
        "wrong-413-service": (["m1"], "blames the service for a request that is too large"),
        "wrong-429-always-retry": (["e1"], "treats a spend-cap 429 as an ordinary rate limit"),
        "wrong-spend-400-ignored": (["e1"], "treats a 400 spend-limit message as a malformed request"),
        "wrong-maxtokens-model": (["e2"], "blames the model for a response cut off by the caller's own max_tokens"),
        "wrong-refusal-retry": (["e2"], "retries a refusal with back-off instead of using a fallback model"),
        "wrong-empty-always-model": (["e3"], "blames the model for every empty end turn"),
        "wrong-empty-text-anywhere": (["e3"], "blames the integration whenever text and a tool result are both present, in any order"),
        "wrong-parse-always-model": (["e4"], "blames the model for every parse failure"),
        "wrong-parse-braces-only": (["e4"], "calls any text with braces a JSON object without parsing it"),
        "wrong-unknown-tool-integration": (["e5"], "blames the integration for a tool name the model invented"),
        "wrong-tool-error-flagged": (["e5"], "reports a tool result flagged is_error as a failure of our code"),
        "wrong-last-failure": (["e6"], "reports the last failure in the trace instead of the first"),
        "wrong-recovered-ignored": (["e6"], "never reports a recovery"),
        "wrong-recovered-empty": (["e6"], "counts an empty end turn as a recovery"),
        "wrong-network-integration": (["e7"], "blames the integration for a dropped connection"),
    },
}

# --- PRACTICES ABOVE ---


def camel(title):
    words = title.split()
    return words[0] + "".join(w[:1].upper() + w[1:] for w in words[1:])


def main():
    problems = 0
    for practice, spec in PRACTICES.items():
        base = ROOT / practice
        cases, tests = {}, {}
        for cid, kind, title in spec["cases"]:
            cases[cid] = {"kind": kind, "python": f"test_{cid}_" + title.replace(" ", "_"), "typescript": f"{cid} {title}",
                          "java": f"{cid}_{camel(title)}", "kotlin": f"{cid}_{camel(title)}"}
        data = {"practice": practice.split(f"{X}/", 1)[1], "name": spec["name"], "tests": {"java": spec["suite"], "kotlin": spec["suite"]},
                "cases": {cid: {k: v for k, v in c.items() if k in ("kind",) + tuple(spec["langs"])} for cid, c in cases.items()},
                "plants": {n: {"caught_by": caught, "defect": defect} for n, (caught, defect) in spec["plants"].items()}}
        if spec["langs"] == ["python", "typescript"]:
            data.pop("tests")
        elif spec["langs"] != ["python", "typescript", "java", "kotlin"]:
            sys.exit(f"{practice}: unsupported languages {spec['langs']}")
        (base / "cases.json").write_text(json.dumps(data, indent=2) + "\n")
        for lang in spec["langs"]:
            found = {"python": next(base.glob("python/tests/*.py"), None), "typescript": next(base.glob("typescript/tests/*.test.ts"), None),
                     "java": next(base.glob("java/tests/*Test.java"), None), "kotlin": next(base.glob("kotlin/tests/*Test.kt"), None)}[lang]
            text = found.read_text() if found else ""
            for cid, c in cases.items():
                name = c[lang]
                ok = (f"def {name}(" in text) if lang == "python" else (f'test("{name}"' in text) if lang == "typescript" else (f"void {name}(" in text or f"fun {name}(" in text)
                if not ok:
                    print(f"{practice}/{lang}: test {name!r} not found")
                    problems += 1
        print(f"{practice}: cases.json written ({len(cases)} cases, {len(spec['plants'])} plants)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
