# CI, code review and workflows triggered by repository events

**Level:** Developer · **Module 40:** Claude in the software life cycle · **Page 2 of 2**
**Exams:** DV1

**After this page you can** set up a GitHub Actions workflow that answers `@claude` mentions, write one that reviews pull requests, bound both with turns, time and concurrency, tell the action, Code Review and routines apart, tune a review with `REVIEW.md`, and finish the module's practice.

Checked on 2026-10-03 against the Claude Code documentation (GitHub Actions, Code Review and routines), which mention behaviour up to Claude Code v2.1.286. The action is `anthropics/claude-code-action@v1`. Nothing was run against GitHub: the course has no key and no network. The example reads workflow files offline, in Python and TypeScript, and applies a linter that is the course's own model of the documented advice.

## Why it matters

CI is where Claude runs with no one watching. That makes its configuration a security and cost decision: who may start it, what it may touch, how long it may run and how many runs may overlap. The exam asks which settings make a workflow safe, and which of the three automation products fits a job.

## The idea

### Three ways to automate

The GitHub Actions page opens by separating products that share a name.

| Product | What it is | You write |
|---|---|---|
| The GitHub Action (`claude-code-action`) | Claude Code "inside your repository's workflows" | Workflow files in the repository |
| Code Review | "automatic review on every pull request, without writing a workflow" | Settings, and optionally `REVIEW.md` |
| Routines | A saved configuration (prompt, repositories, connectors) run on a schedule, an API call or a GitHub event, on Anthropic's cloud | A routine, at claude.ai or with `/schedule` |

The Action is the one you control completely: mention `@claude` "in a pull request or issue comment to have Claude analyze code, implement changes, and push commits", or give it a prompt "to run automatically on any GitHub event". It is built on the Agent SDK from module 35, and routines are in research preview. Code Review runs a fleet of agents on Anthropic infrastructure, and its findings "don't approve or block your PR".

### Setting up the Action

Quick setup is `/install-github-app` from Claude Code, which installs the GitHub App, stores the secret and prepares the workflow pull request. Manual setup is three steps: install the Claude GitHub App, add a secret, and copy the workflow file. For either path you need admin access to the repository. The secret is `ANTHROPIC_API_KEY`, or `CLAUDE_CODE_OAUTH_TOKEN` for a subscription token. In the workflow you pass it as `anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}`. The documentation is blunt about the alternative: "Never commit API keys or OAuth tokens directly to your repository." To avoid a long-lived secret altogether, authenticate through workload identity federation, which needs `id-token: write`.

A mention workflow has this shape: it triggers on `issue_comment` and `pull_request_review_comment` with `types: [created]`, its job starts only when the comment mentions the bot (`if: contains(github.event.comment.body, '@claude')`), it checks the repository out, and it runs the `anthropics/claude-code-action@v1` step. Four details are exam material.

- **The guard on the job.** Without the `if`, a runner starts for every comment in the repository, and each start costs minutes.
- **Permissions are granted per job.** A job that implements changes needs `contents: write`, `pull-requests: write` and `issues: write`. A job that only reviews needs `contents: read`. Grant the least a job needs. The documentation's own review example also reads pull requests and issues, and it includes `id-token: write`, which the documentation calls "required for the Claude Code GitHub Action's default GitHub App authentication".
- **Checkout comes first.** A skill or a `CLAUDE.md` that lives in the repository is not there until `actions/checkout` has run.
- **Pin to the stable action.** `@v1` is the maintained version, and workflows written for `@beta` need to move.

### Bounding every run

The documentation's cost section lists the controls: "Set `--max-turns` in `claude_args` to limit iterations", "Set workflow-level timeouts to avoid runaway jobs", and "Use GitHub's concurrency controls to limit parallel runs". Together they decide the worst case of one workflow: at most N turns, at most M minutes, and at most one run per issue or pull request. Keep `CLAUDE.md` concise, "since Claude reads it on every run". As the course's advice, and not a documented value, for a review set `cancel-in-progress: true` on the concurrency group so a new push replaces the review of an outdated commit. For an implementation run, leave it `false` so that a second comment does not kill work in progress.

