#!/usr/bin/env python3
"""Check the revision aids of Level 1 (exercises/11-exam-readiness-1), Level 2 (exercises/44-exam-readiness-2) and
Level 3 (exercises/78-exam-readiness-3):
flashcards.json and review-bank.json in each folder.

Rules:
  - both files have the documented shape (see course/README.md); ids are unique and in order;
  - every card and item names a course page that exists, a module of its level (1 to 11, 12 to 43, or 45 to 77) and the domains of
    its exam (Associate AS1 to AS7, Developer DV1 to DV8, Architect A1 to A5);
  - Level 1: every module has at least 5 cards and 4 bank items, and every Associate domain at least 5 cards and 4 items;
    Level 2: every module 12 to 43 has at least 4 cards and 2 bank items, and every Developer domain at least 12 cards and
    8 items; Level 3 has the same minimums for modules 45 to 77 and the Architect domains; a Level 2 or 3 bank item is also not a near-duplicate (Jaccard 0.5 on stem and key stems) of any quiz or mock question;
  - a card's front and back are non-empty and short (front <= 200, back <= 420 characters);
  - a bank item has options a to d, a key among them, an explanation, no doubled word or cut-off ending, a key that
    does not echo its stem, a key at most 1.3 times the mean distractor length, a stem that is not a quiz question,
    keys spread over the letters (no letter on more than half) and the key the longest option in at most 40 percent;
  - nothing looks like personal data (an e-mail address or a home path).
usage: tools/check_revision.py   exit 1 on any finding
"""
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_quiz import ROOT, NEAR_DUP, check_question, check_text_shape, jaccard, key_is_longest, stems  # noqa: E402

PERSONAL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+|/home/|/Users/|[A-Z]:\\\\")
LEVELS = {
    1: {"dir": ROOT / "exercises" / "11-exam-readiness-1", "modules": range(1, 12), "domains": {f"AS{i}" for i in range(1, 8)},
        "mins": (5, 4, 5, 4), "label": "Level 1", "near": False},
    2: {"dir": ROOT / "exercises" / "44-exam-readiness-2", "modules": range(12, 44), "domains": {f"DV{i}" for i in range(1, 9)},
        "mins": (4, 2, 12, 8), "label": "Level 2", "near": True},
    3: {"dir": ROOT / "exercises" / "78-exam-readiness-3", "modules": range(45, 78), "domains": {f"A{i}" for i in range(1, 6)},
        "mins": (4, 2, 12, 8), "label": "Level 3", "near": True},
}


DIR = LEVELS[1]["dir"]
# Level 4 (Architect Professional): modules 79 to 93, domains P1 to P7; planned minimums as for Level 2, with fewer per domain.
LEVELS[4] = {"dir": ROOT / "exercises" / "94-exam-readiness-4", "modules": range(79, 94), "domains": {f"P{i}" for i in range(1, 8)},
             "mins": (4, 2, 10, 6), "label": "Level 4", "near": True}


def common(entry, kind, problems, cfg=LEVELS[1]):
    eid = entry.get("id", "?")
    page = entry.get("page", "")
    if not (ROOT / page).is_file():
        problems.append(f"{eid}: page {page!r} does not exist")
    modules = cfg["modules"]
    if entry.get("module") not in modules:
        problems.append(f"{eid}: module must be {modules.start} to {modules.stop - 1}")
    elif page and f"course/{entry['module']:02d}-" not in page:
        problems.append(f"{eid}: page {page!r} is not in module {entry['module']}")
    doms = entry.get("domains")
    if not doms or not set(doms) <= cfg["domains"]:
        problems.append(f"{eid}: domains must be a non-empty list from {min(cfg['domains'])} to {max(cfg['domains'])}")


def check_cards(data, cfg=LEVELS[1]):
    problems, cards = [], data.get("cards", [])
    if not cards:
        return ["flashcards.json: no cards"]
    for n, c in enumerate(cards, 1):
        if c.get("id") != f"fc-{n:03d}":
            problems.append(f"card {n}: id {c.get('id')!r}, want fc-{n:03d}")
        common(c, "card", problems, cfg)
        front, back = c.get("front", ""), c.get("back", "")
        if not front.strip() or not back.strip():
            problems.append(f"{c.get('id')}: empty front or back")
        if len(front) > 200 or len(back) > 420:
            problems.append(f"{c.get('id')}: front or back too long")
        for text in (front, back):
            problems += [f"{c.get('id')}: {p}" for p in check_text_shape(f"{c.get('id')} text", text) if "doubled" in p]
            if PERSONAL.search(text):
                problems.append(f"{c.get('id')}: looks like personal data")
    mod_cards, mod_items, dom_cards, dom_items = cfg["mins"]
    for m in cfg["modules"]:
        if sum(1 for c in cards if c.get("module") == m) < mod_cards:
            problems.append(f"flashcards: module {m} has fewer than {mod_cards} cards")
    for d in sorted(cfg["domains"]):
        if sum(1 for c in cards if d in c.get("domains", [])) < dom_cards:
            problems.append(f"flashcards: domain {d} has fewer than {dom_cards} cards")
    return problems


