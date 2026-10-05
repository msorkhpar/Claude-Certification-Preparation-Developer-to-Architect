#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(5[7-9]|6[0-2])-' "$@"   # the one gate (tools/gate.sh) for this range
