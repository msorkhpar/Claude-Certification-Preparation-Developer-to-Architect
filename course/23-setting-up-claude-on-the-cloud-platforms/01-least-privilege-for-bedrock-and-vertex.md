# Least privilege for Bedrock and Vertex

**Level:** Developer · **Module 23:** Setting up Claude on the cloud platforms · **Page 1 of 2**
**Exams:** DV2

**After this page you can** read an AWS IAM policy for Claude on Bedrock and find what it allows beyond its job, write a policy that
names one action and one model, choose a custom role over a predefined one on Google Cloud, and review both with a few checks that a
program can run.

Checked on 2026-10-02 against the Claude API documentation (Claude in Amazon Bedrock, Claude on Google Cloud), the Amazon Bedrock User Guide
page on identity-based policy examples, the Google Cloud documentation on Agent Platform access control with IAM and on Claude models, and by
running the example offline in the course container (Python and TypeScript). Every name in the example is a placeholder: no real account,
project or key appears.

## Why it matters

The first thing a cloud team sets up for Claude is not a prompt but a permission: which identity may call which model. A policy that
says "everything on everything" works on the first try and is the one a security review rejects. The exam asks what a least-privilege
policy for model access looks like on each platform and which of two similar-looking policies is the safer one.

## The idea

### The rule behind both platforms

AWS states least privilege in one line in the Bedrock guide: "Apply least-privilege permissions". It continues: "When you set permissions with IAM
policies, grant only the permissions required to perform a task." You do this by "defining the actions that can be taken on specific resources
under specific conditions". The starting point is closed: "By default, users and roles don't have permission to create or modify Amazon Bedrock
resources." So a policy adds what a job needs and nothing more.

Google says the same of its roles. Predefined roles are convenient, "but custom roles are recommended because you create them, so you can limit
their access to only the permissions that are required". The reason is plain: "Predefined roles often contain more permissions than you need."

### An AWS policy, piece by piece

An IAM policy is a JSON document with a `Version` and a list of statements. Each statement has an `Effect` (`Allow` or `Deny`), an `Action`
(or list of actions) and a `Resource` (or list). Anthropic's Bedrock page describes the role for federated access: "Create an IAM role scoped to your
Claude models. The trust policy names your identity provider (SAML, OIDC, or AWS Identity Center). The permissions policy grants
`bedrock-mantle:CreateInference` only on the allowed model ARNs." The AWS examples use two older invoke actions, `bedrock:InvokeModel` and
`bedrock:InvokeModelWithResponseStream`. The current version string in every AWS example is `2012-10-17`.

