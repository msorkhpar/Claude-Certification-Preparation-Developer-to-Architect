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
    return status in RETRYABLE or status >= 500


def _spend_cap(response):
    details = ((response.body or {}).get("error") or {}).get("details") or {}
    return details.get("error_code") == "enforced_spend_limit_reached"


def _retryable(response):
    return _status_retryable(response.status) and not _spend_cap(response)


def _error_type(response):
    return ((response.body or {}).get("error") or {}).get("type", "unknown")


def _request_id(response):
    return response.headers.get("request-id")


def _failure(response, attempts):
    return CallFailed(response.status, _error_type(response), attempts, _request_id(response))


def _connection_failure(attempts):
    return CallFailed(0, "connection_error", attempts)


def _retry_after(response):
    try:
        return max(0.0, float(response.headers.get("retry-after")))
    except (TypeError, ValueError):
        return 0.0


def _delay(attempt, base_delay, cap, jitter):
    return jitter(min(cap, base_delay * 2 ** (attempt - 1)))


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
