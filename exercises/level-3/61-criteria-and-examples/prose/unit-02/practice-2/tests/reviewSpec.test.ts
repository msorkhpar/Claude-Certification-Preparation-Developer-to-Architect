import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { buildReviewPrompt, categoryReport: rawCategoryReport, nextStep } = await import(pathToFileURL(resolve(dir, "reviewSpec.ts")).href);
const categoryReport = (...args: unknown[]) => {
  const result = rawCategoryReport(...args);
  assert.ok(result !== null && typeof result === "object", "categoryReport returned nothing");
  return result;
};

const crit = (over: Record<string, unknown> = {}) => ({
  id: "bug", report: "A comment whose claimed behaviour contradicts what the code does.", skip: "Minor style, naming and patterns the codebase already uses.",
  severity: { high: "A null dereference on a request path, such as user.profile.name when user may be None.", low: "A misleading variable name." }, ...over,
});
const REPORT = { verdict: "report", category: "bug", code: "total = price * qty  # sum of the line items", reason: "The comment says the line sums items, the code multiplies one price by a quantity." };
const SKIP = { verdict: "skip", code: "for i in range(n):  # loop", reason: "The comment is terse and accurate; at most a style matter." };
const SPEC = { criteria: [crit()], examples: [REPORT, SKIP] };

function refused(spec: unknown, diff = "+ x = 1"): string {
  try {
    buildReviewPrompt(spec, diff);
  } catch (error) {
    if (error instanceof Error) return error.message;
    throw error;
  }
  assert.fail(`the specification was accepted: ${JSON.stringify(spec)}`);
}

function built(spec: unknown): string {
  const result = buildReviewPrompt(spec, "+ x = 1");
  assert.equal(typeof result, "string", "buildReviewPrompt returned nothing");
  return result;
}

test("m1 the prompt puts criteria first then examples then the diff last", () => {
  const expected = [
    "<criteria>",
    '<criterion id="bug">',
    "Report: A comment whose claimed behaviour contradicts what the code does.",
    "Skip: Minor style, naming and patterns the codebase already uses.",
    "Severity high: A null dereference on a request path, such as user.profile.name when user may be None.",
    "Severity low: A misleading variable name.",
    "</criterion>",
    "</criteria>",
    "<examples>",
    '<example verdict="report" category="bug">',
    "<code>total = price * qty  # sum of the line items</code>",
    "<reason>The comment says the line sums items, the code multiplies one price by a quantity.</reason>",
    "</example>",
    '<example verdict="skip">',
    "<code>for i in range(n):  # loop</code>",
    "<reason>The comment is terse and accurate; at most a style matter.</reason>",
    "</example>",
    "</examples>",
    "<diff>",
    "+ x = 1",
    "</diff>",
  ].join("\n");
  assert.equal(built(SPEC), expected);
});

test("e1 vague criteria are refused in both the report and the skip text", () => {
  for (const phrase of ["Be conservative and flag only what matters.", "Only report high-confidence findings.", "Report it when you are sure.", "Flag only important problems.", "Use your judgment about what matters."]) {
    for (const key of ["report", "skip"]) {
      const message = refused({ ...SPEC, criteria: [crit({ [key]: phrase })] });
      assert.ok(message.includes("vague"), `${key}: '${phrase}' was refused for another reason: ${message}`);
    }
  }
});

test("e2 a criterion needs report skip and a concrete severity example for high and low", () => {
  refused({ ...SPEC, criteria: [] });
  for (const over of [{ report: "" }, { skip: "  " }, { skip: null }, { severity: { high: "A null dereference." } }, { severity: { low: "A misleading name." } }, { severity: { high: "x", low: " " } }, { severity: null }]) {
    refused({ ...SPEC, criteria: [crit(over)] });
  }
  assert.ok(built({ ...SPEC, criteria: [crit()] }).includes('<criterion id="bug">'));
});

