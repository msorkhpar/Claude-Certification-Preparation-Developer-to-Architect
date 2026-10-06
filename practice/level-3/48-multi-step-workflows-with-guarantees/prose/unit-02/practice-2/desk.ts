// A refund desk whose prerequisites are enforced in code, with a structured hand-off to a person. See ../../statement.md.
import { logger } from "./logger.ts";
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
    // TODO 7 of 8 (finish this to pass e6): the first check of call. When the tool name is not one of TOOLS, return the
    //   refusal `unknown_tool` with the message "Unknown tool: NAME". Example: call("delete_everything", {}) -> BLOCKED
    //   unknown_tool: Unknown tool: delete_everything.
    if (name === "escalate") {
      const { result, error } = this.run(name, args);
      return error ?? this.ok(result!);
    }
    if (this.locked) return this.block(name, "locked");
    if (name === "verify_identity") {
      const { result, error } = this.run(name, args);
      if (error) return error;
      // TODO 1 of 8 (finish this to pass m1, e5): what a `verify_identity` result does to the desk. Receives the
      //   backend's result map. A result with `verified` equal to `yes` and a `customer_id` sets the verified customer and
      //   resets the failure count to 0; any other result clears the verified customer, adds one failure and locks the
      //   desk at three. Example: three results of {verified: no} lock the desk.
      return this.ok(result!);
    }
    // TODO 2 of 8 (finish this to pass e1): the prerequisite check. When no customer is verified, return the refusal
    //   `identity_required` for this call: block(name, "identity_required") builds and records it. Example: process_refund
    //   before verify_identity -> BLOCKED identity_required, and the backend is never called.
    if (name === "lookup_order") {
      const { result, error } = this.run(name, args);
      if (error) return error;
      // TODO 3 of 8 (finish this to pass e2): the ownership check on a looked-up order. When the order's customer_id is
      //   not the verified customer, return the refusal `order_not_owned` before the order is remembered. Example:
      //   customer C1 looks up an order of C2 -> BLOCKED order_not_owned, and state()["orders"] stays empty.
      this.orders[result!.order_id] = { ...result };
      return this.ok(result!);
    }
    const order = this.orders[args?.order_id];
    if (order === undefined) return this.block(name, "order_not_checked");
    const amount = args.amount_cents;
    // TODO 4 of 8 (finish this to pass e3): the amount check. Receives the amount from the call. When it is not a whole
    //   number above zero (a boolean, a decimal, a string, a missing value or 0), return the refusal `bad_amount`.
    //   Example: amount_cents 0 or 12.5 or True -> BLOCKED bad_amount.
    // TODO 5 of 8 (finish this to pass e3): the check against the order. When the amount is more than the order's
    //   total_cents minus its refunded_cents, return the refusal `exceeds_order`. Example: 3000 refunded on a 5000 order,
    //   then 2001 -> BLOCKED exceeds_order.
    // TODO 6 of 8 (finish this to pass e4): the authority limit. When the amount is above the desk's limit, return the
    //   refusal `needs_human` and do not call the backend. Example: limit 10000, amount 12000 on a 50000 order -> BLOCKED
    //   needs_human.
    const { result, error } = this.run(name, args);
    if (error) return error;
    order.refunded_cents += amount;
    this.refunds.push({ order_id: order.order_id, amount_cents: amount, refund_id: result!.refund_id });
    return this.ok(result!);
  }

  handoff(reason: string): any {
    const last = this.blockedCalls.length > 0 ? this.blockedCalls[this.blockedCalls.length - 1].code : null;
    // TODO 8 of 8 (finish this to pass e4, e5): the recommended action of the hand-off. `last` is the code of the most
    //   recent refusal or none. Choose verify_identity_manually when the desk is locked, otherwise review_refund when last
    //   is needs_human, otherwise review_case. Example: locked desk -> verify_identity_manually.
    const action = "review_case";
    return { customer_id: this.customer, identity_verified: this.customer !== null, reason, orders_checked: Object.keys(this.orders), refunds_done: structuredClone(this.refunds),
      blocked: structuredClone(this.blockedCalls), recommended_action: action };
  }
}
