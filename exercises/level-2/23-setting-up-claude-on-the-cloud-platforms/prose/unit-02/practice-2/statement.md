# Practice: platform access as configuration, graded by tests

Calling Claude through a cloud platform starts with set-up that no Messages call can fix: who may call which model,
where the traffic goes and how much of it is allowed. Here that set-up is three JSON files, and the tests are the
reviewer. **Edit the three files**; the tests read them and check every rule. The files are language-neutral: the same three files are in each
folder, and the four test suites ask the same questions. The facts come from the Anthropic pages for Amazon Bedrock and
Google Cloud, from AWS's policy documentation and from Google's IAM documentation, all read on 2026-10-02; the lesson pages
name them and explain the rules. Every name in the files is a placeholder: no real account, project or key belongs here.

## The three files

**`bedrock-policy.json`** is an IAM identity policy for the application's role. Task: let the application invoke Claude
Sonnet 5.5 in `us-east-1` and do nothing else. A statement has `Effect`, `Action` and `Resource`; the policy has a `Version`.
The invoke actions are `bedrock-mantle:CreateInference` (the action named on Anthropic's Bedrock page) and
`bedrock:InvokeModel` and `bedrock:InvokeModelWithResponseStream` (the actions in AWS's policy examples). A model resource
has the form `arn:aws:bedrock:<region>::foundation-model/<model id>`, with the Bedrock model id
`anthropic.claude-sonnet-5-5`. Confirm the resource type for your endpoint in AWS's service authorization reference before
you use a policy for real; the tests check the shape above and nothing else about the ARN.

**`vertex.json`** is the Vertex AI set-up: `project_id`, `caller` (the service account), `role` (`id` and `permissions`),
`residency` (`eu`, `us` or `none`), `endpoint` (`global`, the multi-region `us` or `eu`, or a specific region) and `model`
(the Vertex model id). Task: a service account in an EU-resident project that can call Sonnet 5.5 and can do nothing else.

**`quotas.json`** is a quota request for Bedrock: `input_tpm` and `output_tpm` (tokens per minute) and
`anthropic_approval` (true once Anthropic has approved more than the self-service ceiling).

## What is already written, and what you write

The starter holds the three files with their plumbing in place (the statement shape, the project, the caller and the platform field) and eight values cut out or left wrong. A
file is data, so there is no code and no logger to write: run the tests, read the failing case, and the message under it names the value the test found. Fix them in this order:

1. `bedrock-policy.json`: the `Action` is a wildcard where it should be one invoke action. Unlocks `m1` and `e1`.
2. `bedrock-policy.json`: the `Resource` is `*` where it should be the one model ARN. Unlocks `m1` and `e2`.
3. `bedrock-policy.json`: the `Version` is the 2008 language version. Unlocks `e3`.
4. `vertex.json`: the role `id` is a predefined role. Unlocks `e4`.
5. `vertex.json`: the role `permissions` hold a wildcard. Unlocks `e4`.
6. `vertex.json`: the `endpoint` is the global one for EU-resident data. Unlocks `e5`.
7. `vertex.json`: the `model` carries the Bedrock prefix. Unlocks `e6`.
8. `quotas.json`: `input_tpm` and `output_tpm` are over the self-service ceiling. Unlocks `e7`.

About five edited lines in all, the same in every language folder.

## The rules

- The policy has the version `2012-10-17`, only `Allow` statements, and no `NotAction` or `NotResource`.
- Every action is one of the three invoke actions; no wildcard anywhere in an action.
- Every resource is a model ARN with a region from the Bedrock region list, the `anthropic.` model id form, and no wildcard.
  For the main task the one resource is `arn:aws:bedrock:us-east-1::foundation-model/anthropic.claude-sonnet-5-5`.
- The Vertex role is a **custom** role (its `id` starts with `projects/` or `organizations/`, not `roles/`) whose only
  permission is `aiplatform.endpoints.predict`.
- Residency `eu` needs the `eu` multi-region endpoint or a `europe-` region; `us` needs `us` or a `us-` region. A specific
  region serves `claude-sonnet-4-6` only, never the newer models. The model is one of the Vertex ids
  (`claude-fable-5-1`, `claude-opus-5-5`, `claude-sonnet-5-5`, `claude-haiku-4-5@20251001`, `claude-sonnet-4-6`).
- A Vertex model id never carries the `anthropic.` prefix; the dated Haiku id is `claude-haiku-4-5@20251001`.
- Without Anthropic's approval a quota request is at most 5,000,000 input tokens and 500,000 output tokens per minute.

## The cases

| Id | What it checks |
|---|---|
| `m1` | The policy has one statement that allows an invoke action on the one model in the one region |
| `e1` | No action is a wildcard and every action is an invoke action, so nothing else is allowed |
| `e2` | Every resource is a Bedrock model ARN with a documented region and no wildcard |
| `e3` | Every statement allows, and the policy uses the current version |
| `e4` | The Vertex role is a custom role that can only predict |
| `e5` | The Vertex endpoint keeps the data where residency says and serves the model |
| `e6` | Model ids use each platform's own form: `anthropic.` on Bedrock, a Vertex id on Vertex |
| `e7` | The quota request stays under the self-service ceiling |
