import json
import os
import re
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds .mcp.json and .claude/.
ROOT = Path(os.environ.get("SOLUTION_DIR", Path(__file__).resolve().parent.parent / "starter"))
HERE = Path(__file__).resolve()
name = "55-mcp-config/python"
sys.path.insert(0, str(next(p for p in (Path("/w/examples") / name, *(HERE.parents[i] / "examples" / name for i in range(min(len(HERE.parents), 9)))) if p.exists())))
from mcp_config import COVERED, expand_server, lint, mcp_decision, resolve_servers, truncate  # noqa: E402

SERVERS = ["core", "docs", "github", "schema"]


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


def shared():
    return read_json(".mcp.json").get("mcpServers", {})


def server(name):
    found = shared().get(name)
    assert isinstance(found, dict), f".mcp.json has no server named {name}"
    return found


def test_m1_the_shared_file_declares_the_four_team_servers_with_the_right_shape():
    assert sorted(shared()) == SERVERS, "declare exactly core, docs, github and schema"
    assert server("github").get("type") == "http" and server("core").get("type") == "http", "github and core are remote http servers"
    assert server("docs").get("type", "stdio") == "stdio" and server("schema").get("type", "stdio") == "stdio", "docs and schema run as local processes"
    assert str(server("docs").get("command")) == "python3" and str(server("schema").get("command")) == "python3"
    assert lint({"mcpServers": shared()}) == [], lint({"mcpServers": shared()})


def test_e1_credentials_are_read_from_the_environment_and_never_written_in_the_file():
    assert server("github").get("headers", {}).get("Authorization") == "Bearer ${GITHUB_TOKEN}", "send the token as Bearer ${GITHUB_TOKEN}"
    assert server("docs").get("env", {}).get("DOCS_API_KEY") == "${DOCS_API_KEY}", "pass DOCS_API_KEY through from the environment"
    names = set(re.findall(r"\$\{([A-Za-z_][A-Za-z0-9_]*)(?::-[^}]*)?\}", json.dumps(server("github").get("headers", {}))))
    assert not names & COVERED, "a credential name that Claude Code reads as empty toward a remote server cannot carry the token"
    for entry in shared().values():
        for field in ("headers", "env"):
            for key, value in entry.get(field, {}).items():
                assert re.search(r"\$\{[A-Za-z_]\w*\}", value) and ":-" not in value, f"{key}: no default value for a credential, it would be committed"
    out, warnings = expand_server(server("github"), {"GITHUB_TOKEN": "t"})
    assert out["headers"]["Authorization"] == "Bearer t" and warnings == []


def test_e2_endpoints_and_paths_that_are_not_secret_have_a_default_so_the_file_works_unset():
    for name in ("github", "core"):
        assert re.fullmatch(r"\$\{[A-Z_]+:-https://[^}]+\}", server(name).get("url", "")), f"{name}: url needs a ${{VAR:-default}} with an https default"
    for name in ("docs", "schema"):
        args = server(name).get("args", [])
        assert args and args[0].startswith("${CLAUDE_PROJECT_DIR:-.}/"), f"{name}: CLAUDE_PROJECT_DIR is set for the server, not for the command, so it needs a default"
        out, warnings = expand_server(server(name), {})
        assert not [w for w in warnings if "CLAUDE_PROJECT_DIR" in w] and out["args"][0].startswith("./tools/")
    out, _ = expand_server(server("github"), {})
    assert out["url"].startswith("https://") and "${" not in out["url"]


def test_e3_only_the_small_core_server_is_loaded_at_the_start():
    assert server("core").get("alwaysLoad") is True, "core is used on every turn: alwaysLoad true"
    others = [n for n in SERVERS if n != "core" and shared().get(n, {}).get("alwaysLoad")]
    assert not others, f"tool search should find the others on demand: {others}"


