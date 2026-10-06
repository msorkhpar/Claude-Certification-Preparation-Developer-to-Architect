# Quiz quality pass: what changed and what a site build must know

This pass improves the quiz items of the course: page quizzes, module quizzes, mock exams and the Architect scenario
question pool. It changes content only. No tool, practice, example, harness file, page layout, data format or site
behaviour is changed. This file is written for whoever builds or rebuilds the course site from these pages.

## What a quiz item must now meet

Every item was judged against the course quiz rules in `CLAUDE.md` with these points made strict:

- **Reasoning over recall.** The key is reached by applying the page's idea to the scenario, not by spotting a page
  sentence copied into an option.
- **Plausible distractors.** Each wrong option is a mistake a practitioner would make: a technique that is right in a
  different situation, a misconception the page addresses, or a fix aimed at the symptom. No strawmen.
- **Parallel form.** The key is not the odd one out by length, shape, hedging, composition or specificity, and no two
  options form a mirror pair around the key.
- **No leak.** The stem does not paraphrase the key or state the rule the key applies.
- **One best answer.** The key is right according to the page, no distractor is also defensible, and an item that
  rests on the course's own example design says so in its stem.
- **Rule-outs that work.** Each wrong option's explanation quotes, verbatim, a passage that makes that option false.

Items that already met these points were left byte-identical. Every changed item was judged by an independent
reader. In batch 1, an item that a reader still judged weak after the fix rounds was repaired once more or restored
to its earlier text, and a judge compared it with its earlier version, so no item is worse than before the pass. In
the batches stopped part-way, some modules have not reached that comparison yet (see the batch log).

## What is guaranteed unchanged

These hold for every item in every module, checked by script against the commit before the pass:

| Property | Unchanged |
|---|---|
| Item ids (`<page slug>#q<n>`, `#m<n>`, `#x<n>`) and their order | yes |
| Number of items per page section and per module | yes |
| `page`, `scope` (`page`, `module`, `level`) and `select` (multiple response) of each item | yes |
| Key letter of each item, and the option letters (a to d, or a to e for "(Select two.)") | yes |
| Mock exam domains (fixed by item position) and the module and page each mock item is drawn from | yes |
| `quiz.json` file names, location (`exercises/<module>/tests/quiz.json`) and JSON shape | yes |
| `sittings.json`, `flashcards.json`, `review-bank.json` | not touched |
| Lesson prose, code blocks, example output, tables, headings, links and page header lines | not touched (see below) |
| Practices, examples, tools, harness | not touched |

What does change: the **text** of stems, options and folded answer-key paragraphs on the pages, and the same text in
the generated `quiz.json` files (`stem`, `options`, `explanation`).

## What a site build should do

1. **Rebuild from the pages.** The pages are the source; `quiz.json` is generated from them with
   `python3 tools/build_quiz_json.py`. Never hand-edit `quiz.json`. Running the builder on this branch produces no
   diff, which is how to confirm the two agree.
2. **Regenerate anything derived from quiz text.** Pre-rendered quiz HTML, a search index, narration or audio of quiz
   sections, and any cached copy of a page must be rebuilt for the changed pages. The list of changed items per
   module is under "Batch log" below; `git diff <base> -- course exercises` gives the exact text.
3. **Stored learner state stays valid by id.** Progress or answers keyed by item id still point at the same item,
   and the same key letter is still correct. The wording behind the id changed, so a stored copy of an old stem or
   explanation should not be shown next to the new one.
4. **Quote matching.** Explanations quote lesson passages verbatim, and a quote may span a line break in the page
   source. A site feature that highlights a quoted passage on the page must normalise whitespace (as
   `tools/check_quiz.py` does) before matching.
5. **Markdown inside items is unchanged in kind.** Items use plain text with inline code in backticks, as before; no
   links, HTML or line breaks inside an option were introduced. A multi-line option in the source is joined by the
   builder, as before.
6. **Key letters.** The pass kept every key letter, so the per-module key-letter sequences are as before (see "Open
   points"). If the site shuffles options, nothing in this pass depends on their order.

## Checks run for every batch

All from the repository root, all file-only (no container):

- `python3 tools/build_quiz_json.py` then `git status`: no change beyond the batch's pages and their `quiz.json`;
- `python3 tools/check_quiz.py`: every module `ok`, no finding;
- `python3 tools/check_revision.py`, `python3 tools/check_coverage.py`: exit 0;
- `python3 tools/check_personal_data.py --modules '.*'`: 0 findings (run with the global git configuration masked,
  `GIT_CONFIG_GLOBAL=/dev/null`, because the checker reads the runner's git identity);
- `python3 -m unittest tools.test_check_quiz`: passes;
- an invariant comparison of every `quiz.json` with the base commit (ids, order, page, scope, key, select, option
  letters), and a comparison of every changed page outside its quiz sections (no lesson line added or removed).

## Batch log

Base commit of the pass: `c32f09b`. Each batch lands as one or more commits on this branch; a row is added when the batch is complete.

| Batch | Modules | Items judged | Items changed | Still weak (kept or restored) | Lesson lines changed |
|---|---|---|---|---|---|
| 1 | 1 to 11 (Level 1, both mock exams) | 220 | 195 | 22 (2 restored to their base text) | 0 |
| 2 (stopped part-way) | 12 to 27 | 166 | 152 | see `quiz-pass/stopped-state.json` | 0 |
| 4 (stopped part-way) | 45 to 61 | 123 | 104 | see `quiz-pass/stopped-state.json` | 0 |
| 6 (stopped part-way) | 79 to 93 (the module 94 mock exams not started) | 105 | 95 | see `quiz-pass/stopped-state.json` | 0 |

Batches 3 (modules 28 to 44) and 5 (62 to 78) and the module 94 mock exams are not started: their items are as they
were at the base commit. The pass stopped after the rows above; `quiz-pass/README.md` gives the stage
each touched module reached and how to continue.

## Items whose tested point changed

Most changed items test the same point in better form. These items now test a different point or carry a new
scenario, so a reader comparing them with an earlier copy should not expect the same question:

- Level 1: `07-claude-in-the-apps` 03#m1 (the earlier key, syncing the source, does not hold for a shared Project;
  the new key follows page 1) and 03#m4 (new scenario, same key letter); `08-claudes-apps-in-depth` 03#m3 (recast
  to the visible Chrome tabs, as it duplicated page 3 q2) and 03#m4 (new scenario); Level 1 mock 1 x27 (new stem).
- Level 3: `57-memory-files-and-rules` 02#m2 (now asks why imports written as inline code load nothing, instead of
  what splitting a file into imports does).
- Batches 2, 4 and 6 stopped before this list was compiled for them; `git diff c32f09b -- course/<module>` shows each
  item's change.

## Open points

- `docs/process/QUIZ-POLISH.md` lists the Level 1 items an independent reader still judges WEAK after this pass, with
  the reason. Its entries for Levels 2 to 4 predate the pass; for the modules this pass touched, the reader's current
  verdicts are in `quiz-pass/stopped-state.json`.
- Key letters follow a per-module sequence set by `tools/balance_keys.py`, and several modules share the same
  sequence. Varying it would move options in every module; it is left for a separate change.
