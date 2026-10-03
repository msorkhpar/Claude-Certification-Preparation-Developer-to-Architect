"""Assemble a streamed Messages reply from its events. See ../../statement.md for the contract."""
import json


class StreamError(Exception):
    """The stream carried an error event, or ended before message_stop (error_type "incomplete_stream")."""

    def __init__(self, error_type, message):
        super().__init__(f"{error_type}: {message}")
        self.error_type, self.message = error_type, message


def assemble(events):
    message, blocks, fragments, stopped = None, {}, {}, False
    for event in events:
        kind = event.get("type")
        if kind == "message_start":
            message = {k: v for k, v in event["message"].items() if k != "content"}
            message["content"] = []
            message["usage"] = dict(event["message"].get("usage", {}))
        elif kind == "content_block_start":
            blocks[event["index"]] = dict(event["content_block"])
            if event["content_block"].get("type") == "tool_use":
                fragments[event["index"]] = []
        elif kind == "content_block_delta":
            block, delta = blocks[event["index"]], event["delta"]
            if delta["type"] == "text_delta":
                block["text"] = block.get("text", "") + delta["text"]
            elif delta["type"] == "input_json_delta":
                fragments[event["index"]].append(delta["partial_json"])
            elif delta["type"] == "thinking_delta":
                block["thinking"] = block.get("thinking", "") + delta["thinking"]
            elif delta["type"] == "signature_delta":
                block["signature"] = delta["signature"]
        elif kind == "content_block_stop":
            index = event["index"]
            if index in fragments:
                joined = "".join(fragments[index])
                blocks[index]["input"] = json.loads(joined) if joined.strip() else {}
        elif kind == "message_delta":
            message["stop_reason"] = event["delta"].get("stop_reason")
            message["stop_sequence"] = event["delta"].get("stop_sequence")
            message["usage"].update(event.get("usage", {}))
        elif kind == "message_stop":
            stopped = True
        elif kind == "error":
            raise StreamError(event["error"].get("type", "unknown"), event["error"].get("message", ""))
        # ping and event types this client does not know are skipped: new types may be added
    if message is None or not stopped:
        raise StreamError("incomplete_stream", "the stream ended before message_stop")
    message["content"] = [blocks[i] for i in sorted(blocks)]
    return message
