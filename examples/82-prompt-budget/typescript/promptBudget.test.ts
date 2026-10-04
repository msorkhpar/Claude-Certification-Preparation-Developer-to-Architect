import { test } from "node:test";
import assert from "node:assert/strict";
import { MIN_CACHEABLE, MODULES, assemble, cachedPrefix, tokens } from "./promptBudget.ts";

const VARS = { customer: "Ana", tier: "gold", question: "Q" };

test("tokens are the ceiling of characters over four", () => {
  assert.deepEqual(["", "a", "abcd", "abcde"].map(tokens), [0, 1, 1, 2]);
});

test("static modules come first and the breakpoint follows the last one", () => {
  const swapped = [MODULES[2], MODULES[0], MODULES[3], MODULES[1]];
  const prompt = assemble(swapped, VARS);
  assert.deepEqual(prompt.blocks.map((b) => b.name), ["role", "policy", "customer", "question"]);
  assert.equal(prompt.blocks[prompt.breakpoint!].name, "policy");
});

test("dynamic variables are filled and static text is left alone", () => {
  const prompt = assemble(MODULES, VARS);
  assert.equal(prompt.blocks[2].text, "Customer: Ana. Tier: gold.");
  assert.ok(!prompt.blocks[1].text.includes("{"));
});

test("a prefix under the minimum gets no breakpoint", () => {
  const prompt = assemble([MODULES[0], ...MODULES.slice(2)], VARS);
  assert.ok(prompt.prefix_tokens < MIN_CACHEABLE && prompt.breakpoint === null);
});

test("the prefix survives a dynamic change and breaks on a static one", () => {
  const base = assemble(MODULES, VARS);
  const other = assemble(MODULES, { customer: "Ben", tier: "basic", question: "Other" });
  assert.ok(cachedPrefix(base) === cachedPrefix(other) && cachedPrefix(base) !== "");
  const edited = MODULES.map((m) => (m.name === "role" ? { ...m, text: m.text + " Extra." } : m));
  assert.notEqual(cachedPrefix(assemble(edited, VARS)), cachedPrefix(base));
});
