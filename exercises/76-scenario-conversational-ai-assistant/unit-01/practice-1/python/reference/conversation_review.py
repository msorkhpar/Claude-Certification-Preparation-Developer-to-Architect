"""Review a batch of assistant conversations: how many the assistant settled, whether every safety signal reached a person, and whether the assistant may ship."""
import logging

log = logging.getLogger(__name__)


def percent(count, total):
    """A whole percentage, rounded half up; 0 when there is nothing to divide."""
    return (200 * count + total) // (2 * total) if total else 0


def settled(c):
    """True when the assistant settled the conversation alone: resolved and no hand-off."""
    return c["resolved"] and c["handoff"] == "none"


def count_safety_missed(conversations):
    """The conversations with a risk signal whose hand-off is not a safety hand-off."""
    return sum(1 for c in conversations if c["risk"] and c["handoff"] != "safety")


def count_overlong(conversations, max_turns):
    """The conversations above the turn limit that nobody took over."""
    return sum(1 for c in conversations if c["turns"] > max_turns and c["handoff"] == "none")


def is_repeat_ok(repeated, n, max_repeat):
    """True when the repeated questions are at most max_repeat percent of the batch (true for an empty batch)."""
    return repeated * 100 <= max_repeat * n


def count_escalations(conversations):
    """(over_escalated, under_escalated): hand-offs nobody needed, and conversations that needed a person and got none."""
    over = sum(1 for c in conversations if c["handoff"] != "none" and not c["needed_person"] and not c["risk"])
    under = sum(1 for c in conversations if c["handoff"] == "none" and c["needed_person"])
    return over, under


def segments_of(conversations, policy):
    """One entry per segment, sorted by name: {segment, n, resolved, percent, weak}."""
    result = []
    for name in sorted({c["segment"] for c in conversations}):
        group = [c for c in conversations if c["segment"] == name]
        done = sum(1 for c in group if settled(c))
        result.append({"segment": name, "n": len(group), "resolved": done, "percent": percent(done, len(group)),
                       "weak": len(group) >= policy["min_n"] and done * 100 < policy["min_resolved"] * len(group)})
    return result


def choose_verdict(n, safety_missed, segments, repeat_ok):
    """(verdict, reason): hold for no data, then for a missed safety signal, a weak segment and repeats; otherwise ship."""
    if n == 0:
        return "hold", "no_data"
    if safety_missed:
        return "hold", "safety"
    if any(s["weak"] for s in segments):
        return "hold", "weak_segment"
    if not repeat_ok:
        return "hold", "repeats"
    return "ship", "none"


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
