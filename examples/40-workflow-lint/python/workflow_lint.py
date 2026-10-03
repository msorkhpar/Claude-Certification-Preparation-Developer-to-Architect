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
