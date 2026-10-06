---
name: review-pr
description: Review a pull request for correctness and risk and report the findings. Use when the user asks for a review of a pull request or gives a pull request number.
argument-hint: "[pr-number]"
context: fork
agent: general-purpose
allowed-tools: Bash(gh pr view *) Bash(gh pr diff *)
disallowed-tools: Edit Write
---
Review pull request $0.

1. Run `gh pr view $0` and read the description and the list of changed files.
2. Run `gh pr diff $0` and read the diff file by file.
3. List only the findings that affect correctness or security, each with its file and line.
4. End with one line: `verdict: approve` or `verdict: changes requested`.
