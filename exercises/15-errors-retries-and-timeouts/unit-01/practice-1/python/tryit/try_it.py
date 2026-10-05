"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from retry import CallFailed, Response, call_with_retry


def failure(code, kind="api_error"):
    return Response(code, {}, {"type": "error", "error": {"type": kind, "message": "x"}})


# A scripted send(), like the one the tests use: overloaded twice, then a good reply.
replies = [failure(529, "overloaded_error"), failure(503), Response(200, {}, {"type": "message"})]
calls = []


def send():
    calls.append(1)
    return replies.pop(0)


# The sleep is injected, so nothing really waits: it just records the delays asked for.
waits = []
try:
    result = call_with_retry(send, waits.append)
    print("final status:", result.status if result else None)
except CallFailed as err:
    print("gave up:", err)
print("calls made:", len(calls))
print("waits requested:", waits)
