import { logger } from "../logger.ts";
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
      // TODO 2 of 6 (finish this to pass e1): the status of an ok reply. Return success when the reply holds items and
      //   empty when it holds none (a search that found nothing is a valid answer, not a failure). Example: ok with [] ->
      //   empty.
      return { status: "success", items, attempts };
    }
    const kind = reply.type;
    // TODO 1 of 6 (finish this to pass m1): the local retry. When the failure type is one of TRANSIENT and the attempts
    //   so far are below max_attempts, try again; otherwise give up. Example: timeout then ok, limit 2 -> success with
    //   attempts 2.
    // TODO 3 of 6 (finish this to pass e2, e3): the failed outcome. Return status failed with the failure type, the
    //   query that was attempted, the attempts, the partial results the reply carried (none when absent) and the
    //   alternatives listed for that failure type (none when unknown). Example: permission error -> alternatives [request
    //   access, use a public source].
    return { status: "failed", failure_type: kind, attempted: query, attempts, partial_results: [], alternatives: [] };
  }
}

export function coordinatorPlan(results: Record<string, any>): Array<[string, string]> {
  const plan: Array<[string, string]> = [];
  for (const [topic, outcome] of Object.entries(results)) {
    let action: string;
    if (outcome.status === "success") action = "use";
    else if (outcome.status === "empty") action = "no_findings";
    // TODO 4 of 6 (finish this to pass e4): the rows for a failed topic. When the outcome has partial results, the
    //   action is use_partial; otherwise try_alternative when it has alternatives; otherwise flag_gap. Example: failed, no
    //   partial, alternatives [rewrite the query] -> try_alternative.
    else action = "flag_gap";
    plan.push([topic, action]);
  }
  return plan;
}

export function coverageNote(results: Record<string, any>, topics: string[]): string {
  const groups: Record<string, string[]> = { "Well-supported": [], Partial: [], "No findings": [], Gaps: [] };
  for (const topic of topics) {
    const outcome = results[topic];
    // TODO 6 of 6 (finish this to pass e6): the topic with no result. When a requested topic has no outcome at all, list
    //   it under Gaps as "topic (not searched)". Example: topics [a, b], results only for a -> Gaps: b (not searched).
    if (outcome === undefined) continue;
    else if (outcome.status === "success") groups["Well-supported"].push(topic);
    else if (outcome.status === "empty") groups["No findings"].push(topic);
    // TODO 5 of 6 (finish this to pass e5): the groups of a failed topic in the coverage note. With partial results,
    //   list the topic under Partial as "topic (failure type)"; without, under Gaps as "topic (failure type: query
    //   attempted)". Example: partial, timeout -> Partial: topic (timeout).
    else groups["Gaps"].push(topic);
  }
  return Object.entries(groups).filter(([, items]) => items.length > 0).map(([name, items]) => `${name}: ${items.join(", ")}`).join("\n");
}
