"""A conversation the client keeps: the API is stateless, so every request carries the whole history.

Three turns through the real SDK against a scripted transport. The replies are illustrative,
hand-written Messages responses (claude-sonnet-5-5), not captures.
"""
import logging
import anthropic
import httpx2

from harness import ScriptedTransport
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
SYSTEM = "You answer in one short sentence."

REPLIES = [
    message([text("Paris.")], usage={"input_tokens": 18, "output_tokens": 4}),
    message([text("It has been the capital since")], "max_tokens", usage={"input_tokens": 30, "output_tokens": 6}),
    message([text("Seine")], "stop_sequence", usage={"input_tokens": 41, "output_tokens": 2}, stop_sequence="END"),
]
QUESTIONS = ["Capital of France?", "Since when?", "Name its river. End with END."]


def run(client, questions):
    """Keep the history in a list and send all of it every time."""
    history, totals = [], {"input": 0, "output": 0}
    for question in questions:
        history.append({"role": "user", "content": question})
        reply = client.messages.create(model=MODEL, max_tokens=16, system=SYSTEM, messages=history,
                                       stop_sequences=["END"])
        history.append({"role": "assistant", "content": reply.content})
        totals["input"] += reply.usage.input_tokens
        totals["output"] += reply.usage.output_tokens
        yield reply, totals


def main():
    transport = ScriptedTransport(*REPLIES)
    client = anthropic.Anthropic(api_key="placeholder", max_retries=0, http_client=httpx2.Client(transport=transport))
    for number, (reply, totals) in enumerate(run(client, QUESTIONS), start=1):
        sent = transport.requests[-1]["messages"]
        roles = ", ".join(m["role"] for m in sent)
        print(f"turn {number}: sent {len(sent)} message(s) [{roles}] -> {reply.stop_reason}"
              f"{' ' + repr(reply.stop_sequence) if reply.stop_sequence else ''}, {reply.content[0].text!r}")
    print("totals:", totals)
    print("system is a top-level field:", all(r["system"] == SYSTEM for r in transport.requests),
          "| roles ever used in messages:", sorted({m["role"] for r in transport.requests for m in r["messages"]}))


if __name__ == "__main__":
    main()
