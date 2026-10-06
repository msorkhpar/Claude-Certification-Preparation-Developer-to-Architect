import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { argsDigest, callTool, listTools, mintState, readState } = await import(pathToFileURL(resolve(dir, "mrtr.ts")).href);

const SECRET = "s3cret";
const VERSION = "2026-07-28";
const BOTH = { elicitation: {}, sampling: {} };
const ARGS = { service: "api", env: "production" };
const CONFIRM = { confirm: { method: "elicitation/create", params: { mode: "form", message: "Deploy api to production?", requestedSchema: {
  type: "object", properties: { confirm: { type: "boolean", title: "Confirm the deployment" } }, required: ["confirm"] } } } };
const NOTES = { notes: { method: "sampling/createMessage", params: { messages: [{ role: "user", content: { type: "text", text: "Write one sentence of release notes for api." } }], maxTokens: 100 } } };
const YES = { confirm: { action: "accept", content: { confirm: true } } };

function request(name = "deploy", args: any = null, caps: any = null, version: string | null = VERSION, extra: Record<string, unknown> = {}): any {
  const meta: Record<string, unknown> = version === null ? {} : { "io.modelcontextprotocol/protocolVersion": version };
  meta["io.modelcontextprotocol/clientCapabilities"] = caps === null ? BOTH : caps;
  return { name, arguments: args === null ? ARGS : args, _meta: meta, ...extra };
}
const call = (req: any, now = 1000, principal = "alice", secret = SECRET): any => callTool(req, secret, principal, now) ?? {};
const retry = (first: any, responses: unknown, caps: any = null) => request("deploy", null, caps, VERSION, { inputResponses: responses, requestState: first.requestState });
const complete = (text: string, isError = false) => ({ resultType: "complete", content: [{ type: "text", text }], isError });
const errorOf = (result: any) => [result?.error?.code, result?.error?.message];

test("m1 a production deploy takes three round trips and keeps no state", () => {
  const first = call(request());
  assert.ok(first.resultType === "input_required" && typeof first.requestState === "string" && !("content" in first));
  assert.deepEqual(first.inputRequests, CONFIRM);
  const second = call(retry(first, YES), 1010);
  assert.equal(second.resultType, "input_required");
  assert.deepEqual(second.inputRequests, NOTES);
  assert.ok(typeof second.requestState === "string" && second.requestState !== first.requestState);
  const notes = { notes: { role: "assistant", content: { type: "text", text: "Faster checkout." }, model: "m", stopReason: "endTurn" } };
  assert.deepEqual(call(retry(second, notes), 1020), complete("Deployed api to production. Release notes: Faster checkout."));
});

test("e1 a staging deploy and a status call finish at once and the tool list is cacheable", () => {
  assert.deepEqual(call(request("deploy", { service: "api", env: "staging" }, {})), complete("Deployed api to staging"));
  assert.deepEqual(call(request("status", { service: "api" }, {})), complete("api: running"));
  const listing = listTools({ _meta: { "io.modelcontextprotocol/protocolVersion": VERSION } }) ?? {};
  assert.deepEqual([listing.resultType, listing.ttlMs, listing.cacheScope], ["complete", 300000, "public"]);
  assert.deepEqual((listing.tools ?? []).map((t: any) => t.name), ["deploy", "status"]);
  assert.deepEqual([listing.tools?.[0]?.inputSchema?.required, listing.tools?.[1]?.inputSchema?.required], [["service", "env"], ["service"]]);
});

test("e2 the server only asks what the client declared it can answer", () => {
  const refusal = "Deploying to production needs confirmation, and this client cannot be asked.";
  assert.deepEqual(call(request("deploy", null, {})), complete(refusal, true));
  assert.deepEqual(call(request("deploy", null, { sampling: {} })), complete(refusal, true));
  assert.deepEqual(call(request("deploy", null, { elicitation: { url: {} } })), complete(refusal, true));
  const formOnly = { elicitation: { form: {} } };
  const first = call(request("deploy", null, formOnly));
  assert.deepEqual(first.inputRequests, CONFIRM);
  assert.deepEqual(call(retry(first, YES, formOnly), 1010), complete("Deployed api to production"));
});

