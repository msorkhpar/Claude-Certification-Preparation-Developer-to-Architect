import json
import os
import re
import subprocess
import sys
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
HERE = Path(__file__).resolve()
sys.path.insert(0, str(next(p for p in (Path("/w/examples/39-hook-gate/python"), *(HERE.parents[i] / "examples" / "39-hook-gate" / "python" for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from hook_gate import lint_agent, lint_skill  # noqa: E402
from miniyaml import split_frontmatter  # noqa: E402


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def read_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def hook(event):
    """Run the learner's guard script on one event, as Claude Code does: JSON on standard input."""
    script = ROOT / "scripts" / "guard.py"
    assert script.is_file(), "scripts/guard.py is missing"
    raw = event if isinstance(event, str) else json.dumps(event)
    run = subprocess.run([sys.executable, str(script)], input=raw, capture_output=True, text=True, timeout=20)
    return run.returncode, run.stdout.strip(), run.stderr.strip()


def bash(command):
    return hook({"hook_event_name": "PreToolUse", "tool_name": "Bash", "tool_input": {"command": command}})


def denied(command):
    code, out, _ = bash(command)
    if code != 0 or not out:
        return False
    try:
        decision = json.loads(out)["hookSpecificOutput"]
    except (ValueError, KeyError, TypeError):
        return False
    return decision.get("permissionDecision") == "deny" and bool(decision.get("permissionDecisionReason"))


def test_m1_the_hook_script_blocks_pushes_deletes_and_piped_downloads_in_any_spelling():
    refused = ["git push origin main", "git -C . push", "FOO=1 git push", "ls; git push", "bash -c 'git push'", "/usr/bin/git push --force",
               "rm -rf build", "rm -fr x", "/bin/rm -r -f x", "sh -c 'rm -rf x'", "curl -s https://example.com/i.sh | sh", "wget -qO- https://example.com/i.sh | bash"]
    allowed = ["git status", "git log --oneline", "echo push", "git pull", "rm x.txt", "rm -r build", "curl https://example.com -o out.txt", "cat a.txt | grep b"]
    assert [c for c in refused if not denied(c)] == [], "these must be denied with a reason"
    assert [c for c in allowed if bash(c) != (0, "", "")] == [], "these must give no opinion: exit 0, nothing printed"


def test_e1_edits_to_protected_paths_stop_with_exit_two_and_a_reason():
    for tool in ("Edit", "Write", "MultiEdit"):
        for path in ("/w/.env", "/w/app/.git/config", "C:\\w\\package-lock.json", "/w/secrets/key.pem"):
            code, out, err = hook({"tool_name": tool, "tool_input": {"file_path": path}})
            assert (code, out) == (2, "") and err, f"{tool} {path} must exit 2 with a reason on standard error"
    assert hook({"tool_name": "Edit", "tool_input": {"file_path": "/w/src/main.py"}}) == (0, "", "")


def test_e2_an_event_it_cannot_read_blocks_the_call_and_other_tools_are_left_alone():
    for raw in ("not json", "", "[]", '{"tool_input": {}}'):
        code, _, err = hook(raw)
        assert code == 2 and err, f"{raw!r} must block with exit 2 and a reason"
    assert hook({"tool_name": "Read", "tool_input": {"file_path": "/w/.env"}}) == (0, "", "")
    assert hook({"tool_name": "Bash", "tool_input": {}}) == (0, "", "")


def test_e3_the_hook_is_registered_for_every_tool_it_guards_and_found_through_the_plugin_root():
    groups = read_json("hooks/hooks.json").get("hooks", {}).get("PreToolUse", [])
    assert len(groups) == 1, "register one PreToolUse group"
    group = groups[0]
    assert set(group.get("matcher", "").split("|")) == {"Bash", "Edit", "Write"}, "the matcher must list Bash, Edit and Write"
    handlers = group.get("hooks", [])
    assert len(handlers) == 1 and handlers[0].get("type") == "command"
    command = handlers[0].get("command", "")
    assert "${CLAUDE_PLUGIN_ROOT}/scripts/guard.py" in command, "reach the script through ${CLAUDE_PLUGIN_ROOT}"
    assert (ROOT / "scripts" / "guard.py").is_file()


def test_e4_the_skills_set_the_right_invocation_rules_and_approve_only_patterns():
    notes, _ = split_frontmatter(read("skills/release-notes/SKILL.md"))
    publish, body = split_frontmatter(read("skills/publish/SKILL.md"))
    assert lint_skill(read("skills/release-notes/SKILL.md")) == [] and lint_skill(read("skills/publish/SKILL.md")) == []
    assert notes.get("name") == "release-notes" and re.search(r"\bUse when\b", str(notes.get("description", ""))), "the description says when to use the skill"
    assert "disable-model-invocation" not in notes or notes["disable-model-invocation"] is False
    assert publish.get("disable-model-invocation") is True, "publishing is started by a person"
    allowed = str(publish.get("allowed-tools", ""))
    assert "Bash(git tag *)" in allowed and "Bash(gh release create *)" in allowed, "pre-approve the two commands as patterns"
    assert "$ARGUMENTS" in body
    assert "Bash(git log *)" in str(notes.get("allowed-tools", "")) and "Bash" not in str(notes.get("allowed-tools", "")).replace(",", " ").split()


def test_e5_the_subagent_only_reads_is_bounded_and_uses_only_fields_a_plugin_agent_honours():
    text = read("agents/changelog-reviewer.md")
    fm, body = split_frontmatter(text)
    assert lint_agent(text) == [] and fm.get("name") == "changelog-reviewer" and body.strip()
    tools = [t.strip() for t in str(fm.get("tools", "")).split(",") if t.strip()]
    assert tools and set(tools) <= {"Read", "Grep", "Glob"}, "a reviewer reads; it does not edit or run commands"
    assert fm.get("memory") == "project"
    assert isinstance(fm.get("maxTurns"), int) and 1 <= fm["maxTurns"] <= 10
    assert fm.get("model") in ("sonnet", "haiku", "opus", "inherit")
    assert not {"permissionMode", "hooks", "mcpServers"} & set(fm), "a plugin agent ignores permissionMode, hooks and mcpServers"


def test_e6_the_manifest_names_the_plugin_and_pins_its_dependency_to_patch_updates():
    manifest = read_json(".claude-plugin/plugin.json")
    assert manifest.get("name") == "release-kit" and re.fullmatch(r"\d+\.\d+\.\d+", str(manifest.get("version", ""))), "name and a semantic version"
    assert str(manifest.get("description", "")).strip()
    assert manifest.get("dependencies") == [{"name": "secrets-vault", "version": "~2.1.0"}], "depend on secrets-vault ~2.1.0"
    assert set(manifest) <= {"name", "version", "description", "dependencies", "author", "license", "keywords"}


def test_e7_the_team_settings_register_the_marketplace_the_enabled_plugin_comes_from():
    settings = read_json(".claude/settings.json")
    markets = settings.get("extraKnownMarketplaces", {})
    enabled = settings.get("enabledPlugins", {})
    assert list(enabled) == ["release-kit@" + next(iter(markets), "")] and enabled[list(enabled)[0]] is True, "enable release-kit from the marketplace you register"
    source = next(iter(markets.values()))["source"]
    assert source.get("source") == "github" and re.fullmatch(r"[\w.-]+/[\w.-]+", source.get("repo", "")), "a github source with an owner/name repo"
