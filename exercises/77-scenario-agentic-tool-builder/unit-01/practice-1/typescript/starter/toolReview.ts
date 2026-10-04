// Review a tool that an agent proposes: its name and description, the limits it asks for, what its code does and which permissions it declares, and the decision.
// Read statement.md for the fields of a proposal, of the policy and of the report, then replace the body of review().
export type Proposal = { name: string; description: string; permissions: string[]; timeout_s: number; memory_mb: number; code: string };
export type Policy = { min_words: number; max_timeout: number; max_memory: number; denied: string[]; approval: string[] };
export type Report = { name: string; decision: string; refusals: string[]; findings: string[]; used: string[]; audit: string };

export function review(proposal: Proposal, policy: Policy): Report {
  return { name: "", decision: "", refusals: [], findings: [], used: [], audit: "" };
}
