import json
import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from batches import BatchError, build_requests, collect, split_batches

MODEL = "claude-haiku-4-5-20251001"


def item(id, **extra):
    return {"id": id, "params": {"model": MODEL, "max_tokens": 200, "messages": [{"role": "user", "content": f"Classify ticket {id}"}], **extra}}


def line(custom_id, result):
    return json.dumps({"custom_id": custom_id, "result": result})


def message(text, input_tokens=50, output_tokens=7, **extra_usage):
    return {"type": "succeeded", "message": {
        "id": "msg_illustrative", "type": "message", "role": "assistant", "model": MODEL, "stop_reason": "end_turn",
        "content": [{"type": "text", "text": text}], "usage": {"input_tokens": input_tokens, "output_tokens": output_tokens, **extra_usage}}}


def errored(kind):
    return {"type": "errored", "error": {"type": "error", "error": {"type": kind, "message": "x"}}}


def failure_of(fn, *args, **kwargs):
    """The BatchError field fn raises, 'crash' for another exception, None when it returns."""
    try:
        fn(*args, **kwargs)
    except BatchError as err:
        return err.field
    except Exception:  # noqa: BLE001
        return "crash"
    return None


def test_m1_results_are_matched_to_requests_by_custom_id_not_by_position():
    requests = build_requests([item("t-1"), item("t-2"), item("t-3")]) or []
    assert [r["custom_id"] for r in requests] == ["t-1", "t-2", "t-3"]
    assert requests[0]["params"]["max_tokens"] == 200
    lines = [line("t-3", message("billing")), line("t-1", message("refund")), line("t-2", message("shipping"))]
    done = collect(requests, lines) or {}
    assert [(o["custom_id"], o["status"], o["text"]) for o in done.get("outcomes", [])] == [
        ("t-1", "succeeded", "refund"), ("t-2", "succeeded", "shipping"), ("t-3", "succeeded", "billing")]
    assert [done.get("retry"), done.get("fix"), done.get("unknown")] == [[], [], []]


def test_e1_a_custom_id_is_1_to_64_safe_characters_and_unique():
    for bad in ("", "has space", "dot.dot", "x" * 65, "naïve"):
        assert failure_of(build_requests, [item(bad)]) == "custom_id", repr(bad)
    assert failure_of(build_requests, [item("a"), item("a")]) == "custom_id"
    assert failure_of(build_requests, [item("x" * 64), item("A_b-9")]) is None


def test_e2_parameters_a_batch_cannot_take_are_refused():
    assert failure_of(build_requests, [item("a", stream=True)]) == "params.stream"
    assert failure_of(build_requests, [item("a", speed="fast")]) == "params.speed"
    assert failure_of(build_requests, [{"id": "a", "params": {"model": MODEL, "max_tokens": 0, "messages": []}}]) == "params.max_tokens"
    assert failure_of(build_requests, [item("a", stream=False)]) is None


def test_e3_a_big_job_is_cut_in_order_by_request_count_and_by_size():
    requests = build_requests([item(f"r{i}") for i in range(7)]) or []
    by_count = split_batches(requests, max_requests=3) or []
    assert [[r["custom_id"] for r in b] for b in by_count] == [["r0", "r1", "r2"], ["r3", "r4", "r5"], ["r6"]]
    size = len(json.dumps(requests[0] if requests else {}, separators=(",", ":")).encode("utf-8"))
    by_size = split_batches(requests, max_bytes=size * 2 + 5) or []
    assert [len(b) for b in by_size] == [2, 2, 2, 1]
    assert [r["custom_id"] for b in by_size for r in b] == [f"r{i}" for i in range(7)]
    assert split_batches([]) == []
    assert failure_of(split_batches, requests, max_bytes=size - 1) == "size"


def test_e4_invalid_requests_are_fixed_and_the_rest_are_retried():
    requests = build_requests([item("ok"), item("bad"), item("busy"), item("late"), item("stopped")]) or []
    lines = [line("ok", message("fine")), line("bad", errored("invalid_request_error")), line("busy", errored("overloaded_error")),
             line("late", {"type": "expired"}), line("stopped", {"type": "canceled"})]
    done = collect(requests, lines) or {}
    assert done.get("fix") == ["bad"]
    assert done.get("retry") == ["busy", "late", "stopped"]
    assert [o["status"] for o in done.get("outcomes", [])] == ["succeeded", "errored", "errored", "expired", "canceled"]
    assert [o.get("error_type") for o in done.get("outcomes", [])][1] == "invalid_request_error"


def test_e5_a_request_with_no_result_is_missing_and_a_stranger_is_reported():
    requests = build_requests([item("a"), item("b"), item("c")]) or []
    lines = [line("c", message("three")), "", line("zzz", message("who")), line("a", message("one"))]
    done = collect(requests, lines) or {}
    assert [o["status"] for o in done.get("outcomes", [])] == ["succeeded", "missing", "succeeded"]
    assert done.get("retry") == ["b"]
    assert done.get("unknown") == ["zzz"]
    assert len(done.get("outcomes", [])) == 3


def test_e6_only_requests_that_succeeded_count_toward_usage():
    requests = build_requests([item("a"), item("b"), item("c")]) or []
    lines = [line("a", message("x", 100, 10, cache_read_input_tokens=400)), line("b", errored("api_error")),
             line("c", message("y", 30, 5, cache_creation_input_tokens=200))]
    usage = (collect(requests, lines) or {}).get("usage")
    assert usage == {"input_tokens": 130, "output_tokens": 15, "cache_creation_input_tokens": 200, "cache_read_input_tokens": 400}
