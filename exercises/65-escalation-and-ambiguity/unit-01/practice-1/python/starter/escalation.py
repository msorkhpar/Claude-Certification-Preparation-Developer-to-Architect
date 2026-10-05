"""When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md."""

import logging

log = logging.getLogger(__name__)


def _decision(action, reason, acknowledge=False):
    return {"action": action, "reason": reason, "acknowledge": acknowledge}


def decide(case, max_attempts=2):
    log.debug("decide input: %r", case)
    # TODO 1 of 7 (finish this to pass m1, e4): the first rule. When the customer asked for a person, return the
    #   decision escalate with the reason "customer asked for a person", before any other rule. Example: asked_for_person
    #   true and two matches -> escalate.
    # TODO 2 of 7 (finish this to pass e3): the ambiguity rule. When more than one customer record matches, return the
    #   decision clarify with the reason "ambiguous customer match". Example: matches 2 -> clarify.
    # TODO 3 of 7 (finish this to pass e2): the policy rule. When the policy does not cover the request, return the
    #   decision escalate with the reason "policy does not cover the request". Example: policy_covers false -> escalate.
    # TODO 4 of 7 (finish this to pass e5): the progress rule. When the attempts without progress are at or above
    #   max_attempts, return the decision escalate with the reason "no progress". Example: 2 attempts, limit 2 ->
    #   escalate; 1 attempt -> not.
    # TODO 5 of 7 (finish this to pass e1): the acknowledgement of a resolved case. Return resolve with the reason
    #   "within capability" and acknowledge true when the sentiment is anything but calm, false when it is calm. Example:
    #   sentiment frustrated -> resolve, acknowledge true.
    return _decision("resolve", "within capability")


def clarifying_fields(matches):
    if len(matches) < 2:
        return []
    # TODO 6 of 7 (finish this to pass e7): the fields to ask about. Receives the matching records (maps). Return the
    #   names of the fields, other than id, whose values are not all the same across the matches; return none when there
    #   are fewer than two matches. Example: two records that differ only in city -> [city].
    return []


def handoff_text(case):
    if not case.get("customer_id") or not case.get("issue"):
        raise ValueError("a hand-off needs a customer id and an issue")
    lines = [
        f"Customer: {case['customer_id']}",
        f"Issue: {case['issue']}",
        f"Root cause: {case.get('root_cause') or 'unknown'}",
        f"Amount: {case.get('amount') or 'unknown'}",
        # TODO 7 of 7 (finish this to pass e8): the last two lines of the hand-off. Write "Actions taken: " with the
        #   actions joined by "; " (or none when there are none) and "Recommended action: " with the recommendation (or
        #   review the case when it is empty). Never the transcript. Example: actions [refund, email] -> "Actions taken:
        #   refund; email".
        "Actions taken: none",
        "Recommended action: review the case",
    ]
    return "\n".join(lines)