A pitfall with tokens: "GitHub doesn't trigger workflows on commits made with the default `GITHUB_TOKEN`." If the action must push commits that start other workflows, it needs the Claude GitHub App token or a custom app token, not `github_token: ${{ secrets.GITHUB_TOKEN }}`.

### A review workflow

A review workflow triggers on `pull_request` with `opened`, `synchronize`, `ready_for_review` and `reopened`. It needs only read access to the code. Its `prompt` runs a review skill, and `claude_args` allows the one tool that posts inline comments, `mcp__github_inline_comment__create_inline_comment`, while the `--comment` argument makes the findings appear on the pull request and not only in the run log. Concurrency uses the pull request number as its group.

### Code Review, and tuning it

Code Review needs no workflow. When a review runs, "multiple agents analyze the diff and surrounding code in parallel", and a verification step "checks candidates against actual code behavior to filter out false positives". Findings are tagged by severity, such as the nit marker for a minor issue, and posted as inline comments. Each review also fills the Claude Code Review check run, and "The check run always completes with a neutral conclusion so it never blocks merging through branch protection rules." So it advises, and a person merges.

Two files in the repository tune it. `CLAUDE.md` is "shared project instructions that Claude Code uses for all tasks, not just reviews", and the review treats a newly introduced violation as a nit. `REVIEW.md` holds "review-only instructions", and you use it "to say what your team wants flagged, at what severity, and how findings are reported". The reference practice's REVIEW.md has two sections: what to always check, such as money in integer cents and a test for each endpoint, and what to skip, such as formatting the linter owns and generated files. Name paths in backticks, because a path is something the reviewer can match.

### Routines and other events

A routine "is a saved Claude Code configuration: a prompt, one or more repositories, and a set of connectors, packaged once and run automatically." Triggers are scheduled (hourly, nightly, weekly, or once), API (an HTTP POST to a per-routine endpoint with a bearer token) and GitHub (repository events such as pull requests or releases). A single routine can combine them: a review routine that runs nightly, from a deploy script and on every new pull request. Choose a routine when the work should run without your repository's runners and keep working while your laptop is closed, and the Action when the workflow must live in the repository and follow its permissions and secrets.

A webhook is the same idea from the outside. Something posts an event, and a handler starts a run. Wherever the handler lives, the questions are the ones above: who may trigger it, with what permissions, bounded by what, and with which secret.

