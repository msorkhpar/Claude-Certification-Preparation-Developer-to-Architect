// One small task, three ways to build it: a graph you draw, a loop the model drives, and a typed result you validate.
//
// The task is a support ticket: decide whether it is about billing, look the invoice up when it is, and write a reply. The three
// functions below are miniatures of what graph-based, model-driven and typed agent frameworks give you. They are the course's own
// sketches, not any framework's code, and the model is a scripted function, so nothing here calls an API.
import { logger } from "./logger.ts";
const log = logger("three_styles");

export const TICKET = "I was charged twice for invoice 1042.";
const INVOICES: Record<string, string> = { "1042": "paid twice on 2026-09-30" };

export type Model = ((prompt: string) => string) & { seen: string[] };
type State = Record<string, any>;
type Checkpoint = [string, State];

/** A stand-in model: it returns the next scripted reply and keeps the prompts it was shown. */
export function scripted(replies: string[]): Model {
  const queue = [...replies];
  const seen: string[] = [];
  const model = ((prompt: string) => {
    seen.push(prompt);
    return queue.shift() as string;
  }) as Model;
  model.seen = seen;
  return model;
}

// 1. Graph style: the programmer fixes the nodes and the edges; the model only fills in a node. State is explicit and checkpointed.

export function runGraph(model: Model, ticket: string, resumeFrom = 0, checkpoints?: Checkpoint[]) {
  const nodes: Record<string, (s: State) => State> = {
    classify: (s) => ({ topic: model(`Classify as billing or other: ${s.ticket}`).trim().toLowerCase() }),
    lookup: (s) => ({ invoice: INVOICES[s.ticket.split("invoice ")[1].replace(/\.+$/, "")] ?? "unknown" }),
    draft: (s) => ({ reply: model(`Write a reply. Topic: ${s.topic}. Invoice: ${s.invoice ?? "none"}`) }),
  };
  const edges: Record<string, (s: State) => string | null> = {
    classify: (s) => (s.topic === "billing" ? "lookup" : "draft"),
    lookup: () => "draft",
    draft: () => null,
  };
  const saved: Checkpoint[] = checkpoints ?? [["classify", { ticket }]];
  let [node, state]: [string | null, State] = saved[resumeFrom];
  saved.length = resumeFrom + 1;
  const path: string[] = [];
  while (node) {
    state = { ...state, ...nodes[node](state) };
    path.push(node);
    node = edges[node](state);
    if (node) saved.push([node, { ...state }]);
  }
  return { path, state, checkpoints: saved };
}

// 2. Model-driven style: the model sees the tools and decides the next step; the loop only executes and stops.

export function runAgent(model: Model, ticket: string, maxSteps = 5): { reply: string | null; trace: string[]; stopped?: string } {
  const tools: Record<string, (arg: string) => string> = { lookup_invoice: (arg) => INVOICES[arg] ?? "unknown" };
  const trace: string[] = [];
  let observation = "";
  for (let i = 0; i < maxSteps; i++) {
    const step = JSON.parse(model(`Ticket: ${ticket}\nTools: ${JSON.stringify(Object.keys(tools).sort()).replace(/"/g, "'").replace(/,/g, ", ")}\nLast result: ${observation}\nReply JSON: a tool call or a final reply.`));
    if ("final" in step) return { reply: step.final, trace };
    observation = tools[step.tool](step.arg);
    trace.push(`${step.tool}(${step.arg}) -> ${observation}`);
  }
  return { reply: null, trace, stopped: "max_steps" };
}

// 3. Typed style: the answer must match a schema; a mismatch goes back to the model as feedback, once.

const SCHEMA: Record<string, "string" | "integer"> = { topic: "string", refund_cents: "integer" };
const KIND_NAME = { string: "str", integer: "int" };

export function validate(raw: string): [Record<string, any> | null, string | null] {
  let data: any;
  try {
    data = JSON.parse(raw);
  } catch {
    return [null, "the reply is not JSON"];
  }
  for (const [field, kind] of Object.entries(SCHEMA)) {
    const ok = kind === "string" ? typeof data?.[field] === "string" : Number.isInteger(data?.[field]);
    if (!ok) return [null, `field ${field} must be ${KIND_NAME[kind]}`];
  }
  return [data, null];
}

export function runTyped(model: Model, ticket: string, retries = 1) {
  let prompt = `Return JSON with topic and refund_cents for: ${ticket}`;
  let attempts = 0;
  for (;;) {
    attempts++;
    const [data, error] = validate(model(prompt));
    if (data) return { data, attempts };
    if (attempts > retries) return { data: null, attempts, error };
    prompt = `${prompt}\nYour last reply was refused: ${error}. Fix it.`;
  }
}

function show(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(show).join(", ")}]`;
  if (value && typeof value === "object") return `{${Object.entries(value).map(([k, v]) => `'${k}': ${show(v)}`).join(", ")}}`;
  return typeof value === "string" ? `'${value}'` : String(value);
}

function main() {
  const m = scripted(["billing", "We refunded the duplicate charge on invoice 1042."]);
  const graph = runGraph(m, TICKET);
  console.log("graph:", graph.path.join(" -> "), "| model calls:", m.seen.length, "| checkpoints:", graph.checkpoints.length);
  const again = scripted(["We refunded the duplicate charge on invoice 1042."]);
  const resumed = runGraph(again, TICKET, 2, [...graph.checkpoints]);
  console.log("graph resumed from checkpoint 2:", resumed.path.join(" -> "), "| model calls:", again.seen.length, "| same reply:", resumed.state.reply === graph.state.reply ? "True" : "False");
  const a = scripted(['{"tool": "lookup_invoice", "arg": "1042"}', '{"final": "Refunded the second payment."}']);
  const agent = runAgent(a, TICKET);
  console.log("agent:", show(agent.trace), "->", agent.reply, "| model calls:", a.seen.length);
  const loop = scripted(Array(3).fill('{"tool": "lookup_invoice", "arg": "1"}'));
  console.log("agent that never finishes:", runAgent(loop, TICKET, 3).stopped, "after", loop.seen.length, "calls");
  const t = scripted(['{"topic": "billing", "refund_cents": "4999"}', '{"topic": "billing", "refund_cents": 4999}']);
  const typed = runTyped(t, TICKET);
  console.log("typed:", show(typed.data), "| attempts:", typed.attempts, "| second prompt ends:", t.seen[1].split("\n").at(-1));
  const bad = runTyped(scripted(["no json", "still no json"]), TICKET);
  console.log("typed, never valid:", bad.error, "after", bad.attempts, "attempts");
}

if (import.meta.main) main();
