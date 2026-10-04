"""An architecture review against a rubric: the findings, the verdict and the cheapest design that is not rejected. See ../../statement.md."""

SEVERITY_ORDER = {"high": 0, "medium": 1}
AUTONOMOUS = ("agent", "multi-agent")


def _empty(value):
    return not value


def review(design):
    stages = design.get("stages") or {}
    findings = []
    for stage in ("input", "processing", "output"):
        if _empty(stages.get(stage)):
            findings.append({"rule": f"missing-stage:{stage}", "severity": "high"})
    if _empty(stages.get("feedback")):
        findings.append({"rule": "no-feedback", "severity": "high"})
    if design.get("agents", 1) > 1 and (design.get("shared_context") or not design.get("parallel_independent")):
        findings.append({"rule": "team-without-independence", "severity": "high"})
    if design.get("writes_without_approval") and design.get("needs_audit"):
        findings.append({"rule": "unapproved-write", "severity": "high"})
    if design.get("pattern") in AUTONOMOUS and design.get("path_known"):
        findings.append({"rule": "autonomy-without-need", "severity": "medium"})
    if stages.get("output") and "validate" not in stages["output"]:
        findings.append({"rule": "unvalidated-output", "severity": "medium"})
    return sorted(findings, key=lambda f: (SEVERITY_ORDER[f["severity"]], f["rule"]))


def verdict(findings):
    severities = {f["severity"] for f in findings}
    return "reject" if "high" in severities else "revise" if "medium" in severities else "approve"


def cheapest_adequate(designs):
    adequate = [d for d in designs if verdict(review(d)) != "reject"]
    if not adequate:
        return None
    return min(adequate, key=lambda d: (d["cost"], d["name"]))["name"]
