import os
import sys
from pathlib import Path

# The solution is the file beside this test.
sys.path.insert(0, str(Path(__file__).resolve().parent))
from triage import Event, Span, alert_at, drift, keep_trace, redact, request_trail, root_cause


def sp(id, parent="", kind="agent", name="n", status="ok", ms=100, note=""):
    return Span(id, parent, kind, name, status, ms, note)


def keep(trace_id, spans, rate, **kw):
    result = keep_trace(trace_id, spans, rate, **kw)
    assert isinstance(result, str), "keep_trace returned nothing"
    return result


def cause(spans):
    result = root_cause(spans)
    assert isinstance(result, dict), "root_cause returned nothing"
    return result


def listed(result):
    assert isinstance(result, list), "a list was expected"
    return result


PLAIN = [sp("s1", name="assistant", ms=1900), sp("s2", "s1", "retrieval", "search", ms=100), sp("s3", "s1", "llm", "answer", ms=1700)]


def test_m1_every_trace_gets_one_reason_and_a_healthy_trace_is_kept_by_its_id():
    assert keep("a", [sp("s1", status="error")], 0) == "error"
    assert keep("a", [sp("s1", ms=6000)], 0) == "slow"
    assert keep("a", PLAIN, 0, feedback=True) == "feedback"
    assert keep("a", PLAIN, 0) == "dropped"
    assert keep("a", PLAIN, 100) == "sampled"
    ids = [f"trace-{i}" for i in range(100)]
    first = [keep(t, PLAIN, 10) for t in ids]
    assert first == [keep(t, PLAIN, 10) for t in ids]
    assert 0 < first.count("sampled") < 100


def test_e1_an_error_outranks_a_slow_root_which_outranks_retries_which_outrank_a_flag():
    calls = [sp("s2", "s1", "tool", "fetch"), sp("s3", "s1", "tool", "fetch"), sp("s4", "s1", "tool", "fetch")]
    assert keep("a", [sp("s1", ms=9000, status="error")], 0) == "error"
    assert keep("a", [sp("s1", ms=9000)] + calls, 0, feedback=True) == "slow"
    assert keep("a", [sp("s1")] + calls, 0, feedback=True) == "retries"
    assert keep("a", [sp("s1")] + calls[:2], 0) == "dropped"
    assert keep("a", [sp("s1", ms=5000)], 0) == "dropped"
    assert keep("a", [sp("s1", ms=3000)], 0, slow_ms=2000) == "slow"


def test_e2_the_root_cause_is_the_deepest_failing_span_and_its_path_starts_at_the_root():
    spans = [sp("s1", name="orchestrator", status="error"), sp("s2", "s1", "agent", "researcher", "error"), sp("s3", "s2", "tool", "fetch", "error"), sp("s4", "s1", "llm", "summarise")]
    c = cause(spans)
    assert (c["layer"], c["name"], c["why"]) == ("tool", "fetch", "failed")
    assert c["path"] == ["orchestrator", "researcher", "fetch"]
    two = [sp("s1", name="top"), sp("s2", "s1", "tool", "first", "error"), sp("s3", "s1", "tool", "second", "error")]
    assert cause(two)["name"] == "first"


def test_e3_a_stale_or_empty_retrieval_is_blamed_only_when_no_span_failed():
    stale = [sp("s1", name="assistant"), sp("s2", "s1", "retrieval", "search", note="stale"), sp("s3", "s1", "llm", "answer")]
    assert (cause(stale)["layer"], cause(stale)["why"]) == ("retrieval", "stale")
    assert cause(stale)["path"] == ["assistant", "search"]
    empty = [sp("s1", name="assistant"), sp("s2", "s1", "retrieval", "search", note="no-hits")]
    assert cause(empty)["why"] == "no-hits"
    both = stale[:2] + [sp("s3", "s1", "llm", "answer", "error")]
    assert (cause(both)["layer"], cause(both)["why"]) == ("llm", "failed")
    assert cause(PLAIN) == {"layer": "none", "name": "", "why": "no span failed", "path": []}


def test_e4_drift_reports_a_move_in_either_direction_over_the_tolerance_and_never_divides_by_zero():
    assert listed(drift({"a": 10, "b": 10, "c": 10}, {"a": 14, "b": 6, "c": 11}, 25)) == ["a up 40%", "b down 40%"]
    assert listed(drift({"a": 10}, {"a": 13}, 30)) == []
    assert listed(drift({"a": 10}, {"a": 14}, 30)) == ["a up 40%"]
    assert listed(drift({"z": 0, "y": 0}, {"z": 5, "y": 0}, 25)) == ["z up 100%"]
    assert listed(drift({"b": 5, "a": 5}, {"b": 10, "a": 10}, 0)) == ["a up 100%", "b up 100%"]


def test_e5_an_alert_needs_consecutive_windows_over_the_threshold_and_a_dip_starts_the_count_again():
    s = [1, 2, 9, 2, 8, 9, 10, 3]
    assert alert_at(s, 5, 1) == 2
    assert alert_at(s, 5, 3) == 6
    assert alert_at([9, 1, 9, 1, 9], 5, 2) == -1
    assert alert_at([5, 5, 5], 5, 1) == -1
    assert alert_at([], 5, 1) == -1


def test_e6_a_log_record_drops_the_content_fields_unless_they_are_allowed_by_name():
    e = {"trace": "t", "prompt": "x", "response": "y", "tool_input": "z", "tool_output": "w", "input_tokens": 5}
    assert redact(e) == {"trace": "t", "input_tokens": 5}
    assert redact(e, ("tool_input",)) == {"trace": "t", "tool_input": "z", "input_tokens": 5}
    assert redact(e, ("input_tokens",)) == {"trace": "t", "input_tokens": 5}
    assert redact({}) == {}


def test_e7_a_requests_trail_joins_the_events_of_every_component_in_time_order():
    events = [Event("r1", 30, "tool", "lookup done"), Event("r2", 10, "api", "other"), Event("r1", 10, "api", "received"), Event("r1", 20, "agent", "plan"), Event("r1", 20, "llm", "called")]
    assert listed(request_trail(events, "r1")) == ["api: received", "agent: plan", "llm: called", "tool: lookup done"]
    assert listed(request_trail(events, "r9")) == []
