import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { ApiError, buildRequest, sendMessages, textOf } = await import(pathToFileURL(resolve(dir, "rawClient.ts")).href);

const KEY = "sk-test-0123456789abcdef";
const MSGS = [{ role: "user", content: "Capital of France?" }];
const OK_BODY = {
  id: "msg_x", type: "message", role: "assistant", model: "claude-sonnet-5-5",
  content: [{ type: "text", text: "Paris." }], stop_reason: "end_turn", stop_sequence: null,
  usage: { input_tokens: 9, output_tokens: 3 },
};

const reply = (status: number, body: unknown, headers: Record<string, string> = {}) => ({
  status, headers, body: typeof body === "string" ? body : JSON.stringify(body),
});

/** The error the call threw, or null when it did not throw. */
function errorOf(fn: () => unknown): unknown {
  try {
    fn();
  } catch (err) {
    return err;
  }
  return null;
}

test("m1 request has method, url, three headers and a JSON body", () => {
  const req = buildRequest(KEY, "claude-sonnet-5-5", MSGS, 64, "Be brief.");
  assert.equal(req.method, "POST");
  assert.equal(req.url, "https://api.anthropic.com/v1/messages");
  assert.deepEqual(req.headers, { "x-api-key": KEY, "anthropic-version": "2023-06-01", "content-type": "application/json" });
  assert.deepEqual(JSON.parse(req.body), { model: "claude-sonnet-5-5", max_tokens: 64, messages: MSGS, system: "Be brief." });
});

test("e1 blank or absent system is left out of the body", () => {
  for (const system of [undefined, null, "", "   "]) {
    assert.ok(!("system" in JSON.parse(buildRequest(KEY, "m", MSGS, 8, system).body)), String(system));
  }
  assert.equal(JSON.parse(buildRequest(KEY, "m", MSGS, 8, "x").body).system, "x");
});

test("e2 bad input is refused before anything is sent", () => {
  let calls = 0;
  const transport = () => {
    calls++;
    return reply(200, OK_BODY);
  };
  const cases = [{ maxTokens: 0 }, { maxTokens: -5 }, { messages: [] }, { apiKey: "  " }];
  for (const bad of cases) {
    const a = { apiKey: KEY, messages: MSGS, maxTokens: 8, ...bad };
    const err = errorOf(() => sendMessages(transport, a.apiKey, "m", a.messages, a.maxTokens));
    assert.ok(err instanceof Error && !(err instanceof ApiError), `${JSON.stringify(bad)} -> ${err}`);
  }
  assert.equal(calls, 0);
});

test("e3 success returns the message and text joins text blocks only", () => {
  const seen: any[] = [];
  const message = sendMessages((req: any) => (seen.push(req), reply(200, OK_BODY)), KEY, "claude-sonnet-5-5", MSGS, 64);
  assert.ok(message.stop_reason === "end_turn" && seen.length === 1 && seen[0].method === "POST");
  const mixed = { content: [{ type: "text", text: "Let me " }, { type: "tool_use", id: "t", name: "x", input: {} }, { type: "text", text: "check." }] };
  assert.equal(textOf(mixed), "Let me check.");
  assert.equal(textOf({ content: [] }), "");
});

test("e4 an error reply becomes an ApiError with the header request id", () => {
  const body = { type: "error", error: { type: "rate_limit_error", message: "Rate limited" }, request_id: "req_from_body" };
  let err: any = errorOf(() => sendMessages(() => reply(429, body, { "request-id": "req_from_header" }), KEY, "m", MSGS, 8));
  assert.ok(err instanceof ApiError, String(err));
  assert.deepEqual([err.status, err.errorType, err.detail], [429, "rate_limit_error", "Rate limited"]);
  assert.equal(err.requestId, "req_from_header");
  err = errorOf(() => sendMessages(() => reply(529, { ...body, error: { type: "overloaded_error", message: "Overloaded" } }), KEY, "m", MSGS, 8));
  assert.ok(err instanceof ApiError && err.requestId === "req_from_body" && err.errorType === "overloaded_error");
});

test("e5 a reply that is not JSON still gives an ApiError", () => {
  const html = "<html><body><h1>502 Bad Gateway</h1></body></html>";
  const err: any = errorOf(() => sendMessages(() => reply(502, html), KEY, "m", MSGS, 8));
  assert.ok(err instanceof ApiError, String(err));
  assert.ok(err.status === 502 && err.errorType === "unknown" && err.detail.includes("502 Bad Gateway"));
  assert.equal(err.requestId, null);
});

test("e6 the API key never appears in an error", () => {
  const body = { type: "error", error: { type: "authentication_error", message: `invalid x-api-key: ${KEY}` } };
  const err: any = errorOf(() => sendMessages(() => reply(401, body), KEY, "m", MSGS, 8));
  assert.ok(err instanceof ApiError, String(err));
  assert.ok(!String(err.message).includes(KEY) && !err.detail.includes(KEY));
  assert.ok(err.detail.includes("[redacted]"));
});
