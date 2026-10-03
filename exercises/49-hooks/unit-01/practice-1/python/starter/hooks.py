"""Hooks with the Agent SDK and Claude Code: a refund gate, output normalisation, the options that register them, and a command hook. See ../../statement.md."""
from claude_agent_sdk import ClaudeAgentOptions, HookMatcher


async def pre_refund(input_data, tool_use_id, context):
    # TODO: allow small refunds, ask for middle ones, deny large ones, and fail closed on a bad amount.
    return None


async def post_normalise(input_data, tool_use_id, context):
    # TODO: replace epoch seconds, numeric status codes and cents with values the model can read.
    return None


def build_options(cwd, cli_path=None):
    # TODO: register both hooks with matchers that name tools.
    return None


def command_hook(stdin_text):
    # TODO: decide what a command hook returns for the input it receives: exit code and stderr.
    return None


def settings_hooks(script="python3 .claude/hooks/guard.py", timeout=10):
    # TODO: the hooks block of settings.json for the command hook.
    return None
