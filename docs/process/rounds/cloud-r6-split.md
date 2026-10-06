# Cloud round cloud-r6-split: one try-it fix and the learner/process split

For a cloud session working alone on this repository. Read `CLAUDE.md` first. Budget cap: about
15 USD. Push only the two branches named below. No Docker is needed.

## Task 1: module 35's Python try-it (on branch `cloud/cloud-r6-split`)

The Python try-it of `exercises/35-*/unit-*/practice-*/python/tryit/try_it.py` fails with
"Permission denied" on the harness's stand-in Claude program (`fake_claude.py`). Only the tests
make the stand-in executable, so the try-it file must not depend on that.
- Make the try-it start the stand-in the way the tests do: mark it executable first, or call it
  through the Python interpreter.
- Do the same check for the TypeScript try-it of that practice.
- Prove it: `python3 tools/make_tryit.py run <practice dir> python reference` exits 0 and shows
  a DEBUG line; the same with `starter` runs without a crash.

## Task 2: the split, on two branches

`docs/process/PROCESS-SPLIT.md` lists each tracked path as process material or learner material.

1. **`process`:** a branch from the tip of `cloud/cloud-r6-split` (after task 1), with everything.
   It is the keeper of all process material; nothing is purged.
2. **`cloud/learner-main`:** a branch from the same tip, with every path the split marks as process
   material removed, in one commit.
   - Check that nothing left behind needs a removed path: run the course checks (`check_quiz`,
     `check_coverage`, `check_revision`, `check_logger`, `check_personal_data --modules '.*'` and
     `make_tryit.py check`) and grep for links into removed paths.
   - If a learner file links to process material, change the link or move the paragraph. Don't
     bring the process file back.
   - Learner `CLAUDE.md` handling: if `CLAUDE.md` is process material, the learner branch gets
     none.
3. Don't touch `main`; the maintainer swaps it after review.

## Hand back

Commit `HANDBACK-r6.md` at the root of `cloud/cloud-r6-split` only, at most 30 lines:
- the task 1 proof lines;
- the counts of paths removed on the learner branch;
- every link you changed;
- the check results on the learner branch;
- anything that needs the maintainer.

Commit messages end with a `Co-Authored-By:` line naming the model that did the work. Push
`cloud/cloud-r6-split`, `process` and `cloud/learner-main`. No pull request.
