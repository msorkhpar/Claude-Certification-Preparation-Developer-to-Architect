#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(1[89]|2[0-3])-' "$@"   # the one gate (tools/gate.sh) for this range
