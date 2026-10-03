"""Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.

The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
predict permission (Google's IAM documentation, read 2026-10-02).
"""
BROAD = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "bedrock:*", "Resource": "*"}]}
NARROW = {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["bedrock-mantle:CreateInference"],
                                                   "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]}]}
ROLES = {"predefined": {"id": "roles/aiplatform.user", "permissions": ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]},
         "custom": {"id": "projects/example-project/roles/claudeInvoker", "permissions": ["aiplatform.endpoints.predict"]}}


def as_list(value):
    return value if isinstance(value, list) else [value]


def review_policy(policy):
    findings = []
    for number, statement in enumerate(as_list(policy["Statement"]), start=1):
        for action in as_list(statement.get("Action", [])):
            if "*" in action:
                findings.append(f"statement {number}: action {action} is a wildcard")
        for resource in as_list(statement.get("Resource", [])):
            if "*" in resource:
                findings.append(f"statement {number}: resource {resource} names more than one model")
        if statement.get("Effect") != "Allow":
            findings.append(f"statement {number}: effect is {statement.get('Effect')}")
    return findings


def review_role(role):
    findings = []
    if role["id"].startswith("roles/"):
        findings.append(f"{role['id']} is a predefined role, which carries more than the caller needs")
    extra = [p for p in role["permissions"] if p != "aiplatform.endpoints.predict"]
    if extra:
        findings.append("extra permissions: " + ", ".join(extra))
    return findings


def main():
    for name, policy in (("broad policy", BROAD), ("narrow policy", NARROW)):
        found = review_policy(policy)
        print(f"{name}: {len(found)} finding(s)")
        for item in found:
            print("  -", item)
    for name, role in ROLES.items():
        found = review_role(role)
        print(f"{name} role: {len(found)} finding(s)")
        for item in found:
            print("  -", item)


if __name__ == "__main__":
    main()
