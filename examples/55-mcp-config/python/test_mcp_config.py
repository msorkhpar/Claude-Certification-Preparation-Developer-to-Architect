from mcp_config import SCOPES, expand, expand_server, lint, mcp_decision, resolve_servers, truncate


def test_a_variable_expands_and_a_default_fills_an_unset_one():
    assert expand("${A}-${B:-fallback}-${C:-}", {"A": "x"}) == ("x-fallback-", [])


def test_an_unset_variable_without_a_default_keeps_its_text_and_is_reported():
    assert expand("Bearer ${TOKEN}", {}) == ("Bearer ${TOKEN}", ["TOKEN"])


def test_credential_variables_read_empty_toward_a_remote_server_but_not_for_a_local_one():
    env = {"NPM_TOKEN": "t", "MY_TOKEN": "m"}
    assert expand("Bearer ${NPM_TOKEN}", env, remote=True) == ("Bearer ", [])
    assert expand("Bearer ${NPM_TOKEN:-d}", {}, remote=True) == ("Bearer ", [])
    assert expand("Bearer ${MY_TOKEN}", env, remote=True) == ("Bearer m", [])
    assert expand("${NPM_TOKEN}", env, remote=False) == ("t", [])


def test_expansion_covers_command_args_env_url_and_headers_only():
    entry = {"type": "stdio", "command": "${BIN:-run}", "args": ["${A:-1}"], "env": {"K": "${V:-v}"}, "note": "${A}"}
    out, warnings = expand_server(entry, {})
    assert out == {"type": "stdio", "command": "run", "args": ["1"], "env": {"K": "v"}, "note": "${A}"} and warnings == []


def test_the_highest_scope_wins_the_whole_entry_and_a_conflict_is_reported():
    scopes = {"user": {"s": {"url": "u", "extra": 1}}, "project": {"s": {"url": "p"}}, "local": {}}
    servers, warnings = resolve_servers(scopes)
    assert servers["s"] == ("project", {"url": "p"}) and len(warnings) == 1 and SCOPES[0] == "local"


def test_lint_finds_literal_secrets_and_missing_fields():
    findings = lint({"mcpServers": {"a": {"type": "http", "headers": {"Authorization": "x"}}, "b": {}}})
    assert findings == ["a: a http server needs a url", "a: headers.Authorization holds a literal value, reference an environment variable", "b: a stdio server needs a command"]
    assert lint({"mcpServers": {"a": {"type": "http", "url": "u", "headers": {"Authorization": "Bearer ${T}"}}}}) == []


def test_a_description_is_cut_at_the_limit():
    assert len(truncate("x" * 5000)) == 2048


def test_only_an_allow_rule_that_names_its_server_is_honoured_and_deny_wins():
    s = {"permissions": {"allow": ["mcp__docs__*", "mcp__*", "mcp__github__get_*"], "deny": ["mcp__github__get_secret"]}}
    assert [mcp_decision(s, t) for t in ("mcp__docs__a", "mcp__other__a", "mcp__github__get_pr", "mcp__github__get_secret", "mcp__github__push")] == ["allow", "ask", "allow", "deny", "ask"]
