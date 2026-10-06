// The agent loop, driven by the stop reason. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("agent");
type Block = Record<string, any>;
type Model = (messages: any[]) => { stop_reason: string; content: Block[] };
type Tools = Record<string, (input: any) => string>;

// TODO 1 of 6 (unlocks m1 and e5): the text of a reply.
// Receives a reply's content list of blocks. Returns the `text` of every block whose type is "text", joined with nothing between.
// Example: textOf([{ type: "text", text: "Hi " }, { type: "tool_use", id: "t" }, { type: "text", text: "there" }]) -> "Hi there"
function textOf(content: Block[]): string {
  return "";
}

// TODO 2 of 6 (unlocks e2 and e6): the tool calls of a reply.
// Receives a reply's content list. Returns its `tool_use` blocks, in order; an empty array when there are none.
// Example: callsOf([{ type: "text", text: "x" }, { type: "tool_use", id: "a" }]) -> [{ type: "tool_use", id: "a" }]
function callsOf(content: Block[]): Block[] {
  return [];
}

// TODO 3 of 6 (unlocks m1, e2 and e3): one tool_result block.
// Receives the tool_use block, the result text and a flag. Returns { type: "tool_result", tool_use_id: <the block's id>, content: <the text> },
// with is_error: true added only when the flag is set (a good result has no is_error key).
// Example: toolResult({ id: "t1" }, "boom", true) -> { type: "tool_result", tool_use_id: "t1", content: "boom", is_error: true }
function toolResult(block: Block, content: string, isError = false): Block {
  return {};
}

// TODO 4 of 6 (unlocks m1, e5 and e6): the status a stop reason ends the run with (the loop only gets here when it cannot go on).
// Receives the stop reason and the tool calls of the reply. Returns "malformed" for tool_use with no calls, "done" for end_turn and stop_sequence,
// "truncated" for max_tokens, "refused" for refusal and "unexpected" for anything else.
// Example: statusFor("max_tokens", []) -> "truncated", statusFor("tool_use", []) -> "malformed", statusFor("brand_new", []) -> "unexpected"
function statusFor(reason: string, calls: Block[]): string {
  return "";
}

// TODO 5 of 6 (unlocks e4): has the turn limit been reached before the next model call?
// Receives the model calls made so far and the limit. Returns true when no call is left. Example: atLimit(3, 3) -> true, atLimit(2, 3) -> false
function atLimit(turns: number, maxTurns: number): boolean {
  return false;
}

// TODO 6 of 6 (unlocks m1): the copy of the messages that the model is handed.
// Receives the list of messages so far. Returns a new array with the same items, so later changes do not rewrite what the model saw.
// Example: snapshot([a, b]) -> a new array [a, b]
function snapshot(messages: any[]): any[] {
  return messages;
}

function runTool(block: Block, tools: Tools): Block {
  const handler = tools[block.name];
  if (handler === undefined) return toolResult(block, `Unknown tool: ${block.name}`, true);
  try {
    return toolResult(block, handler(block.input ?? {}));
  } catch (error) { // a failing tool is a result for the model, not a crash of the loop
    return toolResult(block, error instanceof Error ? error.message : String(error), true);
  }
}

export function runAgent(model: Model, tools: Tools, task: string, maxTurns = 8): any {
  log.debug("runAgent input", task);
  const messages: any[] = [{ role: "user", content: task }];
  let turns = 0;
  let lastText = "";
  for (;;) {
    if (atLimit(turns, maxTurns)) return { status: "max_turns", text: lastText, turns, messages }; // the count is a backstop: it only ends a run the model has not ended itself
    turns += 1;
    const reply = model(snapshot(messages));
    const content = reply.content;
    lastText = textOf(content);
    messages.push({ role: "assistant", content });
    const reason = reply.stop_reason;
    const calls = callsOf(content);
    if (reason === "tool_use" && calls.length > 0) {
      messages.push({ role: "user", content: calls.map((b) => runTool(b, tools)) });
    } else {
      return { status: statusFor(reason, calls), text: lastText, turns, messages };
    }
  }
}
