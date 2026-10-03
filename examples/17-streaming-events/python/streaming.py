"""A streamed reply read three ways, from a scripted server-sent-event body.

The stream is an illustrative, hand-written sequence of events in the API's framing
(claude-sonnet-5-5): one text block, then one tool_use block whose input arrives in fragments.
"""
import anthropic
import httpx2

from harness import ScriptedTransport, sse_response

MODEL = "claude-sonnet-5-5"
PARAMS = dict(model=MODEL, max_tokens=128, messages=[{"role": "user", "content": "Weather in Paris?"}],
              tools=[{"name": "get_weather", "description": "Weather for a city.",
                      "input_schema": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}}])


def delta(index, kind, key, value):
    return {"type": "content_block_delta", "index": index, "delta": {"type": kind, key: value}}


EVENTS = [
    {"type": "message_start", "message": {"id": "msg_illustrative", "type": "message", "role": "assistant",
     "model": MODEL, "content": [], "stop_reason": None, "stop_sequence": None,
     "usage": {"input_tokens": 52, "output_tokens": 1}}},
    {"type": "content_block_start", "index": 0, "content_block": {"type": "text", "text": ""}},
    {"type": "ping"},
    delta(0, "text_delta", "text", "Let me "),
    delta(0, "text_delta", "text", "check."),
    {"type": "content_block_stop", "index": 0},
    {"type": "content_block_start", "index": 1,
     "content_block": {"type": "tool_use", "id": "toolu_illustrative_1", "name": "get_weather", "input": {}}},
    delta(1, "input_json_delta", "partial_json", ""),
    delta(1, "input_json_delta", "partial_json", '{"ci'),
    delta(1, "input_json_delta", "partial_json", 'ty": "Par'),
    delta(1, "input_json_delta", "partial_json", 'is"}'),
    {"type": "content_block_stop", "index": 1},
    {"type": "message_delta", "delta": {"stop_reason": "tool_use", "stop_sequence": None}, "usage": {"output_tokens": 38}},
    {"type": "message_stop"},
]
FAILING = EVENTS[:5] + [{"type": "error", "error": {"type": "overloaded_error", "message": "Overloaded"}}]


def run_length(names):
    """['a', 'b', 'b'] -> 'a, b x2'"""
    out = []
    for name in names:
        if out and out[-1][0] == name:
            out[-1][1] += 1
        else:
            out.append([name, 1])
    return ", ".join(n if c == 1 else f"{n} x{c}" for n, c in out)


def client_for(events):
    transport = ScriptedTransport(sse_response(events))
    return anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=transport)), transport


def main():
    client, transport = client_for(EVENTS)
    print("request sets stream:", end=" ")
    with client.messages.stream(**PARAMS) as stream:
        print(transport.requests[0]["stream"])
        print("text pieces:", list(stream.text_stream))
        final = stream.get_final_message()
    print("final stop_reason:", final.stop_reason, "| usage:", final.usage.input_tokens, "in,", final.usage.output_tokens, "out")
    print("blocks:", [b.type for b in final.content], "| tool input:", final.content[1].input)

    client, _ = client_for(EVENTS)
    print("raw events:", run_length(e.type for e in client.messages.create(**PARAMS, stream=True)))

    client, _ = client_for(FAILING)
    try:
        with client.messages.stream(**PARAMS) as stream:
            for _ in stream.text_stream:
                pass
    except anthropic.APIStatusError as err:
        print("mid-stream error:", type(err).__name__, err.body["error"]["type"] if isinstance(err.body, dict) and "error" in err.body else err.body)


if __name__ == "__main__":
    main()
