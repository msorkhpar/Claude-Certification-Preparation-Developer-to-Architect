#!/usr/bin/env python3
"""Planted-defect tests for tools/check_quiz.py: each new check must flag its defect and pass the clean form.
usage: tools/test_check_quiz.py   exit 1 when a plant is not caught or a clean form is flagged
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from check_quiz import (check_multi, keys_longest, parse_page_quizzes, select_count, check_duplicate, check_named_page, check_page_has_quiz, check_near_duplicates, longest_verdict, check_key_paragraph, check_question, check_quotes,  # noqa: E402
                        key_is_longest, check_mock_item, check_mock_extremes)

STEM = "A nightly job rejects the largest reports after the vendor changes the tokenizer settings."
CLEAN = {"a": "Measure the input again for the target model", "b": "Split every document into chapters by hand",
         "c": "Raise the output cap on the request body", "d": "Switch the job to a cheaper model tier"}
failures = []


def expect(name, problems, want):
    got = bool(problems)
    print(f"{'ok  ' if got == want else 'FAIL'} {name}: {'flagged' if got else 'clean'}" + (f" ({problems[0]})" if problems else ""))
    if got != want:
        failures.append(name)


expect("clean question", check_question("t#q1", STEM, CLEAN, "a"), False)

# (a) key far longer than the distractors
long_key = dict(CLEAN, a="Measure the input again for the target model before every deployment and compare the result to the window")
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

# (d) stem echo across hyphens and inside longer words
S1 = "A job sends every report to a model with a 1M-token window and the largest reports fail. What is the cause?"
O1 = {"a": "The output cap is counted first now, before the prompt", "b": "Earlier requests are kept and fill the space over time", "c": "More tokens per page after the change", "d": "The vendor quietly lowered the limit for everyone"}
expect("plant: hyphenated stem word (1M-token / tokens)", check_question("t#q1", S1, O1, "c"), True)
expect("clean: same stem, key reworded", check_question("t#q1", S1, dict(O1, c="The newer tokenizer cuts prose into finer pieces"), "c"), True)
expect("clean: key shares nothing", check_question("t#q1", S1, dict(O1, c="Newer text splitting yields finer pieces from identical prose"), "c"), False)
S2 = "A support chat sends only the latest question to the model. The assistant asks for the order number again."
O2 = {"a": "Add a system line about each customer who writes to us", "b": "Use the largest window available on the market now", "c": "Raise the output cap on every reply the assistant gives", "d": "Store the dialogue and resend it each time"}
expect("plant: stem inside longer word (sends / resend)", check_question("t#q1", S2, O2, "d"), True)
expect("clean: key reworded (replay)", check_question("t#q1", S2, dict(O2, d="Keep the dialogue in the application and replay it with every call"), "d"), False)
S3 = "A marketing assistant tells Claude to make it better and gets a rewrite in an unwanted voice. What is the next step?"
O3 = {"a": "Ask again with the same wording and pick the best reply", "b": "Move to the highest tier that exists on the platform", "c": "Edit the whole rewrite by hand until it suits the brand", "d": "Describe the reader and the tone wanted, then compare"}
expect("plant: key word inside stem word (wanted / unwanted)", check_question("t#q1", S3, O3, "d"), True)
expect("clean: key reworded (register)", check_question("t#q1", S3, dict(O3, d="Describe the reader and the register to aim for, then compare"), "d"), False)

# (e) every non-key option is ruled out by a verbatim quotation of 4 or more words from the page
PAGE = "The API remembers nothing. Your code resends the whole history on every request. A cap on output is not a cap on context."
EXP = {"a": "The answer.", "b": 'Ruled out because "your code resends the whole history on every request".',
       "c": 'Ruled out because "a cap on output is not a cap on context".', "d": 'Ruled out because "the API remembers nothing".'}
expect("clean quotations", check_quotes("t#q1", EXP, "a", PAGE), False)
expect("plant: option without a quotation", check_quotes("t#q1", dict(EXP, b="Ruled out because it adds nothing."), "a", PAGE), True)
expect("plant: quotation not on the page", check_quotes("t#q1", dict(EXP, c='Ruled out because "a cap on output is a cap on window".'), "a", PAGE), True)
expect("plant: quotation under 4 words", check_quotes("t#q1", dict(EXP, d='Ruled out because "remembers nothing".'), "a", PAGE), True)

# (f) a module question must not restate a page question of the same module
PAGES = {"p1#q1": "A support chat sends only the latest question and the assistant asks for the order number again."}
expect("plant: module stem restates a page stem",
       check_duplicate("p1#m1", "A support chat sends only the latest question, so the order number is asked for again.", PAGES), True)
expect("clean: module stem on another scenario",
       check_duplicate("p1#m2", "A localisation lead sees one paragraph cost more in one script than in another.", PAGES), False)

# (g) doubled word and truncation
expect("plant: doubled word in an option", check_question("t#q1", STEM, dict(CLEAN, b="Split every every document into chapters by hand"), "a"), True)
expect("plant: doubled word in the stem", check_question("t#q1", STEM + " Which step is is right?", CLEAN, "a"), True)
expect("plant: doubled word in an explanation", check_quotes("t#q1", dict(EXP, b='Ruled out because because "your code resends the whole history on every request".'), "a", PAGE), True)
expect("plant: option ends with a preposition", check_question("t#q1", STEM, dict(CLEAN, b="Split every document into chapters by"), "a"), True)
expect("plant: option ends with an article", check_question("t#q1", STEM, dict(CLEAN, c="Raise the output cap on the"), "a"), True)
expect("plant: stem ends with a conjunction", check_question("t#q1", STEM[:-1] + " and", CLEAN, "a"), True)
expect("clean: well-formed option ending", check_question("t#q1", STEM, dict(CLEAN, b="Split every document into chapters, by hand."), "a"), False)

# (h) form tell, giveaway stem word, near-duplicates, key-longest rate
ABS = {"a": "Measure the input again for the target model", "b": "Always split every document into chapters by hand",
       "c": "Never raise the output cap on the request body", "d": "Ignore the tokenizer and keep the old limit"}
expect("plant: all distractors absolute, key plain", check_question("t#q1", STEM, ABS, "a"), True)
expect("clean: one distractor absolute", check_question("t#q1", STEM, dict(CLEAN, b="Always split documents into chapters by hand"), "a"), False)
HEDGED = dict(CLEAN, a="Measure the input again where needed for the target model")
expect("plant: key alone hedged", check_question("t#q1", STEM, HEDGED, "a"), True)
expect("clean: hedge shared with a distractor", check_question("t#q1", STEM, dict(HEDGED, c="Raise the output cap where needed on the request body"), "a"), False)
for word in ("balanced", "safest", "proper", "correct way", "right way", "best-practice"):
    expect(f"plant: stem word {word}", check_question("t#q1", f"The team wants the {word} fix after the vendor changes the tokenizer settings.", CLEAN, "a"), True)
expect("clean: stem without evaluative word", check_question("t#q1", STEM, CLEAN, "a"), False)
D = {"p#q1": ("A nightly job rejects the largest reports after the vendor changes tokenizer settings", "Measure the input again for the target model"),
     "p#q2": ("A nightly job rejects the largest reports after the vendor changes tokenizer settings", "Measure the input again for the new target model"),
     "p#q3": ("A localisation lead sees one paragraph cost more in one script than another", "Compare token counts per script")}
expect("plant: near-duplicate questions", check_near_duplicates(D), True)
expect("clean: distinct questions", check_near_duplicates({k: v for k, v in D.items() if k != "p#q2"}), False)
# the duplicate rule is level-wide: a Level 3 question that restates a Level 2 question is flagged, whatever the module numbers
CROSS = {"35/02#q2": D["p#q1"], "46/01#q1": D["p#q2"], "47/01#q1": D["p#q3"]}
expect("plant: near-duplicate across Level 2 and Level 3", [p for p in check_near_duplicates(CROSS) if "35/02#q2" in p and "46/01#q1" in p], True)
expect("clean: unrelated questions across Level 2 and Level 3", [p for p in check_near_duplicates(CROSS) if "47/01#q1" in p], False)
expect("plant: key longest above 40 percent fails", [f for f in [longest_verdict("m", 5, 10)[0]] if f], True)
expect("plant: key longest 35 percent warns only", [f for f in [longest_verdict("m", 7, 20)[0]] if f], False)
expect("plant: key longest 35 percent gives a warning", [w for w in [longest_verdict("m", 7, 20)[1]] if w], True)
expect("clean: key longest 30 percent", [w for w in longest_verdict("m", 3, 10)] and [w for w in longest_verdict("m", 3, 10) if w], False)

# (d) a Developer mock question names the page that answers it and quotes it
GOOD = 'Because the page says "Not every success criteria or failing eval is best solved by prompt engineering" (module 42, page 1).'
expect("clean: key explanation names its page and quotes it", check_named_page("x#x1", GOOD), False)
expect("plant: no page named", check_named_page("x#x1", 'The page says "Not every success criteria or failing eval is best solved by prompt engineering".'), True)
expect("plant: page does not exist", check_named_page("x#x1", GOOD.replace("module 42, page 1", "module 42, page 9")), True)
expect("plant: quotation is not on the named page", check_named_page("x#x1", GOOD.replace("module 42, page 1", "module 42, page 2")), True)
expect("plant: the named page is quoted by a phrase of fewer than four words", check_named_page("x#x1", 'Because "failing eval" (module 42, page 1).'), True)

# a page may go without a quiz only when it is an exam-readiness page
ORDINARY = "# A page\n\n**Level:** Foundations \u00b7 **Module 3:** Claude's family and its surfaces \u00b7 **Page 1 of 4**\n\n## The idea\n\ntext\n"
READY = ORDINARY.replace("Claude's family and its surfaces", "Exam readiness 1")
expect("plant: an ordinary page without a quiz is refused", check_page_has_quiz("p.md", ORDINARY), True)
expect("clean: an exam-readiness page may have no quiz", check_page_has_quiz("p.md", READY), False)
expect("clean: an ordinary page with a quiz", check_page_has_quiz("p.md", ORDINARY + "\n## Quiz\n\n1. Q?\n   - **a**: x\n"), False)

# (i) multiple-response items: "(Select two.)", five options, a keyed pair written "**a and c**"
MS = "A nightly job rejects the largest reports after the vendor changes the tokenizer settings. Which two steps fit? (Select two.)"
MO = {"a": "Measure the input again for the target model", "b": "Split every document into chapters by hand",
      "c": "Compare piece counts before any release", "d": "Raise the output cap on the request body", "e": "Switch the job to a cheaper model tier"}
expect("clean multiple-response item", check_multi("t#x1", MS, MO, ["a", "c"]), False)
expect("plant: marker says two but one letter is keyed", check_multi("t#x1", MS, MO, "a"), True)
expect("plant: two letters keyed and no marker", check_multi("t#x1", MS.replace(" (Select two.)", ""), MO, ["a", "c"]), True)
expect("plant: marker says three, two keyed", check_multi("t#x1", MS.replace("two.", "three."), MO, ["a", "c"]), True)
expect("plant: every option keyed", check_multi("t#x1", MS.replace("two.", "five."), MO, ["a", "b", "c", "d", "e"]), True)
expect("plant: key letters out of order", check_multi("t#x1", MS, MO, ["c", "a"]), True)
expect("plant: a multiple-response item with four options", check_multi("t#x1", MS, {k: v for k, v in MO.items() if k != "e"}, ["a", "c"]), True)
expect("plant: a keyed option repeats a stem word", check_multi("t#x1", MS, dict(MO, c="Compare the tokenizer outputs before any release"), ["a", "c"]), True)
expect("plant: keyed options far longer than the distractors", check_multi("t#x1", MS, dict(MO, a="Measure the input again for the target model before every deployment and compare it", c="Compare piece counts before any release and after every vendor update of the tool"), ["a", "c"]), True)
expect("clean: a single-answer item passes through", check_multi("t#q1", STEM, CLEAN, "a"), False)
expect("plant: select_count reads the marker", [1] if select_count(MS) == 2 and select_count(STEM) is None else [], True)
expect("plant: a longest keyed option is detected", [1] if keys_longest(dict(MO, a="Measure the input again for the target model and then compare it with last month"), ["a", "c"]) else [], True)
PAGE_MULTI = """## Mock exam

