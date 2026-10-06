"""Reading a trace: where did it fail, in the integration or in the model, and what should happen next?

The Claude documentation on API errors and on stop reasons (read on 2026-10-03) lists the error types and says that a stop reason is part of
a successful response ("Response contains valid content") while an error is a 4xx or 5xx status. It also says that adding text right after
a tool result can make Claude end its turn with an empty reply. This file reads three hand-written traces, each a list of events, and names
the first failure, its origin and the next action. The traces are scripted and carry no live output.
"""
import logging
import json

log = logging.getLogger(__name__)

ORIGIN = {"invalid_request_error": "integration", "authentication_error": "account", "rate_limit_error": "service", "api_error": "service",
          "overloaded_error": "service", "timeout_error": "service"}
NEXT = {"invalid_request_error": "fix the request, do not retry", "authentication_error": "fix the credential", "rate_limit_error": "wait, then retry",
        "api_error": "retry with back-off", "overloaded_error": "retry with back-off", "timeout_error": "stream the request"}

TRACES = {
    "A: a tool loop that ends in silence": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "tool_use", "content": [{"type": "tool_use"}]},
        {"kind": "request", "last_user_blocks": ["tool_result", "text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": []},
    ],
    "B: a busy service and a retry": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "error", "status": 529, "error_type": "overloaded_error"},
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": [{"type": "text"}]},
    ],
    "C: JSON in a code fence": [
        {"kind": "request", "last_user_blocks": ["text"]},
        {"kind": "response", "status": 200, "stop_reason": "end_turn", "content": [{"type": "text"}]},
        {"kind": "parse", "ok": False, "text": '```json\n{"label": "spam"}\n```'},
    ],
}


def first_failure(trace):
    """(index, what, origin, next action) of the first failing event, or None."""
    last_blocks = []
    for i, e in enumerate(trace):
        if e["kind"] == "request":
            last_blocks = e["last_user_blocks"]
        elif e["kind"] == "error":
            return i, e["error_type"], ORIGIN[e["error_type"]], NEXT[e["error_type"]]
        elif e["kind"] == "response" and e["stop_reason"] == "end_turn" and not e["content"]:
            if "tool_result" in last_blocks and "text" in last_blocks[last_blocks.index("tool_result"):]:
                return i, "empty reply", "integration", "send the tool result alone, with no text after it"
            return i, "empty reply", "model", "add a new user message that asks it to continue"
        elif e["kind"] == "parse" and not e["ok"]:
            start, end = e["text"].find("{"), e["text"].rfind("}")
            try:
                json.loads(e["text"][start:end + 1])
                return i, "parse failure", "integration", "extract the JSON object before parsing"
            except ValueError:
                return i, "parse failure", "model", "validate the output and retry"
    return None


def main():
    for name, trace in TRACES.items():
        found = first_failure(trace)
        recovered = found is not None and any(e["kind"] == "response" and e["stop_reason"] == "end_turn" and e["content"] for e in trace[found[0] + 1:])
        print(name)
        print(f"  first failure: event {found[0]}, {found[1]}; origin: {found[2]}; next: {found[3]}; recovered later: {recovered}")


if __name__ == "__main__":
    main()
