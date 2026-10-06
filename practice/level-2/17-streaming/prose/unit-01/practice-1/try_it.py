"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from assemble import StreamError, assemble

# The parsed `data:` events of one streamed reply, like the ones the tests build: start, one text block in three deltas, finish.
events = [
    {"type": "message_start", "message": {"id": "msg_x", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
                                          "content": [], "stop_reason": None, "stop_sequence": None,
                                          "usage": {"input_tokens": 25, "output_tokens": 1}}},
    {"type": "content_block_start", "index": 0, "content_block": {"type": "text", "text": ""}},
    {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "Hel"}},
    {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "lo, "}},
    {"type": "content_block_delta", "index": 0, "delta": {"type": "text_delta", "text": "world."}},
    {"type": "content_block_stop", "index": 0},
    {"type": "message_delta", "delta": {"stop_reason": "end_turn", "stop_sequence": None}, "usage": {"output_tokens": 15}},
    {"type": "message_stop"},
]

try:
    message = assemble(events) or {}
    print("content:", message.get("content"))
    print("stop reason:", message.get("stop_reason"))
    print("usage:", message.get("usage"))
except StreamError as err:
    print("stream error:", err)
