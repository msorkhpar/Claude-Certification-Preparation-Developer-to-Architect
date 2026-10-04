# Where the data goes: platform, region and retention

**Level:** Architect Professional · **Module 83:** Deployment architecture and data handling · **Page 1 of 2**
**Exams:** P1, P5

**After this page you can** answer a regulated customer's question "where does our data go" as a configuration and not as a promise, say which party is the data processor on each platform, pin the region of inference where the platform allows it, tell zero data retention from HIPAA readiness and say what each needs, and separate the data of tenants by workspace.

Checked on 2026-10-04 against the Claude API documentation pages "API and data retention" and "Data residency" (inference geography and workspace geography), the exam guide's domains 1 and 5 (Claude Certified Architect, Professional, v1.0, July 2026), and the platform pages for Claude on Amazon Bedrock and Google Cloud. Nothing here called a model. Arrangements, regions and model eligibility change: the documentation and your contract are the authority, and anything on this page that has a number or a model name in it should be re-read before it is promised to a customer.

> **Exam guide and current product.** *What the guide states, and so what the exam keys:* domains 1 and 5 expect the architect to design a deployment that meets a customer's residency, retention and compliance requirements, and to separate tenants, so the exam asks what to configure, and what to refuse to promise, for a regulated customer. *What the current documentation says (checked 2026-10-04):* on the Claude API, Claude Platform on AWS and Claude in Microsoft Foundry, Anthropic is the data processor; on Amazon Bedrock and Google Cloud's platform, the cloud provider is. Zero data retention is an arrangement with Anthropic, enabled per organisation. HIPAA readiness applies to the Claude API and is not available on Claude Platform on AWS or Microsoft Foundry. Some models, Claude Fable 5.1, Claude Mythos 5.1, Claude Fable 5 and Claude Mythos 5, require 30-day retention and are not available under zero data retention unless Anthropic expressly authorises it. *How to read both:* the exam keys the answer that reads the requirement into a configuration and checks it against the platform, and rejects the answer that assumes one arrangement covers every platform and model.

## Why it matters

A hospital group asks three questions before it signs: where is our data processed, is any of it kept, and who is responsible for it. A sales answer ("it is secure and compliant") ends the meeting and starts a dispute. An architect answers each question with a setting: the platform and its region, the retention arrangement and the model that allows it, the tenant boundary, the agreement that makes health data permissible. Each setting can be checked against the requirement before anything is built. That check is code in the practice of this module.

## The idea

### Who is the data processor

The first fact to establish is which party processes the data, because the obligations follow it.

| Platform | Data processor | Where retention and compliance are documented |
|---|---|---|
| Claude API | Anthropic | Anthropic's data retention page |
| Claude Platform on AWS | Anthropic | The same policy as the Claude API |
| Claude in Microsoft Foundry | Anthropic | The same page |
| Amazon Bedrock | The cloud provider | The provider's own documentation |
| Google Cloud's Claude platform | The cloud provider | The provider's own documentation |

The cloud platforms are not "the same service somewhere else". Their retention, compliance and region rules are the provider's, and an architect who has promised an Anthropic arrangement on a cloud-provider platform has promised something no one can deliver.

### Region: pin it where the platform lets you

Residency is a requirement of the customer, and the way to meet it depends on the platform. On the Claude API the request carries an inference geography, `us` or `global`, so a customer that needs United States processing sets `inference_geo` to `us` (at the time of checking, a `us` pin cost 1.1 times the standard price). The workspace geography is set per workspace and is `us` only. A customer that needs Europe cannot be served by pinning on that API: there is no European pin, so the finding is "unavailable" and the design is a different platform. On the cloud platforms the region is part of the deployment: a model served from a `us-` region stays in the United States and one from an `eu-` region stays in Europe, and the check is on the region string.

Two rules follow. A requirement that cannot be met on a platform is reported, never worked around by promising it. And for users in different regions, the router sends a user's data only to a deployment that keeps it in that region, and when none exists the answer is none, not "the nearest one": serving a European user from a United States deployment because it was available is the failure the router exists to prevent.

### Zero data retention and HIPAA readiness are different things

