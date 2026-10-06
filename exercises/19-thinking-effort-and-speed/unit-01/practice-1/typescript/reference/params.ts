// Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract.
import { logger } from "../logger.ts";
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

/** Refuse an effort level the model cannot take. */
function checkEffort(haiku: boolean, effort: string): void {
  if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");
  if (!EFFORTS.includes(effort)) throw new RejectedRequest("output_config.effort", `${effort} is not an effort level`);
}

/** Refuse a thinking mode the model does not have. */
function checkMode(haiku: boolean, kind: string): void {
  if (kind === "adaptive" && haiku) throw new RejectedRequest("thinking", "adaptive thinking is not available on this model");
  if (kind === "enabled" && !haiku) throw new RejectedRequest("thinking", "manual thinking budgets are not accepted on this model");
  if (kind === "disabled" && !haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");
}

/** Refuse a manual thinking budget that is missing, below 1024 or not below maxTokens. */
function checkBudget(budget: number | undefined | null, maxTokens: number): void {
  if (budget === undefined || budget === null || budget < 1024 || budget >= maxTokens) {
    throw new RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens");
  }
}

/** Refuse between_tools off Sonnet 5.5, or when the effective effort is xhigh or max. */
function checkBetweenTools(fam: string, effort: string | undefined | null): void {
  if (fam !== "claude-sonnet-5-5") throw new RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5");
  const level = effort ?? DEFAULT_EFFORT[fam];
  if (level === "xhigh" || level === "max") throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");
}

/** The `thinking` value of the request: the type, and the budget for a manual one. */
function thinkingObject(kind: string, budget: number | undefined | null): Record<string, any> {
  if (kind === "enabled") return { type: "enabled", budget_tokens: budget };
  return { type: kind };
}

/** Whether the model accepts this sampling parameter: Haiku all, the others only temperature 1.0. */
function samplingAllowed(haiku: boolean, name: string, value: number): boolean {
  return haiku || (name === "temperature" && value === 1.0);
}

/** The parameters fast mode adds, or a refusal: Opus 5.5 only, and never in a batch. */
function fastParams(fam: string, batch: unknown): Record<string, any> {
  if (fam !== "claude-opus-5-5") throw new RejectedRequest("speed", "fast mode is not available on this model");
  if (batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");
  return { speed: "fast", betas: [FAST_BETA] };
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
