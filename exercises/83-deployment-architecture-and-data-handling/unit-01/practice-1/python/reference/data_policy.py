"""A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md."""
import logging
from datetime import date

log = logging.getLogger(__name__)

SAFE_INPUT = ("tokenise", "redact")  # pii_handling values that keep identifiers out of the prompt
WANT = {"us": "us", "eu": "eu", "other": "global"}  # the residency of the deployment that may serve a user region


def _report(findings):
    return sorted(findings)


def _residency_findings(req, config):
    platform, region = config.get("platform"), config.get("region") or ""
    if req.get("residency") == "us":
        if platform in ("api", "aws-platform"):
            if config.get("inference_geo") != "us":
                return {"residency-not-pinned"}
        elif not region.startswith("us-"):
            return {"residency-region"}
    elif req.get("residency") == "eu":
        if platform in ("api", "aws-platform"):
            return {"residency-unavailable"}
        elif not (region.startswith("eu-") or region.startswith("europe-") or region == "eu"):
            return {"residency-region"}
    return set()


def _zdr_findings(req, config):
    findings = set()
    if req.get("zdr_required"):
        if config.get("platform") in ("bedrock", "vertex"):
            findings.add("zdr-not-anthropics")
        else:
            if not config.get("zdr"):
                findings.add("zdr-missing")
            if str(config.get("model", "")).startswith("claude-fable"):
                findings.add("model-needs-retention")
    return findings


def _phi_findings(req, config):
    findings = set()
    if req.get("phi"):
        if config.get("platform") == "api" and not config.get("hipaa_baa"):
            findings.add("phi-no-baa")
        if config.get("platform") == "aws-platform":
            findings.add("phi-platform-unsupported")
        if config.get("pii_handling") not in SAFE_INPUT:
            findings.add("phi-not-deidentified")
    return findings


def _tenant_findings(req, config):
    if req.get("multi_tenant") and config.get("tenancy") != "workspace-per-tenant":
        return {"tenant-isolation"}
    return set()


def _retention_findings(req, config):
    findings = set()
    days = (config.get("audit") or {}).get("retain_days")
    if days is not None:
        if req.get("audit_max_days") is not None and days > req["audit_max_days"]:
            findings.add("retention-too-long")
        if req.get("audit_min_days") is not None and days < req["audit_min_days"]:
            findings.add("retention-too-short")
    return findings


def _audit_findings(req, config):
    audit = config.get("audit") or {}
    if audit.get("store_prompts") and (req.get("phi") or config.get("pii_handling") not in SAFE_INPUT):
        return {"audit-stores-sensitive"}
    return set()


def check_deployment(config, req):
    log.debug("check_deployment input: %r", config)
    findings = set()
    for check in (_residency_findings, _zdr_findings, _phi_findings, _tenant_findings, _audit_findings, _retention_findings):
        findings |= check(req, config)
    return _report(findings)


def _age_days(day, today):
    return (date.fromisoformat(today) - date.fromisoformat(day)).days


def retention_actions(entries, max_days, today):
    purge, keep = [], []
    for e in sorted(entries, key=lambda e: e["id"]):
        (purge if _age_days(e["date"], today) > max_days and not e.get("hold") else keep).append(e["id"])
    return {"purge": purge, "keep": keep}


def pick_deployment(user_region, deployments):
    matching = sorted(d["name"] for d in deployments if d["residency"] == WANT.get(user_region))
    return matching[0] if matching else None
