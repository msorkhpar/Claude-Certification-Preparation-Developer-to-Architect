"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from computer import run_computer_loop

# A toy screen in memory: a search box and a payment button.
screen = {"width": 1920, "height": 1080, "cursor": [0, 0], "typed": "", "log": [],
          "elements": [{"id": "search", "x": 1000, "y": 200, "w": 400, "h": 40, "risk": "none"},
                       {"id": "pay", "x": 800, "y": 600, "w": 200, "h": 60, "risk": "payment"}]}


def use(id, name, **input):
    return {"type": "tool_use", "id": id, "name": name, "toolset_name": "computer", "input": input}


# The model is a function that returns scripted replies in order, like the one the tests use.
replies = [
    {"content": [use("t1", "screenshot")], "stop_reason": "tool_use"},
    {"content": [use("t2", "left_click", coordinate=[894, 164]), use("t3", "type", text="weather"), use("t4", "screenshot")], "stop_reason": "tool_use"},
    {"content": [{"type": "text", "text": "Typed it."}], "stop_reason": "end_turn"},
]
requests = []


def ask(request):
    requests.append(len(request["messages"]))
    # when the script runs out, the model just ends the conversation
    return replies.pop(0) if replies else {"content": [{"type": "text", "text": "script ran out"}], "stop_reason": "end_turn"}


result = run_computer_loop(ask, screen) or {}

print("status and turns:", result.get("status"), result.get("turns"))
print("messages in each request:", requests)
print("actions performed on the screen:", screen["log"])
print("typed text:", repr(screen["typed"]))
