"""Why escalation is decided by criteria, and what to ask when a lookup finds several people.

The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
(illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
"""
# name, sentiment, asked for a person, policy silent, what a careful person would do
CASES = [
    ("price match with another shop", "calm", False, True, "escalate"),
    ("wrong colour, standard exchange", "angry", False, False, "resolve"),
    ("calm request to speak to a person", "calm", True, False, "escalate"),
    ("password reset", "frustrated", False, False, "resolve"),
    ("refund for an item bought elsewhere", "calm", False, True, "escalate"),
    ("angry, wants a person now", "angry", True, False, "escalate"),
]


def by_sentiment(case):
    return "resolve" if case[1] == "calm" else "escalate"


def by_criteria(case):
    return "escalate" if case[2] or case[3] else "resolve"


def errors(rule):
    return [i for i, case in enumerate(CASES, 1) if rule(case) != case[4]]


def pick_most_recent(matches):
    """The heuristic the guide rejects: choose the account with the latest order."""
    return max(matches, key=lambda m: m["last_order"])["id"]


def ask_for_identifier(matches, fields):
    """What the guide asks for: no choice, a request for something that tells the matches apart."""
    return f"I found {len(matches)} accounts for that name. Please give me one of: " + ", ".join(fields) + "."


def escalation_section(criteria, examples):
    """Explicit criteria and examples for the system prompt: when to escalate, and when not to."""
    lines = ["Escalate to a person when:"] + [f"- {c}" for c in criteria] + ["", "Examples:"]
    lines += [f'Customer: "{text}" -> {decision} ({why})' for text, decision, why in examples]
    return "\n".join(lines)


def main():
    for i, case in enumerate(CASES, 1):
        print(f"case {i} ({case[0]}): sentiment rule {by_sentiment(case)}, criteria {by_criteria(case)}, careful person {case[4]}")
    print(f"sentiment rule routed {len(errors(by_sentiment))} of {len(CASES)} wrongly: cases " + ", ".join(map(str, errors(by_sentiment))))
    print(f"criteria routed {len(errors(by_criteria))} of {len(CASES)} wrongly")
    matches = [{"id": "c1", "last_order": 20260901}, {"id": "c2", "last_order": 20260915}]
    print(f"heuristic: the agent acts on {pick_most_recent(matches)} although the customer may be c1")
    print(ask_for_identifier(matches, ["the email on the account", "the postcode"]))
    print(escalation_section(["the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"],
                             [("Can you match the price on another site?", "escalate", "the policy only covers our own prices"), ("This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration")]))


if __name__ == "__main__":
    main()
