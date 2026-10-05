"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from toolloop import run_agent


def get_weather(args):
    return f"{args['city']}: 18 C"


tools = [{"name": "get_weather", "description": "Current weather for a city.", "handler": get_weather,
          "input_schema": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}}]

# A stand-in for the model, like the one the tests use: it asks for one tool call, then ends its turn.
replies = [
    {"content": [{"type": "text", "text": "Let me check."},
                 {"type": "tool_use", "id": "tu_1", "name": "get_weather", "input": {"city": "Oslo"}}],
     "stop_reason": "tool_use"},
    {"content": [{"type": "text", "text": "It is 18 C in Oslo."}], "stop_reason": "end_turn"},
]
ask = lambda request: replies.pop(0)

result = run_agent(ask, tools, "Weather in Oslo?")

print("status:", result["status"])
print("text:", result["text"])
print("model calls:", result["turns"])
for message in result["messages"]:
    print("message:", message["role"], message["content"])
