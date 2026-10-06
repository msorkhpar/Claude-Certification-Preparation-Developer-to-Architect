// Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract.
import { logger } from "./logger.ts";
const log = logger("params");

const EFFORTS = ["low", "medium", "high", "xhigh", "max"];
const FAST_BETA = "fast-mode-2026-02-01";
const DEFAULT_EFFORT: Record<string, string> = { "claude-fable-5-1": "high", "claude-opus-5-5": "medium", "claude-sonnet-5-5": "high" };

/** The API would answer 400. `param` names the offending parameter. */
export class RejectedRequest extends Error {
  param: string;
  reason: string;
  constructor(param: string, reason: string) {
    super(`${param}: ${reason}`);
    this.param = param;
    this.reason = reason;
  }
}

function family(model: string): string {
  if (model.startsWith("claude-haiku-4-5")) return "claude-haiku-4-5";
  if (model in DEFAULT_EFFORT) return model;
  throw new RejectedRequest("model", `unknown model ${model}`);
}

function checkEffort(haiku: boolean, effort: string): void {
  // TODO 1 of 7 (finish this to pass e3): refuse an effort level the model cannot take.
  // Receives whether the model is Haiku and the effort given. Throws RejectedRequest("output_config.effort", ...) for Haiku (it has no effort) and for a level that is not in EFFORTS.
  // Example: checkEffort(true, "low") throws, checkEffort(false, "adaptive") throws, checkEffort(false, "high") returns
}

function checkMode(haiku: boolean, kind: string): void {
  // TODO 2 of 7 (finish this to pass e1): refuse a thinking mode the model does not have.
  // Receives whether the model is Haiku and the thinking type. Throws RejectedRequest("thinking", ...) for "adaptive" on Haiku, and for "enabled" or "disabled" on any model that is not Haiku.
  // Example: checkMode(false, "disabled") throws, checkMode(true, "disabled") returns
}

function checkBudget(budget: number | undefined | null, maxTokens: number): void {
  // TODO 3 of 7 (finish this to pass e6): refuse a manual thinking budget that does not fit.
  // Receives the budget (undefined or null when missing) and maxTokens. Throws RejectedRequest("thinking.budget_tokens", ...) when the budget is missing, below 1024 or not below maxTokens.
  // Example: checkBudget(1023, 4096) throws, checkBudget(2048, 2048) throws, checkBudget(1024, 2048) returns
}

function checkBetweenTools(fam: string, effort: string | undefined | null): void {
  // TODO 4 of 7 (finish this to pass e2): refuse between_tools where it does not work.
  // Receives the model family and the effort given (undefined or null when absent). Throws RejectedRequest("thinking", ...) unless the family is "claude-sonnet-5-5", and when the
  // effective effort (the one given, else DEFAULT_EFFORT[fam]) is "xhigh" or "max".
  // Example: checkBetweenTools("claude-sonnet-5-5", "max") throws, checkBetweenTools("claude-sonnet-5-5", undefined) returns
}

function thinkingObject(kind: string, budget: number | undefined | null): Record<string, any> {
  // TODO 5 of 7 (finish this to pass m1 and e7): the `thinking` value of the request.
  // Receives the thinking type and the budget (undefined unless "enabled"). Returns { type: kind }, plus budget_tokens: budget for "enabled". Effort never goes in here.
  // Example: thinkingObject("adaptive", undefined) -> { type: "adaptive" }, thinkingObject("enabled", 2048) -> { type: "enabled", budget_tokens: 2048 }
  return {};
}

function samplingAllowed(haiku: boolean, name: string, value: number): boolean {
  // TODO 6 of 7 (finish this to pass e4): whether the model accepts this sampling parameter.
  // Receives whether the model is Haiku, the parameter name (temperature, top_p or top_k) and its value. Returns true for Haiku, and for the others only a temperature of exactly 1.0.
  // Example: samplingAllowed(false, "temperature", 0.2) -> false, samplingAllowed(true, "top_k", 40) -> true
  return true;
}

function fastParams(fam: string, batch: unknown): Record<string, any> {
  // TODO 7 of 7 (finish this to pass e5): the parameters fast mode adds, or a refusal.
  // Receives the model family and whether the request goes into a batch. Throws RejectedRequest("speed", ...) unless the family is "claude-opus-5-5", and when `batch` is truthy.
  // Otherwise returns { speed: "fast", betas: [FAST_BETA] }.
  // Example: fastParams("claude-opus-5-5", false) -> { speed: "fast", betas: ["fast-mode-2026-02-01"] }
  return {};
}

export function buildParams(model: string, maxTokens: number, options: Record<string, any> = {}): Record<string, any> {
  log.debug("buildParams input", model, maxTokens, options);
  const fam = family(model);
  const haiku = fam === "claude-haiku-4-5";
  if (maxTokens < 1) throw new RejectedRequest("max_tokens", "must be at least 1");
  const params: Record<string, any> = { model, max_tokens: maxTokens };

  const effort = options.effort;
  if (effort !== undefined && effort !== null) {
    checkEffort(haiku, effort);
    params.output_config = { effort };
  }

  const thinking = options.thinking;
  if (thinking !== undefined && thinking !== null) {
    const kind = thinking.type;
    if (!["adaptive", "enabled", "disabled", "between_tools"].includes(kind)) throw new RejectedRequest("thinking", `unknown thinking type ${kind}`);
    if (kind === "between_tools") checkBetweenTools(fam, effort);
    else checkMode(haiku, kind);
    if (kind === "enabled") checkBudget(thinking.budget_tokens, maxTokens);
    params.thinking = thinkingObject(kind, thinking.budget_tokens);
  }

  for (const name of ["temperature", "top_p", "top_k"]) {
    const value = options[name];
    if (value === undefined || value === null) continue;
    if (!samplingAllowed(haiku, name, value)) throw new RejectedRequest(name, "this model rejects a non-default value");
    params[name] = value;
  }

  if (options.speed === "fast") Object.assign(params, fastParams(fam, options.batch));
  return params;
}
