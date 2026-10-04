import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "errorFlow.ts")).href);
const got = (fn: (...args: any[]) => any) => (...args: any[]) => {
  const value = fn(...args);
  assert.ok(value !== null && value !== undefined, `${fn.name} returned nothing`);
  return value;
};
const [searchWithRecovery, coordinatorPlan, coverageNote] = [solution.searchWithRecovery, solution.coordinatorPlan, solution.coverageNote].map(got);

const ok = (...items: string[]) => ({ status: "ok", items });
const err = (type: string, ...partial: string[]) => ({ status: "error", type, partial });
function scripted(...replies: any[]) {
  const calls: Array<[string, number]> = [];
  const call = (query: string, attempt: number) => {
    calls.push([query, attempt]);
    return replies[Math.min(attempt, replies.length) - 1];
  };
  return { call, calls };
}
const failed = (failure_type: string, attempts: number, partial: string[] = [], alternatives: string[] = [], attempted = "q") => ({ status: "failed", failure_type, attempted, attempts, partial_results: partial, alternatives });

test("m1 a transient failure is retried locally and the success is reported with its attempts", () => {
  const { call, calls } = scripted(err("timeout"), ok("a", "b"));
  assert.deepEqual(searchWithRecovery("q", call), { status: "success", items: ["a", "b"], attempts: 2 });
  assert.deepEqual(calls, [["q", 1], ["q", 2]]);
});

test("e1 a valid empty result is a success with no findings and never an error", () => {
  const { call, calls } = scripted(ok());
  assert.deepEqual(searchWithRecovery("q", call), { status: "empty", items: [], attempts: 1 });
  assert.equal(calls.length, 1);
});

test("e2 a permission or invalid query error is not retried and carries what was attempted and its alternatives", () => {
  let s = scripted(err("permission"), ok("never reached"));
  assert.deepEqual(searchWithRecovery("q", s.call), failed("permission", 1, [], ["request access", "use a public source"]));
  assert.equal(s.calls.length, 1);
  s = scripted(err("invalid_query"));
  assert.deepEqual(searchWithRecovery("q", s.call), failed("invalid_query", 1, [], ["rewrite the query"]));
  assert.deepEqual(searchWithRecovery("q", scripted(err("weird")).call).alternatives, []);
});

test("e3 a failure that survives the retries carries the partial results of the last attempt", () => {
  let s = scripted(err("timeout", "x"), err("timeout", "x", "y"));
  assert.deepEqual(searchWithRecovery("q", s.call), failed("timeout", 2, ["x", "y"], ["retry later", "try a narrower query"]));
  s = scripted(err("unavailable"));
  assert.ok(searchWithRecovery("q", s.call, 3).attempts === 3 && s.calls.length === 3);
});

test("e4 the coordinator uses partial results tries an alternative or flags a gap and never stops the run", () => {
  const results = {
    a: { status: "success", items: ["x"], attempts: 1 },
    b: { status: "empty", items: [], attempts: 1 },
    c: failed("timeout", 2, ["p"], ["retry later"]),
    d: failed("unavailable", 2, [], ["use a cached source"]),
    e: failed("weird", 1),
  };
  assert.deepEqual(coordinatorPlan(results), [["a", "use"], ["b", "no_findings"], ["c", "use_partial"], ["d", "try_alternative"], ["e", "flag_gap"]]);
  assert.deepEqual(coordinatorPlan({}), []);
});

test("e5 the coverage note separates supported topics from gaps and names the cause", () => {
  const results = {
    news: { status: "success", items: ["x"], attempts: 1 },
    patents: { status: "empty", items: [], attempts: 1 },
    papers: failed("timeout", 2, ["p"], [], "q-papers"),
    filings: failed("permission", 1, [], [], "q-filings"),
  };
  assert.equal(coverageNote(results, ["news", "papers", "patents", "filings"]), "Well-supported: news\nPartial: papers (timeout)\nNo findings: patents\nGaps: filings (permission: q-filings)");
  assert.equal(coverageNote({ news: results.news }, ["news"]), "Well-supported: news");
});

test("e6 a topic with no result is a gap that was not searched", () => {
  const results = { news: { status: "success", items: ["x"], attempts: 1 } };
  assert.equal(coverageNote(results, ["news", "blogs"]), "Well-supported: news\nGaps: blogs (not searched)");
  assert.equal(coverageNote({}, []), "");
});
