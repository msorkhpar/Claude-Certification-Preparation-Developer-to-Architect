import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "promptPlan.ts")).href);

const ROLE = { name: "role", static: true, text: "r".repeat(400) }; // 100 tokens
const POLICY = { name: "policy", static: true, text: "p".repeat(1648) }; // 412 tokens: the prefix is exactly 512
const HISTORY = { name: "history", static: false, priority: 1, text: "h".repeat(200) }; // 50 tokens
const QUESTION = { name: "question", static: false, priority: 9, text: "Q: {q}" };
const EXTRA = { name: "extra", static: false, priority: 1, text: "e".repeat(160) }; // 40 tokens

const assemble = (modules: unknown[], variables: Record<string, string>, budget = 10_000) => {
  const result = solution.assemble(modules, variables, budget);
  assert.ok(result !== null && typeof result === "object", "assemble returned nothing");
  return result;
};
const names = (prompt: any) => prompt.blocks.map((b: any) => b.name);
const refused = (modules: unknown[], variables: Record<string, string>, budget = 10_000): string => {
  try {
    solution.assemble(modules, variables, budget);
  } catch (error) {
    if (error instanceof Error) return error.message;
    throw error;
  }
  assert.fail("the modules were accepted");
};

test("m1 static modules come first and the breakpoint follows the last one", () => {
  const prompt = assemble([QUESTION, ROLE, HISTORY, POLICY], { q: "hello" });
  assert.deepEqual(names(prompt), ["role", "policy", "question", "history"]);
  assert.deepEqual([prompt.breakpoint, prompt.tokens, prompt.dropped], [1, 564, []]);
});

test("e1 a variable in a static module is refused", () => {
  assert.match(refused([ROLE, { ...POLICY, text: "policy for {customer}" }, QUESTION], { q: "x", customer: "Ana" }), /static/);
});

test("e2 dynamic variables are filled and a missing one is refused", () => {
  const prompt = assemble([ROLE, POLICY, QUESTION], { q: "hello", unused: "x" });
  assert.equal(prompt.blocks[2].text, "Q: hello");
  assert.match(refused([ROLE, POLICY, QUESTION], {}), /missing variable: q/);
});

test("e3 the lowest priority dynamic module is dropped first and a tie drops the later one", () => {
  const modules = [ROLE, POLICY, HISTORY, QUESTION, EXTRA];
  const one = assemble(modules, { q: "hello" }, 570);
  assert.deepEqual(one.dropped, ["extra"]);
  assert.deepEqual(names(one), ["role", "policy", "history", "question"]);
  assert.equal(one.tokens, 564);
  const two = assemble(modules, { q: "hello" }, 520);
  assert.deepEqual(two.dropped, ["extra", "history"]);
  assert.deepEqual(names(two), ["role", "policy", "question"]);
  assert.equal(two.tokens, 514);
});

test("e4 static modules are never dropped and a budget they exceed is refused", () => {
  assert.match(refused([ROLE, POLICY, HISTORY, QUESTION], { q: "hello" }, 400), /over budget/);
  assert.match(refused([ROLE, POLICY], {}, 511), /over budget/);
  assert.equal(assemble([ROLE, POLICY], {}, 512).tokens, 512);
});

test("e5 a prefix under the minimum gets no breakpoint", () => {
  assert.equal(solution.MIN_CACHEABLE, 512);
  assert.equal(assemble([ROLE, POLICY], {}).breakpoint, 1);
  assert.equal(assemble([ROLE, { ...POLICY, text: "p".repeat(1644) }], {}).breakpoint, null);
  assert.equal(assemble([HISTORY, QUESTION], { q: "x" }).breakpoint, null);
});

const MODELS = [
  { name: "small", tier: 1, latency_ms: 300, price_out: 1 },
  { name: "mid2", tier: 2, latency_ms: 900, price_out: 5 },
  { name: "big", tier: 3, latency_ms: 2500, price_out: 25 },
  { name: "mid", tier: 2, latency_ms: 900, price_out: 5 },
];

test("e6 the cheapest model that meets the tier and the latency wins and ties go by name", () => {
  const pick = (tier: number, latency: number) => {
    const result = solution.chooseModel({ tier, max_latency_ms: latency }, MODELS);
    assert.ok(result === null || typeof result === "string", "chooseModel returned something that is not a name");
    return result;
  };
  assert.equal(pick(1, 5000), "small");
  assert.equal(pick(2, 1000), "mid");
  assert.equal(pick(3, 5000), "big");
  assert.equal(pick(3, 1000), null);
  assert.equal(pick(2, 500), null);
  assert.equal(pick(1, 100), null);
});

test("e7 only an identical static prefix can be reused", () => {
  const reuse = (a: unknown, b: unknown) => {
    const result = solution.reusablePrefix(a, b);
    assert.ok(typeof result === "number", "reusablePrefix returned nothing");
    return result;
  };
  const base = assemble([ROLE, POLICY, HISTORY, QUESTION], { q: "one" });
  assert.equal(reuse(base, assemble([ROLE, POLICY, HISTORY, QUESTION], { q: "a different question" })), 512);
  assert.equal(reuse(base, assemble([{ ...ROLE, text: "R".repeat(400) }, POLICY, HISTORY, QUESTION], { q: "one" })), 0);
  assert.equal(reuse(base, assemble([ROLE, { ...POLICY, text: "P".repeat(1648) }, HISTORY, QUESTION], { q: "one" })), 0);
  assert.equal(reuse(base, assemble([ROLE, POLICY, { ...HISTORY, text: "other" }, QUESTION], { q: "one" })), 512);
  assert.equal(reuse(base, assemble([HISTORY, QUESTION], { q: "one" })), 0);
  assert.equal(reuse(assemble([HISTORY], {}), assemble([HISTORY], {})), 0);
});
