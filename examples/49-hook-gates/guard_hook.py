#!/usr/bin/env python3
"""A Claude Code command hook: reads the event as JSON on standard input, exits 2 with a reason on standard error to block a Bash call, 0 to let it go on."""
import json
import re
import sys

try:
    event = json.load(sys.stdin)
    command = str((event.get("tool_input") or {}).get("command", ""))
except (ValueError, AttributeError):
    print("guard: input is not a JSON object, blocking to be safe", file=sys.stderr)
    sys.exit(2)
if event.get("tool_name") == "Bash" and re.search(r"\bgit\s+push\b", command):
    print("guard: nothing is pushed from an agent session", file=sys.stderr)
    sys.exit(2)
sys.exit(0)
