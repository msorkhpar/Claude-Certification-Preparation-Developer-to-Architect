// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { assignTools, authorize } from "./distribute.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// The catalog and roles the tests use for the first main case m1.
const catalog = [
  { name: "web_search", tags: ["web"] },
  { name: "fetch_page", tags: ["web"] },
  { name: "load_document", tags: ["documents"] },
  { name: "verify_fact", tags: ["web"], scoped: true },
  { name: "publish_report", tags: ["reports"], irreversible: true },
];
const roles = { searcher: { specialisation: ["web"] }, analyst: { specialisation: ["documents"] } };

const assigned = assignTools(roles, catalog);
console.log("searcher tools:", assigned?.searcher);
console.log("analyst tools:", assigned?.analyst);

// A call to a tool that cannot be undone, checked against the policy.
const policy = { tools: { process_refund: { cap: 500, irreversible: true } } };
const call = { id: "c1", tool: "process_refund", amount: 50, customer: "C-1", verified_customer: "C-1" };
console.log("without approval:", authorize(call, policy, []));
console.log("with approval:", authorize(call, policy, ["c1"]));
