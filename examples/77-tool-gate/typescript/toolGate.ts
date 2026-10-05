// An approval gate for tools that an agent proposes, in miniature: the proposal is decided from the permissions it asks for, a reviewer answers the ones that need a person,
// an approved tool runs and its result is checked against the schema it declared, and every step leaves a line in an audit log.
//
// The tools are made-up proposals with made-up results: nothing generated is executed here, because this example is about the decisions around a run, not about running code.
// The shapes (a decision, a reviewer's answer, a result check, an audit line) are this course's design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("tool_gate");
const DENIED = ["network", "run_process"];
const NEEDS_APPROVAL = ["write_files"];
const SCHEMA: Record<string, string> = { headline: "string", rows: "number" };
const MAX_CHARS = 200;

export type Proposal = { name: string; permissions: string[]; result: Record<string, unknown> };

export const PROPOSALS: Proposal[] = [
  { name: "read_report", permissions: ["read_files"], result: { headline: "Q3 up 4%", rows: 12 } },
  { name: "write_summary", permissions: ["read_files", "write_files"], result: { headline: "Q3 up 4%" } },
  { name: "fetch_prices", permissions: ["network"], result: { headline: "x", rows: 1 } },
  { name: "tidy_up", permissions: ["write_files"], result: { headline: "x", rows: 1 } },
];
const REVIEWER: Record<string, boolean> = { write_summary: true, tidy_up: false }; // the person's answers, by tool name

/** Refused when a denied permission is asked for, held for a person when a permission needs one, otherwise automatic. */
export function decide(permissions: string[]): [string, string[]] {
  log.debug("decide input", permissions);
  const denied = permissions.filter((p) => DENIED.includes(p));
  if (denied.length > 0) return ["refused", denied];
  const gated = permissions.filter((p) => NEEDS_APPROVAL.includes(p));
  if (gated.length > 0) return ["needs_approval", gated];
  return ["auto", []];
}

/** The result of a tool is data to check before the agent uses it: every declared field, of its declared type, and no more than the limit of characters of text. */
export function checkOutput(result: Record<string, unknown>): string[] {
  const problems = Object.keys(SCHEMA).filter((f) => !(f in result)).map((f) => `missing: ${f}`);
  for (const [field, kind] of Object.entries(SCHEMA)) if (field in result && typeof result[field] !== kind) problems.push(`type: ${field}`);
  if (Object.values(result).reduce((n: number, v) => n + (typeof v === "string" ? v.length : 0), 0) > MAX_CHARS) problems.push("too large");
  return problems;
}

function main() {
  const log: [string, string][] = [];
  for (const proposal of PROPOSALS) {
    const name = proposal.name;
    const [decision, why] = decide(proposal.permissions);
    if (decision === "refused") {
      console.log(`${name}: refused (${why.join(", ")})`);
      log.push([name, "refused"]);
      continue;
    }
    if (decision === "needs_approval") {
      const answer = REVIEWER[name];
      console.log(`${name}: needs_approval -> ${answer ? "approved" : "declined"}`);
      if (!answer) {
        log.push([name, "declined"]);
        continue;
      }
    } else {
      console.log(`${name}: auto`);
    }
    const problems = checkOutput(proposal.result);
    log.push([name, problems.length === 0 ? "ran" : "rejected"]);
    if (problems.length > 0) console.log(`  result of ${name} rejected (${problems.join("; ")})`);
  }
  const count = (s: string) => log.filter(([, status]) => status === s).length;
  console.log(`audit: ${PROPOSALS.length} proposals, ${count("ran")} ran, ${count("rejected")} rejected, ${count("refused")} refused, ${count("declined")} declined`);
}

if (import.meta.main) main();
