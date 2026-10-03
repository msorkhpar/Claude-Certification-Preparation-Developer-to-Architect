import { test } from "node:test";
import assert from "node:assert/strict";
import { MODEL, byFixedCount, byStopReason, byTextMarker, clientFor, scenarioA, scenarioB } from "./loops.ts";
import { message, text } from "../../../harness/ts/scriptedFetch.ts";

test("the stop_reason loop runs the tool of a reply that says done", async () => {
  assert.deepEqual(await byStopReason(clientFor(scenarioA()).client, "t"), ["done", 2, ["save_file"], "Saved report.txt."]);
});

test("the text marker loop stops before the tool runs", async () => {
  const [status, calls, ran] = await byTextMarker(clientFor(scenarioA()).client, "t");
  assert.deepEqual([status, calls, ran], ["done", 1, []]);
});

test("a backstop below the real need gets its own status", async () => {
  const [status, calls, ran] = await byStopReason(clientFor(scenarioB()).client, "t", 3);
  assert.deepEqual([status, calls, ran.length], ["max_turns", 3, 3]);
});

test("a backstop above the need is never reached", async () => {
  assert.deepEqual((await byStopReason(clientFor(scenarioB()).client, "t", 10)).slice(0, 2), ["done", 5]);
});

test("the fixed count loop reports done without a final answer", async () => {
  const [status, , ran, answer] = await byFixedCount(clientFor(scenarioB()).client, "t");
  assert.deepEqual([status, ran.length, answer], ["done", 3, ""]);
});

test("an end turn that announces a tool call is still the end", async () => {
  const { fake, client } = clientFor([{ body: message([text("Let me call the lookup tool next.")], "end_turn", { input_tokens: 1, output_tokens: 1 }, MODEL) }]);
  assert.deepEqual((await byStopReason(client, "t")).slice(0, 2), ["done", 1]);
  assert.equal(fake.seen.length, 1);
});
