// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { RefundDesk } from "./desk.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const ORDER = { order_id: "O1", customer_id: "C1", total_cents: 5000, refunded_cents: 0 };
const refunds: any[] = [];

// A scripted backend, like the one the tests use: four functions that answer with fixed data.
const backend: any = {
  verify_identity: (args: any) => (args.code === "1234" ? { verified: "yes", customer_id: "C1" } : { verified: "no" }),
  lookup_order: (_args: any) => ({ ...ORDER }),
  process_refund: (args: any) => { refunds.push(args); return { refund_id: `R${refunds.length}`, amount_cents: args.amount_cents }; },
  escalate: (_args: any) => ({ ticket_id: "T1" }),
};
const desk = new RefundDesk(backend);

// The model breaks the order of the steps: a refund before identity is verified must be blocked in code.
const early: any = desk.call("process_refund", { order_id: "O1", amount_cents: 2000 }) ?? {};
console.log("refund before verifying:", early.content, "| is_error:", early.is_error);

for (const [name, args] of [["verify_identity", { code: "1234" }], ["lookup_order", { order_id: "O1" }],
  ["process_refund", { order_id: "O1", amount_cents: 2000 }]] as [string, any][]) {
  const out: any = desk.call(name, args) ?? {};
  console.log(`${name}:`, out.content);
}
console.log("refunds that reached the backend:", refunds.length);
