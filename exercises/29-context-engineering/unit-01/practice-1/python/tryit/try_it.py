"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from context import compact, count_tokens


def user(text):
    return {"role": "user", "content": text}


def said(text):
    return {"role": "assistant", "content": [{"type": "text", "text": text}]}


def call(id, name):
    return {"role": "assistant", "content": [{"type": "tool_use", "id": id, "name": name, "input": {}}]}


def result(id, text):
    return {"role": "user", "content": [{"type": "tool_result", "tool_use_id": id, "content": text}]}


# A conversation of four turns, like the one the tests compact.
messages = [user("first question"), said("first answer " + "x" * 80),
            user("second question"), call("a1", "search"), result("a1", "R" * 300), said("second answer"),
            user("third question"), said("third answer " + "y" * 60),
            user("fourth question"), call("a2", "search"), result("a2", "S" * 100), said("fourth answer")]


def summarise(older):
    """A stand-in for the summariser model: it is told what to fold away and answers with a fixed text."""
    print("summariser asked to fold", len(older), "messages")
    return "The user asked three things."


budget = count_tokens(messages) // 2
done = compact(messages, budget, summarise, 1) or []

print("tokens before:", count_tokens(messages), "| budget:", budget)
print("messages before and after:", len(messages), "->", len(done))
print("roles after:", [m["role"] for m in done])
print("first message:", done[0]["content"] if done else None)
