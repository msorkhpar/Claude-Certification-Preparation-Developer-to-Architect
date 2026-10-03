#!/usr/bin/env python3
"""A scripted stand-in for the Claude Code binary, for driving the Agent SDKs offline.

The Agent SDKs (Python and TypeScript) start the Claude Code binary as a child process and talk to it with
newline-delimited JSON on stdin and stdout (the `stream-json` protocol, plus a small control protocol for the
initialise handshake, permission prompts and hook callbacks). Point the SDK at this file
(`cli_path` in Python, `pathToClaudeCodeExecutable` in TypeScript) and the SDK's own client code runs for real:
it builds the command line, sends the handshake, answers the permission and hook callbacks and parses every
message. Only the model and the tools behind the binary are replaced by a script.

The script is a JSON file named by the environment variable FAKE_CLAUDE_SCRIPT:

    {"session_id": "s1", "model": "claude-sonnet-5-5",
     "turns": [[ step, step, ... ]]}        # one list of steps per user message received

Steps:
    {"say": "text"}                                           an assistant message with a text block
    {"tool": {"id": "toolu_1", "name": "Bash", "input": {...}, "output": "text", "is_error": false}}
                                                              the model asks for a tool; the stand-in applies
                                                              hooks and the permission rules, then reports
                                                              the output (or the denial) as a tool_result
    {"result": {"subtype": "success", "result": "text", "cost": 0.01, "turns": 2}}
                                                              the final result message (always last)

Permission rules applied to a tool step (a simplified copy of the documented behaviour, not the real binary):
a tool in --disallowedTools is denied; a tool in --allowedTools runs without asking; permission mode
bypassPermissions runs it; mode dontAsk denies it; any other tool is sent to the SDK as a `can_use_tool`
control request and the answer decides. PreToolUse hooks registered at the handshake are called first and may
deny. Every message received and the command line are appended to the file named by FAKE_CLAUDE_RECORD.
Standard library only.
"""
import json
import os
import sys


