"""Hooks in the Agent SDK, offline: a gate before a tool, a normaliser after another, and a command hook run as the process it is.

The SDK starts the Claude Code binary; here the binary is `harness/fake_claude.py`, which replays a script, so no model is called and no network is used.
The refund and order tools are scripted: their output is what the stand-in reports when a call is allowed. `claude-agent-sdk` 0.2.163, checked on
2026-10-03 against the hooks pages of the Claude Code documentation.
"""
import asyncio
import json
import os
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage, ToolResultBlock, ToolUseBlock, UserMessage, query)

HERE = Path(__file__).resolve()
FAKE = str(HERE.parents[3] / "harness" / "fake_claude.py")
GUARD = str(HERE.parents[1] / "guard_hook.py")
log = []


def tier(amount):
    """The decision for a refund amount: small goes through, a middle one needs a person, a large one is refused."""
    if isinstance(amount, bool) or not isinstance(amount, (int, float)) or amount <= 0:
        return "deny", "the amount is missing or invalid"
    if amount <= 200:
        return "allow", "within the automatic limit"
    if amount <= 500:
        return "ask", "needs a person's approval"
    return "deny", "above the limit of 500, escalate to a person"


async def refund_gate(input_data, tool_use_id, context):
    decision, reason = tier(input_data["tool_input"].get("amount"))
    log.append(f"PreToolUse {input_data['tool_name']} amount={input_data['tool_input'].get('amount')} -> {decision}")
    return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": decision, "permissionDecisionReason": reason}}


async def readable_order(input_data, tool_use_id, context):
    order = json.loads(input_data["tool_response"])
    order["created"] = datetime.fromtimestamp(order["created"], timezone.utc).strftime("%Y-%m-%d")
    order["status"] = {0: "pending", 1: "approved", 2: "declined"}.get(order["status"], "unknown")
    log.append(f"PostToolUse {input_data['tool_name']} -> readable output")
    return {"hookSpecificOutput": {"hookEventName": "PostToolUse", "updatedToolOutput": json.dumps(order, separators=(",", ":"))}}


async def person_says_no(tool_name, tool_input, context):
    if tool_name == "get_order":
        return PermissionResultAllow(updated_input=tool_input)
    log.append(f"a person is asked about {tool_name} amount={tool_input.get('amount')} and declines")
    return PermissionResultDeny(message="A person declined this refund")


def options(cwd):
    return ClaudeAgentOptions(
        cli_path=FAKE, cwd=cwd, permission_mode="default", setting_sources=[], max_turns=8,
        can_use_tool=person_says_no,
        hooks={"PreToolUse": [HookMatcher(matcher="process_refund", hooks=[refund_gate])], "PostToolUse": [HookMatcher(matcher="get_order", hooks=[readable_order])]})


def step(id, name, input, output):
    return {"tool": {"id": id, "name": name, "input": input, "output": output}}


SCRIPT = [step("t1", "get_order", {"order": "A-7"}, json.dumps({"order": "A-7", "created": 1700000000, "status": 1, "total_cents": 12950})),
          step("t2", "process_refund", {"order": "A-7", "amount": 50}, "refund R-1 created"),
          step("t3", "process_refund", {"order": "A-7", "amount": 350}, "refund R-2 created"),
          step("t4", "process_refund", {"order": "A-7", "amount": 900}, "refund R-3 created"),
          step("t5", "process_refund", {"order": "A-7"}, "refund R-4 created"),
          {"result": {"subtype": "success", "result": "done", "cost": 0.02, "turns": 6}}]


def run_guard(event):
    """Run the command hook as Claude Code does: JSON on standard input, then read the exit code and standard error."""
    done = subprocess.run([sys.executable, GUARD], input=event, capture_output=True, text=True)
    return done.returncode, done.stderr.strip()


async def main():
    workdir = tempfile.mkdtemp()
    script = Path(workdir, "script.json")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(Path(workdir, "record.jsonl"))
    names = {}
    async for message in query(prompt="Refund order A-7", options=options(workdir)):
        if isinstance(message, AssistantMessage):
            for block in message.content:
                if isinstance(block, ToolUseBlock):
                    names[block.id] = f"{block.name} {json.dumps(block.input, separators=(',', ':'))}"
        elif isinstance(message, UserMessage) and isinstance(message.content, list):
            for block in message.content:
                if isinstance(block, ToolResultBlock):
                    print(f"{names[block.tool_use_id]}\n  the model sees: {block.content}")
        elif isinstance(message, ResultMessage):
            print(f"done: {message.subtype}")
    print("\nwhat ran in this process, in order:")
    for line in log:
        print(" ", line)
    print("\nthe command hook, as a process:")
    for event in ({"tool_name": "Bash", "tool_input": {"command": "git push origin main"}}, {"tool_name": "Bash", "tool_input": {"command": "git status"}}, "{not json"):
        code, reason = run_guard(event if isinstance(event, str) else json.dumps(event))
        print(f"  {event if isinstance(event, str) else event['tool_input']['command']!r}: exit {code}{', ' + reason if reason else ''}")


if __name__ == "__main__":
    asyncio.run(main())
