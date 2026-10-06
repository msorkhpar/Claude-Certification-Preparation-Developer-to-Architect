// Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
import { logger } from "./logger.ts";
const log = logger("tool_review");

export type Proposal = { name: string; description: string; permissions: string[]; timeout_s: number; memory_mb: number; code: string };
export type Policy = { min_words: number; max_timeout: number; max_memory: number; denied: string[]; approval: string[] };
export type Report = { name: string; decision: string; refusals: string[]; findings: string[]; used: string[]; audit: string };

const FORBIDDEN = ["os.system", "subprocess", "eval(", "exec(", "__import__"];
// a deliberately crude scan of the code text: which permission each marker shows (the order of this table is not the order of the report)
const MARKERS: Record<string, string[]> = {
  network: ["requests.", "urllib"],
  write_files: [".write(", "shutil."],
  read_files: ["open(", ".read("],
  run_process: ["subprocess"],
};

/**
 * TODO 1 of 6 (unlocks e1, e2 and e3): the findings about the name, the description and the limits.
 * Receives the proposal and the policy. Returns a list, in this order, of `bad_name` (the name does not match `[a-z][a-z0-9_]{2,63}`),
 * `short_description` (fewer than `min_words` words), `timeout` (`timeout_s` above `max_timeout`) and `memory` (`memory_mb` above `max_memory`).
 * Example: a 3-word description with a policy minimum of 12 -> ["short_description"]
 */
export function findingsOf(proposal: Proposal, policy: Policy): string[] {
  return [];
}

/**
 * TODO 2 of 6 (unlocks e4): the forbidden tokens in the code.
 * Receives the code text. Returns the tokens of `FORBIDDEN` that it contains, in alphabetical order.
 * Example: "eval(text)\nos.system('ls')" -> ["eval(", "os.system"]
 */
export function forbiddenCalls(code: string): string[] {
  return [];
}

/**
 * TODO 3 of 6 (unlocks e5, e6 and e8): the permissions the code shows.
 * Receives the code text. Returns the permissions of `MARKERS` that have a marker in the code, in alphabetical order (the table is not in that order).
 * Example: "shutil.copy(a, b)\nopen(a).read()" -> ["read_files", "write_files"]
 */
export function permissionsUsed(code: string): string[] {
  return [];
}

/**
 * TODO 4 of 6 (unlocks e4 and e5): the refusals, in order.
 * Receives the forbidden tokens, the permissions used, the permissions declared and the permissions the policy denies. Returns
 * `forbidden:<token>` for each forbidden token, then `undeclared:<permission>` for each used permission that was not declared, then
 * `denied:<permission>` for each declared permission that is denied (alphabetical within each group).
 * Example: refusalsOf([], ["network"], ["read_files"], ["network"]) -> ["undeclared:network"]
 */
export function refusalsOf(forbidden: string[], used: string[], declared: string[], denied: string[]): string[] {
  return [];
}

/**
 * TODO 5 of 6 (unlocks e6): does a declared permission need approval?
 * Receives the declared permissions and the permissions that need approval. Returns true when any declared permission is among them.
 * Example: isGated(["read_files", "write_files"], ["write_files"]) -> true
 */
export function isGated(declared: string[], approval: string[]): boolean {
  return false;
}

/**
 * TODO 6 of 6 (unlocks m1, e6 and e7): the decision.
 * Receives the refusals, the findings and `gated`. Returns `refuse` when there are refusals, otherwise `revise` when there are findings,
 * otherwise `approve_with_gate` when gated, otherwise `approve`.
 * Example: decide([], ["timeout"], true) -> "revise"
 */
export function decide(refusals: string[], findings: string[], gated: boolean): string {
  return "";
}

export function review(proposal: Proposal, policy: Policy): Report {
  log.debug("review input", proposal);
  const { name, code, permissions: declared } = proposal;
  const findings = findingsOf(proposal, policy);
  const used = permissionsUsed(code);
  const refusals = refusalsOf(forbiddenCalls(code), used, declared, policy.denied);
  const decision = decide(refusals, findings, isGated(declared, policy.approval));
  return { name, decision, refusals, findings, used, audit: `${name}: ${decision}` };
}
