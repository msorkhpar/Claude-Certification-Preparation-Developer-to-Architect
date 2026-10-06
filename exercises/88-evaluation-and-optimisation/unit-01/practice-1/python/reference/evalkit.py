"""Evaluation kit: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)


def pct(part, whole):
    return (200 * part + whole) // (2 * whole) if whole else 0


def _error_cost(wrong, segment, costs):
    return wrong * costs.get(segment, 1)


def _order(table):
    return sorted(table, key=lambda t: (-t[4], t[0]))


def segment_table(results, costs):
    log.debug("segment_table input: %r", results)
    seen = {}
    for segment, correct in results:
        total, right = seen.get(segment, (0, 0))
        seen[segment] = (total + 1, right + (1 if correct else 0))
    return _order([(s, n, r, pct(r, n), _error_cost(n - r, s, costs)) for s, (n, r) in seen.items()])


def percentile(values, p):
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def ab_verdict(x1, n1, x2, n2, min_n=200):
    if n1 < min_n or n2 < min_n:
        return "too few cases"
    big_n, x = n1 + n2, x1 + x2
    if x == 0 or x == big_n:
        return "no clear difference"
    d = x2 * n1 - x1 * n2
    if d * d * big_n * 10000 < 38416 * n1 * n2 * x * (big_n - x):
        return "no clear difference"
    return "new is better" if d > 0 else "old is better"


def _decision(blocked, lost, gained):
    return "ship" if not blocked and lost <= gained else "hold"


def shadow_gate(pairs, protected):
    lost = [s for s, old_ok, new_ok in pairs if old_ok and not new_ok]
    gained = [s for s, old_ok, new_ok in pairs if new_ok and not old_ok]
    blocked = sorted({s for s in lost if s in protected})
    return {"decision": _decision(blocked, len(lost), len(gained)), "lost": len(lost), "gained": len(gained), "blocked": blocked}


def diagnose(found, supported, format_ok, passes_on_stronger):
    if not found:
        return "retrieval or data"
    if not supported:
        return "ungrounded answer"
    if not format_ok:
        return "format instructions"
    if not passes_on_stronger:
        return "prompt or task"
    return "model mismatch"


def choose_model(options, min_accuracy, max_p95):
    fit = [o for o in options if o[1] >= min_accuracy and o[2] <= max_p95]
    return min(fit, key=lambda o: (o[3], o[0]))[0] if fit else "none"