<!-- example: m40-workflow-lint tabs: python,typescript -->
```python
"""Lint a GitHub Actions workflow that runs Claude Code, against what the Claude Code GitHub Actions documentation recommends.

The checks are the ones the documentation states (read on 2026-10-03): keep keys in GitHub Secrets, grant only the permissions
the job needs, use the v1 action, cap the work of each run with --max-turns, a timeout and concurrency control, start runners only on
comments that mention @claude, and check the repository out before a skill from it can run. Reading the workflow is plain data work:
nothing here needs a runner, a key or a network.
"""
import re

from miniyaml import parse_yaml

CLAUDE_ACTION = "anthropics/claude-code-action"


def claude_steps(job):
    return [s for s in job.get("steps", []) if str(s.get("uses", "")).startswith(CLAUDE_ACTION)]


def lint(text):
    """Findings as (job or '-', rule id, message)."""
    wf = parse_yaml(text)
    findings = []
    if re.search(r"sk-ant-[A-Za-z0-9_-]{8,}", text):
        findings.append(("-", "literal-key", "an API key is written into the file: use ${{ secrets.NAME }}"))
    if "concurrency" not in wf:
        findings.append(("-", "no-concurrency", "no concurrency group: parallel runs are not limited"))
    triggers = wf.get("on")
    names = list(triggers) if isinstance(triggers, dict) else triggers if isinstance(triggers, list) else [triggers]
    for name, job in wf.get("jobs", {}).items():
        steps = claude_steps(job)
        if not steps:
            continue
        for step in steps:
            if not str(step["uses"]).endswith("@v1"):
                findings.append((name, "action-version", f"{step['uses']} is not the v1 action: move @beta workflows to @v1"))
            with_ = step.get("with") or {}
            for field in ("anthropic_api_key", "claude_code_oauth_token"):
                if field in with_ and not str(with_[field]).startswith("${{ secrets."):
                    findings.append((name, "literal-key", f"{field} is not read from the secrets context"))
            if "--max-turns" not in str(with_.get("claude_args", "")):
                findings.append((name, "no-max-turns", "claude_args has no --max-turns: a run has no turn limit"))
            prompt = str(with_.get("prompt", ""))
            if prompt.startswith("/"):
                before = job["steps"][: job["steps"].index(step)]
                if not any(str(s.get("uses", "")).startswith("actions/checkout") for s in before):
                    findings.append((name, "no-checkout", "a skill from the repository needs actions/checkout before the Claude step"))
        perms = job.get("permissions")
        if perms is None:
            findings.append((name, "no-permissions", "no permissions block: grant only what the job needs"))
        elif "review" in name.lower() and perms.get("contents") == "write":
            findings.append((name, "review-writes", "a review job has contents: write; reading is enough to review"))
        if "timeout-minutes" not in job:
            findings.append((name, "no-timeout", "no timeout-minutes: a stuck run keeps the runner"))
        if "issue_comment" in names and "@claude" not in str(job.get("if", "")):
            findings.append((name, "unguarded-trigger", "the job has no if: contains(..., '@claude'): a runner starts on every comment"))
    return findings


BAD = """name: Claude
on:
  issue_comment:
    types: [created]
jobs:
  review:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: anthropics/claude-code-action@beta
        with:
          anthropic_api_key: sk-ant-api03-EXAMPLEEXAMPLE
          prompt: "/code-review"
"""

GOOD = """name: Claude review
on:
  pull_request:
    types: [opened, synchronize]
concurrency:
  group: claude-${{ github.event.pull_request.number }}
  cancel-in-progress: true
jobs:
  review:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    permissions:
      contents: read
      pull-requests: write
      id-token: write
    steps:
      - uses: actions/checkout@v6
        with:
          fetch-depth: 1
      - uses: anthropics/claude-code-action@v1
        with:
          anthropic_api_key: ${{ secrets.ANTHROPIC_API_KEY }}
          prompt: "/code-review"
          claude_args: --max-turns 8
"""


def main():
    for label, text in (("bad workflow", BAD), ("good workflow", GOOD)):
        found = lint(text)
        print(f"{label}: {len(found)} finding(s)")
        for job, rule, message in found:
            print(f"  [{rule}] {job}: {message}")


if __name__ == "__main__":
    main()
```
```text
bad workflow: 9 finding(s)
  [literal-key] -: an API key is written into the file: use ${{ secrets.NAME }}
  [no-concurrency] -: no concurrency group: parallel runs are not limited
  [action-version] review: anthropics/claude-code-action@beta is not the v1 action: move @beta workflows to @v1
  [literal-key] review: anthropic_api_key is not read from the secrets context
  [no-max-turns] review: claude_args has no --max-turns: a run has no turn limit
  [no-checkout] review: a skill from the repository needs actions/checkout before the Claude step
  [review-writes] review: a review job has contents: write; reading is enough to review
  [no-timeout] review: no timeout-minutes: a stuck run keeps the runner
  [unguarded-trigger] review: the job has no if: contains(..., '@claude'): a runner starts on every comment
good workflow: 0 finding(s)
```
```typescript
// Lint a GitHub Actions workflow that runs Claude Code, against what the Claude Code GitHub Actions documentation recommends.
//
// The checks are the ones the documentation states (read on 2026-10-03): keep keys in GitHub Secrets, grant only the permissions
// the job needs, use the v1 action, cap the work of each run with --max-turns, a timeout and concurrency control, start runners only on
// comments that mention @claude, and check the repository out before a skill from it can run. Reading the workflow is plain data work:
// nothing here needs a runner, a key or a network.
import { parseYaml } from "./miniyaml.ts";

const CLAUDE_ACTION = "anthropics/claude-code-action";
type Finding = [string, string, string];

const claudeSteps = (job: any): any[] => (job.steps ?? []).filter((s: any) => String(s.uses ?? "").startsWith(CLAUDE_ACTION));

/** Findings as [job or '-', rule id, message]. */
export function lint(text: string): Finding[] {
  const wf = parseYaml(text) as any;
  const findings: Finding[] = [];
  if (/sk-ant-[A-Za-z0-9_-]{8,}/.test(text)) findings.push(["-", "literal-key", "an API key is written into the file: use ${{ secrets.NAME }}"]);
  if (!("concurrency" in wf)) findings.push(["-", "no-concurrency", "no concurrency group: parallel runs are not limited"]);
  const triggers = wf.on;
  const names: any[] = Array.isArray(triggers) ? triggers : triggers && typeof triggers === "object" ? Object.keys(triggers) : [triggers];
  for (const [name, job] of Object.entries<any>(wf.jobs ?? {})) {
    const steps = claudeSteps(job);
    if (!steps.length) continue;
    for (const step of steps) {
      if (!String(step.uses).endsWith("@v1")) findings.push([name, "action-version", `${step.uses} is not the v1 action: move @beta workflows to @v1`]);
      const w = step.with ?? {};
      for (const field of ["anthropic_api_key", "claude_code_oauth_token"]) {
        if (field in w && !String(w[field]).startsWith("${{ secrets.")) findings.push([name, "literal-key", `${field} is not read from the secrets context`]);
      }
      if (!String(w.claude_args ?? "").includes("--max-turns")) findings.push([name, "no-max-turns", "claude_args has no --max-turns: a run has no turn limit"]);
      if (String(w.prompt ?? "").startsWith("/")) {
        const before = job.steps.slice(0, job.steps.indexOf(step));
        if (!before.some((s: any) => String(s.uses ?? "").startsWith("actions/checkout"))) findings.push([name, "no-checkout", "a skill from the repository needs actions/checkout before the Claude step"]);
      }
    }
    const perms = job.permissions;
    if (perms == null) findings.push([name, "no-permissions", "no permissions block: grant only what the job needs"]);
    else if (name.toLowerCase().includes("review") && perms.contents === "write") findings.push([name, "review-writes", "a review job has contents: write; reading is enough to review"]);
    if (!("timeout-minutes" in job)) findings.push([name, "no-timeout", "no timeout-minutes: a stuck run keeps the runner"]);
    if (names.includes("issue_comment") && !String(job.if ?? "").includes("@claude")) findings.push([name, "unguarded-trigger", "the job has no if: contains(..., '@claude'): a runner starts on every comment"]);
  }
  return findings;
}

export const BAD = `name: Claude
on:
  issue_comment:
    types: [created]
