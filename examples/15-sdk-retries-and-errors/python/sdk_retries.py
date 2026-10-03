"""What the SDK retries on its own, and what it does not, against a scripted transport.

The failures are illustrative, hand-written replies shaped like the API's error bodies.
The SDK sleeps a short exponential back-off between attempts (about 0.5 s, then about 1 s).
"""
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

MODEL = "claude-sonnet-5-5"
PARAMS = dict(model=MODEL, max_tokens=16, messages=[{"role": "user", "content": "Hi"}])


def error(status, kind, request_id):
    body = {"type": "error", "error": {"type": kind, "message": kind}, "request_id": request_id}
    return (status, body, {"request-id": request_id})


OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529")
BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400")
SPEND_CAP = (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "monthly limit reached",
                                              "details": {"error_code": "enforced_spend_limit_reached"}},
                   "request_id": "req_illustrative_0429"}, {"request-id": "req_illustrative_0429"})


def client_for(transport, max_retries):
    return anthropic.Anthropic(api_key="placeholder", max_retries=max_retries,
                               http_client=httpx2.Client(transport=transport))


def attempt(label, script, max_retries):
    transport = ScriptedTransport(*script)
    try:
        reply = client_for(transport, max_retries).messages.create(**PARAMS)
        outcome = f"ok {reply.content[0].text!r}"
    except anthropic.APIStatusError as err:
        outcome = f"{type(err).__name__} {err.status_code} {err.body['error']['type']} request id {err.request_id}"
    except anthropic.APIConnectionError as err:
        outcome = type(err).__name__
    counts = [h["x-stainless-retry-count"] for h in transport.headers]
    print(f"{label}: {len(transport.requests)} request(s), retry-count header {counts} -> {outcome}")


def main():
    attempt("529, 529, then 200, max_retries=2", [OVERLOADED, OVERLOADED, message([text("Hello.")])], 2)
    attempt("529, 529, then 200, max_retries=0", [OVERLOADED, OVERLOADED, message([text("Hello.")])], 0)
    attempt("400 is never retried, max_retries=2", [BAD_REQUEST, message([text("Hello.")])], 2)
    attempt("spend-cap 429, max_retries=2", [SPEND_CAP] * 3, 2)
    attempt("timeout twice, max_retries=1", [httpx2.ReadTimeout("scripted"), httpx2.ReadTimeout("scripted")], 1)


if __name__ == "__main__":
    main()
