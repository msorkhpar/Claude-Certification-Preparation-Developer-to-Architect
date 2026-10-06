"""An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md."""
import logging

log = logging.getLogger(__name__)

SEVERITY_ORDER = {"high": 0, "medium": 1}
AUTONOMOUS = ("agent", "multi-agent")


def _empty(value):
    return not value


def finding(rule, severity):
    return {"rule": rule, "severity": severity}


def stage_findings(stages):
    """missing-stage:<stage> for each absent stage, then no-feedback when the feedback stage is absent; all high."""
    found = [finding(f"missing-stage:{stage}", "high") for stage in ("input", "processing", "output") if _empty(stages.get(stage))]
    if _empty(stages.get("feedback")):
        found.append(finding("no-feedback", "high"))
    return found


def team_findings(design):
    """team-without-independence (high) for more than one agent with a shared context or parts that are not independent."""
    if design.get("agents", 1) > 1 and (design.get("shared_context") or not design.get("parallel_independent")):
        return [finding("team-without-independence", "high")]
    return []


def write_findings(design):
    """unapproved-write (high) when the design writes without approval and an audit is needed."""
    if design.get("writes_without_approval") and design.get("needs_audit"):
        return [finding("unapproved-write", "high")]
    return []


def autonomy_findings(design):
    """autonomy-without-need (medium) when an agent or a team is used on a known path."""
    if design.get("pattern") in AUTONOMOUS and design.get("path_known"):
        return [finding("autonomy-without-need", "medium")]
    return []


def output_findings(stages):
    """unvalidated-output (medium) when the output stage is present and has no validate step."""
    if stages.get("output") and "validate" not in stages["output"]:
        return [finding("unvalidated-output", "medium")]
    return []


def order_findings(findings):
    """High first, then by rule name."""
    return sorted(findings, key=lambda f: (SEVERITY_ORDER[f["severity"]], f["rule"]))


def verdict(findings):
    """reject for any high finding, revise for any medium finding, otherwise approve."""
    severities = {f["severity"] for f in findings}
    return "reject" if "high" in severities else "revise" if "medium" in severities else "approve"


def review(design):
    log.debug("review input: %r", design)
    stages = design.get("stages") or {}
    found = stage_findings(stages) + team_findings(design) + write_findings(design) + autonomy_findings(design) + output_findings(stages)
    return order_findings(found)


def cheapest_adequate(designs):
    """The name of the cheapest design whose verdict is not reject; ties go to the lower name; None when none qualifies."""
    adequate = [d for d in designs if verdict(review(d)) != "reject"]
    if not adequate:
        return None
    return min(adequate, key=lambda d: (d["cost"], d["name"]))["name"]
