"""The Agent SDK driven offline: a custom tool, a hook and a permission callback, against a scripted stand-in for the Claude Code binary.

The Agent SDK is a library around the Claude Code binary: it starts the binary, translates the options into flags, and answers the
binary's control requests (hooks, permission questions, calls to your in-process tools). Here the binary is `harness/fake_claude.py`,
which speaks the same stream-json protocol and replays a script, so no model is called and no network is used. `claude-agent-sdk`
0.2.163, checked on 2026-10-03 against the Agent SDK pages of the Claude Code documentation.
"""
import asyncio
import json
import os
import tempfile
from collections import Counter
from pathlib import Path

from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage, SystemMessage,
                              TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, create_sdk_mcp_server, query, tool)

FAKE = str(Path(__file__).resolve().parents[3] / "harness" / "fake_claude.py")


@tool("add", "Add two whole numbers", {"a": int, "b": int})
async def add(args):
    return {"content": [{"type": "text", "text": f"Sum: {args['a'] + args['b']}"}]}


@tool("divide", "Divide one whole number by another", {"a": int, "b": int})
async def divide(args):
    if args["b"] == 0:
        return {"content": [{"type": "text", "text": "Cannot divide by zero"}], "is_error": True}  # the message Claude reads, written by you
    return {"content": [{"type": "text", "text": f"Quotient: {args['a'] // args['b']}"}]}


async def no_push(input_data, tool_use_id, context):
    """A PreToolUse hook: it runs in your process before the tool and can deny the call."""
    if "git push" in input_data["tool_input"].get("command", ""):
        return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "Nothing is pushed from an agent"}}
    return {}


async def ask_me(tool_name, tool_input, context):
    """The permission callback: it is asked about a call that no rule has decided. The tools of your own server are allowed here, not by
    allowed_tools: an allowed_tools entry approves a tool before this callback is consulted, and the SDK warns when it would shadow it."""
    if tool_name.startswith("mcp__calc__") or (tool_name == "Bash" and tool_input["command"].split()[0] == "ls"):
        return PermissionResultAllow(updated_input=tool_input)
    return PermissionResultDeny(message=f"{tool_name} is not allowed here")


SCRIPT = [{"say": "I will add, then divide."},
          {"tool": {"id": "t1", "name": "mcp__calc__add", "input": {"a": 2, "b": 3}}},
          {"tool": {"id": "t2", "name": "mcp__calc__divide", "input": {"a": 1, "b": 0}}},
          {"tool": {"id": "t3", "name": "Bash", "input": {"command": "git push origin main"}, "output": "pushed"}},
          {"tool": {"id": "t4", "name": "Bash", "input": {"command": "ls"}, "output": "README.md"}},
          {"say": "Two sums, one refused push."},
          {"result": {"subtype": "success", "result": "Two sums, one refused push.", "cost": 0.02, "turns": 5}}]


def options(workdir):
    return ClaudeAgentOptions(cli_path=FAKE, cwd=workdir, tools=["Read", "Bash"], disallowed_tools=["Bash(rm *)"], max_turns=6,
                              permission_mode="default", setting_sources=[], mcp_servers={"calc": create_sdk_mcp_server("calc", tools=[add, divide])}, can_use_tool=ask_me,
                              hooks={"PreToolUse": [HookMatcher(matcher="Bash", hooks=[no_push])]})


def show(message):
    if isinstance(message, SystemMessage) and message.subtype == "init":
        print("init: the binary reports tools", message.data["tools"])
    elif isinstance(message, AssistantMessage):
        for block in message.content:
            print("claude says:" if isinstance(block, TextBlock) else "claude calls:", block.text if isinstance(block, TextBlock) else f"{block.name} {json.dumps(block.input, separators=(',', ':'))}")
    elif isinstance(message, UserMessage):
        for block in message.content:
            if isinstance(block, ToolResultBlock):
                print("  result:", "ERROR" if block.is_error else "ok   ", block.content)
    elif isinstance(message, ResultMessage):
        print(f"done: {message.subtype}, {message.num_turns} turns, cost ${message.total_cost_usd:.2f}")


async def main():
    workdir = tempfile.mkdtemp()
    script, record = Path(workdir, "script.json"), Path(workdir, "record.jsonl")
    script.write_text(json.dumps({"session_id": "demo", "turns": [SCRIPT]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    async for message in query(prompt="Add 2 and 3, divide 1 by 0, push, then list the files.", options=options(workdir)):
        show(message)
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    argv = next(line["argv"] for line in lines if "argv" in line)
    print()
    for flag in ("--tools", "--disallowedTools", "--max-turns", "--permission-mode"):
        print(f"flag {flag} {argv[argv.index(flag) + 1]}")
    asked = Counter(line["ask"]["subtype"] for line in lines if "ask" in line)
    print("what the binary asked your process:", dict(sorted(asked.items())))


if __name__ == "__main__":
    asyncio.run(main())
