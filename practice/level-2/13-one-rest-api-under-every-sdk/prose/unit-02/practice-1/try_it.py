"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

import json

from raw_client import ApiError, Response, send_messages, text_of

# A stand-in transport, like the one the tests use: it records the request and answers with a fixed reply.
sent = []


def transport(request):
    sent.append(request)
    body = {"content": [{"type": "text", "text": "Paris."}], "stop_reason": "end_turn",
            "usage": {"input_tokens": 9, "output_tokens": 3}}
    return Response(200, {}, json.dumps(body))


messages = [{"role": "user", "content": "Capital of France?"}]
message = send_messages(transport, "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64, "Be brief.")

print("requests sent:", len(sent))
print("method and url:", sent[0].method if sent else None, sent[0].url if sent else None)
print("header names:", sorted(sent[0].headers) if sent else None)
print("reply text:", text_of(message) if message else None)

# An error reply becomes an ApiError.
try:
    send_messages(lambda r: Response(429, {"request-id": "req_1"}, json.dumps(
        {"type": "error", "error": {"type": "rate_limit_error", "message": "Rate limited"}})),
        "sk-test-0123456789abcdef", "claude-sonnet-5-5", messages, 64)
except ApiError as err:
    print("api error:", err.status, err.error_type, err.request_id)
