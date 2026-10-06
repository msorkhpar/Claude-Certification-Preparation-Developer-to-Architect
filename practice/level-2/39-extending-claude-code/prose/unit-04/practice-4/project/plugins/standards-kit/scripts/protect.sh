#!/bin/sh
# Refuse edits of protected files: reads the hook event on standard input and exits 2 with a reason on standard error.
input=$(cat)
case "$input" in
  *.env*|*package-lock.json*) echo "Blocked: this file is protected" >&2; exit 2 ;;
esac
exit 0
