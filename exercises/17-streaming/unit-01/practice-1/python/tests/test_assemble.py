import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from assemble import StreamError, assemble


def start(input_tokens=25, output_tokens=1):
    return {"type": "message_start", "message": {"id": "msg_x", "type": "message", "role": "assistant", "model": "claude-sonnet-5-5",
            "content": [], "stop_reason": None, "stop_sequence": None,
            "usage": {"input_tokens": input_tokens, "output_tokens": output_tokens}}}


def block_start(index, block):
    return {"type": "content_block_start", "index": index, "content_block": block}


def delta(index, kind, key, value):
    return {"type": "content_block_delta", "index": index, "delta": {"type": kind, key: value}}


def text(index, value):
    return delta(index, "text_delta", "text", value)


def stop(index):
    return {"type": "content_block_stop", "index": index}


def finish(reason="end_turn", output_tokens=15):
    return [{"type": "message_delta", "delta": {"stop_reason": reason, "stop_sequence": None}, "usage": {"output_tokens": output_tokens}},
            {"type": "message_stop"}]


TEXT_BLOCK = {"type": "text", "text": ""}


def error_of(fn):
    try:
        fn()
    except Exception as err:  # noqa: BLE001
        return err
    return None


def test_m1_text_deltas_are_joined_and_the_message_is_complete():
    events = [start(), block_start(0, TEXT_BLOCK), text(0, "Hel"), text(0, "lo, "), text(0, "world."), stop(0), *finish()]
    message = assemble(events)
    assert message.get("content") == [{"type": "text", "text": "Hello, world."}]
    assert (message.get("id"), message.get("role"), message.get("model")) == ("msg_x", "assistant", "claude-sonnet-5-5")
    assert (message.get("stop_reason"), message.get("stop_sequence")) == ("end_turn", None)


def test_e1_tool_input_is_the_fragments_joined_then_parsed_and_empty_input_is_an_empty_object():
    tool = {"type": "tool_use", "id": "toolu_1", "name": "get_weather", "input": {}}
    events = [start(), block_start(0, tool), delta(0, "input_json_delta", "partial_json", ""),
              delta(0, "input_json_delta", "partial_json", '{"ci'), delta(0, "input_json_delta", "partial_json", 'ty": "Pa'),
              delta(0, "input_json_delta", "partial_json", 'ris", "days": 3}'), stop(0),
              block_start(1, {**tool, "id": "toolu_2"}), stop(1), *finish("tool_use")]
    content = assemble(events).get("content")
    assert content == [{"type": "tool_use", "id": "toolu_1", "name": "get_weather", "input": {"city": "Paris", "days": 3}},
                       {"type": "tool_use", "id": "toolu_2", "name": "get_weather", "input": {}}]


def test_e2_ping_and_unknown_event_types_are_ignored():
    events = [start(), {"type": "ping"}, block_start(0, TEXT_BLOCK), {"type": "ping"}, text(0, "ok"),
              {"type": "some_future_event", "data": {"x": 1}}, stop(0), *finish()]
    assert assemble(events).get("content") == [{"type": "text", "text": "ok"}]


def test_e3_an_error_event_raises_with_its_type_and_message():
    events = [start(), block_start(0, TEXT_BLOCK), text(0, "partial"),
              {"type": "error", "error": {"type": "overloaded_error", "message": "Overloaded"}}]
    err = error_of(lambda: assemble(events))
    assert isinstance(err, StreamError), err
    assert (err.error_type, err.message) == ("overloaded_error", "Overloaded")


def test_e4_a_stream_that_ends_before_message_stop_is_an_error_not_a_short_message():
    events = [start(), block_start(0, TEXT_BLOCK), text(0, "cut off")]
    err = error_of(lambda: assemble(events))
    assert isinstance(err, StreamError), err
    assert err.error_type == "incomplete_stream"


def test_e5_usage_takes_input_tokens_from_the_start_and_the_cumulative_output_from_the_end():
    events = [start(input_tokens=52, output_tokens=1), block_start(0, TEXT_BLOCK), text(0, "x"), stop(0), *finish(output_tokens=38)]
    assert assemble(events).get("usage") == {"input_tokens": 52, "output_tokens": 38}


def test_e6_blocks_keep_their_index_order_and_thinking_fields_are_assembled():
    events = [start(), block_start(0, {"type": "thinking", "thinking": ""}),
              delta(0, "thinking_delta", "thinking", "Let me "), delta(0, "thinking_delta", "thinking", "think."),
              delta(0, "signature_delta", "signature", "sig-abc"), stop(0),
              block_start(1, TEXT_BLOCK), text(1, "Answer."), stop(1), *finish()]
    content = assemble(events).get("content")
    assert content == [{"type": "thinking", "thinking": "Let me think.", "signature": "sig-abc"},
                       {"type": "text", "text": "Answer."}]
