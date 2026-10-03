// Keeping a conversation inside its budget, and checking the citations in an answer. See ../../statement.md.
export type Block = Record<string, any>;
export type Message = { role: "user" | "assistant"; content: string | Block[] };
export type Problem = { block: number; citation: number; problem: string };
export type Doc = { title: string; text: string };

export const SUMMARY_OPEN = "<summary>\n";
export const SUMMARY_CLOSE = "\n</summary>";

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

/** The messages as a list of turns: a turn is a user message that is not only tool results, and everything up to the next one. */
export function splitTurns(messages: Message[]): Message[][] {
  // TODO
  return undefined as unknown as Message[][];
}

export function clearToolResults(messages: Message[], keep = 2, exclude: string[] = [], placeholder = "[cleared]"): Message[] {
  // TODO: a copy in which every tool result but the newest `keep` has its content replaced by the placeholder.
  return undefined as unknown as Message[];
}

export function window(messages: Message[], budget: number, pin = false): Message[] {
  // TODO: drop the oldest whole turns until the conversation fits; the newest turn always stays; with pin the first stays too.
  return undefined as unknown as Message[];
}

export function compact(messages: Message[], budget: number, summarise: (older: Message[]) => string, keepTurns = 1): Message[] {
  // TODO: over budget, replace everything before the newest `keepTurns` turns by one summary block.
  return undefined as unknown as Message[];
}

export function verifyCitations(blocks: Block[], documents: Doc[]): Problem[] {
  // TODO: one { block, citation, problem } per citation that cannot be trusted, in order.
  return undefined as unknown as Problem[];
}

export function footnotes(blocks: Block[], documents: Doc[]): string {
  // TODO: the answer text with a [n] after each cited block and a Sources list; one number per distinct cited span, in order.
  return undefined as unknown as string;
}
