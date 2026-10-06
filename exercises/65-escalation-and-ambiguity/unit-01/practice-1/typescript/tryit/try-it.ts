// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { clarifyingFields, decide, handoffText } from "./escalation.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// A customer who asks for a person is escalated at once; a calm one the agent can resolve is not.
const asked = { asked_for_person: true, matches: 1, policy_covers: true, attempts_without_progress: 0, sentiment: "calm", confidence: 95 };
console.log("asked for a person:", decide(asked));
const calm = { ...asked, asked_for_person: false };
console.log("calm and covered:", decide(calm));

// Two customers match the name: ask only for the field that tells them apart.
const matches = [{ id: "c1", name: "Ana Ruiz", email: "ana@example.com", zip: "10115" },
  { id: "c2", name: "Ana Ruiz", email: "ana.r@example.com", zip: "10115" }];
console.log("ask for:", clarifyingFields(matches));

// The hand-off carries the facts, not the transcript.
const handoffCase = { customer_id: "C-77", issue: "refund over the limit", root_cause: "duplicate charge", amount: "$129.50",
  actions: ["verified identity", "checked order"], recommended: "approve the refund", transcript: "user: hello ... 40 turns ..." };
console.log(handoffText(handoffCase));
