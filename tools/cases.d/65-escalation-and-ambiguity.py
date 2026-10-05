# Case lists of module 65-escalation-and-ambiguity: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/65-escalation-and-ambiguity/unit-01/practice-1"] = {
    "name": "escalation", "suite": "EscalationTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "a customer who asks for a person is escalated at once even when the agent could resolve it"),
        ("e1", "edge", "frustration alone does not escalate and the reply acknowledges it"),
        ("e2", "edge", "a request the policy does not cover is escalated and a covered one is resolved"),
        ("e3", "edge", "several matching records need a clarifying question and never a guess"),
        ("e4", "edge", "an explicit request for a person outranks an ambiguous match"),
        ("e5", "edge", "no progress after the attempt limit escalates and below it does not"),
        ("e6", "edge", "sentiment and confidence scores never change the decision"),
        ("e7", "edge", "the clarifying question names only the fields that tell the matches apart"),
        ("e8", "edge", "the hand off carries the structured facts and no transcript and refuses a case without an id"),
    ],
    "plants": {
        "wrong-investigate-first": (["e4", "m1"], "escalates on an explicit request only when the policy also has a gap"),
        "wrong-angry-escalates": (["e6"], "escalates an angry customer because of the sentiment"),
        "wrong-low-confidence-escalates": (["e6"], "escalates when the model reports low confidence"),
        "wrong-picks-first-match": (["e3", "e6"], "never asks which of several matching customers is meant"),
        "wrong-policy-gap-resolved": (["e2"], "resolves a request the policy does not cover"),
        "wrong-no-acknowledgement": (["e1"], "does not acknowledge a frustrated customer"),
        "wrong-clarify-all-fields": (["e7"], "asks about fields on which the matches agree"),
        "wrong-handoff-transcript": (["e8"], "attaches the whole transcript to the hand-off"),
        "wrong-three-matches-guessed": (["e3"], "asks a clarifying question only for exactly two matches and guesses with more"),
        "wrong-ask-loses-to-many-matches": (["e4"], "lets four or more matches outrank an explicit request for a person"),
        "wrong-limit-off-by-one": (["e5"], "escalates only after the attempts pass the limit instead of reaching it"),
    },
}
