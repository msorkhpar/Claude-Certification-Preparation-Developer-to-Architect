# Practice: platform access as configuration, graded by tests

Calling Claude through a cloud platform starts with set-up that no Messages call can fix: who may call which model,
where the traffic goes and how much of it is allowed. Here that set-up is three JSON files, and the tests are the
reviewer. Pick your language folder (`python`, `typescript`, `java` or `kotlin`), open `starter/` and **edit the three
files there**; the tests read them and check every rule. The files are language-neutral: the same three files are in each
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
| `m1` | The policy allows invoking one model in one region and nothing else |
| `e1` | No action is a wildcard and every action is an invoke action |
| `e2` | Every resource ARN names one documented region and one model |
| `e3` | Every statement allows, and the policy uses the current version |
| `e4` | The Vertex role is a custom role that can only predict |
| `e5` | The Vertex endpoint keeps the data where residency says and serves the model |
| `e6` | Model ids use each platform's own form |
| `e7` | The quota request stays under the self-service ceiling |

Run the tests with the command in the language folder's `run.sh` (Python, TypeScript) or its build file.
