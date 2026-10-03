#!/usr/bin/env python3
"""Write cases.json for the practices of the Level 3 modules from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/l3_make_cases.py
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

PRACTICES[f"{X}/45-the-agentic-loop-in-depth/unit-01/practice-1"] = {
    "name": "agent", "suite": "AgentLoopTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a run alternates model and tools until the model ends its turn"),
        ("e1", "edge", "the stop reason decides and the words of the text do not"),
        ("e2", "edge", "every call of a turn is answered in one user message in order"),
        ("e3", "edge", "a failing or unknown tool becomes an error result and the run goes on"),
        ("e4", "edge", "the turn limit is a backstop that ends only a run the model has not ended"),
        ("e5", "edge", "a cut off or refused reply ends the run with its own status"),
        ("e6", "edge", "a tool use reply without a tool call is malformed and sends nothing more"),
    ],
    "plants": {
        "wrong-text-marker": (["e1"], "ends the run when the text says done, even if the reply also calls a tool"),
        "wrong-cap-reports-done": (["e4"], "reports a run that hit the turn limit as done"),
        "wrong-cap-off-by-one": (["e4"], "allows one model call more than the turn limit"),
        "wrong-results-split": (["e2"], "answers each tool call of a turn in its own user message"),
        "wrong-error-without-flag": (["e3"], "sends a failing tool's message back as an ordinary result"),
        "wrong-truncated-is-done": (["e5"], "treats a reply cut off by max_tokens as a finished one"),
        "wrong-malformed-continues": (["e6"], "sends an empty user message and calls the model again after a tool use reply with no call"),
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
