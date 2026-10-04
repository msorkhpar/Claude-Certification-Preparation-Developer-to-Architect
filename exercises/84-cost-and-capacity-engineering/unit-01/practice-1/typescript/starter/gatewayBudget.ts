/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. */

export function route(request: any, policy: any, status: string): string | null {
  // TODO: the model for the request, or null when the team is blocked.
  return null;
}

export function admit(spend: number, budget: number, estimate: number): string | null {
  // TODO: "allow", "warn" or "block" for one more request against the budget.
  return null;
}

export function showback(rows: any[], prices: any): Array<{ team: string; cents: number }> | null {
  // TODO: [{ team, cents }] for each team, most expensive first, ties by team name.
  return null;
}

export function delivery(p95Seconds: number, timeoutSeconds: number, marginPercent: number): string | null {
  // TODO: "sync" or "accept-and-poll".
  return null;
}
