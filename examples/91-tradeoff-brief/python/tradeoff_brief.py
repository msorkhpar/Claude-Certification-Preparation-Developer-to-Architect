"""One decision told to two audiences: the figures an engineer needs, the same figures in the words a sponsor decides with, and an honest check of each service level at its exact edge.

The figures are invented for a utility's billing-dispute assistant; the break-even rule is the one of module 79. Nothing here calls a model.
"""
from collections import namedtuple

Sla = namedtuple("Sla", "name limit direction unit")
Segment = namedtuple("Segment", "name right total error_cost")


def pct(right, total):
    """Whole percent, halves rounded up, and 0 for no cases."""
    return (200 * right + total) // (2 * total) if total else 0


def break_even(error_cost, review_cost):
    """The accuracy, in whole percent, at or above which a check no longer pays: (1 - accuracy) x error cost <= review cost."""
    return 100 - (-(-100 * review_cost // error_cost))


def sla_line(sla, measured):
    """A service level is met at its limit exactly, and a miss says by how much."""
    met = measured <= sla.limit if sla.direction == "max" else measured >= sla.limit
    verdict = "met" if met else f"missed by {abs(measured - sla.limit)} {sla.unit}"
    word = "limit" if sla.direction == "max" else "floor"
    return f"{sla.name}: {measured} {sla.unit} against a {word} of {sla.limit} {sla.unit}: {verdict}"


def segment_report(segments, review_cost):
    """The costliest segment first, with its accuracy and whether a person checks it."""
    lines = []
    for s in sorted(segments, key=lambda s: (-s.error_cost, s.name)):
        floor = break_even(s.error_cost, review_cost)
        handling = "auto" if pct(s.right, s.total) >= floor else "reviewed"
        lines.append(f"{s.name}: {pct(s.right, s.total)} percent right, error cost {s.error_cost}, {handling} (break-even {floor})")
    return lines


def brief(audience, design, cost, baseline, weakest, ask):
    """The same facts for a sponsor (money, risk and one decision) or for an engineer (the numbers that produced them)."""
    if audience == "sponsor":
        return (f"{design} costs {cost:,} a month against {baseline:,} for people alone, a saving of {baseline - cost:,}. "
                f"The weakest answers are {weakest.name}: {pct(weakest.right, weakest.total)} in 100 are right and each wrong one costs {weakest.error_cost}, "
                f"so a person decides them. Decision asked: {ask}.")
    return f"design={design}; cost={cost}; baseline={baseline}; saving={baseline - cost}; weakest={weakest.name} {pct(weakest.right, weakest.total)}% at {weakest.error_cost} an error"


def main():
    slas = [(Sla("p95 latency", 2000, "max", "ms"), 1800), (Sla("p95 latency", 2000, "max", "ms"), 2000), (Sla("p95 latency", 2000, "max", "ms"), 2150),
            (Sla("availability", 995, "min", "per mille"), 997), (Sla("availability", 995, "min", "per mille"), 990)]
    for sla, measured in slas:
        print(sla_line(sla, measured))
    segments = [Segment("status", 98, 100, 12), Segment("credit", 63, 100, 250), Segment("complaint", 91, 100, 60)]
    for line in segment_report(segments, 5):
        print(line)
    weakest = min(segments, key=lambda s: pct(s.right, s.total))
    for audience in ("sponsor", "engineer"):
        print(f"{audience}: {brief(audience, 'Routing by confidence', 80000, 315000, weakest, 'approve the pilot')}")


if __name__ == "__main__":
    main()
