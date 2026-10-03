# Residency, quotas, logging and the configuration practice

**Level:** Developer · **Module 23:** Setting up Claude on the cloud platforms · **Page 2 of 2**
**Exams:** DV2

**After this page you can** pick the Vertex endpoint that keeps data where a residency rule says, state the Bedrock quota and the point at which
Anthropic must approve more, turn on the logging both platforms recommend, and write the three configuration files that the practice's tests
review.

Checked on 2026-10-02 against the Claude API documentation (Claude in Amazon Bedrock, Claude on Google Cloud), the Google Cloud page on Claude models
and, for the policy rules, the Bedrock User Guide and the Google IAM documentation of page 1. The practice runs offline in the course container in Python,
TypeScript, Java and Kotlin. Every name in the files is a placeholder.

## Why it matters

Set-up is where a Claude project meets the company's rules: data must stay in a geography, traffic must fit a quota, activity must be logged. These are
settings, not code, and they are easy to get wrong without noticing until an audit or a throttled launch. The exam asks which setting satisfies which rule.

## The idea

### Getting access

Both platforms gate models. On Bedrock, "Amazon Bedrock sets access criteria for each Claude model individually", and a model is enabled for the
account before use. On Google Cloud, "To access a Claude on Google Cloud model, go to its Model Garden model card". The Google page also says the
models are "fully managed and serverless models as APIs", billed pay as you go or at a fixed fee with provisioned throughput, and that access through
Agent Platform "meets the FedRAMP High requirements".

### Residency on Google Cloud

The endpoint decides where a request may be processed. Google Cloud has three kinds. "Global endpoints: Dynamic routing for maximum availability". "Multi-region
endpoints: Dynamic routing within a geographic area (for example, the United States or the European Union) for data residency with high availability".
"Regional endpoints: Guaranteed data routing through specific geographic regions". Regional and multi-region endpoints "include a 10% pricing premium over global
endpoints". For an EU residency rule, the choices are the `eu` multi-region endpoint or a `europe-` region.

The catch, from module 22, is the model: a specific region serves `claude-sonnet-4-6` only. A project that must stay in the EU and use Sonnet 5.5 therefore
uses the `eu` multi-region endpoint, at the premium. The Vertex model id is the plain name, `claude-sonnet-5-5`, with no `anthropic.` prefix.

On Bedrock the equivalent choice is the endpoint type: a global endpoint, or a regional one that "resolves to the single AWS region you specify, for data-residency
requirements", at the same 10 percent premium. The region is also the part of the address that must be named in each request.

### The Bedrock quota

The quota is stated in tokens per minute: "Default quota is 2 million input tokens per minute (TPM)." The ceiling for a self-service request is
"You can request up to 5 million input TPM and 500,000 output TPM without additional Anthropic approval." Above those figures, Anthropic has to approve. A
quota request is therefore a small record: the input rate, the output rate and whether approval was granted.

### Logging

Both pages ask for a record. On Bedrock, the service "emits logs to both CloudWatch and CloudTrail", and Anthropic "recommends retaining activity logs on at
least a 30-day rolling basis". On Google Cloud: "Enable 30-day request-response logging of your prompt and completion activity to track any model misuse by your
users." The Vertex page words the advice as "Anthropic recommends that you log your activity on at least a 30-day rolling basis to understand your activity and
investigate any potential misuse." and adds a reassurance: "Turning on this service does not give Google or Anthropic any access to your content."

## The practice: platform access as configuration, graded by tests

Here the set-up is three JSON files and the tests are the reviewer. `bedrock-policy.json` is an IAM policy that lets the application invoke Claude Sonnet 5.5
in `us-east-1` and do nothing else. `vertex.json` is a Vertex set-up for a service account in an EU-resident project that can call Sonnet 5.5 and do nothing else.
`quotas.json` is a Bedrock quota request. The files are the same in every language folder, and the four test suites ask the same questions. The statement is in
`exercises/23-setting-up-claude-on-the-cloud-platforms/unit-01/practice-1/statement.md`; the starter files fail every test.

