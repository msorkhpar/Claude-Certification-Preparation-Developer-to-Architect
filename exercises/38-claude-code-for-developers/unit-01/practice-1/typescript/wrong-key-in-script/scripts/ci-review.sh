#!/bin/sh
# Review the current diff without a person at the keyboard. The key comes from the environment, never from this file.
set -eu
export ANTHROPIC_API_KEY=sk-ant-api03-EXAMPLEKEY123
git diff origin/main...HEAD | claude -p "Review this diff for bugs and reply with a short list." \
  --bare \
  --output-format json \
  --permission-mode dontAsk \
  --max-turns 5 \
  --max-budget-usd 1 \
  --allowedTools "Read,Bash(git diff *),Bash(git log *)"
