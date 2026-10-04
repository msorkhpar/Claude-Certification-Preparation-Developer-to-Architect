"""Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md."""
from collections import namedtuple

Span = namedtuple("Span", "id parent kind name status ms note")
Event = namedtuple("Event", "request ts component message")
CONTENT = {"prompt", "response", "tool_input", "tool_output"}


def bucket(trace_id):
    h = 7
    for c in trace_id:
        h = (h * 31 + ord(c)) % 1000003
    return h % 100


def keep_trace(trace_id, spans, rate, feedback=False, slow_ms=5000):
    if any(s.status == "error" for s in spans):
        return "error"
    if spans[0].ms > slow_ms:
        return "slow"
    tools = [s.name for s in spans if s.kind == "tool"]
    if any(tools.count(n) >= 3 for n in tools):
        return "retries"
    if feedback:
        return "feedback"
    return "sampled" if bucket(trace_id) < rate else "dropped"


def root_cause(spans):
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
    out = []
    for name in sorted(baseline):
        base, now = baseline[name], current[name]
        pct = abs(now - base) * 100 // base if base else (100 if now else 0)
        if pct > tolerance:
            out.append(f"{name} {'up' if now > base else 'down'} {pct}%")
    return out


def alert_at(series, threshold, windows):
    run = 0
    for i, value in enumerate(series):
        run = run + 1 if value > threshold else 0
        if run >= windows:
            return i
    return -1


def redact(event, allowed=()):
    return {k: v for k, v in event.items() if k not in CONTENT or k in allowed}


def request_trail(events, request):
    return [f"{e.component}: {e.message}" for e in sorted((e for e in events if e.request == request), key=lambda e: e.ts)]