The rules, from this module and the last:

- The policy has the version `2012-10-17`, only `Allow` statements, and no `NotAction` or `NotResource`.
- Every action is one of the three invoke actions; no wildcard anywhere in an action.
- Every resource is a model ARN with a region from the Bedrock region list, the `anthropic.` model id form, and no wildcard.
- The Vertex role is a custom role (its `id` starts with `projects/` or `organizations/`, not `roles/`) whose only permission is `aiplatform.endpoints.predict`.
- Residency `eu` needs the `eu` multi-region endpoint or a `europe-` region; `us` needs `us` or a `us-` region. A specific region serves `claude-sonnet-4-6` only.
- A Vertex model id never carries the `anthropic.` prefix; the dated Haiku id is `claude-haiku-4-5@20251001`.
- Without Anthropic's approval a quota request is at most 5,000,000 input tokens and 500,000 output tokens per minute.

| Id | What it checks |
|---|---|
| `m1` | The policy allows invoking one model in one region and nothing else |
| `e1` | No action is a wildcard and every action is an invoke action |
| `e2` | Every resource ARN names one documented region and one model |
| `e3` | Every statement allows, and the policy uses the current version |
| `e4` | The Vertex role is a custom role that can only predict |
| `e5` | The Vertex endpoint keeps the data where residency says and serves the model |
| `e6` | Model ids use each platform's own form |
| `e7` | The quota request stays under the self-service ceiling |

The ARN rule is a shape check only. The `foundation-model` form is the one AWS documents for the `bedrock:InvokeModel` actions, and the resource type for the
`bedrock-mantle` action is to be confirmed in AWS's service authorization reference before real use.

## Traps

1. **Choosing a specific EU region for a new model.** A specific region serves `claude-sonnet-4-6` only, so a project that needs Sonnet 5.5 in the EU uses the `eu`
   multi-region endpoint instead.
2. **Asking for more than the self-service ceiling without approval.** Past 5 million input and 500,000 output tokens per minute, Anthropic has to approve.
3. **Granting the broad role to get started.** A predefined role holds more than the predict permission, and the practice's tests fail it.

## Quiz

1. A project must keep Claude traffic in the EU and use Sonnet 5.5. Which Vertex endpoint setting fits?
   - **a**: The global value, since it keeps data inside the EU by default for everyone
   - **b**: The European multi-area value, since named places serve older names only
   - **c**: A specific region such as europe-west4, since it is the strictest choice
   - **d**: The us value, since Sonnet 5.5 is served there alone in this case

2. A team asks AWS for 8 million input tokens per minute on Bedrock. What do the docs say about getting it?
   - **a**: It is within the self-service range, which reaches ten million
   - **b**: It is within the default, so nothing more is needed
   - **c**: AWS alone decides, since the quota is a Bedrock matter only
   - **d**: It exceeds the self-service ceiling, so Anthropic must approve it first

3. A compliance team wants a record of prompts and completions for Claude on Google Cloud. What do the pages advise?
   - **a**: Rely on Anthropic to hold the prompts for the customer
   - **b**: Switch on request-response logging for at least thirty days
   - **c**: Log the prompts alone, since misuse shows up in what users ask
   - **d**: Leave the platform's own defaults untouched and set nothing

<details>
<summary>Answer key</summary>

