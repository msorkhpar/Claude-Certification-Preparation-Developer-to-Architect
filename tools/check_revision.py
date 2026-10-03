#!/usr/bin/env python3
"""Check the Level 1 revision aids: exercises/11-exam-readiness-1/flashcards.json and review-bank.json.

Rules:
  - both files have the documented shape (see course/README.md); ids are unique and in order;
  - every card and item names a course page that exists, a module from 1 to 11 and Associate domains AS1 to AS7;
  - every module 1 to 11 has at least 5 cards and 4 bank items, and every Associate domain at least 5 cards and 4 items;
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
from check_quiz import ROOT, check_question, check_text_shape, key_is_longest  # noqa: E402

DIR = ROOT / "exercises" / "11-exam-readiness-1"
DOMAINS = {f"AS{i}" for i in range(1, 8)}
PERSONAL = re.compile(r"[\w.+-]+@[\w-]+\.[\w.]+|/home/|/Users/|[A-Z]:\\\\")
MIN_PER_MODULE_CARDS, MIN_PER_MODULE_ITEMS, MIN_PER_DOMAIN_CARDS, MIN_PER_DOMAIN_ITEMS = 5, 4, 5, 4


def common(entry, kind, problems):
    eid = entry.get("id", "?")
    page = entry.get("page", "")
    if not (ROOT / page).is_file():
        problems.append(f"{eid}: page {page!r} does not exist")
    if entry.get("module") not in range(1, 12):
        problems.append(f"{eid}: module must be 1 to 11")
    elif page and f"course/{entry['module']:02d}-" not in page:
        problems.append(f"{eid}: page {page!r} is not in module {entry['module']}")
    doms = entry.get("domains")
    if not doms or not set(doms) <= DOMAINS:
        problems.append(f"{eid}: domains must be a non-empty list from AS1 to AS7")


def check_cards(data):
    problems, cards = [], data.get("cards", [])
    if not cards:
        return ["flashcards.json: no cards"]
    for n, c in enumerate(cards, 1):
        if c.get("id") != f"fc-{n:03d}":
            problems.append(f"card {n}: id {c.get('id')!r}, want fc-{n:03d}")
        common(c, "card", problems)
        front, back = c.get("front", ""), c.get("back", "")
        if not front.strip() or not back.strip():
            problems.append(f"{c.get('id')}: empty front or back")
        if len(front) > 200 or len(back) > 420:
            problems.append(f"{c.get('id')}: front or back too long")
        for text in (front, back):
            problems += [f"{c.get('id')}: {p}" for p in check_text_shape(f"{c.get('id')} text", text) if "doubled" in p]
            if PERSONAL.search(text):
                problems.append(f"{c.get('id')}: looks like personal data")
    for m in range(1, 12):
        if sum(1 for c in cards if c.get("module") == m) < MIN_PER_MODULE_CARDS:
            problems.append(f"flashcards: module {m} has fewer than {MIN_PER_MODULE_CARDS} cards")
    for d in sorted(DOMAINS):
        if sum(1 for c in cards if d in c.get("domains", [])) < MIN_PER_DOMAIN_CARDS:
            problems.append(f"flashcards: domain {d} has fewer than {MIN_PER_DOMAIN_CARDS} cards")
    return problems


def quiz_stems():
    stems = set()
    for f in (ROOT / "exercises").glob("*/tests/quiz.json"):
        stems |= {q["stem"].strip() for q in json.loads(f.read_text())["quizzes"]}
    return stems


def check_bank(data, taken):
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
        common(it, "item", problems)
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
    for m in range(1, 12):
        if sum(1 for it in items if it.get("module") == m) < MIN_PER_MODULE_ITEMS:
            problems.append(f"review-bank: module {m} has fewer than {MIN_PER_MODULE_ITEMS} items")
    for d in sorted(DOMAINS):
        if sum(1 for it in items if d in it.get("domains", [])) < MIN_PER_DOMAIN_ITEMS:
            problems.append(f"review-bank: domain {d} has fewer than {MIN_PER_DOMAIN_ITEMS} items")
    return problems


def main():
    problems = []
    for name in ("flashcards.json", "review-bank.json"):
        if not (DIR / name).is_file():
            problems.append(f"missing {name}")
    if not problems:
        cards = json.loads((DIR / "flashcards.json").read_text())
        bank = json.loads((DIR / "review-bank.json").read_text())
        problems += check_cards(cards)
        problems += check_bank(bank, quiz_stems())
        print(f"flashcards: {len(cards['cards'])} cards; review bank: {len(bank['items'])} items")
    for p in problems:
        print(p)
    print("revision aids: ok" if not problems else f"revision aids: {len(problems)} finding(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
