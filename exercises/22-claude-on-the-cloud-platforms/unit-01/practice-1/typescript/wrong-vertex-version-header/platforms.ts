// One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md.
const ANTHROPIC_VERSION = "2023-06-01";
const VERTEX_VERSION = "vertex-2023-10-16";
const HAIKU = "claude-haiku-4-5";
const HAIKU_VERTEX = "claude-haiku-4-5@20251001";
const BEDROCK_MODELS = new Set(["claude-fable-5-1", "claude-opus-5-5", "claude-sonnet-5-5", HAIKU]);
const VERTEX_MODELS = new Set([...BEDROCK_MODELS, "claude-sonnet-4-6"]);
const REGIONAL_VERTEX_MODELS = new Set(["claude-sonnet-4-6"]); // specific regional endpoints serve Sonnet 4.6 and earlier only
const MISSING: Record<string, Set<string>> = {
  anthropic: new Set(),
  bedrock: new Set(["batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector", "structured_outputs", "skills", "managed_agents"]),
  vertex: new Set(["batches", "fast_mode", "files_api", "web_fetch", "code_execution", "mcp_connector", "skills", "managed_agents"]),
};
const FEATURES = new Set(["batches", "fast_mode", "files_api", "web_search", "web_fetch", "code_execution", "mcp_connector", "structured_outputs", "skills", "managed_agents", "prompt_caching", "thinking", "tool_use", "citations"]);

export type Config = { region?: string; project?: string; endpoint?: string };
export type Built = { method: string; url: string; headers: Record<string, string>; body: Record<string, any> };

/** The request cannot be built for this platform. `field` names the offending part. */
export class PlatformError extends Error {
  field: string;
  reason: string;
  constructor(field: string, reason: string) {
    super(`${field}: ${reason}`);
    this.field = field;
    this.reason = reason;
  }
}

const family = (model: string) => (model.startsWith(HAIKU) ? HAIKU : model);

export function buildRequest(platform: string, model: string, body: Record<string, any>, config: Config): Built {
  const fam = family(model);
  if (platform === "anthropic") {
    return { method: "POST", url: "https://api.anthropic.com/v1/messages", headers: { "anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json" }, body: { ...structuredClone(body), model } };
  }
  if (platform === "bedrock") {
    if (!BEDROCK_MODELS.has(fam)) throw new PlatformError("model", `${model} is not served by Claude in Amazon Bedrock`);
    if (!config.region) throw new PlatformError("config", "a Bedrock request needs a region");
    return {
      method: "POST",
      url: `https://bedrock-mantle.${config.region}.api.aws/anthropic/v1/messages`,
      headers: { "anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json" },
      body: { ...structuredClone(body), model: `anthropic.${fam}` },
    };
  }
  if (platform === "vertex") {
    if (!VERTEX_MODELS.has(fam)) throw new PlatformError("model", `${model} is not served on Google Vertex AI`);
    if (!config.project) throw new PlatformError("config", "a Vertex request needs a project");
    const endpoint = config.endpoint ?? "global";
    const modelId = fam === HAIKU ? HAIKU_VERTEX : fam;
    const path = `/v1/projects/${config.project}/locations/${endpoint}/publishers/anthropic/models/${modelId}:rawPredict`;
    let host: string;
    if (endpoint === "global") host = "aiplatform.googleapis.com";
    else if (endpoint === "us" || endpoint === "eu") host = `aiplatform.${endpoint}.rep.googleapis.com`;
    else {
      if (!REGIONAL_VERTEX_MODELS.has(fam)) throw new PlatformError("endpoint", `${model} is served on the global and multi-region endpoints, not on a specific region`);
      host = `${endpoint}-aiplatform.googleapis.com`;
    }
    const sent = structuredClone(body);
    delete sent.model;
    return { method: "POST", url: `https://${host}${path}`, headers: { "anthropic-version": VERTEX_VERSION, "content-type": "application/json" }, body: sent };
  }
  throw new PlatformError("platform", `unknown platform ${platform}`);
}

export function unsupportedFeatures(platform: string, features: string[]): string[] {
  const missing = MISSING[platform];
  if (!missing) throw new PlatformError("platform", `unknown platform ${platform}`);
  for (const name of features) if (!FEATURES.has(name)) throw new PlatformError("feature", `unknown feature ${name}`);
  return features.filter((name) => missing.has(name));
}
