import asyncio
import json
import os
import sys
import tempfile
from pathlib import Path

from claude_agent_sdk import AssistantMessage, PermissionResultAllow, PermissionResultDeny, ResultMessage, TextBlock, ToolResultBlock, ToolUseBlock, UserMessage

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from agent import bash_guard, build_options, decide, make_can_use_tool, run_agent, summarize

P = "/proj"
HERE = Path(__file__).resolve()
FAKE = next(str(p) for p in (Path("/w/harness/fake_claude.py"), *(HERE.parents[i] / "harness" / "fake_claude.py" for i in range(min(len(HERE.parents), 8)))) if p.exists())


def tool(id, name, **tool_input):
    return {"tool": {"id": id, "name": name, "input": tool_input, "output": f"{name} ok"}}


def finish(text="All done.", subtype="success", cost=0.02, turns=3):
    return [{"say": text}, {"result": {"subtype": subtype, "result": text, "cost": cost, "turns": turns}}]


def run_raw(steps, mode="readonly"):
    """Run run_agent against the scripted CLI. Returns (summary or the exception it raised, records of the CLI, project directory)."""
    d = tempfile.mkdtemp()
    script, record = Path(d, "script.json"), Path(d, "record.jsonl")
    script.write_text(json.dumps({"session_id": "s1", "turns": [steps]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    try:
        summary = asyncio.run(run_agent("go", d, FAKE, mode)) or {}
    except Exception as e:  # noqa: BLE001 - the tests look at what run_agent raised
        summary = e
    records = [json.loads(line) for line in record.read_text().splitlines()] if record.exists() else []
    return summary, records, d


def run(steps, mode="readonly"):
    summary, records, d = run_raw(steps, mode)
    if isinstance(summary, Exception):
        raise AssertionError(f"run_agent raised {summary!r}")
    return summary, records, d


def asks(records):
    return [(r["ask"]["subtype"], r["ask"]["tool_name"]) for r in records if "ask" in r]


def flag(records, name):
    argv = next((r["argv"] for r in records if "argv" in r), [])
    return argv[argv.index(name) + 1] if name in argv else None


def verdict(tool_name, tool_input, mode="readonly"):
    return decide(tool_name, tool_input, P, mode) or {}


def denial(tool_name, tool_input, mode="readonly"):
    v = verdict(tool_name, tool_input, mode)
    return (v.get("behavior"), v.get("message"), v.get("interrupt"))


def test_m1_every_tool_call_goes_through_the_permission_callback_and_the_run_is_summarised():
    summary, records, _ = run([tool("t1", "Read", file_path="a.txt"), tool("t2", "Bash", command="pytest -q"), *finish()])
    assert summary == {"status": "done", "text": "All done.", "tools": ["Read", "Bash"], "turns": 3, "cost": 0.02, "denied": 0}
    assert asks(records) == [("can_use_tool", "Read"), ("hook_callback", None), ("can_use_tool", "Bash")]


def test_e1_the_options_reach_the_cli_as_flags_and_the_run_starts_in_the_project():
    summary, records, d = run(finish(), "readonly")
    assert flag(records, "--tools") == "Read,Grep,Glob,Bash" and flag(records, "--disallowedTools") == "Bash(rm *)"
    assert flag(records, "--max-turns") == "6" and flag(records, "--max-budget-usd") == "0.5" and flag(records, "--permission-mode") == "default"
    assert flag(records, "--allowedTools") in (None, "")
    argv = next((r["argv"] for r in records if "argv" in r), [])
    assert "--setting-sources=" in argv
    cwd = next((r["cwd"] for r in records if "cwd" in r), "")
    assert cwd and os.path.realpath(cwd) == os.path.realpath(d)
    _, edit_records, _ = run(finish(), "edit")
    assert flag(edit_records, "--tools") == "Read,Grep,Glob,Bash,Edit,Write"
    options = build_options(P, FAKE)
    assert options is not None and options.cli_path == FAKE and options.max_turns == 6


def test_e2_file_tools_stay_inside_the_project_and_away_from_secrets():
    assert verdict("Read", {"file_path": "src/a.py"}).get("behavior") == "allow" and verdict("Read", {"file_path": "/proj/src/a.py"}).get("behavior") == "allow"
    assert denial("Read", {"file_path": "../secret.txt"}) == ("deny", "../secret.txt is outside the project", False)
    assert denial("Read", {"file_path": "/etc/passwd"}) == ("deny", "/etc/passwd is outside the project", False)
    assert denial("Read", {"file_path": "src/../../x"})[0] == "deny" and denial("Read", {"file_path": "/proj-other/x"})[0] == "deny"
    assert denial("Read", {"file_path": ".env"}) == ("deny", ".env holds secrets and is never read", False)
    assert denial("Read", {"file_path": "config/.env.local"})[1] == ".env.local holds secrets and is never read"
    assert verdict("Read", {"file_path": ".env.example"}).get("behavior") == "allow"
    assert verdict("Grep", {"pattern": "x"}).get("behavior") == "allow" and verdict("Glob", {"path": "src", "pattern": "*.py"}).get("behavior") == "allow"
    assert denial("Grep", {"pattern": "x", "path": "/other"})[0] == "deny"
    assert denial("Write", {"file_path": "src/a.py"}) == ("deny", "Edits are not allowed in readonly mode", False)
    assert verdict("Write", {"file_path": "src/a.py"}, "edit").get("behavior") == "allow" and verdict("Edit", {"file_path": "src/a.py"}, "edit").get("behavior") == "allow"
    assert denial("Write", {"file_path": ".git/config"}, "edit") == ("deny", ".git/config is inside .git", False)
    assert denial("Write", {"file_path": "src/.git/hooks/x"}, "edit")[0] == "deny" and denial("Write", {"file_path": "../x"}, "edit")[0] == "deny"
    assert denial("Edit", {"file_path": ".env"}, "edit")[0] == "deny"
    assert denial("WebFetch", {"url": "https://example.invalid"}) == ("deny", "WebFetch is not allowed", False)


def test_e3_bash_is_limited_to_a_few_commands_and_dangerous_ones_stop_the_run():
    for ok in ("ls -la", "cat README.md", "pytest -q tests/", "  ls"):
        assert verdict("Bash", {"command": ok}).get("behavior") == "allow", ok
    assert denial("Bash", {"command": "python x.py"}) == ("deny", "Command not allowed: only ls, cat and pytest", False)
    assert denial("Bash", {"command": ""})[0] == "deny"
    for chained in ("ls; rm x", "cat a > b", "ls $(whoami)", "ls | wc", "pytest && ls", "cat `x`"):
        assert denial("Bash", {"command": chained}) == ("deny", "Command not allowed: no chaining or redirection", False), chained
    for dangerous in ("sudo ls", "rm -rf /tmp/x", "pytest && sudo reboot"):
        assert denial("Bash", {"command": dangerous}) == ("deny", "Dangerous command", True), dangerous
    callback = make_can_use_tool(P)
    allowed = asyncio.run(callback("Bash", {"command": "ls"}, None)) if callback else None
    assert isinstance(allowed, PermissionResultAllow) and allowed.updated_input == {"command": "ls"}
    stopped = asyncio.run(callback("Bash", {"command": "sudo ls"}, None)) if callback else None
    assert isinstance(stopped, PermissionResultDeny) and (stopped.message, stopped.interrupt) == ("Dangerous command", True)


def test_e4_a_hook_blocks_a_push_before_the_permission_callback_is_asked():
    guard = lambda command: asyncio.run(bash_guard({"tool_input": {"command": command}}, "t", {}))  # noqa: E731
    deny = {"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "Nothing is pushed from an agent"}}
    for command in ("git push", "git  push --force", "echo done && git push origin main"):
        assert guard(command) == deny, command
    for command in ("git status", "git pushd", "legit push", "ls"):
        assert guard(command) == {}, command
    assert asyncio.run(bash_guard({}, "t", {})) == {}
    summary, records, _ = run([tool("t1", "Bash", command="git push origin main"), tool("t2", "Bash", command="pytest"), *finish()])
    assert asks(records) == [("hook_callback", None), ("hook_callback", None), ("can_use_tool", "Bash")]
    assert summary.get("denied") == 1 and summary.get("tools") == ["Bash", "Bash"]


def test_e5_the_messages_of_a_run_fold_into_a_summary():
    def result(subtype="success", text="final", cost=0.5, turns=4):
        return ResultMessage(subtype=subtype, duration_ms=1, duration_api_ms=1, is_error=subtype != "success", num_turns=turns, session_id="s", result=text, total_cost_usd=cost)

    said = AssistantMessage(content=[TextBlock("first words"), ToolUseBlock("t1", "Read", {}), TextBlock("last words")], model="m")
    more = AssistantMessage(content=[ToolUseBlock("t2", "Bash", {})], model="m")
    errors = UserMessage(content=[ToolResultBlock("t1", "no", True), ToolResultBlock("t2", "yes", False), ToolResultBlock("t3", "n", None)])
    assert summarize([said, errors, more, result()]) == {"status": "done", "text": "final", "tools": ["Read", "Bash"], "turns": 4, "cost": 0.5, "denied": 1}
    assert (summarize([said, result(text=None)]) or {}).get("text") == "last words"
    assert (summarize([said, result(text="")]) or {}).get("text") == "last words"
    assert (summarize([UserMessage(content="plain text"), result()]) or {}).get("denied") == 0
    for subtype, status in (("error_max_turns", "max_turns"), ("error_max_budget_usd", "budget"), ("error_during_execution", "failed"), ("error_max_structured_output_retries", "error_max_structured_output_retries")):
        assert (summarize([result(subtype=subtype)]) or {}).get("status") == status
    assert (summarize([result(cost=None)]) or {}).get("cost") == 0.0
    assert summarize([said, errors]) == {"status": "incomplete", "text": "last words", "tools": ["Read"], "turns": 0, "cost": 0.0, "denied": 1}


def test_e6_an_error_result_still_gives_its_summary_and_a_crash_is_not_hidden():
    summary, records, _ = run([tool(f"t{i}", "Read", file_path="a.txt") for i in range(8)] + finish())  # the CLI stops at the turn limit and exits with code 1
    assert (summary.get("status"), summary.get("turns"), len(summary.get("tools", []))) == ("max_turns", 6, 5)
    assert flag(records, "--max-turns") == "6"
    budget, _, _ = run([tool("t1", "Read", file_path="a.txt"), *finish("Stopped early.", "error_max_budget_usd", 0.5, 2)])
    assert (budget.get("status"), budget.get("cost"), budget.get("text"), budget.get("tools")) == ("budget", 0.5, "Stopped early.", ["Read"])
    crashed, _, _ = run_raw([tool("t1", "Read", file_path="a.txt"), {"exit": 2}])  # the process dies before it sends any result
    assert isinstance(crashed, Exception)


def test_e7_denied_calls_are_counted_and_the_run_still_ends_with_a_result():
    summary, records, _ = run([tool("t1", "Write", file_path="src/a.py", content="x"), tool("t2", "Bash", command="ls; rm x"), tool("t3", "Read", file_path="../secret.txt"),
                               tool("t4", "Read", file_path="src/a.py"), *finish()])
    assert (summary.get("status"), summary.get("denied"), summary.get("tools")) == ("done", 3, ["Write", "Bash", "Read", "Read"])
    assert asks(records) == [("hook_callback", None), ("can_use_tool", "Bash"), ("can_use_tool", "Read"), ("can_use_tool", "Read")]
