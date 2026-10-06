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
  if (!CHOICE_TYPES.has(choice.type)) throw new RequestError("tool_choice.type", "must be auto, any, tool or none");
  if ("disable_parallel_tool_use" in choice && typeof choice.disable_parallel_tool_use !== "boolean") throw new RequestError("tool_choice.disable_parallel_tool_use", "must be a boolean");
  if ((choice.type === "any" || choice.type === "tool") && FORCED_UNSUPPORTED.has(model)) throw new RequestError("tool_choice", `${model} does not support forced tool use`);
  if (choice.type === "tool" && !tools.some((t) => t.name === choice.name)) throw new RequestError("tool_choice.name", "names no tool in the request");
}

/** The keys listed in the tool's required list that the input lacks. */
function missingInputs(tool: Tool, input: Record<string, any>): string[] {
  return (tool.input_schema.required ?? []).filter((k: string) => !(k in input));
}

/** The content of a tool result: a string as it is, any other value as JSON text. */
function resultContent(out: unknown): string {
  return typeof out === "string" ? out : JSON.stringify(out);
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
  return content.filter((b) => b.type === "tool_use").map((b) => runOne(tools, b));
}

/** The status of a reply that ends the loop: done, refused or truncated. */
function finalStatus(stop: string): "done" | "refused" | "truncated" {
  if (stop === "end_turn" || stop === "stop_sequence") return "done";
  if (stop === "refusal") return "refused";
  return "truncated"; // max_tokens, model_context_window_exceeded: the answer is cut off
}

/** The last turn number the loop may use. */
function lastTurn(maxTurns: number): number {
  return maxTurns;
}

/** The tool_choice to send on this turn: a forced choice only on the first request. */
function sentChoice(toolChoice: Record<string, any>, turn: number): Record<string, any> {
  const forced = toolChoice.type === "any" || toolChoice.type === "tool";
  return forced && turn > 1 ? { type: "auto" } : toolChoice;
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
