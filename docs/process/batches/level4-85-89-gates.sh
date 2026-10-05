#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^8[5-9]-' "$@"   # the one gate (tools/gate.sh) for this range