A policy to invoke one model has this shape. The account-free ARN is a placeholder for the form
`arn:aws:bedrock:<region>::foundation-model/<model id>`, with the Bedrock model id `anthropic.claude-sonnet-5-5`:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["bedrock-mantle:CreateInference"],
      "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]
    }
  ]
}
```

One caution is part of the lesson. AWS documents the `foundation-model` ARN shape for the `bedrock:InvokeModel` actions. The resource type to use with the
`bedrock-mantle` action is something to confirm in AWS's service authorization reference before real use. The course checks the shape and nothing
else about the ARN, and it did not verify the resource type for the `bedrock-mantle` action.

### What goes wrong in a policy

Four findings cover most reviews, and a program can find each one:

| Finding | Why it is wrong |
|---|---|
| An action with a wildcard, such as `bedrock:*` | it grants every Bedrock action, including ones that manage resources |
| A resource of `*` | it names every model, including ones the application should not call |
| An `Effect` other than `Allow` in an allow-list policy | a `Deny` statement belongs in a separate guardrail |
| `NotAction` or `NotResource` | they allow everything except a list, the opposite of least privilege |

AWS shows wildcards in a deny policy: "To deny inference access to all foundation models, use * for the model ID." That is the legitimate use, as a guardrail
that blocks, and it explains why the same character is a defect in an allow. The page also notes that a deny on `InvokeModel` is enough for the neighbours: "Other
actions, such as Converse and StartAsyncInvoke, are blocked automatically when InvokeModel is denied."

AWS adds two refinements. "Use conditions in IAM policies to further restrict access", for instance to require SSL. And "IAM Access Analyzer validates new and existing
policies so that the policies adhere to the IAM policy language (JSON) and IAM best practices". Both come after the basics: the right action on the right resource.

### Google Cloud: a custom role that can only predict

A Google Cloud caller is a service account, and what it may do comes from the role it holds. The page names the permission that matches the job: "you can
create a custom role with the aiplatform.endpoints.predict permission, and then assign the role to a service account on an endpoint". It spells out the
effect: "This grants the service account the ability to call the endpoint for predictions, but not the ability of controlling the endpoint."

The page puts it this way: "You grant access by assigning IAM roles to principals." There are three kinds of role. **Basic** roles (Owner, Editor, Viewer) "provide access control to your Agent Platform resources at the project level, and are common to all Google Cloud services". **Predefined** roles
bundle related permissions at the project level. **Custom** roles hold exactly the permissions you list. A predefined role such as `roles/aiplatform.user` is easy to
assign and carries more than a caller needs. One warning: "granting the aiplatform.endpoints.deploy permission might allow a user to export other deployed or
deployable models from the project", so an invoker role should not carry it.

### The example

The example is a small reviewer. It checks a policy for wildcard actions, wildcard resources and a non-Allow effect, and a role for a predefined id and for
extra permissions. It runs on four configurations: a broad AWS policy, a narrow one, a predefined Google role and a custom one. The configurations
are written for the page.

<!-- example: m23-policy-review tabs: python,typescript,java,kotlin -->
```python
"""Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.

The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
predict permission (Google's IAM documentation, read 2026-10-02).
"""
import logging

log = logging.getLogger(__name__)

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
```
```text
broad policy: 2 finding(s)
  - statement 1: action bedrock:* is a wildcard
  - statement 1: resource * names more than one model
narrow policy: 0 finding(s)
predefined role: 2 finding(s)
  - roles/aiplatform.user is a predefined role, which carries more than the caller needs
  - extra permissions: aiplatform.endpoints.deploy
custom role: 0 finding(s)
```
```typescript
// Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
// The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
// named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
// predict permission (Google's IAM documentation, read 2026-10-02).
import { logger } from "./logger.ts";
const log = logger("policy_review");
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
```
```text
broad policy: 2 finding(s)
  - statement 1: action bedrock:* is a wildcard
  - statement 1: resource * names more than one model
narrow policy: 0 finding(s)
predefined role: 2 finding(s)
  - roles/aiplatform.user is a predefined role, which carries more than the caller needs
  - extra permissions: aiplatform.endpoints.deploy