| | Zero data retention | HIPAA readiness |
|---|---|---|
| **What it is** | Anthropic does not store prompts or responses at rest after the response returns | A signed agreement and an organisation setting for processing protected health information |
| **Where it applies** | Claude API and Claude Platform on AWS, by arrangement | The Claude API only, not Claude Platform on AWS or Microsoft Foundry |
| **How you get it** | Request it from Anthropic; it is enabled per organisation, so each new organisation needs its own | The business associate agreement signed and the setting enabled; it cannot be disabled afterwards |
| **What it does not cover** | Models that need 30-day retention, the Console, consumer products, third-party tools | Cloud-provider platforms, Claude Code, beta features unless listed as eligible |
| **What the architect adds** | A check that the chosen model allows it | De-identified input, and no health data in a JSON schema definition |

Three consequences. On Amazon Bedrock or Google Cloud, zero data retention is not Anthropic's arrangement to give, so a requirement for it there is a finding. A model that requires 30-day retention cannot satisfy a zero-retention requirement, so the model is part of the check and not only the platform. And the schemas used for structured output are compiled into grammars that are cached separately from message content, so they do not get the protections of the prompt: put no health data in a schema. Neither arrangement removes the need to de-identify (page 2).

### Tenants are separated by workspace

A service that serves several customers (tenants) must keep their data apart. On the Claude API the unit of separation is the workspace: one workspace per tenant, with its own keys, limits and spend, so that a key, a log or a limit for one tenant cannot reach another. A single shared workspace for all tenants is a finding for a multi-tenant requirement, because the separation then rests on application code alone. This is the tenant boundary of module 86 seen from the deployment side.

### Reading a requirement into a check

The practice of this module makes the reading mechanical. A configuration lists the platform, the model, the geography or region, the retention arrangement, the agreement, the tenancy, the way identifiers are handled and what the audit log keeps. A set of requirements lists residency, health data, zero retention, multi-tenancy and the audit retention window. The check returns the findings that apply, each with an identifier, so that the answer to the customer is a list and an empty list is the only compliant one.

## Traps

These are the wrong answers the exam's options for this domain offer, each with the reason it is rejected.

1. **"Enable zero data retention and the deployment is compliant on every platform."** It is tempting because one arrangement sounds like the whole answer. The exam rejects it: the arrangement is Anthropic's and does not exist on a cloud-provider platform, it excludes models that need 30-day retention, and health data needs a signed agreement and de-identified input besides.
2. **"Serve European users from the nearest deployment, which is in the United States."** It is tempting because latency is lower. The exam rejects it: residency is a requirement of the customer, so a user's data goes only to a deployment that keeps it in the user's region, and none available means none served.
3. **"Share one workspace among all customers and separate their data in application code."** It is tempting because it is less to set up. The exam rejects it: the workspace is the separation the platform enforces, one per tenant, and code-only separation fails the first time a filter is forgotten.

## Quiz

1. Scenario: Linden Clinics asks a vendor to serve its patients from Europe and offers the Claude API with `inference_geo` set to `us` as the compliant option. Which assessment is right?
   - **a**: It meets the need, as long as the audit log itself is kept in a data store located in Europe
   - **b**: It fails the need, because the pin sends the processing to the United States
   - **c**: It meets the need, since the geography of the workspace can still be set to Europe separately
   - **d**: It fails the need only if the clinics have not yet signed the business associate agreement in full

2. Scenario: Harrow Insurance runs Claude on Amazon Bedrock and writes in its design that "zero data retention is enabled with Anthropic". What is the problem?
   - **a**: The statement is correct, since every platform shares one retention arrangement with the model maker
   - **b**: The statement holds only once a second approval has been given by the cloud provider's own sales team
   - **c**: The arrangement belongs to the model maker, and on that platform the cloud provider does the processing
   - **d**: The statement is correct, but it must be repeated for each separate model that the company uses in production

<details>
<summary>Answer key</summary>

1. **b**. The `us` pin is a United States geography, and the API has no European one. *a* is ruled out because the audit log is separate from where inference runs: "A requirement that cannot be met on a platform is reported". *c* is ruled out because "The workspace geography is set per workspace" and the page says it is `us` only. *d* is ruled out because the agreement concerns health data and does not move the processing: "there is no European pin".
2. **c**. Zero data retention is an arrangement with Anthropic, and on Bedrock the cloud provider is the processor. *a* is ruled out because "Their retention, compliance and region rules are the provider's", so no arrangement is shared. *b* is ruled out because the page gives no such approval, and says the provider's rules are "the provider's own documentation". *d* is ruled out because the model is a separate rule ("A model that requires 30-day retention cannot satisfy a zero-retention requirement") and does not change who the processor is.

</details>

Adapted from CLAUDE-CERTIFICATIONS by Amey Thakur (MIT License).
