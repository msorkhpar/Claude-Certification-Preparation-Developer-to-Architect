# Cloud round cloud-r5-tryit-read: an independent reader for the try-it files

For a cloud session working alone on this repository. Read `CLAUDE.md` first. Push only the
branch `cloud/cloud-r5-tryit-read`. Budget cap: about 20 USD; stop and hand back what you have
when you near it.

## Background

In every practice, Run executes the reader's own try-it file (`<practice>/<lang>/tryit/`:
`try_it.py`, `try-it.ts`, `TryIt.java`, `TryIt.kt`) and shows what it printed and logged, with no
tests and no grade. Submit grades against the tests. The files were written in two batches.
You are the reader, not the author.

## Judge every try-it file against these rules

1. It calls the practice's own class or function on the example the statement gives, or on the
   tests' first main case when the statement has none. It uses the same stand-in the tests use.
2. It turns the logger up, so the reader's `log` lines show. `python3 tools/make_tryit.py check`
   enforces this.
3. It prints 3 to 5 readable result lines that a learner can compare with the statement. No
   test asserts, no pass or fail wording, no grading.
4. Its comments tell the learner that Run executes this file, that they may change the calls,
   and that Submit runs the tests. Plain words, current state only.
5. The four languages of one practice do the same thing, in each language's own idiom.
6. It contains no answer that the starter is meant to make the learner write.
7. It contains no personal data, keys or machine paths.

Fix only files that fail a rule, with the smallest change. You cannot run the JVM files; keep
their edits to comments and printed text unless a call is plainly wrong, and list each JVM
edit. Python files can be run:

    python3 tools/make_tryit.py run <practice dir> python reference

Run each Python file you change.

## Checks before the hand-back

`python3 tools/make_tryit.py check`, `python3 tools/check_logger.py`,
`python3 tools/check_personal_data.py --modules '.*'`: all exit 0.

## Hand back

Commit `docs/process/rounds/cloud-r5-tryit-read.handback.md`, at most 40 lines:
- the counts judged, passed and fixed per level;
- each fix: file and rule;
- anything left open.
Commit messages end with `Co-Authored-By: Claude Sonnet 5.5 <noreply@anthropic.com>`. Push only
`cloud/cloud-r5-tryit-read`. No pull request.
