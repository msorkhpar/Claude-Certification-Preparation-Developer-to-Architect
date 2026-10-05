"""Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md."""
import logging
from collections import namedtuple

log = logging.getLogger(__name__)

Span = namedtuple("Span", "id parent kind name status ms note")
Event = namedtuple("Event", "request ts component message")
CONTENT = {"prompt", "response", "tool_input", "tool_output"}


def bucket(trace_id):
    h = 7
    for c in trace_id:
        h = (h * 31 + ord(c)) % 1000003
    return h % 100


def keep_trace(trace_id, spans, rate, feedback=False, slow_ms=5000):
    log.debug("keep_trace input: %r", spans)
    """TODO 1 of 7 (unlocks m1 and e1): why a trace is kept.

    Receives the trace id, its spans (`status`, `ms`, `kind`, `name`; the first span is the root), the sampling rate (0 to 100) and the flags. Returns the first reason
    that applies, in this order: "error" when any span has the status "error"; "slow" when the root's `ms` is over `slow_ms` (equal is not slow); "retries" when
    one tool (a span of kind "tool") was called three times or more by name; "feedback" when `feedback` is set; otherwise "sampled" when `bucket(trace_id)` is
    below `rate` and "dropped" when it is not.
    Example: one tool span called "fetch" three times, nothing else wrong -> "retries"
    """
    return None


def _deepest(failed):
    """TODO 2 of 7 (unlocks e2): the span that is the origin of a failure.

    Receives the spans that failed (each has `id` and `parent`). Returns the first one that is not the parent of another failed span, so the error that a
    parent merely reported is passed over for the span below it.
    Example: s1 (root) <- s2 <- s3, all failed -> s3; two failed children of one parent -> the first of them
    """
    return failed[0]


def _blamed_retrieval(spans):
    """TODO 3 of 7 (unlocks e3): the retrieval span to blame when nothing failed.

    Receives the spans. Returns the first span of kind "retrieval" whose `note` is "stale" or "no-hits", or None when there is none.
    Example: a retrieval span with the note "stale" -> that span
    """
    return None


def root_cause(spans):
    by_id = {s.id: s for s in spans}
    failed = [s for s in spans if s.status == "error"]
    if failed:
        origin = _deepest(failed)
        path, cursor = [], origin
        while cursor is not None:
            path.append(cursor.name)
            cursor = by_id.get(cursor.parent)
        return {"layer": origin.kind, "name": origin.name, "why": "failed", "path": path[::-1]}
    s = _blamed_retrieval(spans)
    if s is not None:
        return {"layer": "retrieval", "name": s.name, "why": s.note, "path": [spans[0].name, s.name]}
    return {"layer": "none", "name": "", "why": "no span failed", "path": []}


def drift(baseline, current, tolerance):
    """TODO 4 of 7 (unlocks e4): the metrics that moved.

    Receives the baseline and the current values (metric name to a whole number) and a tolerance in percent. Goes through the baseline names in alphabetical order.
    The change is `abs(now - base) * 100 // base` (integer division); a baseline of 0 counts as 100 when the value is not 0 and 0 when it is. Returns one string
    "<name> up <pct>%" or "<name> down <pct>%" for each metric whose change is over the tolerance (equal is fine).
    Example: baseline 10, now 14, tolerance 30 -> ["a up 40%"]
    """
    return []


def alert_at(series, threshold, windows):
    """TODO 5 of 7 (unlocks e5): when an alert fires.

    Receives the series of window values, the threshold and how many windows in a row must be over it (strictly over). Returns the index of the window at which the
    count of consecutive windows over the threshold first reaches `windows`, or -1 when it never does. A window at or under the threshold starts the count again.
    Example: [1, 2, 9, 2, 8, 9, 10, 3], threshold 5, windows 3 -> 6
    """
    return -1


def redact(event, allowed=()):
    """TODO 6 of 7 (unlocks e6): what a log record keeps.

    Receives a record (a dict) and the names that may stay. Returns a new dict without the fields in CONTENT (`prompt`, `response`, `tool_input`, `tool_output`) unless the
    field's name is in `allowed`; every other field stays.
    Example: {"trace": "t", "prompt": "x"} -> {"trace": "t"}
    """
    return dict(event)


def request_trail(events, request):
    """TODO 7 of 7 (unlocks e7): one request's story.

    Receives the events of every component (`request`, `ts`, `component`, `message`) and a request id. Returns "<component>: <message>" for each event of that request, in
    order of `ts` (events with the same `ts` stay in the order they came); an unknown request gives an empty list.
    Example: events at ts 30 (tool), 10 (api), 20 (agent) -> ["api: ...", "agent: ...", "tool: ..."]
    """
    return []
