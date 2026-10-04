import { test } from "node:test";
import assert from "node:assert/strict";
import { CACHED, TIERS, UNCACHED, dollars, monthlyCents, requiredCapacity, smallestTier } from "./capacityModel.ts";

test("cache reads do not count toward the input limit", () => {
  assert.deepEqual(requiredCapacity(CACHED, 0), { rpm: 800, itpm: 800 * 1700, otpm: 320_000 });
});

test("headroom rounds every figure up", () => {
  assert.deepEqual(requiredCapacity({ rpm: 1, input: 1, cache_write: 0, cache_read: 0, output: 1 }, 30), { rpm: 2, itpm: 2, otpm: 2 });
  assert.deepEqual(requiredCapacity(CACHED, 30), { rpm: 1040, itpm: 1_768_000, otpm: 416_000 });
});

test("the tier must cover all three limits and custom is the last resort", () => {
  assert.equal(smallestTier({ rpm: 100, itpm: 100, otpm: 100 }, TIERS), "Start");
  assert.equal(smallestTier({ rpm: 100, itpm: 100, otpm: 400_001 }, TIERS), "Build");
  assert.equal(smallestTier({ rpm: 10_001, itpm: 1, otpm: 1 }, TIERS), "Custom");
  assert.equal(smallestTier(requiredCapacity(UNCACHED, 30), TIERS), "Scale");
});

test("the monthly bill counts every token kind and batch is half price", () => {
  assert.equal(monthlyCents(CACHED, 2_000_000, 0), 1_740_000);
  assert.equal(monthlyCents(UNCACHED, 2_000_000, 0), 3_880_000);
  assert.equal(monthlyCents(CACHED, 2_000_000, 100), 870_000);
  assert.equal(monthlyCents(CACHED, 2_000_000, 30), 1_479_000);
});

test("dollars prints cents with a thousands separator", () => {
  assert.deepEqual([dollars(1_740_000), dollars(5), dollars(100)], ["$17,400.00", "$0.05", "$1.00"]);
});
