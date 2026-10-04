"""Launch review: the findings of a design against the seven domains, the verdict, the scorecard and the accuracy a design needs. See ../../statement.md."""
SEVERITY = {"high": 0, "medium": 1, "low": 2}
DOMAINS = ["P1", "P2", "P3", "P4", "P5", "P6", "P7"]


def launch_review(flags, numbers):
    # TODO: the findings "<severity> <domain> <rule>", high first, then by domain, then by rule id.
    return None


def verdict(findings):
    # TODO: reject for a high finding, revise for a medium one, otherwise approve.
    return None


def scorecard(findings):
    # TODO: the number of findings in each domain, P1 to P7.
    return None


def needed_accuracy(error_cost, review_cost):
    # TODO: 100 minus the review cost as a percent of the error cost, rounded up, never below 0; 0 when an error costs nothing.
    return None
