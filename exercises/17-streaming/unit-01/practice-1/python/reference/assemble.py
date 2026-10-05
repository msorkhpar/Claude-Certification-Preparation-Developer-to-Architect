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
    """The message a message_start event begins: its fields except content, an empty content list and a copy of its usage."""
    message = {k: v for k, v in start.items() if k != "content"}
    message["content"] = []
    message["usage"] = dict(start.get("usage", {}))
    return message


def _apply_delta(block, pieces, delta):
    """Fold one content_block_delta into its block; `pieces` is the fragment list of a tool_use block."""
    if delta["type"] == "text_delta":
        block["text"] = block.get("text", "") + delta["text"]
    elif delta["type"] == "input_json_delta":
        pieces.append(delta["partial_json"])
    elif delta["type"] == "thinking_delta":
        block["thinking"] = block.get("thinking", "") + delta["thinking"]
    elif delta["type"] == "signature_delta":
        block["signature"] = delta["signature"]


def _finish_block(block, pieces):
    """At content_block_stop: a tool_use block (it has `pieces`) gets its input, the fragments joined and parsed, {} when empty."""
    if pieces is not None:
        joined = "".join(pieces)
        block["input"] = json.loads(joined) if joined.strip() else {}


def _apply_message_delta(message, event):
    """Fold a message_delta event: the stop reason and sequence, and each usage key replaces the same key (the output count is cumulative)."""
    message["stop_reason"] = event["delta"].get("stop_reason")
    message["stop_sequence"] = event["delta"].get("stop_sequence")
    message["usage"].update(event.get("usage", {}))


def _raise_error(event):
    """An error event ends the assembly with a StreamError carrying the error's type and message."""
    raise StreamError(event["error"].get("type", "unknown"), event["error"].get("message", ""))


def _check_complete(message, stopped):
    """A stream that never started or never reached message_stop is an error, not a short message."""
    if message is None or not stopped:
        raise StreamError("incomplete_stream", "the stream ended before message_stop")


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
