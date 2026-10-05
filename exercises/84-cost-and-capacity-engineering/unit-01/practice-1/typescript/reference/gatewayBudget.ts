/** The decisions of an internal gateway: route a request, admit it against a budget, show back what each team spent and choose sync or accept-and-poll. See ../../statement.md. */
import { logger } from "../logger.ts";
const log = logger("gateway_budget");

function chosenModel(request: any, policy: any): string | null {
  const wanted = request.model;
  return policy.allowed.includes(wanted) ? wanted : (policy.routes[request.task] ?? policy.default);
}

export function route(request: any, policy: any, status: string): string | null {
  log.debug("route input", request);
  if (status === "block") return null;
  const model = chosenModel(request, policy) as string;
  return status === "warn" ? (policy.cheaper[model] ?? model) : model;
}

export function admit(spend: number, budget: number, estimate: number): string | null {
  if (budget <= 0) return "block";
  const after = spend + estimate;
  if (after > budget) return "block";
  if (after * 100 >= budget * 80) return "warn";
  return "allow";
}

function cost(row: any, prices: any): number {
  if (!(row.model in prices)) throw new Error(`unknown model: ${row.model}`);
  const p = prices[row.model];
  return row.input * p.input + row.cache_read * p.cache_read + row.output * p.output;
}

function toCents(value: number): number {
  return Math.floor((value + 500_000) / 1_000_000);
}

export function showback(rows: any[], prices: any): Array<{ team: string; cents: number }> | null {
  const totals = new Map<string, number>();
  for (const r of rows) totals.set(r.team, (totals.get(r.team) ?? 0) + cost(r, prices));
  const result = [...totals].map(([team, value]) => ({ team, cents: toCents(value) }));
  return result.sort((a, b) => b.cents - a.cents || (a.team < b.team ? -1 : a.team > b.team ? 1 : 0));
}

export function delivery(p95Seconds: number, timeoutSeconds: number, marginPercent: number): string | null {
  return p95Seconds * (100 + marginPercent) <= timeoutSeconds * 100 ? "sync" : "accept-and-poll";
}
