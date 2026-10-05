"""Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md."""
import json
import math
import re
from datetime import datetime, timezone

from claude_agent_sdk import ClaudeAgentOptions, HookMatcher
import logging

log = logging.getLogger(__name__)

AUTO_LIMIT = 200
ASK_LIMIT = 500
STATUS = {0: "pending", 1: "approved", 2: "declined"}
BLOCKED_COMMANDS = ((r"\bgit\s+push\b", "Nothing is pushed from an agent session"), (r"\brm\s+-rf\b", "Recursive deletes are not allowed"))


def _answer(event, **fields):
    return {"hookSpecificOutput": {"hookEventName": event, **fields}}


async def pre_refund(input_data, tool_use_id, context):
    """PreToolUse for process_refund: allow small refunds, ask a person for middle ones, deny large ones, and fail closed on a bad amount."""
    log.debug("pre_refund input: %r", input_data)
    # TODO 2 of 10 (finish this to pass e2): the tool filter. When the tool name is not process_refund, return an empty
    #   answer so that the call goes on untouched. Example: a get_order call with an amount of 9999 -> {}.
    amount = (input_data.get("tool_input") or {}).get("amount")
    # TODO 3 of 10 (finish this to pass e2): the fail-closed check. When the amount is missing, a boolean, not a number,
    #   not finite or not above zero, return a deny answer with the reason "The refund amount is missing or invalid".
    #   Example: amount "12" or True or 0 -> deny.
    if not isinstance(amount, (int, float)):
        amount = 0
    if amount <= AUTO_LIMIT:
        return _answer("PreToolUse", permissionDecision="allow", permissionDecisionReason=f"{amount} is within the automatic limit of {AUTO_LIMIT}")
    # TODO 1 of 10 (finish this to pass m1, e1): the two upper tiers of the refund gate (the allow tier above is
    #   written, to show the shape of the answer). `amount` is above AUTO_LIMIT here. Return the hook answer with
    #   permissionDecision ask up to ASK_LIMIT, otherwise deny, each with a permissionDecisionReason that names the
    #   amount. Example: 200 -> allow, 200.01 -> ask, 500 -> ask, 501 -> deny.
    return {}


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
    # TODO 4 of 10 (finish this to pass e3): the date and the status word. When `created` is a number (not a boolean),
    #   replace it with the text from the date helper; when `status` is an integer (not a boolean), replace it with its
    #   word from STATUS, or "unknown". Example: created 1700000000 -> "2023-11-14", status 1 -> "approved", status 9 ->
    #   "unknown".
    cents = out.pop("amount_cents", None)
    # TODO 5 of 10 (finish this to pass e3): the amount. When `amount_cents` is an integer (not a boolean), remove it
    #   and add `amount` as text with two decimals; leave a value that is not an integer untouched. Example: 12950 ->
    #   amount "129.50", and 5 -> "0.05".
    # TODO 6 of 10 (finish this to pass e4): the idempotence rule. When nothing changed, return an empty answer instead
    #   of a replacement. Example: {"status": "approved"} -> {} (and output that is not JSON is already left alone above).
    return _answer("PostToolUse", updatedToolOutput=json.dumps(out))


def build_options(cwd, cli_path=None):
    """Options that register both hooks; a matcher is a tool name pattern, never a path."""
    return ClaudeAgentOptions(
        cwd=cwd, cli_path=cli_path, allowed_tools=["get_order", "process_refund"], permission_mode="default", setting_sources=[], max_turns=6,
        # TODO 7 of 10 (finish this to pass e5, e8): the hook registration. Fill the `hooks` map of the options: a
        #   HookMatcher (Python) or {matcher, hooks, timeout} object (TypeScript) for PreToolUse with the matcher
        #   process_refund and the refund gate, and one for PostToolUse with the matcher get_order|get_refund and the
        #   normaliser, each with a timeout of 5.
        hooks={"PreToolUse": [HookMatcher(hooks=[pre_refund])], "PostToolUse": [HookMatcher(hooks=[post_normalise])]})


def command_hook(stdin_text):
    """What a Claude Code command hook does with its stdin: exit 2 blocks (the reason goes to stderr), 0 lets the call go on, and bad input blocks."""
    try:
        data = json.loads(stdin_text)
    except ValueError:
        data = None
    # TODO 8 of 10 (finish this to pass e6): the fail-closed rule. When the parsed input is not an object, return exit 2
    #   with the reason "The hook input is not valid JSON, so the call is blocked to be safe". Example: "not json" ->
    #   {exit: 2, stderr: ...}.
    data = data if isinstance(data, dict) else {}
    if data.get("tool_name") != "Bash":
        return {"exit": 0, "stderr": ""}
    command = str((data.get("tool_input") or {}).get("command", ""))
    # TODO 9 of 10 (finish this to pass e6): the command rules. For each (pattern, reason) of BLOCKED_COMMANDS, when the
    #   pattern matches the command return exit 2 with the reason. Example: "git push origin main" -> {exit: 2, stderr:
    #   "Nothing is pushed from an agent session"}.
    return {"exit": 0, "stderr": ""}


def settings_hooks(script="python3 .claude/hooks/guard.py", timeout=10):
    """The hooks block of .claude/settings.json that runs the command hook before every Bash call."""
    # TODO 10 of 10 (finish this to pass e7): the settings block. Return the hooks block for .claude/settings.json:
    #   PreToolUse with one entry whose matcher is Bash and whose hooks list has one command hook carrying the script and
    #   the timeout. Example: settings_hooks("sh guard.sh", 3) -> a command hook with command "sh guard.sh" and timeout 3.
    return {"hooks": {"PreToolUse": [{"matcher": "", "hooks": [{"type": "command", "command": script}]}]}}
