"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import asyncio
import json
import os
import tempfile
from pathlib import Path

from agent import run_agent

# The tests never call the model: they start harness/fake_claude.py, a scripted stand-in for the Claude Code binary.
here = Path(__file__).resolve()
fake = next(str(p) for p in (Path("/w/harness/fake_claude.py"), *(here.parents[i] / "harness" / "fake_claude.py" for i in range(min(len(here.parents), 8)))) if p.exists())

# The script: the agent reads a file, runs the tests with Bash, then finishes.
steps = [
    {"tool": {"id": "t1", "name": "Read", "input": {"file_path": "a.txt"}, "output": "Read ok"}},
    {"tool": {"id": "t2", "name": "Bash", "input": {"command": "pytest -q"}, "output": "Bash ok"}},
    {"say": "All done."},
    {"result": {"subtype": "success", "result": "All done.", "cost": 0.02, "turns": 3}},
]
project = tempfile.mkdtemp()
script = Path(project, "script.json")
script.write_text(json.dumps({"session_id": "s1", "turns": [steps]}))
os.environ["FAKE_CLAUDE_SCRIPT"] = str(script)
os.environ["FAKE_CLAUDE_RECORD"] = str(Path(project, "record.jsonl"))

try:
    summary = asyncio.run(run_agent("go", project, fake, "readonly")) or {}
    print("status:", summary.get("status"))
    print("tools used:", summary.get("tools"))
    print("turns and cost:", summary.get("turns"), summary.get("cost"))
    print("calls denied:", summary.get("denied"))
except Exception as err:  # a gap that is not written yet may raise: show it instead of a traceback
    print("raised:", type(err).__name__, err)
