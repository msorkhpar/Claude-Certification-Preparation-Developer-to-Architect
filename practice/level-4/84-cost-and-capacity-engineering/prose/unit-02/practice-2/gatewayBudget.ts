/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. */
import { logger } from "./logger.ts";
const log = logger("gateway_budget");

function chosenModel(request: any, policy: any): string | null {
  // TODO 1 of 6 (unlocks m1 and e6): the model the policy picks for a request.
  // Receives the request (`task`, and perhaps `model`, a pin) and the policy (`allowed`, `routes`, `default`). Returns the pinned model when it is in
  // `policy.allowed`; otherwise the model `policy.routes` names for the task, or `policy.default` for a task that is not listed.
  // Example: task "review" with no pin -> "opus"; task "classify" pinned to "opus" (not allowed) -> "haiku"
  return null;
}

export function route(request: any, policy: any, status: string): string | null {
  log.debug("route input", request);
  // TODO 2 of 6 (unlocks e1): the model for the request once the budget status is known.
  // Receives the request, the policy and the budget status ("allow", "warn" or "block"). Returns null when the status is "block"; the model from
  // `chosenModel` when it is "allow"; for "warn" the cheaper model that `policy.cheaper` names for it (the model itself when none is named).
  // Example: status "warn", chosen model "opus", cheaper { opus: "sonnet" } -> "sonnet"
  return null;
}

export function admit(spend: number, budget: number, estimate: number): string | null {
  // TODO 3 of 6 (unlocks e2): admit one more request against the budget.
  // Receives what the team has spent, its budget and the estimated cost of the request, all in cents. Returns "block" when the budget is not positive or
  // `spend + estimate` is more than the budget; "warn" when it reaches 80 percent of the budget (80 percent itself warns) and is not over; else "allow".
  // Example: admit(700, 1000, 100) -> "warn", admit(900, 1000, 101) -> "block"
  return null;
}

function cost(row: any, prices: any): number {
  // TODO 4 of 6 (unlocks e3): what one row of tokens costs.
  // Receives a row (`model`, `input`, `cache_read`, `output`: token counts) and `prices`, which maps a model to cents per million tokens for the same three
  // kinds. Returns tokens times price, summed over the three kinds (not yet divided by a million). Throws Error(`unknown model: ${name}`) for a model without a price.
  // Example: input 1_000_000 at price 200 and nothing else -> 200_000_000
  return 0;
}

function toCents(value: number): number {
  // TODO 5 of 6 (unlocks e4): a team's total in whole cents.
  // Receives a total in cents times a million. Returns it divided by one million and rounded to the nearest cent, halves up.
  // Example: toCents(500_000) -> 1, toCents(499_999) -> 0
  return 0;
}

export function showback(rows: any[], prices: any): Array<{ team: string; cents: number }> | null {
  const totals = new Map<string, number>();
  for (const r of rows) totals.set(r.team, (totals.get(r.team) ?? 0) + cost(r, prices));
  const result = [...totals].map(([team, value]) => ({ team, cents: toCents(value) }));
  return result.sort((a, b) => b.cents - a.cents || (a.team < b.team ? -1 : a.team > b.team ? 1 : 0));
}

export function delivery(p95Seconds: number, timeoutSeconds: number, marginPercent: number): string | null {
  // TODO 6 of 6 (unlocks e5): sync or accept-and-poll.
  // Receives the p95 latency and the caller's timeout in seconds and a safety margin in percent. Returns "sync" when the p95 plus the margin fits within the
  // timeout (an exact fit counts), otherwise "accept-and-poll". Compare p95 * (100 + margin) with timeout * 100.
  // Example: delivery(8, 10, 25) -> "sync", delivery(8, 10, 26) -> "accept-and-poll"
  return null;
}
