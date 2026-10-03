import { test } from "node:test";
import assert from "node:assert/strict";
import Anthropic from "@anthropic-ai/sdk";
import { message, scriptedFetch, text } from "../../../harness/ts/scriptedFetch.ts";
import { HALT, MODEL, call, newScreen, perform, runLoop, scaleFor, toScreen } from "./screenLoop.ts";

test("the scale comes from the pixel budget for a large screen and clicks map back", () => {
  const scale = scaleFor(2560, 1440);
  assert.equal(scale.toFixed(4), "0.5585");
  assert.deepEqual([Math.floor(2560 * scale), Math.floor(1440 * scale)], [1429, 804]);
  assert.equal(scaleFor(1280, 720), 1);
  assert.deepEqual(toScreen(0, 0, scale, newScreen()), [0, 0]);
  assert.deepEqual(toScreen(10_000, 10_000, scale, newScreen()), [2559, 1439]);
});

test("a risky click is declined unless a person says yes", () => {
  const screen = newScreen();
  const scale = scaleFor(2560, 1440);
  const click = { coordinate: [Math.round(2150 * scale), Math.round(1240 * scale)] };
  assert.deepEqual(perform(screen, "left_click", click, scale), ["Declined: buy needs a person's confirmation (payment)", true]);
  assert.equal(perform(screen, "left_click", click, scale, (a) => a.risk === "payment")[1], false);
  assert.deepEqual(screen.log, [["click", "buy"]]);
});

test("a failure halts the rest of its batch with the documented text", async () => {
  const screen = newScreen();
  const fake = scriptedFetch([
    { body: message([call("t1", "left_click", { coordinate: [900, 900] }), call("t2", "mystery"), call("t3", "type", { text: "x" })], "tool_use", undefined, MODEL) },
    { body: message([text("ok")], "end_turn", undefined, MODEL) },
  ]);
  const client = new Anthropic({ apiKey: "placeholder", maxRetries: 0, fetch: fake.fetch });
  assert.equal((await runLoop(client, screen)).status, "done");
  const results = fake.seen[1].body.messages.at(-1).content;
  assert.deepEqual(results.map((r: any) => !!r.is_error), [false, true, true]);
  assert.equal(results[2].content, HALT);
  assert.equal(screen.typed, "");
  assert.ok(results.every((r: any) => r.toolset_name === "computer"));
});
