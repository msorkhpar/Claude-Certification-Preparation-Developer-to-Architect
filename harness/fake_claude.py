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
    {"exit": 2}                                               the process dies at once with that exit code (a crash)

Rules applied to a tool step (a simplified copy of the documented behaviour, not the real binary): a tool that is
not in --tools (when that flag is given) does not exist (MCP tools are not affected by --tools); PreToolUse hooks registered at the handshake are called
and may deny; a tool in --disallowedTools is denied; a tool in --allowedTools (a pattern such as mcp__calc__* matches too) runs without asking; permission mode
bypassPermissions runs it; mode dontAsk denies it; any other tool is sent to the SDK as a `can_use_tool` control
request and the answer decides. The command line, the working directory, every message received and every
control request sent are appended to the file named by FAKE_CLAUDE_RECORD.
A tool named mcp__<server>__<tool> is run by the SDK's own in-process MCP server: the stand-in sends it JSON-RPC as `mcp_message`
control requests (initialize, then tools/call) and reports what the SDK answered.
Like the real binary in single-shot mode, the process exits with code 1 after it sent an error result (a subtype other than
"success", or the turn limit), which makes the SDK raise after it has yielded that result.
Standard library only.
"""
import fnmatch
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
    rec("cwd", os.getcwd())

    def flag(name):
        return argv[argv.index(name) + 1] if name in argv else None

    available = None if "--tools" not in argv else [t for t in flag("--tools").split(",") if t]
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
        rec("ask", {"subtype": subtype, "tool_name": fields.get("tool_name"), "tool_use_id": fields.get("tool_use_id")})
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

    def mcp_call(tool):
        """A tool of an in-process SDK MCP server (mcp__<server>__<tool>): the SDK answers JSON-RPC sent as `mcp_message` control requests."""
        _, server, name = tool["name"].split("__", 2)
        ids = iter(range(1, 100))

        def rpc(method, params=None, notify=False):
            message = {"jsonrpc": "2.0", "method": method, **({"params": params} if params else {}), **({} if notify else {"id": next(ids)})}
            return ((ask("mcp_message", server_name=server, message=message).get("response") or {}).get("mcp_response")) or {}

        rpc("initialize", {"protocolVersion": "2025-11-25", "capabilities": {}, "clientInfo": {"name": "course-stand-in", "version": "1"}})
        rpc("notifications/initialized", notify=True)
        result = rpc("tools/call", {"name": name, "arguments": tool["input"]}).get("result") or {}
        return "".join(c.get("text", "") for c in result.get("content", [])), bool(result.get("isError"))

    def run_tool(tool):
        assistant([{"type": "tool_use", "id": tool["id"], "name": tool["name"], "input": tool["input"]}], "tool_use")
        denied = None if available is None or tool["name"] in available or tool["name"].startswith("mcp__") else f"No such tool available: {tool['name']}"
        denied = denied or run_hooks("PreToolUse", tool)
        if denied is None:
            name = tool["name"]
            if name in disallowed or mode == "dontAsk" and name not in allowed:
                denied = f"{name} is not permitted"
            elif any(fnmatch.fnmatchcase(name, pattern) for pattern in allowed) or mode == "bypassPermissions":
                pass
            else:
                answer = ask("can_use_tool", tool_name=name, input=tool["input"], tool_use_id=tool["id"])
                decision = answer.get("response") or {}
                if decision.get("behavior") == "deny":
                    denied = decision.get("message", "denied")
                else:
                    tool = {**tool, "input": decision.get("updatedInput", tool["input"])}
        if denied is not None:
            content, is_error = (denied if denied.startswith("No such tool") else f"Permission denied: {denied}"), True
        else:
            content, is_error = mcp_call(tool) if tool["name"].startswith("mcp__") else (tool.get("output", ""), bool(tool.get("is_error")))
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
                  "tools": available if available is not None else allowed, "permissionMode": mode, "apiKeySource": "none"})
            tool_calls = 0
            exit_code = 0
            for step in turns.pop(0):
                if "exit" in step:
                    sys.stdout.flush()
                    return step["exit"]
                if "say" in step:
                    assistant([{"type": "text", "text": step["say"]}])
                elif "tool" in step:
                    tool_calls += 1
                    if max_turns is not None and tool_calls >= max_turns:
                        send({"type": "result", "subtype": "error_max_turns", "is_error": True, "duration_ms": 1, "duration_api_ms": 1,
                              "num_turns": tool_calls, "session_id": session, "total_cost_usd": 0.0, "usage": {}, "modelUsage": {}, "permission_denials": [], "errors": []})
                        exit_code = 1
                        break
                    run_tool(step["tool"])
                elif "result" in step:
                    r = step["result"]
                    send({"type": "result", "subtype": r.get("subtype", "success"), "is_error": r.get("subtype", "success") != "success",
                          "duration_ms": 1, "duration_api_ms": 1, "num_turns": r.get("turns", 1), "session_id": session,
                          "result": r.get("result"), "total_cost_usd": r.get("cost", 0.0), "usage": {}, "modelUsage": {}, "permission_denials": [], "errors": []})
                    exit_code = 0 if r.get("subtype", "success") == "success" else 1
            if exit_code:
                sys.stdout.flush()
                return exit_code
    return 0


if __name__ == "__main__":
    sys.exit(main())
