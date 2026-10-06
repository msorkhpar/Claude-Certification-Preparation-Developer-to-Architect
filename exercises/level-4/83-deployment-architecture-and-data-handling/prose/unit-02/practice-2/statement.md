# Practice: a data policy for a Claude deployment

A regulated customer asks where its data goes. The answer is a configuration: the platform that serves the model, the region, whether zero data retention is in place, how identifiers
are handled, how tenants are separated and what the audit log keeps. In this practice you write the three checks around that configuration: the check that compares it with the
customer's requirements, the rule that decides which audit entries to purge, and the routing rule that keeps each user's data in their region. The model is not called: the tests give you
configurations as maps. It is in Python, TypeScript, Java and Kotlin;

Names are Python's (`check_deployment`, `retention_actions`, `pick_deployment`); TypeScript has `checkDeployment`, `retentionActions`, `pickDeployment`; Java has the same camel-case
names as static methods of `DataPolicy` and Kotlin has top-level functions. Configurations, requirements and results are maps and lists, as the starters show.

## The configuration and the requirements

A configuration has `platform` (`api`, `aws-platform`, `bedrock` or `vertex`), `zdr`, `hipaa_baa` (booleans), `model`, `inference_geo` (`us`, `global` or missing), `region` (for `bedrock` and
`vertex`), `tenancy` (`shared` or `workspace-per-tenant`), `pii_handling` (`none`, `tokenise` or `redact`; may be missing) and `audit` with `store_prompts` and `retain_days`. The
requirements have `residency` (`us`, `eu` or missing), `phi`, `zdr_required`, `multi_tenant` (booleans) and `audit_min_days` and `audit_max_days` (whole numbers, either may be missing). A
missing flag is false.

## What is already written, and what you write

The starter is a working data policy with six gaps cut out of it. Everything that is plumbing is written and correct: the residency findings, the tenant finding, the retention-window findings, the function that joins them and the age of an entry. Each gap is a small function with its signature, a comment that says what it receives and returns with one example, and the cases it unlocks. A gap returns a neutral value (`None`, `null` or an empty set), so the starter runs and fails every case on an assertion. Debug a gap by logging its input with the `log` line at the top of the file (the starter already logs the input of one function; add your own `log.debug` lines the same way); a run shows the lines you logged under the failing case. Write the gaps in this order (the Java and Kotlin names are the camel-case forms, TypeScript has no leading underscore):

1. `_report` unlocks `m1`: the sorted list of finding ids. Once it is written the residency (`e1`), tenant (`e4`) and retention-window (`e6`) checks that are already written start to pass; read them as worked examples.
2. `_zdr_findings` unlocks `e2`: the findings about zero data retention.
3. `_phi_findings` unlocks `e3`: the findings about protected health information.
4. `_audit_findings` unlocks `e5`: the finding about stored prompts.
5. `retention_actions` unlocks `e7`: what a retention rule purges and keeps.
6. `pick_deployment` unlocks `e8`: the deployment that may serve a user's region.

About twenty lines in all. The sections below describe the whole policy; the parts you do not write are there so you can see how your functions are used.

## What to write

`check_deployment(config, requirements)` returns the finding ids that apply, sorted and without duplicates:

- Residency `us`: on `api` and `aws-platform` the request must pin `inference_geo` to `us` (`residency-not-pinned`); on `bedrock` and `vertex` the region must start with `us-`
  (`residency-region`). Residency `eu`: `api` and `aws-platform` have no European pin (`residency-unavailable`); on `bedrock` the region must start with `eu-`, on `vertex` with `europe-` or be
  `eu` (`residency-region`).
- `zdr_required`: on `bedrock` and `vertex` the cloud provider is the data processor and zero data retention is not Anthropic's arrangement there (`zdr-not-anthropics`). On `api` and
  `aws-platform` the configuration must have `zdr` (`zdr-missing`), and a model whose id starts with `claude-fable` is refused (`model-needs-retention`), because the Fable models need 30-day retention.
- `phi`: on `api` a signed agreement is needed (`phi-no-baa`); `aws-platform` has no HIPAA readiness (`phi-platform-unsupported`); `bedrock` and `vertex` add no platform finding (their own
  compliance documentation applies). On every platform the input must be de-identified, which is `tokenise` or `redact` (`phi-not-deidentified`, also when the key is missing).
- `multi_tenant` needs `workspace-per-tenant` (`tenant-isolation`).
- An audit log that stores prompts is a finding when `phi` is true or the input is not de-identified (`audit-stores-sensitive`).
- `retain_days` above `audit_max_days` is `retention-too-long`; below `audit_min_days` is `retention-too-short`; equal is fine; a missing value is not checked.

`retention_actions(entries, max_days, today)`: entries are `{id, date, hold}` with ISO dates (`2026-01-31`), `hold` meaning a legal hold (missing is false). An entry is purged when it is
**more than** `max_days` days old at `today` and not on hold; every other entry is kept. Return `{"purge": [ids], "keep": [ids]}`, both sorted by id.

`pick_deployment(user_region, deployments)`: the user region is `us`, `eu` or `other`, a deployment is `{name, residency}` with residency `us`, `eu` or `global`. A `us` user is served by a `us`
deployment, an `eu` user by an `eu` one and an `other` user by a `global` one; of several matches the lower name wins; with no match the answer is `None` (null), never another deployment.

## Why each part is there, and what you should see

1. **Residency is pinned where the platform lets you pin it.** *You should see* an unpinned request flagged, a Europe requirement refused on the first-party API and a region checked on the clouds.
2. **Zero data retention belongs to Anthropic's own platforms and to models that allow it.** *You should see* the flag refused on a cloud provider and on a Fable model.
3. **Health data needs three things.** A covered platform, an agreement, de-identified input. *You should see* each missing one named.
4. **Tenants are separated by workspace.** *You should see* `tenant-isolation` for a shared tenancy of a multi-tenant service.
5. **An audit log is itself data.** *You should see* stored prompts flagged when they would hold identifiers.
6. **Retention has a floor and a ceiling.** *You should see* both limits accepted exactly at the limit.
7. **Purging is by age, never past a hold.** *You should see* the entry at exactly the limit kept and the held entry kept.
8. **A user's data stays in the user's region.** *You should see* `None` when the regional deployment is missing.

## The cases

| Id | What it checks |
|---|---|
| `m1` | A compliant deployment has no findings |
| `e1` | Residency is pinned by the request or by the region the platform allows |
| `e2` | Zero data retention is an arrangement of the provider's own platforms and not of every model |
| `e3` | Protected health information needs a covered platform, an agreement and de-identified input |
| `e4` | A multi-tenant service needs a workspace for each tenant |
| `e5` | An audit log must not store prompts that hold sensitive data |
| `e6` | Audit retention stays between the minimum and the maximum |
| `e7` | Purge only what is past the retention limit and not on hold |
| `e8` | A request is served only by a deployment that keeps its data in the region |