jobs:
  review:
    runs-on: ubuntu-latest
    permissions:
      contents: write
    steps:
      - uses: anthropics/claude-code-action@beta
        with:
          anthropic_api_key: sk-ant-api03-EXAMPLEEXAMPLE
          prompt: "/code-review"
`;

export const GOOD = `name: Claude review
on:
  pull_request:
    types: [opened, synchronize]
concurrency:
  group: claude-\${{ github.event.pull_request.number }}
  cancel-in-progress: true
jobs:
  review:
    runs-on: ubuntu-latest
    timeout-minutes: 15
    permissions:
      contents: read
      pull-requests: write
      id-token: write
    steps:
      - uses: actions/checkout@v6
        with:
          fetch-depth: 1
      - uses: anthropics/claude-code-action@v1
        with:
          anthropic_api_key: \${{ secrets.ANTHROPIC_API_KEY }}
          prompt: "/code-review"
          claude_args: --max-turns 8
`;

function main() {
  for (const [label, text] of [["bad workflow", BAD], ["good workflow", GOOD]]) {
    const found = lint(text);
    console.log(`${label}: ${found.length} finding(s)`);
    for (const [job, rule, message] of found) console.log(`  [${rule}] ${job}: ${message}`);
  }
}

if (import.meta.main) main();
```
```text
bad workflow: 9 finding(s)
  [literal-key] -: an API key is written into the file: use ${{ secrets.NAME }}
  [no-concurrency] -: no concurrency group: parallel runs are not limited
  [action-version] review: anthropics/claude-code-action@beta is not the v1 action: move @beta workflows to @v1
  [literal-key] review: anthropic_api_key is not read from the secrets context
  [no-max-turns] review: claude_args has no --max-turns: a run has no turn limit
  [no-checkout] review: a skill from the repository needs actions/checkout before the Claude step
  [review-writes] review: a review job has contents: write; reading is enough to review
  [no-timeout] review: no timeout-minutes: a stuck run keeps the runner
  [unguarded-trigger] review: the job has no if: contains(..., '@claude'): a runner starts on every comment
good workflow: 0 finding(s)
```
<!-- /example -->

