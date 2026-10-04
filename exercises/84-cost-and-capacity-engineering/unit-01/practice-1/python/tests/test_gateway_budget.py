import os
import sys
from pathlib import Path


# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from gateway_budget import admit as _admit
from gateway_budget import delivery as _delivery
from gateway_budget import route as _route
from gateway_budget import showback as _showback

POLICY = {"allowed": ["haiku", "sonnet"], "routes": {"classify": "haiku", "draft": "sonnet", "review": "opus"}, "default": "sonnet", "cheaper": {"opus": "sonnet", "sonnet": "haiku"}}
PRICES = {"haiku": {"input": 100, "cache_read": 10, "output": 500}, "sonnet": {"input": 200, "cache_read": 20, "output": 1000}}


def route(request, status="allow"):
    result = _route(request, POLICY, status)
    assert result is None or isinstance(result, str), "route returned something that is not a model name"
    return result


def admit(spend, budget, estimate):
    result = _admit(spend, budget, estimate)
    assert isinstance(result, str), "admit returned nothing"
    return result


def showback(rows):
    result = _showback(rows, PRICES)
    assert isinstance(result, list), "showback returned nothing"
    return result


def test_m1_a_request_follows_the_route_table_of_the_gateway():
    assert [route({"task": t}) for t in ("classify", "draft", "review")] == ["haiku", "sonnet", "opus"]
    assert route({"task": "translate"}) == "sonnet"


def test_e1_a_team_near_its_budget_is_moved_to_a_cheaper_model_and_a_team_over_it_is_refused():
    assert route({"task": "review"}, "warn") == "sonnet"
    assert route({"task": "draft"}, "warn") == "haiku"
    assert route({"task": "classify"}, "warn") == "haiku"
    assert route({"task": "review"}, "block") is None


def test_e2_a_request_is_admitted_warned_or_blocked_against_the_budget():
    assert admit(0, 1000, 100) == "allow"
    assert admit(700, 1000, 99) == "allow"
    assert admit(700, 1000, 100) == "warn"
    assert admit(900, 1000, 100) == "warn"
    assert admit(900, 1000, 101) == "block"
    assert admit(0, 0, 0) == "block"
    assert admit(0, -5, 0) == "block"


def test_e3_showback_adds_each_teams_tokens_at_the_price_of_the_model_and_refuses_an_unknown_model():
    rows = [{"team": "a", "model": "sonnet", "input": 1_000_000, "cache_read": 5_000_000, "output": 100_000},
            {"team": "b", "model": "haiku", "input": 2_000_000, "cache_read": 0, "output": 1_000_000},
            {"team": "a", "model": "haiku", "input": 500_000, "cache_read": 0, "output": 0}]
    assert showback(rows) == [{"team": "b", "cents": 700}, {"team": "a", "cents": 450}]
    assert showback([{"team": "a", "model": "sonnet", "input": 1, "cache_read": 0, "output": 0}, {"team": "b", "model": "sonnet", "input": 1, "cache_read": 0, "output": 0}])[0]["team"] == "a"
    try:
        _showback([{"team": "a", "model": "other", "input": 1, "cache_read": 0, "output": 0}], PRICES)
        refused = None
    except ValueError as error:
        refused = str(error)
    assert refused is not None and "unknown model: other" in refused, "showback did not refuse a model that has no price"
    assert showback([]) == []


def test_e4_showback_rounds_each_teams_total_to_a_cent_once():
    def row(team, tokens):
        return {"team": team, "model": "sonnet", "input": tokens, "cache_read": 0, "output": 0}

    result = showback([row("x", 2000), row("x", 2000), row("y", 2500), row("z", 2499)])
    assert result == [{"team": "x", "cents": 1}, {"team": "y", "cents": 1}, {"team": "z", "cents": 0}]


def test_e5_a_caller_with_a_hard_latency_limit_gets_accept_and_poll_when_the_slow_case_does_not_fit():
    def pick(p95, timeout, margin):
        result = _delivery(p95, timeout, margin)
        assert isinstance(result, str), "delivery returned nothing"
        return result

    assert pick(8, 10, 25) == "sync"
    assert pick(8, 10, 26) == "accept-and-poll"
    assert pick(30, 10, 0) == "accept-and-poll"
    assert pick(10, 10, 0) == "sync"
    assert pick(10, 10, 1) == "accept-and-poll"


def test_e6_a_model_pinned_by_a_team_is_honoured_only_when_the_policy_allows_it():
    assert route({"task": "classify", "model": "sonnet"}) == "sonnet"
    assert route({"task": "classify", "model": "opus"}) == "haiku"
    assert route({"task": "review", "model": "haiku"}) == "haiku"
    assert route({"task": "draft", "model": "sonnet"}, "warn") == "haiku"
