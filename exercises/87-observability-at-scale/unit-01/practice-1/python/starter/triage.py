"""Trace triage: which traces to keep, the layer that failed, drift, alerts, redaction and one request's trail. See ../../statement.md."""
from collections import namedtuple

Span = namedtuple("Span", "id parent kind name status ms note")
Event = namedtuple("Event", "request ts component message")
CONTENT = {"prompt", "response", "tool_input", "tool_output"}


def bucket(trace_id):
    """A number from 0 to 99 that depends on the trace id alone (written for you)."""
    h = 7
    for c in trace_id:
        h = (h * 31 + ord(c)) % 1000003
    return h % 100


def keep_trace(trace_id, spans, rate, feedback=False, slow_ms=5000):
    # TODO: error, slow, retries, feedback, sampled or dropped, in that order of priority.
    return None


def root_cause(spans):
    # TODO: {"layer", "name", "why", "path"} of the deepest failing span, or of a stale or empty retrieval when nothing failed.
    return None


def drift(baseline, current, tolerance):
    # TODO: "<name> up|down <percent>%" for each metric that moved by more than the tolerance, sorted by name.
    return None


def alert_at(series, threshold, windows):
    # TODO: the index that completes `windows` consecutive values over the threshold, or -1.
    return None


def redact(event, allowed=()):
    # TODO: drop the content fields unless they are allowed by name.
    return None


def request_trail(events, request):
    # TODO: "<component>: <message>" for each event of the request, in time order.
    return None
