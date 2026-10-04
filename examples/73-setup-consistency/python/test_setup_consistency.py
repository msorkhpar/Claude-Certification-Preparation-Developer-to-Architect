import json

from setup_consistency import HERE, agents, audit, literal_secrets, load


def project(tmp_path, mcp, settings, agent_files):
    (tmp_path / ".claude/agents").mkdir(parents=True)
    (tmp_path / ".mcp.json").write_text(json.dumps({"mcpServers": mcp}))
    (tmp_path / ".claude/settings.json").write_text(json.dumps({"permissions": settings}))
    for name, text in agent_files.items():
        (tmp_path / ".claude/agents" / f"{name}.md").write_text(text)
    return tmp_path


def test_the_flawed_project_has_every_finding_and_the_fixed_one_has_none():
    assert audit(HERE / "project-before") == ["literal-secret: tickets headers.Authorization", "unknown-server: wiki (explorer)", "agent-bare-bash: explorer",
                                              "agent-inherits-all: scaffolder", "env-readable", "bare-write-allowed"]
    assert audit(HERE / "project-after") == []


def test_a_subagent_without_a_tools_line_inherits_everything_and_is_not_an_empty_list():
    found = agents(HERE / "project-before")
    assert found["scaffolder"] is None and found["explorer"] == ["Read", "Grep", "Bash", "mcp__wiki__search"]


def test_only_a_value_with_no_environment_reference_is_a_literal_secret():
    servers = {"a": {"headers": {"Authorization": "Bearer ${TOKEN}", "X-Trace": "abc"}}, "b": {"env": {"API_KEY": "abc", "REGION": "eu"}}}
    assert literal_secrets(servers) == ["b env.API_KEY"]


def test_a_permission_rule_for_a_server_that_is_not_configured_is_found(tmp_path):
    root = project(tmp_path, {"docs": {"type": "stdio", "command": "x"}}, {"allow": ["mcp__ghost__read"], "deny": ["Read(./.env)"]},
                   {"a": "---\nname: a\ndescription: Use when.\ntools: Read\n---\nbody\n"})
    assert audit(root) == ["unknown-server: ghost (settings)"]
    assert load(root)[1] == {"a": ["Read"]}
