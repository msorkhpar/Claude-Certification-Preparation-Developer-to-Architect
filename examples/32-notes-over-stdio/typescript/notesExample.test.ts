import { test } from "node:test";
import assert from "node:assert/strict";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { InMemoryTransport } from "@modelcontextprotocol/sdk/inMemory.js";
import { buildServer } from "./notesExample.ts";

async function connected() {
  const [clientSide, serverSide] = InMemoryTransport.createLinkedPair(); // a client and a server in the same process
  await buildServer().connect(serverSide);
  const client = new Client({ name: "test", version: "1.0.0" });
  await client.connect(clientSide);
  return client;
}
const text = (r: any) => r.content.map((c: any) => c.text).join("");

test("a blank title is a tool error and does not use an id", async () => {
  const c = await connected();
  const bad: any = await c.callTool({ name: "add_note", arguments: { title: " ", text: "x" } });
  const good: any = await c.callTool({ name: "add_note", arguments: { title: "A", text: "x" } });
  assert.ok(bad.isError === true && text(bad).includes("title is required"));
  assert.ok(!good.isError && text(good) === "Saved note 1: A");
});

test("search is case insensitive and says when nothing matches", async () => {
  const c = await connected();
  await c.callTool({ name: "add_note", arguments: { title: "Plan", text: "x" } });
  assert.equal(text(await c.callTool({ name: "search_notes", arguments: { query: "PLAN" } })), "1. Plan");
  assert.equal(text(await c.callTool({ name: "search_notes", arguments: { query: "zzz" } })), 'No notes match "zzz"');
});

test("the count resource and the prompt follow the notes", async () => {
  const c = await connected();
  const count = async () => ((await c.readResource({ uri: "notes://count" })).contents[0] as any).text;
  const before = await count();
  await c.callTool({ name: "add_note", arguments: { title: "A", text: "x" } });
  const prompt: any = await c.getPrompt({ name: "review_notes", arguments: { tone: "formal" } });
  assert.deepEqual([before, await count(), prompt.messages[0].content.text], ["0 notes", "1 note", "Review these notes in a formal tone:\n- A"]);
});
