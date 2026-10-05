#!/bin/sh
# Reads the session JSON on standard input and prints one line: the model, the share of the context window used, and the branch.
input=$(cat)
model=$(echo "$input" | jq -r '.model.display_name')
used=$(echo "$input" | jq -r '.context_window.used_percentage // 0' | cut -d. -f1)
dir=$(echo "$input" | jq -r '.workspace.current_dir')
branch=$(git -C "$dir" branch --show-current 2>/dev/null)
echo "[$model] ${used}% context ${branch:+| $branch}"
