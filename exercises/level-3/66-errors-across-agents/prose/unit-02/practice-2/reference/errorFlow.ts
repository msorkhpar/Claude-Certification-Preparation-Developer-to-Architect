import { logger } from "./logger.ts";
const log = logger("error_flow");
/** How a subagent's failure reaches the coordinator and the report: local recovery, structured error context, valid empty results, and coverage notes. See ../../statement.md. */

export const ALTERNATIVES: Record<string, string[]> = {
  timeout: ["retry later", "try a narrower query"],
  unavailable: ["use a cached source", "try another provider"],
  permission: ["request access", "use a public source"],
  invalid_query: ["rewrite the query"],
};
export const TRANSIENT = ["timeout", "unavailable"];

export function searchWithRecovery(query: string, call: (query: string, attempt: number) => any, maxAttempts = 2): any {
  log.debug("searchWithRecovery input", query);
  let attempts = 0;
  for (;;) {
    attempts += 1;
    const reply = call(query, attempts);
    if (reply.status === "ok") {
      const items = reply.items;
      return { status: items.length > 0 ? "success" : "empty", items, attempts };
    }
    const kind = reply.type;
    if (TRANSIENT.includes(kind) && attempts < maxAttempts) continue;
    return { status: "failed", failure_type: kind, attempted: query, attempts, partial_results: reply.partial ?? [], alternatives: ALTERNATIVES[kind] ?? [] };
  }
}

export function coordinatorPlan(results: Record<string, any>): Array<[string, string]> {
  const plan: Array<[string, string]> = [];
  for (const [topic, outcome] of Object.entries(results)) {
    let action: string;
    if (outcome.status === "success") action = "use";
    else if (outcome.status === "empty") action = "no_findings";
    else if (outcome.partial_results.length > 0) action = "use_partial";
    else if (outcome.alternatives.length > 0) action = "try_alternative";
    else action = "flag_gap";
    plan.push([topic, action]);
  }
  return plan;
}

export function coverageNote(results: Record<string, any>, topics: string[]): string {
  const groups: Record<string, string[]> = { "Well-supported": [], Partial: [], "No findings": [], Gaps: [] };
  for (const topic of topics) {
    const outcome = results[topic];
    if (outcome === undefined) groups["Gaps"].push(`${topic} (not searched)`);
    else if (outcome.status === "success") groups["Well-supported"].push(topic);
    else if (outcome.status === "empty") groups["No findings"].push(topic);
    else if (outcome.partial_results.length > 0) groups["Partial"].push(`${topic} (${outcome.failure_type})`);
    else groups["Gaps"].push(`${topic} (${outcome.failure_type}: ${outcome.attempted})`);
  }
  return Object.entries(groups).filter(([, items]) => items.length > 0).map(([name, items]) => `${name}: ${items.join(", ")}`).join("\n");
}
