// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { abVerdict, chooseModel, segmentTable, shadowGate } from "./evalkit.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Results of an evaluation as [segment, correct] rows; a wrong refund costs more than a wrong order status.
const costs = { "order status": 1, refund: 20, policy: 5 };
const results: Array<[string, boolean]> = [
  ...Array.from({ length: 30 }, (): [string, boolean] => ["order status", true]),
  ...Array.from({ length: 5 }, (): [string, boolean] => ["refund", true]),
  ...Array.from({ length: 3 }, (): [string, boolean] => ["refund", false]),
  ...Array.from({ length: 9 }, (): [string, boolean] => ["policy", true]),
  ["policy", false],
];
console.log("segments:", JSON.stringify(segmentTable(results, costs)));

// The same cases under the old and the new prompt: a gain in one segment must not hide a loss in a protected one.
const pairs: Array<[string, boolean, boolean]> = [["refund", true, false], ["policy", false, true], ["policy", false, true], ["order status", true, true]];
console.log("shadow gate:", JSON.stringify(shadowGate(pairs, ["refund"])));
console.log("A/B verdict:", abVerdict(100, 200, 160, 200));

// The cheapest model that is accurate and fast enough: [name, accuracy, p95 latency, cost].
console.log("model:", chooseModel([["small", 88, 900, 1], ["medium", 94, 1500, 3], ["large", 97, 4000, 9]], 90, 2000));
