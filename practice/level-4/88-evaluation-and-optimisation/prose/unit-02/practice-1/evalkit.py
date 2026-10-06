"""Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def pct(part, whole):
    """Whole percent, half up, integers only (written for you)."""
    return (200 * part + whole) // (2 * whole) if whole else 0


def _error_cost(wrong, segment, costs):
    """TODO 1 of 7 (unlocks m1 and e1): the cost of the wrong answers of one segment.

    Receives the number of wrong answers, the segment name and the costs by segment. Returns wrong times the segment's cost, 1 when the
    segment has no entry. Example: _error_cost(3, "refund", {"refund": 20}) -> 60, _error_cost(2, "odd", {"refund": 20}) -> 2
    """
    return 0


def _order(table):
    """TODO 2 of 7 (unlocks m1 and e1): order the report lines.

    Receives the lines (segment, cases, right, percent, cost). Returns them with the highest cost first and equal costs by segment name.
    Example: _order([("a", 1, 1, 100, 0), ("b", 2, 1, 50, 5)]) -> [("b", 2, 1, 50, 5), ("a", 1, 1, 100, 0)]
    """
    return table


def segment_table(results, costs):
    log.debug("segment_table input: %r", results)
    seen = {}
    for segment, correct in results:
        total, right = seen.get(segment, (0, 0))
        seen[segment] = (total + 1, right + (1 if correct else 0))
    return _order([(s, n, r, pct(r, n), _error_cost(n - r, s, costs)) for s, (n, r) in seen.items()])


def percentile(values, p):
    """TODO 3 of 7 (unlocks e2): the nearest-rank percentile.

    Receives the values in any order and p from 1 to 100. Returns the value at rank ceil(p * n / 100) of the sorted values, counting from 1,
    and 0 for no values. Example: percentile([4800, 800, 1000, 900], 95) -> 4800
    """
    return -1


def ab_verdict(x1, n1, x2, n2, min_n=200):
    """TODO 4 of 7 (unlocks e3 and e4): the A/B verdict at 95 percent.

    Receives the right answers and cases of the old version (x1 of n1) and the new one (x2 of n2). Returns `too few cases` when an arm has
    fewer than min_n cases, `no clear difference` when the pooled right answers are 0 or all, otherwise the integer test from the statement:
    `new is better`, `old is better` or `no clear difference`. Example: ab_verdict(410, 500, 438, 500) -> "new is better"
    """
    return ""


def _decision(blocked, lost, gained):
    """TODO 5 of 7 (unlocks e5): ship or hold.

    Receives the protected segments that lost, the number lost and the number gained. Returns `hold` when a protected segment lost or more
    were lost than gained, `ship` otherwise. Example: _decision([], 2, 2) -> "ship", _decision(["refund"], 1, 5) -> "hold"
    """
    return ""


def shadow_gate(pairs, protected):
    lost = [s for s, old_ok, new_ok in pairs if old_ok and not new_ok]
    gained = [s for s, old_ok, new_ok in pairs if new_ok and not old_ok]
    blocked = sorted({s for s in lost if s in protected})
    return {"decision": _decision(blocked, len(lost), len(gained)), "lost": len(lost), "gained": len(gained), "blocked": blocked}


def diagnose(found, supported, format_ok, passes_on_stronger):
    """TODO 6 of 7 (unlocks e6): where to look first for a wrong answer.

    Receives four booleans. Returns `retrieval or data` when the evidence was not found, then `ungrounded answer` when it is not supported,
    then `format instructions` when the format is wrong, then `prompt or task` when it fails on a stronger model, else `model mismatch`.
    Example: diagnose(True, True, False, True) -> "format instructions"
    """
    return ""


def choose_model(options, min_accuracy, max_p95):
    """TODO 7 of 7 (unlocks e7): the cheapest model that meets both limits.

    Receives options (name, accuracy, p95, cost). Returns the name of the cheapest one with accuracy >= min_accuracy and p95 <= max_p95,
    equal costs by name, or `none`. Example: choose_model([("a", 90, 100, 2)], 95, 100) -> "none"
    """
    return ""
