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
        """TODO 1 of 8 (finish this to pass e6): refuse a blank turn before anything is sent.

        Receives the text of the turn. Raises ValueError when it is not a string, empty or only whitespace; otherwise returns nothing.
        Example: _check_text("   ") -> ValueError, _check_text("hi") -> None
        """

    def _request_body(self):
        """TODO 2 of 8 (finish this to pass m1 and e5): the request body, a snapshot.

        Receives nothing (it reads self). Returns a dict with `model`, `max_tokens` and `messages`, where `messages` is a deep copy of
        the whole history so far (the new user turn is already in it), so a later turn cannot change a request already sent.
        Example: after say("a"), the first body is {"model": ..., "max_tokens": ..., "messages": [{"role": "user", "content": "a"}]}
        """
        return {"model": self.model, "max_tokens": self.max_tokens, "messages": []}

    def _optional_fields(self, body):
        """TODO 3 of 8 (finish this to pass e4): the top-level fields that are only sometimes there.

        Receives the body dict and adds to it: `system` (a top-level field, never a message) when self.system is not blank, and
        `stop_sequences` (a copy of the list) when self.stop_sequences is given and not empty. Returns nothing.
        Example: with system="Be brief." the body gains {"system": "Be brief."}; with system="  " it gains nothing
        """

    def _send_or_roll_back(self, body):
        """TODO 4 of 8 (finish this to pass e2): send the body, and leave no dangling user turn when the call fails.

        Receives the body. Returns self.send(body). When send raises, removes the user turn that say added to self._history and raises
        the same exception again, so roles keep alternating on the next call.
        Example: a send that raises RuntimeError leaves the history as it was before say
        """
        return self.send(body)

    def _assistant_turn(self, response):
        """TODO 5 of 8 (finish this to pass m1): the turn to store for the reply.

        Receives the response dict. Returns {"role": "assistant", "content": <the response's content list, as received>}.
        Example: a response with content [{"type": "text", "text": "Paris."}] -> {"role": "assistant", "content": [that same list]}
        """
        return {"role": "assistant", "content": []}

    def _add_usage(self, usage):
        """TODO 6 of 8 (finish this to pass e1): keep the running totals.

        Receives the response's usage dict (it may lack a key). Adds its `input_tokens` and `output_tokens` to self._totals. Returns nothing.
        Example: totals {"input_tokens": 12, ...} plus usage {"input_tokens": 30, "output_tokens": 9} -> input_tokens 42
        """

    def _reply(self, response):
        """TODO 7 of 8 (finish this to pass m1 and e3): what say returns.

        Receives the response dict. Returns a Reply: the `text` of the content blocks whose `type` is "text" joined with nothing between
        them, the response's `stop_reason`, and `truncated`, true only when the stop reason is "max_tokens".
        Example: stop_reason "max_tokens" -> Reply(text, "max_tokens", True); "end_turn" -> truncated False
        """
        return Reply("", "", False)

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
        """TODO 8 of 8 (finish this to pass e5): the turns so far, as a copy.

        Returns a deep copy of self._history, so changing what the caller gets changes nothing here.
        Example: chat.history().append(x) leaves len(chat.history()) unchanged
        """
        return []

    def totals(self):
        return dict(self._totals)

    def reset(self):
        self._history = []
        self._totals = {"input_tokens": 0, "output_tokens": 0}
