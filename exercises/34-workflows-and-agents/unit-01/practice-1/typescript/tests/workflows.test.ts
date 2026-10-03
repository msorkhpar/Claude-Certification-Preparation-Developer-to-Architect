import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const { orchestrate, refine, route, vote } = await import(pathToFileURL(resolve(dir, "workflows.ts")).href);

const PLAN3 = '["research the topic", "draft the outline", "check the facts"]';

/** A stand-in for the model: the first handler whose prefix starts the prompt answers; every prompt is kept. */
function model(byPrefix: Record<string, string | ((prompt: string) => string)>) {
  const prompts: string[] = [];
  const fn = (prompt: string): string => {
    prompts.push(prompt);
    for (const [prefix, handler] of Object.entries(byPrefix)) if (prompt.startsWith(prefix)) return typeof handler === "function" ? handler(prompt) : handler;
    throw new assert.AssertionError({ message: `no scripted answer for ${JSON.stringify(prompt)}` });
  };
  return Object.assign(fn, { prompts });
}
const starting = (m: { prompts: string[] }, prefix: string) => m.prompts.filter((p) => p.startsWith(prefix));
const judged = (...replies: string[]) => model({ Judge: () => replies.shift() as string });
const sequence = (...items: string[]) => () => items.shift() as string;

test("m1 an orchestrator plans runs a worker per subtask and combines", () => {
  const m = model({ Plan: PLAN3, Subtask: (p) => "done: " + p.split("\n")[0].slice("Subtask: ".length), Combine: "FINAL" });
  const result = orchestrate(m, "Write a guide") ?? {};
  assert.deepEqual([result.status, result.fallback, result.answer, result.calls], ["done", false, "FINAL", 5]);
  assert.deepEqual(result.plan, ["research the topic", "draft the outline", "check the facts"]);
  assert.deepEqual((result.results ?? []).map((r: any) => [r.subtask, r.status, r.output]), [
    ["research the topic", "ok", "done: research the topic"], ["draft the outline", "ok", "done: draft the outline"], ["check the facts", "ok", "done: check the facts"]]);
  assert.deepEqual(m.prompts.slice(0, 1), ["Plan: split the task into at most 5 independent subtasks. Reply with a JSON array of strings only.\nTask: Write a guide"]);
  assert.ok(starting(m, "Subtask")[0] === "Subtask: research the topic\nTask: Write a guide" && starting(m, "Subtask").length === 3);
  assert.deepEqual(starting(m, "Combine"), ["Combine: write one answer to the task from the results.\nTask: Write a guide\n1. research the topic -> done: research the topic\n" +
    "2. draft the outline -> done: draft the outline\n3. check the facts -> done: check the facts"]);
});

test("e1 the plan is read from prose cleaned capped and replaced when unusable", () => {
  const reply = 'Here is the plan:\n```json\n["a", "b", "a", "  c  ", "", 5, "d", "e"]\n```\nGood luck.';
  const capped = orchestrate(model({ Plan: reply, Subtask: "x", Combine: "F" }), "T", 3) ?? {};
  assert.deepEqual(capped.plan, ["a", "b", "c"]);
  assert.equal(capped.calls, 5);
  assert.deepEqual((orchestrate(model({ Plan: reply, Subtask: "x", Combine: "F" }), "T") ?? {}).plan, ["a", "b", "c", "d", "e"]);
  const seen = model({ Plan: reply, Subtask: "x", Combine: "F" });
  orchestrate(seen, "T", 2);
  assert.ok(seen.prompts[0].startsWith("Plan: split the task into at most 2 independent subtasks."));
  for (const bad of ["I cannot plan this.", "[]", '["", 7]', "[not json]"]) {
    const m = model({ Plan: bad, Subtask: "x", Combine: "F" });
    const result = orchestrate(m, "T") ?? {};
    assert.deepEqual([result.plan, result.fallback, result.calls], [["T"], true, 3]);
    assert.deepEqual(starting(m, "Subtask"), ["Subtask: T\nTask: T"]);
  }
});

test("e2 one failing worker does not stop the others or the answer", () => {
  const worker = (prompt: string) => {
    if (prompt.startsWith("Subtask: b")) throw new Error("disk full");
    return prompt.startsWith("Subtask: c") ? "" : "ok " + prompt[9];
  };
  const m = model({ Plan: '["a", "b", "c"]', Subtask: worker, Combine: "FINAL" });
  const result = orchestrate(m, "T") ?? {};
  assert.deepEqual([result.status, result.answer, result.calls], ["partial", "FINAL", 5]);
  assert.deepEqual((result.results ?? []).map((r: any) => [r.subtask, r.status, r.error]), [["a", "ok", undefined], ["b", "failed", "disk full"], ["c", "failed", "empty reply"]]);
  assert.deepEqual(starting(m, "Combine"), ["Combine: write one answer to the task from the results.\nTask: T\n1. a -> ok a\n2. b -> FAILED\n3. c -> FAILED"]);
  const nothing = model({ Plan: '["a", "b"]', Subtask: "  ", Combine: "never" });
  const failed = orchestrate(nothing, "T") ?? {};
  assert.deepEqual([failed.status, failed.answer, failed.calls], ["failed", null, 3]);
  assert.deepEqual(starting(nothing, "Combine"), []);
});

