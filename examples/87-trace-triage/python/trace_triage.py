"""Observability decisions for a system of agents and tools: which traces to keep, how to find the layer that failed, when a change in a metric is drift, when to alert and what a log record may hold.

The traces, metrics and events are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domains 3 and 4), Anthropic's article on its multi-agent research system
and the Claude Code monitoring documentation, read on 2026-10-04. Nothing here calls a model.
"""
import logging
from collections import namedtuple

log = logging.getLogger(__name__)

Span = namedtuple("Span", "id parent kind name status ms note")
CONTENT = {"prompt", "response", "tool_input", "tool_output"}
TRACES = {
    "t-refund": [Span("s1", "", "agent", "orchestrator", "error", 9200, ""), Span("s2", "s1", "agent", "order-researcher", "error", 8700, ""), Span("s3", "s2", "llm", "plan", "ok", 900, ""),
                 Span("s4", "s2", "tool", "web_fetch", "error", 5000, ""), Span("s5", "s1", "llm", "summarise", "ok", 400, "")],
    "t-policy": [Span("s1", "", "agent", "assistant", "ok", 2100, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 120, "stale"), Span("s3", "s1", "llm", "answer", "ok", 1800, "")],
    "t-empty": [Span("s1", "", "agent", "assistant", "ok", 1500, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 90, "no-hits"), Span("s3", "s1", "llm", "answer", "ok", 1300, "")],
    "t-plain": [Span("s1", "", "agent", "assistant", "ok", 1900, ""), Span("s2", "s1", "retrieval", "policy_search", "ok", 110, ""), Span("s3", "s1", "llm", "answer", "ok", 1700, "")],
}


def bucket(trace_id):
    """A number from 0 to 99 that depends on the trace id alone, the same in every agent and every language."""
    h = 7
    for c in trace_id:
        h = (h * 31 + ord(c)) % 1000003
    return h % 100


def keep_reason(trace_id, spans, rate, feedback=False, slow_ms=5000):
    """Tail-based sampling: a trace with an error, a slow root or a bad-answer flag is always kept, and the rest are kept by their id at `rate` percent."""
    if any(s.status == "error" for s in spans):
        return "error"
    if spans[0].ms > slow_ms:
        return "slow"
    if feedback:
        return "feedback"
    return "sampled" if bucket(trace_id) < rate else "dropped"


def root_cause(spans):
    """The deepest failing span is the origin, not the span that reported the error; with no failure, a retrieval that returned stale or no chunks is blamed."""
    log.debug("root_cause input: %r", spans)
    by_id = {s.id: s for s in spans}
    failed = [s for s in spans if s.status == "error"]
    if failed:
        parents = {s.parent for s in failed}
        origin = next(s for s in failed if s.id not in parents)
        path, cursor = [], origin
        while cursor is not None:
            path.append(cursor.name)
            cursor = by_id.get(cursor.parent)
        return {"layer": origin.kind, "name": origin.name, "why": "failed", "path": path[::-1]}
    for s in spans:
        if s.kind == "retrieval" and s.note in ("stale", "no-hits"):
            return {"layer": "retrieval", "name": s.name, "why": s.note, "path": [spans[0].name, s.name]}
    return {"layer": "none", "name": "", "why": "no span failed", "path": []}


def drift(baseline, current, tolerance):
    """Metrics whose relative change since the baseline is over `tolerance` percent, in either direction."""
    out = []
    for name in sorted(baseline):
        base, now = baseline[name], current[name]
        pct = abs(now - base) * 100 // base if base else (100 if now else 0)
        if pct > tolerance:
            out.append(f"{name} {'up' if now > base else 'down'} {pct}%")
    return out


def alert_at(series, threshold, windows):
    """The index of the window that completes `windows` consecutive values over the threshold, or -1."""
    run = 0
    for i, value in enumerate(series):
        run = run + 1 if value > threshold else 0
        if run >= windows:
            return i
    return -1


def redact(event, allowed=()):
    """A log record keeps ids, counts and timings and drops the content fields unless they are allowed by name."""
    return {k: v for k, v in event.items() if k not in CONTENT or k in allowed}


def main():
    healthy = [f"trace-{i}" for i in range(100)]
    kept = sum(1 for t in healthy if keep_reason(t, TRACES["t-plain"], 10) == "sampled")
    print(f"100 healthy traces at a 10 percent rate: {kept} kept by id, the same {kept} in every agent: {kept == sum(1 for t in healthy if bucket(t) < 10)}")
    for name, spans in TRACES.items():
        cause = root_cause(spans)
        print(f"{name}: kept as {keep_reason(name, spans, 0)}; cause: " + " ".join(x for x in (cause["layer"], cause["name"], cause["why"]) if x))
    print("path of t-refund: " + " > ".join(root_cause(TRACES["t-refund"])["path"]))
    print("a bad-answer flag keeps t-plain at rate 0:", keep_reason("t-plain", TRACES["t-plain"], 0, feedback=True))
    drifted = drift({"retrieval_hits": 5, "refusals_per_1000": 4, "tokens_per_answer": 900, "tool_errors_per_1000": 12}, {"retrieval_hits": 3, "refusals_per_1000": 4, "tokens_per_answer": 1260, "tool_errors_per_1000": 13}, 25)
    print("drift against last week, tolerance 25%: " + ", ".join(drifted))
    series = [1, 2, 9, 2, 8, 9, 10, 3]
    print(f"error rate per window {series}, threshold 5: one window over fires at {alert_at(series, 5, 1)}, three in a row fire at {alert_at(series, 5, 3)}")
    event = {"trace": "t-1", "model": "claude-sonnet-5-5", "input_tokens": 1200, "output_tokens": 300, "tool": "lookup_order", "status": "ok", "prompt": "(text)", "tool_input": "(text)"}
    print("log record keeps: " + ", ".join(sorted(redact(event))) + "; with tool_input allowed by name: " + ", ".join(sorted(set(redact(event, ('tool_input',))) - set(redact(event)))))


if __name__ == "__main__":
    main()
