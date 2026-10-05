#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(27|39|60)-' --examples '^(39|60)-' "$@"   # the one gate (tools/gate.sh) for this range
