import { logger } from "./logger.ts";
const log = logger("escalation");
/** When a support agent resolves, asks or hands off, and what a hand-off carries. See ../../statement.md. */

function decision(action: string, reason: string, acknowledge = false) {
  return { action, reason, acknowledge };
}

export function decide(c: any, maxAttempts = 2): { action: string; reason: string; acknowledge: boolean } {
  log.debug("decide input", c);
  // TODO 1 of 7 (finish this to pass m1, e4): the first rule. When the customer asked for a person, return the decision
  //   escalate with the reason "customer asked for a person", before any other rule. Example: asked_for_person true and
  //   two matches -> escalate.
  // TODO 2 of 7 (finish this to pass e3): the ambiguity rule. When more than one customer record matches, return the
  //   decision clarify with the reason "ambiguous customer match". Example: matches 2 -> clarify.
  // TODO 3 of 7 (finish this to pass e2): the policy rule. When the policy does not cover the request, return the
  //   decision escalate with the reason "policy does not cover the request". Example: policy_covers false -> escalate.
  // TODO 4 of 7 (finish this to pass e5): the progress rule. When the attempts without progress are at or above
  //   max_attempts, return the decision escalate with the reason "no progress". Example: 2 attempts, limit 2 -> escalate;
  //   1 attempt -> not.
  // TODO 5 of 7 (finish this to pass e1): the acknowledgement of a resolved case. Return resolve with the reason "within
  //   capability" and acknowledge true when the sentiment is anything but calm, false when it is calm. Example: sentiment
  //   frustrated -> resolve, acknowledge true.
  return decision("resolve", "within capability");
}

export function clarifyingFields(matches: Array<Record<string, string>>): string[] {
  if (matches.length < 2) return [];
  // TODO 6 of 7 (finish this to pass e7): the fields to ask about. Receives the matching records (maps). Return the
  //   names of the fields, other than id, whose values are not all the same across the matches; return none when there are
  //   fewer than two matches. Example: two records that differ only in city -> [city].
  return [];
}

export function handoffText(c: any): string {
  if (!c.customer_id || !c.issue) throw new Error("a hand-off needs a customer id and an issue");
  const lines = [
    `Customer: ${c.customer_id}`,
    `Issue: ${c.issue}`,
    `Root cause: ${c.root_cause || "unknown"}`,
    `Amount: ${c.amount || "unknown"}`,
    // TODO 7 of 7 (finish this to pass e8): the last two lines of the hand-off. Write "Actions taken: " with the actions
    //   joined by "; " (or none when there are none) and "Recommended action: " with the recommendation (or review the
    //   case when it is empty). Never the transcript. Example: actions [refund, email] -> "Actions taken: refund; email".
    "Actions taken: none",
    "Recommended action: review the case",
  ];
  return lines.join("\n");
}
