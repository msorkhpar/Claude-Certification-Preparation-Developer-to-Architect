#!/bin/sh
# usage: tools/test_gate.sh [--modules RX]   runs the gate's planted-defect tests (tools/test_gate.py); see its docstring
cd "$(dirname "$0")/.." || exit 2
exec python3 tools/test_gate.py "$@"
