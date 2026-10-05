// What a loop does with seven failed or odd tool calls: a structured result for each, bounded retries, and the next action.
//
// The tools are scripted functions, so the output shows the control flow and the text the model would be given, and nothing about how a model would
// answer. The refund service, the orders and the limits are illustrative.
import { logger } from "./logger.ts";
const log = logger("error_flow");

export class ToolError extends Error {
  kind: string;
  retryAfterMs: number | null;
  explanation: string | null;
  constructor(kind: string, message: string, retryAfterMs: number | null = null, explanation: string | null = null) {
    super(message);
    this.kind = kind;
    this.retryAfterMs = retryAfterMs;
    this.explanation = explanation;
  }
}

const RETRYABLE: Record<string, string> = { transient: "yes", validation: "no", permission: "no", business: "no", outcome_unknown: "no", internal: "no" };
const ACTION: Record<string, string> = { transient: "retry later", validation: "repair the input", permission: "escalate", business: "explain to the customer", outcome_unknown: "check the state first", internal: "escalate" };

type Result = { is_error: boolean; kind?: string; content: any; attempts: number; empty?: boolean };
type Tool = ((args: Record<string, any>) => any) & { state: { n: number; seen: Array<string | null> } };

function failure(kind: string, message: string, attempts: number, explanation: string | null = null): Result {
  let text = `${kind} error (retryable: ${RETRYABLE[kind]}): ${message}`;
  if (explanation) text += ` Tell the customer: ${explanation}`;
  return { is_error: true, kind, content: text, attempts };
}

/** Retry what is safe to retry, give up with a structured error on the rest. Returns the result and the waits. */
export function run(tool: Tool, args: Record<string, any>, options: { key?: string; read_only?: boolean } = {}): [Result, number[]] {
  const waits: number[] = [];
  const maxRetries = 2;
  const baseMs = 100;
  let attempts = 0;
  while (true) {
    attempts++;
    const call = options.key ? { ...args, idempotency_key: options.key } : { ...args };
    try {
      const value = tool(call);
      return [{ is_error: false, content: value, attempts, empty: value === "" || value === null || (Array.isArray(value) && value.length === 0) }, waits];
    } catch (error: any) {
      if (!(error instanceof ToolError)) return [failure("internal", `unexpected failure in the tool: ${error.message}`, attempts), waits];
      let kind = error.kind;
      if (kind === "timeout") {
        if (!(options.key || options.read_only)) return [failure("outcome_unknown", `${error.message} The call may have taken effect: check the current state before trying again.`, attempts), waits];
        kind = "transient";
      }
      if (kind !== "transient") return [failure(kind, error.message, attempts, error.explanation), waits];
      if (attempts > maxRetries) return [failure("transient", `${error.message} Gave up after ${attempts} attempts.`, attempts), waits];
      waits.push(error.retryAfterMs !== null ? error.retryAfterMs : baseMs * 2 ** (attempts - 1));
    }
  }
}

export function nextStep(result: Result): string {
  if (!result.is_error) return result.empty ? "accept the empty result" : "continue";
  return ACTION[result.kind as string];
}

/** A tool that does what the script says, one entry per call; the last entry repeats. */
export function scripted(...steps: any[]): Tool {
  const state = { n: 0, seen: [] as Array<string | null> };
  const tool = ((args: Record<string, any>) => {
    state.seen.push(args.idempotency_key ?? null);
    const step = steps[Math.min(state.n, steps.length - 1)];
    state.n++;
    if (step instanceof Error) throw step;
    return step;
  }) as Tool;
  tool.state = state;
  return tool;
}

export function scenarios(): Array<[string, Tool, { key?: string; read_only?: boolean }]> {
  return [
    ["get_order order=A-7", scripted(new ToolError("transient", "The order service is busy."), new ToolError("transient", "The order service is busy."), "order A-7: 2 items"), {}],
    ["process_refund amount=-5", scripted(new ToolError("validation", "amount must be a positive whole number, for example 40")), {}],
    ["process_refund amount=900", scripted(new ToolError("business", "Refunds above 500 need a person.", null, "A colleague will contact you about this refund.")), {}],
    ["process_refund amount=40, no key", scripted(new ToolError("timeout", "No answer from the refund service.")), {}],
    ["process_refund amount=40, key refund-A-7-1", scripted(new ToolError("timeout", "No answer from the refund service."), "refund R-1 created"), { key: "refund-A-7-1" }],
    ["list_orders customer=C-9", scripted([]), { read_only: true }],
    ["process_refund amount=40, tool bug", scripted(new Error("the currency table is missing")), {}],
  ];
}

function main() {
  scenarios().forEach(([title, tool, options], index) => {
    const [result, waits] = run(tool, { order: "A-7" }, options);
    console.log(`${index + 1}. ${title}`);
    console.log(`   attempts ${result.attempts}, waits [${waits.join(", ")}], keys sent [${tool.state.seen.map((k) => (k === null ? "None" : `'${k}'`)).join(", ")}]`);
    console.log(`   tool_result is_error=${result.is_error ? "true" : "false"}: ${pyRepr(result.content)}`);
    console.log(`   next: ${nextStep(result)}`);
  });
}

/** The text in single quotes as Python prints it; content here is always a string or an empty list. */
function pyRepr(value: any): string {
  if (Array.isArray(value)) return "[]";
  return `'${String(value).replaceAll("\\", "\\\\").replaceAll("'", "\\'")}'`;
}

if (import.meta.main) main();
