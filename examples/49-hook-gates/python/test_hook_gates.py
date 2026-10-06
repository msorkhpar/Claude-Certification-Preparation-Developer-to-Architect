import asyncio
import json

from hook_gates import readable_order, run_guard, tier


def test_each_limit_belongs_to_the_lower_tier():
    assert [tier(a)[0] for a in (200, 200.01, 500, 500.01)] == ["allow", "ask", "ask", "deny"]


def test_an_amount_that_cannot_be_read_is_denied_and_not_allowed():
    assert [tier(a)[0] for a in (None, 0, -1, "50", True)] == ["deny"] * 5


def test_the_order_gets_a_date_and_a_status_word():
    out = asyncio.run(readable_order({"tool_name": "get_order", "tool_response": json.dumps({"created": 1700000000, "status": 2})}, "t", None))
    assert json.loads(out["hookSpecificOutput"]["updatedToolOutput"]) == {"created": "2023-11-14", "status": "declined"}


def test_the_command_hook_blocks_with_exit_two_and_passes_other_commands():
    assert run_guard(json.dumps({"tool_name": "Bash", "tool_input": {"command": "git push"}})) == (2, "guard: nothing is pushed from an agent session")
    assert run_guard(json.dumps({"tool_name": "Bash", "tool_input": {"command": "ls"}})) == (0, "")
    assert run_guard("{nope")[0] == 2
