"""A conversation client that keeps the state the API does not. See ../../statement.md for the contract."""
import copy
from dataclasses import dataclass


@dataclass
class Reply:
    text: str
    stop_reason: str
    truncated: bool


class Conversation:
    def __init__(self, send, model, max_tokens, system=None, stop_sequences=None):
        self.send, self.model, self.max_tokens = send, model, max_tokens
        self.system, self.stop_sequences = system, stop_sequences
        self._history = []
        self._totals = {"input_tokens": 0, "output_tokens": 0}

    def say(self, text):
        # TODO: add the user turn, send the whole history, keep the assistant turn, count usage.
        return Reply("", "", False)

    def history(self):
        return []

    def totals(self):
        return dict(self._totals)

    def reset(self):
        self._history = []
        self._totals = {"input_tokens": 0, "output_tokens": 0}
