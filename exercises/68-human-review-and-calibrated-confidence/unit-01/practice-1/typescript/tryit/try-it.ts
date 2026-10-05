// Run executes this file. Change the calls to try your code; Submit runs the tests.
import { logTo } from "./logger.ts";
import { accuracyBy, calibrateThreshold, checkpoint, route } from "./reviewRouting.ts";

// Turn the logger up, so the `log.debug` lines of your code show under the printed lines.
logTo("try-it");

// Accuracy per document type and field, next to the overall figure.
const rec = (doc_type: string, field: string, correct: boolean) => ({ doc_type, field, correct });
const records = [...Array(90).fill(rec("invoice", "total", true)), ...Array(8).fill(rec("receipt", "date", true)), ...Array(2).fill(rec("receipt", "date", false))];
for (const row of accuracyBy(records)) console.log("accuracy:", JSON.stringify(row));

// The lowest confidence at which the model still reaches the target accuracy, from labelled outcomes.
const labeled: Array<[number, boolean]> = [[95, true], [90, true], [85, true], [80, false], [60, false]];
console.log("threshold for 90% accuracy:", calibrateThreshold(labeled, 90));

// Which extractions a person reviews: conflicts and low confidence first, up to the capacity.
const extractions = [{ id: "x1", confidence: 95, conflict: false }, { id: "x2", confidence: 60, conflict: false },
  { id: "x3", confidence: 90, conflict: true }, { id: "x4", confidence: 70, conflict: false }];
console.log("routing:", JSON.stringify(route(extractions, 80, 2)));
console.log("checkpoints:", checkpoint("send_payment", 5), checkpoint("update_note", 50));
