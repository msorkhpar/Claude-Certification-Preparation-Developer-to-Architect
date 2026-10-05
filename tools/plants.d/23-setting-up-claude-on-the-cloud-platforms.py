# Planted wrong solutions of module 23-setting-up-claude-on-the-cloud-platforms: exact replacements against the practice's reference (see tools/make_plants.py).
# Runs with PLANTS, X and both() in scope; names defined here are local to this file.

_ARN = "arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"
_AND = ',\n        "'

# Each plant adds or changes one thing and keeps the wanted action and the wanted resource, so the main case still passes and exactly one edge fails.
_P23 = {
    "wrong-wildcard-action": {"bedrock-policy.json": [('"bedrock-mantle:CreateInference"', '"bedrock-mantle:CreateInference"' + _AND + 'bedrock-mantle:*"')]},
    "wrong-non-invoke-action": {"bedrock-policy.json": [('"bedrock-mantle:CreateInference"', '"bedrock-mantle:CreateInference"' + _AND + 'bedrock:ListFoundationModels"')]},
    "wrong-resource-star": {"bedrock-policy.json": [(f'"{_ARN}"', f'"{_ARN}"' + _AND + '*"')]},
    "wrong-region-wildcard": {"bedrock-policy.json": [(f'"{_ARN}"', f'"{_ARN}"' + _AND + 'arn:aws:bedrock:*::foundation-model/anthropic.claude-sonnet-5-5"')]},
    "wrong-undocumented-region": {"bedrock-policy.json": [(f'"{_ARN}"', f'"{_ARN}"' + _AND + 'arn:aws:bedrock:xx-north-9::foundation-model/anthropic.claude-sonnet-5-5"')]},
    "wrong-old-policy-version": {"bedrock-policy.json": [('"2012-10-17"', '"2008-10-17"')]},
    "wrong-deny-statement": {"bedrock-policy.json": [('"Effect": "Allow"', '"Effect": "Deny"')]},
    "wrong-predefined-role": {"vertex.json": [('"projects/example-project/roles/claudeInvoker"', '"roles/aiplatform.user"')]},
    "wrong-extra-permission": {"vertex.json": [('["aiplatform.endpoints.predict"]', '["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]')]},
    "wrong-global-for-eu": {"vertex.json": [('"endpoint": "eu"', '"endpoint": "global"')]},
    "wrong-regional-new-model": {"vertex.json": [('"endpoint": "eu"', '"endpoint": "europe-west1"')]},
    "wrong-bare-model-id": {"bedrock-policy.json": [(f'"{_ARN}"', f'"{_ARN}"' + _AND + 'arn:aws:bedrock:us-east-1::foundation-model/claude-sonnet-5-5"')]},
    "wrong-vertex-prefixed-model": {"vertex.json": [('"model": "claude-sonnet-5-5"', '"model": "anthropic.claude-sonnet-5-5"')]},
    "wrong-haiku-undated": {"vertex.json": [('"model": "claude-sonnet-5-5"', '"model": "claude-haiku-4-5"')]},
    "wrong-quota-over": {"quotas.json": [('"input_tpm": 4000000', '"input_tpm": 8000000')]},
}

PLANTS[f"{X}/23-setting-up-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    lang: ("bedrock-policy.json", dict(_P23)) for lang in ("python", "typescript", "java", "kotlin")
}
