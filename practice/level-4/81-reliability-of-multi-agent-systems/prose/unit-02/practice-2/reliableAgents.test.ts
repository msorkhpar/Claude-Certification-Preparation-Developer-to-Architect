import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const solution = await import(pathToFileURL(resolve(dir, "reliableAgents.ts")).href);
const { Fatal, Transient } = solution;

type Agent = (key: string, inputs: Record<string, string>) => string;
const runPlan = (plan: unknown[], agents: Record<string, Agent>, store: Record<string, string>, attempts = 3, breakerThreshold = 3) => {
  const result = solution.runPlan(plan, agents, store, attempts, breakerThreshold);
  assert.ok(result !== null && typeof result === "object", "runPlan returned nothing");
  return result;
};

const task = (tid: string, agent = "w", needs: string[] = [], fallback: string | null = null) => ({ id: tid, agent, key: `key-${tid}`, needs, fallback });
const echo: Agent = (key, inputs) => key + "|" + Object.keys(inputs).sort().map((k) => inputs[k]).join(",");

test("m1 tasks run in order and receive the results of the tasks they need", () => {
  const store: Record<string, string> = {};
  const result = runPlan([task("a"), task("b", "w", ["a"]), task("c", "w", ["a", "b"])], { w: echo }, store);
  assert.deepEqual(result.done, { a: "key-a|", b: "key-b|key-a|", c: "key-c|key-a|,key-b|key-a|" });
  assert.deepEqual(result.attempts, { a: 1, b: 1, c: 1 });
  assert.deepEqual([result.failed, result.skipped, result.degraded, result.resumed], [{}, {}, [], []]);
  assert.deepEqual(store, result.done);
});

test("e1 every retry of a task carries the same idempotency key", () => {
  const seen: string[] = [];
  const flaky: Agent = (key) => {
    seen.push(key);
    if (seen.length < 3) throw new Transient("lost");
    return "ok";
  };
  const result = runPlan([task("t")], { w: flaky }, {});
  assert.deepEqual(seen, ["key-t", "key-t", "key-t"]);
  assert.deepEqual(result.done, { t: "ok" });
  assert.deepEqual(result.attempts, { t: 3 });
});

test("e2 retries stop at the limit and a fatal failure is not retried", () => {
  const down: Agent = () => { throw new Transient("down"); };
  const bad: Agent = () => { throw new Fatal("bad input"); };
  const result = runPlan([task("a"), task("b", "x")], { w: down, x: bad }, {}, 3, 99);
  assert.deepEqual(result.failed, { a: "retries exhausted", b: "fatal: bad input" });
  assert.deepEqual(result.attempts, { a: 3, b: 1 });
});

test("e3 a failure stays inside its branch and dependents are skipped", () => {
  const agent: Agent = (key) => {
    if (key === "key-a") throw new Fatal("boom");
    return "ok";
  };
  const result = runPlan([task("a"), task("b", "w", ["a"]), task("c"), task("d", "w", ["b"])], { w: agent }, {});
  assert.deepEqual(result.done, { c: "ok" });
  assert.deepEqual(result.failed, { a: "fatal: boom" });
  assert.deepEqual(result.skipped, { b: "dependency failed: a", d: "dependency failed: b" });
  assert.deepEqual(result.attempts, { a: 1, b: 0, c: 1, d: 0 });
});

test("e4 a breaker stops calls to an agent that keeps failing and a success resets it", () => {
  const calls: string[] = [];
  const flaky: Agent = (key) => {
    calls.push(key);
    throw new Transient("down");
  };
  const good: Agent = () => "fine";
  const plan = [task("t1", "flaky"), task("t2", "flaky"), task("t3", "flaky"), task("t4", "good")];
  const result = runPlan(plan, { flaky, good }, {}, 2, 3);
  assert.deepEqual(result.failed, { t1: "retries exhausted", t2: "circuit open", t3: "circuit open" });
  assert.deepEqual(result.done, { t4: "fine" });
  assert.deepEqual(result.attempts, { t1: 2, t2: 1, t3: 0, t4: 1 });
  assert.equal(calls.length, 3);

  const count: string[] = [];
  const everyOther: Agent = (key) => {
    count.push(key);
    if (count.length % 2 === 1) throw new Transient("blip");
    return "ok";
  };
  const healthy = runPlan([task("a", "x"), task("b", "x")], { x: everyOther }, {}, 2, 2);
  assert.deepEqual(healthy.done, { a: "ok", b: "ok" });
  assert.deepEqual(healthy.failed, {});
});

test("e5 a fallback degrades one task with its own key and is not checkpointed", () => {
  const seen: string[] = [];
  const primary: Agent = () => { throw new Fatal("down"); };
  const backup: Agent = (key) => {
    seen.push(key);
    return "from backup";
  };
  const store: Record<string, string> = {};
  const plan = [task("s", "primary", [], "backup"), task("t", "use", ["s"])];
  const result = runPlan(plan, { primary, backup, use: (_key, inputs) => "got " + inputs.s }, store);
  assert.deepEqual(result.done, { s: "from backup", t: "got from backup" });
  assert.deepEqual(result.degraded, ["s"]);
  assert.deepEqual(result.failed, {});
  assert.deepEqual(seen, ["key-s:fallback"]);
  assert.deepEqual(Object.keys(store), ["t"]);
  const bothDown = runPlan([task("s", "primary", [], "primary")], { primary }, {});
  assert.deepEqual(bothDown.failed, { s: "fatal: down" });
  assert.deepEqual(bothDown.degraded, []);
});

test("e6 a second run resumes from the checkpoint and retries only what failed", () => {
  const calls: string[] = [];
  let healthy = false;
  const agent: Agent = (key) => {
    calls.push(key);
    if (key === "key-b" && !healthy) throw new Transient("down");
    return "ok-" + key;
  };
  const store: Record<string, string> = {};
  const plan = [task("a"), task("b", "w", ["a"])];
  const first = runPlan(plan, { w: agent }, store, 2, 99);
  assert.deepEqual(first.failed, { b: "retries exhausted" });
  assert.deepEqual(Object.keys(store), ["a"]);
  healthy = true;
  const second = runPlan(plan, { w: agent }, store, 2, 99);
  assert.deepEqual(second.resumed, ["a"]);
  assert.deepEqual(second.done, { a: "ok-key-a", b: "ok-key-b" });
  assert.deepEqual(second.attempts, { a: 0, b: 1 });
  assert.equal(calls.filter((k) => k === "key-a").length, 1);
});

test("e7 an unexpected crash is not swallowed and keeps the work already checkpointed", () => {
  const agent: Agent = (key) => {
    if (key === "key-b") throw new Error("process died");
    return "ok";
  };
  const store: Record<string, string> = {};
  assert.throws(() => runPlan([task("a"), task("b"), task("c")], { w: agent }, store), /process died/);
  assert.deepEqual(store, { a: "ok" });
});
