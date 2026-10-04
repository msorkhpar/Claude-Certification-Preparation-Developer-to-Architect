// Check the design of a CI pipeline that uses Claude: which calls wait for a person, how a pull request is reviewed, and what each run is allowed to do.
//
// The pipelines are two small JSON files, project-before and project-after, beside this example; their shape (job, audience, api, passes, session, context, tools, command) is this
// course's own description of a pipeline, not a product file. The checks are the exam's lessons for the scenario, built on the documented behaviour (checked 2026-10-04): claude -p runs
// without a person; the Message Batches API gives a discount and may take up to 24 hours with no latency guarantee, so it fits work nobody waits for; a review of many files at once is
// better split into a pass per file and one integration pass; a review that runs in the session that wrote the code is biased toward it, so a fresh session reviews. Nothing here calls
// Claude.
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

export const HERE = fileURLToPath(new URL("..", import.meta.url));
const WRITERS = ["Bash", "Edit", "Write"];

export type Job = { name: string; kind: string; audience: string; api: string; passes: string[]; session: string; context: string[]; tools: string[]; command: string };

export const load = (root: string): Job[] => JSON.parse(readFileSync(join(root, "ci/pipeline.json"), "utf8")).jobs;

/** Split a command line the way a shell does for the quoting this pipeline uses: spaces separate words, double quotes group them and a backslash escapes a quote. */
export function words(command: string): string[] {
  const out: string[] = [];
  let word = "", quoted = false, started = false;
  for (let i = 0; i < command.length; i++) {
    const c = command[i];
    if (c === "\\" && i + 1 < command.length) { word += command[++i]; started = true; }
    else if (c === '"') { quoted = !quoted; started = true; }
    else if (c === " " && !quoted) { if (started) out.push(word); word = ""; started = false; }
    else { word += c; started = true; }
  }
  if (started) out.push(word);
  return out;
}

export function audit(root: string): string[] {
  const found: string[] = [];
  for (const job of load(root)) {
    const { name, kind } = job;
    const tokens = words(job.command);
    if (tokens[0] === "claude" && !tokens.includes("-p") && !tokens.includes("--print")) found.push(`no-print-flag: ${name}`);
    if (job.audience === "waiting" && job.api === "batch") found.push(`blocking-batch: ${name}`);
    if (job.audience === "scheduled" && job.api === "realtime") found.push(`batchable: ${name}`);
    if (kind === "review" && job.passes.join(",") !== "per-file,integration") found.push(`single-pass-review: ${name}`);
    if (kind === "review" && job.session !== "fresh") found.push(`shared-session: ${name}`);
    if (kind === "review" && !job.context.includes("prior_findings")) found.push(`no-prior-findings: ${name}`);
    if (kind === "testgen" && !job.context.includes("existing_tests")) found.push(`no-existing-tests: ${name}`);
    if (kind === "review" && job.tools.some((t) => WRITERS.includes(t))) found.push(`writes: ${name}`);
  }
  return found;
}

function main() {
  for (const name of ["project-before", "project-after"]) {
    const jobs = load(join(HERE, name));
    const batch = jobs.filter((j) => j.api === "batch").length;
    console.log(`${name}: ${jobs.length} jobs (${jobs.length - batch} real-time, ${batch} batch)`);
    const found = audit(join(HERE, name));
    for (const finding of found) console.log(`  finding: ${finding}`);
    if (found.length === 0) {
      console.log("  no findings");
      for (const j of jobs) console.log(`  ${j.name}: ${j.api}, passes ${j.passes.join("+") || "none"}`);
    }
  }
}

if (import.meta.main) main();
