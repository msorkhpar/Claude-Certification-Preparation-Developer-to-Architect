import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { PlanError, planRequest } = await import(pathToFileURL(resolve(dir, "cacheplan.ts")).href);

const block = (id: string, section: string, tokens: number, extra: Record<string, unknown> = {}) => ({ id, section, tokens, ...extra });
const ids = (plan: any) => (plan ?? []).map((p: any) => p.id);
const caches = (plan: any) => Object.fromEntries((plan ?? []).map((p: any) => [p.id, p.cache]));
const values = (plan: any) => (plan ?? []).map((p: any) => p.cache);
const raised = (blocks: any[], minTokens?: number) => {
  try {
    planRequest(blocks, minTokens);
  } catch (err) {
    return err;
  }
  return null;
};

test("m1 stable content comes first and the volatile date goes last", () => {
  const blocks = [
    block("date", "system", 20, { volatile: true }),
    block("tools", "tools", 2000),
    block("rules", "system", 3000, { breakpoint: true }),
    block("manual", "messages", 6000, { breakpoint: true }),
    block("question", "messages", 40),
  ];
  const plan = planRequest(blocks, 1024);
  assert.deepEqual(ids(plan), ["tools", "rules", "manual", "question", "date"]);
  assert.deepEqual(caches(plan), { tools: null, rules: "5m", manual: "5m", question: null, date: null });
});

test("e1 sections follow the prefix order and keep their own order", () => {
  const blocks = [block("m1", "messages", 10), block("s1", "system", 10), block("t1", "tools", 10), block("s2", "system", 10), block("t2", "tools", 10), block("m2", "messages", 10)];
  assert.deepEqual(ids(planRequest(blocks)), ["t1", "t2", "s1", "s2", "m1", "m2"]);
});

test("e2 a breakpoint needs the stable prefix to reach the minimum", () => {
  const blocks = [block("tools", "tools", 400, { breakpoint: true }), block("rules", "system", 500, { breakpoint: true }), block("doc", "messages", 700, { breakpoint: true }), block("ask", "messages", 30)];
  assert.deepEqual(caches(planRequest(blocks, 1024)), { tools: null, rules: null, doc: "5m", ask: null });
  assert.deepEqual(caches(planRequest(blocks, 4096)), { tools: null, rules: null, doc: null, ask: null });
  // volatile tokens come after the prefix, so they never help it reach the minimum
  const padded = [block("stamp", "system", 5000, { volatile: true }), block("rules", "system", 500, { breakpoint: true })];
  assert.deepEqual(caches(planRequest(padded, 1024)), { rules: null, stamp: null });
});

test("e3 at most four breakpoints are sent", () => {
  const five = Array.from({ length: 5 }, (_, i) => block(`b${i}`, "messages", 2000, { breakpoint: true }));
  assert.ok(raised(five, 1024) instanceof PlanError);
  assert.deepEqual(values(planRequest(five.slice(0, 4), 1024)), ["5m", "5m", "5m", "5m"]);
  // two of the five never reach the minimum, so only three breakpoints are sent
  const small = [block("a", "tools", 10, { breakpoint: true }), block("b", "system", 10, { breakpoint: true }), ...five.slice(0, 3)];
  assert.deepEqual(values(planRequest(small, 1024)), [null, null, "5m", "5m", "5m"]);
});

test("e4 a one hour breakpoint may not follow a five minute one", () => {
  const longFirst = [block("docs", "system", 3000, { breakpoint: true, ttl: "1h" }), block("turns", "messages", 3000, { breakpoint: true })];
  assert.deepEqual(caches(planRequest(longFirst, 1024)), { docs: "1h", turns: "5m" });
  const wrongWay = [block("turns", "system", 3000, { breakpoint: true }), block("docs", "messages", 3000, { breakpoint: true, ttl: "1h" })];
  assert.ok(raised(wrongWay, 1024) instanceof PlanError);
});

test("e5 volatile blocks never carry a breakpoint and tools cannot be volatile", () => {
  const blocks = [block("rules", "system", 3000, { breakpoint: true }), block("stamp", "messages", 3000, { volatile: true, breakpoint: true })];
  assert.deepEqual(caches(planRequest(blocks, 1024)), { rules: "5m", stamp: null });
  assert.ok(raised([block("t", "tools", 3000, { volatile: true })]) instanceof PlanError);
});

test("e6 every block comes out once and the input is not changed", () => {
  const blocks = [block("date", "system", 20, { volatile: true }), block("a", "messages", 2000, { breakpoint: true }), block("t", "tools", 2000)];
  const before = structuredClone(blocks);
  const plan = planRequest(blocks, 1024);
  assert.deepEqual(ids(plan).sort(), ["a", "date", "t"]);
  assert.deepEqual(blocks, before);
});
