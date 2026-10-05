#!/bin/sh
# Review the current diff without a person at the keyboard. The key comes from the environment, never from this file.
# TODO 8 of 8 (unlocks e5): bound the run. Add the permission-mode flag set to a mode that never prompts, a turn cap of five and a
# spend cap of one dollar, and make the allowed-tools list hold `Read` and read-only git patterns, never a whole tool that changes things.
# Example of one capped flag: the turn-cap flag followed by the number 5.
set -eu
git diff origin/main...HEAD | claude -p "Review this diff for bugs and reply with a short list." \
  --bare \
  --output-format json \
  --allowedTools "Read"
