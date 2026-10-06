# Quiz quality pass: how to continue it

What the pass changes and what a site build must know is in `../QUIZ-QUALITY-PASS.md`. This folder is for an agent
that picks the pass up and carries it on. Everything here is process material; nothing in `tools/` was changed.

## Batches

Base commit of the pass: `c32f09b`. A batch is a set of modules; a level's mock exam pages run after the rest of
that level.

| Batch | Modules | State |
|---|---|---|
| 1 | 1 to 10, then the Level 1 mock exams (module 11, pages 04 and 05) | done |
| 2 | 12 to 27 | running |
| 3 | 28 to 43, then the Developer mock exams (module 44, pages 03 and 04) | waits for batch 2 |
| 4 | 45 to 61 | running |
| 5 | 62 to 77, then the Architect mocks and pool (module 78, pages 03, 04 and 05) | waits for batch 4 |
| 6 | 79 to 93, then the Professional mock exams (module 94, pages 03 and 04) | running |

The State column is updated when a batch completes; between those, `snapshot.py` pushes the consistent modules about every 20 minutes. To see what is done for a module, compare it with the base:
`python3 docs/process/quiz-pass/check_invariants.py . c32f09b <prefix>` lists the changed items. A module with no
changed items is either not started or had nothing weak: check `git log -- course/<module>`.

## Files here

- `quiz-batch-workflow.js`: the workflow that runs a batch. Per module (or per mock page): an author judges every
  item against the rubric in the script and reworks the weak ones; an independent reader judges the result; up to two
  fix and re-read rounds; then a resolve step for items still weak (repair once more, or restore the base text), a
  judge that compares each with its base, and a revert of any item judged worse than its base. Arguments: `repo` (the
  checkout to edit), `scratch` (this folder, for the helper scripts), `base`, `modules` (folder names),
  `mockPages` and `mockPageList` (`{module, page}`), or `resolveOnly` with `openFile` for a resolve pass alone.
- `build_one.py <repo> <prefix>...`: rebuilds `quiz.json` for the named modules only, with the logic of
  `tools/build_quiz_json.py` (which rewrites every module and so cannot run while other modules are mid-edit).
- `check_invariants.py <repo> <base> [prefix...]`: every `quiz.json` against the base: same ids, order, page, scope,
  key letter, select count and option letters; prints the changed items.
- `check_prose.py <repo> <base>`: every changed page against the base outside its quiz sections; must print
  "prose lines added 0, removed 0" unless an approved one-sentence addition is listed in `../QUIZ-QUALITY-PASS.md`.
- `snapshot.py <main checkout> <base> <branch> [worktree...]`: copies every consistent module from the batch
  worktrees into the main checkout, stages only modules that pass `check_quiz`, the invariants and the prose check,
  commits and pushes. A module caught mid-edit waits for the next snapshot.
- `batch1-open-items.json`: the reader's verdicts on the Level 1 items still weak after two fix rounds (the input of
  the batch 1 resolve pass).

## Finishing a batch

1. Run the batch (worktree beside the project, one per batch, so its checks are not disturbed by others).
2. When it returns, copy its modules into the main checkout (or run `snapshot.py`), then run the checks listed in
   `../QUIZ-QUALITY-PASS.md` ("Checks run for every batch") on the whole course.
3. Update `../QUIZ-POLISH.md`: replace the batch's entries with the items a reader still judges weak, with reasons.
4. Add the batch's row to the batch log in `../QUIZ-QUALITY-PASS.md`, commit and push.
