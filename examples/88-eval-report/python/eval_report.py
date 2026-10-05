"""Evaluation decisions for a system that changes: a report by segment, a latency percentile, an A/B verdict, a shadow-run gate, a diagnosis order and a model choice under limits.

The cases, counts and timings are invented for the example. The rules come from the Claude Certified Architect - Professional exam guide (domain 4) and the Claude documentation pages on defining success, developing tests, reducing hallucinations and reducing latency, read on 2026-10-04. Nothing here calls a model.
"""
import logging

log = logging.getLogger(__name__)
COSTS = {"order status": 1, "refund": 20, "policy": 5, "complaint": 10}
GROUPS = [("order status", 30), ("refund", 8), ("policy", 10), ("complaint", 4)]


def build_cases():
    """One (segment, old_ok, new_ok) row per case: right in both, right only with the current prompt, right only with the new one, then wrong in both."""
    rows = []
    both = {"order status": 30, "refund": 5, "policy": 8, "complaint": 3}
    old_only = {"order status": 0, "refund": 0, "policy": 1, "complaint": 1}
    new_only = {"order status": 0, "refund": 2, "policy": 0, "complaint": 0}
    for segment, count in GROUPS:
        rows += [(segment, True, True)] * both[segment]
        rows += [(segment, True, False)] * old_only[segment]
        rows += [(segment, False, True)] * new_only[segment]
        rows += [(segment, False, False)] * (count - both[segment] - old_only[segment] - new_only[segment])
    return rows


def pct(part, whole):
    """Whole percent, half up, with integers only so that every language agrees."""
    return (200 * part + whole) // (2 * whole) if whole else 0


def segment_table(rows, which):
    """Per segment: cases, right answers, accuracy and the cost of the wrong ones, worst cost first."""
    out = {}
    for segment, old_ok, new_ok in rows:
        ok = old_ok if which == "old" else new_ok
        n, right = out.get(segment, (0, 0))
        out[segment] = (n + 1, right + (1 if ok else 0))
    table = [(s, n, r, pct(r, n), (n - r) * COSTS.get(s, 1)) for s, (n, r) in out.items()]
    return sorted(table, key=lambda t: (-t[4], t[0]))


def percentile(values, p):
    """Nearest rank: the value at rank ceil(p * n / 100) of the sorted list."""
    ordered = sorted(values)
    return ordered[(p * len(ordered) + 99) // 100 - 1] if ordered else 0


def ab_verdict(x1, n1, x2, n2, min_n=200):
    """Two-proportion test at 95 percent, done with integers: z squared = D*D*N / (n1*n2*X*(N-X)), compared with 1.96 squared."""
    if n1 < min_n or n2 < min_n:
        return "too few cases"
    big_n, x = n1 + n2, x1 + x2
    if x == 0 or x == big_n:
        return "no clear difference"
    d = x2 * n1 - x1 * n2
    if d * d * big_n * 10000 < 38416 * n1 * n2 * x * (big_n - x):
        return "no clear difference"
    return "new is better" if d > 0 else "old is better"


def shadow_gate(rows, protected):
    """Ship only when no protected segment lost a right answer and the new version lost fewer than it gained."""
    log.debug("shadow_gate input: %r", rows)
    lost = [s for s, old_ok, new_ok in rows if old_ok and not new_ok]
    gained = [s for s, old_ok, new_ok in rows if new_ok and not old_ok]
    blocked = sorted({s for s in lost if s in protected})
    ship = not blocked and len(lost) <= len(gained)
    return {"decision": "ship" if ship else "hold", "lost": len(lost), "gained": len(gained), "blocked": blocked}


def diagnose(found, supported, format_ok, passes_on_stronger):
    """Where to look first: the evidence, then the grounding, then the format, then the task, and the model last."""
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
    """The cheapest option that meets the accuracy floor and the latency limit, ties by name; none when nothing does."""
    fit = [o for o in options if o[1] >= min_accuracy and o[2] <= max_p95]
    return min(fit, key=lambda o: (o[3], o[0]))[0] if fit else "none"


def main():
    rows = build_cases()
    for which, label in (("old", "current prompt"), ("new", "new prompt")):
        table = segment_table(rows, which)
        right = sum(t[2] for t in table)
        print(f"{label}: {right}/{len(rows)} right, {pct(right, len(rows))}% overall, error cost {sum(t[4] for t in table)}")
        for s, n, r, p, c in table:
            print(f"  {s:<13} {r}/{n} {p}% cost {c}")
    latencies = [800, 820, 850, 900, 950, 980, 1000, 1100, 1200, 1500, 2400, 4800]
    print(f"latency ms: mean {sum(latencies) // len(latencies)}, p50 {percentile(latencies, 50)}, p95 {percentile(latencies, 95)}")
    print("live test, 500 cases each, 410 right against 438:", ab_verdict(410, 500, 438, 500))
    print("live test, 500 cases each, 410 right against 425:", ab_verdict(410, 500, 425, 500))
    print("live test, 100 cases each, 82 right against 90:", ab_verdict(82, 100, 90, 100))
    gate = shadow_gate(rows, {"refund", "complaint"})
    print(f"shadow run: {gate['decision']}, lost {gate['lost']}, gained {gate['gained']}, protected segments hit: {', '.join(gate['blocked']) or 'none'}")
    for name, args in (("no chunk had the answer", (False, False, True, True)), ("a claim no chunk supports", (True, False, True, True)), ("a reply in the wrong shape", (True, True, False, True)),
                       ("fails on a stronger model too", (True, True, True, False)), ("passes only on a stronger model", (True, True, True, True))):
        print(f"diagnose, {name}: {diagnose(*args)}")
    options = [("small", 84, 900, 1), ("medium", 91, 1800, 3), ("large", 95, 4200, 9)]
    print(f"model for 90% accuracy within 2000 ms: {choose_model(options, 90, 2000)}; for 94% within 2000 ms: {choose_model(options, 94, 2000)}")


if __name__ == "__main__":
    main()
