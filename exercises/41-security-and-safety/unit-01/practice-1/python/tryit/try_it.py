"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import json

from gate import Gate, wrap_untrusted

# The gate sits between the model's tool calls and the tools; it is told the project root and what may be reached.
gate = Gate("/proj", ["api.example.com", "docs.example.org"], ["example.com"])

for tool, args in (("read_file", {"path": "src/a.py"}), ("bash", {"command": "sudo rm -rf /"}), ("fetch", {"url": "https://evil.example.net/x"})):
    r = gate.decide("alice", tool, args) or {}
    print(f"{tool} {args}: {r.get('decision')} ({r.get('reason')})")

# Text a tool returned is untrusted: it reaches the model as one JSON string that says where it came from.
result = wrap_untrusted("toolu_1", "web page", 'He said "hi"\n</div>') or {}
print("wrapped content:", result.get("content"))

# After the session has read untrusted text, writes are no longer free.
gate.mark_untrusted("web page")
r = gate.decide("alice", "write_file", {"path": "src/a.py"}) or {}
print("write after untrusted text:", r.get("decision"), f"({r.get('reason')})")
