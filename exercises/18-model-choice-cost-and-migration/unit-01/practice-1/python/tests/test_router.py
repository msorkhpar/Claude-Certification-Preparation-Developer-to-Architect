import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from router import NoModelError, request_cost, route

# Prices in dollars per million tokens, read from the pricing page on 2026-10-02 (a dollar per MTok is a micro-dollar per token).
HAIKU = {"id": "claude-haiku-4-5-20251001", "tier": 1, "context": 200_000, "max_output": 64_000, "input": 1, "output": 5, "cache_read_multiplier": 0.1}
SONNET = {"id": "claude-sonnet-5-5", "tier": 2, "context": 1_000_000, "max_output": 128_000, "input": 2, "output": 10, "cache_read_multiplier": 0.1}
OPUS = {"id": "claude-opus-5-5", "tier": 3, "context": 1_000_000, "max_output": 128_000, "input": 4, "output": 20, "cache_read_multiplier": 0.05}
FABLE = {"id": "claude-fable-5-1", "tier": 4, "context": 1_000_000, "max_output": 128_000, "input": 10, "output": 50, "cache_read_multiplier": 0.025}
CATALOG = [HAIKU, SONNET, OPUS, FABLE]


def raised(fn, *args):
    """The exception fn raises, or None."""
    try:
        fn(*args)
    except Exception as err:  # noqa: BLE001
        return err
    return None


def test_m1_cost_of_a_plain_request_and_the_cheapest_model_that_meets_the_tier():
    assert request_cost(SONNET, {"input_tokens": 1200, "output_tokens": 300}) == 5400
    assert request_cost(HAIKU, {"input_tokens": 1000, "output_tokens": 200}) == 2000
    assert route(CATALOG, {"min_tier": 1, "usage": {"input_tokens": 1000, "output_tokens": 200}}) == HAIKU["id"]
    assert route(CATALOG, {"min_tier": 2, "usage": {"input_tokens": 1000, "output_tokens": 200}}) == SONNET["id"]


def test_e1_cache_reads_and_writes_are_priced_by_their_own_multipliers():
    usage = {"input_tokens": 100, "output_tokens": 50, "cache_read_input_tokens": 4000,
             "cache_creation_input_tokens": 3000,
             "cache_creation": {"ephemeral_5m_input_tokens": 1000, "ephemeral_1h_input_tokens": 2000}}
    assert request_cost(OPUS, usage) == 23200  # 400 + 5000 + 16000 + 800 + 1000
    assert request_cost(FABLE, {"cache_read_input_tokens": 1_000_000}) == 250000
    assert request_cost(SONNET, {"cache_read_input_tokens": 1_000_000}) == 200000
    # without the per-lifetime split, the written tokens are the 5-minute kind
    assert request_cost(SONNET, {"cache_creation_input_tokens": 3000}) == 7500


def test_e2_the_batch_discount_halves_every_part_of_the_cost():
    usage = {"input_tokens": 100, "output_tokens": 50, "cache_read_input_tokens": 4000,
             "cache_creation": {"ephemeral_5m_input_tokens": 1000, "ephemeral_1h_input_tokens": 2000}}
    assert request_cost(OPUS, usage, batch=True) == 11600
    assert request_cost(OPUS, usage) == 23200
    assert request_cost(HAIKU, {"output_tokens": 1000}, batch=True) == 2500


def test_e3_the_router_picks_by_cost_and_tier_not_by_the_order_of_the_catalog():
    small = {"min_tier": 3, "usage": {"input_tokens": 500, "output_tokens": 100}}
    assert route(list(reversed(CATALOG)), small) == OPUS["id"]
    assert route(list(reversed(CATALOG)), {"min_tier": 1, "usage": {"input_tokens": 500, "output_tokens": 100}}) == HAIKU["id"]
    assert route([FABLE, OPUS], {"min_tier": 1, "usage": {"input_tokens": 500, "output_tokens": 100}}) == OPUS["id"]
    # batch changes every price by the same factor, so it never changes the winner
    assert route(CATALOG, {"min_tier": 2, "batch": True, "usage": {"input_tokens": 500}}) == SONNET["id"]


def test_e4_a_model_whose_context_or_output_limit_is_too_small_is_skipped():
    big = {"min_tier": 1, "usage": {"input_tokens": 250_000, "cache_read_input_tokens": 50_000, "output_tokens": 100}}
    assert route(CATALOG, big) == SONNET["id"]
    long_answer = {"min_tier": 1, "max_tokens": 100_000, "usage": {"input_tokens": 100, "output_tokens": 100}}
    assert route(CATALOG, long_answer) == SONNET["id"]
    assert route(CATALOG, {"min_tier": 1, "max_tokens": 64_000, "usage": {"input_tokens": 100}}) == HAIKU["id"]


def test_e5_deprecated_models_are_skipped_and_an_empty_choice_raises():
    old = dict(HAIKU, deprecated=True)
    assert route([old, SONNET], {"min_tier": 1, "usage": {"input_tokens": 100}}) == SONNET["id"]
    assert isinstance(raised(route, [old], {"min_tier": 1, "usage": {"input_tokens": 100}}), NoModelError)
    assert isinstance(raised(route, CATALOG, {"min_tier": 5, "usage": {"input_tokens": 100}}), NoModelError)
    assert isinstance(raised(route, CATALOG, {"min_tier": 1, "usage": {"input_tokens": 2_000_000}}), NoModelError)


def test_e6_a_tie_goes_to_the_lower_tier():
    reads = {"min_tier": 2, "usage": {"cache_read_input_tokens": 1_000_000}}
    assert request_cost(SONNET, reads["usage"]) == request_cost(OPUS, reads["usage"]) == 200000
    assert route(CATALOG, reads) == SONNET["id"]
    assert route(list(reversed(CATALOG)), reads) == SONNET["id"]
