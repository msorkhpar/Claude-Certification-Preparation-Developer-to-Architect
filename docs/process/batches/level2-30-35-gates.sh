#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^3[0-5]-' "$@"   # the one gate (tools/gate.sh) for this range
