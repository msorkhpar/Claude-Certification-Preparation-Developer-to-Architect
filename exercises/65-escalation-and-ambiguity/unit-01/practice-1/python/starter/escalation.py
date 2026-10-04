"""When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md."""


def decide(case, max_attempts=2):
    # TODO: {"action": "resolve" | "clarify" | "escalate", "reason": str, "acknowledge": bool} for a case.
    return None


def clarifying_fields(matches):
    # TODO: the fields (never "id") on which the matching records differ, in the order of the first record.
    return None


def handoff_text(case):
    # TODO: the text of a hand-off: six labelled lines, no transcript; an error when the customer id or the issue is missing.
    return None
