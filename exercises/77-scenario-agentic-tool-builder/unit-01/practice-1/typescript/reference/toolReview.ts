// Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
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

export function review(proposal: Proposal, policy: Policy): Report {
  const { name, code, permissions: declared } = proposal;
  const findings: string[] = [];
  if (!/^[a-z][a-z0-9_]{2,63}$/.test(name)) findings.push("bad_name");
  if (proposal.description.split(/\s+/).filter(Boolean).length < policy.min_words) findings.push("short_description");
  if (proposal.timeout_s > policy.max_timeout) findings.push("timeout");
  if (proposal.memory_mb > policy.max_memory) findings.push("memory");
  const forbidden = FORBIDDEN.filter((token) => code.includes(token)).sort();
  const used = Object.entries(MARKERS).filter(([, markers]) => markers.some((m) => code.includes(m))).map(([perm]) => perm).sort();
  const refusals = forbidden.map((t) => `forbidden:${t}`);
  refusals.push(...used.filter((p) => !declared.includes(p)).map((p) => `undeclared:${p}`));
  refusals.push(...[...new Set(declared)].filter((p) => policy.denied.includes(p)).sort().map((p) => `denied:${p}`));
  const gated = declared.some((p) => policy.approval.includes(p));
  let decision: string;
  if (refusals.length > 0) decision = "refuse";
  else if (findings.length > 0) decision = "revise";
  else if (gated) decision = "approve_with_gate";
  else decision = "approve";
  return { name, decision, refusals, findings, used, audit: `${name}: ${decision}` };
}
