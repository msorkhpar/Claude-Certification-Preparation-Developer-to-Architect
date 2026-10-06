import json
import os
from pathlib import Path

# The solution is the file beside this test.
DIR = Path(str(Path(__file__).resolve().parent.parent / "project"))

INVOKE = {"bedrock-mantle:CreateInference", "bedrock:InvokeModel", "bedrock:InvokeModelWithResponseStream"}
# the AWS regions listed for Claude in Amazon Bedrock on 2026-10-02
REGIONS = {"af-south-1", "ap-northeast-1", "ap-northeast-2", "ap-northeast-3", "ap-south-1", "ap-south-2", "ap-southeast-1", "ap-southeast-2",
           "ap-southeast-3", "ap-southeast-4", "ca-central-1", "ca-west-1", "eu-central-1", "eu-central-2", "eu-north-1", "eu-south-1",
           "eu-south-2", "eu-west-1", "eu-west-2", "eu-west-3", "il-central-1", "me-central-1", "sa-east-1", "us-east-1", "us-east-2",
           "us-west-1", "us-west-2"}
VERTEX_MODELS = {"claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-4-5@20251001", "claude-sonnet-4-6"}
WANTED_ARN = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"


def load(name):
    return json.loads((DIR / name).read_text())


def as_list(value):
    return value if isinstance(value, list) else [value]


def statements():
    doc = load("bedrock-policy.json")
    return as_list(doc.get("Statement", []))


def actions():
    return [a for s in statements() for a in as_list(s.get("Action", []))]


def resources():
    return [r for s in statements() for r in as_list(s.get("Resource", []))]


def test_m1_the_policy_allows_invoking_one_model_in_one_region_and_nothing_else():
    stmts = statements()
    assert len(stmts) == 1
    assert set(actions()) & INVOKE, "the policy grants no invoke action"
    assert WANTED_ARN in resources()


def test_e1_no_action_is_a_wildcard_and_every_action_is_an_invoke_action():
    assert actions(), "the policy has no action"
    for action in actions():
        assert "*" not in action, action
        assert action in INVOKE, action


def test_e2_every_resource_arn_names_one_documented_region_and_one_model():
    assert resources(), "the policy has no resource"
    for arn in resources():
        assert arn != "*" and "*" not in arn, arn
        parts = arn.split(":")
        assert len(parts) == 6 and parts[:3] == ["arn", "aws", "bedrock"], arn
        assert parts[3] in REGIONS, arn
        assert parts[5].startswith("foundation-model/"), arn


def test_e3_every_statement_allows_and_the_policy_uses_the_current_version():
    doc = load("bedrock-policy.json")
    assert doc.get("Version") == "2012-10-17"
    assert statements()
    for s in statements():
        assert s.get("Effect") == "Allow"
        assert "NotAction" not in s and "NotResource" not in s


def test_e4_the_vertex_role_is_a_custom_role_that_can_only_predict():
    role = load("vertex.json").get("role", {})
    assert str(role.get("id", "")).split("/")[0] in ("projects", "organizations"), role.get("id")
    assert role.get("permissions") == ["aiplatform.endpoints.predict"]


def test_e5_the_vertex_endpoint_keeps_the_data_where_residency_says_and_serves_the_model():
    cfg = load("vertex.json")
    endpoint, residency, model = cfg.get("endpoint"), cfg.get("residency"), cfg.get("model")
    if residency == "eu":
        assert endpoint == "eu" or str(endpoint).startswith("europe-"), endpoint
    if residency == "us":
        assert endpoint == "us" or str(endpoint).startswith("us-"), endpoint
    if endpoint not in ("global", "us", "eu"):
        assert model == "claude-sonnet-4-6", f"{model} is not served on a specific region"


def test_e6_model_ids_use_each_platforms_own_form():
    for arn in resources():
        if arn.startswith("arn:aws:bedrock:"):
            assert arn.split("/")[-1].startswith("anthropic.claude-"), arn
    model = str(load("vertex.json").get("model"))
    assert not model.startswith("anthropic."), model
    assert model in VERTEX_MODELS, model
    if model.startswith("claude-haiku-4-5"):
        assert model == "claude-haiku-4-5@20251001"


def test_e7_the_quota_request_stays_under_the_self_service_ceiling():
    quota = load("quotas.json")
    assert quota.get("input_tpm", 0) > 0 and quota.get("output_tpm", 0) > 0
    if not quota.get("anthropic_approval"):
        assert quota["input_tpm"] <= 5_000_000
        assert quota["output_tpm"] <= 500_000
