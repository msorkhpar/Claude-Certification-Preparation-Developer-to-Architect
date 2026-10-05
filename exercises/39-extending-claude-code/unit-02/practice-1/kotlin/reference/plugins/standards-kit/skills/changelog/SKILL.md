---
name: changelog
description: Draft a changelog entry from the merged pull requests since the last tag. Use when asked for a changelog entry, release notes or a summary of what shipped.
allowed-tools: Read Grep Bash(git log *)
---
Collect the merged pull requests with `git log --merges --oneline <last-tag>..HEAD`.
Group them under Added, Changed and Fixed, one line each, and link the pull request number.
