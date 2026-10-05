// One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md.
import { logger } from "../logger.ts";
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
  // TODO 1 of 7 (finish this to pass e1): the model name without its date.
  // Receives a model id. Returns HAIKU when the id starts with HAIKU (so a dated id such as claude-haiku-4-5-20251001 and the plain name
  // are one family), the id itself otherwise. Example: family("claude-haiku-4-5-20251001") -> "claude-haiku-4-5"
  return model;
}

function checkBedrock(model: string, fam: string, config: Config): void {
  // TODO 2 of 7 (finish this to pass e4 and e6): refuse what Bedrock cannot take.
  // Receives the model id, its family and the config. Throws PlatformError("model", reason) when the family is not in BEDROCK_MODELS,
  // and PlatformError("config", reason) when config has no region or an empty one.
  // Example: checkBedrock("claude-sonnet-4-6", "claude-sonnet-4-6", { region: "us-east-1" }) throws with field "model"
}

function bedrockModelId(fam: string): string {
  // TODO 3 of 7 (finish this to pass m1 and e1): the model id in a Bedrock body.
  // Receives the family. Returns "anthropic." followed by it. Example: bedrockModelId("claude-opus-5-5") -> "anthropic.claude-opus-5-5"
  return fam;
}

function vertexModelId(fam: string): string {
  // TODO 4 of 7 (finish this to pass e1): the model id in a Vertex URL.
  // Receives the family. Returns HAIKU_VERTEX for HAIKU and the family itself for the others.
  // Example: vertexModelId("claude-haiku-4-5") -> "claude-haiku-4-5@20251001"
  return fam;
}

function vertexHost(endpoint: string, fam: string, model: string): string {
  // TODO 5 of 7 (finish this to pass m1 and e3): the host of a Vertex endpoint.
  // Receives the endpoint, the model's family and the model id. Returns aiplatform.googleapis.com for "global",
  // aiplatform.<endpoint>.rep.googleapis.com for "us" and "eu", and <endpoint>-aiplatform.googleapis.com for a specific region, which
  // throws PlatformError("endpoint", reason) unless the family is in REGIONAL_VERTEX_MODELS.
  // Example: vertexHost("eu", "claude-opus-5-5", "claude-opus-5-5") -> "aiplatform.eu.rep.googleapis.com"
  return "";
}

function vertexBody(body: Record<string, any>): Record<string, any> {
  // TODO 6 of 7 (finish this to pass e2): the body Vertex takes.
  // Receives the caller's body. Returns a deep copy without the `model` key (the model is in the URL) and with anthropic_version set to
  // VERTEX_VERSION; the caller's own body is never changed. Example: { model: "x", max_tokens: 5 } -> { max_tokens: 5, anthropic_version: ... }
  return structuredClone(body);
}

function lacking(platform: string, features: string[]): string[] {
  // TODO 7 of 7 (finish this to pass e5): the features this platform lacks.
  // Receives a known platform and a list of known feature names. Returns the names that are in MISSING[platform], in the order given.
  // Example: lacking("vertex", ["batches", "thinking"]) -> ["batches"]
  return [];
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
