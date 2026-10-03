// What the SDK retries on its own, and what it does not, against a scripted fetch.
// The failures are illustrative, hand-written replies shaped like the API's error bodies.
// The SDK sleeps a short exponential back-off between attempts (about 0.5 s, then about 1 s).
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text, type Reply } from "../../../harness/ts/scriptedFetch.ts";

const PARAMS = { model: "claude-sonnet-5-5", max_tokens: 16, messages: [{ role: "user" as const, content: "Hi" }] };

function error(status: number, kind: string, requestId: string, details?: object): Reply {
  const err: Record<string, unknown> = { type: kind, message: kind };
  if (details) err.details = details;
  return { status, body: { type: "error", error: err, request_id: requestId }, headers: { "request-id": requestId } };
}

export const OVERLOADED = error(529, "overloaded_error", "req_illustrative_0529");
export const BAD_REQUEST = error(400, "invalid_request_error", "req_illustrative_0400");
export const SPEND_CAP = error(429, "rate_limit_error", "req_illustrative_0429", { error_code: "enforced_spend_limit_reached" });
export const HELLO: Reply = { body: message([text("Hello.")]) };

export function clientFor(script: Reply[], maxRetries: number) {
  const fake = scriptedFetch(script);
  return { fake, client: new Anthropic({ apiKey: "placeholder", maxRetries, fetch: fake.fetch }), params: PARAMS };
}

async function attempt(label: string, script: Reply[], maxRetries: number) {
  const { fake, client } = clientFor(script, maxRetries);
  let outcome: string;
  try {
    const reply = await client.messages.create(PARAMS);
    outcome = `ok '${(reply.content[0] as any).text}'`;
  } catch (err) {
    if (err instanceof Anthropic.APIError && err.status) outcome = `${err.constructor.name} ${err.status} ${(err.error as any).error.type} request id ${err.requestID}`;
    else if (err instanceof Anthropic.APIConnectionError) outcome = err.constructor.name;
    else throw err;
  }
  const counts = fake.seen.map((r) => r.headers["x-stainless-retry-count"]);
  console.log(`${label}: ${fake.seen.length} request(s), retry-count header [${counts.join(", ")}] -> ${outcome}`);
}

async function main() {
  await attempt("529, 529, then 200, maxRetries=2", [OVERLOADED, OVERLOADED, HELLO], 2);
  await attempt("529, 529, then 200, maxRetries=0", [OVERLOADED, OVERLOADED, HELLO], 0);
  await attempt("400 is never retried, maxRetries=2", [BAD_REQUEST, HELLO], 2);
  await attempt("spend-cap 429, maxRetries=2", [SPEND_CAP, SPEND_CAP, SPEND_CAP], 2);
  await attempt("connection fails twice, maxRetries=1", [{ networkError: true }, { networkError: true }], 1);
}

if (import.meta.main) await main();
