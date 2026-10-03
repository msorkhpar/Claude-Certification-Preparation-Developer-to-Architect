"""Assemble a streamed Messages reply from its events. See ../../statement.md for the contract."""
import json


class StreamError(Exception):
    """The stream carried an error event, or ended before message_stop (error_type "incomplete_stream")."""

    def __init__(self, error_type, message):
        super().__init__(f"{error_type}: {message}")
        self.error_type, self.message = error_type, message


def assemble(events):
    # TODO: fold the events into the message a non-streaming call would have returned.
    return {}
