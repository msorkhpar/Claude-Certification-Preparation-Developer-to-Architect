import os
import sys
from pathlib import Path


# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from reliable_agents import Fatal, Transient
from reliable_agents import run_plan as _run_plan


def run_plan(plan, agents, store, attempts=3, breaker_threshold=3):
    result = _run_plan(plan, agents, store, attempts, breaker_threshold)
    assert isinstance(result, dict), "run_plan returned nothing"
    return result


def task(tid, agent="w", needs=(), fallback=None):
    return {"id": tid, "agent": agent, "key": f"key-{tid}", "needs": list(needs), "fallback": fallback}


def echo(key, inputs):
    return key + "|" + ",".join(inputs[k] for k in sorted(inputs))


def test_m1_tasks_run_in_order_and_receive_the_results_of_the_tasks_they_need():
    store = {}
    result = run_plan([task("a"), task("b", needs=["a"]), task("c", needs=["a", "b"])], {"w": echo}, store)
    assert result["done"] == {"a": "key-a|", "b": "key-b|key-a|", "c": "key-c|key-a|,key-b|key-a|"}
    assert result["attempts"] == {"a": 1, "b": 1, "c": 1}
    assert (result["failed"], result["skipped"], result["degraded"], result["resumed"]) == ({}, {}, [], [])
    assert store == result["done"]


def test_e1_every_retry_of_a_task_carries_the_same_idempotency_key():
    seen = []

    def flaky(key, inputs):
        seen.append(key)
        if len(seen) < 3:
            raise Transient("lost")
        return "ok"

    result = run_plan([task("t")], {"w": flaky}, {})
    assert seen == ["key-t", "key-t", "key-t"]
    assert result["done"] == {"t": "ok"} and result["attempts"] == {"t": 3}


def test_e2_retries_stop_at_the_limit_and_a_fatal_failure_is_not_retried():
    def down(key, inputs):
        raise Transient("down")

    def bad(key, inputs):
        raise Fatal("bad input")

    result = run_plan([task("a"), task("b", agent="x")], {"w": down, "x": bad}, {}, attempts=3, breaker_threshold=99)
    assert result["failed"] == {"a": "retries exhausted", "b": "fatal: bad input"}
    assert result["attempts"] == {"a": 3, "b": 1}


def test_e3_a_failure_stays_inside_its_branch_and_dependents_are_skipped():
    def agents(key, inputs):
        if key == "key-a":
            raise Fatal("boom")
        return "ok"

    plan = [task("a"), task("b", needs=["a"]), task("c"), task("d", needs=["b"])]
    result = run_plan(plan, {"w": agents}, {})
    assert result["done"] == {"c": "ok"}
    assert result["failed"] == {"a": "fatal: boom"}
    assert result["skipped"] == {"b": "dependency failed: a", "d": "dependency failed: b"}
    assert result["attempts"] == {"a": 1, "b": 0, "c": 1, "d": 0}


def test_e4_a_breaker_stops_calls_to_an_agent_that_keeps_failing_and_a_success_resets_it():
    calls = []

    def flaky(key, inputs):
        calls.append(key)
        raise Transient("down")

    def good(key, inputs):
        return "fine"

    plan = [task("t1", agent="flaky"), task("t2", agent="flaky"), task("t3", agent="flaky"), task("t4", agent="good")]
    result = run_plan(plan, {"flaky": flaky, "good": good}, {}, attempts=2, breaker_threshold=3)
    assert result["failed"] == {"t1": "retries exhausted", "t2": "circuit open", "t3": "circuit open"}
    assert result["done"] == {"t4": "fine"}
    assert result["attempts"] == {"t1": 2, "t2": 1, "t3": 0, "t4": 1}
    assert len(calls) == 3

    count = []

    def every_other(key, inputs):
        count.append(key)
        if len(count) % 2 == 1:
            raise Transient("blip")
        return "ok"

    healthy = run_plan([task("a", agent="x"), task("b", agent="x")], {"x": every_other}, {}, attempts=2, breaker_threshold=2)
    assert healthy["done"] == {"a": "ok", "b": "ok"} and healthy["failed"] == {}


def test_e5_a_fallback_degrades_one_task_with_its_own_key_and_is_not_checkpointed():
    seen = []

    def primary(key, inputs):
        raise Fatal("down")

    def backup(key, inputs):
        seen.append(key)
        return "from backup"

    store = {}
    plan = [task("s", agent="primary", fallback="backup"), task("t", agent="use", needs=["s"])]
    result = run_plan(plan, {"primary": primary, "backup": backup, "use": lambda key, inputs: "got " + inputs["s"]}, store)
    assert result["done"] == {"s": "from backup", "t": "got from backup"}
    assert result["degraded"] == ["s"] and result["failed"] == {}
    assert seen == ["key-s:fallback"]
    assert set(store) == {"t"}
    both_down = run_plan([task("s", agent="primary", fallback="primary")], {"primary": primary}, {})
    assert both_down["failed"] == {"s": "fatal: down"} and both_down["degraded"] == []


def test_e6_a_second_run_resumes_from_the_checkpoint_and_retries_only_what_failed():
    calls = []
    healthy = {"b": False}

    def agent(key, inputs):
        calls.append(key)
        if key == "key-b" and not healthy["b"]:
            raise Transient("down")
        return "ok-" + key

    store = {}
    plan = [task("a"), task("b", needs=["a"])]
    first = run_plan(plan, {"w": agent}, store, attempts=2, breaker_threshold=99)
    assert first["failed"] == {"b": "retries exhausted"} and set(store) == {"a"}
    healthy["b"] = True
    second = run_plan(plan, {"w": agent}, store, attempts=2, breaker_threshold=99)
    assert second["resumed"] == ["a"] and second["done"] == {"a": "ok-key-a", "b": "ok-key-b"}
    assert second["attempts"] == {"a": 0, "b": 1}
    assert calls.count("key-a") == 1


def test_e7_an_unexpected_crash_is_not_swallowed_and_keeps_the_work_already_checkpointed():
    def agent(key, inputs):
        if key == "key-b":
            raise RuntimeError("process died")
        return "ok"

    store = {}
    crash = None
    try:
        run_plan([task("a"), task("b"), task("c")], {"w": agent}, store)
    except RuntimeError as error:
        crash = error
    assert crash is not None, "the crash was swallowed by the runner"
    assert store == {"a": "ok"}
