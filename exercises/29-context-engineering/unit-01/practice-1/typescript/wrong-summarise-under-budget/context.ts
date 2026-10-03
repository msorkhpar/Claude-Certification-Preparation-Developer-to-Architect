// Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md.
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
  const turns: Message[][] = [];
  for (const message of messages) {
    if (startsTurn(message) || turns.length === 0) turns.push([]);
    turns[turns.length - 1].push(message);
  }
  return turns;
}

/** Copy of the conversation in which every tool result but the newest `keep` has its content replaced by the placeholder. */
export function clearToolResults(messages: Message[], keep = 2, exclude: string[] = [], placeholder = "[cleared]"): Message[] {
  const out: Message[] = structuredClone(messages);
  const names = new Map<string, string>();
  for (const m of out) if (Array.isArray(m.content)) for (const b of m.content) if (b.type === "tool_use") names.set(b.id, b.name);
  const results: Block[] = [];
  for (const m of out) if (Array.isArray(m.content)) for (const b of m.content) if (b.type === "tool_result" && !exclude.includes(names.get(b.tool_use_id) ?? "")) results.push(b);
  for (const b of results.slice(0, Math.max(results.length - keep, 0))) b.content = placeholder;
  return out;
}

/** Drop the oldest whole turns until the conversation fits; the newest turn always stays. With pin, the first turn stays too. */
export function window(messages: Message[], budget: number, pin = false): Message[] {
  const turns = splitTurns(messages);
  const pinned = pin ? turns.slice(0, 1) : [];
  let rest = pin ? turns.slice(1) : turns;
  while (rest.length > 1 && countTokens([...pinned, ...rest].flat()) > budget) rest = rest.slice(1);
  return [...pinned, ...rest].flat();
}

/** When the conversation is over budget, replace everything before the newest `keepTurns` turns by one summary. */
export function compact(messages: Message[], budget: number, summarise: (older: Message[]) => string, keepTurns = 1): Message[] {
  const turns = splitTurns(messages);
  if (turns.length <= keepTurns) return [...messages];
  const older = turns.slice(0, -keepTurns).flat();
  const kept = turns.slice(-keepTurns).flat();
  const summary = summarise(older);
  const first: Message = { role: kept[0].role, content: [{ type: "text", text: `${SUMMARY_OPEN}${summary}${SUMMARY_CLOSE}` }, ...blocksOf(kept[0])] };
  return [first, ...kept.slice(1)];
}

/** One problem per citation that cannot be trusted, in order. */
export function verifyCitations(blocks: Block[], documents: Doc[]): Problem[] {
  const problems: Problem[] = [];
  blocks.forEach((block, i) => {
    (block.citations ?? []).forEach((cite: Block, j: number) => {
      let problem: string | null = null;
      if (cite.type !== "char_location") problem = "unsupported_type";
      else if (!(cite.document_index >= 0 && cite.document_index < documents.length)) problem = "unknown_document";
      else {
        const text = documents[cite.document_index].text;
        const start = cite.start_char_index;
        const end = cite.end_char_index;
        if (start < 0 || end <= start || end > text.length) problem = "bad_range";
        else if (text.slice(start, end) !== cite.cited_text) problem = "text_mismatch";
      }
      if (problem) problems.push({ block: i, citation: j, problem });
    });
  });
  return problems;
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
      if (!numbers.has(key)) {
        numbers.set(key, numbers.size + 1);
        sources.push(`[${numbers.get(key)}] ${documents[cite.document_index].title}: "${cite.cited_text}"`);
      }
      out += `[${numbers.get(key)}]`;
    }
  }
  return out + (sources.length ? "\n\nSources:\n" + sources.join("\n") : "");
}
