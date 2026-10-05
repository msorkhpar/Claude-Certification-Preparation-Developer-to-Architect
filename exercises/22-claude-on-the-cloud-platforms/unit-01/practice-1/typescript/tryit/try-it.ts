// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { PlatformError, buildRequest, unsupportedFeatures } from "./platforms.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const body = { model: "ignored-by-the-builder", max_tokens: 256, messages: [{ role: "user", content: "Hello, Claude" }] };
const OPUS = "claude-opus-5-5";

// The same message for three front doors: the URL, the model id and the version header change.
try {
  const doors: [string, Record<string, string>][] = [["anthropic", {}], ["bedrock", { region: "us-east-1" }], ["vertex", { project: "my-project" }]];
  for (const [platform, config] of doors) {
    const request: any = buildRequest(platform, OPUS, { ...body }, config) ?? {};
    console.log(platform, "->", request.url);
    console.log("   model in body:", request.body?.model, "| headers:", Object.keys(request.headers ?? {}).sort().join(", "));
  }
} catch (err) {
  if (err instanceof PlatformError) console.log("platform error:", err.message);
  else throw err;
}

// What a team would lose by moving to Bedrock.
console.log("missing on bedrock:", unsupportedFeatures("bedrock", ["batches", "fast_mode", "files_api"]));
