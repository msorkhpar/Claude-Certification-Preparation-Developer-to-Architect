"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from triage import Event, Span, keep_trace, redact, request_trail, root_cause

# One trace of a request: the assistant calls a search, then the model answers slowly.
spans = [Span("s1", "", "agent", "assistant", "ok", 1900, ""),
         Span("s2", "s1", "retrieval", "search", "ok", 100, ""),
         Span("s3", "s1", "llm", "answer", "ok", 1700, "")]
print("kept as:", keep_trace("trace-1", spans, 0))
print("kept as (a failed span):", keep_trace("trace-1", [Span("s1", "", "agent", "assistant", "error", 90, "timeout")], 0))

# Where the failure started, from the deepest failed span.
failed = [Span("s1", "", "agent", "assistant", "error", 900, ""), Span("s2", "s1", "tool", "lookup_order", "error", 800, "HTTP 500")]
print("root cause:", root_cause(failed))

# A log event without content, and the trail of one request across components.
print("redacted:", redact({"trace": "t", "tool_input": "secret", "input_tokens": 5}))
events = [Event("r1", 30, "tool", "lookup done"), Event("r2", 10, "api", "other"), Event("r1", 10, "api", "received")]
print("trail:", request_trail(events, "r1"))
