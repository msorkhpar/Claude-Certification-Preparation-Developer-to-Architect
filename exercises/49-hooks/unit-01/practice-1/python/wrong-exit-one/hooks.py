"""Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md."""
import json
import math
import re
from datetime import datetime, timezone

from claude_agent_sdk import ClaudeAgentOptions, HookMatcher

AUTO_LIMIT = 200
ASK_LIMIT = 500
STATUS = {0: "pending", 1: "approved", 2: "declined"}
BLOCKED_COMMANDS = ((r"\bgit\s+push\b", "Nothing is pushed from an agent session"), (r"\brm\s+-rf\b", "Recursive deletes are not allowed"))


def _answer(event, **fields):
    return {"hookSpecificOutput": {"hookEventName": event, **fields}}


async def pre_refund(input_data, tool_use_id, context):
    """PreToolUse for process_refund: allow small refunds, ask a person for middle ones, deny large ones, and fail closed on a bad amount."""
    if input_data.get("tool_name") != "process_refund":
        return {}
    amount = (input_data.get("tool_input") or {}).get("amount")
    if isinstance(amount, bool) or not isinstance(amount, (int, float)) or not math.isfinite(amount) or amount <= 0:
        return _answer("PreToolUse", permissionDecision="deny", permissionDecisionReason="The refund amount is missing or invalid")
    if amount <= AUTO_LIMIT:
        return _answer("PreToolUse", permissionDecision="allow", permissionDecisionReason=f"{amount} is within the automatic limit of {AUTO_LIMIT}")
    if amount <= ASK_LIMIT:
        return _answer("PreToolUse", permissionDecision="ask", permissionDecisionReason=f"{amount} needs a person's approval")
    return _answer("PreToolUse", permissionDecision="deny", permissionDecisionReason=f"{amount} is above the limit of {ASK_LIMIT}; escalate the case to a person")


def _date(value):
    seconds = value / 1000 if value > 1e11 else value
    return datetime.fromtimestamp(seconds, timezone.utc).strftime("%Y-%m-%d")


async def post_normalise(input_data, tool_use_id, context):
    """PostToolUse: give the model readable values in place of epoch seconds, numeric status codes and cents."""
    raw = input_data.get("tool_response")
    try:
        data = json.loads(raw) if isinstance(raw, str) else raw
    except ValueError:
        return {}
    if not isinstance(data, dict):
        return {}
    out = dict(data)
    created = out.get("created")
    if isinstance(created, (int, float)) and not isinstance(created, bool):
        out["created"] = _date(created)
    status = out.get("status")
    if isinstance(status, int) and not isinstance(status, bool):
        out["status"] = STATUS.get(status, "unknown")
    cents = out.pop("amount_cents", None)
    if isinstance(cents, int) and not isinstance(cents, bool):
        out["amount"] = f"{cents // 100}.{cents % 100:02d}"
    elif cents is not None:
        out["amount_cents"] = cents
    if out == data:
        return {}
    return _answer("PostToolUse", updatedToolOutput=json.dumps(out))


def build_options(cwd, cli_path=None):
    """Options that register both hooks; a matcher is a tool name pattern, never a path."""
    return ClaudeAgentOptions(
        cwd=cwd, cli_path=cli_path, allowed_tools=["get_order", "process_refund"], permission_mode="default", setting_sources=[], max_turns=6,
        hooks={"PreToolUse": [HookMatcher(matcher="process_refund", hooks=[pre_refund], timeout=5)],
               "PostToolUse": [HookMatcher(matcher="get_order|get_refund", hooks=[post_normalise], timeout=5)]})


def command_hook(stdin_text):
    """What a Claude Code command hook does with its stdin: exit 2 blocks (the reason goes to stderr), 0 lets the call go on, and bad input blocks."""
    try:
        data = json.loads(stdin_text)
    except ValueError:
        data = None
    if not isinstance(data, dict):
        return {"exit": 2, "stderr": "The hook input is not valid JSON, so the call is blocked to be safe"}
    if data.get("tool_name") != "Bash":
        return {"exit": 0, "stderr": ""}
    command = str((data.get("tool_input") or {}).get("command", ""))
    for pattern, reason in BLOCKED_COMMANDS:
        if re.search(pattern, command):
            return {"exit": 1, "stderr": reason}
    return {"exit": 0, "stderr": ""}


def settings_hooks(script="python3 .claude/hooks/guard.py", timeout=10):
    """The hooks block of .claude/settings.json that runs the command hook before every Bash call."""
    return {"hooks": {"PreToolUse": [{"matcher": "Bash", "hooks": [{"type": "command", "command": script, "timeout": timeout}]}]}}
