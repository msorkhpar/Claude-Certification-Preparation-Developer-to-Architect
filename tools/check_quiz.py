#!/usr/bin/env python3
"""Check the quizzes of Level 1 modules: pages agree with quiz.json, shape and wording rules hold.

Rules (CLAUDE.md quiz rules that a script can check):
  - every question has four options a-d and a key among them;
  - the key option shares no content word with the stem;
  - the folded key on the page names the same letter as quiz.json, in the same order;
  - quiz.json explains every option.
usage: tools/check_quiz.py [module-folder-prefix ...]   exit 1 on any finding
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
STOP = set("""a an the and or but of to in on at for from with by as is are was were be been being it its this that these those
not no do does did can could should would will may might must than then so if when what which who whom whose how why where
into over under about after before between each every any all some one two three four more most less least only also just
their there they them he she his her you your we our i me my us has have had get got use used using via per""".split())


def words(text):
    return {w for w in re.findall(r"[a-z][a-z'-]+", text.lower().replace("’", "'")) if w not in STOP and len(w) > 2}


def stem_of(stem):
    return words(re.sub(r"`[^`]*`", " ", stem))


def parse_page_quizzes(md):
    """Return a list of (heading, [(stem, {letter: text})], [key letters]) per quiz section."""
    out = []
    for m in re.finditer(r"^## (Quiz|Module quiz)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        body = m.group(2)
        qblock, _, keyblock = body.partition("<details>")
        questions = []
        for q in re.finditer(r"^\d+\. (.*?)(?=^\d+\. |\Z)", qblock, re.S | re.M):
            lines = q.group(1).strip().split("\n")
            stem_lines, opts = [], {}
            for line in lines:
                om = re.match(r"\s*- \*\*([a-d])\*\*: (.*)", line)
                if om:
                    opts[om.group(1)] = om.group(2).strip()
                elif not opts:
                    stem_lines.append(line.strip())
            questions.append((" ".join(stem_lines), opts))
        keys = re.findall(r"^\d+\. \*\*([a-d])\*\*", keyblock, re.M)
        out.append((m.group(1), questions, keys))
    return out


def check_module(folder):
    problems = []
    qj = ROOT / "exercises" / folder.name / "tests" / "quiz.json"
    if not qj.exists():
        return [f"{folder.name}: missing {qj.relative_to(ROOT)}"]
    data = json.loads(qj.read_text())
    by_id = {q["id"]: q for q in data["quizzes"]}
    seen = set()
    for page in sorted(folder.glob("*.md")):
        md = page.read_text()
        for kind, questions, keys in parse_page_quizzes(md):
            if len(keys) != len(questions):
                problems.append(f"{page.name}: {kind} has {len(questions)} questions but {len(keys)} keys")
            for n, (stem, opts) in enumerate(questions, start=1):
                qid = f"{page.stem}#{'m' if kind == 'Module quiz' else 'q'}{n}"
                q = by_id.get(qid)
                if q is None:
                    problems.append(f"{qid}: not in quiz.json")
                    continue
                seen.add(qid)
                if sorted(opts) != list("abcd"):
                    problems.append(f"{qid}: options are {sorted(opts)}, want a-d")
                if q["stem"] != stem or q["options"] != opts:
                    problems.append(f"{qid}: page text differs from quiz.json")
                key = keys[n - 1] if n - 1 < len(keys) else None
                if key != q["key"]:
                    problems.append(f"{qid}: page key {key} differs from quiz.json key {q['key']}")
                if sorted(q.get("explanation", {})) != list("abcd"):
                    problems.append(f"{qid}: quiz.json must explain every option a-d")
                shared = words(opts.get(q["key"], "")) & stem_of(stem)
                if shared:
                    problems.append(f"{qid}: key repeats stem words {sorted(shared)}")
    for qid in by_id:
        if qid not in seen:
            problems.append(f"{qid}: in quiz.json but not on any page")
    keys_used = [q["key"] for q in data["quizzes"]]
    for letter in "abcd":
        if keys_used and keys_used.count(letter) > (len(keys_used) + 1) // 2:
            problems.append(f"{folder.name}: key letter {letter} is overused ({keys_used.count(letter)} of {len(keys_used)})")
    return problems


def main(argv):
    folders = sorted(p for p in (ROOT / "course").iterdir() if p.is_dir() and re.match(r"0[1-6]-", p.name))
    if argv:
        folders = [f for f in folders if any(f.name.startswith(a) for a in argv)]
    total = 0
    for f in folders:
        probs = check_module(f)
        total += len(probs)
        for p in probs:
            print(p)
        print(f"{f.name}: {'ok' if not probs else str(len(probs)) + ' finding(s)'}")
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
