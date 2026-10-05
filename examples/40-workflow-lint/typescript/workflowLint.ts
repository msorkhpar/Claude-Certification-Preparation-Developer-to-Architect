// Lint a GitHub Actions workflow that runs Claude Code, against what the Claude Code GitHub Actions documentation recommends.
//
// The checks are the ones the documentation states (read on 2026-10-03): keep keys in GitHub Secrets, grant only the permissions
// the job needs, use the v1 action, cap the work of each run with --max-turns, a timeout and concurrency control, start runners only on
// comments that mention @claude, and check the repository out before a skill from it can run. Reading the workflow is plain data work:
// nothing here needs a runner, a key or a network.
import { parseYaml } from "./miniyaml.ts";
import { logger } from "./logger.ts";
const log = logger("workflow_lint");

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
