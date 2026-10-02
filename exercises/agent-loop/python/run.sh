#!/bin/sh
# usage: run.sh <starter|reference|wrong-...>   (run inside the runner container, cwd = this folder)
SOLUTION_DIR="$(pwd)/$1" exec python3 -m pytest tests -q -p no:cacheprovider
