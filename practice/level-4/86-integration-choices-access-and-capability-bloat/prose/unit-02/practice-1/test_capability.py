import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from capability import audit, authorize, choose_mechanism, gateway, plan_loading

CATALOG = {
    "read_ticket": {"access": "read", "tokens": 160},
    "draft_reply": {"access": "draft", "tokens": 220},
    "issue_refund": {"access": "money", "tokens": 240},
    "delete_account": {"access": "destroy", "tokens": 210},
    "export_report": {"access": "read", "tokens": 300},
}
POLICY = {
    "credentials": {"k1": "support", "k2": "research"},
    "models": {"support": {"standard"}, "research": {"standard", "deep"}},
    "tools": {"support": {"read_ticket", "draft_reply"}, "research": {"query"}},
    "limits": {"support": 2, "research": 5},
    "routes": {"standard": "claude-sonnet-5-5", "deep": "claude-opus-5-5"},
}


def request(credential, model, tool, recent):
    return {"credential": credential, "model": model, "tool": tool, "recent": recent}


def plan(*args, **kwargs):
    result = plan_loading(*args, **kwargs)
    assert isinstance(result, dict), "plan_loading returned nothing"
    return result


def decide(credential, model, tool, recent):
    result = gateway(request(credential, model, tool, recent), POLICY)
    assert isinstance(result, dict), "gateway returned nothing"
    return result


def names(count):
    return {f"t{i:02d}": 100 for i in range(1, count + 1)}


def test_m1_an_agent_loses_the_tools_its_role_does_not_need_and_the_risky_ones_among_them_are_named():
    agent = {"holds": list(CATALOG), "needs": ["read_ticket", "draft_reply"], "used": {"read_ticket": 12, "draft_reply": 9}}
    assert audit(agent, CATALOG) == {"remove": ["issue_refund", "delete_account", "export_report"], "risky": ["issue_refund", "delete_account"], "missing": [], "dormant": []}


def test_e1_a_tool_that_is_held_and_needed_but_never_used_is_reported_as_dormant_and_never_removed():
    agent = {"holds": ["read_ticket", "draft_reply"], "needs": ["read_ticket", "draft_reply", "issue_refund"], "used": {"read_ticket": 5, "draft_reply": 0}}
    assert audit(agent, CATALOG) == {"remove": [], "risky": [], "missing": ["issue_refund"], "dormant": ["draft_reply"]}
    agent["used"] = {"read_ticket": 5}
    assert audit(agent, CATALOG)["dormant"] == ["draft_reply"]


def test_e2_a_small_set_loads_whole_and_a_large_one_defers_when_it_has_ten_tools_or_over_ten_thousand_tokens():
    assert plan(names(9), {}) == {"search": False, "load_now": list(names(9)), "deferred": [], "tokens": 900}
    ten = plan(names(10), {})
    assert ten["search"] is True and ten["load_now"] == ["t01", "t02", "t03", "t04"] and len(ten["deferred"]) == 6 and ten["tokens"] == 750
    assert plan({"a": 5000, "b": 5000}, {})["search"] is False
    heavy = plan({"a": 5000, "b": 5001}, {})
    assert heavy["search"] is True and heavy["tokens"] == 10001 + 350 and heavy["deferred"] == []


def test_e3_the_number_of_tools_kept_loaded_stays_between_three_and_five_and_ties_are_broken_by_name():
    assert len(plan(names(12), {}, keep=1)["load_now"]) == 3
    assert len(plan(names(12), {}, keep=8)["load_now"]) == 5
    assert plan(dict(reversed(list(names(12).items()))), {"t05": 9, "t02": 9}, keep=3)["load_now"] == ["t02", "t05", "t01"]
    assert plan(names(12), {}, keep=4, search_tokens=0)["tokens"] == 400


def test_e4_the_mechanism_follows_the_counterpart_then_the_path_then_the_number_of_clients():
    rows = [(1, "agent", "fixed"), (4, "agent", "model-chosen"), (4, "tool", "fixed"), (1, "tool", "fixed"), (4, "tool", "model-chosen"), (1, "tool", "model-chosen")]
    assert [choose_mechanism(*row) for row in rows] == ["agent-to-agent", "agent-to-agent", "direct call in code", "direct call in code", "MCP server", "custom tool"]


def test_e5_a_call_needs_the_users_scope_and_the_agents_scope_and_an_unknown_tool_is_refused():
    required = {"issue_refund": "refunds:write", "read_ticket": "tickets:read"}
    assert authorize("issue_refund", {"tickets:read"}, {"refunds:write"}, required) == "deny: user lacks refunds:write"
    assert authorize("issue_refund", {"refunds:write"}, {"tickets:read"}, required) == "deny: agent lacks refunds:write"
    assert authorize("issue_refund", set(), set(), required) == "deny: user lacks refunds:write"
    assert authorize("issue_refund", {"refunds:write"}, {"refunds:write"}, required) == "allow"
    assert authorize("wipe_disk", {"x"}, {"x"}, required) == "deny: unknown tool"


def test_e6_the_gateway_checks_the_credential_then_the_model_then_the_tool_then_the_rate():
    reason = lambda *args: decide(*args)["reason"]
    assert reason(None, "deep", "wipe", 99) == "unauthenticated"
    assert reason("zzz", "deep", "wipe", 99) == "unauthenticated"
    assert reason("k1", "deep", "wipe", 99) == "model not allowed"
    assert reason("k1", "standard", "issue_refund", 99) == "tool not allowed"
    assert reason("k1", "standard", "read_ticket", 2) == "rate limited"
    assert reason("k1", "standard", "read_ticket", 1) == "routed to claude-sonnet-5-5"
    assert decide("k2", "deep", None, 0)["decision"] == "allow" and reason("k2", "deep", None, 0) == "routed to claude-opus-5-5"
    assert decide("k1", "standard", "read_ticket", 2)["decision"] == "deny"


def test_e7_the_gateway_keeps_a_record_of_every_decision_with_the_team_or_unknown_and_no_content():
    assert decide(None, "deep", None, 0)["audit"] == {"team": "unknown", "model": "deep", "tool": "none", "decision": "deny"}
    assert decide("k1", "standard", "issue_refund", 0)["audit"] == {"team": "support", "model": "standard", "tool": "issue_refund", "decision": "deny"}
    assert decide("k2", "deep", "query", 0)["audit"] == {"team": "research", "model": "deep", "tool": "query", "decision": "allow"}
