import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { chooseStrategy, reviewChanges, runAdaptive } = await import(pathToFileURL(resolve(dir, "decompose.ts")).href);

const FILES = [{ path: "api.py", text: "def get(): ..." }, { path: "db.py", text: "def query(): ..." }, { path: "ui.py", text: "def show(): ..." }];

/** A scripted file pass: findings and summary by path; every call is kept. A path scripted as an Error raises it. */
function makeFilePass(scripted: Record<string, any> = {}) {
  const calls: any[][] = [];
  const pass = (path: string, text: string, part: number, parts: number) => {
    calls.push([path, text, part, parts]);
    const script = scripted[path.split(".")[0]] ?? { findings: [], summary: `${path} part ${part}` };
    if (script instanceof Error) throw script;
    return script;
  };
  return { pass, calls };
}

function makeCross(findings: string[] = [], error?: Error) {
  const seen: any[] = [];
  const pass = (summaries: any[]) => {
    seen.push(summaries);
    if (error) throw error;
    return findings;
  };
  return { pass, seen };
}

function review(files: any[] = FILES, filePass?: any, crossPass?: any, ...extra: any[]) {
  const result = reviewChanges(files, filePass ?? makeFilePass().pass, crossPass ?? makeCross().pass, ...extra);
  assert.ok(result !== null && result !== undefined, "reviewChanges returned nothing");
  return result;
}

test("m1 each file is reviewed alone and the cross pass reads their summaries", () => {
  const fp = makeFilePass({ api: { findings: ["api: no auth"], summary: "api calls db.query(id)" }, db: { findings: [], summary: "db.query takes a name" }, ui: { findings: ["ui: unused"], summary: "ui shows rows" } });
  const cross = makeCross(["api passes id but db expects a name"]);
  const result = review(FILES, fp.pass, cross.pass);
  assert.deepEqual(fp.calls.map((c) => c[0]), ["api.py", "db.py", "ui.py"]);
  assert.deepEqual(result.files["api.py"], { findings: ["api: no auth"], summary: "api calls db.query(id)", parts: 1 });
  assert.deepEqual(result.files["ui.py"].findings, ["ui: unused"]);
  assert.deepEqual(result.cross, ["api passes id but db expects a name"]);
  assert.deepEqual(cross.seen, [[{ path: "api.py", summary: "api calls db.query(id)" }, { path: "db.py", summary: "db.query takes a name" }, { path: "ui.py", summary: "ui shows rows" }]]);
  assert.ok(Object.keys(result.failed).length === 0 && result.skipped.length === 0 && result.cross_error === null);
});

test("e1 a file pass sees only its own file and the cross pass sees summaries never the text", () => {
  const fp = makeFilePass();
  const cross = makeCross();
  review(FILES, fp.pass, cross.pass);
  assert.deepEqual(fp.calls, [["api.py", "def get(): ...", 1, 1], ["db.py", "def query(): ...", 1, 1], ["ui.py", "def show(): ...", 1, 1]]);
  const seen = cross.seen[0] ?? [];
  assert.ok(seen.length === 3 && seen.every((item: any) => JSON.stringify(Object.keys(item).sort()) === JSON.stringify(["path", "summary"])));
  assert.ok(!JSON.stringify(seen).includes("def "));
});

test("e2 a long file is reviewed in labelled parts and a blank file is skipped", () => {
  const longText = [1, 2, 3, 4, 5, 6, 7].map((n) => `line ${n}`).join("\n");
  const files = [{ path: "big.py", text: longText }, { path: "empty.py", text: "  \n \n" }, { path: "small.py", text: "x = 1" }];
  const fp = makeFilePass();
  const result = review(files, fp.pass, undefined, 3);
  assert.deepEqual(fp.calls, [["big.py", "line 1\nline 2\nline 3", 1, 3], ["big.py", "line 4\nline 5\nline 6", 2, 3], ["big.py", "line 7", 3, 3], ["small.py", "x = 1", 1, 1]]);
  assert.ok(result.files["big.py"].parts === 3 && result.files["big.py"].summary === "big.py part 1 big.py part 2 big.py part 3");
  assert.ok(JSON.stringify(result.skipped) === JSON.stringify(["empty.py"]) && !("empty.py" in result.files));
  assert.equal(review([{ path: "a.py", text: "1\n2\n3" }], undefined, undefined, 3).files["a.py"].parts, 1);
});

test("e3 a failing file is reported and left out of the cross pass which needs two files", () => {
  const fp = makeFilePass({ db: new Error("model timed out"), api: { findings: ["f1"], summary: "api summary" }, ui: { findings: ["f2"], summary: "ui summary" } });
  const cross = makeCross(["relation"]);
  const result = review(FILES, fp.pass, cross.pass);
  assert.deepEqual(result.failed, { "db.py": "model timed out" });
  assert.deepEqual(Object.keys(result.files).sort(), ["api.py", "ui.py"]);
  assert.deepEqual(cross.seen, [[{ path: "api.py", summary: "api summary" }, { path: "ui.py", summary: "ui summary" }]]);
  const lone = makeCross(["never"]);
  const one = review(FILES, makeFilePass({ db: new Error("boom"), ui: new Error("boom") }).pass, lone.pass);
  assert.ok(JSON.stringify(Object.keys(one.files)) === JSON.stringify(["api.py"]) && lone.seen.length === 0 && one.cross.length === 0);
  assert.deepEqual(Object.keys(one.failed).sort(), ["db.py", "ui.py"]);
  const broken = review(FILES, undefined, makeCross([], new Error("cross failed")).pass);
  assert.ok(broken.cross.length === 0 && broken.cross_error === "cross failed" && Object.keys(broken.files).length === 3);
});

