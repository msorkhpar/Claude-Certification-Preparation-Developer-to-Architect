import json
import os
import re
import sys
from pathlib import Path

# The solution is the file beside this test.
ROOT = Path(str(Path(__file__).resolve().parent.parent / "project"))
HERE = Path(__file__).resolve()
for name in ("38-settings-layers/python", "39-hook-gate/python"):
    sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from miniyaml import split_frontmatter  # noqa: E402
from settings_layers import decide, effective_settings, load_memory  # noqa: E402

MANAGED = {"permissions": {"deny": ["Bash(sudo *)"]}}  # a fictional organisation policy
USER = {"model": "haiku", "permissions": {"allow": ["Bash(ls *)"]}}


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


def layers(with_local=True):
    out = {"managed": MANAGED, "user": USER, "project": read_json(".claude/settings.json")}
    if with_local and (ROOT / ".claude/settings.local.json").is_file():
        out["local"] = read_json(".claude/settings.local.json")
    return out


def test_m1_the_memory_file_is_short_concrete_and_pulls_in_the_architecture_notes():
    text = read("CLAUDE.md")
    assert len(text.splitlines()) <= 200, "keep CLAUDE.md under 200 lines"
    assert "`make test`" in text and "`make lint`" in text, "name the test and lint commands in backticks"
    assert re.search(r"(?m)^[^`\n]*@docs/architecture\.md", text), "import docs/architecture.md with an @ line"
    emphasis = [line for line in text.splitlines() if re.search(r"\b(IMPORTANT|MUST|NEVER|ALWAYS)\b", line)]
    assert len(emphasis) <= 2, "emphasis on many lines makes none of them stand out"
    files = {"/p/CLAUDE.md": text, "/p/docs/architecture.md": read("docs/architecture.md")}
    assert load_memory(files, "/p") == ["/p/CLAUDE.md", "/p/docs/architecture.md"], "the import must resolve to the architecture file"


def test_e1_the_permission_rules_allow_the_daily_commands_ask_before_commits_and_deny_secrets_and_pushes():
    s = effective_settings(layers())
    mode = s.get("permissions", {}).get("defaultMode", "default")
    table = {("Bash", "make test"): "allow", ("Bash", "make lint"): "allow", ("Bash", "make deploy"): "ask", ("Bash", "git status"): "allow",
             ("Bash", "git diff HEAD~1"): "allow", ("Bash", "git commit -m 'AB-1 fix'"): "ask", ("Bash", "git push origin main"): "deny",
             ("Bash", "make test && git push origin main"): "deny", ("Bash", "curl https://example.com"): "deny", ("Bash", "sudo make test"): "deny",
             ("Bash", "ls -la"): "allow", ("Read", "./.env"): "deny", ("Read", "secrets/key.pem"): "deny", ("Edit", ".env"): "deny",
             ("Read", "src/api/app.py"): "allow", ("Edit", "src/api/app.py"): "allow"}
    got = {k: decide(s, k[0], k[1], mode) for k in table}
    assert got == table, {k: (got[k], v) for k, v in table.items() if got[k] != v}


def test_e2_the_shared_file_sets_a_mode_it_may_set_and_uses_only_rules_that_are_consulted():
    shared = read_json(".claude/settings.json")
    perms = shared.get("permissions", {})
    assert perms.get("defaultMode") == "acceptEdits", "set defaultMode to acceptEdits; auto and bypassPermissions are ignored in a repository file"
    assert effective_settings({"project": shared})["permissions"].get("defaultMode") == "acceptEdits"
    rules = [r for kind in ("allow", "ask", "deny") for r in perms.get(kind, [])]
    assert not [r for r in rules if re.match(r"(Write|NotebookEdit|MultiEdit)\(", r)], "path rules for Write are never consulted: use Edit or Read"
    assert not [r for r in perms.get("allow", []) if r in ("Bash", "Bash(*)")], "a bare Bash allow rule approves every command"
    assert set(perms) <= {"defaultMode", "allow", "ask", "deny"}


def test_e3_personal_settings_stay_local_and_the_local_file_wins():
    ignore = [line.strip() for line in read(".gitignore").splitlines()]
    assert ".claude/settings.local.json" in ignore and "CLAUDE.local.md" in ignore, "git must ignore both personal files"
    assert read_json(".claude/settings.json").get("model") == "opus", "the team default model is opus"
    assert effective_settings(layers())["model"] == "sonnet", "the local file overrides the team model"
    assert effective_settings(layers(with_local=False))["model"] == "opus"
    local = read_json(".claude/settings.local.json")
    assert "permissions" not in local and "env" not in local, "keep the local file to the model override"


def test_e4_the_custom_command_is_a_skill_that_only_a_person_can_start():
    fm, body = split_frontmatter(read(".claude/skills/fix-issue/SKILL.md"))
    assert fm.get("name") == "fix-issue" and str(fm.get("description") or "").strip(), "name and description are required"
    assert fm.get("disable-model-invocation") is True, "a command that edits code and calls gh is started by a person"
    assert fm.get("argument-hint"), "show the argument in the menu"
    assert "$ARGUMENTS" in body and "`make test`" in body


def test_e5_the_headless_script_is_bounded_and_does_not_skip_permissions():
    script = read("scripts/ci-review.sh")
    flat = re.sub(r"\\\n", " ", script)
    assert re.search(r"\bclaude -p\b", flat) and "--output-format json" in flat and "--bare" in flat
    turns = re.search(r"--max-turns (\d+)", flat)
    assert turns and 1 <= int(turns.group(1)) <= 10, "cap the turns at 10 or fewer"
    assert re.search(r"--max-budget-usd \d", flat), "cap the spend"
    assert re.search(r"--permission-mode (dontAsk|plan)\b", flat), "start from a mode that never prompts and never auto-approves"
    allowed = re.search(r'--allowedTools "([^"]*)"', flat)
    assert allowed, "list the pre-approved tools"
    tools = [t.strip() for t in re.split(r",(?![^()]*\))", allowed.group(1))]
    assert tools and "Bash" not in tools and "Edit" not in tools and "Write" not in tools, "pre-approve patterns, not whole tools that change things"
    assert "dangerously-skip-permissions" not in flat and "bypassPermissions" not in flat


def test_e6_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
