import { test } from "node:test";
import assert from "node:assert/strict";
import { MODEL, REPLIES, clientFor, loop, runTool } from "./toolLoop.ts";
import { message, text } from "../../../harness/ts/scriptedFetch.ts";

test("a failing call becomes an error result with a hint", () => {
  const result: any = runTool({ id: "t1", name: "get_weather", input: { city: "Atlantis" } });
  assert.ok(result.is_error === true && result.content.includes("Known cities"));
});

test("all results go back in one user message in call order", async () => {
  const { fake, client } = clientFor(REPLIES());
  const { reply, messages } = await loop(client, "q");
  assert.deepEqual(messages.map((m) => m.role), ["user", "assistant", "user", "assistant"]);
  assert.deepEqual((messages[2].content as any[]).map((r) => r.tool_use_id), ["toolu_01", "toolu_02", "toolu_03"]);
  assert.ok(reply.stop_reason === "end_turn" && fake.seen.length === 2);
});

test("a forced choice is sent once", async () => {
  const { fake, client } = clientFor([
    { body: message([{ type: "tool_use", id: "toolu_01", name: "get_time", input: { city: "Oslo" } }], "tool_use", { input_tokens: 1, output_tokens: 1 }, MODEL) },
    { body: message([text("09:15")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) },
  ]);
  await loop(client, "time?", { type: "tool", name: "get_time" });
  assert.deepEqual(fake.seen.map((r) => r.body.tool_choice), [{ type: "tool", name: "get_time" }, undefined]);
});
