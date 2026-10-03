import asyncio

from harness.scripted import message, text
from harness import scripted_async_client
from routing_and_voting import CHEAP, STRONG, guarded, route, scripted, vote

DESK = [("billing specialist", "I see two charges."), ("front desk", "Open 9 to 5.")]


def run(coro):
    return asyncio.run(coro)


def test_a_label_picks_the_model_and_an_unknown_label_takes_the_default():
    client, transport = scripted([("Classify", "Billing.")] + DESK, 2)
    billing = run(route(client, "charged twice"))
    assert (billing["label"], billing["model"], billing["fallback"]) == ("billing", STRONG, False) and transport.requests[0]["model"] == CHEAP
    client, _ = scripted([("Classify", "refunds?")] + DESK, 2)
    odd = run(route(client, "hello"))
    assert (odd["label"], odd["model"], odd["fallback"], odd["answer"]) == ("refunds?", CHEAP, True, "Open 9 to 5.")


def test_sectioning_runs_both_calls_together_and_drops_the_answer_when_the_screen_blocks():
    client, transport = scripted([("Answer the question", "Fine."), ("Screen the question", "block")], 2, delay=0.02)
    result = run(guarded(client, "q"))
    assert result == {"screen": "block", "answer": None} and transport.max_in_flight == 2


def test_voting_counts_the_reviews_against_the_threshold():
    def verdicts(threshold):
        replies = iter(["VULNERABLE", "SAFE", "VULNERABLE"])
        client, transport = scripted_async_client(*[lambda body: message([text(next(replies))], model=body["model"])] * 3, delay=0.01)
        return run(vote(client, "code", threshold)), transport.max_in_flight
    (two, flight), (three, _) = verdicts(2), verdicts(3)
    assert two["flagged"] is True and three["flagged"] is False and flight == 3 and two["votes"] == {"SAFE": 1, "VULNERABLE": 2}
