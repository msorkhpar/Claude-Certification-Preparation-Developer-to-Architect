import { test } from "node:test";
import assert from "node:assert/strict";
import { chmodSync, copyFileSync, existsSync, mkdtempSync, readFileSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { buildSummary, changeNotice, continueOptions, firstPrompt, planSession, resolveName, runSession, sessionOptions } = await import(pathToFileURL(resolve(dir, "sessions.ts")).href);

const FAKE = (() => {
  const candidates = ["/w/harness/fake_claude.py"];
  for (let d = resolve("."), i = 0; i < 8; i++, d = resolve(d, "..")) candidates.push(join(d, "harness", "fake_claude.py"));
  // The SDK starts the stand-in as a program, so it must be executable: use a copy marked so (the harness folder may be read-only).
  const exe = join(mkdtempSync(join(tmpdir(), "fake-claude-")), "fake_claude.py");
  copyFileSync(candidates.find((c) => existsSync(c)) as string, exe);
  chmodSync(exe, 0o755);
  return exe;
})();

const NOW = 1_800_000_000;
const DAY = 24 * 3600;
const FILES: { [path: string]: string } = { "a.py": "d1", "b.py": "d2", "c.py": "d3", "d.py": "d4" };
const RECORD = { id: "s-base", name: "auth-review", last_used: NOW - 3600, files: { ...FILES } };

function plan(current: { [path: string]: string } = { ...FILES }, fork = false, now = NOW, record: any = RECORD) {
  const result = planSession(record, current, now, fork);
  assert.ok(result !== null && result !== undefined, "planSession returned nothing");
  return result;
}

test("m1 the plan resumes an unchanged session and tells a changed one what differs and starts fresh when most of it changed", () => {
  const same = plan();
  assert.deepEqual([same.action, same.session_id, same.changed], ["resume", "s-base", []]);
  const one = plan({ ...FILES, "b.py": "x" });
  assert.deepEqual([one.action, one.session_id, one.changed], ["resume_with_notice", "s-base", ["b.py"]]);
  const most = plan({ "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "d4" });
  assert.deepEqual([most.action, most.session_id], ["fresh_with_summary", null]);
});

test("e1 changed deleted and added files are listed in order and any difference asks for a notice", () => {
  const p = plan({ "d.py": "d4", "c.py": "d3", "a.py": "new", "f.py": "n1", "e.py": "n2" });
  assert.deepEqual([p.changed, p.deleted, p.added], [["a.py"], ["b.py"], ["e.py", "f.py"]]);
  const onlyNew = plan({ ...FILES, "z.py": "n" });
  assert.deepEqual([onlyNew.action, onlyNew.session_id, onlyNew.added], ["resume_with_notice", "s-base", ["z.py"]]);
});

test("e2 more than half of the files changed or gone starts fresh and exactly half does not", () => {
  assert.equal(plan({ ...FILES, "a.py": "x", "b.py": "x" }).action, "resume_with_notice");
  assert.equal(plan({ "a.py": "x", "c.py": "d3", "d.py": "d4" }).action, "resume_with_notice");
  const over = plan({ "a.py": "x", "b.py": "x", "d.py": "d4" });
  assert.deepEqual([over.action, over.session_id], ["fresh_with_summary", null]);
  const extra: { [path: string]: string } = { ...FILES, "a.py": "x" };
  for (let i = 0; i < 5; i++) extra[`n${i}.py`] = "n";
  assert.equal(plan(extra).action, "resume_with_notice");
});

test("e3 a session idle for more than a week starts fresh and exactly a week is resumed", () => {
  const week = plan({ ...FILES }, false, RECORD.last_used + 7 * DAY);
  assert.deepEqual([week.action, week.session_id], ["resume", "s-base"]);
  const older = plan({ ...FILES }, false, RECORD.last_used + 7 * DAY + 1);
  assert.deepEqual([older.action, older.session_id], ["fresh_with_summary", null]);
});

test("e4 no saved session starts fresh and a fork is only planned from a session that is resumed", () => {
  const none = planSession(null, { ...FILES }, NOW, true);
  assert.ok(none && none.action === "fresh" && none.session_id === null && none.fork === false);
  assert.equal(plan({ ...FILES }, true).fork, true);
  assert.equal(plan({ ...FILES, "a.py": "x" }, true).fork, true);
  assert.equal(plan({ "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "x" }, true).fork, false);
  assert.equal(plan().fork, false);
});

test("e5 the change notice names only what differs and the prompt puts the notice or the summary before the task", () => {
  const p = plan({ "d.py": "d4", "c.py": "d3", "a.py": "new", "f.py": "n1", "e.py": "n2" });
  assert.equal(changeNotice(p), "Since your earlier analysis:\n- changed: a.py\n- deleted: b.py\n- new: e.py, f.py\n"
    + "Re-read these files before relying on earlier conclusions about them. Every other file is unchanged.");
  assert.equal(changeNotice(plan()), "");
  assert.deepEqual(String(changeNotice(plan({ ...FILES, "z.py": "n" }))).split("\n").slice(1, 2), ["- new: z.py"]);
  assert.equal(firstPrompt(p, "Continue the review."), changeNotice(p) + "\n\nContinue the review.");
  const fresh = plan({ "a.py": "x", "b.py": "x", "c.py": "x", "d.py": "x" });
  assert.equal(firstPrompt(fresh, "Continue the review.", "## Findings\n- none"), "## Findings\n- none\n\nContinue the review.");
  assert.equal(firstPrompt(plan(), "Continue the review.", "ignored"), "Continue the review.");
  assert.equal(firstPrompt(planSession(null, {}, NOW), "Start.", "ignored"), "Start.");
});

test("e6 the summary has a fixed layout with blank and repeated items dropped and files listed by path", () => {
  const text = buildSummary(["Auth uses JWT", "  ", "Auth uses JWT", " Tokens last 1h "], ["Keep sessions"], [], { "b.py": "d2", "a.py": "d1" });
  assert.equal(text, "## Findings\n- Auth uses JWT\n- Tokens last 1h\n\n## Decisions\n- Keep sessions\n\n## Open questions\n- none\n\n## Files\n- a.py (d1)\n- b.py (d2)");
  assert.equal(buildSummary([], [], ["Is the cache shared?"], {}), "## Findings\n- none\n\n## Decisions\n- none\n\n## Open questions\n- Is the cache shared?\n\n## Files\n- none");
});

test("e7 options resume by id and fork together and continue is refused unless one session exists and a name must be unique", () => {
  assert.deepEqual(sessionOptions(plan()), { resume: "s-base" });
  assert.deepEqual(sessionOptions(plan({ ...FILES }, true)), { resume: "s-base", forkSession: true });
  assert.deepEqual(sessionOptions(planSession(null, {}, NOW), { maxTurns: 5 }), { maxTurns: 5 });
  assert.deepEqual(sessionOptions({ action: "fresh_with_summary", session_id: null, fork: true }), {});
  assert.deepEqual(continueOptions([{ id: "s1" }], { maxTurns: 3 }), { maxTurns: 3, continue: true });
  for (const sessions of [[], [{ id: "s1" }, { id: "s2" }]]) assert.throws(() => continueOptions(sessions));
  const index = [{ id: "s1", name: "auth-review" }, { id: "s2", name: "billing" }, { id: "s3", name: "billing" }];
  assert.equal(resolveName("auth-review", index), "s1");
  for (const name of ["billing", "Auth-Review", "missing"]) assert.throws(() => resolveName(name, index));
});

function resumedId(argv: string[]): string | null {
  for (let i = 0; i < argv.length; i++) {
    if (argv[i] === "--resume") return argv[i + 1];
    if (argv[i].startsWith("--resume=")) return argv[i].split("=")[1];
  }
  return null;
}

async function run(sessionId: string, result: any, options: any, prompt = "Continue") {
  const folder = mkdtempSync(join(tmpdir(), "session-"));
  const script = join(folder, "script.json"), record = join(folder, "record.jsonl");
  writeFileSync(script, JSON.stringify({ session_id: sessionId, turns: [[{ say: "ok" }, { result }]] }));
  process.env.FAKE_CLAUDE_SCRIPT = script;
  process.env.FAKE_CLAUDE_RECORD = record;
  const done = (await runSession(prompt, { ...options, pathToClaudeCodeExecutable: FAKE, cwd: folder, settingSources: [], env: { ...process.env } })) ?? ({} as any);
  assert.ok(existsSync(record), "the SDK was never started: runSession did not call query");
  const lines = readFileSync(record, "utf8").split("\n").filter(Boolean).map((l) => JSON.parse(l));
  return { done, argv: lines.find((l) => l.argv).argv as string[] };
}

test("e8 a run through the sdk passes resume and fork to the binary and returns the session id even after an error", async () => {
  const ok = { subtype: "success", result: "analysis done", cost: 0.01, turns: 1 };
  const first = await run("s-base", ok, {});
  assert.ok(first.done.session_id === "s-base" && first.done.result === "analysis done" && first.done.error === null);
  assert.ok(resumedId(first.argv) === null && !first.argv.includes("--fork-session"));
  const forked = await run("s-fork", ok, sessionOptions(plan({ ...FILES }, true)));
  assert.equal(forked.done.session_id, "s-fork");
  assert.equal(resumedId(forked.argv), "s-base");
  assert.ok(forked.argv.includes("--fork-session"));
  const resumed = await run("s-base", ok, sessionOptions(plan()));
  assert.ok(resumed.done.session_id === "s-base" && resumedId(resumed.argv) === "s-base" && !resumed.argv.includes("--fork-session"));
  const failed = await run("s-err", { subtype: "error_max_turns", result: null }, {});
  assert.ok(failed.done.session_id === "s-err" && failed.done.error);
});
