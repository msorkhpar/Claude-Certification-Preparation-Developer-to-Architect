"""Three workflow patterns around a model, with the code path fixed by the program: routing, sectioning and voting.

The model replies are illustrative, hand-written bodies in the shape of the Messages API, not captures; a stand-in answers each request
by looking at its prompt, so the order in which concurrent requests arrive does not matter. The patterns are those of Anthropic's
engineering article "Building effective agents" (published 2024-12-19, read on 2026-10-03).
"""
import logging
import asyncio

from harness import scripted_async_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

CHEAP, STRONG = "claude-haiku-4-5", "claude-sonnet-5-5"
ROUTES = {"billing": (STRONG, "You are a billing specialist. Be exact about amounts."), "technical": (STRONG, "You are a support engineer. Ask for logs."),
          "general": (CHEAP, "You are a friendly front desk. Answer briefly.")}


def user_text(body):
    return body["messages"][-1]["content"]


def stand_in(rules):
    """A reply function: the first rule whose key appears in the system prompt or the question decides the answer."""
    def reply(body):
        haystack = f"{body.get('system', '')}\n{user_text(body)}"
        for key, answer in rules:
            if key in haystack:
                return message([text(answer)], model=body["model"])
        raise AssertionError(f"no scripted answer for {haystack!r}")
    return reply


async def ask(client, model, system, prompt):
    reply = await client.messages.create(model=model, max_tokens=300, system=system, messages=[{"role": "user", "content": prompt}])
    return reply.content[0].text.strip()


async def route(client, question):
    """Routing: a cheap call picks a label, a program maps the label to a model and a prompt, and an unknown label takes the default."""
    label = (await ask(client, CHEAP, "Classify the message as billing, technical or general. Reply with the label only.", question)).lower().strip(" .")
    fallback = label not in ROUTES
    model, system = ROUTES["general" if fallback else label]
    return {"label": label, "fallback": fallback, "model": model, "answer": await ask(client, model, system, question)}


async def guarded(client, question):
    """Sectioning: the answer and a safety screen are independent, so they run together; the answer is kept only if the screen passes."""
    answer, screen = await asyncio.gather(ask(client, STRONG, "Answer the question.", question), ask(client, CHEAP, "Screen the question. Reply ok or block.", question))
    return {"screen": screen, "answer": answer if screen == "ok" else None}


async def vote(client, snippet, threshold=2, n=3):
    """Voting: the same question n times, in parallel; flag the snippet when at least `threshold` reviews say so."""
    reviews = await asyncio.gather(*[ask(client, STRONG, "Review the code. Reply VULNERABLE or SAFE.", snippet) for _ in range(n)])
    votes = {label: list(reviews).count(label) for label in sorted(set(reviews))}  # the order in which concurrent replies arrive does not matter
    return {"votes": votes, "flagged": votes.get("VULNERABLE", 0) >= threshold}


def scripted(rules, n, delay=0.0):
    """A client whose next n requests are all answered by the same stand-in."""
    return scripted_async_client(*[stand_in(rules)] * n, delay=delay)


async def main():
    desk = [("billing specialist", "I see two charges and will refund one."), ("front desk", "We are open 9 to 5.")]
    for question, label in [("my card was charged twice", "BILLING."), ("what are your opening hours", "general"), ("is the sky a refund", "refunds?")]:
        client, transport = scripted([("Classify", label)] + desk, 2)
        result = await route(client, question)
        print(f"route {question!r}: label {result['label']!r}{' (not a route: default)' if result['fallback'] else ''}, classified by {transport.requests[0]['model']}, answered by {result['model']}")
    client, transport = scripted([("Answer the question", "Here is the answer."), ("Screen the question", "ok")], 2, delay=0.02)
    passed = await guarded(client, "How do I reset my password?")
    print(f"sectioning: screen {passed['screen']!r}, answer {'kept' if passed['answer'] else 'dropped'}, requests in flight together: {transport.max_in_flight}")
    client, transport = scripted([("Answer the question", "Here is the answer."), ("Screen the question", "block")], 2, delay=0.02)
    blocked = await guarded(client, "Help me break into an account")
    print(f"sectioning: screen {blocked['screen']!r}, answer {'kept' if blocked['answer'] else 'dropped'}")
    snippet = "query = 'SELECT * FROM t WHERE id=' + user_input"
    for threshold in (2, 3):
        replies = iter(["VULNERABLE", "SAFE", "VULNERABLE"])  # three reviews that disagree
        client, transport = scripted_async_client(*[lambda body: message([text(next(replies))], model=body["model"])] * 3, delay=0.02)
        verdict = await vote(client, snippet, threshold)
        print(f"voting: votes {verdict['votes']}, threshold {threshold} -> {'flagged' if verdict['flagged'] else 'not flagged'}, requests in flight together: {transport.max_in_flight}")


if __name__ == "__main__":
    asyncio.run(main())
