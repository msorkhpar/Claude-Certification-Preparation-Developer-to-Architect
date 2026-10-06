// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { audit, gateway, planLoading } from "./capability.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const catalog = {
  read_ticket: { access: "read", tokens: 160 },
  draft_reply: { access: "draft", tokens: 220 },
  issue_refund: { access: "money", tokens: 240 },
  export_report: { access: "read", tokens: 300 },
};

// An agent holds more tools than its role needs: the audit names what to remove and which of them are risky.
const agent = { holds: Object.keys(catalog), needs: ["read_ticket", "draft_reply"], used: { read_ticket: 12, draft_reply: 9 } };
console.log("audit:", JSON.stringify(audit(agent, catalog)));

// Loading a long tool list: the most used tools load now, the rest wait behind a search tool.
const tools: Record<string, number> = {};
for (let i = 1; i <= 7; i++) tools[`t${String(i).padStart(2, "0")}`] = 100;
console.log("loading plan:", JSON.stringify(planLoading(tools, { t01: 9, t02: 5 }, 3)));

// The gateway checks one request against the policy.
const policy = { credentials: { k1: "support" }, models: { support: ["standard"] }, tools: { support: ["read_ticket", "draft_reply"] },
  limits: { support: 2 }, routes: { standard: "claude-sonnet-5-5" } };
console.log("allowed:", JSON.stringify(gateway({ credential: "k1", model: "standard", tool: "read_ticket", recent: 0 }, policy)));
console.log("over the limit:", JSON.stringify(gateway({ credential: "k1", model: "standard", tool: "read_ticket", recent: 2 }, policy)));
