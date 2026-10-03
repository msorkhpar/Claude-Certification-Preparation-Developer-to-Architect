// Request parameters for thinking, effort and speed, checked per model. See ../../statement.md for the contract.
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

export function buildParams(model: string, maxTokens: number, options: Record<string, any> = {}): Record<string, any> {
  const fam = family(model);
  const haiku = fam === "claude-haiku-4-5";
  if (maxTokens < 1) throw new RejectedRequest("max_tokens", "must be at least 1");
  const params: Record<string, any> = { model, max_tokens: maxTokens };

  const effort = options.effort;
  if (effort !== undefined && effort !== null) {
    if (haiku) throw new RejectedRequest("output_config.effort", "this model does not support effort");
    if (!EFFORTS.includes(effort)) throw new RejectedRequest("output_config.effort", `${effort} is not an effort level`);
    params.output_config = { effort };
  }

  const thinking = options.thinking;
  if (thinking !== undefined && thinking !== null) {
    const kind = thinking.type;
    if (kind === "adaptive") {
      if (haiku) throw new RejectedRequest("thinking", "adaptive thinking is not available on this model");
      params.thinking = { type: "adaptive" };
    } else if (kind === "enabled") {
      const budget = thinking.budget_tokens;
      if (!haiku) throw new RejectedRequest("thinking", "manual thinking budgets are not accepted on this model");
      if (budget === undefined || budget === null || budget < 1000 || budget >= maxTokens) {
        throw new RejectedRequest("thinking.budget_tokens", "at least 1024 and below max_tokens");
      }
      params.thinking = { type: "enabled", budget_tokens: budget };
    } else if (kind === "disabled") {
      if (!haiku) throw new RejectedRequest("thinking", "thinking cannot be turned off on this model");
      params.thinking = { type: "disabled" };
    } else if (kind === "between_tools") {
      if (fam !== "claude-sonnet-5-5") throw new RejectedRequest("thinking", "between_tools exists only on Claude Sonnet 5.5");
      const level = effort ?? DEFAULT_EFFORT[fam];
      if (level === "xhigh" || level === "max") throw new RejectedRequest("thinking", "between_tools works at low, medium and high effort only");
      params.thinking = { type: "between_tools" };
    } else {
      throw new RejectedRequest("thinking", `unknown thinking type ${kind}`);
    }
  }

  for (const name of ["temperature", "top_p", "top_k"]) {
    const value = options[name];
    if (value === undefined || value === null) continue;
    if (!haiku && !(name === "temperature" && value === 1.0)) throw new RejectedRequest(name, "this model rejects a non-default value");
    params[name] = value;
  }

  if (options.speed === "fast") {
    if (fam !== "claude-opus-5-5") throw new RejectedRequest("speed", "fast mode is not available on this model");
    if (options.batch) throw new RejectedRequest("speed", "fast mode is not available in a batch");
    params.speed = "fast";
    params.betas = [FAST_BETA];
  }
  return params;
}
