"""Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)

SEVERITY = {"high": 0, "medium": 1, "low": 2}
DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"]


def _p1_rules(f, n, add):
    """TODO 1 of 8 (unlocks e1, e4 and e7): the rules of domain P1.

    Receives the flags and the numbers and reports each rule it finds through `add`. The rules: missing-feedback, autonomy-without-need and team-below-price from the statement's table. Example: missing-feedback: the flag feedback_loop is absent -> one finding `high P1 missing-feedback`.
    """
    pass


def _p3_rules(f, n, add):
    """TODO 2 of 8 (unlocks e4 and e8): the rules of domain P3.

    Receives the flags and the numbers and reports each rule it finds through `add`. The rules: filter-after-ranking, stale-index, tool-bloat and agent-rights-only. Example: stale-index: the flag replace_on_change is absent -> one finding `high P3 stale-index`.
    """
    pass


def _p4_rules(f, n, add):
    """TODO 3 of 8 (unlocks e1, e3 and e4): the rules of domain P4.

    Receives the flags and the numbers and reports each rule it finds through `add`. The rules: no-protected-segment, small-eval-set, no-way-back and big-bang-rollout. Example: no-way-back: the flag rollback is absent -> one finding `high P4 no-way-back`.
    """
    pass


def _p5_rules(f, n, add):
    """TODO 4 of 8 (unlocks e4, e7 and e8): the rules of domain P5.

    Receives the flags and the numbers and reports each rule it finds through `add`. The rules: identifiers-reach-model, residency-unmet, audit-keeps-content, irreversible-without-person and retention-outside-window. Example: irreversible-without-person: irreversible_action present and human_step absent -> `high P5 irreversible-without-person`.
    """
    pass


def _p2_rules(f, n, add):
    add("volatile_prefix" in f, "medium", "P2", "volatile-prefix")
    add("model_measured" not in f, "low", "P2", "model-not-measured")


def _p6_rules(f, n, add):
    add("owner" not in f, "medium", "P6", "no-accountable-owner")
    add(n.get("latency_ms", 0) <= 0 or n.get("availability_tenths", 0) <= 0, "medium", "P6", "sla-without-numbers")
    add("accuracy_stated" not in f, "low", "P6", "accuracy-unstated")


def _p7_rules(f, n, add):
    add(n.get("team_size", 0) > 10 and "managed_settings" not in f, "medium", "P7", "unmanaged-team-settings")


def _order(found):
    """TODO 5 of 8 (unlocks e3): order the findings.

    Receives the findings. Returns them ordered by severity (high, medium, low), then by domain, then by rule id in alphabetical order. Example: ["low P2 a", "high P6 z", "high P1 y"] -> ["high P1 y", "high P6 z", "low P2 a"]
    """
    return found


def launch_review(flags, numbers):
    """The findings, each "<severity> <domain> <rule>", high first, then by domain, then by rule."""
    log.debug("launch_review input: %r", sorted(flags))
    f, n = set(flags), dict(numbers)
    found = []

    def add(condition, severity, domain, rule):
        if condition:
            found.append(f"{severity} {domain} {rule}")

    for rules in (_p1_rules, _p2_rules, _p3_rules, _p4_rules, _p5_rules, _p6_rules, _p7_rules):
        rules(f, n, add)
    return _order(found)


def verdict(findings):
    """TODO 6 of 8 (unlocks e1 and e2): the verdict.

    Receives the findings. Returns `reject` when any is high, `revise` when none is high and any is medium, `approve` otherwise. Example: ["low P2 a"] -> "approve"
    """
    return ""


def scorecard(findings):
    """TODO 7 of 8 (unlocks e5): the scorecard.

    Receives the findings. Returns the seven counts of the findings of P1 to P7 in that order. Example: ["high P1 a", "low P7 c"] -> [1, 0, 0, 0, 0, 0, 1]
    """
    return []


def needed_accuracy(error_cost, review_cost):
    """TODO 8 of 8 (unlocks m1 and e6): the accuracy a design needs.

    Receives the cost of an error and the cost of a check. Returns 100 minus the check cost as a percent of the error cost, the percent rounded up, never below 0, and 0 when an error costs nothing or less. Example: needed_accuracy(250, 5) -> 98
    """
    return -1
