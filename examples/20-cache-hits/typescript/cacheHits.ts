// Prompt caching seen through the usage object, against a scripted server that applies the documented prefix rule.
// The server (CacheSim) is an illustrative, hand-written stand-in, not a capture: it counts a token as four characters,
// caches the prefix up to a block that carries cache_control when that prefix reaches the minimum size, keeps it for five
// minutes from its last use, and reports `cache_creation_input_tokens`, `cache_read_input_tokens` and `input_tokens` (the
// tokens after the last breakpoint) as the prompt caching page describes them (claude-sonnet-5-5, minimum 512 tokens).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";

export const MODEL = "claude-sonnet-5-5";
export const POLICY = "Refund policy clause: items may be returned within 14 days. ".repeat(40); // about 600 tokens
const MARK = { type: "ephemeral" };

export const tokens = (piece: string) => Math.ceil(piece.length / 4);

type Piece = [string, unknown];

function blocksOf(body: any): Piece[] {
  const out: Piece[] = (body.tools ?? []).map((t: any): Piece => [JSON.stringify(t), t.cache_control]);
  const system = typeof body.system === "string" ? [{ text: body.system }] : (body.system ?? []);
  for (const b of system) out.push([b.text, b.cache_control]);
  for (const m of body.messages) {
    const content = Array.isArray(m.content) ? m.content : [{ text: m.content }];
    for (const b of content) out.push([`${m.role}: ${b.text}`, b.cache_control]);
  }
  return out;
}

export class CacheSim {
  clock = 0;
  entries = new Map<string, number>();
  minimum: number;
  ttl: number;
  constructor(minimum = 512, ttl = 300) {
    this.minimum = minimum;
    this.ttl = ttl;
  }

  reply(body: any) {
    const blocks = blocksOf(body);
    const marks = blocks.flatMap(([, mark], i) => (mark ? [i] : []));
    const key = (i: number) => blocks.slice(0, i + 1).map(([t]) => t).join("\0");
    const size = (i: number) => blocks.slice(0, i + 1).reduce((n, [t]) => n + tokens(t), 0);
    let read = 0;
    let written = 0;
    let hit: number | null = null;
    for (const i of [...marks].reverse()) {
      if ((this.entries.get(key(i)) ?? -1) > this.clock) {
        read = size(i);
        hit = i;
        this.entries.set(key(i), this.clock + this.ttl); // a hit refreshes the entry
        break;
      }
    }
    const last = marks.length ? marks[marks.length - 1] : null;
    if (last !== null && hit !== last && size(last) >= this.minimum) {
      written = size(last) - read;
      this.entries.set(key(last), this.clock + this.ttl);
    }
    const fresh = size(blocks.length - 1) - read - written;
    const usage = {
      input_tokens: fresh, output_tokens: 20, cache_read_input_tokens: read, cache_creation_input_tokens: written,
      cache_creation: { ephemeral_5m_input_tokens: written, ephemeral_1h_input_tokens: 0 },
    };
    return { body: message([text("ok")], "end_turn", usage as any) };
  }
}

const request = (system: unknown[], question: string) => ({ model: MODEL, max_tokens: 50, system, messages: [{ role: "user" as const, content: question }] });
const policyBlock = { type: "text", text: POLICY, cache_control: MARK };

export const stable = (question: string) => request([policyBlock], question);
export const stampFirst = (label: string, question: string) => request([{ type: "text", text: `Current time: ${label}` }, policyBlock], question);
export const stampLast = (label: string, question: string) => request([policyBlock, { type: "text", text: `Current time: ${label}` }], question);

export async function usageOf(sim: CacheSim, body: any, wait = 0): Promise<any> {
  sim.clock += wait;
  const fake = scriptedFetch([(sent: any) => sim.reply(sent)]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  return (await client.messages.create(body)).usage;
}

async function run(sim: CacheSim, label: string, body: any, wait = 0) {
  const u = await usageOf(sim, body, wait);
  console.log(`${label.padEnd(34)} write ${String(u.cache_creation_input_tokens).padStart(4)}  read ${String(u.cache_read_input_tokens).padStart(4)}  fresh ${String(u.input_tokens).padStart(4)}`);
}

async function main() {
  let sim = new CacheSim();
  await run(sim, "1 stable system, first call", stable("Can I return a lamp?"));
  await run(sim, "2 same system, new question", stable("Can I return a chair?"), 60);
  await run(sim, "3 six minutes of silence", stable("Can I return a desk?"), 360);
  sim = new CacheSim();
  await run(sim, "4 timestamp first, 10:01", stampFirst("10:01", "Can I return a lamp?"));
  await run(sim, "5 timestamp first, 10:02", stampFirst("10:02", "Can I return a lamp?"), 60);
  await run(sim, "6 timestamp last, 10:03", stampLast("10:03", "Can I return a lamp?"), 60);
  await run(sim, "7 timestamp last, 10:04", stampLast("10:04", "Can I return a lamp?"), 60);
}

if (import.meta.main) await main();