The example reads a workflow as data and applies nine rules from this page. The first workflow breaks all of them and gets nine findings: a literal key, no concurrency group, the `@beta` action, a key that is not read from the secrets context, no `--max-turns`, no checkout, `contents: write` on a review job, no timeout, and no `@claude` guard. The second workflow follows every rule and gets none. Both languages print the same text.

### The practice

The practice puts Claude into a small repository. You fix a mention workflow that holds a key, uses `@beta` and has no limits, write a review workflow, version a prompt with a changelog, write `REVIEW.md` and `CLAUDE.md`, and keep every file free of keys and addresses. Tests parse your YAML and Markdown and apply the course's linter. The files are language-neutral, so the module has Python and TypeScript test suites and no Java or Kotlin edition: no YAML library is available offline for those two here.

## Traps

1. **A mention workflow with no `if` guard.** A runner starts on every comment, so the cost follows the comment volume and not the number of requests to Claude.
2. **Giving a review job write access.** Reading is enough to review, and a compromised review run with write access can change the repository.
3. **Running without turn, time and concurrency limits.** The worst case of a loop or a burst of comments is then unbounded.
4. **Expecting Code Review to block a merge.** Its check run is neutral, and its findings advise.

## Quiz

1. A workflow starts a runner whenever anyone posts anything, and the bill follows the volume. Which line fixes it?
   - **a**: A longer `timeout-minutes` value on the job that runs the step
   - **b**: An `if` on the job that tests the comment body for `@claude`
   - **c**: A larger `--max-turns` value in the arguments of the action step
   - **d**: A write permission on the contents of the repository for the job

2. A review job should inspect the code and post findings. Which permission set fits?
   - **a**: Write access to every scope, so the job never fails for a missing grant
   - **b**: Write access to the contents, so that findings can be committed
   - **c**: No permissions at all, because comments need no access to the code
   - **d**: Read access to the contents, plus the token needed for authentication

3. A team wants Claude's review to stop a merge until findings are fixed. What does Code Review do?
   - **a**: It fails the check run whenever a finding is marked important by the review
   - **b**: It completes its check run as neutral, so branch protection never blocks on it
   - **c**: It approves the pull request when no finding is marked important at all
   - **d**: It requests changes from the author on every single finding it posts

<details>
<summary>Answer key</summary>

1. **b**. The page says "Without the `if`, a runner starts for every comment in the repository", and the guard is `if: contains(github.event.comment.body, '@claude')`. *a* is ruled out because the timeout bounds one run and does not stop runs from starting: the page lists it with "workflow-level timeouts to avoid runaway jobs". *c* is ruled out because "Set `--max-turns` in `claude_args` to limit iterations", which acts inside a run and does not stop one from starting. *d* is ruled out because write access is a permission and not a trigger filter, and the page says "Grant the least a job needs".
2. **d**. The page says "A job that only reviews needs `contents: read`", and the review example reads pull requests and issues as well. *b* is ruled out because the page says "Reading is enough to review", and write access lets a compromised run change the repository. *c* is ruled out because "A job that only reviews needs `contents: read`", so some access to the code is required. *a* is ruled out because the page says "Grant the least a job needs", and a grant of every scope is the opposite.
3. **b**. The page says "The check run always completes with a neutral conclusion so it never blocks merging through branch protection rules." *a* is ruled out because the same sentence says the check run "always completes with a neutral conclusion", so it never fails. *c* is ruled out because findings "don't approve or block" a pull request, and so an empty review is no approval. *d* is ruled out because findings are posted "as inline comments", and Code Review "so it advises, and a person merges".

