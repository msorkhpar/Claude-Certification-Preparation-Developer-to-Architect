import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { review } = await import(pathToFileURL(resolve(dir, "toolReview.ts")).href);

const POLICY = { min_words: 12, max_timeout: 10, max_memory: 256, denied: ["network", "run_process"], approval: ["write_files"] };
const READ = "def run(path):\n    return open(path).read()\n";
type Fields = { name?: string; words?: number; permissions?: string[]; timeout_s?: number; memory_mb?: number; code?: string };
const proposal = (f: Fields = {}) => ({ name: f.name ?? "summarise_report", description: Array(f.words ?? 15).fill("word").join(" "), permissions: f.permissions ?? ["read_files"],
  timeout_s: f.timeout_s ?? 5, memory_mb: f.memory_mb ?? 128, code: f.code ?? READ });
const out = (name: string, decision: string, refusals: string[] = [], findings: string[] = [], used: string[] = ["read_files"]) =>
  ({ name, decision, refusals, findings, used, audit: `${name}: ${decision}` });

test("m1 a well formed read only tool is approved and leaves an audit line", () => {
  assert.deepEqual(review(proposal(), POLICY), out("summarise_report", "approve"));
});

test("e1 a description needs at least the minimum number of words", () => {
  assert.deepEqual(review(proposal({ words: 12 }), POLICY).findings, []);
  const short = review(proposal({ words: 11 }), POLICY);
  assert.deepEqual(short.findings, ["short_description"]);
  assert.equal(short.decision, "revise");
});

test("e2 the timeout and the memory may equal their limits and not exceed them", () => {
  assert.deepEqual(review(proposal({ timeout_s: 10, memory_mb: 256 }), POLICY).findings, []);
  assert.deepEqual(review(proposal({ timeout_s: 11 }), POLICY).findings, ["timeout"]);
  assert.deepEqual(review(proposal({ memory_mb: 257 }), POLICY).findings, ["memory"]);
});

test("e3 a name is lower case snake case of at most sixty four characters", () => {
  assert.deepEqual(review(proposal({ name: "a".repeat(64) }), POLICY).findings, []);
  assert.deepEqual(review(proposal({ name: "a".repeat(65) }), POLICY).findings, ["bad_name"]);
  assert.deepEqual(review(proposal({ name: "Summarise" }), POLICY).findings, ["bad_name"]);
  assert.deepEqual(review(proposal({ name: "ab" }), POLICY).findings, ["bad_name"]);
});

test("e4 forbidden calls in the code refuse the tool and are all listed in order", () => {
  const result = review(proposal({ code: READ + "eval(text)\nos.system('ls')\n" }), POLICY);
  assert.equal(result.decision, "refuse");
  assert.deepEqual(result.refusals, ["forbidden:eval(", "forbidden:os.system"]);
});

test("e5 a permission the code uses without declaring it or a denied one refuses the tool", () => {
  const sneaky = review(proposal({ code: READ + "requests.get(url)\n" }), POLICY);
  assert.equal(sneaky.decision, "refuse");
  assert.deepEqual(sneaky.refusals, ["undeclared:network"]);
  assert.deepEqual(sneaky.used, ["network", "read_files"]);
  const declared = review(proposal({ permissions: ["read_files", "network"], code: READ + "requests.get(url)\n" }), POLICY);
  assert.equal(declared.decision, "refuse");
  assert.deepEqual(declared.refusals, ["denied:network"]);
});

test("e6 a declared write is approved only with a gate and a read alone is approved outright", () => {
  const writer = review(proposal({ permissions: ["read_files", "write_files"], code: READ + "out.write(text)\n" }), POLICY);
  assert.deepEqual(writer, out("summarise_report", "approve_with_gate", [], [], ["read_files", "write_files"]));
  assert.equal(review(proposal(), POLICY).decision, "approve");
});

test("e7 a refusal beats a revision and a revision beats a gate", () => {
  const both = review(proposal({ words: 3, code: READ + "eval(text)\n" }), POLICY);
  assert.equal(both.decision, "refuse");
  assert.deepEqual(both.findings, ["short_description"]);
  assert.equal(review(proposal({ words: 3, permissions: ["read_files", "write_files"], code: READ + "out.write(text)\n" }), POLICY).decision, "revise");
});

test("e8 the permissions the code uses are reported in alphabetical order", () => {
  const result = review(proposal({ permissions: ["read_files", "write_files"], code: "shutil.copy(a, b)\nopen(a).read()\n" }), POLICY);
  assert.deepEqual(result.used, ["read_files", "write_files"]);
});
