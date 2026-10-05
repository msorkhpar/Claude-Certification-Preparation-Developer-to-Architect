"""One Messages call written by hand with httpx2, then the same call through the SDK.

Both go through the same scripted transport, so nothing leaves the container. The reply is an
illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
"""
import logging
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
URL = "https://api.anthropic.com/v1/messages"
PAYLOAD = {"model": MODEL, "max_tokens": 64, "messages": [{"role": "user", "content": "Capital of France?"}]}
OK = message([text("Paris.")])
LIMITED = (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "Rate limited"},
                 "request_id": "req_illustrative_0001"}, {"retry-after": "7", "request-id": "req_illustrative_0001"})


def raw_call(transport):
    """The HTTP request the SDK would build, written out: three headers and a JSON body."""
    headers = {"x-api-key": "placeholder", "anthropic-version": "2023-06-01", "content-type": "application/json"}
    with httpx2.Client(transport=transport) as http:
        return http.post(URL, headers=headers, json=PAYLOAD)


def sdk_call(transport):
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0,
                                 http_client=httpx2.Client(transport=transport))
    return client.messages.create(**PAYLOAD)


def main():
    t_raw, t_sdk = ScriptedTransport(OK, LIMITED), ScriptedTransport(OK, LIMITED)

    reply = raw_call(t_raw)
    print("raw :", reply.request.method, t_raw.urls[0], "->", reply.status_code, repr(reply.json()["content"][0]["text"]))
    message_ = sdk_call(t_sdk)
    print("sdk :", "POST", t_sdk.urls[0], "->", "200", repr(message_.content[0].text))
    print("same URL:", t_raw.urls[0] == t_sdk.urls[0], "| same body:", t_raw.requests[0] == t_sdk.requests[0])
    for name in ("anthropic-version", "content-type"):
        print(f"{name}: raw {t_raw.headers[0][name]} | sdk {t_sdk.headers[0][name]}")
    print("headers only the SDK adds:", ", ".join(sorted(set(t_sdk.headers[0]) - set(t_raw.headers[0]))))

    limited = raw_call(t_raw)
    print("raw 429 :", limited.status_code, limited.json()["error"]["type"], "retry-after", limited.headers["retry-after"])
    try:
        sdk_call(t_sdk)
    except anthropic.RateLimitError as err:
        print("sdk 429 :", type(err).__name__, err.status_code, "request id", err.request_id)


if __name__ == "__main__":
    main()
