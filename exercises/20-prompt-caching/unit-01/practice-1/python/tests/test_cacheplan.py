import copy
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from cacheplan import PlanError, plan_request


def block(id, section, tokens, **extra):
    return {"id": id, "section": section, "tokens": tokens, **extra}


def ids(plan):
    return [p["id"] for p in (plan or [])]


def caches(plan):
    return {p["id"]: p["cache"] for p in (plan or [])}


def raised(blocks, **kwargs):
    try:
        plan_request(blocks, **kwargs)
    except PlanError as err:
        return err
    except Exception as err:  # noqa: BLE001
        return err
    return None


def test_m1_stable_content_comes_first_and_the_volatile_date_goes_last():
    blocks = [
        block("date", "system", 20, volatile=True),
        block("tools", "tools", 2000),
        block("rules", "system", 3000, breakpoint=True),
        block("manual", "messages", 6000, breakpoint=True),
        block("question", "messages", 40),
    ]
    plan = plan_request(blocks, min_tokens=1024)
    assert ids(plan) == ["tools", "rules", "manual", "question", "date"]
    assert caches(plan) == {"tools": None, "rules": "5m", "manual": "5m", "question": None, "date": None}


def test_e1_sections_follow_the_prefix_order_and_keep_their_own_order():
    blocks = [block("m1", "messages", 10), block("s1", "system", 10), block("t1", "tools", 10),
              block("s2", "system", 10), block("t2", "tools", 10), block("m2", "messages", 10)]
    assert ids(plan_request(blocks)) == ["t1", "t2", "s1", "s2", "m1", "m2"]


def test_e2_a_breakpoint_needs_the_stable_prefix_to_reach_the_minimum():
    blocks = [block("tools", "tools", 400, breakpoint=True), block("rules", "system", 500, breakpoint=True),
              block("doc", "messages", 700, breakpoint=True), block("ask", "messages", 30)]
    assert caches(plan_request(blocks, min_tokens=1024)) == {"tools": None, "rules": None, "doc": "5m", "ask": None}
    assert caches(plan_request(blocks, min_tokens=4096)) == {"tools": None, "rules": None, "doc": None, "ask": None}
    # volatile tokens come after the prefix, so they never help it reach the minimum
    padded = [block("stamp", "system", 5000, volatile=True), block("rules", "system", 500, breakpoint=True)]
    assert caches(plan_request(padded, min_tokens=1024)) == {"rules": None, "stamp": None}


def test_e3_at_most_four_breakpoints_are_sent():
    five = [block(f"b{i}", "messages", 2000, breakpoint=True) for i in range(5)]
    assert isinstance(raised(five, min_tokens=1024), PlanError)
    four = five[:4]
    assert list(caches(plan_request(four, min_tokens=1024)).values()) == ["5m"] * 4
    # two of the five never reach the minimum, so only three breakpoints are sent
    small = [block("a", "tools", 10, breakpoint=True), block("b", "system", 10, breakpoint=True)] + five[:3]
    assert list(caches(plan_request(small, min_tokens=1024)).values()) == [None, None, "5m", "5m", "5m"]


def test_e4_a_one_hour_breakpoint_may_not_follow_a_five_minute_one():
    long_first = [block("docs", "system", 3000, breakpoint=True, ttl="1h"), block("turns", "messages", 3000, breakpoint=True)]
    assert caches(plan_request(long_first, min_tokens=1024)) == {"docs": "1h", "turns": "5m"}
    wrong_way = [block("turns", "system", 3000, breakpoint=True), block("docs", "messages", 3000, breakpoint=True, ttl="1h")]
    assert isinstance(raised(wrong_way, min_tokens=1024), PlanError)


def test_e5_volatile_blocks_never_carry_a_breakpoint_and_tools_cannot_be_volatile():
    blocks = [block("rules", "system", 3000, breakpoint=True), block("stamp", "messages", 3000, volatile=True, breakpoint=True)]
    assert caches(plan_request(blocks, min_tokens=1024)) == {"rules": "5m", "stamp": None}
    assert isinstance(raised([block("t", "tools", 3000, volatile=True)]), PlanError)


def test_e6_every_block_comes_out_once_and_the_input_is_not_changed():
    blocks = [block("date", "system", 20, volatile=True), block("a", "messages", 2000, breakpoint=True), block("t", "tools", 2000)]
    before = copy.deepcopy(blocks)
    plan = plan_request(blocks, min_tokens=1024)
    assert sorted(ids(plan)) == ["a", "date", "t"]
    assert blocks == before
