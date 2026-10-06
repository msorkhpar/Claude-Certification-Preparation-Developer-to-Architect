import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from distribute import assign_tools, authorize, cache_impact, check_turn, plan_turn

CATALOG = [
    {"name": "web_search", "tags": ["web"]},
    {"name": "fetch_page", "tags": ["web"]},
    {"name": "load_document", "tags": ["documents"]},
    {"name": "extract_data_points", "tags": ["documents"]},
    {"name": "summarize_content", "tags": ["synthesis"]},
    {"name": "verify_fact", "tags": ["web"], "scoped": True},
    {"name": "write_report", "tags": ["reports"]},
    {"name": "publish_report", "tags": ["reports"], "irreversible": True},
]
ROLES = {"searcher": {"specialisation": ["web"]}, "analyst": {"specialisation": ["documents"]},
         "synthesizer": {"specialisation": ["synthesis"], "extra": ["verify_fact"]}, "reporter": {"specialisation": ["reports"]}}
POLICY = {"tools": {"process_refund": {"cap": 500, "irreversible": True}, "lookup_order": {"cap": None, "irreversible": False}}}


def assign(roles=None, catalog=None, **kw):
    result = assign_tools(ROLES if roles is None else roles, CATALOG if catalog is None else catalog, **kw)
    assert result is not None, "assign_tools returned nothing"
    return result


def call(**over):
    return {"id": "c1", "tool": "process_refund", "amount": 50, "customer": "C-1", "verified_customer": "C-1", **over}


def decide(c, approvals=()):
    answer = authorize(c, POLICY, approvals)
    assert answer is not None, "authorize returned nothing"
    return answer


def test_m1_each_role_gets_only_the_tools_of_its_specialisation_in_catalog_order():
    assert assign() == {"searcher": ["web_search", "fetch_page", "verify_fact"], "analyst": ["load_document", "extract_data_points"],
                        "synthesizer": ["summarize_content", "verify_fact"], "reporter": ["write_report"]}


def test_e1_a_role_over_its_budget_or_given_an_unknown_or_unscoped_outside_tool_or_a_duplicate_catalog_name_is_refused():
    refused = 0
    for roles, catalog, kw in (
        (ROLES, CATALOG, {"budget": 2}),
        ({"synthesizer": {"specialisation": ["synthesis"], "extra": ["nope"]}}, CATALOG, {}),
        ({"synthesizer": {"specialisation": ["synthesis"], "extra": ["fetch_page"]}}, CATALOG, {}),
        (ROLES, CATALOG + [{"name": "web_search", "tags": ["web"]}], {}),
        ({"nobody": {"specialisation": []}}, CATALOG, {}),
    ):
        try:
            assign_tools(roles, catalog, **kw)
        except ValueError:
            refused += 1
    assert refused == 5
    assert assign(budget=3)["searcher"] == ["web_search", "fetch_page", "verify_fact"]
    assert assign({"searcher": {"specialisation": ["web"], "extra": ["fetch_page"]}})["searcher"] == ["web_search", "fetch_page", "verify_fact"]


def test_e2_an_irreversible_tool_is_given_only_by_an_explicit_grant():
    assert "publish_report" not in assign()["reporter"]
    granted = assign({"reporter": {"specialisation": ["reports"], "extra": ["publish_report"]}})
    assert granted["reporter"] == ["write_report", "publish_report"]
    other = assign({"analyst": {"specialisation": ["documents"], "extra": ["publish_report"]}})
    assert other["analyst"] == ["load_document", "extract_data_points", "publish_report"]


def test_e3_a_model_that_accepts_forcing_gets_the_native_choice_and_the_others_get_auto_with_strict_tools_and_one_named_tool():
    tools = ["extract_metadata", "enrich"]
    native = plan_turn("claude-opus-5", "named", tools, forced="extract_metadata")
    assert native == {"tool_choice": {"type": "tool", "name": "extract_metadata"}, "tools": tools, "strict": False, "verify_call": False}
    assert plan_turn("claude-opus-5", "any", tools) == {"tool_choice": {"type": "any"}, "tools": tools, "strict": False, "verify_call": False}
    for model in ("claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"):
        assert plan_turn(model, "named", tools, forced="extract_metadata") == {"tool_choice": {"type": "auto"}, "tools": ["extract_metadata"], "strict": True, "verify_call": True}
        assert plan_turn(model, "any", tools) == {"tool_choice": {"type": "auto"}, "tools": tools, "strict": True, "verify_call": True}
        assert plan_turn(model, "free", tools)["tool_choice"] == {"type": "auto"} and plan_turn(model, "none", tools)["tool_choice"] == {"type": "none"}
    assert plan_turn("claude-opus-5", "named", tools, forced="enrich", manual_thinking=True)["tools"] == ["enrich"]
    refused = 0
    for args in (("claude-opus-5", "sometimes", tools), ("claude-opus-5", "named", tools)):
        try:
            plan_turn(*args)
        except ValueError:
            refused += 1
    assert refused == 2


