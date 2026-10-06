import os
import sys
from pathlib import Path

import pytest

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from prompt_plan import MIN_CACHEABLE
from prompt_plan import assemble as _assemble
from prompt_plan import choose_model as _choose_model
from prompt_plan import reusable_prefix as _reusable_prefix

ROLE = {"name": "role", "static": True, "text": "r" * 400}  # 100 tokens
POLICY = {"name": "policy", "static": True, "text": "p" * 1648}  # 412 tokens: the prefix is exactly 512
HISTORY = {"name": "history", "static": False, "priority": 1, "text": "h" * 200}  # 50 tokens
QUESTION = {"name": "question", "static": False, "priority": 9, "text": "Q: {q}"}
EXTRA = {"name": "extra", "static": False, "priority": 1, "text": "e" * 160}  # 40 tokens


def assemble(modules, variables, budget=10_000):
    result = _assemble(modules, variables, budget)
    assert isinstance(result, dict), "assemble returned nothing"
    return result


def names(prompt):
    return [b["name"] for b in prompt["blocks"]]


def refused(modules, variables, budget=10_000):
    try:
        _assemble(modules, variables, budget)
    except ValueError as error:
        return str(error)
    raise AssertionError("the modules were accepted")


def test_m1_static_modules_come_first_and_the_breakpoint_follows_the_last_one():
    prompt = assemble([QUESTION, ROLE, HISTORY, POLICY], {"q": "hello"})
    assert names(prompt) == ["role", "policy", "question", "history"]
    assert prompt["breakpoint"] == 1 and prompt["tokens"] == 564 and prompt["dropped"] == []


def test_e1_a_variable_in_a_static_module_is_refused():
    assert "static" in refused([ROLE, {**POLICY, "text": "policy for {customer}"}, QUESTION], {"q": "x", "customer": "Ana"})


def test_e2_dynamic_variables_are_filled_and_a_missing_one_is_refused():
    prompt = assemble([ROLE, POLICY, QUESTION], {"q": "hello", "unused": "x"})
    assert prompt["blocks"][2]["text"] == "Q: hello"
    assert "missing variable: q" in refused([ROLE, POLICY, QUESTION], {})


def test_e3_the_lowest_priority_dynamic_module_is_dropped_first_and_a_tie_drops_the_later_one():
    modules = [ROLE, POLICY, HISTORY, QUESTION, EXTRA]
    one = assemble(modules, {"q": "hello"}, budget=570)
    assert one["dropped"] == ["extra"] and names(one) == ["role", "policy", "history", "question"] and one["tokens"] == 564
    two = assemble(modules, {"q": "hello"}, budget=520)
    assert two["dropped"] == ["extra", "history"] and names(two) == ["role", "policy", "question"] and two["tokens"] == 514


def test_e4_static_modules_are_never_dropped_and_a_budget_they_exceed_is_refused():
    assert "over budget" in refused([ROLE, POLICY, HISTORY, QUESTION], {"q": "hello"}, budget=400)
    assert "over budget" in refused([ROLE, POLICY], {}, budget=511)
    assert assemble([ROLE, POLICY], {}, budget=512)["tokens"] == 512


def test_e5_a_prefix_under_the_minimum_gets_no_breakpoint():
    assert MIN_CACHEABLE == 512
    assert assemble([ROLE, POLICY], {})["breakpoint"] == 1
    assert assemble([ROLE, {**POLICY, "text": "p" * 1644}], {})["breakpoint"] is None
    assert assemble([HISTORY, QUESTION], {"q": "x"})["breakpoint"] is None


MODELS = [
    {"name": "small", "tier": 1, "latency_ms": 300, "price_out": 1},
    {"name": "mid2", "tier": 2, "latency_ms": 900, "price_out": 5},
    {"name": "big", "tier": 3, "latency_ms": 2500, "price_out": 25},
    {"name": "mid", "tier": 2, "latency_ms": 900, "price_out": 5},
]


def test_e6_the_cheapest_model_that_meets_the_tier_and_the_latency_wins_and_ties_go_by_name():
    def pick(tier, latency):
        result = _choose_model({"tier": tier, "max_latency_ms": latency}, MODELS)
        assert result is None or isinstance(result, str), "choose_model returned something that is not a name"
        return result

    assert pick(1, 5000) == "small"
    assert pick(2, 1000) == "mid"
    assert pick(3, 5000) == "big"
    assert pick(3, 1000) is None
    assert pick(2, 500) is None
    assert pick(1, 100) is None


def test_e7_only_an_identical_static_prefix_can_be_reused():
    def reuse(a, b):
        result = _reusable_prefix(a, b)
        assert isinstance(result, int), "reusable_prefix returned nothing"
        return result

    base = assemble([ROLE, POLICY, HISTORY, QUESTION], {"q": "one"})
    other_question = assemble([ROLE, POLICY, HISTORY, QUESTION], {"q": "a different question"})
    assert reuse(base, other_question) == 512
    assert reuse(base, assemble([{**ROLE, "text": "R" * 400}, POLICY, HISTORY, QUESTION], {"q": "one"})) == 0
    assert reuse(base, assemble([ROLE, {**POLICY, "text": "P" * 1648}, HISTORY, QUESTION], {"q": "one"})) == 0
    assert reuse(base, assemble([ROLE, POLICY, {**HISTORY, "text": "other"}, QUESTION], {"q": "one"})) == 512
    assert reuse(base, assemble([HISTORY, QUESTION], {"q": "one"})) == 0
    assert reuse(assemble([HISTORY], {}), assemble([HISTORY], {})) == 0
