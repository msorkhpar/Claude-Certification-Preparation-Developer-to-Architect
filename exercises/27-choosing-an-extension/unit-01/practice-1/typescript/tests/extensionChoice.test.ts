import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "extensionChoice.ts")).href);

function choose(situation: Record<string, any>): { mechanism: string; reason: string } {
  const value = solution.choose({ ...situation });
  assert.ok(value !== null && value !== undefined, "choose returned nothing");
  return value;
}

function refused(situation: Record<string, any>): boolean {
  try {
    choose(situation);
  } catch (error) {
    if (error instanceof assert.AssertionError) throw error;
    return true;
  }
  return false;
}

/** Every combination of the axes, each added to the base situation. */
function* sweep(axes: Array<[string, any[]]>, base: Record<string, any>): Generator<Record<string, any>> {
  let combos: Array<Record<string, any>> = [{ ...base }];
  for (const [key, values] of axes) combos = combos.flatMap((c) => values.map((v) => ({ ...c, [key]: v })));
  yield* combos;
}

// The scenario bank of the page: id, situation, expected mechanism and reason code.
const BANK: Array<[string, Record<string, any>, string, string]> = [
  ["s01", { knowledge: "convention" }, "claude-md", "always-known"],
  ["s02", { guarantee: true, knowledge: "convention" }, "hook", "must-hold-every-time"],
  ["s03", { guarantee: true }, "hook", "must-hold-every-time"],
  ["s04", { knowledge: "convention", path_scoped: true }, "path-rule", "scoped-convention"],
  ["s05", { knowledge: "reference" }, "skill", "on-demand-reference"],
  ["s06", { knowledge: "procedure" }, "skill", "repeatable-procedure"],
  ["s07", { external_system: true }, "mcp", "external-system"],
  ["s08", { noisy: true }, "subagent", "isolate-context"],
  ["s09", {}, "builtin-tool", "built-in-covers"],
  ["s10", { external_system: true, knowledge: "procedure", repos: 6 }, "plugin", "shared-setup"],
  ["s11", { knowledge: "procedure", repos: 2 }, "plugin", "shared-setup"],
  ["s12", { guarantee: true, external_system: true }, "hook", "must-hold-every-time"],
  ["s13", { external_system: true, noisy: true }, "mcp", "external-system"],
  ["s14", { knowledge: "convention", repos: 6 }, "claude-md", "always-known"],
  ["s15", { noisy: true, repos: 4 }, "plugin", "shared-setup"],
  ["s16", { surface: "api" }, "api-tool", "own-schema-and-code"],
  ["s17", { surface: "api", builtin_covers: true }, "builtin-tool", "provided-schema"],
  ["s18", { surface: "api", external_system: true, remote_server: true }, "mcp", "remote-server"],
];

test("m1 every situation of the bank gets its mechanism and its reason", () => {
  const wrong = BANK.filter(([, s, mech, reason]) => { const c = choose(s); return c.mechanism !== mech || c.reason !== reason; }).map(([id]) => id);
  assert.deepEqual(wrong, [], `these situations got the wrong mechanism or reason: ${wrong}`);
});

test("e1 a rule that must hold goes to a hook whatever else is true", () => {
  for (const s of sweep([["knowledge", ["none", "convention", "reference", "procedure"]], ["external_system", [false, true]], ["noisy", [false, true]], ["path_scoped", [false, true]]], { guarantee: true })) assert.deepEqual(choose(s), { mechanism: "hook", reason: "must-hold-every-time" }, JSON.stringify(s));
});

test("e2 an outside system needs a server and noisy work alone needs a subagent", () => {
  for (const s of sweep([["knowledge", ["none", "convention", "reference", "procedure"]], ["noisy", [false, true]], ["path_scoped", [false, true]]], { external_system: true })) assert.deepEqual(choose(s), { mechanism: "mcp", reason: "external-system" }, JSON.stringify(s));
  for (const s of sweep([["knowledge", ["none", "convention", "reference", "procedure"]], ["path_scoped", [false, true]]], { noisy: true })) assert.deepEqual(choose(s), { mechanism: "subagent", reason: "isolate-context" }, JSON.stringify(s));
});

