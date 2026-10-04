import json
import os
import re
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds .mcp.json, .claude/ and docs/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))

AGENT_FILES = [".claude/agents/explorer.md", ".claude/agents/scaffolder.md"]
SECRET_KEY = re.compile(r"token|key|secret|authorization", re.I)


def read(rel):
    path = ROOT / rel
    assert path.is_file(), f"{rel} is missing"
    return path.read_text()


def load_json(rel):
    try:
        return json.loads(read(rel))
    except json.JSONDecodeError as error:
        raise AssertionError(f"{rel} is not valid JSON: {error}")


def front_matter(text):
    head = re.match(r"---\n(.*?)\n---\n", text, re.S)
    return (head.group(1), text[head.end():]) if head else ("", text)


def agent(rel):
    head, _ = front_matter(read(rel))
    description = re.search(r"^description:\s*(.*)$", head, re.M)
    tools = re.search(r"^tools:\s*(.*)$", head, re.M)
    return (description.group(1).strip() if description else ""), ([t.strip() for t in tools.group(1).split(",")] if tools else None)


def servers():
    return load_json(".mcp.json").get("mcpServers", {})


def rules(kind):
    return load_json(".claude/settings.json").get("permissions", {}).get(kind, [])


def test_m1_every_mcp_tool_reference_belongs_to_a_configured_server():
    configured = servers()
    assert configured, "configure at least one server in .mcp.json"
    refs = [t for rel in AGENT_FILES for t in (agent(rel)[1] or []) if t.startswith("mcp__")]
    refs += [r for kind in ("allow", "deny") for r in rules(kind) if r.startswith("mcp__")]
    assert refs, "the setup refers to at least one MCP tool"
    for ref in refs:
        assert ref.split("__")[1] in configured, f"{ref} names a server that .mcp.json does not configure"


def test_e1_credentials_come_from_the_environment_and_the_token_has_no_default():
    configured = servers()
    for name, server in configured.items():
        for field in ("headers", "env"):
            for key, value in server.get(field, {}).items():
                assert not SECRET_KEY.search(key) or "${" in value, f"{name} {field}.{key} holds a literal credential"
    auth = configured.get("tickets", {}).get("headers", {}).get("Authorization", "")
    assert re.search(r"\$\{[A-Z_][A-Z0-9_]*\}", auth), "the tickets token is a ${VAR} reference"
    assert ":-" not in auth, "a default for a token would be a credential in the file"


def test_e2_the_explorer_is_read_only_and_says_when_to_use_it():
    description, tools = agent(".claude/agents/explorer.md")
    assert description.startswith("Use when"), "the description says when to delegate"
    assert tools is not None, "list the tools: without a tools line the subagent inherits every tool"
    assert "Read" in tools and "Grep" in tools, "an explorer reads and searches"
    assert not [t for t in tools if t not in ("Read", "Grep", "Glob", "mcp__docs__search")], f"read-only tools only, found {tools}"


def test_e3_the_scaffolder_writes_only_in_the_generated_folder():
    _, tools = agent(".claude/agents/scaffolder.md")
    assert tools is not None, "list the tools: without a tools line the subagent inherits every tool"
    assert "Edit" in tools and "Bash" not in tools, "the scaffolder edits files and runs no commands"
    writes = [r for r in rules("allow") if re.match(r"(Edit|Write|MultiEdit)\b", r)]
    assert writes, "allow the scaffolder to edit the generated folder"
    for rule in writes:
        assert re.fullmatch(r"Edit\(src/generated/[^)]*\)", rule), f"{rule} is not an Edit rule limited to the generated folder (a Write path rule is never consulted)"


def test_e4_the_tickets_server_is_read_only_for_agents():
    deny = rules("deny")
    for tool in ("mcp__tickets__create_ticket", "mcp__tickets__delete_ticket"):
        assert tool in deny, f"deny {tool}"
    for rule in rules("allow"):
        if rule.startswith("mcp__tickets"):
            assert re.fullmatch(r"mcp__tickets__(get|list|search)_\w+", rule), f"{rule} approves more than reading tickets"


def test_e5_the_environment_file_is_denied_and_no_whole_tool_is_approved():
    assert "Read(./.env)" in rules("deny"), "deny reading the environment file"
    whole = [r for r in rules("allow") if r in ("Bash", "Bash(*)", "Edit", "Write", "Read", "mcp__tickets")]
    assert not whole, f"a bare allow rule approves every call of that tool: {whole}"


def test_e6_the_team_note_lists_every_variable_and_says_which_session_to_start():
    note = read("docs/team-setup.md")
    for var in sorted(set(re.findall(r"\$\{([A-Za-z_]\w*)", read(".mcp.json")))):
        assert re.search(rf"\b{var}\b", note), f"the note does not list {var}"
    rows = {}
    for line in note.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if line.startswith("|") and len(cells) >= 2 and cells[1].lower() in ("resume", "fork", "fresh"):
            rows[cells[0].lower()] = cells[1].lower()
    for keyword, mode in (("yesterday", "resume"), ("compare", "fork"), ("rewritten", "fresh")):
        situation = next((t for t in rows if keyword in t), None)
        assert situation, f"the table has no row for the {keyword!r} situation"
        assert rows[situation] == mode, f"{keyword!r} should be {mode}"


def test_e7_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"), ("key", r"sk-ant-[\w-]{6,}")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