custom role: 0 finding(s)
```
```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
 *
 * <p>The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
 * named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
 * predict permission (Google's IAM documentation, read 2026-10-02).
 */
public final class PolicyReview {
    private static final System.Logger LOG = System.getLogger(PolicyReview.class.getName());
    static final String BROAD = """
        {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "bedrock:*", "Resource": "*"}]}""";
    static final String NARROW = """
        {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["bedrock-mantle:CreateInference"],
          "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]}]}""";
    static final Map<String, String> ROLES = Map.of(
        "predefined", """
            {"id": "roles/aiplatform.user", "permissions": ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]}""",
        "custom", """
            {"id": "projects/example-project/roles/claudeInvoker", "permissions": ["aiplatform.endpoints.predict"]}""");

    private static final ObjectMapper JSON = new ObjectMapper();

    @SuppressWarnings("unchecked")
    static Map<String, Object> parse(String json) {
        try {
            return JSON.readValue(json, Map.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    static List<Object> asList(Object value) {
        if (value == null) return List.of();
        return value instanceof List<?> l ? (List<Object>) l : List.of(value);
    }

    @SuppressWarnings("unchecked")
    static List<String> reviewPolicy(Map<String, Object> policy) {
        List<String> findings = new ArrayList<>();
        int number = 0;
        for (Object item : asList(policy.get("Statement"))) {
            Map<String, Object> statement = (Map<String, Object>) item;
            number++;
            for (Object action : asList(statement.get("Action"))) {
                if (((String) action).contains("*")) findings.add("statement " + number + ": action " + action + " is a wildcard");
            }
            for (Object resource : asList(statement.get("Resource"))) {
                if (((String) resource).contains("*")) findings.add("statement " + number + ": resource " + resource + " names more than one model");
            }
            if (!"Allow".equals(statement.get("Effect"))) findings.add("statement " + number + ": effect is " + statement.get("Effect"));
        }
        return findings;
    }

    @SuppressWarnings("unchecked")
    static List<String> reviewRole(Map<String, Object> role) {
        List<String> findings = new ArrayList<>();
        String id = (String) role.get("id");
        if (id.startsWith("roles/")) findings.add(id + " is a predefined role, which carries more than the caller needs");
        List<String> extra = new ArrayList<>();
        for (Object p : (List<Object>) role.get("permissions")) if (!"aiplatform.endpoints.predict".equals(p)) extra.add((String) p);
        if (!extra.isEmpty()) findings.add("extra permissions: " + String.join(", ", extra));
        return findings;
    }

    private static void show(String name, List<String> found) {
        System.out.println(name + ": " + found.size() + " finding(s)");
        for (String item : found) System.out.println("  - " + item);
    }

    public static void main(String[] args) {
        show("broad policy", reviewPolicy(parse(BROAD)));
        show("narrow policy", reviewPolicy(parse(NARROW)));
        for (String name : List.of("predefined", "custom")) show(name + " role", reviewRole(parse(ROLES.get(name))));
    }
}
```
```text
broad policy: 2 finding(s)
  - statement 1: action bedrock:* is a wildcard
  - statement 1: resource * names more than one model
narrow policy: 0 finding(s)
predefined role: 2 finding(s)
  - roles/aiplatform.user is a predefined role, which carries more than the caller needs
  - extra permissions: aiplatform.endpoints.deploy
custom role: 0 finding(s)
```
```kotlin
import com.fasterxml.jackson.databind.ObjectMapper

private val log = System.getLogger("policy_review")

/**
 * Reading a platform configuration the way a reviewer would: two IAM policies and one Vertex role, with findings.
 *
 * The policies are written for this page (placeholder account-free ARNs and names). The checks are the ones the module teaches:
 * named actions instead of wildcards, one model resource instead of `*`, an Allow-only policy, and a role that holds only the
 * predict permission (Google's IAM documentation, read 2026-10-02).
 */
val BROAD = """{"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": "bedrock:*", "Resource": "*"}]}"""
val NARROW = """
    {"Version": "2012-10-17", "Statement": [{"Effect": "Allow", "Action": ["bedrock-mantle:CreateInference"],
      "Resource": ["arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5"]}]}"""
val ROLES = mapOf(
    "predefined" to """{"id": "roles/aiplatform.user", "permissions": ["aiplatform.endpoints.predict", "aiplatform.endpoints.deploy"]}""",
    "custom" to """{"id": "projects/example-project/roles/claudeInvoker", "permissions": ["aiplatform.endpoints.predict"]}""",
)

@Suppress("UNCHECKED_CAST")
fun parse(json: String): Map<String, Any?> = ObjectMapper().readValue(json, Map::class.java) as Map<String, Any?>

fun asList(value: Any?): List<Any?> = when (value) {
    null -> emptyList()
    is List<*> -> value
    else -> listOf(value)
}

fun reviewPolicy(policy: Map<String, Any?>): List<String> {
    val findings = mutableListOf<String>()
    for ((index, item) in asList(policy["Statement"]).withIndex()) {
        val number = index + 1
        @Suppress("UNCHECKED_CAST") val statement = item as Map<String, Any?>
        for (action in asList(statement["Action"])) if ("*" in action as String) findings += "statement $number: action $action is a wildcard"
        for (resource in asList(statement["Resource"])) if ("*" in resource as String) findings += "statement $number: resource $resource names more than one model"
        if (statement["Effect"] != "Allow") findings += "statement $number: effect is ${statement["Effect"]}"
    }
    return findings
}

fun reviewRole(role: Map<String, Any?>): List<String> {
    val findings = mutableListOf<String>()
    val id = role["id"] as String
    if (id.startsWith("roles/")) findings += "$id is a predefined role, which carries more than the caller needs"
    val extra = asList(role["permissions"]).filter { it != "aiplatform.endpoints.predict" }
    if (extra.isNotEmpty()) findings += "extra permissions: " + extra.joinToString(", ")
    return findings
}

private fun show(name: String, found: List<String>) {
    println("$name: ${found.size} finding(s)")
    for (item in found) println("  - $item")
}

fun main() {
    show("broad policy", reviewPolicy(parse(BROAD)))
    show("narrow policy", reviewPolicy(parse(NARROW)))
    for (name in listOf("predefined", "custom")) show("$name role", reviewRole(parse(ROLES.getValue(name))))
}
```
```text
broad policy: 2 finding(s)
  - statement 1: action bedrock:* is a wildcard
  - statement 1: resource * names more than one model
narrow policy: 0 finding(s)
predefined role: 2 finding(s)
  - roles/aiplatform.user is a predefined role, which carries more than the caller needs
  - extra permissions: aiplatform.endpoints.deploy
custom role: 0 finding(s)
```
<!-- /example -->

Read the output. The broad policy has two findings, a wildcard action and a resource that names more than one model. The narrow policy has none. The predefined role
has two: it is predefined, and it carries `aiplatform.endpoints.deploy` besides predict. The custom role has none. A reviewer that returns a list of findings
is easy to test, which is the idea of this module's practice.

## Traps

1. **Using `bedrock:*` and `*` because it works on the first try.** It grants every action on every model. Name the invoke action and the model.
2. **Choosing the predefined role to save time.** It carries permissions the application never uses, and one of them can export models.
3. **Treating a `Deny` with `*` as the same defect as an `Allow` with `*`.** In a guardrail, the wildcard is the point. In an allow-list it is the defect.

## Quiz

1. A policy allows bedrock:* on * for an application's role. A reviewer asks for least privilege. Which rewrite fits?
   - **a**: Keep the policy as written and add a condition that requires SSL
   - **b**: Keep the wildcard action but name the single model's ARN in the resource
   - **c**: Replace the wildcard with a deny statement for every other model
   - **d**: List the one invoke action and name the single model's ARN

2. A service account must only call a Claude endpoint on Google Cloud. The team compares a ready-made grant bundle with one it authors. What does the page favour?
   - **a**: The tailored role with just the predict permission
   - **b**: A predefined role, since the platform maintains it for the team
   - **c**: A basic role such as Editor, since it covers every service at once
   - **d**: No role at all, since a service account works without any grant

3. A team adds the deploy permission to its invoker role just in case. What does the Google page warn?
   - **a**: It blocks the predict permission until the deploy one is removed
   - **b**: It makes every prediction slower on the endpoint that holds it
   - **c**: It opens a route to exporting other models held in the project
   - **d**: It has no effect at all, because deploy only adds serving capacity

<details>
<summary>Answer key</summary>

1. **d**. The page says to "grant only the permissions required to perform a task", by "defining the actions that can be taken on specific resources". *b* is ruled out because a wildcard action still grants more than "the permissions required to perform a task". *c* is ruled out because "By default, users and roles don't have permission to create or modify Amazon Bedrock resources", so a policy should add what is needed and not rely on denies. *a* is ruled out because conditions come after the basics: "defining the actions that can be taken on specific resources under specific conditions".
2. **a**. The page says "custom roles are recommended because you create them, so you can limit their access to only the permissions that are required". *b* is ruled out because "Predefined roles often contain more permissions than you need." *c* is ruled out because basic roles "provide access control to your Agent Platform resources at the project level, and are common to all Google Cloud services", which is broad. *d* is ruled out because "You grant access by assigning IAM roles to principals."
3. **c**. The page warns that the deploy permission "might allow a user to export other deployed or deployable models from the project". *b* is ruled out because the predict permission "grants the service account the ability to call the endpoint for predictions", and the page says nothing of speed. *a* is ruled out because the same sentence shows the predict permission grants "the ability to call the endpoint for predictions" on its own. *d* is ruled out because the warning is that it "might allow a user to export other deployed or deployable models from the project".

</details>
