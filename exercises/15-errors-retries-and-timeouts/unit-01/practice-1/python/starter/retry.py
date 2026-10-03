"""A retry policy for API calls. See ../../statement.md for the contract."""
from dataclasses import dataclass


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


def call_with_retry(send, sleep, max_attempts=4, base_delay=0.5, cap=8.0, jitter=lambda delay: delay):
    # TODO: call send() up to max_attempts times; sleep(delay) between attempts; return the first good Response.
    return None
