// Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("context");
export type Block = Record<string, any>;
export type Message = { role: "user" | "assistant"; content: string | Block[] };
export type Problem = { block: number; citation: number; problem: string };
export type Doc = { title: string; text: string };

const SUMMARY_OPEN = "<summary>\n";
const SUMMARY_CLOSE = "\n</summary>";

const tokens = (text: string) => Math.floor((text.length + 3) / 4);

/** Given: a rough size of a conversation. 4 per message, plus 1 per 4 characters of text and of tool result text, plus 10 per tool call. */
export function countTokens(messages: Message[]): number {
  let total = 0;
  for (const message of messages) {
    total += 4;
    if (typeof message.content === "string") {
      total += tokens(message.content);
      continue;
    }
    for (const block of message.content) {
      if (block.type === "text") total += tokens(block.text);
      else if (block.type === "tool_result") total += tokens(block.content ?? "");
      else if (block.type === "tool_use") total += 10;
    }
  }
  return total;
}

const blocksOf = (message: Message): Block[] => (typeof message.content === "string" ? [{ type: "text", text: message.content }] : structuredClone(message.content));

/** A user message that is not made only of tool results begins a turn. */
function startsTurn(message: Message): boolean {
  if (message.role !== "user") return false;
  return typeof message.content === "string" || message.content.some((b) => b.type !== "tool_result");
}

/** The messages as a list of turns: each turn is a user message that is not a tool result, and everything up to the next one. */
export function splitTurns(messages: Message[]): Message[][] {
  log.debug("splitTurns input", messages);
  // TODO 1 of 7 (unlocks m1, e2, e3): group the messages into turns.
  // Receives the list of messages (`startsTurn(message)` says whether one begins a turn; the first message always does).
  // Returns a list of turns, each a list of messages, in order.
  // Example: [user "q", assistant tool_use, user tool_result, assistant "a", user "q2"] -> [[q, tool_use, tool_result, a], [q2]]
  return [[...messages]];
}

// TODO 2 of 7 (unlocks e1): replace the content of every result but the newest `keep` with the placeholder.
// Receives the tool_result blocks in conversation order, `keep` and the placeholder; changes the blocks in place, returns nothing.
// Example: three results with keep = 2 -> the first one's content becomes "[cleared]".
function clearOldest(results: Block[], keep: number, placeholder: string): void {
}

/** Copy of the conversation in which every tool result but the newest `keep` has its content replaced by the placeholder. */
export function clearToolResults(messages: Message[], keep = 2, exclude: string[] = [], placeholder = "[cleared]"): Message[] {
  const out: Message[] = structuredClone(messages);
  const names = new Map<string, string>();
  for (const m of out) if (Array.isArray(m.content)) for (const b of m.content) if (b.type === "tool_use") names.set(b.id, b.name);
  const results: Block[] = [];
  for (const m of out) if (Array.isArray(m.content)) for (const b of m.content) if (b.type === "tool_result" && !exclude.includes(names.get(b.tool_use_id) ?? "")) results.push(b);
  clearOldest(results, keep, placeholder);
  return out;
}

// TODO 3 of 7 (unlocks e2): the turns of `rest` that remain.
// Receives the pinned turns, the other turns oldest first, and the budget. Drops the oldest turn of `rest` while `countTokens` of
// pinned + rest is over the budget, but never the last one. Returns the remaining turns.
// Example: three turns that are over budget by one turn's size -> the last two.
function trim(pinned: Message[][], rest: Message[][], budget: number): Message[][] {
  return rest;
}

/** Drop the oldest whole turns until the conversation fits; the newest turn always stays. With pin, the first turn stays too. */
export function window(messages: Message[], budget: number, pin = false): Message[] {
  const turns = splitTurns(messages);
  const pinned = pin ? turns.slice(0, 1) : [];
  const rest = trim(pinned, pin ? turns.slice(1) : turns, budget);
  return [...pinned, ...rest].flat();
}

// TODO 4 of 7 (unlocks e3): whether compaction has nothing to do.
// Receives the messages, their turns, the budget and keepTurns. True when the conversation fits the budget or there are no more
// turns than `keepTurns`. Example: a conversation of 40 tokens with budget 50 -> true.
function leaveAlone(messages: Message[], turns: Message[][], budget: number, keepTurns: number): boolean {
  return false;
}

// TODO 5 of 7 (unlocks m1, e4): the first kept message with the summary block placed before its own blocks.
// Receives the kept messages and the summary text. Returns one message with the role of `kept[0]` and as content the text block
// `SUMMARY_OPEN + summary + SUMMARY_CLOSE` followed by `blocksOf(kept[0])`.
// Example: kept[0] = user "q2", summary "S" -> { role: "user", content: [<summary block>, { type: "text", text: "q2" }] }
function withSummary(kept: Message[], summary: string): Message {
  return { role: kept[0].role, content: [] };
}

/** When the conversation is over budget, replace everything before the newest `keepTurns` turns by one summary. */
export function compact(messages: Message[], budget: number, summarise: (older: Message[]) => string, keepTurns = 1): Message[] {
  const turns = splitTurns(messages);
  if (leaveAlone(messages, turns, budget, keepTurns)) return [...messages];
  const older = turns.slice(0, -keepTurns).flat();
  const kept = turns.slice(-keepTurns).flat();
  const summary = summarise(older);
  return [withSummary(kept, summary), ...kept.slice(1)];
}

// TODO 6 of 7 (unlocks e5): the problem of a citation whose document exists, or null when it can be trusted.
// Receives the document text and the citation. Returns "bad_range" (start below 0, end not after start, or end past the text),
// else "text_mismatch" (`text.slice(start, end)` is not the cited text, end excluded), else null.
// Example: text "abcdef", span 1 to 3, cited_text "bc" -> null; cited_text "cd" -> "text_mismatch"
function spanProblem(text: string, cite: Block): string | null {
  return null;
}

/** One problem per citation that cannot be trusted, in order. */
export function verifyCitations(blocks: Block[], documents: Doc[]): Problem[] {
  const problems: Problem[] = [];
  blocks.forEach((block, i) => {
    (block.citations ?? []).forEach((cite: Block, j: number) => {
      let problem: string | null = null;
      if (cite.type !== "char_location") problem = "unsupported_type";
      else if (!(cite.document_index >= 0 && cite.document_index < documents.length)) problem = "unknown_document";
      else problem = spanProblem(documents[cite.document_index].text, cite);
      if (problem) problems.push({ block: i, citation: j, problem });
    });
  });
  return problems;
}

// TODO 7 of 7 (unlocks e6): give a new key the next number; true when the key was new.
// Receives the map of numbers so far and a key. A key not in it gets `numbers.size + 1` and the answer is true; a known key keeps
// its number and the answer is false. Example: empty map and "a" -> true, map is {a: 1}; then "a" again -> false
function numberFor(numbers: Map<string, number>, key: string): boolean {
  return false;
}

/** The answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order. */
export function footnotes(blocks: Block[], documents: Doc[]): string {
  const numbers = new Map<string, number>();
  const sources: string[] = [];
  let out = "";
  for (const block of blocks) {
    out += block.text;
    for (const cite of block.citations ?? []) {
      const key = `${cite.document_index}:${cite.start_char_index}:${cite.end_char_index}`;
      if (numberFor(numbers, key)) {
        sources.push(`[${numbers.get(key)}] ${documents[cite.document_index].title}: "${cite.cited_text}"`);
      }
      out += `[${numbers.get(key)}]`;
    }
  }
  return out + (sources.length ? "\n\nSources:\n" + sources.join("\n") : "");
}
