import { test } from "node:test";
import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution: the project folder that holds governance/ and docs/.
const ROOT = resolve(process.env.SOLUTION_DIR ?? "starter");

const BAD_OWNERS = new Set(["", "tbd", "everyone", "team", "n/a", "none"]);

function read(rel: string): string {
  const path = join(ROOT, rel);
  assert.ok(existsSync(path) && statSync(path).isFile(), `${rel} is missing`);
  return readFileSync(path, "utf8");
}

function loadJson(rel: string): any {
  try {
    return JSON.parse(read(rel));
  } catch (error) {
    if (error instanceof SyntaxError) assert.fail(`${rel} is not valid JSON: ${error.message}`);
    throw error;
  }
}

const controls = (): any[] => loadJson("governance/controls.json").controls ?? [];

function registerRows(): string[][] {
  const rows: string[][] = [];
  for (const line of read("docs/risk-register.md").split("\n")) {
    if (line.startsWith("|")) {
      const cells = line.trim().replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
      if (!cells.every((c) => /^[-: ]*$/.test(c))) rows.push(cells);
    }
  }
  return rows.slice(1);
}

function walk(dir: string): string[] {
  return readdirSync(dir).sort().flatMap((name) => {
    const full = join(dir, name);
    return statSync(full).isDirectory() ? walk(full) : [full];
  });
}

test("m1 no control that guards an input or an action fails open", () => {
  const found = controls();
  assert.ok(found.length > 0, "list the controls");
  for (const c of found) {
    assert.ok(["hold", "proceed-flagged"].includes(c.on_failure), `${c.id}: on_failure is hold or proceed-flagged`);
    if (c.tier === "high" || ["input", "action"].includes(c.layer)) assert.equal(c.on_failure, "hold", `${c.id} guards an input or an action and must hold when it fails`);
  }
});

test("e1 every high consequence action has a human step that exists and holds", () => {
  const data = loadJson("governance/controls.json");
  const byId: Record<string, any> = {};
  for (const c of data.controls ?? []) byId[c.id] = c;
  const actions: string[] = data.high_consequence_actions ?? [];
  assert.ok(actions.length > 0, "name the high consequence actions");
  for (const action of actions) {
    const step = (data.human_review ?? {})[action];
    assert.ok(step, `${action} has no human review step`);
    assert.ok(step in byId, `${action} names the control ${step}, which is not defined`);
    assert.ok(byId[step].tier === "high" && byId[step].on_failure === "hold", `${step} must be a high tier control that holds`);
  }
});

test("e2 the automatic threshold is at least 95 and a high consequence action is never automatic", () => {
  const routing = loadJson("governance/routing.json");
  const threshold = routing.auto_confidence_min;
  assert.ok(Number.isInteger(threshold) && threshold >= 95 && threshold <= 100, "auto_confidence_min is a whole number from 95 to 100");
  assert.equal(routing.high_consequence_auto, false, "a high consequence action is never automatic");
  assert.equal(routing.unsupported_answer, "hold", "an unsupported answer is held");
});

test("e3 retention keeps at least 90 days within the ceiling and stores no content", () => {
  const audit = loadJson("governance/retention.json").audit ?? {};
  assert.ok((audit.floor_days ?? 0) >= 90, "the audit floor is at least 90 days");
  assert.ok(audit.floor_days <= (audit.retain_days ?? -1) && (audit.retain_days ?? -1) <= (audit.ceiling_days ?? -1), "retain between the floor and the ceiling");
  assert.equal(audit.store_content, false, "the audit log stores no content");
  assert.equal(audit.legal_hold_overrides_ceiling, true, "a legal hold outranks the ceiling");
});

test("e4 the risk register names a control and an owner for each of four failure modes", () => {
  const ids = new Set(controls().map((c) => c.id));
  const rows = registerRows();
  assert.ok(rows.length >= 4, "the register has a row for each of four failure modes");
  for (const row of rows) {
    assert.ok(row.length >= 5, `${row} is missing a column`);
    assert.ok(ids.has(row[2]), `the control '${row[2]}' is not defined in controls.json`);
    assert.ok(!BAD_OWNERS.has(row[3].toLowerCase()), `${row[1]}: name an owner`);
  }
  const modes = rows.map((row) => row[1].toLowerCase()).join(" ");
  for (const word of ["hallucination", "prompt injection", "privacy", "unfair"]) assert.ok(modes.includes(word), `no row covers ${word}`);
});

test("e5 users are told that ai helped and no file holds personal data", () => {
  assert.ok(/told that ai (helped|assisted)/i.test(read("docs/risk-register.md")), "say that users are told that AI helped");
  const hits: string[] = [];
  for (const path of walk(ROOT)) {
    const text = readFileSync(path, "utf8");
    if (/(\/home\/\w+|\/Users\/\w+|C:\\Users)/.test(text)) hits.push(`${relative(ROOT, path)}: home path`);
    if (/[\w.+-]+@(?!example\.(com|invalid))[\w-]+\.[\w.]+/.test(text)) hits.push(`${relative(ROOT, path)}: email address`);
  }
  assert.deepEqual(hits, []);
});

test("e6 erasure removes the vault mapping within 30 days", () => {
  const erasure = loadJson("governance/retention.json").erasure ?? {};
  assert.equal(erasure.remove_vault_mapping, true, "erasure removes the map from tokens to people");
  const days = erasure.max_days_to_complete ?? 999;
  assert.ok(days >= 1 && days <= 30, "erasure completes within 30 days");
});
