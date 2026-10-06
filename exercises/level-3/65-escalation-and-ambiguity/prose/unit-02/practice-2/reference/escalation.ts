import { logger } from "./logger.ts";
const log = logger("escalation");
/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

function decision(action: string, reason: string, acknowledge = false) {
  return { action, reason, acknowledge };
}

export function decide(c: any, maxAttempts = 2): { action: string; reason: string; acknowledge: boolean } {
  log.debug("decide input", c);
  if (c.asked_for_person ?? false) return decision("escalate", "customer asked for a person");
  if ((c.matches ?? 1) > 1) return decision("clarify", "ambiguous customer match");
  if (!(c.policy_covers ?? true)) return decision("escalate", "policy does not cover the request");
  if ((c.attempts_without_progress ?? 0) >= maxAttempts) return decision("escalate", "no progress");
  return decision("resolve", "within capability", (c.sentiment ?? "calm") !== "calm");
}

export function clarifyingFields(matches: Array<Record<string, string>>): string[] {
  if (matches.length < 2) return [];
  return Object.keys(matches[0]).filter((field) => field !== "id" && new Set(matches.map((m) => m[field])).size > 1);
}

export function handoffText(c: any): string {
  if (!c.customer_id || !c.issue) throw new Error("a hand-off needs a customer id and an issue");
  const lines = [
    `Customer: ${c.customer_id}`,
    `Issue: ${c.issue}`,
    `Root cause: ${c.root_cause || "unknown"}`,
    `Amount: ${c.amount || "unknown"}`,
    `Actions taken: ${(c.actions ?? []).join("; ") || "none"}`,
    `Recommended action: ${c.recommended || "review the case"}`,
  ];
  return lines.join("\n");
}