def main():
    argv = sys.argv[1:]
    if argv[:1] == ["-v"] or argv[:1] == ["--version"]:
        print("2.1.287 (course stand-in)")
        return 0
    record = os.environ.get("FAKE_CLAUDE_RECORD")

    def rec(kind, value):
        if record:
            with open(record, "a") as f:
                f.write(json.dumps({kind: value}) + "\n")

    rec("argv", argv)

    def flag(name):
        return argv[argv.index(name) + 1] if name in argv else None

    allowed = [t for t in (flag("--allowedTools") or "").split(",") if t]
    disallowed = [t for t in (flag("--disallowedTools") or "").split(",") if t]
    mode = flag("--permission-mode") or "default"
    max_turns = int(flag("--max-turns")) if flag("--max-turns") else None
    script = json.load(open(os.environ["FAKE_CLAUDE_SCRIPT"]))
    session = script.get("session_id", "session-1")
    model = script.get("model", "claude-sonnet-5-5")
    hooks = {}
    pending = {}
    counter = [0]

    def send(obj):
        sys.stdout.write(json.dumps(obj) + "\n")
        sys.stdout.flush()

    def ask(subtype, **fields):
        """A control request to the SDK; returns the response payload."""
        counter[0] += 1
        rid = f"cli_{counter[0]}"
        send({"type": "control_request", "request_id": rid, "request": {"subtype": subtype, **fields}})
        while rid not in pending:
            line = sys.stdin.readline()
            if not line:
                raise SystemExit(1)
            msg = json.loads(line)
            rec("stdin", msg)
            if msg.get("type") == "control_response":
                pending[msg["response"]["request_id"]] = msg["response"]
        return pending.pop(rid)

    def assistant(blocks, stop="end_turn"):
        send({"type": "assistant", "session_id": session, "parent_tool_use_id": None,
              "message": {"id": "msg_stub", "role": "assistant", "model": model, "content": blocks, "stop_reason": stop,
                          "usage": {"input_tokens": 1, "output_tokens": 1}}})

    def run_hooks(event, tool):
        """Call the SDK's hook callbacks that match; return a deny reason or None."""
        for entry in hooks.get(event, []):
            matcher = entry.get("matcher")
            if matcher and tool["name"] not in matcher.split("|"):
                continue
            for cb in entry.get("hookCallbackIds", []):
                answer = ask("hook_callback", callback_id=cb, tool_use_id=tool["id"],
                             input={"hook_event_name": event, "session_id": session, "tool_name": tool["name"],
                                    "tool_input": tool["input"], "tool_use_id": tool["id"]})
                out = (answer.get("response") or {})
                spec = out.get("hookSpecificOutput") or {}
                if spec.get("permissionDecision") == "deny":
                    return spec.get("permissionDecisionReason", "denied by a hook")
                if out.get("decision") == "block":
                    return out.get("reason", "blocked by a hook")
        return None

    def run_tool(tool):
        assistant([{"type": "tool_use", "id": tool["id"], "name": tool["name"], "input": tool["input"]}], "tool_use")
        denied = run_hooks("PreToolUse", tool)
        if denied is None:
            name = tool["name"]
            if name in disallowed or mode == "dontAsk" and name not in allowed:
                denied = f"{name} is not permitted"
            elif name in allowed or mode == "bypassPermissions":
                pass
            else:
                answer = ask("can_use_tool", tool_name=name, input=tool["input"], tool_use_id=tool["id"])
                decision = answer.get("response") or {}
                if decision.get("behavior") == "deny":
                    denied = decision.get("message", "denied")
                else:
                    tool = {**tool, "input": decision.get("updatedInput", tool["input"])}
        if denied is not None:
            content, is_error = f"Permission denied: {denied}", True
        else:
            content, is_error = tool.get("output", ""), bool(tool.get("is_error"))
            run_hooks("PostToolUse", tool)
        send({"type": "user", "session_id": session, "parent_tool_use_id": None,
              "message": {"role": "user", "content": [{"type": "tool_result", "tool_use_id": tool["id"],
                                                       "content": content, "is_error": is_error}]}})
        return denied is None

    turns = list(script["turns"])
    for line in sys.stdin:
        msg = json.loads(line)
        rec("stdin", msg)
        kind = msg.get("type")
        if kind == "control_request":
            request = msg["request"]
            if request.get("subtype") == "initialize":
                hooks.update(request.get("hooks") or {})
            send({"type": "control_response", "response": {"subtype": "success", "request_id": msg["request_id"], "response": {"commands": [], "models": []}}})
        elif kind == "user" and turns:
            send({"type": "system", "subtype": "init", "session_id": session, "model": model, "cwd": "/work",
                  "tools": allowed, "permissionMode": mode, "apiKeySource": "none"})
            tool_calls = 0
            for step in turns.pop(0):
                if "say" in step:
                    assistant([{"type": "text", "text": step["say"]}])
                elif "tool" in step:
                    tool_calls += 1
                    if max_turns is not None and tool_calls >= max_turns:
                        send({"type": "result", "subtype": "error_max_turns", "is_error": True, "duration_ms": 1, "duration_api_ms": 1,
                              "num_turns": tool_calls, "session_id": session, "total_cost_usd": 0.0, "usage": {}})
                        break
                    run_tool(step["tool"])
                elif "result" in step:
                    r = step["result"]
                    send({"type": "result", "subtype": r.get("subtype", "success"), "is_error": r.get("subtype", "success") != "success",
                          "duration_ms": 1, "duration_api_ms": 1, "num_turns": r.get("turns", 1), "session_id": session,
                          "result": r.get("result"), "total_cost_usd": r.get("cost", 0.0), "usage": {}})
    return 0


if __name__ == "__main__":
    sys.exit(main())
