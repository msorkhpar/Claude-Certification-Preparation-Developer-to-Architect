import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { RejectedRequest, buildParams } = await import(pathToFileURL(resolve(dir, "params.ts")).href);

const FABLE = "claude-fable-5-1", OPUS = "claude-opus-5-5", SONNET = "claude-sonnet-5-5", HAIKU = "claude-haiku-4-5-20251001";

/** The parameter named by the RejectedRequest, or null when the request was accepted. */
function refused(model: string, options: Record<string, unknown> = {}, maxTokens = 4096): string | null {
  try {
    buildParams(model, maxTokens, options);
  } catch (err) {
    return err instanceof RejectedRequest ? err.param : `crash: ${err}`;
  }
  return null;
}

test("m1 each course model gets the request it accepts", () => {
  assert.deepEqual(buildParams(FABLE, 8000, { effort: "xhigh" }), { model: FABLE, max_tokens: 8000, output_config: { effort: "xhigh" } });
  assert.deepEqual(buildParams(OPUS, 4096), { model: OPUS, max_tokens: 4096 });
  assert.deepEqual(buildParams(SONNET, 4096, { thinking: { type: "adaptive" }, effort: "medium" }), {
    model: SONNET, max_tokens: 4096, thinking: { type: "adaptive" }, output_config: { effort: "medium" } });
  assert.deepEqual(buildParams(HAIKU, 4096, { thinking: { type: "enabled", budget_tokens: 2048 } }), {
    model: HAIKU, max_tokens: 4096, thinking: { type: "enabled", budget_tokens: 2048 } });
});

test("e1 thinking modes a model does not have are refused", () => {
  for (const model of [FABLE, OPUS, SONNET]) assert.equal(refused(model, { thinking: { type: "disabled" } }), "thinking", model);
  for (const model of [OPUS, SONNET, FABLE]) assert.equal(refused(model, { thinking: { type: "enabled", budget_tokens: 2048 } }), "thinking", model);
  assert.equal(refused(HAIKU, { thinking: { type: "adaptive" } }), "thinking");
  assert.equal(refused(HAIKU, { thinking: { type: "disabled" } }), null);
  assert.equal(refused(OPUS, { thinking: { type: "adaptive" } }), null);
});

test("e2 the mode that skips thinking up front is sonnet only and needs high effort or below", () => {
  assert.deepEqual((buildParams(SONNET, 4096, { thinking: { type: "between_tools" } }) ?? {}).thinking, { type: "between_tools" });
  for (const level of ["low", "medium", "high"]) assert.equal(refused(SONNET, { thinking: { type: "between_tools" }, effort: level }), null, level);
  for (const level of ["xhigh", "max"]) assert.equal(refused(SONNET, { thinking: { type: "between_tools" }, effort: level }), "thinking", level);
  assert.equal(refused(OPUS, { thinking: { type: "between_tools" } }), "thinking");
  assert.equal(refused(HAIKU, { thinking: { type: "between_tools" } }), "thinking");
});

test("e3 effort needs a supporting model and a real level", () => {
  assert.equal(refused(HAIKU, { effort: "low" }), "output_config.effort");
  for (const level of ["low", "medium", "high", "xhigh", "max"]) {
    assert.equal(refused(SONNET, { effort: level }), null, level);
    assert.equal(refused(FABLE, { effort: level }), null, level);
  }
  assert.equal(refused(OPUS, { effort: "adaptive" }), "output_config.effort");
  assert.equal(refused(OPUS, { effort: "extreme" }), "output_config.effort");
});

test("e4 newer models reject sampling parameters and haiku keeps them", () => {
  assert.equal(refused(OPUS, { temperature: 0.2 }), "temperature");
  assert.equal(refused(SONNET, { top_p: 0.9 }), "top_p");
  assert.equal(refused(FABLE, { top_k: 40 }), "top_k");
  assert.equal(refused(OPUS, { temperature: 1.0 }), null);
  assert.deepEqual(buildParams(HAIKU, 1024, { temperature: 0.2, top_k: 40 }), { model: HAIKU, max_tokens: 1024, temperature: 0.2, top_k: 40 });
});

test("e5 fast mode is opus only with its beta header and never in a batch", () => {
  const params = buildParams(OPUS, 4096, { speed: "fast" }) ?? {};
  assert.deepEqual([params.speed, params.betas], ["fast", ["fast-mode-2026-02-01"]]);
  assert.equal(refused(SONNET, { speed: "fast" }), "speed");
  assert.equal(refused(HAIKU, { speed: "fast" }), "speed");
  assert.equal(refused(OPUS, { speed: "fast", batch: true }), "speed");
  assert.equal("speed" in (buildParams(OPUS, 4096, { speed: "standard" }) ?? { speed: null }), false);
});

test("e6 a manual budget is at least 1024 and below max tokens", () => {
  assert.equal(refused(HAIKU, { thinking: { type: "enabled", budget_tokens: 1023 } }, 4096), "thinking.budget_tokens");
  assert.equal(refused(HAIKU, { thinking: { type: "enabled", budget_tokens: 1024 } }, 2048), null);
  assert.equal(refused(HAIKU, { thinking: { type: "enabled", budget_tokens: 2048 } }, 2048), "thinking.budget_tokens");
  assert.equal(refused(HAIKU, { thinking: { type: "enabled" } }, 2048), "thinking.budget_tokens");
  assert.equal(refused(OPUS, {}, 0), "max_tokens");
});

test("e7 effort lives in output config and never inside thinking", () => {
  const params = buildParams(SONNET, 4096, { thinking: { type: "adaptive" }, effort: "high" }) ?? {};
  assert.deepEqual(params.thinking, { type: "adaptive" });
  assert.deepEqual(params.output_config, { effort: "high" });
  const options = { thinking: { type: "adaptive" }, effort: "low" };
  buildParams(SONNET, 4096, options);
  assert.deepEqual(options, { thinking: { type: "adaptive" }, effort: "low" });
});
