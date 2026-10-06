import { test } from "node:test";
import assert from "node:assert/strict";
import { DOC, LOCAL_SCHEMA, REPLIES, body, clientFor, extract, forApi, normaliseEnum, problems } from "./structuredExtraction.ts";

test("the api schema has no numeric constraint and keeps it in the description", () => {
  const sent = forApi(LOCAL_SCHEMA);
  assert.ok(!("minimum" in sent.properties.total) && sent.properties.total.description.includes("minimum 0"));
  assert.equal(LOCAL_SCHEMA.properties.total.minimum, 0);
});

test("a value below the minimum is rejected by the program", () => {
  assert.ok(problems(JSON.parse(body({ total: -5 })), DOC).includes("$.total: must be a number of at least 0"));
});

test("enum capitalisation is normalised before the enum check", () => {
  assert.equal(normaliseEnum({ currency: "Eur" }).currency, "EUR");
  assert.equal(normaliseEnum({ currency: "XYZ" }).currency, "XYZ");
});

test("an invented quotation is sent back and the second reply is accepted", async () => {
  const { fake, client } = clientFor(REPLIES().slice(1, 3));
  const result = await extract(client, DOC);
  assert.ok(result.status === "ok" && result.attempts === 2);
  assert.deepEqual(fake.seen[1].body.messages.map((m: any) => m.role), ["user", "assistant", "user"]);
});

test("a refusal and a cut off reply are not retried", async () => {
  const { fake, client } = clientFor(REPLIES().slice(3, 5));
  assert.equal((await extract(client, DOC)).status, "refused");
  assert.equal((await extract(client, DOC)).status, "truncated");
  assert.equal(fake.seen.length, 2);
});
