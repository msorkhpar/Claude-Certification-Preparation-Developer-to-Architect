"""An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md."""

SEVERITY_ORDER = {"high": 0, "medium": 1}  # high findings come first
AUTONOMOUS = ("agent", "multi-agent")


def review(design):
    # TODO: the findings of the rubric, each {"rule", "severity"}, ordered by severity and then by rule.
    return None


def verdict(findings):
    # TODO: "reject", "revise" or "approve" from the worst severity among the findings.
    return None


def cheapest_adequate(designs):
    # TODO: the name of the cheapest design that is not rejected, or None.
    return None
