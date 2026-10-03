"""Code around the Claude Agent SDK: options, a permission callback, a hook and the message handling. See ../../statement.md."""
from claude_agent_sdk import (AssistantMessage, ClaudeAgentOptions, HookMatcher, PermissionResultAllow, PermissionResultDeny, ResultMessage,
                              TextBlock, ToolResultBlock, ToolUseBlock, UserMessage, query)

READ_TOOLS = ["Read", "Grep", "Glob"]
EDIT_TOOLS = ["Edit", "Write"]
SAFE_COMMANDS = ("ls", "cat", "pytest")
STATUS = {"success": "done", "error_max_turns": "max_turns", "error_max_budget_usd": "budget", "error_during_execution": "failed"}


def decide(tool_name, tool_input, project_dir, mode="readonly"):
    # TODO: the permission policy as data: {"behavior": "allow"} or {"behavior": "deny", "message": ..., "interrupt": ...}.
    return None


def make_can_use_tool(project_dir, mode="readonly"):
    # TODO: an async callback (tool_name, tool_input, context) that turns decide() into PermissionResultAllow or PermissionResultDeny.
    return None


async def bash_guard(input_data, tool_use_id, context):
    # TODO: a PreToolUse hook that denies a Bash command that runs git push.
    return None


def build_options(project_dir, cli_path, mode="readonly"):
    # TODO: ClaudeAgentOptions for a read-only (or edit) agent confined to the project; see the statement.
    return None


def summarize(messages):
    # TODO: fold the messages of one run into {"status", "text", "tools", "turns", "cost", "denied"}.
    return None


async def run_agent(prompt, project_dir, cli_path, mode="readonly"):
    # TODO: run query() with build_options and summarize what it yields.
    return None
