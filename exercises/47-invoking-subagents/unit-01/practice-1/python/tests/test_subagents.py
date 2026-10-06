import asyncio
import json
import os
import shutil
import sys
import tempfile
from pathlib import Path

from claude_agent_sdk import AssistantMessage, TextBlock, ToolResultBlock, ToolUseBlock, UserMessage

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from subagents import build_options, by_subagent, make_brief, merge_findings, package_finding, run_team, spawned

HERE = Path(__file__).resolve()
_found = next(str(p) for p in (Path("/w/harness/fake_claude.py"), *(HERE.parents[i] / "harness" / "fake_claude.py" for i in range(min(len(HERE.parents), 8)))) if p.exists())
# The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
FAKE = shutil.copy(_found, Path(tempfile.mkdtemp(), "fake_claude.py"))
os.chmod(FAKE, 0o755)

SPECS = {
    "reviewer": {"description": "Reviews one module for security problems. Use for any review request.", "prompt": "You review code.", "tools": ["Read", "Grep"], "model": "sonnet"},
    "finder": {"description": "Finds the files that touch a feature.", "prompt": "You find files."},
}


def options(specs=SPECS, **extra):
    built = build_options(specs, "/proj", **extra)
    assert built is not None, "build_options returned None"
    return built


def refused(specs):
    try:
        build_options(specs, "/proj")
    except ValueError:
        return True
    return False


def call(id, name, parent=None, **tool_input):
    return AssistantMessage(content=[ToolUseBlock(id=id, name=name, input=tool_input)], model="m", parent_tool_use_id=parent)


def returned(id, text, parent=None):
    return UserMessage(content=[ToolResultBlock(tool_use_id=id, content=text)], parent_tool_use_id=parent)


def test_m1_the_options_register_each_named_agent_with_its_description_prompt_tools_and_model():
    o = options()
    assert sorted(o.agents) == ["finder", "reviewer"]
    r = o.agents["reviewer"]
    assert (r.description, r.prompt, r.tools, r.model) == (SPECS["reviewer"]["description"], "You review code.", ["Read", "Grep"], "sonnet")
    assert o.allowed_tools[0] == "Agent" and o.cwd == "/proj" and o.permission_mode == "default" and o.setting_sources == []


def test_e1_a_subagent_gets_read_only_tools_by_default_and_never_the_right_to_spawn_another():
    o = options()
    assert o.agents["finder"].tools == ["Read", "Grep", "Glob"] and o.agents["finder"].model == "inherit"
    nested = options({"boss": {"description": "Delegates work.", "prompt": "p", "tools": ["Read", "Agent", "Bash"]}})
    assert nested.agents["boss"].tools == ["Read", "Bash"]
    assert options({"mute": {"description": "Says nothing.", "prompt": "p", "tools": []}}).agents["mute"].tools == []


def test_e2_a_definition_with_a_bad_name_or_no_description_or_no_prompt_is_refused():
    base = {"description": "Does a job.", "prompt": "p"}
    assert refused({"Bad Name": base}) and refused({"under_score": base}) and refused({"9lives": base})
    assert refused({"ok": {**base, "description": "  "}}) and refused({"ok": {"prompt": "p"}}) and refused({"ok": {**base, "prompt": ""}})
    assert not refused({"ok-name-2": base})


def test_e3_depth_concurrency_budget_and_turn_limits_are_set_on_the_options():
    o = options(max_budget_usd=1.5, max_turns=9, max_concurrent=3, cli_path="/x/claude")
    assert o.env == {"CLAUDE_CODE_MAX_SUBAGENT_SPAWN_DEPTH": "1", "CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS": "3"}
    assert (o.max_budget_usd, o.max_turns, o.cli_path) == (1.5, 9, "/x/claude")
    d = options()
    assert (d.max_budget_usd, d.max_turns, d.env["CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS"]) == (2.0, 20, "5")


def test_e4_a_spawn_is_recognised_under_the_new_and_the_old_tool_name_and_nothing_else_is():
    messages = [call("a", "Agent", subagent_type="reviewer", description="review auth", prompt="Review auth.py"), call("b", "Task", description="old name", prompt="Look"),
                call("c", "Read", file_path="x"), AssistantMessage(content=[TextBlock(text="hi")], model="m"), returned("a", "done")]
    found = spawned(messages)
    assert found == [{"id": "a", "subagent_type": "reviewer", "description": "review auth", "prompt": "Review auth.py"},
                     {"id": "b", "subagent_type": "general-purpose", "description": "old name", "prompt": "Look"}]


