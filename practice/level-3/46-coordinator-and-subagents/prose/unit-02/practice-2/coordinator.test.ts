import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { coordinate } = await import(pathToFileURL(resolve(dir, "coordinator.ts")).href);

const plan = (subtasks: Array<[string, string]>, delegate = true, answer: string | null = null) => () => ({ delegate, answer, subtasks: subtasks.map(([scope, brief]) => ({ scope, brief })) });

/** Subagents: the report is looked up by the start of the brief; every brief received is kept. */
function spokes(reports: Record<string, string | Error>) {
  const briefs: string[] = [];
  const fn = (brief: string): string => {
    briefs.push(brief);
    for (const [prefix, report] of Object.entries(reports)) {
      if (brief.startsWith(prefix.replaceAll("_", " "))) {
        if (report instanceof Error) throw report;
        return report;
      }
    }
    throw new assert.AssertionError({ message: `no scripted report for ${JSON.stringify(brief)}` });
  };
  return Object.assign(fn, { briefs });
}

const noGaps = () => [] as string[];
const echo = (_q: string, findings: Array<{ text: string }>) => findings.map((f) => f.text).join(" | ");
const THREE: Array<[string, string]> = [["chips", "chips: find 2024 chip supply news"], ["cars", "cars: find 2024 car output news"], ["rates", "rates: find 2024 interest rates"]];
const REPORTS = { chips: "chips report", cars: "cars report", rates: "rates report" };

test("m1 a hub sends one brief to each spoke and synthesizes what comes back", () => {
  const s = spokes(REPORTS);
  const result = coordinate(plan(THREE), s, noGaps, echo, "How did supply change?") ?? {};
  assert.deepEqual([result.status, result.answer, result.subagent_calls, result.rounds], ["complete", "chips report | cars report | rates report", 3, 0]);
  assert.deepEqual(result.findings, [{ scope: "chips", text: "chips report" }, { scope: "cars", text: "cars report" }, { scope: "rates", text: "rates report" }]);
  assert.deepEqual([result.failed, result.dropped, result.gaps], [[], [], []]);
});

test("e1 a question the coordinator can answer itself is answered without any subagent", () => {
  const s = spokes({});
  const asked: string[] = [];
  const result = coordinate(plan(THREE, false, "Paris."), s, (q: string) => { asked.push(q); return []; }, () => { asked.push("synth"); return "x"; }, "Capital of France?") ?? {};
  assert.deepEqual([result.status, result.answer, result.subagent_calls, result.findings], ["direct", "Paris.", 0, []]);
  assert.deepEqual([s.briefs, asked], [[], []]);
});

test("e2 a subagent sees its own brief and nothing the others found", () => {
  const s = spokes(REPORTS);
  coordinate(plan(THREE), s, noGaps, echo, "How did supply change?");
  assert.ok(s.briefs[1] === "cars: find 2024 car output news" && s.briefs[2] === "rates: find 2024 interest rates");
  assert.ok(!s.briefs.some((b) => b.includes("report")));
});

test("e3 the plan is cleaned of empty briefs and duplicate scopes and capped", () => {
  const s = spokes({ a: "a report", b: "b report", c: "c report" });
  const messy = plan([["A", "a: first"], ["a ", "a: again"], ["B", "   "], ["b", "b: second"], ["c", "c: third"], ["d", "d: fourth"]]);
  const result = coordinate(messy, s, noGaps, echo, "q", 3) ?? {};
  assert.deepEqual(s.briefs, ["a: first", "b: second", "c: third"]);
  assert.equal(result.subagent_calls, 3);
  assert.deepEqual(result.dropped, [{ scope: "a ", reason: "duplicate scope" }, { scope: "B", reason: "empty brief" }, { scope: "d", reason: "over limit" }]);
  const empty = coordinate(plan([["x", ""], ["y", "  "]]), spokes({}), noGaps, echo, "q") ?? {};
  assert.deepEqual([empty.status, empty.subagent_calls, empty.answer], ["failed", 0, null]);
});

test("e4 a failing subagent does not stop the others and a run with no findings does not synthesize", () => {
  const s = spokes({ chips: new Error("search offline"), cars: "cars report", rates: "   " });
  const result = coordinate(plan(THREE), s, noGaps, echo, "q") ?? {};
  assert.deepEqual([result.status, result.answer, result.subagent_calls], ["partial", "cars report", 3]);
  assert.deepEqual(result.failed, [{ scope: "chips", error: "search offline" }, { scope: "rates", error: "empty report" }]);
  const synthesized: unknown[] = [];
  const dead = coordinate(plan(THREE), spokes({ chips: new Error("x"), cars: new Error("y"), rates: "" }), noGaps, (_q: string, f: unknown) => { synthesized.push(f); return "z"; }, "q") ?? {};
  assert.deepEqual([dead.status, dead.answer, dead.findings, synthesized], ["failed", null, [], []]);
});

test("e5 a review sends only the gaps back out and stops when none are left", () => {
  const s = spokes({ chips: "chips report", cars: "cars report", Follow: "follow-up report" });
  const reviews: string[][] = [];
  const reviewer = (_q: string, findings: Array<{ scope: string }>) => {
    reviews.push(findings.map((f) => f.scope));
    return reviews.length === 1 ? ["2023 baseline", "  ", "2023 baseline"] : [];
  };
  const result = coordinate(plan(THREE.slice(0, 2)), s, reviewer, echo, "How did supply change?") ?? {};
  assert.deepEqual(s.briefs, [THREE[0][1], THREE[1][1], "Follow up: 2023 baseline\nQuestion: How did supply change?"]);
  assert.deepEqual(reviews, [["chips", "cars"], ["chips", "cars", "2023 baseline"]]);
  assert.deepEqual([result.status, result.rounds, result.subagent_calls, result.gaps], ["complete", 1, 3, []]);
  assert.equal(result.answer, "chips report | cars report | follow-up report");
});

test("e6 the rounds are capped and the gaps that remain are reported", () => {
  const s = spokes({ chips: "chips report", Follow: "more" });
  const reviews: number[] = [];
  const reviewer = (_q: string, findings: unknown[]) => { reviews.push(findings.length); return ["still missing"]; };
  const result = coordinate(plan([THREE[0]]), s, reviewer, echo, "q", 4, 2) ?? {};
  assert.deepEqual([result.status, result.rounds, result.subagent_calls, result.gaps], ["partial", 2, 3, ["still missing"]]);
  assert.deepEqual(reviews, [1, 2, 3]);
});
