import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { ParseError, extract, parseJson, validate } = await import(pathToFileURL(resolve(dir, "extractor.ts")).href);

const DOC = "Invoice from Acme Tools. Total due: 120.50 EUR. Thank you for your business.";
const SCHEMA = {
  type: "object",
  required: ["vendor", "total", "currency", "evidence"],
  properties: {
    vendor: { type: "string" },
    total: { type: "number", minimum: 0 },
    currency: { type: "string", enum: ["USD", "EUR", "GBP"] },
    items: { type: "array", items: { type: "object", required: ["name", "qty"], properties: { name: { type: "string" }, qty: { type: "integer", minimum: 1 } } } },
    evidence: { type: "string" },
  },
  additionalProperties: false,
};
const GOOD = { vendor: "Acme Tools", total: 120.5, currency: "EUR", evidence: "Total due: 120.50 EUR" };

const reply = (text: string, stop_reason = "end_turn") => ({ id: "msg_illustrative", type: "message", role: "assistant", model: "claude-sonnet-5-5", content: [{ type: "text", text }], stop_reason, usage: { input_tokens: 1, output_tokens: 1 } });

/** A hand-written, illustrative model: returns the next reply and records the messages it was sent. */
function scripted(...replies: any[]) {
  const seen: any[][] = [];
  const ask = (messages: any[]) => {
    seen.push(JSON.parse(JSON.stringify(messages)));
    return replies.shift();
  };
  return { ask, seen };
}
/** The parsed value, or the string ParseError when the text holds no JSON object. */
function parsed(text: string): unknown {
  try {
    return parseJson(text);
  } catch (err) {
    if (err instanceof ParseError) return "ParseError";
    throw err;
  }
}
const paths = (errors: any) => (errors ?? []).map((e: any) => e.path);

test("m1 a valid reply is returned after one call", () => {
  const m = scripted(reply(JSON.stringify(GOOD)));
  const r = extract(m.ask, DOC, SCHEMA, 3, ["evidence"]) ?? {};
  assert.deepEqual([r.status, r.value, r.attempts, r.errors], ["ok", GOOD, 1, []]);
  assert.equal(m.seen.length, 1);
  assert.ok(m.seen[0][0].role === "user" && m.seen[0][0].content.includes(DOC));
});

test("e1 json is found in fences and prose and bad text is retried", () => {
  assert.deepEqual(parsed("Here you go:\n```json\n" + JSON.stringify(GOOD) + "\n```\nHope that helps."), GOOD);
  assert.deepEqual(parsed("Sure! " + JSON.stringify(GOOD) + " Done."), GOOD);
  for (const bad of ["I cannot find an invoice.", "{not json}", "```json\n```"]) assert.throws(() => parseJson(bad), (e: any) => e instanceof ParseError, bad);
  const m = scripted(reply("I think the total is 120.50"), reply(JSON.stringify(GOOD)));
  const r = extract(m.ask, DOC, SCHEMA) ?? {};
  assert.deepEqual([r.status, r.attempts], ["ok", 2]);
  assert.ok(m.seen[1][2].content.includes("$"));
});

test("e2 every schema violation is listed and sent back to the model", () => {
  const bad = { vendor: "Acme Tools", total: "120.50", currency: "usd", extra: 1, evidence: "x" };
  assert.deepEqual(paths(validate(SCHEMA, bad)), ["$.total", "$.currency", "$.extra"]);
  assert.deepEqual(paths(validate(SCHEMA, { total: -1, currency: "EUR" })), ["$.vendor", "$.evidence", "$.total"]);
  const items = { ...GOOD, items: [{ name: "bolt", qty: 2 }, { name: "nut", qty: 0 }, { name: "gear" }] };
  assert.deepEqual(paths(validate(SCHEMA, items)), ["$.items[1].qty", "$.items[2].qty"]);
  assert.deepEqual(validate(SCHEMA, GOOD), []);
  const first = JSON.stringify(bad);
  const m = scripted(reply(first), reply(JSON.stringify(GOOD)));
  const r = extract(m.ask, DOC, SCHEMA) ?? {};
  assert.deepEqual([r.status, r.attempts], ["ok", 2]);
  const again = m.seen[1];
  assert.deepEqual(again.map((x: any) => x.role), ["user", "assistant", "user"]);
  assert.equal(again[1].content, first);
  for (const p of ["$.total", "$.currency", "$.extra"]) assert.ok(again[2].content.includes(p), p);
});

test("e3 the number of attempts is bounded", () => {
  const wrong = reply(JSON.stringify({ vendor: 1 }));
  const m = scripted(...Array(5).fill(wrong));
  const r = extract(m.ask, DOC, SCHEMA, 3) ?? {};
  assert.deepEqual([r.status, r.attempts, r.value], ["failed", 3, null]);
  assert.equal(m.seen.length, 3);
  assert.ok(paths(r.errors).includes("$.vendor"));
  const once = scripted(...Array(5).fill(wrong));
  assert.equal((extract(once.ask, DOC, SCHEMA, 1) ?? {}).status, "failed");
  assert.equal(once.seen.length, 1);
});

test("e4 a refusal or a cut off reply is not retried", () => {
  const refusal = scripted(reply("I can't help with that.", "refusal"), reply(JSON.stringify(GOOD)));
  let r = extract(refusal.ask, DOC, SCHEMA) ?? {};
  assert.deepEqual([r.status, r.attempts, r.value], ["refused", 1, null]);
  assert.equal(refusal.seen.length, 1);
  const cut = scripted(reply('{"vendor": "Acme', "max_tokens"), reply(JSON.stringify(GOOD)));
  r = extract(cut.ask, DOC, SCHEMA) ?? {};
  assert.deepEqual([r.status, r.attempts], ["truncated", 1]);
  assert.equal(cut.seen.length, 1);
});

test("e5 a quote that is not in the document is rejected", () => {
  const invented = { ...GOOD, evidence: "Total due: 999.00 USD" };
  const m = scripted(reply(JSON.stringify(invented)), reply(JSON.stringify(GOOD)));
  const r = extract(m.ask, DOC, SCHEMA, 3, ["evidence"]) ?? {};
  assert.deepEqual([r.status, r.attempts], ["ok", 2]);
  assert.ok(m.seen[1][2].content.includes("$.evidence"));
  const unchecked = scripted(reply(JSON.stringify(invented)));
  assert.equal((extract(unchecked.ask, DOC, SCHEMA) ?? {}).status, "ok");
});

test("e6 types are exact booleans are not numbers and integers have no fraction", () => {
  assert.deepEqual(paths(validate({ type: "integer" }, true)), ["$"]);
  assert.deepEqual(paths(validate({ type: "number" }, false)), ["$"]);
  assert.deepEqual(paths(validate({ type: "integer" }, 2.5)), ["$"]);
  assert.deepEqual(validate({ type: "integer" }, 2.0), []);
  assert.deepEqual(validate({ type: "number" }, 3), []);
  assert.deepEqual(paths(validate({ type: "string" }, null)), ["$"]);
  assert.deepEqual(validate({ type: "null" }, null), []);
  assert.deepEqual(paths(validate({ type: "array", items: { type: "boolean" } }, [true, 1])), ["$[1]"]);
});
