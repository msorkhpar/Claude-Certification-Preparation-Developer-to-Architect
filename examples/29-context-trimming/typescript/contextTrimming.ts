// Clearing old tool results, asking the API to clear them, and checking the citations in an answer.
// The replies are illustrative, hand-written bodies in the shapes of the context editing and citations pages (claude-sonnet-5-5),
// not captures; the numbers in the context editing response are the documentation's own example.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("context_trimming");

export const MODEL = "claude-sonnet-5-5";
export const POLICY = "The grass is green. The sky is blue. Water is essential for life.";

type Msg = { role: string; content: string | any[] };

/** A rough size: 4 per message, 1 per 4 characters of text, 10 per tool call. */
export function tokens(messages: Msg[]): number {
  let total = 0;
  for (const m of messages) {
    total += 4;
    for (const b of typeof m.content === "string" ? [{ type: "text", text: m.content }] : m.content) {
      total += b.type === "text" ? Math.floor(((b.text ?? "").length + 3) / 4) : b.type === "tool_result" ? Math.floor((b.content.length + 3) / 4) : 10;
    }
  }
  return total;
}

export function conversation(): Msg[] {
  const messages: Msg[] = [{ role: "user", content: "Find every mention of the grass in the logs." }];
  for (let i = 1; i <= 5; i++) {
    messages.push({ role: "assistant", content: [{ type: "tool_use", id: `toolu_${i}`, name: "grep_logs", input: { pattern: `grass-${i}` } }] });
    messages.push({ role: "user", content: [{ type: "tool_result", tool_use_id: `toolu_${i}`, content: `log line ${i}: ` + "x".repeat(400) }] });
  }
  messages.push({ role: "assistant", content: [{ type: "text", text: "Found them all." }] });
  return messages;
}

/** A copy in which every tool result but the newest `keep` has its content replaced; the calls stay. */
export function clearToolResults(messages: Msg[], keep = 2, placeholder = "[cleared]"): Msg[] {
  const out: Msg[] = structuredClone(messages);
  const results = out.flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b) => b.type === "tool_result");
  for (const block of results.slice(0, Math.max(results.length - keep, 0))) block.content = placeholder;
  return out;
}

/** A citation is a claim about where text came from; check it against the document. */
export function verify(blocks: any[], documents: string[]) {
  const bad: Array<{ block: number; citation: number; problem: string }> = [];
  blocks.forEach((block, i) => (block.citations ?? []).forEach((cite: any, j: number) => {
    if (documents[cite.document_index].slice(cite.start_char_index, cite.end_char_index) !== cite.cited_text) bad.push({ block: i, citation: j, problem: "text_mismatch" });
  }));
  return bad;
}

export function footnotes(blocks: any[], titles: string[]): string {
  const numbers = new Map<string, number>();
  const lines: string[] = [];
  let out = "";
  for (const block of blocks) {
    out += block.text;
    for (const cite of block.citations ?? []) {
      const key = `${cite.document_index}:${cite.start_char_index}:${cite.end_char_index}`;
      if (!numbers.has(key)) {
        numbers.set(key, numbers.size + 1);
        lines.push(`[${numbers.get(key)}] ${titles[cite.document_index]}: "${cite.cited_text}"`);
      }
      out += `[${numbers.get(key)}]`;
    }
  }
  return out + (lines.length ? "\n\nSources:\n" + lines.join("\n") : "");
}

export const cite = (start: number, end: number) => ({ type: "char_location", cited_text: POLICY.slice(start, end), document_index: 0, document_title: "Policy", start_char_index: start, end_char_index: end, file_id: null });

export const EDITS = { edits: [{ type: "clear_tool_uses_20250919", trigger: { type: "input_tokens", value: 30000 }, keep: { type: "tool_uses", value: 3 },
  clear_at_least: { type: "input_tokens", value: 5000 }, exclude_tools: ["web_search"] }] };

const usage = { input_tokens: 1, output_tokens: 1 };
export const editingReply = () => ({ body: { ...message([text("Found them all.")], "end_turn", usage, MODEL), context_management: { applied_edits: [{ type: "clear_tool_uses_20250919", cleared_tool_uses: 8, cleared_input_tokens: 50000 }] } } });
export const citedBlocks = () => [{ type: "text", text: "The grass is green. ", citations: [cite(0, 19)] }, { type: "text", text: "Water matters. ", citations: [cite(37, 65)] }, { type: "text", text: "Green again.", citations: [cite(0, 19)] }];
export const citedReply = () => ({ body: message(citedBlocks(), "end_turn", usage, MODEL) });

export function clientFor(replies: Array<Record<string, unknown>>) {
  const fake = scriptedFetch(replies as any);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch }) };
}

const py = (v: any): string => {
  if (v === null || v === undefined) return "None";
  if (typeof v === "boolean") return v ? "True" : "False";
  if (typeof v === "string") return `'${v}'`;
  if (Array.isArray(v)) return `[${v.map(py).join(", ")}]`;
  if (typeof v === "object") return `{${Object.entries(v).map(([k, x]) => `${py(k)}: ${py(x)}`).join(", ")}}`;
  return String(v);
};

async function main() {
  const before = conversation();
  const after = clearToolResults(before, 2);
  const cleared = after.flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b) => b.type === "tool_result" && b.content === "[cleared]").length;
  const same = JSON.stringify(after.filter((m) => m.role === "assistant")) === JSON.stringify(before.filter((m) => m.role === "assistant"));
  console.log(`conversation: ${before.length} messages, 5 tool results, about ${tokens(before)} tokens`);
  console.log(`after clearing all but the newest 2 results: about ${tokens(after)} tokens, ${cleared} results replaced, calls kept: ${py(same)}`);
  const { fake, client } = clientFor([editingReply(), citedReply()]);
  const reply: any = await client.beta.messages.create({ model: MODEL, max_tokens: 300, messages: before as any, betas: ["context-management-2025-06-27"], context_management: EDITS as any });
  console.log("beta header sent:", fake.seen[0].headers["anthropic-beta"]);
  const edit = fake.seen[0].body.context_management.edits[0];
  console.log("edit sent:", edit.type, "trigger", edit.trigger.value, "keep", edit.keep.value, "exclude", py(edit.exclude_tools));
  const applied = reply.context_management.applied_edits[0];
  console.log("applied edit reported:", applied.type, `cleared ${applied.cleared_tool_uses} tool uses, ${applied.cleared_input_tokens} input tokens`);
  const document = { type: "document" as const, source: { type: "text" as const, media_type: "text/plain" as const, data: POLICY }, title: "Policy", citations: { enabled: true } };
  const answer = await client.messages.create({ model: MODEL, max_tokens: 300, messages: [{ role: "user", content: [document, { type: "text", text: "What does the policy say about grass and water?" }] }] });
  const blocks = answer.content as any[];
  console.log("citations enabled in the request:", py(fake.seen[1].body.messages[0].content[0].citations));
  console.log("citation problems:", py(verify(blocks, [POLICY])));
  const tampered = structuredClone(blocks);
  tampered[1].citations[0].cited_text = "Water is optional.";
  console.log("after tampering with one cited_text:", py(verify(tampered, [POLICY])));
  console.log(footnotes(blocks, ["Policy"]));
}

if (import.meta.main) await main();