test("e3 a no or a missing answer is handled without an error", () => {
  const first = call(request());
  for (const answer of [{ action: "decline" }, { action: "cancel" }, { action: "accept", content: { confirm: false } }, { action: "accept" }, { action: "decline", content: { confirm: true } }]) {
    assert.deepEqual(call(retry(first, { confirm: answer }), 1010), complete("Deployment cancelled"));
  }
  for (const responses of [{}, { other: 1 }, { confirm: { action: "maybe" } }, { confirm: "yes" }]) {
    const again = call(retry(first, responses), 1100);
    assert.equal(again.resultType, "input_required");
    assert.deepEqual(again.inputRequests, CONFIRM);
    assert.ok(again.requestState !== undefined && again.requestState !== first.requestState);
  }
  const second = call(retry(first, YES), 1010);
  assert.deepEqual(call(retry(second, {}), 1020).inputRequests, NOTES);
  assert.deepEqual(call(retry(second, { notes: { role: "assistant", content: { type: "image" } } }), 1020).inputRequests, NOTES);
});

test("e4 a state the server did not sign is refused", () => {
  const first = call(request());
  const token: string = first.requestState ?? "x.y";
  const flipped = token.slice(0, -1) + (token.endsWith("A") ? "B" : "A");
  const foreign = call(request(), 1000, "alice", "another secret").requestState;
  for (const bad of [flipped, "abc", "", foreign]) {
    assert.deepEqual(errorOf(call(request("deploy", null, null, VERSION, { inputResponses: YES, requestState: bad }), 1010)), [-32602, "Invalid requestState"]);
  }
});

test("e5 a state works only for the same user the same call and before it expires", () => {
  const state = (over: Record<string, unknown> = {}) => mintState(SECRET, { v: 1, tool: "deploy", digest: argsDigest(ARGS), sub: "alice", exp: 999, step: "confirm", ...over });
  const go = (token: string, now = 999, principal = "alice", args: any = null) => call(request("deploy", args, null, VERSION, { inputResponses: YES, requestState: token }), now, principal);
  assert.deepEqual(go(state()).inputRequests, NOTES);
  assert.deepEqual(errorOf(go(state(), 1000)), [-32602, "Expired requestState"]);
  assert.deepEqual(errorOf(go(state(), 999, "bob")), [-32602, "requestState does not match this request"]);
  assert.deepEqual(errorOf(go(state(), 999, "alice", { service: "billing", env: "production" })), [-32602, "requestState does not match this request"]);
  assert.deepEqual(errorOf(go(state({ tool: "status" }))), [-32602, "requestState does not match this request"]);
  assert.deepEqual(go(state({ step: "notes" })).inputRequests, NOTES);
});

test("e6 a request the server cannot serve is a protocol error with a code", () => {
  const old = call(request("deploy", null, null, "2025-11-25"));
  assert.deepEqual(errorOf(old), [-32022, "Unsupported protocol version"]);
  assert.deepEqual(old.error?.data, { supported: [VERSION] });
  assert.equal(errorOf(call(request("deploy", null, null, null)))[0], -32022);
  assert.deepEqual(errorOf(call(request("rollback"))), [-32602, "Unknown tool: rollback"]);
  assert.deepEqual(errorOf(call(request("deploy", { env: "staging" }))), [-32602, "Invalid params: service is required"]);
  assert.deepEqual(errorOf(call(request("deploy", { service: "  ", env: "staging" }))), [-32602, "Invalid params: service is required"]);
  assert.deepEqual(errorOf(call(request("deploy", { service: "api", env: "dev" }))), [-32602, "Invalid params: env must be staging or production"]);
  assert.equal(errorOf(call(request("status", {})))[1], "Invalid params: service is required");
  assert.equal(errorOf(listTools({ _meta: { "io.modelcontextprotocol/protocolVersion": "2024-11-05" } }) ?? {})[0], -32022);
});

test("e7 the state carries the whole context and an answer alone never skips a step", () => {
  const first = call(request(), 1000);
  const again = call(request(), 1000);
  assert.deepEqual(first.inputRequests, again.inputRequests);
  assert.deepEqual(readState(SECRET, first.requestState ?? "") ?? {}, { v: 1, tool: "deploy", digest: argsDigest(ARGS), sub: "alice", exp: 1300, step: "confirm" });
  assert.equal(argsDigest({ env: "production", service: "api" }), argsDigest(ARGS));
  const alone = call(request("deploy", null, null, VERSION, { inputResponses: YES }));
  assert.equal(alone.resultType, "input_required");
  assert.deepEqual(alone.inputRequests, CONFIRM);
  assert.deepEqual(call(retry(first, { ...YES, junk: { action: "accept" } }), 1010).inputRequests, NOTES);
  const second = call(retry(first, YES), 1010);
  assert.equal((readState(SECRET, second.requestState ?? "") ?? {}).step, "notes");
  const before = JSON.stringify(first);
  call(retry(first, YES), 1010);
  assert.equal(JSON.stringify(first), before);
});