def test_e4_a_change_of_tool_choice_costs_the_cached_messages_a_change_of_tools_costs_everything_and_a_repeat_costs_nothing():
    tools = ["extract_metadata", "enrich"]
    free = {"tool_choice": {"type": "auto"}, "tools": tools}
    assert cache_impact(None, free) == "none"
    assert cache_impact(free, {"tool_choice": {"type": "auto"}, "tools": ["extract_metadata", "enrich"]}) == "none"
    any_ = {"tool_choice": {"type": "any"}, "tools": tools}
    assert cache_impact(free, any_) == "messages" and cache_impact(any_, free) == "messages"
    a, b = {"tool_choice": {"type": "tool", "name": "a"}, "tools": tools}, {"tool_choice": {"type": "tool", "name": "b"}, "tools": tools}
    assert cache_impact(a, b) == "messages" and cache_impact(a, a) == "none"
    fallback = plan_turn("claude-sonnet-5-5", "named", tools, forced="extract_metadata")
    assert fallback is not None and cache_impact(free, fallback) == "all"
    assert cache_impact(free, {"tool_choice": {"type": "auto"}, "tools": ["enrich"]}) == "all"


def test_e5_a_reply_is_checked_against_the_call_that_was_required():
    text, use_a, use_b = {"type": "text", "text": "I think so."}, {"type": "tool_use", "name": "a"}, {"type": "tool_use", "name": "b"}
    assert check_turn([text], "any") == "missed_call"
    assert check_turn([text, use_b], "any") == "ok"
    assert check_turn([], "named", forced="a") == "missed_call"
    assert check_turn([use_b], "named", forced="a") == "wrong_tool"
    assert check_turn([use_a, use_b], "named", forced="a") == "ok"
    assert check_turn([use_b, use_a], "named", forced="a") == "wrong_tool"
    assert check_turn([text], "free") == "ok" and check_turn([use_a], "none") == "ok"


def test_e6_an_unknown_tool_a_wrong_owner_or_a_bad_amount_is_refused():
    unknown = decide(call(tool="delete_account"))
    assert unknown["allowed"] is False and unknown["code"] == "unknown_tool" and unknown["escalate"] is False
    for other in ("C-2", None):
        answer = decide(call(customer=other))
        assert answer["allowed"] is False and answer["code"] == "not_owner" and answer["escalate"] is False
    assert decide(call(verified_customer=None))["code"] == "not_owner"
    for amount in (0, -5, 12.5, "50", True, None):
        answer = decide(call(amount=amount), approvals={"c1"})
        assert answer["allowed"] is False and answer["code"] == "bad_amount", amount
    free = decide({"id": "c2", "tool": "lookup_order", "customer": "C-1", "verified_customer": "C-1"})
    assert free["allowed"] is True and free["code"] == "ok"


def test_e7_the_cap_is_inclusive_and_an_irreversible_call_needs_approval_that_never_lifts_the_cap():
    assert decide(call(amount=500), approvals={"c1"})["code"] == "ok"
    waiting = decide(call(amount=50))
    assert waiting["allowed"] is False and waiting["code"] == "needs_approval" and waiting["escalate"] is True
    assert decide(call(amount=50), approvals=["c1"])["allowed"] is True
    assert decide(call(amount=50), approvals={"c9"})["code"] == "needs_approval"
    over = decide(call(amount=501), approvals={"c1"})
    assert over["allowed"] is False and over["code"] == "over_cap" and over["escalate"] is True and "500" in over["message"]
    assert decide(call(amount=501, customer="C-2"))["code"] == "not_owner"
