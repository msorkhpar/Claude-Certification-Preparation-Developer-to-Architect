#!/bin/sh
# usage: run.sh <starter|reference|wrong-...>   (inside the runner container, cwd = this folder)
SOLUTION_DIR="$(pwd)/$1" exec node --test tests/mcpSetup.test.ts
