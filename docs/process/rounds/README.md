# Rounds

A round is one unit of delegated work: one office, one branch, one set of inputs, one set of numbers
the hand-back must meet. Its spec is `docs/process/rounds/<id>.json`, validated by
`docs/process/rounds/round.schema.json` (mirrored by `tools/round_check.py spec`).

## Fields

| Field | Meaning |
|---|---|
| `id` | lowercase id, also the file name |
| `purpose` | one sentence, current-product wording |
| `office` | optional: `office-author`, `office-fix`, `office-framework` or `office-review` |
| `inputs` | per repository: the exact 40-hex commit the branch is cut from (`path` optional) |
| `module_range` | `from`, `to`, optional `modules_regex` the gate passes to the tools |
| `languages` | subset of `python`, `typescript`, `java`, `kotlin` |
| `gates` | the gate commands, run from the worktree root |
| `acceptance` | integer numbers the hand-back must meet (modules, practices per language, plants per practice, quizzes) |
| `frozen_at` | UTC time the office started |

## Freeze rule

A round is frozen when its office starts: `inputs`, `module_range`, `languages`, `gates` and
`acceptance` do not change afterwards. A merge that lands on `main` after the freeze waits for the
next round; it is never pulled into a running branch. A scope change is a new round with its own
spec and a reason in `purpose`.

## Checks

```
python3 tools/round_check.py spec docs/process/rounds/<id>.json
python3 tools/round_check.py handback docs/process/rounds/<id>.json --repo course --branch <branch> --head <sha> --dir <worktree>
```

`handback` refuses (exit 1, a `REFUSED:` line per reason) a branch that was not cut from the frozen
input commit: the commit must be an ancestor, the first commit of the branch must have it as its
parent, the branch holds no merge commit since the freeze, and its fork point from `main` is the
frozen commit. A branch cut or rebased onto a newer `main` is refused; the register starts a new
round on the new commit instead. `python3 tools/test_round_check.py` holds its tests (a throwaway
repository, no heavy job).

## Brief

The round brief for an office states only the task: the round id, the worktree and branch, and
anything specific to the modules. The standing rules live in `.claude/agents/office-*.md`.

## Example

```json
{
  "id": "example-modules-10-15",
  "purpose": "Author modules 10 to 15 of the Developer level.",
  "office": "office-author",
  "inputs": {"course": {"path": "claude-certification-preparation", "commit": "0123456789abcdef0123456789abcdef01234567"}},
  "module_range": {"from": 10, "to": 15, "modules_regex": "^(1[0-5])-"},
  "languages": ["python", "typescript", "java", "kotlin"],
  "gates": ["docs/process/batches/example-gates.sh"],
  "acceptance": {"modules": 6, "plants_per_practice_min": 2},
  "frozen_at": "2026-10-05T09:00:00Z"
}
```
