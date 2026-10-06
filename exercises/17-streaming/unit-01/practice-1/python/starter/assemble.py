"""Assemble a streamed Messages reply from its events. See ../../statement.md for the contract."""
import json
import logging

log = logging.getLogger(__name__)


class StreamError(Exception):
    """The stream carried an error event, or ended before message_stop (error_type "incomplete_stream")."""

    def __init__(self, error_type, message):
        super().__init__(f"{error_type}: {message}")
        self.error_type, self.message = error_type, message


def _start_message(start):
    """TODO 1 of 6 (finish this to pass m1 and e5): the message a message_start event begins.

    Receives the `message` object of the event. Returns a new dict with its fields except `content`, then `content` as an empty list and `usage` as a COPY of its usage.
    Example: _start_message({"id": "m", "content": [], "usage": {"input_tokens": 5}}) -> {"id": "m", "content": [], "usage": {"input_tokens": 5}}
    """
    return {}


def _apply_delta(block, pieces, delta):
    """TODO 2 of 6 (finish this to pass m1 and e6): fold one content_block_delta into its block.

    Receives the block (a dict), `pieces` (the fragment list of a tool_use block, else None) and the `delta`. `text_delta` appends `text` to the block's text,
    `thinking_delta` appends `thinking`, `signature_delta` sets `signature`, `input_json_delta` appends `partial_json` to `pieces`. Returns nothing.
    Example: block {"text": "He"}, delta {"type": "text_delta", "text": "llo"} -> block {"text": "Hello"}
    """


def _finish_block(block, pieces):
    """TODO 3 of 6 (finish this to pass e1): at content_block_stop, give a tool_use block its input.

    Receives the block and its fragment list `pieces` (None for a block that is not tool_use). When `pieces` is a list, sets block["input"] to the fragments joined and
    parsed as JSON, or to {} when the joined text is empty or only white space. Returns nothing.
    Example: pieces ['{"ci', 'ty": "Pa', 'ris"}'] -> block["input"] == {"city": "Paris"}; pieces [""] -> {}
    """


def _apply_message_delta(message, event):
    """TODO 4 of 6 (finish this to pass m1 and e5): fold a message_delta event into the message.

    Receives the message and the event. Sets message["stop_reason"] and ["stop_sequence"] from event["delta"], and each key of event["usage"] replaces the same key in
    message["usage"] (the output count is cumulative: replace, do not add). Returns nothing.
    Example: usage {"input_tokens": 52, "output_tokens": 1} and event usage {"output_tokens": 38} -> {"input_tokens": 52, "output_tokens": 38}
    """


def _raise_error(event):
    """TODO 5 of 6 (finish this to pass e3): an error event ends the assembly.

    Receives the event, whose "error" holds a "type" and a "message". Raises StreamError(type, message).
    Example: {"error": {"type": "overloaded_error", "message": "Overloaded"}} raises StreamError("overloaded_error", "Overloaded")
    """


def _check_complete(message, stopped):
    """TODO 6 of 6 (finish this to pass e4): a stream that never started or never reached message_stop is an error.

    Receives the message (None before message_start) and whether message_stop was seen. Raises StreamError("incomplete_stream", ...) unless both hold.
    Example: _check_complete({...}, False) raises StreamError with error_type "incomplete_stream"
    """


def assemble(events):
    log.debug("assemble input: %r", events)
    message, blocks, fragments, stopped = None, {}, {}, False
    for event in events:
        kind = event.get("type")
        if kind == "message_start":
            message = _start_message(event["message"])
        elif kind == "content_block_start":
            blocks[event["index"]] = dict(event["content_block"])
            if event["content_block"].get("type") == "tool_use":
                fragments[event["index"]] = []
        elif kind == "content_block_delta":
            _apply_delta(blocks[event["index"]], fragments.get(event["index"]), event["delta"])
        elif kind == "content_block_stop":
            _finish_block(blocks[event["index"]], fragments.get(event["index"]))
        elif kind == "message_delta":
            _apply_message_delta(message, event)
        elif kind == "message_stop":
            stopped = True
        elif kind == "error":
            _raise_error(event)
        # ping and event types this client does not know are skipped: new types may be added
    _check_complete(message, stopped)
    message["content"] = [blocks[i] for i in sorted(blocks)]
    return message
