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
    """TODO 1 of 8 (unlocks e1): the findings about absent stages.

    Receives the design's `stages` dict. Returns a list of findings: `missing-stage:input`, `missing-stage:processing` and `missing-stage:output` for each of
    those stages that is absent or empty, then `no-feedback` when `feedback` is absent or empty; every one with severity `high`.
    Example: {"input": ["parse"], "processing": ["act"], "output": ["send"]} -> [{"rule": "no-feedback", "severity": "high"}]
    """
    return []


def team_findings(design):
    """TODO 2 of 8 (unlocks e3): the finding about a team.

    Receives the design. Returns [`team-without-independence`, severity `high`] when there is more than one agent (`agents`, default 1) and either
    `shared_context` is true or `parallel_independent` is not true; otherwise an empty list.
    Example: {"agents": 3, "shared_context": True, "parallel_independent": True} -> one finding; {"agents": 1, "shared_context": True} -> []
    """
    return []


def write_findings(design):
    """TODO 3 of 8 (unlocks e4): the finding about an unapproved write.

    Receives the design. Returns [`unapproved-write`, severity `high`] when `writes_without_approval` and `needs_audit` are both true; otherwise an empty list.
    Example: {"writes_without_approval": True, "needs_audit": False} -> []
    """
    return []


def autonomy_findings(design):
    """TODO 4 of 8 (unlocks e2): the finding about autonomy.

    Receives the design. Returns [`autonomy-without-need`, severity `medium`] when `pattern` is in `AUTONOMOUS` and `path_known` is true; otherwise an empty list.
    Example: {"pattern": "agent", "path_known": False} -> []
    """
    return []


def output_findings(stages):
    """TODO 5 of 8 (unlocks e5): the finding about unvalidated output.

    Receives the design's `stages` dict. Returns [`unvalidated-output`, severity `medium`] when the `output` stage is present and does not contain `validate`;
    otherwise an empty list (an absent output stage is already reported as a missing stage).
    Example: {"output": ["send"]} -> one finding; {"output": ["validate", "send"]} -> []
    """
    return []


def order_findings(findings):
    """TODO 6 of 8 (unlocks e6): order the findings.

    Receives a list of findings. Returns them ordered by severity (`high` before `medium`, see `SEVERITY_ORDER`) and then by rule name.
    Example: [medium "autonomy-without-need", high "no-feedback"] -> the high one first
    """
    return findings


def verdict(findings):
    """TODO 7 of 8 (unlocks m1 and e6): the verdict.

    Receives a list of findings. Returns `reject` when any is `high`, otherwise `revise` when any is `medium`, otherwise `approve`.
    Example: verdict([]) -> "approve"
    """
    return ""


def review(design):
    log.debug("review input: %r", design)
    stages = design.get("stages") or {}
    found = stage_findings(stages) + team_findings(design) + write_findings(design) + autonomy_findings(design) + output_findings(stages)
    return order_findings(found)


def cheapest_adequate(designs):
    """TODO 8 of 8 (unlocks e7): the cheapest design that is not rejected.

    Receives a list of designs, each with `name` and `cost`. Returns the name of the one with the lowest `cost` among those whose `verdict(review(d))` is not
    `reject`; equal costs go to the lower name; `None` when none qualifies or the list is empty.
    Example: costs 2 (name "zeta") and 2 (name "alpha"), both sound -> "alpha"
    """
    return None
