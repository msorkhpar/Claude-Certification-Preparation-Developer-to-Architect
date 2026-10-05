"""Code around the Claude Agent SDK: options, a permission callback, a hook and the message handling. See ../../statement.md."""
import logging
import os
import re

from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage,
                              TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, query)

log = logging.getLogger(__name__)

READ_TOOLS = ["Read", "Grep", "Glob"]
EDIT_TOOLS = ["Edit", "Write"]
SAFE_COMMANDS = ("ls", "cat", "pytest")
STATUS = {"success": "done", "error_max_turns": "max_turns", "error_max_budget_usd": "budget", "error_during_execution": "failed"}


def _resolve(project_dir, path):
    return os.path.normpath(os.path.join(project_dir, path))


def _inside(project_dir, path):
    full, root = _resolve(project_dir, path), os.path.normpath(project_dir)
    return full == root or full.startswith(root + os.sep)


def _is_secret_name(base):
    return base.startswith(".env") and base != ".env.example"


def _dangerous(command):
    return bool(re.search(r"\bsudo\b", command)) or "rm -rf" in command


def _chained(command):
    return bool(re.search(r"[;&|<>`]|\$\(", command))


def _command_allowed(command):
    words = command.split()
    return bool(words) and words[0] in SAFE_COMMANDS


def decide(tool_name, tool_input, project_dir, mode="readonly"):
    """The permission policy as plain data: {"behavior": "allow"} or {"behavior": "deny", "message", "interrupt"}."""
    log.debug("decide input: %r %r", tool_name, tool_input)
    def deny(message, interrupt=False):
        return {"behavior": "deny", "message": message, "interrupt": interrupt}

    if tool_name in READ_TOOLS + EDIT_TOOLS:
        path = tool_input.get("file_path") or tool_input.get("path")
        if tool_name in EDIT_TOOLS and mode != "edit":
            return deny("Edits are not allowed in readonly mode")
        if path is not None:
            if not _inside(project_dir, path):
                return deny(f"{path} is outside the project")
            base = os.path.basename(_resolve(project_dir, path))
            if _is_secret_name(base):
                return deny(f"{base} holds secrets and is never read")
            if tool_name in EDIT_TOOLS and ".git" in _resolve(project_dir, path).split(os.sep):
                return deny(f"{path} is inside .git")
        return {"behavior": "allow"}
    if tool_name == "Bash":
        command = tool_input.get("command", "")
        if _dangerous(command):
            return deny("Dangerous command", True)
        if _chained(command):
            return deny("Command not allowed: no chaining or redirection")
        if _command_allowed(command):
            return {"behavior": "allow"}
        return deny("Command not allowed: only ls, cat and pytest")
    return deny(f"{tool_name} is not allowed")


def make_can_use_tool(project_dir, mode="readonly"):
    async def can_use_tool(tool_name, tool_input, context):
        verdict = decide(tool_name, tool_input, project_dir, mode)
        if verdict["behavior"] == "allow":
            return PermissionResultAllow(updated_input=tool_input)
        return PermissionResultDeny(message=verdict["message"], interrupt=verdict["interrupt"])
    return can_use_tool


def _is_push(command):
    return bool(re.search(r"\bgit\s+push\b", command))


async def bash_guard(input_data, tool_use_id, context):
    """A PreToolUse hook: nothing is pushed from an agent."""
    if _is_push((input_data.get("tool_input") or {}).get("command", "")):
        return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "Nothing is pushed from an agent"}}
    return {}


def build_options(project_dir, cli_path, mode="readonly"):
    return ClaudeAgentOptions(
        cli_path=cli_path, cwd=project_dir, tools=READ_TOOLS + ["Bash"] + (EDIT_TOOLS if mode == "edit" else []), allowed_tools=[],
        disallowed_tools=["Bash(rm *)"], max_turns=6, max_budget_usd=0.5, permission_mode="default", setting_sources=[],
        can_use_tool=make_can_use_tool(project_dir, mode), hooks={"PreToolUse": [HookMatcher(matcher="Bash", hooks=[bash_guard])]})


def _denied(content):
    return sum(1 for b in content if isinstance(b, ToolResultBlock) and b.is_error)


def _status(subtype):
    return STATUS.get(subtype, subtype)


def summarize(messages):
    text, tools, denied, result = "", [], 0, None
    for message in messages:
        if isinstance(message, AssistantMessage):
            for block in message.content:
                if isinstance(block, TextBlock):
                    text = block.text
                elif isinstance(block, ToolUseBlock):
                    tools.append(block.name)
        elif isinstance(message, UserMessage) and isinstance(message.content, list):
            denied += _denied(message.content)
        elif isinstance(message, ResultMessage):
            result = message
    if result is None:
        return {"status": "incomplete", "text": text, "tools": tools, "turns": 0, "cost": 0.0, "denied": denied}
    return {"status": _status(result.subtype), "text": result.result or text, "tools": tools, "turns": result.num_turns,
            "cost": result.total_cost_usd or 0.0, "denied": denied}


async def run_agent(prompt, project_dir, cli_path, mode="readonly"):
    messages = []
    try:
        async for message in query(prompt=prompt, options=build_options(project_dir, cli_path, mode)):
            messages.append(message)
    except Exception:
        # After an error result (turn limit, budget) a single-shot query() yields the result and then raises, because the process exits with a
        # nonzero code. That is not a failure of the run; a crash before any result message is, and is not hidden.
        if not any(isinstance(m, ResultMessage) for m in messages):
            raise
    return summarize(messages)
