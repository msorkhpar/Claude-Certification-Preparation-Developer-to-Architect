// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { mapBounded } from "./bounded.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

const active = { now: 0, peak: 0 };

// A stand-in for one API call: it waits a little and doubles the item, noting how many run at once.
async function work(item: number): Promise<number> {
  active.now++;
  active.peak = Math.max(active.peak, active.now);
  try {
    await new Promise((resolve) => setTimeout(resolve, 20));
    return item * 2;
  } finally {
    active.now--;
  }
}

// Ten items, never more than three at a time.
const outcomes = await mapBounded(Array.from({ length: 10 }, (_, n) => n), work, 3);

console.log("outcomes:", outcomes.length);
console.log("values:", outcomes.flatMap((o) => (o.ok ? [o.value] : [])).sort((a, b) => a - b).join(", "));
console.log("all ok:", outcomes.every((o) => o.ok));
console.log("peak in flight:", active.peak);
