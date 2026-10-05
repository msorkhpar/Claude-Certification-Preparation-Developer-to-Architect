#!/bin/sh
# Reads the session JSON on standard input and prints one line: the model, the share of the context window used, and the branch.
# Gap: also read `.context_window.used_percentage` (a number that may be missing; show 0 then and drop the decimals) and the branch of
# `.workspace.current_dir`, and print them after the model. Example line: [Sonnet] 42% context | main
input=$(cat)
model=$(echo "$input" | jq -r '.model.display_name')
echo "[$model]"