def quiz_stems():
    stems = set()
    for f in (ROOT / "exercises").glob("*/tests/quiz.json"):
        stems |= {q["stem"].strip() for q in json.loads(f.read_text())["quizzes"]}
    return stems


def quiz_item_stems():
    """{question id: stem and key stems} of every quiz and mock question, for the near-duplicate rule."""
    out = {}
    for f in (ROOT / "exercises").glob("*/tests/quiz.json"):
        for q in json.loads(f.read_text())["quizzes"]:
            out[q["id"]] = stems(re.sub(r"`[^`]*`", " ", q["stem"]) + " " + q["options"][q["key"]])
    return out


def check_bank(data, taken, cfg=LEVELS[1], quiz_items=None):
    problems, items = [], data.get("items", [])
    if not items:
        return ["review-bank.json: no items"]
    if data.get("intervals_days") != sorted(data.get("intervals_days", [])) or not data.get("intervals_days"):
        problems.append("review-bank.json: intervals_days must be a sorted, non-empty list")
    longest = 0
    for n, it in enumerate(items, 1):
        eid = it.get("id", "?")
        if eid != f"rb-{n:03d}":
            problems.append(f"item {n}: id {eid!r}, want rb-{n:03d}")
        common(it, "item", problems, cfg)
        opts, key = it.get("options", {}), it.get("key")
        if sorted(opts) != list("abcd"):
            problems.append(f"{eid}: options must be a to d")
            continue
        if key not in opts:
            problems.append(f"{eid}: key {key!r} is not an option")
            continue
        if not it.get("explanation", "").strip():
            problems.append(f"{eid}: empty explanation")
        if it.get("stem", "").strip() in taken:
            problems.append(f"{eid}: stem repeats a quiz question")
        problems += check_question(eid, it.get("stem", ""), opts, key)
        for text in [it.get("explanation", "")]:
            problems += [p for p in check_text_shape(f"{eid} explanation", text) if "doubled" in p]
        blob = " ".join([it.get("stem", ""), it.get("explanation", "")] + list(opts.values()))
        if PERSONAL.search(blob):
            problems.append(f"{eid}: looks like personal data")
        longest += key_is_longest(opts, key)
    keys = [it.get("key") for it in items]
    for letter in "abcd":
        if keys.count(letter) > len(keys) / 2:
            problems.append(f"review-bank.json: key letter {letter} is overused")
    if longest / len(items) > 0.4:
        problems.append(f"review-bank.json: the key is the longest option in {longest} of {len(items)} items")
    mod_cards, mod_items, dom_cards, dom_items = cfg["mins"]
    for m in cfg["modules"]:
        if sum(1 for it in items if it.get("module") == m) < mod_items:
            problems.append(f"review-bank: module {m} has fewer than {mod_items} items")
    for d in sorted(cfg["domains"]):
        if sum(1 for it in items if d in it.get("domains", [])) < dom_items:
            problems.append(f"review-bank: domain {d} has fewer than {dom_items} items")
    if cfg["near"] and quiz_items:
        for it in items:
            mine = stems(re.sub(r"`[^`]*`", " ", it.get("stem", "")) + " " + it.get("options", {}).get(it.get("key"), ""))
            for qid, theirs in quiz_items.items():
                if jaccard(mine, theirs) >= NEAR_DUP:
                    problems.append(f"{it.get('id')}: near-duplicate of the quiz question {qid}")
                    break
    return problems


def main():
    problems = []
    taken, near = quiz_stems(), quiz_item_stems()
    for level, cfg in LEVELS.items():
        files = {name: cfg["dir"] / name for name in ("flashcards.json", "review-bank.json")}
        missing = [n for n, f in files.items() if not f.is_file()]
        if missing:
            if level == 1:
                problems += [f"{cfg['label']}: missing {n}" for n in missing]
            continue
        cards = json.loads(files["flashcards.json"].read_text())
        bank = json.loads(files["review-bank.json"].read_text())
        found = check_cards(cards, cfg) + check_bank(bank, taken, cfg, near)
        problems += [f"{cfg['label']}: {p}" for p in found]
        print(f"{cfg['label']} flashcards: {len(cards['cards'])} cards; review bank: {len(bank['items'])} items")
    for p in problems:
        print(p)
    print("revision aids: ok" if not problems else f"revision aids: {len(problems)} finding(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
