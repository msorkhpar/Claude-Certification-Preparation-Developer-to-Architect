#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(3[6-9]|4[01])-' "$@"   # the one gate (tools/gate.sh) for this range
