"""A retry policy for API calls. See ../../statement.md for the contract."""
import logging
from dataclasses import dataclass

log = logging.getLogger(__name__)


@dataclass
class Response:
    status: int
    headers: dict  # lower-case names
    body: dict  # the parsed JSON body, or {} when there is none


class TransportError(Exception):
    """The connection failed or timed out: no reply was received."""


class CallFailed(Exception):
    """The call did not succeed. error_type is the API's error type, "connection_error" or "unknown"."""

    def __init__(self, status, error_type, attempts, request_id=None):
        super().__init__(f"{status} {error_type} after {attempts} attempt(s)")
        self.status, self.error_type, self.attempts, self.request_id = status, error_type, attempts, request_id


RETRYABLE = {408, 409, 429}


def _status_retryable(status):
    """TODO 1 of 7 (finish this to pass m1 and e1): may a reply with this status be tried again?

    Receives the HTTP status. Returns True for the statuses in RETRYABLE (408, 409, 429) and for every status of 500 and above (529
    included); False for every other one.
    Example: _status_retryable(529) -> True, _status_retryable(404) -> False
    """
    return False


def _spend_cap(response):
    """TODO 2 of 7 (finish this to pass e4): is this 429 a spend cap rather than a rate limit?

    Receives the Response. Returns True when its body's error.details.error_code is "enforced_spend_limit_reached" (any level may be
    missing); retrying cannot succeed then.
    Example: a body {"error": {"details": {"error_code": "enforced_spend_limit_reached"}}} -> True; {} -> False
    """
    return False


def _retryable(response):
    return _status_retryable(response.status) and not _spend_cap(response)


def _error_type(response):
    """TODO 3 of 7 (finish this to pass e1, e4 and e6): the API's error type of a reply.

    Receives the Response. Returns body.error.type, or "unknown" when the body has none.
    Example: a body {"error": {"type": "overloaded_error"}} -> "overloaded_error"; {} -> "unknown"
    """
    return ""


def _request_id(response):
    """TODO 4 of 7 (finish this to pass e1, e4 and e6): the request id of a reply.

    Receives the Response. Returns its `request-id` header (header names are lower case), or None when there is none.
    Example: headers {"request-id": "req_1"} -> "req_1"; {} -> None
    """
    return None


def _failure(response, attempts):
    return CallFailed(response.status, _error_type(response), attempts, _request_id(response))


def _connection_failure(attempts):
    """TODO 5 of 7 (finish this to pass e5): the failure when the last attempt lost its connection.

    Receives the number of attempts made. Returns a CallFailed with status 0, error type "connection_error", that many attempts and no
    request id.
    Example: _connection_failure(2) -> CallFailed(0, "connection_error", 2)
    """
    return CallFailed(0, "", attempts)


def _retry_after(response):
    """TODO 6 of 7 (finish this to pass e2): the wait the server asked for.

    Receives the Response. Returns its `retry-after` header as seconds (a float, never below 0); 0.0 when the header is absent or not a
    number.
    Example: headers {"retry-after": "3"} -> 3.0; {} -> 0.0; {"retry-after": "soon"} -> 0.0
    """
    return 0.0


def _delay(attempt, base_delay, cap, jitter):
    """TODO 7 of 7 (finish this to pass m1, e3 and e5): the wait before the next attempt.

    Receives the attempt that just failed (1 for the first), base_delay, cap and the jitter function. Returns jitter(min(cap, base_delay
    times 2 to the power attempt - 1)): the jitter is applied last, after the cap.
    Example: _delay(3, 1.0, 5.0, lambda d: d) -> 4.0, _delay(4, 1.0, 5.0, lambda d: d) -> 5.0
    """
    return 0.0


def call_with_retry(send, sleep, max_attempts=4, base_delay=0.5, cap=8.0, jitter=lambda delay: delay):
    log.debug("call_with_retry input: max_attempts=%r base_delay=%r cap=%r", max_attempts, base_delay, cap)
    attempt = 0
    while True:
        attempt += 1
        try:
            response = send()
        except TransportError:
            if attempt >= max_attempts:
                raise _connection_failure(attempt)
            wait = 0.0
        else:
            if response.status < 400:
                return response
            if not _retryable(response):
                raise _failure(response, attempt)
            if attempt >= max_attempts:
                raise _failure(response, attempt)
            wait = _retry_after(response)
        sleep(max(_delay(attempt, base_delay, cap, jitter), wait))
