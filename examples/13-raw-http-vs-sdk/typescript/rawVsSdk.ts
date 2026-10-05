// One Messages call written by hand with fetch, then the same call through the SDK.
// Both go through the same scripted fetch, so nothing leaves the container. The reply is an
// illustrative, hand-written Messages response (claude-sonnet-5-5), not a capture.
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { logger } from "./logger.ts";
const log = logger("raw_vs_sdk");

const MODEL = "claude-sonnet-5-5";
const URL = "https://api.anthropic.com/v1/messages";
const PAYLOAD = { model: MODEL, max_tokens: 64, messages: [{ role: "user" as const, content: "Capital of France?" }] };
const OK = { body: message([text("Paris.")]) };
const LIMITED = {
  status: 429,
  body: { type: "error", error: { type: "rate_limit_error", message: "Rate limited" }, request_id: "req_illustrative_0001" },
  headers: { "retry-after": "7", "request-id": "req_illustrative_0001" },
};

// The HTTP request the SDK would build, written out: three headers and a JSON body.
export function rawCall(fetchFn: typeof fetch) {
  const headers = { "x-api-key": "placeholder", "anthropic-version": "2023-06-01", "content-type": "application/json" };
  return fetchFn(URL, { method: "POST", headers, body: JSON.stringify(PAYLOAD) });
}

export function sdkCall(fetchFn: typeof fetch) {
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fetchFn });
  return client.messages.create(PAYLOAD);
}

async function main() {
  const raw = scriptedFetch([OK, LIMITED]);
  const sdk = scriptedFetch([OK, LIMITED]);

  const reply = await rawCall(raw.fetch);
  console.log("raw :", raw.seen[0].method, raw.seen[0].url, "->", reply.status, JSON.stringify(((await reply.json()) as any).content[0].text));
  const msg = await sdkCall(sdk.fetch);
  console.log("sdk :", sdk.seen[0].method, sdk.seen[0].url, "->", "200", JSON.stringify((msg.content[0] as any).text));
  console.log("same URL:", raw.seen[0].url === sdk.seen[0].url, "| same body:", JSON.stringify(raw.seen[0].body) === JSON.stringify(sdk.seen[0].body));
  for (const name of ["anthropic-version", "content-type"]) {
    console.log(`${name}: raw ${raw.seen[0].headers[name]} | sdk ${sdk.seen[0].headers[name]}`);
  }
  const onlySdk = Object.keys(sdk.seen[0].headers).filter((h) => !(h in raw.seen[0].headers)).sort();
  console.log("headers only the SDK adds:", onlySdk.join(", "));

  const limited = await rawCall(raw.fetch);
  console.log("raw 429 :", limited.status, ((await limited.json()) as any).error.type, "retry-after", limited.headers.get("retry-after"));
  try {
    await sdkCall(sdk.fetch);
  } catch (err) {
    if (err instanceof Anthropic.RateLimitError) console.log("sdk 429 :", err.constructor.name, err.status, "request id", err.requestID);
    else throw err;
  }
}

if (import.meta.main) await main();
