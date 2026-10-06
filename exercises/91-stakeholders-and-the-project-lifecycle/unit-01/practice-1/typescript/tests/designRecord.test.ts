import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds docs/design-record.md.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");

const SECTIONS = ["Summary for the sponsor", "Decision statement", "Options considered", "Recommendation and trade-offs", "Accuracy by segment", "Service levels", "Pilot to scale", "Hand-off and monitoring"];
const ROLE = "billing operations manager";
const ERROR_COST = 250;
const REVIEW_COST = 5;
const MAX_LATENCY_MS = 2000;
const MIN_AVAILABILITY = 99.5;
const BAD_OWNERS = new Set(["", "tbd", "everyone", "team", "n/a", "none"]);
const SHAPES = ["wrong amount", "outdated figure", "refusal", "made-up clause", "omitted exception"];
const ERROR_COSTS: Record<string, number> = { credit: 250, complaint: 60, status: 12 };

function record(): string {
  const path = join(ROOT, "docs", "design-record.md");
  assert.ok(existsSync(path), "docs/design-record.md is missing");
  return readFileSync(path, "utf8");
}

/** [title, body] for every level 2 heading, in order. */
function sections(): [string, string][] {
  const parts = record().split(/^## (.*)$/m);
  const out: [string, string][] = [];
  for (let i = 1; i < parts.length - 1; i += 2) out.push([parts[i].trim(), parts[i + 1].trim()]);
  return out;
}

function body(title: string): string {
  const found = sections().find(([name]) => name === title);
  assert.ok(found, `the section '${title}' is missing`);
  return found[1];
}

/** The rows of the first table in a section, without the header and the separator. */
function table(text: string): string[][] {
  const rows: string[][] = [];
  for (const line of text.split("\n")) {
    if (line.startsWith("|")) {
      const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
      if (!cells.every((c) => /^[-: ]*$/.test(c))) rows.push(cells);
    }
  }
  return rows.slice(1);
}

function number(cell: string): number {
  const found = cell.match(/\d[\d,]*\.?\d*/);
  assert.ok(found, `no number in '${cell}'`);
  return parseFloat(found[0].replace(/,/g, ""));
}

const words = (text: string): number => text.split(/\s+/).filter(Boolean).length;
const fmt = (n: number): string => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, ",");

function walk(dir: string): string[] {
  return readdirSync(dir).sort().flatMap((name) => {
    const full = join(dir, name);
    return statSync(full).isDirectory() ? walk(full) : [full];
  });
}

test("m1 the eight sections are present in order and filled", () => {
  assert.deepEqual(sections().map(([name]) => name), SECTIONS, "the sections are the eight headings, in order");
  for (const [name, text] of sections()) assert.ok(words(text) >= 15, `'${name}' says too little`);
  assert.ok(!record().includes("TODO"), "replace every TODO");
});

test("e1 the decision statement carries the volume the latency the error cost and the accountable role", () => {
  const text = body("Decision statement").toLowerCase();
  for (const needed of ["3,000", "2 seconds", ROLE]) assert.ok(text.includes(needed), `the decision statement does not say '${needed}'`);
  for (const value of [ERROR_COST, REVIEW_COST]) assert.ok(new RegExp(`\\b${value}\\b`).test(text), `the decision statement does not give the cost ${value}`);
});

test("e2 the options include a recommended one that is the cheapest to meet the service levels and a reason for each rejection", () => {
  const rows = table(body("Options considered"));
  assert.ok(rows.length >= 4, "compare at least four options");
  for (const row of rows) {
    assert.ok(row.length === 5 && ["recommended", "alternative", "rejected"].includes(row[3]), `${row}: status is recommended, alternative or rejected`);
    if (row[3] === "rejected") assert.ok(words(row[4]) >= 5, `${row[0]}: give a reason for the rejection`);
  }
  const chosen = rows.filter((r) => r[3] === "recommended");
  assert.equal(chosen.length, 1, "exactly one option is recommended");
  assert.equal(chosen[0][2].toLowerCase(), "yes", "the recommended option meets the service levels");
  const cheapest = Math.min(...rows.filter((r) => r[2].toLowerCase() === "yes").map((r) => number(r[1])));
  assert.equal(number(chosen[0][1]), cheapest, "the recommended option is the cheapest one that meets the service levels");
});

