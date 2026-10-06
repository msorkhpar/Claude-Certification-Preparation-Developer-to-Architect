"""A conversation client that keeps the state the API does not. See ../../statement.md for the contract."""
import copy
import logging
from dataclasses import dataclass

log = logging.getLogger(__name__)


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

    def _check_text(self, text):
        if not isinstance(text, str) or not text.strip():
            raise ValueError("a turn needs text")

    def _request_body(self):
        return {"model": self.model, "max_tokens": self.max_tokens, "messages": copy.deepcopy(self._history)}

    def _optional_fields(self, body):
        if self.system and self.system.strip():
            body["system"] = self.system
        if self.stop_sequences:
            body["stop_sequences"] = list(self.stop_sequences)

    def _send_or_roll_back(self, body):
        try:
            return self.send(body)
        except Exception:
            self._history.pop()
            raise

    def _assistant_turn(self, response):
        return {"role": "assistant", "content": response["content"]}

    def _add_usage(self, usage):
        self._totals["input_tokens"] += usage.get("input_tokens", 0)
        self._totals["output_tokens"] += usage.get("output_tokens", 0)

    def _reply(self, response):
        text = "".join(b.get("text", "") for b in response["content"] if b.get("type") == "text")
        stop = response.get("stop_reason")
        return Reply(text, stop, stop == "max_tokens")

    def say(self, text):
        log.debug("say input: %r", text)
        self._check_text(text)
        self._history.append({"role": "user", "content": text})
        body = self._request_body()
        self._optional_fields(body)
        response = self._send_or_roll_back(body)
        self._history.append(self._assistant_turn(response))
        self._add_usage(response.get("usage", {}))
        return self._reply(response)

    def history(self):
        return copy.deepcopy(self._history)

    def totals(self):
        return dict(self._totals)

    def reset(self):
        self._history = []
        self._totals = {"input_tokens": 0, "output_tokens": 0}
