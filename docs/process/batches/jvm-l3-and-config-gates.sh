#!/bin/sh
exec sh "$(dirname "$0")/../../../tools/gate.sh" --modules "${L3_PRACTICES:-^(38|39|40|55|56|57|58|60)-}" --examples '.*' "$@"   # the one gate (tools/gate.sh) for this range
