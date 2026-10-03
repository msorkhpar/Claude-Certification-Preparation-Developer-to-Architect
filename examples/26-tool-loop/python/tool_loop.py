"""A tool loop on the official SDK, against a scripted model: parallel calls, one failing tool and a forced choice.

The replies are illustrative, hand-written bodies in the shape of the Messages API (claude-sonnet-5-5), not captures.
"""
import json

from harness import scripted_client
from harness.scripted import message, text, tool_use

MODEL = "claude-sonnet-5-5"
TOOLS = [
    {"name": "get_weather", "description": "Current weather for one city. Use it when the user asks about weather now. Returns a short sentence; it knows nothing about forecasts.",
     "input_schema": {"type": "object", "properties": {"city": {"type": "string", "description": "City name, for example Oslo"}}, "required": ["city"]}},
    {"name": "get_time", "description": "Local time for one city, as HH:MM on a 24 hour clock. Use it when the user asks what time it is somewhere.",
     "input_schema": {"type": "object", "properties": {"city": {"type": "string", "description": "City name, for example Oslo"}}, "required": ["city"]}},
]
HANDLERS = {"get_weather": lambda a: {"Oslo": "Oslo: 4 C, light rain"}[a["city"]], "get_time": lambda a: {"Oslo": "09:15", "Rome": "09:15"}[a["city"]]}


def run_tool(block):
    try:
        return {"type": "tool_result", "tool_use_id": block.id, "content": HANDLERS[block.name](block.input)}
    except KeyError as err:
        return {"type": "tool_result", "tool_use_id": block.id, "content": f"No data for {err.args[0]!r}. Known cities: Oslo, Rome.", "is_error": True}


def loop(client, question, **extra):
    messages = [{"role": "user", "content": question}]
    while True:
        reply = client.messages.create(model=MODEL, max_tokens=500, tools=TOOLS, messages=messages, **extra)
        messages.append({"role": "assistant", "content": reply.content})
        if reply.stop_reason != "tool_use":
            return reply, messages
        messages.append({"role": "user", "content": [run_tool(b) for b in reply.content if b.type == "tool_use"]})
        extra = {k: v for k, v in extra.items() if k != "tool_choice"}  # a forced choice applies to the first request only


REPLIES = [
    message([text("Checking all three."), tool_use("toolu_01", "get_weather", city="Oslo"), tool_use("toolu_02", "get_time", city="Oslo"), tool_use("toolu_03", "get_weather", city="Atlantis")],
            stop_reason="tool_use", model=MODEL),
    message([text("In Oslo it is 09:15 and 4 C with light rain. I have no weather data for Atlantis.")], model=MODEL),
]


def main():
    client, transport = scripted_client(*REPLIES)
    final, messages = loop(client, "Weather and time in Oslo, and the weather in Atlantis?", tool_choice={"type": "auto", "disable_parallel_tool_use": False})
    print("model calls:", len(transport.requests), "| stop reasons:", ["tool_use", final.stop_reason])
    print("roles after the first reply:", [m["role"] for m in messages])
    results = messages[2]["content"]
    print("tool results in ONE user message:", len(results), "| ids in order:", [r["tool_use_id"] for r in results])
    for r in results:
        print(f"  {r['tool_use_id']}: is_error={r.get('is_error', False)} content={r['content']!r}")
    print("tool_choice sent on request 1 and 2:", [r.get("tool_choice") for r in transport.requests])
    print("tool definitions sent carry no handler:", all(set(t) == {"name", "description", "input_schema"} for t in transport.requests[0]["tools"]))
    print("final text:", final.content[0].text)


if __name__ == "__main__":
    main()
