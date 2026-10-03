#!/usr/bin/env python3
"""Write cases.json for the practices of modules 30 to 35 from one specification, and check that every test the file
names really exists in the test files of every language of the practice.

A case title is written once; the test names are derived from it:
  python       test_<id>_<title with spaces as underscores>
  typescript   <id> <title>
  java/kotlin  <id>_<title in camel case>
usage: tools/make_cases_l2d.py
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

PRACTICES[f"{X}/30-vision-and-documents/unit-01/practice-1"] = {
    "name": "vision", "suite": "VisionTest", "langs": ["python", "typescript", "java", "kotlin"], "pyfile": "test_vision.py", "tsfile": "vision.test.ts",
    "cases": [
        ("m1", "main", "images come first with labels and the question last"),
        ("e1", "edge", "the token cost follows the models resolution tier"),
        ("e2", "edge", "formats dimensions sizes and counts are checked before any call"),
        ("e3", "edge", "more than twenty images lower the side limit to 2000 pixels"),
        ("e4", "edge", "an image whose size matters is rejected instead of resized"),
        ("e5", "edge", "pdfs become document blocks in order and are limited by pages"),
        ("e6", "edge", "bedrock and vertex take only base64 sources and smaller images"),
        ("e7", "edge", "coordinates map back to the original and cost follows the price"),
    ],
    "plants": {
        "wrong-edge-only-resize": (["e1"], "sizes an image by the edge limit alone and ignores the visual token budget"),
        "wrong-text-first": (["m1"], "puts the question before the images"),
        "wrong-no-labels": (["m1"], "does not label the images when there are several"),
        "wrong-padded-coordinates": (["e7"], "divides a returned coordinate by the padded height instead of the resized height"),
        "wrong-same-limit-all-models": (["e2"], "allows 600 images for every model, including the 200k-context one"),
        "wrong-many-image-rule": (["e3"], "ignores the 2000 pixel limit that applies above 20 images"),
        "wrong-exact-silent": (["e4"], "lets an oversized image through even when its exact size matters"),
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
