---
name: fix-issue
description: Fix a GitHub issue by number, then run the tests and the linter
argument-hint: [issue-number]
disable-model-invocation: true
---
Fix issue $ARGUMENTS:

1. Read the issue with `gh issue view $ARGUMENTS`.
2. Write a failing test that reproduces it, then make it pass.
3. Run `make test` and `make lint`, and report their output.
