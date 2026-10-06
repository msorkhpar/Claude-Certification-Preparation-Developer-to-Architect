import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { compare, grade, meets, runEval } = await import(pathToFileURL(resolve(dir, "harness.ts")).href);

const mk = (check: Record<string, unknown>) => ({ id: "c", input: "x", check });
const verdict = (check: Record<string, unknown>, output: string, judge?: ((p: string) => string) | null): [boolean | undefined, string | undefined] => {
  const r = grade(mk(check), output, judge) ?? {};
  return [r.passed, r.reason];
};
const near = (a: number, b: number) => Math.abs(a - b) < 1e-9;

test("m1 a run grades every case with its own check and reports the pass rate", () => {
  const cases = [
    { id: "c1", input: "I love it", tags: ["core"], check: { type: "exact", expected: "positive" } },
    { id: "c2", input: "awful", tags: ["core"], check: { type: "exact", expected: "negative" } },
    { id: "c3", input: "order 7", tags: ["extract"], check: { type: "regex", pattern: "ORD-\\d{4}" } },
    { id: "c4", input: "meh", tags: ["core", "edge"], check: { type: "exact", expected: "neutral" } },
  ];
  const answers: Record<string, string> = { "I love it": "positive", awful: "negative", "order 7": "The order is ORD-0007.", meh: "positive" };
  const report = runEval(cases, (t: string) => answers[t]) ?? {};
  assert.ok(report.total === 4 && report.passed === 3);
  assert.ok(near(report.pass_rate, 0.75));
  assert.deepEqual(report.results.map((r: any) => [r.id, r.passed, r.reason]), [["c1", true, "ok"], ["c2", true, "ok"], ["c3", true, "ok"], ["c4", false, "mismatch"]]);
  const empty = runEval([], (t: string) => t) ?? {};
  assert.ok(empty.total === 0 && empty.pass_rate === 0 && empty.results.length === 0);
});

test("e1 an exact check ignores case and spacing but nothing else", () => {
  assert.deepEqual(verdict({ type: "exact", expected: "positive" }, "  Positive \n"), [true, "ok"]);
  assert.deepEqual(verdict({ type: "exact", expected: "not  enough info" }, "Not enough\ninfo"), [true, "ok"]);
  assert.deepEqual(verdict({ type: "exact", expected: "positive" }, "positively"), [false, "mismatch"]);
  assert.deepEqual(verdict({ type: "exact", expected: "positive" }, "negative"), [false, "mismatch"]);
  assert.deepEqual(verdict({ type: "exact", expected: "positive" }, ""), [false, "mismatch"]);
  assert.deepEqual(verdict({ type: "regex", pattern: "^ORD-\\d{4}$" }, "ORD-12345"), [false, "mismatch"]);
});

test("e2 a json field check needs a json object with the field and the same typed value", () => {
  const spam = { type: "json_field", field: "label", equals: "spam" };
  assert.deepEqual(verdict(spam, '{"label":"spam","score":0.9}'), [true, "ok"]);
  assert.deepEqual(verdict(spam, ' \n{"label": "spam"}\n'), [true, "ok"]);
  assert.deepEqual(verdict(spam, '{"label":"ham"}'), [false, "mismatch"]);
  assert.deepEqual(verdict(spam, '{"score":1}'), [false, "missing field"]);
  assert.deepEqual(verdict(spam, 'Sure! {"label":"spam"}'), [false, "not json"]);
  assert.deepEqual(verdict(spam, '```json\n{"label":"spam"}\n```'), [false, "not json"]);
  assert.deepEqual(verdict(spam, '["spam"]'), [false, "not json"]);
  const count = { type: "json_field", field: "count", equals: 3 };
  assert.deepEqual(verdict(count, '{"count":3}'), [true, "ok"]);
  assert.deepEqual(verdict(count, '{"count":"3"}'), [false, "mismatch"]);
  assert.deepEqual(verdict({ type: "json_field", field: "ok", equals: true }, '{"ok":1}'), [false, "mismatch"]);
});

test("e3 a judge check sends the rubric prompt and accepts only a bare score at the threshold", () => {
  const check = { type: "judge", criterion: "empathetic", threshold: 4 };
  const seen: string[] = [];
  const judge = (reply: string) => (prompt: string) => {
    seen.push(prompt);
    return reply;
  };
  const r = grade(mk(check), "We are sorry.", judge("5")) ?? {};
  assert.deepEqual([r.passed, r.reason, r.score], [true, "ok", 5]);
  assert.deepEqual(seen, ["Rate this response on a scale of 1-5 for empathetic:\n<response>We are sorry.</response>\n1: Not at all empathetic\n5: Perfectly empathetic\nOutput only the number."]);
  assert.deepEqual(verdict(check, "x", judge(" 4\n")), [true, "ok"]);
  assert.deepEqual(verdict(check, "x", judge("3")), [false, "below threshold"]);
  assert.deepEqual(verdict({ type: "judge", criterion: "calm" }, "x", judge("4")), [true, "ok"]);
  assert.deepEqual(verdict({ type: "judge", criterion: "calm" }, "x", judge("3")), [false, "below threshold"]);
  for (const reply of ["Score: 4", "I'd say 4 or 5", "6", "0", "", "4.5"]) assert.deepEqual(verdict(check, "x", judge(reply)), [false, "ungradable"], reply);
  assert.deepEqual(verdict(check, "x", null), [false, "ungradable"]);
  assert.deepEqual(verdict(check, "x", () => { throw new Error("judge down"); }), [false, "ungradable"]);
  let calls = 0;
  runEval([{ id: "a", input: "q", check: { type: "exact", expected: "y" } }], () => "y", () => { calls++; return "5"; });
  assert.equal(calls, 0);
});