test("e3 a draft is revised with the feedback until the judge accepts it", () => {
  const writer = model({ Task: sequence("draft1", "draft2", "draft3") });
  const judge = judged('{"score": 5, "feedback": "add examples"}', '{"score": 9, "feedback": "good"}');
  const result = refine(writer, judge, "T") ?? {};
  assert.deepEqual([result.status, result.draft, result.score, result.rounds], ["accepted", "draft2", 9, 2]);
  assert.deepEqual(writer.prompts, ["Task: T", "Task: T\nPrevious draft: draft1\nFeedback: add examples\nRevise the draft."]);
  assert.equal(judge.prompts[0], 'Judge: score the draft from 0 to 10 and reply with JSON {"score": n, "feedback": "..."}.\nTask: T\nDraft: draft1');
  assert.deepEqual(result.history, [{ round: 1, score: 5, feedback: "add examples" }, { round: 2, score: 9, feedback: "good" }]);
  const edge = refine(model({ Task: "d" }), judged('{"score": 8, "feedback": ""}'), "T") ?? {};
  assert.deepEqual([edge.status, edge.rounds], ["accepted", 1]);
  const custom = refine(model({ Task: "d" }), judged('{"score": 8, "feedback": ""}', '{"score": 10}'), "T", 4, 10) ?? {};
  assert.deepEqual([custom.status, custom.rounds, custom.score], ["accepted", 2, 10]);
});

test("e4 when the rounds run out the best draft wins and an unreadable judge scores zero", () => {
  const result = refine(model({ Task: sequence("draft1", "draft2", "draft3") }), judged('{"score": 6, "feedback": "x"}', '{"score": 7, "feedback": "y"}', '{"score": 7, "feedback": "z"}'), "T") ?? {};
  assert.deepEqual([result.status, result.draft, result.score, result.rounds], ["max_rounds", "draft2", 7, 3]);
  const unreadable = refine(model({ Task: sequence("draft1", "draft2", "draft3", "draft4") }), judged("not json", '{"score": "high"}', '{"score": 11}', '{"score": true, "feedback": "x"}'), "T", 4) ?? {};
  assert.deepEqual([unreadable.status, unreadable.draft, unreadable.score], ["max_rounds", "draft1", 0]);
  assert.deepEqual((unreadable.history ?? []).map((h: any) => h.feedback), Array(4).fill("The judge reply could not be read."));
  const prose = refine(model({ Task: sequence("d1", "d2") }), judged("hmm", 'Verdict: {"score": 9, "feedback": "ok"} thanks'), "T") ?? {};
  assert.deepEqual([prose.status, prose.draft, prose.score], ["accepted", "d2", 9]);
});

test("e5 a label is read from the reply and anything else takes the default route", () => {
  const routes = { billing: (t: string) => "B:" + t, technical: (t: string) => "T:" + t, other: (t: string) => "O:" + t };
  const text = "My card was charged twice";
  const m = model({ Classify: " Billing. " });
  assert.deepEqual(route(m, text, routes, "other"), { label: "billing", output: "B:" + text, fallback: false });
  assert.deepEqual(m.prompts, ["Classify: My card was charged twice\nLabels: billing, technical, other"]);
  assert.equal((route(model({ Classify: "TECHNICAL" }), text, routes, "other") ?? {}).label, "technical");
  for (const reply of ["I think it is billing", "", "refund"]) {
    assert.deepEqual(route(model({ Classify: reply }), text, routes, "other"), { label: "other", output: "O:" + text, fallback: true });
  }
});

test("e6 the majority answer wins and a tie goes to the one seen first", () => {
  const m = model({ Is: sequence("Yes", "yes ", " NO", "yes") });
  const result = vote(m, "Is it safe?", 4) ?? {};
  assert.deepEqual([result.answer, result.votes, result.agreement], ["yes", { yes: 3, no: 1 }, 0.75]);
  assert.deepEqual(m.prompts, Array(4).fill("Is it safe?"));
  assert.equal((vote(model({ Is: sequence("b", "a", "a", "b") }), "Is it safe?", 4) ?? {}).answer, "b");
  assert.equal((vote(model({ Is: "x" }), "Is it safe?", 0) ?? { answer: "missing" }).answer, null);
  assert.deepEqual((vote(model({ Is: "x" }), "Is it safe?") ?? {}).votes, { x: 5 });
});

test("e7 a writer that fails ends the loop with the best draft so far", () => {
  let first = true;
  const flaky = () => {
    if (first) { first = false; return "draft1"; }
    throw new Error("boom");
  };
  const result = refine(model({ Task: flaky }), judged('{"score": 4, "feedback": "more"}'), "T") ?? {};
  assert.deepEqual([result.status, result.draft, result.score, result.rounds, result.error], ["error", "draft1", 4, 1, "boom"]);
  assert.equal((result.history ?? []).length, 1);
  const none = refine(model({ Task: () => { throw new Error("down"); } }), judged(), "T") ?? {};
  assert.deepEqual([none.status, none.draft, none.score, none.rounds, none.error], ["error", null, null, 0, "down"]);
});
