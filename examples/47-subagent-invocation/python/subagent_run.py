"""Invoking a subagent with the Agent SDK, offline: the definitions sent to the binary, the spawn in the stream and the messages that ran inside it.

The Agent SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is
used. The stand-in puts the messages of the subagent after the call that started it, which the real binary may order differently. `claude-agent-sdk`
0.2.163, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
"""
import logging
import asyncio
import json
import os
import shutil
import tempfile
from pathlib import Path

from claude_agent_sdk import AgentDefinition, AssistantMessage, ClaudeAgentOptions, ResultMessage, TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, query

log = logging.getLogger(__name__)

_found = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)

AGENTS = {
    "reviewer": AgentDefinition(description="Reviews one module for security problems. Use for any review request.", prompt="You review code and report findings only.",
                                tools=["Read", "Grep"], model="sonnet"),
    "finder": AgentDefinition(description="Finds the files that touch a feature.", prompt="You list files.", tools=["Read", "Grep", "Glob"]),
}

BRIEF = "Task: Review auth.py for injection risks\nFiles:\n- src/auth.py\nKnown:\n- the login query is built with string formatting\nReturn: findings, one line each"

SCRIPT = [{"say": "I will delegate the review."},
          {"tool": {"id": "t1", "name": "Agent", "input": {"subagent_type": "reviewer", "description": "Review auth.py", "prompt": BRIEF}, "output": "1 finding: string-built query in login()."}},
          {"tool": {"id": "t2", "name": "Read", "input": {"file_path": "src/auth.py"}, "output": "def login(): ...", "parent": "t1"}},
          {"tool": {"id": "t3", "name": "Grep", "input": {"pattern": "execute("}, "output": "src/auth.py:12", "parent": "t1"}},
          {"say": "One finding, from the reviewer."},
          {"result": {"subtype": "success", "result": "One finding, from the reviewer.", "cost": 0.04, "turns": 3}}]


def describe(message):
    """One line per interesting message: a spawn, a message from inside a subagent, a report, the result."""
    lines = []
    inside = getattr(message, "parent_tool_use_id", None)
    if isinstance(message, AssistantMessage):
        for block in message.content:
            if isinstance(block, ToolUseBlock) and block.name in ("Agent", "Task"):
                lines.append(f"spawn: {block.name} -> {block.input['subagent_type']}, brief of {len(block.input['prompt'].splitlines())} lines")
            elif isinstance(block, ToolUseBlock):
                lines.append(f"  inside {inside}: {block.name} {json.dumps(block.input, separators=(',', ':'))}" if inside else f"coordinator calls {block.name}")
            elif isinstance(block, TextBlock):
                lines.append(f"coordinator says: {block.text}")
    elif isinstance(message, UserMessage):
        for block in message.content:
            if isinstance(block, ToolResultBlock) and not inside:
                lines.append(f"report to the coordinator: {block.content}")
    elif isinstance(message, ResultMessage):
        lines.append(f"done: {message.subtype}, cost ${message.total_cost_usd:.2f}")
    return lines


async def main():
    workdir = tempfile.mkdtemp()
    script, record = Path(workdir, "script.json"), Path(workdir, "record.jsonl")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    options = ClaudeAgentOptions(cli_path=FAKE, cwd=workdir, agents=AGENTS, allowed_tools=["Agent", "Read", "Grep", "Glob"], max_budget_usd=0.5, max_turns=10,
                                 permission_mode="default", setting_sources=[], env={"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1"})
    async for message in query(prompt="Review auth.py", options=options):
        for line in describe(message):
            print(line)
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    sent = next(line["agents"] for line in lines if "agents" in line)
    print()
    for name in sorted(sent):
        print(f"agent sent to the binary: {name} tools={sent[name].get('tools')} model={sent[name].get('model', 'not set')}")
    argv = next(line["argv"] for line in lines if "argv" in line)
    for flag in ("--allowedTools", "--max-budget-usd", "--max-turns"):
        print(f"flag {flag} {argv[argv.index(flag) + 1]}")


if __name__ == "__main__":
    asyncio.run(main())
