import json
import os
import re
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds .claude/ and docs/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))
HERE = Path(__file__).resolve()
for name in ("38-settings-layers/python", "39-hook-gate/python", "56-builtin-tools/python"):
    sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from miniyaml import split_frontmatter  # noqa: E402
from builtin_tools import rule_tool, tool_set  # noqa: E402
from settings_layers import decide  # noqa: E402


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def read_json(rel):
    try:
        data = json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")
    assert isinstance(data, dict), f"{rel} must hold a JSON object"
    return data


def settings():
    return read_json(".claude/settings.json")


def check(table):
    s = settings()
    got = {k: decide(s, rule_tool(k[0]), k[1]) for k in table}
    assert got == table, {k: (got[k], v) for k, v in table.items() if got[k] != v}


def sections(text):
    head, _, tail = text.partition("## When an edit does not apply")
    steps = lambda block: [m.group(1) for m in re.finditer(r"(?m)^\d+\. (.*)$", block)]
    return head, steps(head), steps(tail)


def test_m1_the_permission_rules_let_an_explorer_read_search_and_take_notes_but_not_change_the_source():
    check({("Read", "src/inventory/stock.py"): "allow", ("Grep", "src/inventory"): "allow", ("Glob", "src/**/handlers/*.py"): "allow",
           ("Edit", "src/inventory/stock.py"): "deny", ("Write", "src/inventory/new.py"): "deny", ("Edit", "notes/findings.md"): "allow",
           ("Write", "notes/findings.md"): "allow", ("Edit", "docs/readme.md"): "ask", ("Bash", "git log --oneline"): "allow",
           ("Bash", "git diff HEAD~1"): "allow", ("Bash", "git status"): "allow", ("Bash", "rm -rf build"): "ask", ("Bash", "git push origin main"): "ask"})


def test_e1_a_read_rule_protects_secrets_from_reading_searching_and_writing():
    check({("Read", "./.env"): "deny", ("Read", "secrets/prod.key"): "deny", ("Grep", "secrets/prod.key"): "deny", ("Glob", "secrets/prod.key"): "deny",
           ("Edit", ".env"): "deny", ("Write", "secrets/new.key"): "deny", ("Edit", "secrets/prod.key"): "deny", ("Read", "vendor/secrets/a.txt"): "deny"})


def test_e2_only_rule_forms_that_are_consulted_are_used_and_no_whole_tool_that_changes_things_is_allowed():
    perms = settings().get("permissions", {})
    rules = [r for kind in ("allow", "ask", "deny") for r in perms.get(kind, [])]
    assert rules, "write the permission rules"
    assert not [r for r in rules if re.match(r"(Write|NotebookEdit|MultiEdit)\(", r)], "path rules for Write are never matched: write them as Edit(...)"
    assert not [r for r in rules if re.match(r"(Grep|Glob)\(", r)], "path rules for the search tools are written as Read(...)"
    assert not [r for r in perms.get("allow", []) if r in ("Bash", "Bash(*)", "Edit", "Write")], "a bare allow rule approves every call of that tool"
    assert set(perms) <= {"allow", "ask", "deny"}


def test_e3_the_explorer_agent_reads_and_searches_and_says_when_to_use_it():
    fm, body = split_frontmatter(read(".claude/agents/explorer.md"))
    assert fm.get("name") == "explorer"
    tools = [t.strip() for t in str(fm.get("tools", "")).split(",") if t.strip()]
    assert sorted(tools) == ["Glob", "Grep", "Read"], "an explorer lists Read, Grep and Glob and nothing that edits or runs commands"
    assert re.search(r"(?:^|\. )Use when\b", str(fm.get("description") or "")), "the description starts a sentence with Use when"
    assert isinstance(fm.get("maxTurns"), int) and 1 <= fm["maxTurns"] <= 15 and fm.get("model"), "bound the turns and choose a model"
    assert body.strip(), "the body holds the agent's instructions"


def test_e4_the_sdk_options_make_the_search_tools_available_and_remove_the_tools_that_change_things():
    o = read_json("agent-options.json")
    tools, allowed, disallowed = o.get("tools"), o.get("allowedTools"), o.get("disallowedTools")
    assert sorted(tools or []) == ["Glob", "Grep", "Read"], "tools lists the three the agent may have; naming Grep and Glob puts them back on macOS, Linux and WSL"
    assert tool_set("linux", tools=tools, allowed_tools=allowed or [], disallowed_tools=disallowed or []) == ["Read", "Grep", "Glob"]
    assert allowed is not None and set(allowed) <= set(tools), "allowedTools pre-approves only listed tools"
    assert "Bash" not in allowed and "Edit" not in allowed and "Write" not in allowed
    assert {"Bash", "Edit", "Write"} <= set(disallowed or []), "a bare name in disallowedTools removes the tool from the model's context"


def test_e5_the_exploration_plan_starts_with_a_search_then_reads_and_never_reads_everything_first():
    head, steps, _ = sections(read("docs/exploration-plan.md"))
    assert re.search(r"(?i)do not read every file", head), "say that the whole repository is not read first"
    assert len(steps) >= 4, "write the steps as a numbered list"
    grep = next((i for i, s in enumerate(steps) if re.search(r"\bGrep\b", s)), None)
    read_ = next((i for i, s in enumerate(steps) if re.search(r"\bRead\b", s)), None)
    assert grep is not None and read_ is not None and grep < read_, "search for entry points before reading anything"
    assert any(re.search(r"\bGlob\b", s) and re.search(r"`[^`]*(\*\*/|\*\.)[^`]*`", s) for s in steps), "use Glob with a name pattern"
    assert any(i > read_ and re.search(r"export", s) and re.search(r"\beach\b", s) and re.search(r"\bGrep\b", s) for i, s in enumerate(steps)), \
        "to trace usage through wrappers, list the exported names and search for each"
    assert any("notes/" in s for s in steps), "findings go to notes/, the only place the agent may write"


def test_e6_the_edit_fallback_widens_the_anchor_then_replaces_all_then_rewrites_the_file():
    _, _, fallback = sections(read("docs/exploration-plan.md"))
    assert len(fallback) >= 3, "write the three remedies as a numbered list under 'When an edit does not apply'"
    assert re.search(r"surrounding|more context|longer", fallback[0]) and "unique" in fallback[0]
    assert "replace_all" in fallback[1]
    assert re.search(r"\bRead\b", fallback[2]) and re.search(r"\bWrite\b", fallback[2]) and re.search(r"(?i)whole file", fallback[2])


def test_e7_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
