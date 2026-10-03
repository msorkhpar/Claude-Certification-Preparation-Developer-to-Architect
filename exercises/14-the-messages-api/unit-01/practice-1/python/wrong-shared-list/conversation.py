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
        if not isinstance(text, str) or not text.strip():
            raise ValueError("a turn needs text")
        self._history.append({"role": "user", "content": text})
        body = {"model": self.model, "max_tokens": self.max_tokens, "messages": self._history}
        if self.system and self.system.strip():
            body["system"] = self.system
        if self.stop_sequences:
            body["stop_sequences"] = list(self.stop_sequences)
        try:
            response = self.send(body)
        except Exception:
            self._history.pop()
            raise
        self._history.append({"role": "assistant", "content": response["content"]})
        usage = response.get("usage", {})
        self._totals["input_tokens"] += usage.get("input_tokens", 0)
        self._totals["output_tokens"] += usage.get("output_tokens", 0)
        reply_text = "".join(b.get("text", "") for b in response["content"] if b.get("type") == "text")
        stop = response.get("stop_reason")
        return Reply(reply_text, stop, stop == "max_tokens")

    def history(self):
        return copy.deepcopy(self._history)

    def totals(self):
        return dict(self._totals)

    def reset(self):
        self._history = []
        self._totals = {"input_tokens": 0, "output_tokens": 0}
