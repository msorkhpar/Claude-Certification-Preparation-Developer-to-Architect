/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */

export const ALTERNATIVES: Record<string, string[]> = {
  timeout: ["retry later", "try a narrower query"],
  unavailable: ["use a cached source", "try another provider"],
  permission: ["request access", "use a public source"],
  invalid_query: ["rewrite the query"],
};
export const TRANSIENT = ["timeout", "unavailable"];

export function searchWithRecovery(query: string, call: (query: string, attempt: number) => any, maxAttempts = 2): any {
  // TODO: call(query, attempt) until it succeeds, fails for good or the attempts run out; return the outcome object.
  return null;
}

export function coordinatorPlan(results: Record<string, any>): Array<[string, string]> | null {
  // TODO: [topic, action] for every topic of results, in order; the run is never stopped.
  return null;
}

export function coverageNote(results: Record<string, any>, topics: string[]): string | null {
  // TODO: the coverage annotation for the report: which topics are well supported, partial, without findings or gaps.
  return null;
}
