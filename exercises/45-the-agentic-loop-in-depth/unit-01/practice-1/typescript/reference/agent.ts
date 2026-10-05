// The agent loop, driven by the stop reason. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("agent");
type Block = Record<string, any>;
type Model = (messages: any[]) => { stop_reason: string; content: Block[] };
type Tools = Record<string, (input: any) => string>;

function textOf(content: Block[]): string {
  return content.filter((b) => b.type === "text").map((b) => b.text ?? "").join("");
}

function callsOf(content: Block[]): Block[] {
  return content.filter((b) => b.type === "tool_use");
}

function toolResult(block: Block, content: string, isError = false): Block {
  return isError ? { type: "tool_result", tool_use_id: block.id, content, is_error: true } : { type: "tool_result", tool_use_id: block.id, content };
}

function statusFor(reason: string, calls: Block[]): string {
  if (reason === "tool_use" && calls.length === 0) return "malformed";
  if (reason === "end_turn" || reason === "stop_sequence") return "done";
  if (reason === "max_tokens") return "truncated";
  if (reason === "refusal") return "refused";
  return "unexpected";
}

function atLimit(turns: number, maxTurns: number): boolean {
  return turns >= maxTurns;
}

function snapshot(messages: any[]): any[] {
  return [...messages];
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