test("e3 the break even accuracy is 98 percent and the recommendation states the cost", () => {
  const text = body("Recommendation and trade-offs");
  const expected = 100 - Math.ceil((100 * REVIEW_COST) / ERROR_COST);
  const found = text.match(/at or above (\d+) percent/);
  assert.ok(found && parseInt(found[1], 10) === expected, `state the rule 'at or above ${expected} percent'`);
  const chosen = table(body("Options considered")).filter((r) => r[3] === "recommended");
  assert.ok(chosen.length > 0 && text.includes(fmt(number(chosen[0][1]))), "the recommendation states the monthly cost of the recommended option");
});

test("e4 each service level has a target within the case limits and a named owner", () => {
  const rows = table(body("Service levels")).filter((r) => r.length === 4);
  const pick = (word: string) => rows.find((r) => r[0].toLowerCase().includes(word));
  const latency = pick("latency");
  const availability = pick("availability");
  const accuracy = pick("accuracy");
  assert.ok(latency && availability && accuracy, "list latency, availability and accuracy");
  assert.ok(latency[1].includes("ms") && number(latency[1]) <= MAX_LATENCY_MS, `the latency target is at most ${MAX_LATENCY_MS} ms`);
  assert.ok(availability[1].includes("percent") && number(availability[1]) >= MIN_AVAILABILITY, `the availability target is at least ${MIN_AVAILABILITY} percent`);
  assert.ok(accuracy[1].toLowerCase().includes("credit"), "the accuracy target is stated for the credit segment");
  for (const row of [latency, availability, accuracy]) assert.ok(row[2] && !BAD_OWNERS.has(row[3].toLowerCase()), `${row[0]}: say how it is measured and who owns it`);
});

test("e5 segments are listed by error cost and handled by the break even", () => {
  const rows = table(body("Accuracy by segment"));
  assert.deepEqual(rows.map((r) => r[0]).sort(), Object.keys(ERROR_COSTS).sort(), "list the credit, complaint and status segments");
  const costs: number[] = [];
  for (const row of rows) {
    assert.equal(row.length, 6, `${row} is missing a column`);
    const [segment, , acc, shape, cost, handling] = row;
    const accuracy = number(acc);
    assert.ok(accuracy >= 0 && accuracy <= 100 && SHAPES.includes(shape), `${segment}: give an accuracy and one of the failure shapes`);
    assert.equal(number(cost), ERROR_COSTS[segment], `${segment}: the cost per error is ${ERROR_COSTS[segment]}`);
    assert.equal(handling, accuracy >= 98 ? "auto" : "reviewed", `${segment}: handling follows the break-even accuracy of 98 percent`);
    costs.push(number(cost));
  }
  assert.deepEqual(costs, [...costs].sort((a, b) => b - a), "list the costliest segment first");
});

test("e6 the pilot to scale table has four assumptions each with a test and a stop trigger with a number", () => {
  const rows = table(body("Pilot to scale"));
  assert.ok(rows.length >= 4, "name at least four assumptions of the pilot");
  for (const row of rows) {
    assert.ok(row.length === 3 && row.every(Boolean), `${row}: give the assumption, how to test it and what stops the roll-out`);
    assert.ok(/\d/.test(row[2]), `${row[0]}: the stop trigger needs a number`);
  }
});

test("e7 the hand off names an owner a runbook a rollback and monitors", () => {
  const text = body("Hand-off and monitoring").toLowerCase();
  for (const needed of ["owner", "runbook", "rollback", "previous model"]) assert.ok(text.includes(needed), `the hand-off does not mention '${needed}'`);
  assert.ok(["refusals", "tokens per answer", "flagged", "latency"].filter((s) => text.includes(s)).length >= 2, "name at least two monitors");
});

test("e8 the sponsor summary has at most 80 words and states the cost and the decision and no file holds personal data", () => {
  const summary = body("Summary for the sponsor");
  assert.ok(words(summary) <= 80, "the sponsor summary has at most 80 words");
  const chosen = table(body("Options considered")).filter((r) => r[3] === "recommended");
  assert.ok(chosen.length > 0 && summary.includes(fmt(number(chosen[0][1]))), "the summary states the monthly cost");
  assert.ok(summary.toLowerCase().includes("decision") && !summary.includes("`"), "the summary asks for a decision, in plain words");
  const hits: string[] = [];
  for (const path of walk(ROOT)) {
    const text = readFileSync(path, "utf8");
    if (/(\/home\/\w+|\/Users\/\w+|C:\\Users)/.test(text)) hits.push(`${relative(ROOT, path)}: home path`);
    if (/[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/.test(text)) hits.push(`${relative(ROOT, path)}: email address`);
  }
  assert.deepEqual(hits, []);
});
