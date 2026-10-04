import os
import sys
from pathlib import Path

# SOLUTION_DIR selects starter, reference or a planted wrong solution.
sys.path.insert(0, os.environ.get("SOLUTION_DIR", str(Path(__file__).resolve().parent.parent / "starter")))
from data_policy import check_deployment as _check
from data_policy import pick_deployment as _pick
from data_policy import retention_actions as _actions

GOOD = {"platform": "api", "zdr": True, "hipaa_baa": False, "model": "claude-sonnet-5-5", "inference_geo": "us", "region": None, "tenancy": "workspace-per-tenant",
        "pii_handling": "tokenise", "audit": {"store_prompts": False, "retain_days": 365}}
REQ = {"residency": "us", "phi": False, "zdr_required": True, "multi_tenant": True, "audit_min_days": 180, "audit_max_days": 400}


def cfg(**over):
    return {**GOOD, **over}


def req(**over):
    return {**REQ, **over}


def check(config, requirements):
    result = _check(config, requirements)
    assert isinstance(result, list), "check_deployment returned nothing"
    return result


def test_m1_a_compliant_deployment_has_no_findings():
    assert check(GOOD, REQ) == []


def test_e1_residency_is_pinned_by_the_request_or_by_the_region_the_platform_allows():
    us = req(zdr_required=False)
    assert check(cfg(inference_geo="global"), us) == ["residency-not-pinned"]
    assert check(cfg(inference_geo=None), us) == ["residency-not-pinned"]
    assert check(cfg(platform="aws-platform"), us) == []
    assert check(cfg(platform="bedrock", region="us-east-1", inference_geo=None), us) == []
    assert check(cfg(platform="bedrock", region="eu-west-1"), us) == ["residency-region"]
    assert check(cfg(platform="vertex", region="us-east5"), us) == []
    eu = req(zdr_required=False, residency="eu")
    assert check(cfg(), eu) == ["residency-unavailable"]
    assert check(cfg(platform="bedrock", region="eu-west-1"), eu) == []
    assert check(cfg(platform="vertex", region="europe-west4"), eu) == []
    assert check(cfg(platform="vertex", region="eu"), eu) == []
    assert check(cfg(platform="vertex", region="us-east5"), eu) == ["residency-region"]
    assert check(cfg(platform="bedrock", region=None), eu) == ["residency-region"]


def test_e2_zero_data_retention_is_an_arrangement_of_the_providers_own_platforms_and_not_of_every_model():
    assert check(cfg(zdr=False), REQ) == ["zdr-missing"]
    assert check(cfg(zdr=False), req(zdr_required=False)) == []
    assert check(cfg(platform="bedrock", region="us-east-1", zdr=True), REQ) == ["zdr-not-anthropics"]
    assert check(cfg(model="claude-fable-5-1"), REQ) == ["model-needs-retention"]
    assert check(cfg(model="claude-fable-5-1", zdr=False), REQ) == ["model-needs-retention", "zdr-missing"]
    assert check(cfg(model="claude-fable-5-1"), req(zdr_required=False)) == []


def test_e3_protected_health_information_needs_a_covered_platform_an_agreement_and_deidentified_input():
    phi = req(zdr_required=False, phi=True)
    assert check(cfg(), phi) == ["phi-no-baa"]
    assert check(cfg(hipaa_baa=True), phi) == []
    assert check(cfg(platform="aws-platform", hipaa_baa=True), phi) == ["phi-platform-unsupported"]
    assert check(cfg(platform="bedrock", region="us-east-1"), phi) == []
    assert check(cfg(hipaa_baa=True, pii_handling="none"), phi) == ["phi-not-deidentified"]
    assert check(cfg(hipaa_baa=True, pii_handling="redact"), phi) == []
    assert check({k: v for k, v in cfg(hipaa_baa=True).items() if k != "pii_handling"}, phi) == ["phi-not-deidentified"]


def test_e4_a_multi_tenant_service_needs_a_workspace_for_each_tenant():
    assert check(cfg(tenancy="shared"), REQ) == ["tenant-isolation"]
    assert check(cfg(tenancy="shared"), req(multi_tenant=False)) == []


def test_e5_an_audit_log_must_not_store_prompts_that_hold_sensitive_data():
    stores = {"store_prompts": True, "retain_days": 365}
    assert check(cfg(audit=stores), REQ) == []
    assert check(cfg(audit=stores, pii_handling="none"), REQ) == ["audit-stores-sensitive"]
    assert check(cfg(audit=stores, hipaa_baa=True), req(phi=True)) == ["audit-stores-sensitive"]
    assert check(cfg(audit={"store_prompts": False, "retain_days": 365}, pii_handling="none"), REQ) == []


def test_e6_audit_retention_stays_between_the_minimum_and_the_maximum():
    def days(n):
        return cfg(audit={"store_prompts": False, "retain_days": n})

    assert check(days(400), REQ) == [] and check(days(180), REQ) == []
    assert check(days(401), REQ) == ["retention-too-long"]
    assert check(days(179), REQ) == ["retention-too-short"]
    assert check(cfg(audit={"store_prompts": False}), REQ) == []
    assert check(days(10_000), {"residency": None}) == []


def test_e7_purge_only_what_is_past_the_retention_limit_and_not_on_hold():
    entries = [{"id": "old", "date": "2025-01-01"}, {"id": "edge", "date": "2025-01-31"}, {"id": "over", "date": "2025-01-30"},
               {"id": "held", "date": "2020-01-01", "hold": True}, {"id": "new", "date": "2026-01-30"}]
    result = _actions(entries, 365, "2026-01-31")
    assert isinstance(result, dict), "retention_actions returned nothing"
    assert result == {"purge": ["old", "over"], "keep": ["edge", "held", "new"]}
    assert _actions([], 30, "2026-01-31") == {"purge": [], "keep": []}


def test_e8_a_request_is_served_only_by_a_deployment_that_keeps_its_data_in_the_region():
    deployments = [{"name": "us-main", "residency": "us"}, {"name": "eu-main", "residency": "eu"}, {"name": "global-a", "residency": "global"}, {"name": "eu-backup", "residency": "eu"}]

    def pick(region, available=deployments):
        result = _pick(region, available)
        assert result is None or isinstance(result, str), "pick_deployment returned something that is not a name"
        return result

    assert pick("us") == "us-main"
    assert pick("eu") == "eu-backup"
    assert pick("other") == "global-a"
    assert pick("eu", [deployments[0], deployments[2]]) is None
    assert pick("other", deployments[:2]) is None
    assert pick("us", []) is None
