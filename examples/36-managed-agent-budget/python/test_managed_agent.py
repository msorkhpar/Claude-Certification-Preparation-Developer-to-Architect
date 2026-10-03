from managed_agent import budget_state, check_agent, check_budget, check_environment, check_session_resources, list_cost_cents


def test_an_omitted_networking_field_is_a_finding_and_limited_needs_the_package_flag():
    assert "unrestricted" in check_environment({"type": "cloud"})[0]
    limited = {"type": "cloud", "packages": {"pip": ["x"]}, "networking": {"type": "limited"}}
    assert any("allow_package_managers" in f for f in check_environment(limited))
    limited["networking"]["allow_package_managers"] = True
    assert check_environment(limited) == []


def test_hosts_are_bare_and_mcp_hosts_must_be_reachable():
    env = {"type": "cloud", "networking": {"type": "limited", "allowed_hosts": ["https://a.example.com", "b.example.com:443", "c.example.com"]}}
    found = check_environment(env, agent_mcp_hosts=["m.example.com", "c.example.com"])
    assert len([f for f in found if "bare hostname" in f]) == 2 and len([f for f in found if "MCP host" in f]) == 1
    env["networking"]["allow_mcp_servers"] = True
    assert not [f for f in check_environment(env, ["m.example.com"]) if "MCP host" in f]


def test_default_policies_flag_bash_on_an_open_network_and_unreviewed_mcp_tools():
    agent = {"tools": [{"type": "agent_toolset_20260401"}, {"type": "mcp_toolset", "mcp_server_name": "github", "default_config": {"permission_policy": {"type": "always_allow"}}}]}
    assert len(check_agent(agent, {"type": "cloud"})) == 2
    assert len(check_agent(agent, {"type": "cloud", "networking": {"type": "limited"}})) == 1
    asked = {"tools": [{"type": "agent_toolset_20260401", "configs": [{"name": "bash", "permission_policy": {"type": "always_ask"}}]}]}
    assert check_agent(asked, {"type": "cloud"}) == []


def test_self_hosted_sandboxes_accept_only_memory_stores():
    assert check_session_resources({"type": "self_hosted"}, [{"type": "file"}, {"type": "github_repository"}, {"type": "memory_store"}]) == [
        "self-hosted sandboxes reject file resources (400)", "self-hosted sandboxes reject github_repository resources (400)"]
    assert check_session_resources({"type": "cloud"}, [{"type": "file"}]) == []


def test_list_cost_adds_tokens_searches_and_running_time():
    assert list_cost_cents("claude-opus-5-5", 1_200_000, 150_000, 8, 7200) == 804
    assert list_cost_cents("claude-sonnet-5-5", 1_200_000, 150_000, 8, 7200) == 414
    assert list_cost_cents("claude-opus-5-5", 0, 0, 0, 3600) == 8


def test_a_budget_is_whole_cents_as_a_string_and_stops_new_work_at_the_cap():
    assert [check_budget(a) for a in ("125", "25.00", "050", "0")] == ["ok"] + [f"amount {a!r} is rejected: write whole cents as a string with no leading zeros" for a in ("25.00", "050", "0")]
    assert budget_state(804, "800") == "budget_reached" and budget_state(800, "800") == "budget_reached"
    assert budget_state(804, "1000") == "running, 196 cents left"
