#!/usr/bin/env python3
"""Check (or with --fill, write) the example blocks of the pages against examples/ and the outputs the
container produced (.survey-out/ex-<example>-<lang>-out.txt).
A block's code fences must equal the source files exactly, and each output fence the program's real output.
usage: tools/check_examples.py [--fill] [page glob under the repository root, e.g. 'course/1[2-7]-*/*.md' ...]
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EXAMPLES = {
    "m1-sampler": ("01-language-model", {"python": "sampler.py", "typescript": "sampler.ts"}),
    "m2-toy-bpe": ("02-toy-tokenizer", {"python": "bpe.py", "typescript": "bpe.ts"}),
}
# Examples from Level 2 on describe themselves: examples/<dir>/example.json = {"id": ..., "files": {lang: file}}
for _spec in sorted(ROOT.glob("examples/*/example.json")):
    _data = json.loads(_spec.read_text())
    EXAMPLES[_data["id"]] = (_spec.parent.name, _data["files"])
FENCE = {"python": "python", "typescript": "typescript", "java": "java", "kotlin": "kotlin"}
PLACE = {"python": ("EXAMPLE_PYTHON", "EXAMPLE_OUT_PY"), "typescript": ("EXAMPLE_TS", "EXAMPLE_OUT_TS")}


def expected(eid):
    d, files = EXAMPLES[eid]
    parts = []
    for lang, fname in files.items():
        src = (ROOT / "examples" / d / lang / fname).read_text().rstrip("\n")
        out = (ROOT / ".survey-out" / f"ex-{d}-{lang}-out.txt").read_text().rstrip("\n")
        parts.append((lang, src, out))
    return parts


def main(fill, globs):
    problems = 0
    pages = sorted({p for g in globs for p in ROOT.glob(g)}) if globs else sorted((ROOT / "course").glob("*/*.md"))
    for page in pages:
        md = page.read_text()
        new = md
        for m in re.finditer(r"<!-- example: (\S+) tabs: ([\w,]+) -->\n(.*?)<!-- /example -->", md, re.S):
            eid = m.group(1)
            if eid not in EXAMPLES:
                print(f"{page.name}: unknown example id {eid}")
                problems += 1
                continue
            parts = expected(eid)
            body = "".join(f"```{FENCE[l]}\n{src}\n```\n```text\n{out}\n```\n" for l, src, out in parts)
            tabs = ",".join(l for l, _, _ in parts)  # the tabs list follows the example's own file map (python, typescript, java, kotlin)
            if m.group(3) != body or m.group(2) != tabs:
                if fill:
                    new = new.replace(m.group(0), f"<!-- example: {eid} tabs: {tabs} -->\n{body}<!-- /example -->")
                else:
                    print(f"{page.name}: example {eid} differs from examples/ or its recorded output")
                    problems += 1
        if fill and new != md:
            page.write_text(new)
    print("example blocks:", "filled" if fill else ("ok" if not problems else f"{problems} finding(s)"))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main("--fill" in sys.argv, [a for a in sys.argv[1:] if not a.startswith("--")]))
