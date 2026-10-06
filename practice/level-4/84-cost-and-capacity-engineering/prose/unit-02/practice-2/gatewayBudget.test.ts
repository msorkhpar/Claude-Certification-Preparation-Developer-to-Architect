import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "gatewayBudget.ts")).href);

const POLICY = { allowed: ["haiku", "sonnet"], routes: { classify: "haiku", draft: "sonnet", review: "opus" }, default: "sonnet", cheaper: { opus: "sonnet", sonnet: "haiku" } };
const PRICES = { haiku: { input: 100, cache_read: 10, output: 500 }, sonnet: { input: 200, cache_read: 20, output: 1000 } };

const route = (request: Record<string, unknown>, status = "allow") => {
  const result = solution.route(request, POLICY, status);
  assert.ok(result === null || typeof result === "string", "route returned something that is not a model name");
  return result;
};
const admit = (spend: number, budget: number, estimate: number) => {
  const result = solution.admit(spend, budget, estimate);
  assert.ok(typeof result === "string", "admit returned nothing");
  return result;
};
const showback = (rows: unknown[]) => {
  const result = solution.showback(rows, PRICES);
  assert.ok(Array.isArray(result), "showback returned nothing");
  return result;
};

test("m1 a request follows the route table of the gateway", () => {
  assert.deepEqual(["classify", "draft", "review"].map((task) => route({ task })), ["haiku", "sonnet", "opus"]);
  assert.equal(route({ task: "translate" }), "sonnet");
});

test("e1 a team near its budget is moved to a cheaper model and a team over it is refused", () => {
  assert.equal(route({ task: "review" }, "warn"), "sonnet");
  assert.equal(route({ task: "draft" }, "warn"), "haiku");
  assert.equal(route({ task: "classify" }, "warn"), "haiku");
  assert.equal(route({ task: "review" }, "block"), null);
});

test("e2 a request is admitted warned or blocked against the budget", () => {
  assert.equal(admit(0, 1000, 100), "allow");
  assert.equal(admit(700, 1000, 99), "allow");
  assert.equal(admit(700, 1000, 100), "warn");
  assert.equal(admit(900, 1000, 100), "warn");
  assert.equal(admit(900, 1000, 101), "block");
  assert.equal(admit(0, 0, 0), "block");
  assert.equal(admit(0, -5, 0), "block");
});

test("e3 showback adds each teams tokens at the price of the model and refuses an unknown model", () => {
  const rows = [{ team: "a", model: "sonnet", input: 1_000_000, cache_read: 5_000_000, output: 100_000 },
    { team: "b", model: "haiku", input: 2_000_000, cache_read: 0, output: 1_000_000 },
    { team: "a", model: "haiku", input: 500_000, cache_read: 0, output: 0 }];
  assert.deepEqual(showback(rows), [{ team: "b", cents: 700 }, { team: "a", cents: 450 }]);
  assert.equal(showback([{ team: "b", model: "sonnet", input: 1, cache_read: 0, output: 0 }, { team: "a", model: "sonnet", input: 1, cache_read: 0, output: 0 }])[0].team, "a");
  assert.throws(() => solution.showback([{ team: "a", model: "other", input: 1, cache_read: 0, output: 0 }], PRICES), /unknown model: other/);
  assert.deepEqual(showback([]), []);
});

test("e4 showback rounds each teams total to a cent once", () => {
  const row = (team: string, tokens: number) => ({ team, model: "sonnet", input: tokens, cache_read: 0, output: 0 });
  assert.deepEqual(showback([row("x", 2000), row("x", 2000), row("y", 2500), row("z", 2499)]), [{ team: "x", cents: 1 }, { team: "y", cents: 1 }, { team: "z", cents: 0 }]);
});

test("e5 a caller with a hard latency limit gets accept and poll when the slow case does not fit", () => {
  const pick = (p95: number, timeout: number, margin: number) => {
    const result = solution.delivery(p95, timeout, margin);
    assert.ok(typeof result === "string", "delivery returned nothing");
    return result;
  };
  assert.equal(pick(8, 10, 25), "sync");
  assert.equal(pick(8, 10, 26), "accept-and-poll");
  assert.equal(pick(30, 10, 0), "accept-and-poll");
  assert.equal(pick(10, 10, 0), "sync");
  assert.equal(pick(10, 10, 1), "accept-and-poll");
});

test("e6 a model pinned by a team is honoured only when the policy allows it", () => {
  assert.equal(route({ task: "classify", model: "sonnet" }), "sonnet");
  assert.equal(route({ task: "classify", model: "opus" }), "haiku");
  assert.equal(route({ task: "review", model: "haiku" }), "haiku");
  assert.equal(route({ task: "draft", model: "sonnet" }, "warn"), "haiku");
});
