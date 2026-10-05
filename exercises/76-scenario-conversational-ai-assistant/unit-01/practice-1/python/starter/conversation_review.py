"""Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship."""
import logging

log = logging.getLogger(__name__)


def percent(count, total):
    """TODO 1 of 8 (unlocks m1, e1 and e9): a whole percentage, rounded half up.

    Receives a count and a total. Returns `(200 * count + total) // (2 * total)`, and 0 when the total is 0.
    Example: percent(2, 3) -> 67, percent(1, 8) -> 13, percent(0, 0) -> 0
    """
    return 0


def settled(c):
    """TODO 2 of 8 (unlocks m1 and e8): did the assistant settle this conversation alone?

    Receives one conversation. Returns True only when `resolved` is true and `handoff` is `none` (a conversation a person settled is not one the assistant settled).
    Example: settled({"resolved": True, "handoff": "requested", ...}) -> False
    """
    return False


def count_safety_missed(conversations):
    """TODO 3 of 8 (unlocks e2): count the safety signals that did not reach a person as a safety hand-off.

    Receives the conversations. Returns how many have `risk` and a `handoff` other than `safety` (`none` and `requested` both count as missed).
    Example: one risk conversation with handoff "requested" and one with "safety" -> 1
    """
    return 0


def count_overlong(conversations, max_turns):
    """TODO 4 of 8 (unlocks e3): count the overlong conversations.

    Receives the conversations and the limit. Returns how many have more than `max_turns` turns and `handoff` `none`.
    Example: with a limit of 12, turns 12 -> 0, turns 13 -> 1, turns 30 with handoff "stalled" -> 0
    """
    return 0


def is_repeat_ok(repeated, n, max_repeat):
    """TODO 5 of 8 (unlocks e1 and e4): are the repeated questions within the limit?

    Receives the repeated count, the batch size and the limit in percent. Returns True when `repeated * 100 <= max_repeat * n` (so true for an empty batch).
    Example: is_repeat_ok(1, 10, 10) -> True, is_repeat_ok(2, 10, 10) -> False
    """
    return False


def count_escalations(conversations):
    """TODO 6 of 8 (unlocks e7): count over- and under-escalation.

    Receives the conversations. Returns (over_escalated, under_escalated): hand-offs (any `handoff` but `none`) with no `needed_person` and no `risk`,
    and conversations with `handoff` `none` that had `needed_person`.
    Example: a "requested" hand-off nobody needed -> (1, 0); a "safety" hand-off with risk -> (0, 0)
    """
    return 0, 0


def segments_of(conversations, policy):
    """TODO 7 of 8 (unlocks m1, e5 and e6): one entry per segment.

    Receives the conversations and the policy. Returns a list sorted by segment name of {segment, n, resolved, percent, weak}, with `resolved` counted by `settled`.
    `weak` needs at least `min_n` conversations and `resolved * 100 < min_resolved * n` for the segment.
    Example: 3 unresolved "refunds" conversations with min_n 3 -> [{"segment": "refunds", "n": 3, "resolved": 0, "percent": 0, "weak": True}]
    """
    return [{"segment": "", "n": 0, "resolved": 0, "percent": 0, "weak": False}]


def choose_verdict(n, safety_missed, segments, repeat_ok):
    """TODO 8 of 8 (unlocks m1, e1, e2 and e4): the verdict and its reason.

    Receives the batch size, the missed safety count, the segments and `repeat_ok`. Returns (verdict, reason): ("hold", "no_data") for an empty batch,
    otherwise the first that applies of ("hold", "safety"), ("hold", "weak_segment") and ("hold", "repeats"), and ("ship", "none") when none does.
    Example: choose_verdict(5, 0, [], False) -> ("hold", "repeats")
    """
    return "", ""


def review(conversations, policy):
    log.debug("review input: %r", conversations)
    n = len(conversations)
    resolved = sum(1 for c in conversations if settled(c))
    safety_missed = count_safety_missed(conversations)
    repeated = sum(1 for c in conversations if c["repeated"])
    repeat_ok = is_repeat_ok(repeated, n, policy["max_repeat"])
    over_escalated, under_escalated = count_escalations(conversations)
    segments = segments_of(conversations, policy)
    verdict, reason = choose_verdict(n, safety_missed, segments, repeat_ok)
    return {"n": n, "resolved": resolved, "resolved_pct": percent(resolved, n), "safety_missed": safety_missed,
            "overlong": count_overlong(conversations, policy["max_turns"]), "repeat_pct": percent(repeated, n), "repeat_ok": repeat_ok,
            "over_escalated": over_escalated, "under_escalated": under_escalated, "segments": segments, "verdict": verdict, "reason": reason}
