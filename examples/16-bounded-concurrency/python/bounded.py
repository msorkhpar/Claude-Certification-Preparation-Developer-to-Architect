"""Twelve classification calls with the async SDK: unbounded, then bounded by a semaphore.

The transport is scripted: every request takes 50 ms inside it, the reply is a label built from
the ticket in the request, and ticket 7 is answered with a 429. The labels are illustrative.
"""
import logging
import asyncio


from harness import scripted_async_client
from harness.scripted import message, text

log = logging.getLogger(__name__)

MODEL = "claude-sonnet-5-5"
TICKETS = [f"ticket {n}" for n in range(1, 13)]


def responder(body):
    ticket = body["messages"][0]["content"]
    if ticket == "ticket 7":
        return (429, {"type": "error", "error": {"type": "rate_limit_error", "message": "slow down"}})
    return message([text("label for " + ticket)])


async def classify(client, ticket):
    reply = await client.messages.create(model=MODEL, max_tokens=16, messages=[{"role": "user", "content": ticket}])
    return reply.content[0].text


async def run_all(limit):
    client, transport = scripted_async_client(*[responder] * len(TICKETS), delay=0.05)
    gate = asyncio.Semaphore(limit) if limit else None

    async def one(ticket):
        if gate is None:
            return await classify(client, ticket)
        async with gate:
            return await classify(client, ticket)

    results = await asyncio.gather(*(one(t) for t in TICKETS), return_exceptions=True)
    return results, transport.max_in_flight


async def main():
    for label, limit in (("unbounded", None), ("bounded by 4", 4)):
        results, peak = await run_all(limit)
        failed = [i + 1 for i, r in enumerate(results) if isinstance(r, Exception)]
        print(f"{label}: peak in flight {peak}, {len(results) - len(failed)} answered, failed tickets {failed}")
    print("results keep input order:", results[0], "|", results[5], "|", type(results[6]).__name__, "|", results[7])


if __name__ == "__main__":
    asyncio.run(main())
