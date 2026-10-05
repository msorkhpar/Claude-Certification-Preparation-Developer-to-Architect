---
name: gate-and-merge
description: The register's verify-and-merge procedure for an office hand-back (read the commits, validate the gate report, one plant, merge --no-ff, post-merge checks, remove the worktree). Invoke by hand with the round id, branch, worktree and module regex.
disable-model-invocation: true
argument-hint: <round-id> <branch> <worktree-dir> <modules-regex>
allowed-tools: Read, Grep, Glob, Edit, Bash(git *), Bash(python3 tools/*), Bash(sh tools/*), Bash(tools/*), Bash(../.heavy-slot/run-heavy.sh *), Bash(sha256sum *), Bash(pgrep *), Bash(df *)
---

# Gate and merge

Verify an office's hand-back by its effect, then merge. A hand-back is a claim: read the commits, read the report, plant a defect of your own, run the gate. Arguments: `$ARGUMENTS` = `<round-id> <branch> <worktree-dir> <modules-regex>`. Below, `ID`, `BR`, `WT` (the office's worktree, beside the course repository) and `RE` stand for them; `MAIN` is the main checkout of the course repository. Placeholders only; no personal data, no quotes from anyone, no push, no board edit by an office.

Every step stops the procedure at the first failure: print the exact result, do not continue, hand the failure back to the office (or ask for the real report if the hand-back is a stub). Run each numbered step as its own call, and never chain anything after the merge.

## 1. Read the commits and the diff scope

```
git -C WT status --short                      # must print nothing
git -C WT log --oneline MAIN_BASE..BR         # MAIN_BASE = the round's frozen input commit
git -C WT diff --stat MAIN_BASE BR | tail -5
git -C WT diff --name-only MAIN_BASE BR | grep -Ev "^(course|exercises|examples)/(RE_DIRS)|^tools/(make_plants|make_cases)\.py$|^docs/process/batches/|^tools/.*quiz" ; echo "outside-range-files-above"
```

`RE_DIRS` is the module folders the round covers, from `docs/process/rounds/ID.json`. Every file listed after the filter is outside the range: justify each one or send the batch back (a course-wide `tools/balance_keys.py` run touches dozens of merged files). Read the commit messages: each ends with the attribution line, none names a source, none holds personal data or conversation. Scan the whole diff once:

```
git -C WT diff MAIN_BASE BR | grep -nEi "@[a-z0-9-]+\.(com|org|net|io)|/home/[a-z]|sk-ant-|ANTHROPIC_API_KEY *= *[A-Za-z0-9]" | grep -v "example\.\(com\|invalid\)"; echo "scan-done"
```

Only `scan-done` may print. If `tools/check_personal_data.py` or the gate's personal-data scan exists, it covers this too.

## 2. Frozen base and report

```
python3 tools/round_check.py spec docs/process/rounds/ID.json
python3 tools/round_check.py handback docs/process/rounds/ID.json --repo course --branch BR --head HEAD_SHA --dir WT
( cd WT && python3 tools/validate_handback.py .survey-out/gate-report.json; echo rc=$? )
```

`validate_handback.py` reads its own repository, hence the `cd WT`. The scripts print `REFUSED:` lines or an accepted summary; read the per-language executed and expected counts yourself and compare them with the round's `acceptance` numbers. If `gate-report.json` is absent (an older office), fall back: run the office's gate from `WT` as its own call and read its real exit status (`sh docs/process/batches/<batch>-gates.sh; echo rc=$?`, or `tools/gate.sh --modules RE; echo rc=$?`). A hand-back whose gate was `--report-only`, whose report commit is not in the branch history, or with a language at `executed == 0` is refused.

## 3. One plant of the register's own, with the gate's filters

Pick a reference solution the office did not mention (prefer a Java or Kotlin one, and a module whose practice has the most edge cases). Plant with an exact replacement whose text differs in length from the original, never sed:

```
python3 - <<'PY'
import pathlib, sys
p = pathlib.Path("WT/exercises/<module>/unit-<n>/practice-<k>/<language>/<reference file>")
s = p.read_text()
old, new = "<exact text that occurs once>", "<wrong text of a different length>"
assert s.count(old) == 1, s.count(old)
p.write_text(s.replace(old, new))
print("planted", p)
PY
git -C WT diff --stat          # confirm the plant changed the file
```

Run the gate with the same filters the office used (its round `gates` entry; for the one gate: `--modules RE`, narrowed to the single practice by the regex when the round allows) from `WT`, through the slot, as its own call: `cd WT && tools/gate.sh --modules RE; echo rc=$?`. The expected result is rc not 0 and a grade line that names the planted practice and language failing **on an assertion** (`AssertionFailedError`, `AssertionError`, `ComparisonFailure`), on a case the tests map to that behaviour. Read the failure output, not only the exit code. A plant that passes, or fails on a crash or a compile error, means the tests do not guard that behaviour: send the batch back. Restore and prove the restore:

```
git -C WT checkout -- <planted file>
git -C WT status --short       # must print nothing
```

A Python plant of the same length can leave a stale `__pycache__`: delete the touched module's `__pycache__` after restoring. Then re-run the gate (or `tools/gate.sh --modules RE --report-only` when the heavy outputs are fresh) and read `rc=0`.

## 4. Merge, nothing after it

Write the message to a file (never `-F -`, never inline quotes), then merge in a call by itself:

```
printf '%s\n\n%s\n' "Merge ID: <modules>, <one line of what landed>" "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" > MAIN/.register-merge-msg
git -C MAIN merge --no-ff -F MAIN/.register-merge-msg BR; echo rc=$?
```

**STOP here.** Read `rc`. On non-zero, or any `CONFLICT` line, run `git -C MAIN status --short | grep -E "^(UU|AA|DD|AU|UA)"` and resolve the shared-tool hunks (`tools/make_plants.py`, `tools/make_cases.py`: keep both sections, in module order) in a separate call; never commit a merge with conflict markers in it (`git -C MAIN grep -n "^<<<<<<<\|^>>>>>>>" -- tools course exercises` must print nothing). Do not remove the worktree, edit the board or run another merge until the merge is clean and committed. Delete the message file in the next call (`rm MAIN/.register-merge-msg`), after `git -C MAIN status --short` shows a clean tree.

## 5. Post-merge checks, on MAIN

Each as its own call; read the result:

```
python3 tools/check_quiz.py; echo rc=$?
python3 tools/check_coverage.py; echo rc=$?
python3 tools/make_cases.py --modules '.*'; echo rc=$?
git status --short -- exercises tools          # idempotence: nothing may have changed
python3 tools/make_plants.py --modules 'RE' ; echo rc=$?   # wrong-* folders are git-ignored; status stays clean
git status --short                             # must print nothing
git diff --stat HEAD~1 HEAD | tail -1          # the merge brought in the range and nothing else
```

Also: `python3 tools/build_quiz_json.py` leaves `exercises/*/tests/quiz.json` byte-identical (`sha256sum` before and after, printed side by side). A cross-batch quiz duplicate search (same fact asked in another batch) is the independent reader's job, started by you after several parallel batches merge. A failure here is a defect of the merge: fix it on `MAIN` with a small commit, or revert the merge commit (`git revert -m 1 HEAD`) and send the batch back.

## 6. Remove the merged worktree, keep the branch

```
git -C MAIN worktree remove --force WT; echo rc=$?
df -h /home | tail -1                           # a batch worktree holds about 2 GB of caches
git -C MAIN worktree list
```

Remove scratch Docker images and containers the proofs left (with `--context desktop-linux` and `STUDYFORGE_NAMESPACE=studyforge-local`). Never remove a worktree whose office has not handed back, and never kill a process you did not start.

## 7. Record

Update the register's memory (one entry, short): round id, merged commit, modules, the plant used and its failure line, per-language counts, anything the office listed for the register (new JVM libraries, held units). Update the board in the register's own commit (`docs/process/BOARD.md`, M4 to M6 lines and one log line). Next: the round's follow-ups wait for the next round; a merge that landed after another round's freeze starts a new frozen input for it.
