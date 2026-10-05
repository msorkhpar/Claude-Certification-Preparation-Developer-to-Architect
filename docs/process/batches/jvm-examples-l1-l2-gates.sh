#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^$' --examples '^([0-3][0-9]|4[0-3])-' "$@"   # the one gate (tools/gate.sh) for this range
