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
    """TODO 1 of 9 (unlocks e2): does this file name hold secrets?

    Receives a file name (no folders). Returns True when it starts with `.env` unless it is `.env.example`.
    Example: _is_secret_name(".env.local") -> True, _is_secret_name(".env.example") -> False
    """
    return False


def _dangerous(command):
    """TODO 2 of 9 (unlocks e3): is this shell command dangerous enough to stop the run?

    Receives a command. Returns True when it contains the word `sudo` (as a whole word) or the text `rm -rf`.
    Example: _dangerous("pytest && sudo reboot") -> True, _dangerous("ls") -> False
    """
    return False


def _chained(command):
    """TODO 3 of 9 (unlocks e3): does this command chain, pipe, redirect or substitute?

    Receives a command. Returns True when it contains `;`, `&`, `|`, `<`, `>`, a backtick or `$(`.
    Example: _chained("ls | wc") -> True, _chained("ls -la") -> False
    """
    return False


def _command_allowed(command):
    """TODO 4 of 9 (unlocks e3): is the first word of the command one of SAFE_COMMANDS?

    Receives a command. Returns True when its first word is in SAFE_COMMANDS; False for anything else, including an empty command.
    Example: _command_allowed("  pytest -q") -> True, _command_allowed("python x.py") -> False
    """
    return False


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
    """TODO 5 of 9 (unlocks m1 and e7): the callback the SDK asks for a tool that is not already decided.

    Receives the project directory and the mode. Returns an async function (tool_name, tool_input, context) that turns `decide()`
    into `PermissionResultAllow(updated_input=tool_input)` or `PermissionResultDeny(message=..., interrupt=...)`.
    Example: for {"behavior": "deny", "message": "no", "interrupt": False} it returns PermissionResultDeny(message="no", interrupt=False)
    """
    async def can_use_tool(tool_name, tool_input, context):
        return PermissionResultDeny(message="not written yet", interrupt=False)
    return can_use_tool


def _is_push(command):
    """TODO 6 of 9 (unlocks e4): does this command run `git push`?

    Receives a command. Returns True when it has the two words `git` and `push` with any white space between them, as whole words.
    Example: _is_push("echo ok && git  push origin") -> True, _is_push("git pushd") -> False, _is_push("legit push") -> False
    """
    return False


async def bash_guard(input_data, tool_use_id, context):
    """A PreToolUse hook: nothing is pushed from an agent."""
    if _is_push((input_data.get("tool_input") or {}).get("command", "")):
        return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "Nothing is pushed from an agent"}}
    return {}


def build_options(project_dir, cli_path, mode="readonly"):
    """TODO 7 of 9 (unlocks e1): the SDK options of the agent.

    Receives the project directory, the CLI path and the mode. Returns ClaudeAgentOptions with the CLI path, `cwd` the project, `tools`
    (the read tools, `Bash`, and the edit tools in `edit` mode), no `allowed_tools`, `Bash(rm *)` disallowed, 6 turns, a budget of 0.5,
    permission mode `default`, no setting sources, the callback from `make_can_use_tool` and a `PreToolUse` hook for `Bash` (`bash_guard`).
    Example: build_options("/proj", "/bin/cli").max_turns -> 6
    """
    return ClaudeAgentOptions(cli_path=cli_path, cwd=project_dir)


def _denied(content):
    """TODO 8 of 9 (unlocks e5 and e7): how many tool results in this list of blocks are errors?

    Receives a list of content blocks. Returns how many are a `ToolResultBlock` with `is_error` true (a missing flag is not an error).
    Example: [ToolResultBlock("t1", "no", True), ToolResultBlock("t2", "ok", False)] -> 1
    """
    return 0


def _status(subtype):
    """TODO 9 of 9 (unlocks e5): the course status of a result subtype.

    Receives a result subtype. Returns `STATUS[subtype]`, or the subtype itself when it is not in `STATUS`.
    Example: _status("error_max_turns") -> "max_turns", _status("something_new") -> "something_new"
    """
    return ""


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
