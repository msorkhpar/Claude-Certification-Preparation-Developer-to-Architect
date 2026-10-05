#!/bin/sh
# Review the current diff without a person at the keyboard. The key comes from the environment, never from this file.
# TODO 8 of 8 (unlocks e5): bound the run. Add `--permission-mode dontAsk`, `--max-turns 5` and `--max-budget-usd 1`, and make
# --allowedTools list `Read` and read-only git patterns such as `Bash(git diff *)`, never a whole tool that changes things.
# Example: claude -p "..." --bare --output-format json --max-turns 5 --allowedTools "Read"
set -eu
git diff origin/main...HEAD | claude -p "Review this diff for bugs and reply with a short list." \
  --bare \
  --output-format json \
  --allowedTools "Read"
