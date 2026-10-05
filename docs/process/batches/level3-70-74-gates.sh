#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^7[0-4]-' "$@"   # the one gate (tools/gate.sh) for this range
