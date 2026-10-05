#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^0[1-6]-' "$@"   # the one gate (tools/gate.sh) for this range
