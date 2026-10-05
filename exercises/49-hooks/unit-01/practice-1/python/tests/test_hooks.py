import asyncio
import json
import os
import sys
import tempfile
from pathlib import Path

from claude_agent_sdk import ToolResultBlock, UserMessage, query

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from hooks import build_options, command_hook, post_normalise, pre_refund, settings_hooks

HERE = Path(__file__).resolve()
FAKE = next(str(p) for p in (Path("/w/harness/fake_claude.py"), *(HERE.parents[i] / "harness" / "fake_claude.py" for i in range(min(len(HERE.parents), 8)))) if p.exists())


def pre(tool_name="process_refund", **tool_input):
    out = asyncio.run(pre_refund({"hook_event_name": "PreToolUse", "tool_name": tool_name, "tool_input": tool_input}, "t1", None))
    assert out is not None, "pre_refund returned None"
    return out.get("hookSpecificOutput", {}) if out else {}


def post(response):
    out = asyncio.run(post_normalise({"hook_event_name": "PostToolUse", "tool_name": "get_order", "tool_input": {}, "tool_response": response}, "t1", None))
    assert out is not None, "post_normalise returned None"
    spec = out.get("hookSpecificOutput") if out else None
    return None if spec is None else spec.get("updatedToolOutput")


def decision(**tool_input):
    return pre(**tool_input).get("permissionDecision")


def test_m1_the_gate_allows_a_small_refund_asks_about_a_middle_one_and_denies_a_large_one():
    small, middle, large = pre(amount=50), pre(amount=350), pre(amount=900)
    assert small.get("permissionDecision") == "allow" and middle.get("permissionDecision") == "ask" and large.get("permissionDecision") == "deny"
    assert small.get("hookEventName") == "PreToolUse"
    assert "900" in large.get("permissionDecisionReason", "") and "500" in large.get("permissionDecisionReason", "")


def test_e1_each_limit_belongs_to_the_lower_tier_and_the_next_cent_goes_up():
    assert [decision(amount=a) for a in (200, 200.01, 500, 500.01)] == ["allow", "ask", "ask", "deny"]
    assert decision(amount=0.01) == "allow"


def test_e2_a_missing_or_invalid_amount_is_denied_and_other_tools_are_left_alone():
    for bad in (None, 0, -5, "100", True, float("nan"), float("inf"), [50]):
        assert decision(amount=bad) == "deny", f"amount {bad!r} must be denied"
    assert pre().get("permissionDecision") == "deny"
    other = asyncio.run(pre_refund({"hook_event_name": "PreToolUse", "tool_name": "get_order", "tool_input": {"amount": 9999}}, "t1", None))
    assert other is not None and not other


def test_e3_the_output_gets_a_date_a_status_word_and_a_decimal_amount():
    raw = json.dumps({"order": "A-7", "created": 1700000000, "status": 2, "amount_cents": 12950, "note": "ok"})
    out = post(raw)
    assert out is not None
    assert json.loads(out) == {"order": "A-7", "created": "2023-11-14", "status": "declined", "amount": "129.50", "note": "ok"}
    assert json.loads(post(json.dumps({"created": 1700000000000, "status": 0, "amount_cents": 5})))["created"] == "2023-11-14"
    assert json.loads(post(json.dumps({"status": 9, "amount_cents": 5})) or "{}") == {"status": "unknown", "amount": "0.05"}


def test_e4_output_that_is_not_json_or_is_already_readable_is_left_alone():
    assert post("plain text, not json") is None
    assert post("[1, 2, 3]") is None
    assert post(json.dumps({"created": "2023-11-14", "status": "approved", "amount": "129.50"})) is None
    again = post(json.dumps({"created": 1700000000, "status": 1}))
    assert again is not None and post(again) is None


def test_e5_the_hooks_are_registered_on_tool_name_matchers_with_a_timeout():
    o = build_options("/proj")
    assert o is not None and o.cwd == "/proj" and sorted(o.hooks) == ["PostToolUse", "PreToolUse"]
    pre_m, post_m = o.hooks["PreToolUse"][0], o.hooks["PostToolUse"][0]
    assert pre_m.matcher == "process_refund" and pre_m.hooks == [pre_refund] and pre_m.timeout and pre_m.timeout <= 10
    assert post_m.matcher == "get_order|get_refund" and post_m.hooks == [post_normalise] and post_m.timeout and post_m.timeout <= 10
    assert build_options("/proj", cli_path="/bin/x").cli_path == "/bin/x"


def test_e6_a_command_hook_blocks_with_exit_two_and_a_reason_and_fails_closed_on_bad_input():
    def run(payload):
        out = command_hook(payload if isinstance(payload, str) else json.dumps(payload))
        assert out is not None, "command_hook returned None"
        return out
    pushed = run({"tool_name": "Bash", "tool_input": {"command": "git push origin main"}})
    assert pushed["exit"] == 2 and "pushed" in pushed["stderr"]
    assert run({"tool_name": "Bash", "tool_input": {"command": "rm -rf build"}})["exit"] == 2
    assert run({"tool_name": "Bash", "tool_input": {"command": "git status"}}) == {"exit": 0, "stderr": ""}
    assert run({"tool_name": "Read", "tool_input": {"command": "git push"}})["exit"] == 0
    for broken in ("{not json", "", "[1]", "null"):
        assert run(broken)["exit"] == 2 and run(broken)["stderr"], f"{broken!r} must block"


def test_e7_the_settings_block_runs_the_command_hook_on_bash_with_a_timeout():
    s = settings_hooks()
    assert s is not None
    entry = s["hooks"]["PreToolUse"][0]
    assert entry["matcher"] == "Bash" and entry["hooks"] == [{"type": "command", "command": "python3 .claude/hooks/guard.py", "timeout": 10}]
    assert settings_hooks("sh guard.sh", 3)["hooks"]["PreToolUse"][0]["hooks"][0] == {"type": "command", "command": "sh guard.sh", "timeout": 3}


def test_e8_a_run_through_the_sdk_denies_the_large_refund_and_shows_the_model_a_readable_order():
    d = tempfile.mkdtemp()
    order = json.dumps({"order": "A-7", "created": 1700000000, "status": 1, "amount_cents": 12950})
    steps = [{"tool": {"id": "t1", "name": "get_order", "input": {"order": "A-7"}, "output": order}},
             {"tool": {"id": "t2", "name": "process_refund", "input": {"order": "A-7", "amount": 900}, "output": "refunded"}},
             {"tool": {"id": "t3", "name": "process_refund", "input": {"order": "A-7", "amount": 50}, "output": "refunded"}},
             {"result": {"subtype": "success", "result": "done", "cost": 0.01, "turns": 4}}]
    script = Path(d, "script.json")
    script.write_text(json.dumps({"session_id": "s1", "turns": [steps]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(Path(d, "record.jsonl"))
    options = build_options(d, cli_path=FAKE)
    assert options is not None

    async def collect():
        return [m async for m in query(prompt="Refund order A-7", options=options)]
    results = {}
    for m in asyncio.run(collect()):
        if isinstance(m, UserMessage) and isinstance(m.content, list):
            for b in m.content:
                if isinstance(b, ToolResultBlock):
                    results[b.tool_use_id] = b.content if isinstance(b.content, str) else json.dumps(b.content)
    assert json.loads(results.get("t1") or "null") == {"order": "A-7", "created": "2023-11-14", "status": "approved", "amount": "129.50"}
    assert "refunded" not in results.get("t2", "") and "limit" in results.get("t2", "")
    assert results.get("t3") == "refunded"