def test_e4_shared_servers_stay_in_the_project_file_and_personal_ones_in_the_user_scope_file():
    user = read_json("user-scope.example.json").get("mcpServers", {})
    assert list(user) == ["scratch"], "the user-scope example holds one personal server, scratch"
    assert "scratch" not in shared(), "a personal server does not belong in the committed file"
    assert not set(user) & set(shared()), "the same name in two scopes: the whole entry of the higher scope wins and a warning is shown"
    servers, warnings = resolve_servers({"project": shared(), "user": user})
    assert sorted(servers) == sorted(SERVERS + ["scratch"]) and warnings == []
    assert not [e for e in user.values() if re.search(r"/home/|/Users/", json.dumps(e))], "no absolute home path: use ${HOME}"


def test_e5_permissions_allow_the_read_only_servers_by_name_and_deny_the_destructive_tool():
    settings = read_json(".claude/settings.json")
    table = {"mcp__docs__search_docs": "allow", "mcp__schema__read_schema": "allow", "mcp__github__list_pull_requests": "ask",
             "mcp__github__create_issue": "ask", "mcp__github__delete_repository": "deny", "mcp__core__ping": "ask", "mcp__scratch__anything": "ask"}
    got = {t: mcp_decision(settings, t) for t in table}
    assert got == table, {t: (got[t], v) for t, v in table.items() if got[t] != v}
    rules = settings.get("permissions", {}).get("allow", [])
    assert not [r for r in rules if r in ("*", "mcp__*") or r.startswith("mcp__*")], "an allow rule must name its server; mcp__* is ignored"


def test_e6_the_tool_description_says_when_to_use_it_instead_of_grep_and_fits_the_limit():
    tools = read_json("docs/tool-descriptions.json").get("tools", [])
    found = [t for t in tools if t.get("name") == "search_docs"]
    assert found, "describe the search_docs tool"
    text = found[0].get("description", "")
    assert len(text) <= 2048 and truncate(text) == text, "Claude Code cuts a description at 2,048 characters"
    assert re.search(r"instead of Grep", text[:300]), "put the boundary against Grep in the first 300 characters"
    assert "Returns" in text or "returns" in text, "say what comes back"
    assert re.search(r"\b(does not|doesn't|not search)\b", text), "say what it does not do"
    props = found[0].get("inputSchema", {}).get("properties", {})
    assert props and all(p.get("description") for p in props.values()), "every parameter needs a description"
    assert found[0]["inputSchema"].get("required") == ["query"]


def test_e7_the_notes_list_every_server_with_its_scope_and_read_the_catalog_as_a_resource():
    notes = read("docs/mcp-servers.md")
    for name in SERVERS:
        row = re.search(rf"(?m)^\|\s*`{name}`\s*\|\s*(\w+)\s*\|(.*)$", notes)
        assert row, f"add a table row for {name}"
        assert row.group(1) == "project", f"{name} is shared, so its scope is project"
    refs = re.findall(r"@([\w-]+):(\w+://[\w./-]+)", notes)
    assert refs and all(server_name in SERVERS for server_name, _ in refs), "@server:protocol://path must name a configured server"
    assert ("schema", "schema://orders") in refs, "show how to read the orders schema"
    row = re.search(r"(?m)^\|\s*`schema`.*$", notes).group(0)
    assert "resource" in row.lower(), "the schema server exposes resources"


def test_e8_no_file_holds_a_personal_path_an_address_or_a_key():
    hits = []
    for path in sorted(ROOT.rglob("*")):
        if path.is_file():
            text = path.read_text(errors="ignore")
            for label, pattern in (("home path", r"(/home/\w+|/Users/\w+|C:\\Users)"), ("email address", r"[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+"),
                                   ("key", r"(sk-ant-[\w-]{6,}|ghp_\w{6,}|Bearer [A-Za-z0-9]{12,})")):
                if re.search(pattern, text):
                    hits.append(f"{path.relative_to(ROOT)}: {label}")
    assert not hits, hits
