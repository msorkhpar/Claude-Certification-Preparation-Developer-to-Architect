import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from retry import CallFailed, Response, TransportError, call_with_retry

OK = Response(200, {}, {"type": "message"})


def status(code, headers=None, kind=None, details=None):
    error = {"type": kind or "api_error", "message": "x"}
    if details:
        error["details"] = details
    return Response(code, headers or {}, {"type": "error", "error": error})


class Sender:
    """A scripted send(): replies in order (an exception is raised); counts the calls."""

    def __init__(self, *replies):
        self.replies, self.calls = list(replies), 0

    def __call__(self):
        self.calls += 1
        item = self.replies.pop(0)
        if isinstance(item, Exception):
            raise item
        return item


def run(*replies, **options):
    sender, slept = Sender(*replies), []
    try:
        result = call_with_retry(sender, slept.append, **options)
        return sender, slept, result, None
    except Exception as err:  # noqa: BLE001
        return sender, slept, None, err


def test_m1_overloaded_twice_then_success_returns_the_good_reply_after_two_waits():
    sender, slept, result, err = run(status(529, kind="overloaded_error"), status(503), OK)
    assert err is None and result is OK
    assert sender.calls == 3
    assert slept == [0.5, 1.0]


def test_e1_client_errors_are_not_retried():
    for code, kind in ((400, "invalid_request_error"), (401, "authentication_error"), (404, "not_found_error"), (413, "request_too_large")):
        sender, slept, _, err = run(status(code, {"request-id": "req_1"}, kind), OK)
        assert isinstance(err, CallFailed), (code, err)
        assert (err.status, err.error_type, err.attempts, err.request_id) == (code, kind, 1, "req_1")
        assert sender.calls == 1 and slept == []


def test_e2_retry_after_is_a_floor_for_the_wait():
    _, slept, _, err = run(status(429, {"retry-after": "3"}, "rate_limit_error"), status(429, {"retry-after": "1"}, "rate_limit_error"),
                           status(529, {"retry-after": "1"}), OK, max_attempts=5)
    assert err is None
    assert slept == [3.0, 1.0, 2.0]  # waits: max(0.5, 3), max(1, 1), max(2, 1)


def test_e3_delay_doubles_up_to_the_cap_and_the_jitter_is_applied_last():
    failures = [status(500) for _ in range(6)]
    _, slept, _, err = run(*failures, OK, max_attempts=7, base_delay=1.0, cap=5.0)
    assert err is None and slept == [1.0, 2.0, 4.0, 5.0, 5.0, 5.0]
    _, halved, _, _ = run(status(500), status(500), OK, jitter=lambda d: d / 2)
    assert halved == [0.25, 0.5]


def test_e4_a_spend_cap_429_is_not_retried():
    cap = status(429, {"request-id": "req_cap"}, "rate_limit_error", {"error_code": "enforced_spend_limit_reached"})
    sender, slept, _, err = run(cap, OK)
    assert isinstance(err, CallFailed) and err.status == 429 and err.attempts == 1 and err.request_id == "req_cap"
    assert sender.calls == 1 and slept == []


def test_e5_connection_errors_are_retried_like_server_errors():
    sender, slept, result, err = run(TransportError("reset"), TransportError("timeout"), OK)
    assert err is None and result is OK and sender.calls == 3 and slept == [0.5, 1.0]
    _, _, _, err = run(TransportError("a"), TransportError("b"), max_attempts=2)
    assert isinstance(err, CallFailed)
    assert (err.status, err.error_type, err.attempts, err.request_id) == (0, "connection_error", 2, None)


def test_e6_giving_up_reports_the_last_reply_and_does_not_wait_after_the_last_attempt():
    sender, slept, _, err = run(status(500, {"request-id": "req_a"}), status(503, {"request-id": "req_b"}),
                                status(529, {"request-id": "req_c"}, "overloaded_error"), OK, max_attempts=3)
    assert isinstance(err, CallFailed), err
    assert sender.calls == 3 and len(slept) == 2
    assert (err.status, err.error_type, err.attempts, err.request_id) == (529, "overloaded_error", 3, "req_c")
