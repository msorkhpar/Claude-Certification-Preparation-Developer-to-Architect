// A support agent's whole control surface in one dispatcher: the identity gate, errors the loop can act on, a stall guard and the three escalation triggers.
//
// The model is a script of the calls it asks for, in the shapes of the tool_use blocks of module 26, without the client: this example is about the code that
// runs the tools, not about what a model says. Customers, orders and the incidents are made up. The limit, the stall count and the codes are this course's own
// design, not an Anthropic interface.
import { logger } from "./logger.ts";
const log = logger("support_desk");

export const LIMIT = 10_000; // cents: a refund above it is a person's decision
export const STALL = 3; // the same call this many times in a row is no progress

const CUSTOMERS: Record<string, [string, string]> = { C1: ["Ana Silva", "ana@example.com"], C2: ["Ana Silva", "ana.s@example.com"], C3: ["Ben Ortiz", "ben@example.com"] };
const ORDERS: Record<string, [string, number]> = { O1: ["C3", 5000], O2: ["C3", 30000], O3: ["C1", 4000] };

export type Args = Record<string, any>;
export type Call = [string, Args];
export type Result = { ok: boolean; code?: string; retryable?: boolean; message?: string; content?: string };
export type Escalation = { trigger: string; reason: string; customer: string | null; verified: boolean; orders: string[]; refunds: string[]; refused: string[] };

/** One conversation's state. A new case gets a new Desk, so nothing of one customer reaches another. */
export class Desk {
  customer: string | null = null;
  checked: string[] = [];
  refunds: string[] = [];
  refused: string[] = [];
  backend: string[] = [];
  recent: string[] = [];
  escalation: Escalation | null = null;
  lastCode: string | null = null;
  retries = 0;
  flaky = new Set(["O2"]); // the first lookup of this order fails with a transient fault

  fail(code: string, message: string, retryable = false): Result {
    this.lastCode = code;
    if (code !== "transient" && code !== "not_found") this.refused.push(code);
    return { ok: false, code, retryable, message };
  }

  /** The record a person reads, built from the desk's own state and not from the model's account. */
  escalate(trigger: string, reason: string): Result {
    this.escalation = { trigger, reason, customer: this.customer, verified: this.customer !== null, orders: [...this.checked].sort(), refunds: [...this.refunds], refused: [...this.refused] };
    return { ok: true, content: "handed over" };
  }

  call(tool: string, args: Args): Result {
    this.lastCode = null;
    this.recent = [...this.recent, JSON.stringify([tool, Object.entries(args).sort()])].slice(-STALL);
    if (tool === "escalate_to_human") return this.escalate(args.trigger, args.reason); // the way to a person never waits for a prerequisite
    if (this.recent.length === STALL && new Set(this.recent).size === 1) return this.escalate("stalled", `${tool} repeated ${STALL} times without progress`);
    if (tool === "get_customer") {
      const found = Object.entries(CUSTOMERS).filter(([, [name, email]]) => args.query === name || args.query === email).map(([id]) => id);
      if (found.length > 1) return this.fail("ambiguous_match", `${found.length} customers match. Ask for the e-mail address. Do not pick one.`);
      if (found.length === 0) return this.fail("not_found", "No customer matches. Ask for the e-mail address.");
      this.customer = found[0];
      return { ok: true, content: `customer_id=${found[0]}` };
    }
    if (this.customer === null) return this.fail("identity_required", "Identify the customer with get_customer before this action.");
    const [owner, total] = ORDERS[args.order_id] ?? [null, 0];
    if (tool === "lookup_order") {
      this.backend.push(tool);
      if (this.flaky.delete(args.order_id)) return this.fail("transient", "The order service timed out. Retry.", true);
      if (owner === null) return this.fail("not_found", "No such order.");
      if (owner !== this.customer) return this.fail("order_not_owned", "That order does not belong to the verified customer.");
      this.checked.push(args.order_id);
      return { ok: true, content: `total_cents=${total}` };
    }
    if (tool === "process_refund") {
      if (!this.checked.includes(args.order_id)) return this.fail("order_not_checked", "Look up the order before refunding it.");
      if (args.amount_cents > LIMIT) return this.fail("needs_human", "A refund above the limit is decided by a person. Escalate.");
      this.backend.push(tool);
      this.refunds.push(`${args.order_id}:${args.amount_cents}`);
      return { ok: true, content: `refund_id=R${this.refunds.length}` };
    }
    return this.fail("unknown_tool", `No tool named ${tool}.`);
  }
}

/** The loop: each call goes through the desk, a retryable error is retried once, and the outcome is read from the desk. */
export function run(script: Call[]): [Desk, string] {
  const desk = new Desk();
  for (const [tool, args] of script) {
    const result = desk.call(tool, args);
    if (!result.ok && result.retryable) {
      desk.retries += 1;
      desk.call(tool, args);
    }
  }
  const outcome = desk.escalation ? "escalated" : desk.lastCode === "ambiguous_match" ? "asked" : "resolved";
  return [desk, outcome];
}

const join = (items: string[]) => items.join(",") || "-";
const BEN: Call = ["get_customer", { query: "ben@example.com" }];
export const INCIDENTS: Array<[string, Call[]]> = [
  ["skips identity", [["lookup_order", { order_id: "O1" }], BEN, ["lookup_order", { order_id: "O1" }], ["process_refund", { order_id: "O1", amount_cents: 2000 }]]],
  ["transient fault", [BEN, ["lookup_order", { order_id: "O2" }], ["process_refund", { order_id: "O2", amount_cents: 8000 }]]],
  ["over the limit", [BEN, ["lookup_order", { order_id: "O2" }], ["process_refund", { order_id: "O2", amount_cents: 25000 }],
    ["escalate_to_human", { trigger: "needs_human", reason: "refund of 250.00 asked" }]]],
  ["someone else's order", [["get_customer", { query: "ana@example.com" }], ["lookup_order", { order_id: "O1" }]]],
  ["two customers match", [["get_customer", { query: "Ana Silva" }]]],
  ["asks for a person", [["escalate_to_human", { trigger: "customer_request", reason: "customer asked for a person" }]]],
  ["no progress", [BEN, ...Array.from({ length: 3 }, (): Call => ["lookup_order", { order_id: "O9" }])]],
];

function main() {
  for (const [name, script] of INCIDENTS) {
    const [desk, outcome] = run(script);
    console.log(`${name.padEnd(21)} outcome=${outcome.padEnd(9)} refused=${join(desk.refused)} retries=${desk.retries} refunds=${join(desk.refunds)} backend=${join(desk.backend)}`);
    if (desk.escalation) {
      const e = desk.escalation;
      console.log(`  handoff: trigger=${e.trigger} verified=${e.verified ? "yes" : "no"} customer=${e.customer ?? "-"} orders=${join(e.orders)} refunds=${join(e.refunds)} refused=${join(e.refused)}`);
    }
  }
}

if (import.meta.main) main();
