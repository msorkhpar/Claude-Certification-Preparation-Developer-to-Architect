import { test } from "node:test";
import assert from "node:assert/strict";
import { MULTIPLIER, TASKS, choosePattern, cost } from "./patternLadder.ts";

const byName = (name: string) => TASKS.find((t) => t.name === name)!;

test("a single step stays on the first two rungs", () => {
  assert.equal(choosePattern(byName("classify ticket")), "plain call");
  assert.equal(choosePattern(byName("answer from policy")), "augmented call");
});

test("a known path is a workflow and costs one chat per step", () => {
  const task = byName("claims intake");
  assert.equal(choosePattern(task), "workflow");
  assert.equal(cost(task, "workflow").toFixed(2), "0.08");
});

test("an open path is an agent and a team needs independent parts and value", () => {
  assert.equal(choosePattern(byName("investigate outage")), "agent");
  assert.equal(choosePattern(byName("market research brief")), "multi-agent");
  assert.equal(choosePattern(byName("trivia round-up")), "agent");
});

test("the team threshold is fifteen chats", () => {
  const task = { ...byName("market research brief") };
  task.value = MULTIPLIER["multi-agent"] * task.chat_cost;
  assert.equal(choosePattern(task), "multi-agent");
  task.value = task.value - 0.01;
  assert.equal(choosePattern(task), "agent");
});
