"""The scripted stand-in for the Claude Code binary: its protocol and the rules it applies, tested with the standard library only."""
import json
import os
import subprocess
import sys
from pathlib import Path

FAKE = str(Path(__file__).resolve().parents[1] / "fake_claude.py")


def run(script, args=(), answers=None, tmp_path=None):
    """Start the stand-in, send the handshake and one user message, answer its control requests; return (messages, control requests, exit code)."""
    script_file = tmp_path / "script.json"
    script_file.write_text(json.dumps(script))
    env = {**os.environ, "FAKE_CLAUDE_SCRIPT": str(script_file)}
    child = subprocess.Popen([sys.executable, FAKE, *args], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True, env=env)
    send = lambda obj: (child.stdin.write(json.dumps(obj) + "\n"), child.stdin.flush())
    send({"type": "control_request", "request_id": "init", "request": {"subtype": "initialize", "hooks": {}}})
    send({"type": "user", "message": {"role": "user", "content": "go"}})
    messages, asked = [], []
    for line in child.stdout:
        message = json.loads(line)
        if message.get("type") == "control_request":
            asked.append(message["request"])
            decision = (answers or {}).get(message["request"].get("tool_name"), {"behavior": "allow"})
            send({"type": "control_response", "response": {"subtype": "success", "request_id": message["request_id"], "response": decision}})
        else:
            messages.append(message)
    child.stdin.close()
    return messages, asked, child.wait(timeout=20)


def tool_results(messages):
    return [(b["content"], b["is_error"]) for m in messages if m["type"] == "user" for b in m["message"]["content"]]


def turn(*steps):
    return {"session_id": "s", "turns": [list(steps)]}


def tool(name, **extra):
    return {"tool": {"id": "t1", "name": name, "input": {"command": "ls"}, "output": "listing", **extra}}


SUCCESS = {"result": {"subtype": "success", "result": "done", "turns": 1}}


def test_a_tool_the_stand_in_was_not_given_does_not_exist(tmp_path):
    messages, _, code = run(turn(tool("Bash"), SUCCESS), ["--tools", "Read", "--permission-mode", "bypassPermissions"], tmp_path=tmp_path)
    assert tool_results(messages) == [("No such tool available: Bash", True)] and code == 0


def test_an_allowed_tool_runs_without_asking_and_others_ask(tmp_path):
    messages, asked, _ = run(turn(tool("Bash"), SUCCESS), ["--allowedTools", "Bash"], tmp_path=tmp_path)
    assert tool_results(messages) == [("listing", False)] and asked == []
    messages, asked, _ = run(turn(tool("Bash"), SUCCESS), tmp_path=tmp_path)
    assert [a["subtype"] for a in asked] == ["can_use_tool"] and tool_results(messages) == [("listing", False)]


def test_the_answer_to_the_permission_question_decides(tmp_path):
    messages, _, _ = run(turn(tool("Bash"), SUCCESS), answers={"Bash": {"behavior": "deny", "message": "no shell"}}, tmp_path=tmp_path)
    assert tool_results(messages) == [("Permission denied: no shell", True)]


def test_disallowed_tools_win_and_dont_ask_denies_what_is_not_allowed(tmp_path):
    messages, asked, _ = run(turn(tool("Bash"), SUCCESS), ["--disallowedTools", "Bash", "--allowedTools", "Bash"], tmp_path=tmp_path)
    assert tool_results(messages) == [("Permission denied: Bash is not permitted", True)] and asked == []
    messages, _, _ = run(turn(tool("Bash"), SUCCESS), ["--permission-mode", "dontAsk"], tmp_path=tmp_path)
    assert tool_results(messages) == [("Permission denied: Bash is not permitted", True)]


def test_an_allowed_pattern_with_a_wildcard_matches_by_name(tmp_path):
    messages, asked, _ = run(turn(tool("Bash"), SUCCESS), ["--allowedTools", "Ba*"], tmp_path=tmp_path)
    assert tool_results(messages) == [("listing", False)] and asked == []
    _, asked, _ = run(turn(tool("Bash"), SUCCESS), ["--allowedTools", "Re*"], tmp_path=tmp_path)
    assert [a["subtype"] for a in asked] == ["can_use_tool"]


def test_the_turn_limit_ends_the_run_with_an_error_result_and_a_nonzero_exit(tmp_path):
    messages, _, code = run(turn(tool("Bash"), tool("Bash"), SUCCESS), ["--allowedTools", "Bash", "--max-turns", "2"], tmp_path=tmp_path)
    results = [m for m in messages if m["type"] == "result"]
    assert [r["subtype"] for r in results] == ["error_max_turns"] and results[0]["is_error"] is True and code == 1


def test_an_error_result_from_the_script_exits_nonzero_and_a_success_exits_zero(tmp_path):
    _, _, code = run(turn({"result": {"subtype": "error_max_budget_usd"}}), tmp_path=tmp_path)
    assert code == 1
    _, _, code = run(turn(SUCCESS), tmp_path=tmp_path)
    assert code == 0


def test_an_exit_step_is_a_crash_with_no_result_message(tmp_path):
    messages, _, code = run(turn({"say": "hello"}, {"exit": 3}), tmp_path=tmp_path)
    assert code == 3 and not [m for m in messages if m["type"] == "result"]
