# Cloud rounds: file-only work in a cloud session

A cloud round is a round (see `README.md` here) run in a cloud session that has only this repository,
no Docker and no network beyond it. It reads and edits files and runs the file-only checks. Everything
that needs containers (practice gates, image builds, the site) is done later by the register on the
host.

## How to run one

You were started with a line such as: *read `docs/process/rounds/CLOUD-BRIEF.md` and run round
`<id>`*. Then:

1. Read `CLAUDE.md`, `docs/process/QUIZ-POLISH.md` and the round's spec `docs/process/rounds/<id>.json`.
2. Create branch `cloud/<id>` from the spec's input commit (`git switch -c cloud/<id> <commit>`). Never
   rebase it, never merge `main` into it.
3. Do the round's task (below). Edit only the files in its scope.
4. Run every command in the spec's `gates` list. Each must exit 0 with no findings.
5. Write `docs/process/rounds/<id>.handback.json`:
   `{"round": "<id>", "branch": "cloud/<id>", "head": "<sha of your last content commit>",
   "gates": {"<command>": "<last output line>"}, "changed_files": <count>, "fixed": [<one line per
   fix>], "open": [<one line per finding you did not fix, with the reason>]}`. Commit it last.
6. Push the branch `cloud/<id>`. Never push `main` or any other branch.

## Rules for every cloud round

- No personal data in any file or commit: no names, emails, accounts, machine names or home paths.
  Use placeholders.
- No conversation in files: write fixes as plain statements of the change.
- Never copy text from a source page; write in the course's own words. Do not name sources.
- A claim about a product behaviour stays as it is unless the page itself contradicts it; list doubtful
  claims under `open` instead of rewriting them from memory.
- Quiz rules (`CLAUDE.md`, "Quiz"): the key never repeats a word of the stem, options are parallel in
  form, every wrong option is ruled out by a passage on the page, the folded key explains the key
  option and matches `quiz.json` (rebuild it with `python3 tools/build_quiz_json.py` after any quiz
  edit).
- Commit in small steps (one module or one page per commit), message ending with
  `Co-Authored-By: Claude <noreply@anthropic.com>`.
- Do not edit `tools/`, `exercises/`, `examples/`, the board or anything under `docs/` except your
  hand-back file.

## Round tasks

### cloud-readthrough-l1, -l2, -l3, -l4 (one level each)

Scope: `course/<module>/*.md` for the modules in the spec's range, **except** the mock exam and pool
pages: `course/11-exam-readiness-1/04-*.md`, `05-*.md`; `course/44-exam-readiness-2/03-*.md`, `04-*.md`;
`course/78-exam-readiness-3/03-*.md`, `04-*.md`, `05-*.md`; `course/94-exam-readiness-4/03-*.md`,
`04-*.md`; plus the `quiz.json` files that `build_quiz_json.py` rewrites for your modules.

Read every page in order, as a learner of that level would. Fix:
- sentences that are unclear, too long, or say the same thing twice;
- a term used before the page explains it, or used with two meanings;
- a statement that contradicts another page of the course (name both pages in the fix line);
- page quizzes that break the quiz rules above;
- a broken link to another page, or a heading that does not match its content.
Do not change code blocks, example output blocks, practice statements or the page structure. Keep each
page's length within 10% of the original.

### cloud-mock-polish

Scope: the mock exam and pool pages listed above, and their `quiz.json` entries.

Fix:
- options written as participle tails (", X being Y"): rewrite each as natural English. Do not add a
  reason clause (", since", ", because", ", as", "so that") to only some options of an item;
  `check_quiz.py` refuses that;
- rule-outs (the folded key's sentence for a wrong option) that only repeat the key's quote: each must
  point to the passage that makes **that** option false. Known: pool items 1, 2, 3, 10, 11, 14, 15, 16,
  17, 23, 24, 26, 29, 30, and about 70 mock items with the same pattern;
- Level 1 mock 1 item 31: it asks "Which two statements" but three options are noun phrases; make
  every option a statement.
Keep each item's key, its domain, the count per domain and the "Select two" items' form unchanged.

### cloud-r2-l1, -l2, -l3, -l4 (second pass, one level each)

Start by reading the `open` list of the first-pass hand-backs for your level
(`docs/process/rounds/cloud-readthrough-l<N>.handback.json`) and, for the mock items, of
`cloud-mock-polish.handback.json`. Those lists are your task list.

Scope: every `course/<module>/*.md` of your level **including** that level's mock exam and pool pages
(Level 1: module 11; Level 2: module 44; Level 3: module 78; Level 4: module 94), and their
`quiz.json` files.

1. **Product claims.** For each claim the first pass left unverified, check it against the official
   documentation (docs.claude.com, code.claude.com, support.claude.com, anthropic.com, and the
   official SDK repositories). Fix the sentence if the documentation says otherwise; if the
   documentation is silent, soften the sentence to what it does say. Do not name or link the
   documentation page in the course. List each claim in `fixed` or `open` with the URL you checked
   (the URL goes only in the hand-back, never in a course file).
2. **Sentences quoted by mock items.** When you change a lesson sentence that a mock item or pool item
   quotes, update that item's quote and rule-out in the same commit. If the quoting item belongs to
   another level, do not change the sentence; list it under `open`.
3. **Weak quiz items.** Rework the items the first pass listed as weak, loose rule-outs or key-longest:
   new options where needed, every wrong option ruled out by a passage on the page (add a short passage
   to the page when the page lacks one, inside the 10% length limit).
4. **Small items** from the lists: consistent spelling within a level, a missing scenario label, a
   module named without its number, gendered pronouns for unnamed people (use they/them).
Keep keys, domains, counts per domain and the "Select two" form of mock items unchanged.
Out of scope: `exercises/`, `examples/` and `tools/` (list any practice or example mismatch under
`open`).
