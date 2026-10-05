# Case lists of module 23-setting-up-claude-on-the-cloud-platforms: one entry per practice (see tools/make_cases.py).
# Runs with PRACTICES and X in scope; names defined here are local to this file.

PRACTICES[f"{X}/23-setting-up-claude-on-the-cloud-platforms/unit-01/practice-1"] = {
    "name": "platformconfig", "suite": "PlatformConfigTest", "langs": ["python", "typescript", "java", "kotlin"],
    "cases": [
        ("m1", "main", "the policy allows invoking one model in one region and nothing else"),
        ("e1", "edge", "no action is a wildcard and every action is an invoke action"),
        ("e2", "edge", "every resource arn names one documented region and one model"),
        ("e3", "edge", "every statement allows and the policy uses the current version"),
        ("e4", "edge", "the vertex role is a custom role that can only predict"),
        ("e5", "edge", "the vertex endpoint keeps the data where residency says and serves the model"),
        ("e6", "edge", "model ids use each platforms own form"),
        ("e7", "edge", "the quota request stays under the self service ceiling"),
    ],
    "plants": {
        "wrong-wildcard-action": (["e1", "m1"], "grants a wildcard action instead of the invoke action"),
        "wrong-resource-star": (["e2", "m1"], "allows the action on every resource"),
        "wrong-region-wildcard": (["e2", "m1"], "leaves the region of the model ARN as a wildcard"),
        "wrong-old-policy-version": (["e3"], "uses the 2008 policy language version"),
        "wrong-predefined-role": (["e4"], "binds a broad predefined role instead of a custom role"),
        "wrong-extra-permission": (["e4"], "adds a deploy permission to the role"),
        "wrong-global-for-eu": (["e5"], "sends EU-resident traffic to the global endpoint"),
        "wrong-regional-new-model": (["e5"], "picks a specific region for a model that only the global and multi-region endpoints serve"),
        "wrong-bare-model-id": (["e6", "m1"], "names the model without the Bedrock provider prefix"),
        "wrong-quota-over": (["e7"], "asks for more input tokens per minute than the self-service ceiling"),
    },
}