</details>

## Module quiz

This quiz covers both pages of the module.

1. A team wants its own rules about money handling and generated files applied only when pull requests are checked, and not in ordinary sessions. Which file holds them?
   - **a**: `CLAUDE.md`, which every session reads before its first prompt
   - **b**: `REVIEW.md`, read by the agents that examine the diff
   - **c**: `.claude/settings.json`, which the whole team commits to the repository
   - **d**: `prompts/CHANGELOG.md`, which lists the prompt versions and their dates

2. A prompt file is at version 1.2.0, and its changelog's newest heading says 1.1.0. What does a reviewer conclude?
   - **a**: The edit is unrecorded, because the record and the number disagree
   - **b**: The prompt is older than its record, so it should be reverted at once
   - **c**: The number is wrong, because the record always wins out in such a case
   - **d**: Nothing, because the changelog is only for people to read

3. A job must start only for a new note that names the bot, and a second note on the same issue must not stop work that is running. Which pair of settings fits?
   - **a**: A guard on the comment body, with `cancel-in-progress` set to false
   - **b**: No guard on the comment body, with `cancel-in-progress` set to true
   - **c**: A guard on the comment body, with `cancel-in-progress` set to true
   - **d**: No guard on the comment body, with `cancel-in-progress` set to false

4. A team must choose how to run a nightly dependency review that keeps working when nobody's laptop is on, with no workflow in the repository. Which option fits?
   - **a**: A hook in the settings file that fires after each edit
   - **b**: A mention workflow that waits for someone to comment
   - **c**: A routine with a scheduled trigger on Anthropic's cloud
   - **d**: A subagent in the project folder that starts at midnight

<details>
<summary>Answer key</summary>

1. **b**. The page says `REVIEW.md` holds "review-only instructions", and that you use it "to say what your team wants flagged, at what severity, and how findings are reported". *a* is ruled out because `CLAUDE.md` is "shared project instructions that Claude Code uses for all tasks, not just reviews". *c* is ruled out because the first page gives that file "Team permission rules, default model, hooks, plugins", which are settings and not review instructions. *d* is ruled out because the changelog explains versions of prompts, and the first page says it must have "A changelog entry for every change" and not review rules.
2. **a**. The first page says the changelog's "latest heading equals the file's version", so a mismatch means a change nobody recorded. *b* is ruled out because the rule is only a heading that is "whose latest heading equals the file's version", and it names no age comparison. *c* is ruled out because the only rule is "whose latest heading equals the file's version", with no winner, so a reviewer asks for the missing entry. *d* is ruled out because the page says "A reviewer reading the pull request sees what changed and why", which makes it part of the review.
3. **a**. The page says to guard the job with `if: contains(github.event.comment.body, '@claude')`, and for an implementation run to "leave it `false` so that a second comment does not kill work in progress." *b* is ruled out because "Without the `if`, a runner starts for every comment in the repository". *c* is ruled out because `cancel-in-progress: true` suits a review: "so a new push replaces the review of an outdated commit". *d* is ruled out because it lacks the guard, and "Without the `if`, a runner starts for every comment in the repository".
4. **c**. The page says a routine is "a saved Claude Code configuration", with a scheduled trigger, and it runs on cloud infrastructure so it keeps working "while your laptop is closed". *b* is ruled out because "its job starts only when the comment mentions the bot", and the job here is nightly. *a* is ruled out because the page lists "Triggers are scheduled (hourly, nightly, weekly, or once)" for routines, and a hook has no schedule and no open session. *d* is ruled out because "Triggers are scheduled (hourly, nightly, weekly, or once)" belongs to a routine, and a subagent has no clock.

</details>
