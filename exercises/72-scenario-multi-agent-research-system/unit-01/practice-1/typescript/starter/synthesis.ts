// The coordinator's last step in a research run: merge what the subagents returned into claims with their sources, conflicts, errors and a coverage note.
// Read statement.md for the shapes of a result and of the report, then replace the body of synthesize().
export type Finding = { claim: string; value: string; source: string; date: string };
export type Failure = { type: string; query: string; partial: Finding[]; alternatives: string[] };
export type Result = { scope: string; status: "ok" | "error"; findings: Finding[]; error: Failure | null };

export function synthesize(required: string[], results: Result[]) {
  return { status: "", covered: [] as string[], gaps: [] as string[], partial: [] as string[], claims: [] as unknown[], conflicts: [] as unknown[], errors: [] as unknown[], note: "" };
}
