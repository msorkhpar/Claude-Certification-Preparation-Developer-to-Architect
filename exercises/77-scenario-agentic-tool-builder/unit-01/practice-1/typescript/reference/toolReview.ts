// Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
import { logger } from "../logger.ts";
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

/** The findings, in order: bad_name, short_description, timeout, memory. */
export function findingsOf(proposal: Proposal, policy: Policy): string[] {
  const findings: string[] = [];
  if (!/^[a-z][a-z0-9_]{2,63}$/.test(proposal.name)) findings.push("bad_name");
  if (proposal.description.split(/\s+/).filter(Boolean).length < policy.min_words) findings.push("short_description");
  if (proposal.timeout_s > policy.max_timeout) findings.push("timeout");
  if (proposal.memory_mb > policy.max_memory) findings.push("memory");
  return findings;
}

/** The forbidden tokens the code contains, in alphabetical order. */
export function forbiddenCalls(code: string): string[] {
  return FORBIDDEN.filter((token) => code.includes(token)).sort();
}

/** The permissions the code text shows, in alphabetical order. */
export function permissionsUsed(code: string): string[] {
  return Object.entries(MARKERS).filter(([, markers]) => markers.some((m) => code.includes(m))).map(([perm]) => perm).sort();
}

/** forbidden:<token>, then undeclared:<permission>, then denied:<permission>, each group in alphabetical order. */
export function refusalsOf(forbidden: string[], used: string[], declared: string[], denied: string[]): string[] {
  const refusals = forbidden.map((t) => `forbidden:${t}`);
  refusals.push(...used.filter((p) => !declared.includes(p)).map((p) => `undeclared:${p}`));
  refusals.push(...[...new Set(declared)].filter((p) => denied.includes(p)).sort().map((p) => `denied:${p}`));
  return refusals;
}

/** True when any declared permission needs a person's approval. */
export function isGated(declared: string[], approval: string[]): boolean {
  return declared.some((p) => approval.includes(p));
}

/** refuse beats revise, revise beats approve_with_gate, otherwise approve. */
export function decide(refusals: string[], findings: string[], gated: boolean): string {
  if (refusals.length > 0) return "refuse";
  if (findings.length > 0) return "revise";
  if (gated) return "approve_with_gate";
  return "approve";
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