test("e3 knowledge goes to the file or skill that loads it at the right time", () => {
  assert.deepEqual(choose({ knowledge: "convention" }), { mechanism: "claude-md", reason: "always-known" });
  assert.deepEqual(choose({ knowledge: "convention", path_scoped: true }), { mechanism: "path-rule", reason: "scoped-convention" });
  assert.deepEqual(choose({ knowledge: "reference" }), { mechanism: "skill", reason: "on-demand-reference" });
  assert.deepEqual(choose({ knowledge: "procedure" }), { mechanism: "skill", reason: "repeatable-procedure" });
  assert.deepEqual(choose({ knowledge: "reference", path_scoped: true }), { mechanism: "skill", reason: "on-demand-reference" });
  assert.deepEqual(choose({ path_scoped: true }), { mechanism: "builtin-tool", reason: "built-in-covers" });
  assert.deepEqual(choose({ builtin_covers: true, knowledge: "procedure" }), { mechanism: "skill", reason: "repeatable-procedure" });
});

test("e4 a plugin carries a skill hook subagent or server to a second repository and nothing else", () => {
  assert.deepEqual(choose({ knowledge: "procedure", repos: 1 }), { mechanism: "skill", reason: "repeatable-procedure" });
  assert.deepEqual(choose({ guarantee: true, repos: 1 }), { mechanism: "hook", reason: "must-hold-every-time" });
  assert.deepEqual(choose({ external_system: true, repos: 1 }), { mechanism: "mcp", reason: "external-system" });
  assert.deepEqual(choose({ noisy: true, repos: 1 }), { mechanism: "subagent", reason: "isolate-context" });
  assert.deepEqual(choose({ knowledge: "procedure", repos: 2 }), { mechanism: "plugin", reason: "shared-setup" });
  assert.deepEqual(choose({ guarantee: true, repos: 2 }), { mechanism: "plugin", reason: "shared-setup" });
  assert.deepEqual(choose({ external_system: true, repos: 3 }), { mechanism: "plugin", reason: "shared-setup" });
  assert.deepEqual(choose({ noisy: true, repos: 2 }), { mechanism: "plugin", reason: "shared-setup" });
  assert.deepEqual(choose({ knowledge: "convention", repos: 2 }), { mechanism: "claude-md", reason: "always-known" });
  assert.deepEqual(choose({ knowledge: "convention", path_scoped: true, repos: 5 }), { mechanism: "path-rule", reason: "scoped-convention" });
  assert.deepEqual(choose({ repos: 9 }), { mechanism: "builtin-tool", reason: "built-in-covers" });
});

test("e5 in an application the platform may supply the schema and only a remote server replaces your own tool", () => {
  assert.deepEqual(choose({ surface: "api" }), { mechanism: "api-tool", reason: "own-schema-and-code" });
  assert.deepEqual(choose({ surface: "api", builtin_covers: true, external_system: true, remote_server: true }), { mechanism: "builtin-tool", reason: "provided-schema" });
  assert.deepEqual(choose({ surface: "api", external_system: true, remote_server: true }), { mechanism: "mcp", reason: "remote-server" });
  assert.deepEqual(choose({ surface: "api", external_system: true }), { mechanism: "api-tool", reason: "own-schema-and-code" });
  assert.deepEqual(choose({ surface: "api", remote_server: true }), { mechanism: "api-tool", reason: "own-schema-and-code" });
  assert.deepEqual(choose({ surface: "api", guarantee: true, knowledge: "convention", noisy: true, repos: 4 }), { mechanism: "api-tool", reason: "own-schema-and-code" });
});

test("e6 an unknown value is an error and a missing key takes its default", () => {
  assert.deepEqual(choose({}), { mechanism: "builtin-tool", reason: "built-in-covers" });
  assert.ok(refused({ surface: "cli" }), "{\"surface\": \"cli\"} must be an error");
  assert.ok(refused({ knowledge: "tips" }), "{\"knowledge\": \"tips\"} must be an error");
  assert.ok(refused({ repos: 0 }), "{\"repos\": 0} must be an error");
  assert.ok(refused({ repos: -1 }), "{\"repos\": -1} must be an error");
  assert.ok(refused({ surface: "api", knowledge: "tips" }), "{\"surface\": \"api\", \"knowledge\": \"tips\"} must be an error");
});
