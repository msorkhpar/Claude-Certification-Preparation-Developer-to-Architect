#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules '^(4[5-9]|50)-' "$@"   # the one gate (tools/gate.sh) for this range
