import { test } from "node:test";
import assert from "node:assert/strict";
import { DOORS, send } from "./frontDoors.ts";

test("only the direct API and Bedrock put the model in the body", async () => {
  const models: Record<string, unknown> = {};
  for (const name of Object.keys(DOORS)) models[name] = (await send(name)).seen.body.model ?? null;
  assert.deepEqual(models, { anthropic: "claude-sonnet-5-5", bedrock: "anthropic.claude-sonnet-5-5", vertex: null });
});

test("vertex puts its version in the body and sends no version header", async () => {
  const { seen } = await send("vertex");
  assert.equal(seen.body.anthropic_version, "vertex-2023-10-16");
  assert.equal("anthropic-version" in seen.headers, false);
});

test("the model is in the vertex url", async () => {
  assert.ok((await send("vertex")).seen.url.includes("/publishers/anthropic/models/claude-sonnet-5-5:rawPredict"));
});

test("every door returns the same reply shape", async () => {
  const texts = new Set<string>();
  for (const name of Object.keys(DOORS)) texts.add((await send(name)).json.content[0].text);
  assert.deepEqual([...texts], ["Paris."]);
});
