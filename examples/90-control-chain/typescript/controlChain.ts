/**
 * Governing a model call: a control that fails closed where the cost of an error is high, an independent check against the source that a confident answer must pass, and an audit record that holds no content.
 *
 * The requests, answers and thresholds are invented; the confidence threshold of 95 is a value to tune to your own error costs. Nothing here calls a model.
 */
export type Action = { name: string; consequence: string };
export type Answer = { text: string; confidence: number; quote: string };

export const AUTO_CONFIDENCE = 95;

/** Decide what happens to an answer. A down screen holds a high-consequence action, an unsupported answer is held whatever its confidence, and only a confident, supported, low-consequence answer goes out unreviewed. */
export function route(action: Action, answer: Answer, source: string, screenUp: boolean, confidenceMin = AUTO_CONFIDENCE): string {
  if (!screenUp && action.consequence === "high") return "hold: screen down";
  const flag = screenUp ? "" : " (unscreened)";
  if (!source.includes(answer.quote)) return "hold: unsupported" + flag;
  if (action.consequence === "high") return "human" + flag;
  return (answer.confidence >= confidenceMin ? "auto" : "review") + flag;
}

/** Proof of what happened without a copy of the data: who, what, how big and the outcome, and never the text. */
export function auditRecord(requestId: string, action: Action, outcome: string, text: string): Record<string, string | number | boolean> {
  return { request: requestId, action: action.name, consequence: action.consequence, outcome, chars: text.length, content_stored: false };
}

/** Erasure removes the map from a token to a person, so the audit entries that carry only tokens can no longer be linked to anyone. */
export function erase(vault: Record<string, string>, subject: string): [Record<string, string>, number] {
  const kept: Record<string, string> = {};
  for (const [token, person] of Object.entries(vault)) if (person !== subject) kept[token] = person;
  return [kept, Object.keys(vault).length - Object.keys(kept).length];
}

function main(): void {
  const source = "Water damage is covered up to 5,000 per claim. Flood damage is excluded.";
  const reply: Action = { name: "draft_reply", consequence: "low" };
  const refund: Action = { name: "issue_refund", consequence: "high" };
  const supported = "Water damage is covered up to 5,000 per claim.";
  const good: Answer = { text: "Water damage is covered up to 5,000.", confidence: 99, quote: supported };
  const edge: Answer = { text: "Water damage is covered up to 5,000.", confidence: 95, quote: supported };
  const unsure: Answer = { text: "Water damage is covered up to 5,000.", confidence: 94, quote: supported };
  const wrong: Answer = { text: "Water damage is covered up to 8,000.", confidence: 99, quote: "Water damage is covered up to 8,000 per claim." };
  const cases: [string, Action, Answer, boolean][] = [
    ["screen up, refund, supported", refund, good, true],
    ["screen up, reply, confidence 99", reply, good, true],
    ["screen up, reply, confidence 95", reply, edge, true],
    ["screen up, reply, confidence 94", reply, unsure, true],
    ["screen up, reply, confident but unsupported", reply, wrong, true],
    ["screen down, refund", refund, good, false],
    ["screen down, reply", reply, good, false],
  ];
  for (const [label, action, answer, up] of cases) console.log(`${label}: ${route(action, answer, source, up)}`);
  const record = auditRecord("r-1001", refund, "human", good.text);
  console.log("audit record:", Object.entries(record).map(([k, v]) => `${k}=${v === true ? "True" : v === false ? "False" : v}`).join(", "));
  const vault = { "<EMAIL_1>": "person-a", "<EMAIL_2>": "person-b", "<MEMBER_1>": "person-a" };
  const [kept, removed] = erase(vault, "person-a");
  console.log(`erasure removed ${removed} of ${Object.keys(vault).length} mappings; the audit entries stay, with ${Object.keys(kept).length} token still linkable`);
}

if (import.meta.main) main();
