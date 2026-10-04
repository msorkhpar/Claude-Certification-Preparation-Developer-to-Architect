"""A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md."""
from datetime import date

SAFE_INPUT = ("tokenise", "redact")
WANT = {"us": "us", "eu": "eu", "other": "global"}


def check_deployment(config, req):
    platform = config.get("platform")
    region = config.get("region") or ""
    audit = config.get("audit") or {}
    findings = set()
    if req.get("residency") == "us":
        if platform in ("api", "aws-platform"):
            if config.get("inference_geo") != "us":
                findings.add("residency-not-pinned")
        elif not region.startswith("us-"):
            findings.add("residency-region")
    elif req.get("residency") == "eu":
        if platform in ("api", "aws-platform"):
            findings.add("residency-unavailable")
        elif not (region.startswith("eu-") or region.startswith("europe-") or region == "eu"):
            findings.add("residency-region")
    if req.get("zdr_required"):
        if platform in ("bedrock", "vertex"):
            findings.add("zdr-not-anthropics")
        else:
            if not config.get("zdr"):
                findings.add("zdr-missing")
            if str(config.get("model", "")).startswith("claude-fable"):
                findings.add("model-needs-retention")
    if req.get("phi"):
        if platform == "api" and not config.get("hipaa_baa"):
            findings.add("phi-no-baa")
        if platform == "aws-platform":
            findings.add("phi-platform-unsupported")
        if config.get("pii_handling") not in SAFE_INPUT:
            findings.add("phi-not-deidentified")
    if req.get("multi_tenant") and config.get("tenancy") != "workspace-per-tenant":
        findings.add("tenant-isolation")
    if audit.get("store_prompts") and (req.get("phi") or config.get("pii_handling") not in SAFE_INPUT):
        findings.add("audit-stores-sensitive")
    days = audit.get("retain_days")
    if days is not None:
        if req.get("audit_max_days") is not None and days > req["audit_max_days"]:
            findings.add("retention-too-long")
        if req.get("audit_min_days") is not None and days < req["audit_min_days"]:
            findings.add("retention-too-short")
    return sorted(findings)


def retention_actions(entries, max_days, today):
    now = date.fromisoformat(today)
    purge, keep = [], []
    for e in sorted(entries, key=lambda e: e["id"]):
        age = (now - date.fromisoformat(e["date"])).days
        (purge if age > max_days and not e.get("hold") else keep).append(e["id"])
    return {"purge": purge, "keep": keep}


def pick_deployment(user_region, deployments):
    matching = sorted(d["name"] for d in deployments if d["residency"] == WANT.get(user_region))
    return matching[0] if matching else None