/** A scripted planner: the replies in order; every call is kept with the steps it was given. */
function makePlanner(...replies: any[]) {
  const calls: Array<[string, any[]]> = [];
  const planner = (goal: string, steps: any[]) => {
    calls.push([goal, steps]);
    return replies[Math.min(calls.length, replies.length) - 1];
  };
  return { planner, calls };
}

function adapt(planner: any, worker?: (s: string) => string, goal = "map the module", ...extra: any[]) {
  const seen: string[] = [];
  const result = runAdaptive(planner, worker ?? ((subtask: string) => { seen.push(subtask); return `did ${subtask}`; }), goal, ...extra);
  assert.ok(result !== null && result !== undefined, "runAdaptive returned nothing");
  return { result, seen };
}

test("e4 the planner is asked again after each step with the steps so far and the loop ends when it says done", () => {
  const p = makePlanner({ done: false, next: "list the files" }, { done: false, next: "read the entry point" }, { done: true, summary: "two modules, one entry point" });
  const { result, seen } = adapt(p.planner);
  assert.deepEqual(seen, ["list the files", "read the entry point"]);
  assert.ok(result.status === "done" && result.summary === "two modules, one entry point");
  assert.deepEqual(result.steps, [{ subtask: "list the files", result: "did list the files" }, { subtask: "read the entry point", result: "did read the entry point" }]);
  assert.deepEqual(p.calls.map(([, steps]) => steps.length), [0, 1, 2]);
  assert.equal(p.calls[0][0], "map the module");
  assert.deepEqual(p.calls[2][1][1], { subtask: "read the entry point", result: "did read the entry point" });
  const f = makePlanner({ done: false, next: "open the file" }, { done: true, summary: "saw the error" });
  const failing = adapt(f.planner, () => { throw new Error("no such file"); });
  assert.deepEqual(failing.result.steps, [{ subtask: "open the file", result: "ERROR: no such file" }]);
  assert.equal(f.calls[1][1][0].result, "ERROR: no such file");
});

test("e5 the loop stops on a repeated subtask or no next step or an unreadable reply and counts the step limit exactly", () => {
  const repeated = adapt(makePlanner({ done: false, next: "list the files" }, { done: false, next: "  List The Files " }).planner);
  assert.ok(repeated.result.status === "stuck" && JSON.stringify(repeated.seen) === JSON.stringify(["list the files"]) && repeated.result.reason.toLowerCase().includes("list the files"));
  const blank = adapt(makePlanner({ done: false, next: "   " }).planner);
  assert.ok(blank.result.status === "stuck" && blank.result.steps.length === 0);
  for (const reply of ["not a plan", { next: "x" }, { done: "yes" }, null]) {
    const bad = adapt(makePlanner(reply).planner);
    assert.ok(bad.result.status === "bad_plan" && bad.seen.length === 0, `${JSON.stringify(reply)} must be a bad plan`);
  }
  const endless = makePlanner(...[1, 2, 3, 4, 5, 6, 7, 8, 9].map((n) => ({ done: false, next: `step ${n}` })));
  const limited = adapt(endless.planner, undefined, "map the module", 3);
  assert.ok(limited.result.status === "step_limit" && JSON.stringify(limited.seen) === JSON.stringify(["step 1", "step 2", "step 3"]) && endless.calls.length === 4);
  const last = makePlanner({ done: false, next: "one" }, { done: false, next: "two" }, { done: true, summary: "finished on the last step" });
  const finished = adapt(last.planner, undefined, "map the module", 2);
  assert.ok(finished.result.status === "done" && JSON.stringify(finished.seen) === JSON.stringify(["one", "two"]));
});

test("e6 the strategy follows what is known about the steps and whether the items interact", () => {
  assert.equal(chooseStrategy({ steps_known: true, items: 1, items_interact: false }), "fixed_chain");
  assert.equal(chooseStrategy({ steps_known: true, items: 12, items_interact: false }), "fixed_chain");
  assert.equal(chooseStrategy({ steps_known: true, items: 12, items_interact: true }), "per_item_then_cross");
  assert.equal(chooseStrategy({ steps_known: true, items: 1, items_interact: true }), "fixed_chain");
  assert.equal(chooseStrategy({ steps_known: false, items: 12, items_interact: true }), "adaptive");
  assert.equal(chooseStrategy({ steps_known: false, items: 0 }), "adaptive");
  assert.equal(chooseStrategy({ steps_known: true, items: 3 }), "fixed_chain");
  let bad = 0;
  for (const task of [{ items: 3 }, { steps_known: true }, { steps_known: true, items: -1 }, { steps_known: "yes", items: 2 }, { steps_known: true, items: 2.5 }]) {
    try {
      chooseStrategy(task);
    } catch {
      bad += 1;
    }
  }
  assert.equal(bad, 5);
});