1. **b**. The page says a specific region serves `claude-sonnet-4-6` only and that the EU needs "the `eu` multi-region endpoint or a `europe-` region", so Sonnet 5.5 takes the multi-region value. *a* is ruled out because "Global endpoints: Dynamic routing for maximum availability" says nothing of keeping data in a geography. *c* is ruled out because "A specific region serves `claude-sonnet-4-6` only". *d* is ruled out because the rules say "Residency `eu` needs the `eu` multi-region endpoint or a `europe-` region".
2. **d**. The page says "You can request up to 5 million input TPM and 500,000 output TPM without additional Anthropic approval", and 8 million is above it. *b* is ruled out because "Default quota is 2 million input tokens per minute (TPM)." and 8 million is far above that. *c* is ruled out because the approval named is "without additional Anthropic approval", so Anthropic is part of the decision. *a* is ruled out because the self-service ceiling is "You can request up to 5 million input TPM and 500,000 output TPM", not ten million.
3. **b**. The page says "Enable 30-day request-response logging of your prompt and completion activity to track any model misuse by your users." *a* is ruled out because "Turning on this service does not give Google or Anthropic any access to your content." *c* is ruled out because the page asks for "prompt and completion activity to track any model misuse by your users", not prompts alone. *d* is ruled out because "Anthropic recommends that you log your activity on at least a 30-day rolling basis", so the customer keeps the log.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A reviewer finds a single star in the policy field that points at a specific resource. Which practice rule does it break?
   - **a**: The document must carry the current version string at the top
   - **b**: Each entry must be a model ARN in a documented region, without wildcards
   - **c**: Every action must be one of the three invoke actions allowed
   - **d**: The statements must be allow statements and nothing else at all

2. A reviewer sees the ready-made aiplatform user bundle attached to the calling service account. What does the practice require instead?
   - **a**: A custom role whose only permission is predict
   - **b**: The same bundle plus a deny for the deploy permission
   - **c**: A basic Editor grant scoped to one endpoint of the project
   - **d**: An organization-level predefined bundle with fewer permissions

3. A quota file asks for 6,000,000 input tokens per minute with anthropic_approval false. What does the review do?
   - **a**: It fails the request, since 5,000,000 is the unapproved ceiling
   - **b**: It passes the request, since the default is 2,000,000 already
   - **c**: It passes the request, since the output figure is under 500,000
   - **d**: It passes the request once the region has been named in it

4. A project with residency us asks for endpoint us-central1 and model claude-opus-5-5. What does the review say?
   - **a**: It passes, since the global value always satisfies any residency
   - **b**: It passes, since the region begins with the us prefix
   - **c**: It fails, since a named location serves one older name only
   - **d**: It fails, since the region is spelled in the wrong form

<details>
<summary>Answer key</summary>

1. **b**. The rule is "Every resource is a model ARN with a region from the Bedrock region list, the `anthropic.` model id form, and no wildcard." *a* is ruled out because that rule concerns the version: "The policy has the version `2012-10-17`", which a star in a resource does not change. *c* is ruled out because that rule concerns actions: "Every action is one of the three invoke actions". *d* is ruled out because that rule concerns the effect: "only `Allow` statements, and no `NotAction` or `NotResource`".
2. **a**. The rule is "The Vertex role is a custom role (its `id` starts with `projects/` or `organizations/`, not `roles/`) whose only permission is `aiplatform.endpoints.predict`." *b* is ruled out because the rule asks for a role "whose only permission is `aiplatform.endpoints.predict`", and adding a deny leaves the bundle's extra permissions in place. *c* is ruled out because the rule requires a custom role "whose only permission is `aiplatform.endpoints.predict`". *d* is ruled out because the id "starts with `projects/` or `organizations/`, not `roles/`", and a predefined id starts with `roles/`.
3. **a**. The rule is "Without Anthropic's approval a quota request is at most 5,000,000 input tokens and 500,000 output tokens per minute." *b* is ruled out because "Default quota is 2 million input tokens per minute (TPM)." is the starting quota, and a request may go higher up to the ceiling. *c* is ruled out because the ceiling covers both figures: "You can request up to 5 million input TPM and 500,000 output TPM". *d* is ruled out because only approval lifts the ceiling: "without additional Anthropic approval".
4. **c**. The rule is "A specific region serves `claude-sonnet-4-6` only", and the model asked for is newer. *b* is ruled out because the prefix rule, "`us` needs `us` or a `us-` region", is met but is not the only rule. *a* is ruled out because the global value is "Dynamic routing for maximum availability", which says nothing of residency. *d* is ruled out because the spelling is fine: "`us` needs `us` or a `us-` region".

</details>