test("e3 two to four examples with a report and a skip each carrying a reason", () => {
  refused({ ...SPEC, examples: [REPORT] });
  refused({ ...SPEC, examples: [REPORT, SKIP, REPORT, SKIP, REPORT] });
  refused({ ...SPEC, examples: [REPORT, { ...REPORT }] });
  refused({ ...SPEC, examples: [SKIP, { ...SKIP }] });
  refused({ ...SPEC, examples: [REPORT, { ...SKIP, verdict: "maybe" }] });
  refused({ ...SPEC, examples: [REPORT, { ...SKIP, reason: " " }] });
  refused({ ...SPEC, examples: [{ ...REPORT, category: "performance" }, SKIP] });
  for (const count of [2, 3, 4]) assert.equal(built({ ...SPEC, examples: [REPORT, SKIP, REPORT, SKIP].slice(0, count) }).split("<example ").length - 1, count);
});

function findings(category: string, accepted: number, dismissed: number, pattern = "p") {
  return [...Array(accepted).fill({ category, verdict: "accepted", detected_pattern: pattern }), ...Array(dismissed).fill({ category, verdict: "dismissed", detected_pattern: pattern })];
}

test("e4 a category with enough reviews and low precision is disabled", () => {
  const data = [...findings("bug", 8, 2), ...findings("style", 2, 4), ...findings("naming", 0, 4), ...findings("docs", 3, 3)];
  const report = categoryReport(data);
  assert.deepEqual(report.disable, ["style"]);
  const brief = Object.fromEntries(Object.entries(report.categories).map(([k, v]: [string, any]) => [k, [v.reviewed, v.precision, v.disable]]));
  assert.deepEqual(brief, { bug: [10, 0.8, false], style: [6, 0.33, true], naming: [4, 0, false], docs: [6, 0.5, false] });
  assert.deepEqual(categoryReport(data, 4).disable, ["naming", "style"]);
  assert.deepEqual(categoryReport(data, 5, 0.9).disable, ["bug", "docs", "style"]);
  assert.deepEqual(categoryReport([]), { categories: {}, disable: [] });
});

test("e5 the most dismissed patterns are listed by count then name and capped at three", () => {
  const data = [...findings("style", 0, 3, "line-length"), ...findings("style", 0, 2, "import-order"), ...findings("style", 0, 1, "quote-style"), ...findings("style", 0, 1, "brace-style"), ...findings("style", 5, 0, "accepted-only")];
  assert.deepEqual(categoryReport(data).categories.style.top_dismissed, [["line-length", 3], ["import-order", 2], ["brace-style", 1]]);
  assert.deepEqual(categoryReport(findings("bug", 3, 0)).categories.bug.top_dismissed, []);
});

const REQUEST = { repo: "api", branch: "", reviewer: null };
const REQUIRED = ["repo", "branch", "reviewer"];

function step(request: unknown, defaults: Record<string, string>, attended: boolean) {
  const result = nextStep(request, REQUIRED, defaults, attended);
  assert.ok(result && typeof result === "object", "nextStep returned nothing");
  return result;
}

test("e6 an attended run asks only what it cannot assume and states its assumptions", () => {
  assert.deepEqual(step(REQUEST, { branch: "main" }, true), { action: "ask", ask: ["reviewer"], assumptions: { branch: "main" } });
  assert.deepEqual(step({ repo: "api", branch: " ", reviewer: "ana" }, { branch: "main" }, true), { action: "proceed", ask: [], assumptions: { branch: "main" } });
  assert.deepEqual(step({ repo: "api", branch: "dev", reviewer: "ana" }, { branch: "main" }, true), { action: "proceed", ask: [], assumptions: {} });
  assert.deepEqual(step({}, {}, true), { action: "ask", ask: ["repo", "branch", "reviewer"], assumptions: {} });
});

test("e7 an unattended run never asks it states assumptions or stops", () => {
  assert.deepEqual(step(REQUEST, { branch: "main" }, false), { action: "stop", ask: [], assumptions: { branch: "main" } });
  assert.deepEqual(step({ repo: "api", branch: "", reviewer: "ana" }, { branch: "main" }, false), { action: "proceed", ask: [], assumptions: { branch: "main" } });
  assert.deepEqual(step({ repo: "api", branch: "dev", reviewer: "ana" }, {}, false), { action: "proceed", ask: [], assumptions: {} });
});
