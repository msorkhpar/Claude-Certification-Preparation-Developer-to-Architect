from capability_audit import CATALOG, POLICY, TOOLS, USAGE, audit, authorize, choose_mechanism, gateway, plan_loading


def test_a_role_loses_the_tools_it_does_not_need_and_the_risky_ones_are_named():
    result = audit(list(CATALOG), ["read_ticket", "draft_reply"], CATALOG)
    assert result == {"remove": ["issue_refund", "delete_account"], "risky": ["issue_refund", "delete_account"], "missing": []}
    assert audit(["read_ticket"], ["read_ticket", "draft_reply"], CATALOG) == {"remove": [], "risky": [], "missing": ["draft_reply"]}


def test_a_large_tool_set_defers_all_but_the_most_used_and_a_small_one_loads_whole():
    plan = plan_loading(TOOLS, USAGE)
    assert plan["search"] and plan["load_now"] == ["github_create_issue", "github_search_code", "slack_post_message", "github_get_pr"]
    assert len(plan["deferred"]) == 20 and plan["tokens"] == 2320
    small = {name: 100 for name in list(TOOLS)[:9]}
    assert plan_loading(small, USAGE) == {"search": False, "load_now": list(small), "deferred": [], "tokens": 900}
    assert plan_loading({"a": 6000, "b": 5000}, {})["search"] is True
    assert len(plan_loading(TOOLS, USAGE, keep=9)["load_now"]) == 5 and len(plan_loading(TOOLS, USAGE, keep=1)["load_now"]) == 3


def test_the_mechanism_follows_the_counterpart_then_the_path_then_the_number_of_clients():
    assert [choose_mechanism(c, k, p) for c, k, p in [(1, "agent", "fixed"), (4, "tool", "fixed"), (4, "tool", "model-chosen"), (1, "tool", "model-chosen")]] == ["agent-to-agent", "direct call in code", "MCP server", "custom tool"]


def test_the_agent_rights_are_a_ceiling_and_the_user_rights_decide():
    required = {"issue_refund": "refunds:write"}
    assert authorize("issue_refund", {"tickets:read"}, {"refunds:write"}, required) == "deny: user lacks refunds:write"
    assert authorize("issue_refund", {"refunds:write"}, {"tickets:read"}, required) == "deny: agent lacks refunds:write"
    assert authorize("issue_refund", {"refunds:write"}, {"refunds:write"}, required) == "allow"
    assert authorize("wipe", {"x"}, {"x"}, required) == "deny: unknown tool"


def test_the_gateway_decides_in_order_and_keeps_a_record_of_every_decision():
    assert gateway(None, "standard", 0, POLICY)["reason"] == "unauthenticated"
    assert gateway("key-a", "deep", 0, POLICY)["reason"] == "model not allowed"
    assert gateway("key-a", "standard", 30, POLICY)["reason"] == "rate limited"
    allowed = gateway("key-b", "deep", 3, POLICY)
    assert allowed == {"decision": "allow", "reason": "routed to claude-opus-5-5", "audit": {"team": "research", "model": "deep", "decision": "allow"}}
    assert gateway("nope", "deep", 0, POLICY)["audit"] == {"team": "unknown", "model": "deep", "decision": "deny"}
