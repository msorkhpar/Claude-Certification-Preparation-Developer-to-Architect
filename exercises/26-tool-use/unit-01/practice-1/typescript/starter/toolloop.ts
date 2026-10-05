// A tool loop against a scripted model. See ../../statement.md.
import { logger } from "../logger.ts";
const log = logger("toolloop");
export type Block = Record<string, any>;
export type Tool = { name: string; description: string; input_schema: Record<string, any>; handler: (input: Record<string, any>) => unknown };
export type Reply = { content: Block[]; stop_reason: string };
export type Ask = (request: Record<string, any>) => Reply;
export type Outcome = { status: "done" | "refused" | "truncated" | "max_turns"; text: string; turns: number; messages: any[] };

const FORCED_UNSUPPORTED = new Set(["claude-opus-5-5", "claude-sonnet-5-5", "claude-fable-5-1", "claude-mythos-5-1"]);
const CHOICE_TYPES = new Set(["auto", "any", "tool", "none"]);

/** A tool refuses or fails; its message goes back to the model as an error result. */
export class ToolError extends Error {}

/** The request would be rejected with a 400. `field` names the offending part. */
export class RequestError extends Error {
  field: string;
  reason: string;
  constructor(field: string, reason: string) {
    super(`${field}: ${reason}`);
    this.field = field;
    this.reason = reason;
  }
}

/** Refuse a tool_choice the API would reject. */
function checkChoice(tools: Tool[], model: string, choice: Record<string, any>): void {
  // TODO 1 of 7 (finish this to pass e5): refuse a tool_choice the API would reject.
  // Receives the tools, the model id and the tool_choice map. Throws RequestError, in this order: field "tool_choice.type" unless the type is in
  // CHOICE_TYPES; "tool_choice.disable_parallel_tool_use" when that key is present and not a boolean; "tool_choice" for type "any" or "tool" on a
  // model in FORCED_UNSUPPORTED; "tool_choice.name" for type "tool" when no tool has that name. Returns nothing otherwise.
  // Example: checkChoice([], "claude-opus-5-5", { type: "any" }) throws a RequestError with field "tool_choice"
}

/** The keys listed in the tool's required list that the input lacks. */
function missingInputs(tool: Tool, input: Record<string, any>): string[] {
  // TODO 2 of 7 (finish this to pass e2): the required keys a call leaves out.
  // Receives a tool and the input map of the call. Returns the keys in the tool's input_schema `required` list that the input lacks, in the
  // order of that list; an empty array when none is missing.
  // Example: missingInputs({ ...tool, input_schema: { required: ["city"] } }, {}) -> ["city"]
  return [];
}

/** The content of a tool result: a string as it is, any other value as JSON text. */
function resultContent(out: unknown): string {
  // TODO 3 of 7 (finish this to pass e6): the content of a tool_result.
  // Receives what a handler returned. Returns a string as it is and any other value as JSON text (use JSON.stringify).
  // Example: resultContent({ city: "Oslo" }) -> '{"city":"Oslo"}'
  return out as string;
}

function runOne(tools: Tool[], block: Block): Block {
  const result: Block = { type: "tool_result", tool_use_id: block.id };
  const tool = tools.find((t) => t.name === block.name);
  if (!tool) return { ...result, content: `Unknown tool: ${block.name}`, is_error: true };
  const missing = missingInputs(tool, block.input);
  if (missing.length > 0) return { ...result, content: `Missing required input: ${missing.join(", ")}`, is_error: true };
  try {
    const out = tool.handler(block.input);
    return { ...result, content: resultContent(out) };
  } catch (err) {
    return { ...result, content: (err as Error).message, is_error: true };
  }
}

const textOf = (content: Block[]) => content.filter((b) => b.type === "text").map((b) => b.text).join("");

/** One tool_result per tool_use block of the reply, in order. */
function toolResults(tools: Tool[], content: Block[]): Block[] {
  // TODO 4 of 7 (finish this to pass m1 and e1): run every tool call of a reply.
  // Receives the tools and the content blocks of the reply. Returns one tool_result (from runOne) per block of type "tool_use", in the
  // order of the blocks; blocks of any other type, such as "server_tool_use", get no result.
  // Example: [a text block, a tool_use block] -> an array with one tool_result
  return [];
}

/** The status of a reply that ends the loop: done, refused or truncated. */
function finalStatus(stop: string): "done" | "refused" | "truncated" {
  // TODO 5 of 7 (finish this to pass e4): the status of a reply that ends the loop.
  // Receives the stop_reason. Returns "done" for "end_turn" and "stop_sequence", "refused" for "refusal" and "truncated" for any other.
  // Example: finalStatus("max_tokens") -> "truncated"
  return "done";
}

/** The last turn number the loop may use. */
function lastTurn(maxTurns: number): number {
  // TODO 6 of 7 (finish this to pass e3): the last turn number the loop may use.
  // Receives maxTurns. Returns the highest turn number, so that at most maxTurns calls are made.
  // Example: lastTurn(3) -> 3
  return 1;
}

/** The tool_choice to send on this turn: a forced choice only on the first request. */
function sentChoice(toolChoice: Record<string, any>, turn: number): Record<string, any> {
  // TODO 7 of 7 (finish this to pass e5): the tool_choice to send on a turn.
  // Receives the tool_choice map and the turn number (1 for the first request). A forced choice (type "any" or "tool") is sent as given on
  // turn 1 and as { type: "auto" } from turn 2 on; "auto" and "none" are sent unchanged every turn.
  // Example: sentChoice({ type: "any" }, 2) -> { type: "auto" }
  return toolChoice;
}

/** Call the model, run every tool it asks for, send the results back, until it ends its turn or a limit is hit. */
export function runAgent(ask: Ask, tools: Tool[], userText: string, model = "claude-sonnet-5-5", maxTurns = 8, toolChoice?: Record<string, any>): Outcome {
  log.debug("runAgent input", userText);
  if (toolChoice !== undefined) checkChoice(tools, model, toolChoice);
  const definitions = tools.map(({ handler, ...definition }) => definition);
  const messages: any[] = [{ role: "user", content: userText }];
  let text = "";
  let calls = 0;
  for (let turn = 1; turn <= lastTurn(maxTurns); turn++) {
    const request: Record<string, any> = { model, max_tokens: 1024, messages: [...messages], tools: definitions };
    if (toolChoice !== undefined) request.tool_choice = sentChoice(toolChoice, turn);
    const reply = ask(request);
    calls = turn;
    messages.push({ role: "assistant", content: reply.content });
    text = textOf(reply.content);
    const stop = reply.stop_reason;
    if (stop === "tool_use") {
      messages.push({ role: "user", content: toolResults(tools, reply.content) });
    } else if (stop === "pause_turn") {
      continue;
    } else {
      return { status: finalStatus(stop), text, turns: calls, messages };
    }
  }
  return { status: "max_turns", text, turns: calls, messages };
}
