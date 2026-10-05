"""A data policy for a Claude deployment: the findings of a configuration check, what a retention rule purges, and the deployment that may serve a user. See ../../statement.md."""
import logging
from datetime import date

log = logging.getLogger(__name__)

SAFE_INPUT = ("tokenise", "redact")  # pii_handling values that keep identifiers out of the prompt
WANT = {"us": "us", "eu": "eu", "other": "global"}  # the residency of the deployment that may serve a user region


def _report(findings):
    """TODO 1 of 6 (unlocks m1): the answer of check_deployment.

    Receives the set of finding ids. Returns them as a list sorted by text, or None when there is nothing to report yet.
    Example: _report({"zdr-missing", "phi-no-baa"}) -> ["phi-no-baa", "zdr-missing"]
    """
    return None


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
    """TODO 2 of 6 (unlocks e2): the findings about zero data retention.

    Receives the requirements and the configuration. Returns a set of finding ids. When `zdr_required` is set: `zdr-not-anthropics` on the
    platforms `bedrock` and `vertex` (and nothing else); on the others `zdr-missing` when `zdr` is not set, and `model-needs-retention` when the
    model name starts with `claude-fable`. Nothing is required, nothing is found.
    Example: platform "api", zdr False, zdr_required True -> {"zdr-missing"}
    """
    return set()


def _phi_findings(req, config):
    """TODO 3 of 6 (unlocks e3): the findings about protected health information.

    Receives the requirements and the configuration. Returns a set of finding ids. When `phi` is set: `phi-no-baa` on the platform `api` without
    `hipaa_baa`; `phi-platform-unsupported` on `aws-platform`; `phi-not-deidentified` when `pii_handling` is not in SAFE_INPUT.
    Example: platform "api", hipaa_baa False, pii_handling "tokenise", phi True -> {"phi-no-baa"}
    """
    return set()


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
    """TODO 4 of 6 (unlocks e5): the finding about stored prompts.

    Receives the requirements and the configuration; the audit settings are `config["audit"]` (it may be missing). Returns
    {"audit-stores-sensitive"} when `store_prompts` is set and either `phi` is required or `pii_handling` is not in SAFE_INPUT; an empty set otherwise.
    Example: store_prompts True, pii_handling "none" -> {"audit-stores-sensitive"}
    """
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
    """TODO 5 of 6 (unlocks e7): what a retention rule purges.

    Receives the entries (each has `id` and an ISO `date`, and may have `hold`), the limit in days and today's ISO date. Returns
    {"purge": [ids], "keep": [ids]}, both sorted by id. An entry is purged when it is older than the limit (strictly) and is not on hold.
    Example: an entry dated 365 days before today with max_days 365 is kept; one dated 366 days before is purged.
    """
    return None


def pick_deployment(user_region, deployments):
    """TODO 6 of 6 (unlocks e8): the deployment that may serve a user.

    Receives the user's region ("us", "eu" or "other") and the deployments (each has `name` and `residency`). Returns the first name, in
    alphabetical order, of the deployments whose residency is WANT[user_region], or None when there is none (a global deployment does not serve "us" or "eu").
    Example: "eu" with eu-main and eu-backup -> "eu-backup"
    """
    return None