def test_e5_messages_from_inside_a_subagent_are_grouped_under_the_call_that_started_it():
    messages = [call("a", "Agent", subagent_type="reviewer", description="d", prompt="p"), call("r1", "Read", parent="a", file_path="x"), returned("r1", "code", parent="a"),
                call("r2", "Grep", parent="a", pattern="y"), call("r3", "Read", parent="a", file_path="z"), returned("a", "report"),
                call("b", "Agent", subagent_type="finder", description="d", prompt="p"), call("r4", "Glob", parent="b", pattern="*")]
    assert by_subagent(messages) == {"a": {"subagent_type": "reviewer", "messages": 4, "tools": ["Read", "Grep"]}, "b": {"subagent_type": "finder", "messages": 1, "tools": ["Glob"]}}


def test_e6_a_brief_carries_the_task_and_every_fact_the_subagent_needs_in_a_fixed_layout():
    full = make_brief("  Fix the broken check ", files=["src/auth.py", " ", "tests/test_auth.py"], facts=["the error is KeyError: 'token'", "do not touch vendor/"], output=" a diff and one sentence ")
    assert full == "Task: Fix the broken check\nFiles:\n- src/auth.py\n- tests/test_auth.py\nKnown:\n- the error is KeyError: 'token'\n- do not touch vendor/\nReturn: a diff and one sentence"
    assert make_brief("Find usages") == "Task: Find usages"
    raised = False
    try:
        make_brief("   ")
    except ValueError:
        raised = True
    assert raised


def test_e7_findings_keep_the_claim_apart_from_its_source_and_merging_keeps_every_source():
    a = package_finding(" Rates held in 2024 ", url="https://example.com/a", title="Bank statement", page=3)
    b = package_finding("rates  held in 2024", url="https://example.com/b")
    c = package_finding("Loans stayed dear")
    assert a == {"claim": "Rates held in 2024", "source": {"url": "https://example.com/a", "title": "Bank statement", "page": 3}} and c == {"claim": "Loans stayed dear", "source": None}
    assert merge_findings([a, b, c, a]) == [
        {"claim": "Rates held in 2024", "sources": [{"url": "https://example.com/a", "title": "Bank statement", "page": 3}, {"url": "https://example.com/b"}], "attributed": True},
        {"claim": "Loans stayed dear", "sources": [], "attributed": False}]


def test_e8_a_run_through_the_sdk_shows_the_spawn_the_inner_messages_and_the_agents_sent_to_the_binary():
    d = tempfile.mkdtemp()
    steps = [{"say": "I will delegate."},
             {"tool": {"id": "t1", "name": "Agent", "input": {"subagent_type": "reviewer", "description": "review auth", "prompt": "Review auth.py"}, "output": "Two issues found."}},
             {"tool": {"id": "t2", "name": "Read", "input": {"file_path": "auth.py"}, "output": "code", "parent": "t1"}},
             {"say": "Two issues, see the report."}, {"result": {"subtype": "success", "result": "Two issues, see the report.", "cost": 0.03, "turns": 2}}]
    script, record = Path(d, "script.json"), Path(d, "record.jsonl")
    script.write_text(json.dumps({"session_id": "s1", "turns": [steps]}))
    os.environ["FAKE_CLAUDE_SCRIPT"], os.environ["FAKE_CLAUDE_RECORD"] = str(script), str(record)
    run = asyncio.run(run_team("Review auth.py", build_options(SPECS, d, cli_path=FAKE, max_budget_usd=0.5))) or {}
    messages = run.get("messages", [])
    assert run.get("error") is None and len(messages) > 0
    assert [(c["id"], c["subagent_type"]) for c in spawned(messages) or []] == [("t1", "reviewer")]
    assert (by_subagent(messages) or {}).get("t1") == {"subagent_type": "reviewer", "messages": 2, "tools": ["Read"]}
    lines = [json.loads(line) for line in record.read_text().splitlines()]
    agents = next(line["agents"] for line in lines if "agents" in line)
    assert sorted(agents) == ["finder", "reviewer"] and agents["reviewer"].get("tools") == ["Read", "Grep"] and agents["finder"].get("tools") == ["Read", "Grep", "Glob"]
    argv = next(line["argv"] for line in lines if "argv" in line)
    assert "--max-budget-usd" in argv and argv[argv.index("--max-budget-usd") + 1] == "0.5"
