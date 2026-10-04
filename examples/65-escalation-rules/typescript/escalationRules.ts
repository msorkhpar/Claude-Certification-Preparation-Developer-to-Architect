/**
 * Why escalation is decided by criteria, and what to ask when a lookup finds several people.
 *
 * The exam guide (task 5.2) names the triggers (a customer asks for a person, the policy is silent or makes an exception, the agent cannot make progress) and says that sentiment and a model's own confidence score are
 * unreliable proxies for how hard a case is. It also says that when a lookup returns several customers the agent asks for more identifiers and does not choose by a heuristic. Below, six hand-written cases
 * (illustrative, not data from a deployment) are routed by a sentiment rule and by the guide's criteria, and a name that matches two accounts is handled both ways. Nothing here calls a model.
 */
// name, sentiment, asked for a person, policy silent, what a careful person would do
export type Case = [string, string, boolean, boolean, string];
export const CASES: Case[] = [
  ["price match with another shop", "calm", false, true, "escalate"],
  ["wrong colour, standard exchange", "angry", false, false, "resolve"],
  ["calm request to speak to a person", "calm", true, false, "escalate"],
  ["password reset", "frustrated", false, false, "resolve"],
  ["refund for an item bought elsewhere", "calm", false, true, "escalate"],
  ["angry, wants a person now", "angry", true, false, "escalate"],
];

export const bySentiment = (c: Case): string => (c[1] === "calm" ? "resolve" : "escalate");
export const byCriteria = (c: Case): string => (c[2] || c[3] ? "escalate" : "resolve");

export function errors(rule: (c: Case) => string): number[] {
  return CASES.flatMap((c, i) => (rule(c) !== c[4] ? [i + 1] : []));
}

/** The heuristic the guide rejects: choose the account with the latest order. */
export function pickMostRecent(matches: Array<{ id: string; last_order: number }>): string {
  return matches.reduce((best, m) => (m.last_order > best.last_order ? m : best)).id;
}

/** What the guide asks for: no choice, a request for something that tells the matches apart. */
export function askForIdentifier(matches: unknown[], fields: string[]): string {
  return `I found ${matches.length} accounts for that name. Please give me one of: ` + fields.join(", ") + ".";
}

/** Explicit criteria and examples for the system prompt: when to escalate, and when not to. */
export function escalationSection(criteria: string[], examples: Array<[string, string, string]>): string {
  const lines = ["Escalate to a person when:", ...criteria.map((c) => `- ${c}`), "", "Examples:"];
  lines.push(...examples.map(([text, decision, why]) => `Customer: "${text}" -> ${decision} (${why})`));
  return lines.join("\n");
}

function main() {
  CASES.forEach((c, i) => console.log(`case ${i + 1} (${c[0]}): sentiment rule ${bySentiment(c)}, criteria ${byCriteria(c)}, careful person ${c[4]}`));
  console.log(`sentiment rule routed ${errors(bySentiment).length} of ${CASES.length} wrongly: cases ` + errors(bySentiment).join(", "));
  console.log(`criteria routed ${errors(byCriteria).length} of ${CASES.length} wrongly`);
  const matches = [{ id: "c1", last_order: 20260901 }, { id: "c2", last_order: 20260915 }];
  console.log(`heuristic: the agent acts on ${pickMostRecent(matches)} although the customer may be c1`);
  console.log(askForIdentifier(matches, ["the email on the account", "the postcode"]));
  console.log(escalationSection(["the customer asks for a person", "the policy does not cover the request", "two attempts made no progress"],
    [["Can you match the price on another site?", "escalate", "the policy only covers our own prices"], ["This is the third time my parcel is late!", "resolve", "a late parcel is within the agent's tools; acknowledge the frustration"]]));
}

if (import.meta.main) main();
