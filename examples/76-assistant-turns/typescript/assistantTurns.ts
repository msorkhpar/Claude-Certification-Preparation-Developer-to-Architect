// The turn logic of a conversational assistant in miniature: a message is routed in code before the model sees it, the window keeps the pinned facts and the newest turns
// that fit, and a memory that crosses sessions is read per customer and marked when it is old.
//
// The words are made up and the routing is a plain list of phrases: this example is about where each decision lives, not about what a model would say. The shapes (a route,
// a window, a recalled fact with a status) are this course's design, not an Anthropic interface.
const RISK = ["hurt myself", "end my life", "emergency"];
const ASKS_FOR_PERSON = ["human", "a person", "an agent"];
const STORE: Record<string, [key: string, value: string, saved: string][]> = { ada: [["address", "12 Elm Road", "2026-09-20"], ["plan", "Plus", "2025-12-01"]] };

/** Decided in code, in this order: a signal of risk, a request for a person, a stalled conversation, otherwise the model answers. */
export function route(message: string, misses = 0): string {
  const text = message.toLowerCase();
  if (RISK.some((p) => text.includes(p))) return "handoff:safety";
  if (ASKS_FOR_PERSON.some((p) => text.includes(p))) return "handoff:requested";
  if (misses >= 2) return "handoff:stalled";
  return "answer";
}

/** The pinned facts always stay; of the turns, the newest ones that fit the budget (counted in words) stay, as one block at the end. */
export function window(turns: string[], budget: number, facts: string[]) {
  const kept: string[] = [];
  let used = 0;
  for (const turn of [...turns].reverse()) {
    const words = turn.split(/\s+/).length;
    if (used + words > budget) break;
    kept.unshift(turn);
    used += words;
  }
  return { kept, dropped: turns.length - kept.length, facts };
}

/** The facts saved for this customer only, each marked current or to be verified when it is older than the limit. */
export function recall(user: string, today: string, maxAgeDays = 30): [string, string, string][] {
  return (STORE[user] ?? []).map(([key, value, saved]) => {
    const age = Math.round((Date.parse(today) - Date.parse(saved)) / 86400000);
    return [key, value, age <= maxAgeDays ? "current" : "verify"];
  });
}

function main() {
  for (const [message, misses] of [["Where is my parcel?", 0], ["I want to talk to a human", 0], ["I feel like I might hurt myself", 0], ["what?", 2]] as [string, number][]) {
    console.log(`route '${message}'` + (misses ? ` after ${misses} misses` : "") + `: ${route(message, misses)}`);
  }
  const turns = ["Hello", "My parcel has not arrived", "It was due on Monday", "Can you check the order", "Order 1234 please"];
  const facts = ["order 1234: parcel due 2026-09-28", "address: 12 Elm Road"];
  const w = window(turns, 12, facts);
  console.log(`window: ${turns.length} turns, budget 12 words -> kept ${w.kept.length}, dropped ${w.dropped}, facts kept ${w.facts.length}`);
  for (const user of ["ada", "bob"]) {
    const found = recall(user, "2026-10-04");
    console.log(`recall ${user} on 2026-10-04: ` + (found.map(([k, v, s]) => `${k}=${v} (${s})`).join(", ") || "nothing stored"));
  }
}

if (import.meta.main) main();
