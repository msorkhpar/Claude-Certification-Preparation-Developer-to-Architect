// One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md.
import { logger } from "./logger.ts";
const log = logger("platforms");

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

function family(model: string): string {
  return model.startsWith(HAIKU) ? HAIKU : model;
}

function checkBedrock(model: string, fam: string, config: Config): void {
  if (!BEDROCK_MODELS.has(fam)) throw new PlatformError("model", `${model} is not served by Claude in Amazon Bedrock`);
  if (!config.region) throw new PlatformError("config", "a Bedrock request needs a region");
}

function bedrockModelId(fam: string): string {
  return `anthropic.${fam}`;
}

function vertexModelId(fam: string): string {
  return fam === HAIKU ? HAIKU_VERTEX : fam;
}

function vertexHost(endpoint: string, fam: string, model: string): string {
  if (endpoint === "global") return "aiplatform.googleapis.com";
  if (endpoint === "us" || endpoint === "eu") return `aiplatform.${endpoint}.rep.googleapis.com`;
  if (!REGIONAL_VERTEX_MODELS.has(fam)) throw new PlatformError("endpoint", `${model} is served on the global and multi-region endpoints, not on a specific region`);
  return `${endpoint}-aiplatform.googleapis.com`;
}

function vertexBody(body: Record<string, any>): Record<string, any> {
  const sent = structuredClone(body);
  delete sent.model;
  sent.anthropic_version = VERTEX_VERSION;
  return sent;
}

function lacking(platform: string, features: string[]): string[] {
  return features.filter((name) => MISSING[platform].has(name));
}

export function buildRequest(platform: string, model: string, body: Record<string, any>, config: Config): Built {
  log.debug("buildRequest input", platform, model, body, config);
  const fam = family(model);
  if (platform === "anthropic") {
    return { method: "POST", url: "https://api.anthropic.com/v1/messages", headers: { "anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json" }, body: { ...structuredClone(body), model } };
  }
  if (platform === "bedrock") {
    checkBedrock(model, fam, config);
    return {
      method: "POST",
      url: `https://bedrock-mantle.${config.region}.api.aws/anthropic/v1/messages`,
      headers: { "anthropic-version": ANTHROPIC_VERSION, "content-type": "application/json" },
      body: { ...structuredClone(body), model: bedrockModelId(fam) },
    };
  }
  if (platform === "vertex") {
    if (!VERTEX_MODELS.has(fam)) throw new PlatformError("model", `${model} is not served on Google Vertex AI`);
    if (!config.project) throw new PlatformError("config", "a Vertex request needs a project");
    const endpoint = config.endpoint ?? "global";
    const path = `/v1/projects/${config.project}/locations/${endpoint}/publishers/anthropic/models/${vertexModelId(fam)}:rawPredict`;
    const host = vertexHost(endpoint, fam, model);
    return { method: "POST", url: `https://${host}${path}`, headers: { "content-type": "application/json" }, body: vertexBody(body) };
  }
  throw new PlatformError("platform", `unknown platform ${platform}`);
}

export function unsupportedFeatures(platform: string, features: string[]): string[] {
  const missing = MISSING[platform];
  if (!missing) throw new PlatformError("platform", `unknown platform ${platform}`);
  for (const name of features) if (!FEATURES.has(name)) throw new PlatformError("feature", `unknown feature ${name}`);
  return lacking(platform, features);
}
