"""Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.

The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
"""
from collections import namedtuple

Action = namedtuple("Action", "name consequence")
Answer = namedtuple("Answer", "text confidence quote")
AUTO_CONFIDENCE = 95


def route(action, answer, source, screen_up, confidence_min=AUTO_CONFIDENCE):
    """Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed."""
    if not screen_up and action.consequence == "high":
        return "hold: screen down"
    flag = "" if screen_up else " (unscreened)"
    if answer.quote not in source:
        return "hold: unsupported" + flag
    if action.consequence == "high":
        return "human" + flag
    return ("auto" if answer.confidence >= confidence_min else "review") + flag


def audit_record(request_id, action, outcome, text):
    """Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text."""
    return {"request": request_id, "action": action.name, "consequence": action.consequence, "outcome": outcome, "chars": len(text), "content_stored": False}


def erase(vault, subject):
    """Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone."""
    kept = {token: person for token, person in vault.items() if person != subject}
    return kept, len(vault) - len(kept)


def main():
    source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded."
    reply = Action("draft_reply", "low")
    refund = Action("issue_refund", "high")
    good = Answer("Water damage is covered up to 5,000.", 99, "Water damage is covered up to 5,000 per claim.")
    edge = Answer("Water damage is covered up to 5,000.", 95, "Water damage is covered up to 5,000 per claim.")
    unsure = Answer("Water damage is covered up to 5,000.", 94, "Water damage is covered up to 5,000 per claim.")
    wrong = Answer("Water damage is covered up to 8,000.", 99, "Water damage is covered up to 8,000 per claim.")
    cases = [
        ("screen up, refund, supported", refund, good, True),
        ("screen up, reply, confidence 99", reply, good, True),
        ("screen up, reply, confidence 95", reply, edge, True),
        ("screen up, reply, confidence 94", reply, unsure, True),
        ("screen up, reply, confident but unsupported", reply, wrong, True),
        ("screen down, refund", refund, good, False),
        ("screen down, reply", reply, good, False),
    ]
    for label, action, answer, up in cases:
        print(f"{label}: {route(action, answer, source, up)}")
    record = audit_record("r-1001", refund, "human", good.text)
    print("audit record:", ", ".join(f"{k}={v}" for k, v in record.items()))
    vault = {"<EMAIL_1>": "person-a", "<EMAIL_2>": "person-b", "<MEMBER_1>": "person-a"}
    kept, removed = erase(vault, "person-a")
    print(f"erasure removed {removed} of {len(vault)} mappings; the audit entries stay, with {len(kept)} token still linkable")


if __name__ == "__main__":
    main()