test("e4 a model that fails on one case does not stop the run", () => {
  const model = (text: string) => {
    if (text === "boom") throw new Error("503 from upstream");
    return "ok";
  };
  const cases = ["a", "boom", "c"].map((n) => ({ id: n, input: n, check: { type: "exact", expected: "ok" } }));
  const report = runEval(cases, model) ?? {};
  assert.ok(report.total === 3 && report.passed === 2);
  assert.deepEqual(report.results.map((r: any) => [r.id, r.passed, r.reason]), [["a", true, "ok"], ["boom", false, "model error"], ["c", true, "ok"]]);
});

test("e5 tags report their own rates and success criteria judge each dimension", () => {
  const cases = [
    { id: "a", input: "1", tags: ["core"], check: { type: "exact", expected: "1" } },
    { id: "b", input: "2", tags: ["core", "edge"], check: { type: "exact", expected: "2" } },
    { id: "c", input: "3", tags: ["edge"], check: { type: "exact", expected: "x" } },
  ];
  const report = runEval(cases, (t: string) => t) ?? {};
  assert.deepEqual(report.by_tag, { core: { passed: 2, total: 2 }, edge: { passed: 1, total: 2 } });
  const rep = { total: 10, passed: 8, pass_rate: 0.8, results: [], flaky: [] as string[], by_tag: { core: { passed: 6, total: 6 }, edge: { passed: 2, total: 4 } } };
  assert.deepEqual(meets(rep, { min_pass_rate: 0.8, tags: { edge: 0.5 } }), { met: true, failures: [] });
  assert.deepEqual(meets(rep, {}), { met: true, failures: [] });
  assert.deepEqual(meets(rep, { min_pass_rate: 0.85, tags: { edge: 0.75, core: 1.0, rare: 0.5 } }), { met: false, failures: ["overall", "tag:edge", "tag:rare"] });
  assert.deepEqual(meets({ ...rep, flaky: ["c3"] }, { max_flaky: 0 }), { met: false, failures: ["flaky"] });
  assert.deepEqual(meets({ ...rep, flaky: ["c3"] }, { max_flaky: 1 }), { met: true, failures: [] });
});

const rep = (rate: number, rows: [string, boolean][]) => ({ pass_rate: rate, results: rows.map(([id, passed]) => ({ id, passed })) });

test("e6 a regression run names what broke what was fixed and what went missing", () => {
  const base = rep(0.75, [["a", true], ["b", true], ["c", false], ["d", true]]);
  const diff = compare(base, rep(0.75, [["a", true], ["b", false], ["c", true], ["e", true]])) ?? {};
  assert.deepEqual([diff.regressions, diff.fixed, diff.added, diff.removed], [["b"], ["c"], ["e"], ["d"]]);
  assert.ok(near(diff.pass_rate_delta ?? 9, 0) && diff.ok === false);
  const better = compare(rep(0.5, [["a", true], ["b", true], ["c", false], ["d", false]]), rep(0.75, [["a", true], ["b", false], ["c", true], ["d", true]])) ?? {};
  assert.ok(near(better.pass_rate_delta ?? 9, 0.25) && better.ok === false);
  assert.deepEqual(better.regressions, ["b"]);
  const same = compare(base, rep(1.0, [["a", true], ["b", true], ["c", true], ["d", true]])) ?? {};
  assert.deepEqual([same.regressions, same.fixed, same.ok], [[], ["c"], true]);
  const dropped = compare(rep(0.5, [["a", true], ["d", false]]), rep(1.0, [["a", true]])) ?? {};
  assert.deepEqual([dropped.regressions, dropped.removed, dropped.ok], [[], ["d"], false]);
});

test("e7 repeated runs expose flaky cases and a case passes only if every run does", () => {
  const outputs: Record<string, string[]> = { q: ["yes", "yes", "no"], r: ["yes", "yes", "yes"], s: ["no", "no", "no"] };
  const counts: Record<string, number> = {};
  let calls = 0;
  const model = (text: string) => {
    calls++;
    const n = counts[text] ?? 0;
    counts[text] = n + 1;
    return outputs[text][n];
  };
  const cases = ["q", "r", "s"].map((n) => ({ id: n, input: n, check: { type: "exact", expected: "yes" } }));
  const report = runEval(cases, model, undefined, 3) ?? {};
  assert.equal(calls, 9);
  assert.deepEqual(report.results.map((r: any) => [r.id, r.passed, r.reason, r.flaky]), [["q", false, "mismatch", true], ["r", true, "ok", false], ["s", false, "mismatch", false]]);
  assert.deepEqual([report.flaky, report.passed], [["q"], 1]);
  const single = runEval(cases.slice(0, 1), () => "yes") ?? {};
  assert.ok(single.results[0].flaky === false && single.flaky.length === 0);
});
