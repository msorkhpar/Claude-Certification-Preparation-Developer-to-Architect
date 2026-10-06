import { test } from "node:test";
import assert from "node:assert/strict";
import { REQUIRED, coverage, recover, replan, report, research, search, verifyFact, type Task } from "./researchRun.ts";

const NARROW: Task[] = ["AI in digital art", "AI in graphic design", "AI in photography"].map((query) => ({ scope: "visual arts", query }));
const FILM_DOWN = ["AI in film", "AI in film production"];

test("the coordinator finds the scopes its plan leaves out and adds them once", () => {
  assert.deepEqual(coverage(NARROW), [["visual arts"], ["music", "writing", "film"]]);
  const plan = replan(NARROW);
  assert.deepEqual(coverage(plan), [REQUIRED, []]);
  assert.equal(plan.length, 6);
  assert.deepEqual(replan(plan), plan);
});

test("a failed search returns its type query partial results and alternatives", () => {
  const error = search("AI in film").error!;
  assert.deepEqual([error.type, error.query, error.partial, error.alternatives], ["timeout", "AI in film", [], ["AI in film production"]]);
});

test("one retry uses the alternative and a second failure keeps both queries", () => {
  const done = recover(search("AI in film"), []);
  assert.equal(done.status, "ok");
  assert.equal(done.recovered_from, "AI in film");
  const still = recover(search("AI in film", FILM_DOWN), FILM_DOWN);
  assert.equal(still.status, "error");
  assert.deepEqual(still.error!.tried, FILM_DOWN);
});

test("a failure with no alternative is returned as it is and never as success", () => {
  const result = recover(search("AI in music", ["AI in music"]), ["AI in music"]);
  assert.equal(result.status, "error");
  assert.deepEqual(result.error!.alternatives, []);
});

test("the report is partial whenever a scope is not covered and says which", () => {
  const plan = replan(NARROW);
  assert.equal(report(research(plan)).status, "complete");
  const partial = report(research(plan, FILM_DOWN));
  assert.deepEqual([partial.status, partial.covered, partial.errors], ["partial", 3, 1]);
  assert.deepEqual(partial.notes, ["film not covered: timeout on 'AI in film' and on 'AI in film production'"]);
  assert.equal(report(research(NARROW)).status, "partial");
});

test("the scoped verification tool checks dates here and sends the rest back", () => {
  assert.equal(verifyFact("date", "survey-a", "2025-02-01"), "confirmed");
  assert.equal(verifyFact("date", "survey-a", "2024-01-01"), "mismatch");
  assert.equal(verifyFact("statistic", "survey-a", "60%"), "needs_search");
});
