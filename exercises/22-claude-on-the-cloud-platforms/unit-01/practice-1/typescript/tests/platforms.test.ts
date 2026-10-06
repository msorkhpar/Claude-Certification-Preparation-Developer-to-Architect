import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { PlatformError, buildRequest, unsupportedFeatures } = await import(pathToFileURL(resolve(dir, "platforms.ts")).href);

const BODY = { model: "ignored-by-the-builder", max_tokens: 256, messages: [{ role: "user", content: "Hello, Claude" }] };
const OPUS = "claude-opus-5-5", SONNET5 = "claude-sonnet-5-5", SONNET46 = "claude-sonnet-4-6", HAIKU = "claude-haiku-4-5-20251001", FABLE = "claude-fable-5-1";

const built = (platform: string, model: string, config: Record<string, string> = {}, body: any = BODY): any => buildRequest(platform, model, structuredClone(body), config) ?? {};
const fieldOf = (platform: string, model: string, config: Record<string, string> = {}): string | null => {
  try {
    buildRequest(platform, model, structuredClone(BODY), config);
  } catch (err) {
    return err instanceof PlatformError ? err.field : `crash: ${err}`;
  }
  return null;
};

test("m1 the same message takes three shapes", () => {
  const direct = built("anthropic", OPUS);
  assert.equal(direct.url, "https://api.anthropic.com/v1/messages");
  assert.deepEqual(direct.headers, { "anthropic-version": "2023-06-01", "content-type": "application/json" });
  assert.equal(direct.body?.model, OPUS);
  const bedrock = built("bedrock", OPUS, { region: "us-east-1" });
  assert.equal(bedrock.url, "https://bedrock-mantle.us-east-1.api.aws/anthropic/v1/messages");
  assert.equal(bedrock.headers?.["anthropic-version"], "2023-06-01");
  assert.equal(bedrock.body?.model, "anthropic.claude-opus-5-5");
  const vertex = built("vertex", OPUS, { project: "my-project" });
  assert.equal(vertex.url, "https://aiplatform.googleapis.com/v1/projects/my-project/locations/global/publishers/anthropic/models/claude-opus-5-5:rawPredict");
  assert.equal(vertex.headers !== undefined && !("anthropic-version" in vertex.headers), true);
  assert.equal(vertex.method, "POST");
});

test("e1 model ids change with the platform", () => {
  assert.equal(built("anthropic", HAIKU).body?.model, HAIKU);
  assert.equal(built("bedrock", HAIKU, { region: "eu-west-1" }).body?.model, "anthropic.claude-haiku-4-5");
  assert.equal(built("bedrock", "claude-haiku-4-5", { region: "eu-west-1" }).body?.model, "anthropic.claude-haiku-4-5");
  assert.equal(built("bedrock", FABLE, { region: "us-east-1" }).body?.model, "anthropic.claude-fable-5-1");
  assert.equal(String(built("vertex", HAIKU, { project: "p" }).url).endsWith("/models/claude-haiku-4-5@20251001:rawPredict"), true);
  assert.equal(String(built("vertex", SONNET5, { project: "p" }).url).endsWith("/models/claude-sonnet-5-5:rawPredict"), true);
});

test("e2 vertex moves the model into the url and the version into the body", () => {
  const body = structuredClone(BODY);
  const sent = buildRequest("vertex", OPUS, body, { project: "p" })?.body ?? { model: "missing" };
  assert.equal("model" in sent, false);
  assert.equal(sent.anthropic_version, "vertex-2023-10-16");
  assert.equal(sent.max_tokens, 256);
  assert.deepEqual(sent.messages, BODY.messages);
  assert.deepEqual(body, BODY); // the caller's body is left as it was
  const other = buildRequest("bedrock", OPUS, body, { region: "us-east-1" })?.body ?? { anthropic_version: "missing" };
  assert.equal("anthropic_version" in other, false);
  assert.equal(other.max_tokens, 256);
  assert.deepEqual(body, BODY);
});

test("e3 vertex endpoints global multi region and regional", () => {
  assert.equal(built("vertex", OPUS, { project: "p", endpoint: "us" }).url, "https://aiplatform.us.rep.googleapis.com/v1/projects/p/locations/us/publishers/anthropic/models/claude-opus-5-5:rawPredict");
  assert.equal(String(built("vertex", SONNET5, { project: "p", endpoint: "eu" }).url).startsWith("https://aiplatform.eu.rep.googleapis.com/v1/projects/p/locations/eu/"), true);
  assert.equal(built("vertex", SONNET46, { project: "p", endpoint: "europe-west1" }).url, "https://europe-west1-aiplatform.googleapis.com/v1/projects/p/locations/europe-west1/publishers/anthropic/models/claude-sonnet-4-6:rawPredict");
  assert.equal(fieldOf("vertex", OPUS, { project: "p", endpoint: "europe-west1" }), "endpoint");
  assert.equal(fieldOf("vertex", HAIKU, { project: "p", endpoint: "us-east5" }), "endpoint");
});

test("e4 a platform serves only its own models", () => {
  assert.equal(fieldOf("bedrock", SONNET46, { region: "us-east-1" }), "model");
  assert.equal(fieldOf("bedrock", "claude-nonexistent-9", { region: "us-east-1" }), "model");
  assert.equal(fieldOf("vertex", "claude-nonexistent-9", { project: "p" }), "model");
  assert.equal(fieldOf("vertex", SONNET46, { project: "p" }), null);
  assert.equal(fieldOf("mystery", OPUS), "platform");
});

test("e5 each platform lacks its own features", () => {
  const names = ["batches", "fast_mode", "prompt_caching", "thinking", "web_search", "structured_outputs", "files_api", "tool_use"];
  assert.deepEqual(unsupportedFeatures("anthropic", names), []);
  assert.deepEqual(unsupportedFeatures("bedrock", names), ["batches", "fast_mode", "web_search", "structured_outputs", "files_api"]);
  assert.deepEqual(unsupportedFeatures("vertex", names), ["batches", "fast_mode", "files_api"]);
  assert.deepEqual(unsupportedFeatures("vertex", ["web_fetch", "mcp_connector", "citations"]), ["web_fetch", "mcp_connector"]);
});

test("e6 a request needs the place it is sent to", () => {
  assert.equal(fieldOf("bedrock", OPUS), "config");
  assert.equal(fieldOf("vertex", OPUS), "config");
  assert.equal(fieldOf("bedrock", OPUS, { region: "" }), "config");
  assert.equal(fieldOf("anthropic", OPUS), null);
});
