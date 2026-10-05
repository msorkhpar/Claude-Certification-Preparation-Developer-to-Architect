---
name: fix-issue
description: Fix a GitHub issue by number, then run the tests and the linter
---
<!-- TODO 7 of 8 (unlocks e4): add two front matter lines: `argument-hint: [issue-number]` and the line that stops Claude from starting
     this command by itself (`disable-model-invocation: true`). The body must also tell Claude to run `make test`. -->
Fix issue $ARGUMENTS:

1. Read the issue with `gh issue view $ARGUMENTS`.
2. Write a failing test that reproduces it, then make it pass.
3. Report the result.
