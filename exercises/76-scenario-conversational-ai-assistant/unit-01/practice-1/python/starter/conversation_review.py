"""Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship.

Read statement.md for the fields of a conversation, of the policy and of the report, then replace the body of review().
"""


def review(conversations, policy):
    return {"n": 0, "resolved": 0, "resolved_pct": 0, "safety_missed": 0, "overlong": 0, "repeat_pct": 0, "repeat_ok": False, "over_escalated": 0,
            "under_escalated": 0, "segments": [], "verdict": "", "reason": ""}
