#!/usr/bin/env python3
"""Planted-defect tests for tools/check_quiz.py: each new check must flag its defect and pass the clean form.
usage: tools/test_check_quiz.py   exit 1 when a plant is not caught or a clean form is flagged
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_quiz import check_key_paragraph, check_question, key_is_longest  # noqa: E402

STEM = "A nightly job rejects the largest reports after the vendor changes the tokenizer settings."
CLEAN = {"a": "Count the tokens again for the target model", "b": "Split every document into chapters by hand",
         "c": "Raise the output cap on the request body", "d": "Switch the job to a cheaper model tier"}
failures = []


def expect(name, problems, want):
    got = bool(problems)
    print(f"{'ok  ' if got == want else 'FAIL'} {name}: {'flagged' if got else 'clean'}" + (f" ({problems[0]})" if problems else ""))
    if got != want:
        failures.append(name)


expect("clean question", check_question("t#q1", STEM, CLEAN, "a"), False)

# (a) key far longer than the distractors
long_key = dict(CLEAN, a="Count the tokens again for the target model before every deployment and compare the result to the window")
expect("plant: key 1.3x longer", check_question("t#q1", STEM, long_key, "a"), True)
expect("plant: key longest is detected", [1] if key_is_longest(long_key, "a") else [], True)

# (b) stem echo by stem, not by exact word
echo = dict(CLEAN, a="Tokenizing the documents again for the target")  # tokenizing ~ tokenizer
expect("plant: stem echo (tokenizing/tokenizer)", check_question("t#q1", STEM, echo, "a"), True)
echo2 = dict(CLEAN, a="Rejecting any oversize input early")  # rejecting ~ rejects
expect("plant: stem echo (rejecting/rejects)", check_question("t#q1", STEM, echo2, "a"), True)

# (c) merged explanation in the folded key
good = "**a**. Because the tokenizer changed. *b* is ruled out because x. *c* is ruled out because y. *d* is ruled out because z."
bad = "**a**. Because the tokenizer changed. *b*, *c* and *d* are ruled out because they do nothing."
missing = "**a**. Because the tokenizer changed. *b* is ruled out because x. *c* is ruled out because y."
expect("clean folded key", check_key_paragraph("t#q1", good, "a"), False)
expect("plant: merged folded key", check_key_paragraph("t#q1", bad, "a"), True)
expect("plant: option without a sentence", check_key_paragraph("t#q1", missing, "a"), True)

sys.exit(1 if failures else 0)
