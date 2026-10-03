// Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
// The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
// named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
// predict permission (Google's IAM documentation, read 2026-10-02).
export const BROAD = { Version: "2012-10-17", Statement: [{ Effect: "Allow", Action: "bedrock:*", Resource: "*" }] };
export const NARROW = {
  Version: "2012-10-17",
  Statement: [{ Effect: "Allow", Action: ["bedrock-mantle:CreateInference"], Resource: ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"] }],
};
export const ROLES: Record<string, { id: string; permissions: string[] }> = {
  predefined: { id: "roles/aiplatform.user", permissions: ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"] },
  custom: { id: "projects/example-project/roles/claudeInvoker", permissions: ["aiplatform.endpoints.predict"] },
};

const asList = (value: any): any[] => (Array.isArray(value) ? value : [value]);

export function reviewPolicy(policy: any): string[] {
  const findings: string[] = [];
  asList(policy.Statement).forEach((statement, i) => {
    const number = i + 1;
    for (const action of asList(statement.Action ?? [])) if (action.includes("*")) findings.push(`statement ${number}: action ${action} is a wildcard`);
    for (const resource of asList(statement.Resource ?? [])) if (resource.includes("*")) findings.push(`statement ${number}: resource ${resource} names more than one model`);
    if (statement.Effect !== "Allow") findings.push(`statement ${number}: effect is ${statement.Effect}`);
  });
  return findings;
}

export function reviewRole(role: { id: string; permissions: string[] }): string[] {
  const findings: string[] = [];
  if (role.id.startsWith("roles/")) findings.push(`${role.id} is a predefined role, which carries more than the caller needs`);
  const extra = role.permissions.filter((p) => p !== "aiplatform.endpoints.predict");
  if (extra.length) findings.push("extra permissions: " + extra.join(", "));
  return findings;
}

function main() {
  for (const [name, policy] of [["broad policy", BROAD], ["narrow policy", NARROW]] as const) {
    const found = reviewPolicy(policy);
    console.log(`${name}: ${found.length} finding(s)`);
    for (const item of found) console.log("  -", item);
  }
  for (const [name, role] of Object.entries(ROLES)) {
    const found = reviewRole(role);
    console.log(`${name} role: ${found.length} finding(s)`);
    for (const item of found) console.log("  -", item);
  }
}

if (import.meta.main) main();
