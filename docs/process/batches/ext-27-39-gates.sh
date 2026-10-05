#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(27|39)-' --examples '^39-' "$@"   # the one gate (tools/gate.sh) for this range
