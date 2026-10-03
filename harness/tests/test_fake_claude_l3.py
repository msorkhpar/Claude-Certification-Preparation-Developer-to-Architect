"""The stand-in's subagent and hook extensions: parent ids on messages, the agents of the handshake, and the hook answers that change a call."""
import json
import os
import subprocess
import sys
from pathlib import Path

FAKE = str(Path(__file__).resolve().parents[1] / "fake_claude.py")
SUCCESS = {"result": {"subtype": "success", "result": "done", "turns": 1}}


def run(steps, tmp_path, args=(), hooks=None, agents=None, answer=None):
    """Start the stand-in, send the handshake and one user message; `answer(request)` returns the response to a control request."""
    script = tmp_path / "script.json"
    record = tmp_path / "record.jsonl"
    script.write_text(json.dumps({"session_id": "s", "turns": [steps]}))
    env = {**os.environ, "FAKE_CLAUDE_SCRIPT": str(script), "FAKE_CLAUDE_RECORD": str(record)}
    child = subprocess.Popen([sys.executable, FAKE, *args], stdin=subprocess.PIPE, stdout=subprocess.PIPE, text=True, env=env)
    send = lambda obj: (child.stdin.write(json.dumps(obj) + "\n"), child.stdin.flush())
    send({"type": "control_request", "request_id": "init", "request": {"subtype": "initialize", "hooks": hooks or {}, "agents": agents}})
    send({"type": "user", "message": {"role": "user", "content": "go"}})
    messages, asked = [], []
    for line in child.stdout:
        message = json.loads(line)
        if message.get("type") == "control_request":
            asked.append(message["request"])
            send({"type": "control_response", "response": {"subtype": "success", "request_id": message["request_id"],
                                                           "response": (answer or (lambda r: {"behavior": "allow"}))(message["request"])}})
        else:
            messages.append(message)
            if message.get("type") == "result":
                child.stdin.close()
    child.wait(timeout=20)
    return messages, asked, [json.loads(l) for l in record.read_text().splitlines()]


def tool(id, name, output="out", **extra):
    return {"tool": {"id": id, "name": name, "input": {"command": "ls"}, "output": output, **extra}}


def results(messages):
    return [(b["tool_use_id"], b["content"]) for m in messages if m["type"] == "user" for b in m["message"]["content"]]


def test_a_step_with_a_parent_marks_its_messages_as_run_inside_the_subagent(tmp_path):
    messages, _, _ = run([tool("t1", "Agent", "report"), tool("t2", "Read", "inner", parent="t1"), SUCCESS], tmp_path, ["--permission-mode", "bypassPermissions"])
    parents = [(m["type"], m["parent_tool_use_id"]) for m in messages if m["type"] in ("assistant", "user")]
    assert parents == [("assistant", None), ("user", None), ("assistant", "t1"), ("user", "t1")]


def test_the_agents_of_the_handshake_are_recorded(tmp_path):
    _, _, record = run([SUCCESS], tmp_path, agents={"reviewer": {"description": "d", "prompt": "p", "tools": ["Read"]}})
    assert {"agents": {"reviewer": {"description": "d", "prompt": "p", "tools": ["Read"]}}} in record


def test_a_post_tool_use_hook_sees_the_output_and_can_replace_it(tmp_path):
    hooks = {"PostToolUse": [{"matcher": "Bash", "hookCallbackIds": ["post"]}]}
    seen = []

    def answer(request):
        if request["subtype"] == "hook_callback":
            seen.append(request["input"]["tool_response"])
            return {"hookSpecificOutput": {"hookEventName": "PostToolUse", "updatedToolOutput": "cleaned"}}
        return {"behavior": "allow"}

    messages, _, _ = run([tool("t1", "Bash", "raw 1700000000"), SUCCESS], tmp_path, ["--allowedTools", "Bash"], hooks=hooks, answer=answer)
    assert seen == ["raw 1700000000"] and results(messages) == [("t1", "cleaned")]


def test_a_pre_tool_use_hook_can_change_the_input_that_the_permission_step_sees(tmp_path):
    hooks = {"PreToolUse": [{"matcher": "Bash", "hookCallbackIds": ["pre"]}]}

    def answer(request):
        if request["subtype"] == "hook_callback":
            return {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "ask", "updatedInput": {"command": "ls -l"}}}
        return {"behavior": "allow"}

    _, asked, _ = run([tool("t1", "Bash"), SUCCESS], tmp_path, hooks=hooks, answer=answer)
    assert [(a["subtype"], a["input"]) for a in asked if a["subtype"] == "can_use_tool"] == [("can_use_tool", {"command": "ls -l"})]


def test_a_hook_that_answers_ask_sends_an_approved_call_to_the_permission_callback(tmp_path):
    hooks = {"PreToolUse": [{"matcher": "Bash", "hookCallbackIds": ["pre"]}]}
    answer = lambda request: ({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "ask"}} if request["subtype"] == "hook_callback"
                              else {"behavior": "deny", "message": "a person said no"})
    messages, asked, _ = run([tool("t1", "Bash"), SUCCESS], tmp_path, ["--allowedTools", "Bash"], hooks=hooks, answer=answer)
    assert [a["subtype"] for a in asked] == ["hook_callback", "can_use_tool"] and results(messages) == [("t1", "Permission denied: a person said no")]


def test_a_hook_that_answers_allow_approves_the_call_without_asking_but_a_deny_rule_still_wins(tmp_path):
    hooks = {"PreToolUse": [{"matcher": "Bash", "hookCallbackIds": ["pre"]}]}
    answer = lambda request: ({"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "allow"}} if request["subtype"] == "hook_callback"
                              else {"behavior": "deny", "message": "asked"})
    messages, asked, _ = run([tool("t1", "Bash", "ran"), SUCCESS], tmp_path, hooks=hooks, answer=answer)
    assert [a["subtype"] for a in asked] == ["hook_callback"] and results(messages) == [("t1", "ran")]
    messages, asked, _ = run([tool("t1", "Bash", "ran"), SUCCESS], tmp_path, ["--disallowedTools", "Bash"], hooks=hooks, answer=answer)
    assert results(messages) == [("t1", "Permission denied: Bash is not permitted")]
