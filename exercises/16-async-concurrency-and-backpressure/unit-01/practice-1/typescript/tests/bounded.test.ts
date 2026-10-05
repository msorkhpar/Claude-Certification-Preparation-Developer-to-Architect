import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { mapBounded } = await import(pathToFileURL(resolve(dir, "bounded.ts")).href);

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/** An async work function that records how many calls are in flight at once. */
function probe(delay = 20) {
  const state = { active: 0, peak: 0, started: 0 };
  const work = async (item: number) => {
    state.started++;
    state.active++;
    state.peak = Math.max(state.peak, state.active);
    try {
      await sleep(delay);
      return item * 2;
    } finally {
      state.active--;
    }
  };
  return { state, work };
}

test("m1 results come back for every item and never more than limit run at once", async () => {
  const p = probe();
  const outcomes = await mapBounded(Array.from({ length: 10 }, (_, i) => i), p.work, 3);
  assert.deepEqual(outcomes.map((o: any) => o.value).sort((a: number, b: number) => a - b), Array.from({ length: 10 }, (_, i) => i * 2)); // the order is the point of e1 alone
  assert.ok(outcomes.every((o: any) => o.ok));
  assert.equal(p.state.peak, 3);
  assert.equal(p.state.started, 10);
});

test("e1 results keep the input order even when later items finish first", async () => {
  const slowFirst = async (item: number) => {
    await sleep(item === 0 ? 50 : 1);
    return item;
  };
  const outcomes = await mapBounded([0, 1, 2, 3, 4], slowFirst, 5);
  assert.deepEqual(outcomes.map((o: any) => o.value), [0, 1, 2, 3, 4]);
});

test("e2 a failing item is reported and the others still finish", async () => {
  const sometimes = async (item: number) => {
    if (item === 2) throw new Error("boom");
    await sleep(5);
    return item;
  };
  let outcomes: any[] = [];
  try {
    outcomes = await mapBounded([0, 1, 2, 3, 4], sometimes, 2);
  } catch (err) {
    assert.fail(`the run rejected: ${err}`);
  }
  const failed = outcomes.filter((o) => !o.ok);
  assert.equal(outcomes.length, 5);
  assert.equal(failed.length, 1);
  assert.ok(failed[0].error instanceof Error && failed[0].error.message === "boom");
  assert.deepEqual(outcomes.filter((o) => o.ok).map((o) => o.value).sort((a, b) => a - b), [0, 1, 3, 4]);
});

test("e3 a limit above the item count and an empty input both work", async () => {
  const p = probe();
  const outcomes = await mapBounded([1, 2, 3], p.work, 50);
  assert.deepEqual(outcomes.map((o: any) => o.value).sort((a: number, b: number) => a - b), [2, 4, 6]);
  assert.equal(p.state.peak, 3);
  assert.deepEqual(await mapBounded([], probe().work, 4), []);
});

test("e4 a limit below one is refused", async () => {
  for (const bad of [0, -1]) {
    let outcome = "accepted";
    try {
      await mapBounded([1], probe().work, bad);
    } catch (err) {
      outcome = err instanceof Error ? "refused" : "odd";
    }
    assert.equal(outcome, "refused", `limit ${bad}`);
  }
});

test("e5 items are pulled lazily so a slow consumer holds the producer back", async () => {
  const pulled: number[] = [];
  function* source() {
    for (let n = 0; n < 20; n++) {
      pulled.push(n);
      yield n;
    }
  }
  let open!: () => void;
  const gate = new Promise<void>((r) => (open = r));
  const blocked = async (item: number) => {
    await gate;
    return item;
  };
  const run = mapBounded(source(), blocked, 2);
  await sleep(50);
  const held = pulled.length; // every worker is blocked: only `limit` items may have been taken
  open();
  const outcomes = await run;
  assert.equal(held, 2, `${held} items were pulled while 2 workers were blocked`);
  assert.deepEqual(outcomes.map((o: any) => o.value).sort((a: number, b: number) => a - b), Array.from({ length: 20 }, (_, i) => i));
});
