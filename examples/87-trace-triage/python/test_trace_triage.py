from trace_triage import *


def test_errors_and_slow_traces_are_always_kept():
    assert keep_reason("t-refund", TRACES["t-refund"], 0) == "error"
    slow = [Span("s1", "", "agent", "a", "ok", 6000, "")]
    assert keep_reason("x", slow, 0) == "slow"


def test_healthy_traces_are_kept_by_id_not_by_chance():
    ids = [f"trace-{i}" for i in range(100)]
    first = [keep_reason(t, TRACES["t-plain"], 10) for t in ids]
    second = [keep_reason(t, TRACES["t-plain"], 10) for t in ids]
    assert first == second
    assert 0 < first.count("sampled") < 100
    assert keep_reason("trace-1", TRACES["t-plain"], 0) == "dropped"


def test_root_cause_is_the_deepest_failing_span():
    c = root_cause(TRACES["t-refund"])
    assert (c["layer"], c["name"]) == ("tool", "web_fetch")
    assert c["path"] == ["orchestrator", "order-researcher", "web_fetch"]


def test_stale_retrieval_is_blamed_when_nothing_failed():
    assert root_cause(TRACES["t-policy"])["why"] == "stale"
    assert root_cause(TRACES["t-empty"])["why"] == "no-hits"
    assert root_cause(TRACES["t-plain"])["layer"] == "none"


def test_drift_flags_both_directions_over_tolerance():
    d = drift({"a": 10, "b": 10, "c": 10}, {"a": 14, "b": 6, "c": 11}, 25)
    assert d == ["a up 40%", "b down 40%"]


def test_alert_needs_consecutive_windows():
    s = [1, 2, 9, 2, 8, 9, 10, 3]
    assert alert_at(s, 5, 1) == 2
    assert alert_at(s, 5, 3) == 6
    assert alert_at([9, 1, 9, 1, 9], 5, 2) == -1


def test_redact_drops_content_unless_allowed():
    e = {"trace": "t", "prompt": "x", "tool_input": "y", "input_tokens": 5}
    assert redact(e) == {"trace": "t", "input_tokens": 5}
    assert redact(e, ("tool_input",)) == {"trace": "t", "tool_input": "y", "input_tokens": 5}
