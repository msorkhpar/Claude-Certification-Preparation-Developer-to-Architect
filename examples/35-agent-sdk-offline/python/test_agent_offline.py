import asyncio

from claude_agent_sdk import PermissionResultAllow, PermissionResultDeny

from agent_offline import add, ask_me, divide, no_push


def test_the_tools_return_text_and_a_composed_error():
    assert asyncio.run(add.handler({"a": 2, "b": 3}))["content"][0]["text"] == "Sum: 5"
    error = asyncio.run(divide.handler({"a": 1, "b": 0}))
    assert error["is_error"] is True and error["content"][0]["text"] == "Cannot divide by zero"
    assert asyncio.run(divide.handler({"a": 7, "b": 2}))["content"][0]["text"] == "Quotient: 3"


def test_the_hook_denies_a_push_and_leaves_other_commands_alone():
    deny = asyncio.run(no_push({"tool_input": {"command": "git push origin main"}}, "t", {}))
    assert deny["hookSpecificOutput"]["permissionDecision"] == "deny"
    assert asyncio.run(no_push({"tool_input": {"command": "git status"}}, "t", {})) == {}


def test_the_permission_callback_allows_listing_and_denies_the_rest():
    assert isinstance(asyncio.run(ask_me("Bash", {"command": "ls -la"}, None)), PermissionResultAllow)
    assert isinstance(asyncio.run(ask_me("mcp__calc__add", {"a": 1, "b": 2}, None)), PermissionResultAllow)
    refused = asyncio.run(ask_me("Bash", {"command": "curl example.invalid"}, None))
    assert isinstance(refused, PermissionResultDeny) and refused.message == "Bash is not allowed here"
