"""Session state, offline: what continue, resume and fork send to the binary, and when a saved session is worth resuming.

The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
The session ids come from the script (the stand-in does not store sessions); the flags are the ones the real SDK builds. `claude-agent-sdk` 0.2.163,
checked on 2026-10-03 against the "Work with sessions" page of the Claude Code documentation.
"""
import asyncio
import shutil
import json
import os
import tempfile
from pathlib import Path

from claude_agent_sdk import ClaudeAgentOptions, ResultMessage, query
import logging

log = logging.getLogger(__name__)

_found = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)
DAY = 24 * 3600


def decide(saved, current, idle_days):
    """resume, resume with a notice, or start fresh with a summary, from the files a session analysed and the files now."""
    changed = sorted(p for p in saved if p in current and saved[p] != current[p])
    gone = sorted(p for p in saved if p not in current)
    if (len(changed) + len(gone)) / len(saved) > 0.5 or idle_days > 7:
        return "start fresh with a summary", changed
    return ("resume with a notice", changed) if changed or gone else ("resume", changed)


def notice(changed):
    return "\n".join(["Since your earlier analysis:", f"- changed: {', '.join(changed)}",
                      "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged."])


def flags(argv):
    """The session flags of one command line, in the order they appear."""
    out = []
    for i, arg in enumerate(argv):
        if arg == "--resume":
            out.append(f"--resume {argv[i + 1]}")
        elif arg.startswith("--resume="):  # the Python SDK writes the id after an equals sign
            out.append(f"--resume {arg.split('=', 1)[1]}")
        elif arg in ("--fork-session", "--continue"):
            out.append(arg)
    return ", ".join(out) or "none"


async def run(session_id, **options):
    """One single-shot run against the stand-in, scripted to report `session_id`; returns the id the result carries and the session flags sent."""
    folder = tempfile.mkdtemp()
    script, record = Path(folder, "script.json"), Path(folder, "record.jsonl")
    script.write_text(json.dumps({"session_id": session_id, "turns": [[{"say": "ok"}, {"result": {"subtype": "success", "result": "ok", "cost": 0.01, "turns": 1}}]]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    seen = None
    async for message in query(prompt="Continue the review", options=ClaudeAgentOptions(cli_path=FAKE, cwd=folder, setting_sources=[], **options)):
        if isinstance(message, ResultMessage):
            seen = message.session_id
    argv = next(json.loads(line)["argv"] for line in record.read_text().splitlines() if '"argv"' in line)
    return seen, flags(argv)


async def main():
    print("what each control sends to the binary")
    runs = [("new session", "s-auth-1", {}), ("resume s-auth-1", "s-auth-1", {"resume": "s-auth-1"}),
            ("resume s-auth-1 and fork", "s-auth-2", {"resume": "s-auth-1", "fork_session": True}),
            ("resume s-auth-1 again", "s-auth-1", {"resume": "s-auth-1"}), ("continue the latest", "s-auth-1", {"continue_conversation": True})]
    for label, scripted, options in runs:
        seen, sent = await run(scripted, **options)
        print(f"  {label:<26} -> session {seen}, flags: {sent}")
    saved = {"a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4"}
    print("\nwhat to do with a saved session (4 files analysed)")
    cases = [("nothing changed, idle 1 day", saved, 1), ("b.py changed", {**saved, "b.py": "x"}, 1),
             ("3 of 4 files changed", {"a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4"}, 1), ("nothing changed, idle 8 days", saved, 8)]
    for label, current, idle in cases:
        print(f"  {label:<30} -> {decide(saved, current, idle)[0]}")
    print("\nthe notice for the second case")
    for line in notice(decide(saved, cases[1][1], 1)[1]).splitlines():
        print(f"  {line}")


if __name__ == "__main__":
    asyncio.run(main())
