"""Run executes this file. Change the calls to try your code; Submit runs the tests."""
import logging

# Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logging.basicConfig(level=logging.DEBUG, format="%(levelname)s %(message)s")

from conversation import Conversation


def fake_send(body):
    """A stand-in for the API, like the one the tests use: it answers every request the same way."""
    return {"content": [{"type": "text", "text": "Paris."}], "stop_reason": "end_turn",
            "usage": {"input_tokens": 10, "output_tokens": 5}}


chat = Conversation(fake_send, "claude-sonnet-5-5", 64, system="Be brief.")
reply = chat.say("Capital of France?")
chat.say("Since when?")

print("reply text:", reply.text)
print("stop reason:", reply.stop_reason, "| truncated:", reply.truncated)
print("history size:", len(chat.history()))
print("totals:", chat.totals())
