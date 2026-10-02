#!/usr/bin/env python3
"""Check the quizzes of Level 1 modules: pages agree with quiz.json, shape and wording rules hold.

Rules (CLAUDE.md quiz rules that a script can check):
  - every question has four options a-d and a key among them;
  - the key option shares no content word (after simple stemming) with the stem;
  - the key is at most 1.3 times the mean length of the distractors, in characters (warning when the key is the
    longest option in more than 40 percent of a module's questions);
  - the folded key gives every option its own explanation sentence (no merged "a, b, c are ..." sentence);
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


SUFFIXES = ("ers", "ions", "ments", "ing", "ion", "ment", "ed", "er", "es", "ly", "s")


def stemmed(word):
    """Lowercase stem: strip one common suffix and a doubled final consonant; kept when it has 4 letters or more."""
    for suf in SUFFIXES:
        if word.endswith(suf) and len(word) - len(suf) >= 3:
            word = word[: -len(suf)]
            break
    if len(word) > 3 and word[-1] == word[-2] and word[-1] not in "aeiou":
        word = word[:-1]
    return word if len(word) >= 4 else None


def stems(text):
    return {x for x in (stemmed(w) for w in words(text)) if x}


def stem_of(stem):
    return words(re.sub(r"`[^`]*`", " ", stem))


LENGTH_RATIO = 1.3
LONGEST_SHARE = 0.4


def key_length_ratio(opts, key):
    others = [len(v) for k, v in opts.items() if k != key]
    return len(opts[key]) / (sum(others) / len(others)) if others else 0.0


def key_is_longest(opts, key):
    return all(len(opts[key]) > len(v) for k, v in opts.items() if k != key)


def check_question(qid, stem, opts, key):
    """Wording findings for one question (stem, {letter: text}, key letter)."""
    problems = []
    if key not in opts:
        return problems
    plain = re.sub(r"`[^`]*`", " ", stem)
    shared = words(opts[key]) & stem_of(stem)
    if shared:
        problems.append(f"{qid}: key repeats stem words {sorted(shared)}")
    echoed = stems(opts[key]) & stems(plain)
    if echoed and not shared:
        problems.append(f"{qid}: key echoes the stem by stem {sorted(echoed)}")
    ratio = key_length_ratio(opts, key)
    if ratio > LENGTH_RATIO:
        problems.append(f"{qid}: key is {ratio:.2f} times the mean distractor length (limit {LENGTH_RATIO})")
    return problems


def check_key_paragraph(qid, para, key):
    """The folded key: key letter first, then one sentence per other option, none merged."""
    problems = []
    text = re.sub(r"^\*\*[a-d]\*\*\.\s*", "", para.strip())
    groups = re.findall(r"(?:\*[a-d]\*(?:, and |, | and )?)+", text)
    singles = [g for g in groups if len(re.findall(r"\*([a-d])\*", g)) == 1]
    merged = [g for g in groups if len(re.findall(r"\*([a-d])\*", g)) > 1]
    if merged:
        problems.append(f"{qid}: folded key merges options ({merged[0].strip()}); give each its own sentence")
    letters = sorted(re.findall(r"\*([a-d])\*", " ".join(singles)))
    want = sorted(set("abcd") - {key})
    if letters != want and not merged:
        problems.append(f"{qid}: folded key explains {letters}, want one sentence each for {want}")
    return problems


def key_paragraphs(md):
    result = []
    for m in re.finditer(r"^## (Quiz|Module quiz)\n(.*?)(?=^## |\Z)", md, re.S | re.M):
        _, _, keyblock = m.group(2).partition("<details>")
        result.append(re.findall(r"^\d+\. (\*\*[a-d]\*\*.*?)(?=^\d+\. |\n</details>|\Z)", keyblock, re.S | re.M))
    return result


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
    longest_total = longest_hits = 0
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
                problems += check_question(qid, stem, opts, q["key"])
                paras = key_paragraphs(md)
                idx = [k for k, _, _ in parse_page_quizzes(md)].index(kind)
                if n - 1 < len(paras[idx]):
                    problems += check_key_paragraph(qid, paras[idx][n - 1], q["key"])
                longest_total += 1
                longest_hits += key_is_longest(opts, q["key"])
    for qid in by_id:
        if qid not in seen:
            problems.append(f"{qid}: in quiz.json but not on any page")
    if longest_total and longest_hits / longest_total > LONGEST_SHARE:
        print(f"warning: {folder.name}: key is the longest option in {longest_hits} of {longest_total} questions")
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
