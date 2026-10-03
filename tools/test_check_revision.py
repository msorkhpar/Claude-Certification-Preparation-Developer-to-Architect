#!/usr/bin/env python3
"""Planted-defect tests for tools/check_revision.py: each defect must be flagged and the clean data must pass.
usage: tools/test_check_revision.py   exit 1 when a plant is not caught or the clean data is flagged
"""
import copy
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_revision as cr  # noqa: E402

cards = json.loads((cr.DIR / "flashcards.json").read_text())
bank = json.loads((cr.DIR / "review-bank.json").read_text())
taken = cr.quiz_stems()
failures = []


def expect(name, problems, want, needle=""):
    problems = [x for x in problems if needle in x] if want else problems
    got = bool(problems)
    print(f"{'ok  ' if got == want else 'FAIL'} {name}: {'flagged' if got else 'clean'}" + (f" ({problems[0]})" if problems else ""))
    if got != want:
        failures.append(name)


def mutate(data, fn):
    d = copy.deepcopy(data)
    fn(d)
    return d


expect("clean cards", cr.check_cards(cards), False)
expect("clean bank", cr.check_bank(bank, taken), False)
expect("plant: card with a missing page", cr.check_cards(mutate(cards, lambda d: d["cards"][0].update(page="course/none.md"))), True, "does not exist")
expect("plant: card with an unknown domain", cr.check_cards(mutate(cards, lambda d: d["cards"][1].update(domains=["AS9"]))), True, "domains")
expect("plant: card with an empty back", cr.check_cards(mutate(cards, lambda d: d["cards"][2].update(back=" "))), True, "empty")
expect("plant: card with a personal path", cr.check_cards(mutate(cards, lambda d: d["cards"][3].update(back="see /home/someone/notes"))), True, "personal")
expect("plant: card id out of order", cr.check_cards(mutate(cards, lambda d: d["cards"][4].update(id="fc-999"))), True, "want fc-005")
expect("plant: module without cards", cr.check_cards(mutate(cards, lambda d: [c.update(module=8, page=c["page"].replace("09-claude-for-every-role", "08-claudes-apps-in-depth")) for c in d["cards"] if c["module"] == 9])), True, "module 9")
expect("plant: bank key not an option", cr.check_bank(mutate(bank, lambda d: d["items"][0].update(key="e")), taken), True, "not an option")
expect("plant: bank item missing an option", cr.check_bank(mutate(bank, lambda d: d["items"][1]["options"].pop("d")), taken), True, "a to d")
expect("plant: key far longer than the others",
       cr.check_bank(mutate(bank, lambda d: d["items"][2]["options"].update({d["items"][2]["key"]: " ".join(["Alpha", "bravo"] * 45)})), taken), True, "times the mean")
expect("plant: key repeats a stem word",
       cr.check_bank(mutate(bank, lambda d: d["items"][3]["options"].update({d["items"][3]["key"]: "Constrain the output to a fixed list and validate in code, as the team wants"})), taken), True, "repeats stem words")
expect("plant: bank item repeats a quiz stem",
       cr.check_bank(mutate(bank, lambda d: d["items"][4].update(stem=sorted(taken)[0])), taken), True, "repeats a quiz")
expect("plant: every key is a", cr.check_bank(mutate(bank, lambda d: [it.update(key="a", options=dict(it["options"], a=it["options"]["a"])) for it in d["items"]]), taken), True, "overused")
expect("plant: doubled word in an option",
       cr.check_bank(mutate(bank, lambda d: d["items"][5]["options"].update({"a" if d["items"][5]["key"] != "a" else "b": "Ask it it to try again"})), taken), True, "doubled")
if failures:
    print(f"{len(failures)} plant(s) not caught: {failures}")
    sys.exit(1)
print("check_revision planted-defect tests: ok")
