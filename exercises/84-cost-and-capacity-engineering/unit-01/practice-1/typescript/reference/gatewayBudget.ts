/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. */

export function route(request: any, policy: any, status: string): string | null {
  if (status === "block") return null;
  const wanted = request.model;
  let model: string = policy.allowed.includes(wanted) ? wanted : (policy.routes[request.task] ?? policy.default);
  if (status === "warn") {
    model = policy.cheaper[model] ?? model;
  }
  return model;
}

export function admit(spend: number, budget: number, estimate: number): string {
  if (budget <= 0) {
    return "block";
  }
  const after = spend + estimate;
  if (after > budget) return "block";
  if (after * 100 >= budget * 80) return "warn";
  return "allow";
}

export function showback(rows: any[], prices: any): Array<{ team: string; cents: number }> {
  const totals = new Map<string, number>();
  for (const r of rows) {
    if (!(r.model in prices)) throw new Error(`unknown model: ${r.model}`);
    const p = prices[r.model];
    totals.set(r.team, (totals.get(r.team) ?? 0) + r.input * p.input + r.cache_read * p.cache_read + r.output * p.output);
  }
  const result = [...totals].map(([team, value]) => ({ team, cents: Math.floor((value + 500_000) / 1_000_000) }));
  return result.sort((a, b) => b.cents - a.cents || (a.team < b.team ? -1 : a.team > b.team ? 1 : 0));
}

export function delivery(p95Seconds: number, timeoutSeconds: number, marginPercent: number): string {
  return p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100 ? "sync" : "accept-and-poll";
}