1. Which two steps fit? (Select two.)
   - **a**: one
   - **b**: two
   - **c**: three
   - **d**: four
   - **e**: five

<details>
<summary>Answer key</summary>

1. **a and c**. Because. *b* is ruled out because x. *d* is ruled out because y. *e* is ruled out because z.

</details>
"""
(_, _qs, _keys), = parse_page_quizzes(PAGE_MULTI)
expect("clean: a page parses a pair key and five options", [] if _keys == [["a", "c"]] and sorted(_qs[0][1]) == list("abcde") else [1], False)
expect("clean folded key for a pair", check_key_paragraph("t#x1", "**a and c**. Because. *b* is ruled out because x. *d* is ruled out because y. *e* is ruled out because z.", ["a", "c"], "abcde"), False)
expect("plant: folded key for a pair misses an option", check_key_paragraph("t#x1", "**a and c**. Because. *b* is ruled out because x. *d* is ruled out because y.", ["a", "c"], "abcde"), True)
expect("plant: folded key for a pair merges two options", check_key_paragraph("t#x1", "**a and c**. Because. *b*, *d* and *e* are ruled out because they do nothing.", ["a", "c"], "abcde"), True)

# mock-page tells: reason-clause asymmetry, extreme key length, duplicate options, odd form
M_CLEAN = {"a": "Measure the input again for the target model", "b": "Split every document into chapters by hand",
           "c": "Raise the output cap on the request body", "d": "Switch the job to a cheaper model tier"}
expect("clean mock item", check_mock_item("t#x1", M_CLEAN, "a"), False)
expect("plant: reason clause only on distractors", check_mock_item("t#x1", dict(M_CLEAN, b="Split every document by hand, since chapters are small"), "a"), True)
expect("plant: reason clause only on the key", check_mock_item("t#x1", dict(M_CLEAN, a="Measure the input again, because the tokenizer changed"), "a"), True)
expect("clean: reason clause on key and distractor", check_mock_item("t#x1", dict(M_CLEAN, a="Measure the input again, since the tokenizer changed", b="Split every document by hand, since chapters are small"), "a"), False)
expect("plant: select-two key set lacks the clause the rest carry", check_mock_item("t#x1", {"a": "Measure the input again", "b": "Split by hand, so that chapters stay small", "c": "Check the new limit", "d": "Raise the cap, as it is cheap", "e": "Switch tiers"}, ["a", "c"]), True)
expect("plant: duplicate options", check_mock_item("t#x1", dict(M_CLEAN, c="split every document into chapters by hand."), "a"), True)
expect("plant: key is an odd phrase among clauses", check_mock_item("t#x1", {"a": "Remeasuring the input", "b": "Raising fails when the cap is low", "c": "The window is too small", "d": "Tokens are counted twice"}, "a"), True)
expect("plant: every other option opens with the same word", check_mock_item("t#x1", {"a": "Remeasure the input", "b": "Cut the input by hand", "c": "Cut the cap on the body", "d": "Cut the tier of the job"}, "a"), True)
ROWS_LONG = [({"a": "x" * 30, "b": "y" * 10, "c": "z" * 11, "d": "w" * 12}, "a")] * 4 + [({"a": "x" * 5, "b": "y" * 20, "c": "z" * 11, "d": "w" * 25}, "a")] * 4
expect("plant: key is the longest in a third of the items", check_mock_extremes("t", ROWS_LONG[:2] + ROWS_LONG[4:]), True)
expect("plant: key is the longest in more than a quarter", check_mock_extremes("t", ROWS_LONG[:3] + ROWS_LONG[4:6]), True)
expect("plant: key is the shortest in more than a quarter", check_mock_extremes("t", ROWS_LONG[4:]), True)
expect("clean: key length is spread", check_mock_extremes("t", [({"a": "x" * 15, "b": "y" * 10, "c": "z" * 20, "d": "w" * 25}, "a")] * 4), False)

sys.exit(1 if failures else 0)
