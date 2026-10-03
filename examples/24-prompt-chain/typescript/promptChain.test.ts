import { test } from "node:test";
import assert from "node:assert/strict";
import { DOCUMENTS, QUESTION, REPLIES, TEMPLATES, clientFor, documentsBlock, render, step } from "./promptChain.ts";

test("a missing variable is an error, not a blank", () => {
  assert.throws(() => render("Hello {{name}} and {{other}}", { name: "x" }), /unfilled variables/);
});

test("a value that looks like a placeholder is left alone", () => {
  assert.equal(render("Q: {{a}} / {{b}}", { a: "{{b}}", b: "real" }), "Q: {{b}} / real");
});

test("documents are numbered and carry their source", () => {
  const block = documentsBlock(DOCUMENTS);
  assert.ok(block.startsWith("<documents>") && block.includes('<document index="2">') && block.includes("<source>expenses-faq.txt</source>"));
});

test("the long documents come first and the question last", async () => {
  const { fake, client } = clientFor(REPLIES().slice(0, 1));
  await step(client, "extract-quotes@2", { documents: documentsBlock(DOCUMENTS), question: QUESTION });
  const prompt: string = fake.seen[0].body.messages[0].content;
  assert.ok(prompt.indexOf("<documents>") < prompt.indexOf("<question>") && prompt.trimEnd().endsWith("</question>"));
});

test("each step sends its own system prompt and ends on a user turn", async () => {
  const { fake, client } = clientFor(REPLIES());
  await step(client, "extract-quotes@2", { documents: documentsBlock(DOCUMENTS), question: QUESTION });
  await step(client, "answer-from-quotes@1", { quotes: "<quotes></quotes>", question: QUESTION });
  assert.deepEqual(fake.seen.map((r) => r.body.system), [TEMPLATES["extract-quotes@2"].system, TEMPLATES["answer-from-quotes@1"].system]);
  assert.ok(fake.seen.every((r) => r.body.messages.at(-1).role === "user"));
  assert.ok(!["temperature", "top_p", "top_k"].some((k) => k in fake.seen[0].body));
});
