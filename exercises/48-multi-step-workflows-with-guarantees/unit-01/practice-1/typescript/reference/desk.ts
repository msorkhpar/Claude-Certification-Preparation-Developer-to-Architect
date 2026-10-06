// A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("desk");
const MESSAGES: Record<string, string> = {
  identity_required: "Verify the customer's identity before this action.",
  order_not_owned: "That order does not belong to the verified customer.",
  order_not_checked: "Look up the order before refunding it.",
  bad_amount: "The amount must be a positive whole number of cents.",
  exceeds_order: "The amount is more than what is left to refund on the order.",
  needs_human: "Refunds over the limit need a person.",
  locked: "Too many failed identity checks; escalate to a person.",
};
const TOOLS = ["verify_identity", "lookup_order", "process_refund", "escalate"];

type Result = { content: string; is_error: boolean; blocked: string | null };
type Backend = Record<string, (args: any) => Record<string, any>>;

const render = (result: Record<string, any>) => Object.entries(result).map(([k, v]) => `${k}=${v}`).join("; ");

export class RefundDesk {
  private customer: string | null = null;
  private locked = false;
  private failures = 0;
  private orders: Record<string, any> = {};
  private refunds: Array<Record<string, any>> = [];
  private blockedCalls: Array<{ tool: string; code: string }> = [];
  private calls: string[] = [];

  private backend: Backend;
  private limitCents: number;

  constructor(backend: Backend, limitCents = 10000) {
    this.backend = backend;
    this.limitCents = limitCents;
  }

  state(): any {
    return { customer: this.customer, locked: this.locked, failures: this.failures, orders: structuredClone(this.orders), refunds: structuredClone(this.refunds),
      blocked: structuredClone(this.blockedCalls), backend_calls: [...this.calls] };
  }

  private block(tool: string, code: string, message?: string): Result {
    this.blockedCalls.push({ tool, code });
    return { content: `BLOCKED ${code}: ${message ?? MESSAGES[code]}`, is_error: true, blocked: code };
  }

  private run(name: string, args: any): { result?: Record<string, any>; error?: Result } {
    this.calls.push(name);
    try {
      return { result: this.backend[name](args) };
    } catch (error) { // a backend failure is a result for the model and changes nothing here
      return { error: { content: error instanceof Error ? error.message : String(error), is_error: true, blocked: null } };
    }
  }

  private ok(result: Record<string, any>): Result {
    return { content: render(result), is_error: false, blocked: null };
  }

  call(name: string, args: any): Result {
    log.debug("call input", args);
    if (!TOOLS.includes(name)) return this.block(name, "unknown_tool", `Unknown tool: ${name}`);
    if (name === "escalate") {
      const { result, error } = this.run(name, args);
      return error ?? this.ok(result!);
    }
    if (this.locked) return this.block(name, "locked");
    if (name === "verify_identity") {
      const { result, error } = this.run(name, args);
      if (error) return error;
      if (result!.verified === "yes" && result!.customer_id) {
        this.customer = result!.customer_id;
        this.failures = 0;
      } else {
        this.customer = null;
        this.failures += 1;
        this.locked = this.failures >= 3;
      }
      return this.ok(result!);
    }
    if (this.customer === null) return this.block(name, "identity_required");
    if (name === "lookup_order") {
      const { result, error } = this.run(name, args);
      if (error) return error;
      if (result!.customer_id !== this.customer) return this.block(name, "order_not_owned");
      this.orders[result!.order_id] = { ...result };
      return this.ok(result!);
    }
    const order = this.orders[args?.order_id];
    if (order === undefined) return this.block(name, "order_not_checked");
    const amount = args.amount_cents;
    if (typeof amount !== "number" || !Number.isInteger(amount) || amount <= 0) return this.block(name, "bad_amount");
    if (amount > order.total_cents - order.refunded_cents) return this.block(name, "exceeds_order");
    if (amount > this.limitCents) return this.block(name, "needs_human");
    const { result, error } = this.run(name, args);
    if (error) return error;
    order.refunded_cents += amount;
    this.refunds.push({ order_id: order.order_id, amount_cents: amount, refund_id: result!.refund_id });
    return this.ok(result!);
  }

  handoff(reason: string): any {
    const last = this.blockedCalls.length > 0 ? this.blockedCalls[this.blockedCalls.length - 1].code : null;
    const action = this.locked ? "verify_identity_manually" : last === "needs_human" ? "review_refund" : "review_case";
    return { customer_id: this.customer, identity_verified: this.customer !== null, reason, orders_checked: Object.keys(this.orders), refunds_done: structuredClone(this.refunds),
      blocked: structuredClone(this.blockedCalls), recommended_action: action };
  }
}
