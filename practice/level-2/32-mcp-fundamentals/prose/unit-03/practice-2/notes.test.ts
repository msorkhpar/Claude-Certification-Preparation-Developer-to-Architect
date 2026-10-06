import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

// The solution is the file beside this test.
const SOLUTION = resolve(import.meta.dirname, "notes_server.ts");

/** Start the solution as a server over stdio, connect the SDK client to it and run steps(client). */
async function session<T>(steps: (c: Client) => Promise<T>): Promise<T> {
  const transport = new StdioClientTransport({ command: process.execPath, args: [SOLUTION], env: process.env as Record<string, string>, stderr: "ignore" });
  const client = new Client({ name: "course-tests", version: "1.0.0" });
  await client.connect(transport);
  try {
    return await steps(client);
  } finally {
    await client.close();
  }
}

/** The result of a promise, or the error it was rejected with. */
const attempt = async (p: Promise<any>): Promise<any> => {
  try {
    return await p;
  } catch (e) {
    return e instanceof Error ? e : new Error(String(e));
  }
};
const text = (result: any): string => (result?.content ?? []).map((c: any) => c.text ?? "").join("");
const add = (c: Client, title: string, body = "body") => attempt(c.callTool({ name: "add_note", arguments: { title, text: body } }));
const search = (c: Client, args: Record<string, unknown>) => attempt(c.callTool({ name: "search_notes", arguments: args }));
const read = (c: Client, uri: string) => attempt(c.readResource({ uri }));
const failed = (r: any) => r instanceof Error;

test("m1 a client can save a note find it and read it back", async () => {
  const [info, saved, found, got] = await session(async (c) => {
    const saved = await add(c, "Plan", "ship it");
    return [c.getServerVersion(), saved, await search(c, { query: "ship" }), await read(c, "notes://note/1")];
  });
  assert.deepEqual([info?.name, info?.version], ["notes", "1.0.0"]);
  assert.ok(!failed(saved) && text(saved) === "Saved note 1: Plan" && !saved.isError);
  assert.equal(text(found), "1. Plan");
  assert.ok(!failed(got) && got.contents[0].text === "Plan\n\nship it");
});

test("e1 the server declares tools resources and prompts and names its tools", async () => {
  const [caps, listed] = await session(async (c) => [c.getServerCapabilities(), await attempt(c.listTools())]);
  assert.ok(caps?.tools && caps?.resources && caps?.prompts);
  assert.ok(!failed(listed));
  const tools: Record<string, any> = Object.fromEntries(listed.tools.map((t: any) => [t.name, t]));
  assert.deepEqual(Object.keys(tools).sort(), ["add_note", "search_notes"]);
  assert.ok(Object.values(tools).every((t: any) => t.description));
  assert.deepEqual(tools.add_note.inputSchema.required, ["title", "text"]);
  assert.deepEqual(tools.search_notes.inputSchema.required, ["query"]);
  const limit = tools.search_notes.inputSchema.properties.limit;
  assert.ok(limit.type === "integer" && limit.default === 5);
});

test("e2 bad input comes back as a tool error the model can read", async () => {
  const [blankTitle, blankText, longText, edgeText, blankQuery, low, high] = await session(async (c) => [
    await add(c, "  ", "x"), await add(c, "T", "   "), await add(c, "T", "x".repeat(501)), await add(c, "T", "x".repeat(500)),
    await search(c, { query: " " }), await search(c, { query: "x", limit: 0 }), await search(c, { query: "x", limit: 21 }),
  ]);
  const pairs: [any, string][] = [[blankTitle, "title is required"], [blankText, "text is required"], [longText, "text is too long (max 500)"], [blankQuery, "query is required"], [low, "limit must be between 1 and 20"], [high, "limit must be between 1 and 20"]];
  for (const [result, message] of pairs) {
    assert.ok(!failed(result), String(result));
    assert.ok(result.isError === true && text(result).includes(message), `${message}: ${JSON.stringify(result)}`);
  }
  assert.ok(!failed(edgeText) && !edgeText.isError && text(edgeText) === "Saved note 1: T");
});

test("e3 search ignores case keeps id order honours the limit and says when nothing matches", async () => {
  const [allHits, limited, none, one] = await session(async (c) => {
    for (const [title, body] of [["Alpha", "first"], ["beta", "ALPHA again"], ["Gamma", "third"], ["alphabet", "soup"]]) await add(c, title, body);
    return [await search(c, { query: "ALPHA" }), await search(c, { query: "alpha", limit: 2 }), await search(c, { query: "  zeta " }), await search(c, { query: "third", limit: 20 })];
  });
  assert.equal(text(allHits), "1. Alpha\n2. beta\n4. alphabet");
  assert.equal(text(limited), "1. Alpha\n2. beta");
  assert.ok(text(none) === 'No notes match "zeta"' && !none.isError);
  assert.equal(text(one), "3. Gamma");
});

test("e4 tool annotations tell a client which tool only reads", async () => {
  const listed = await session((c) => attempt(c.listTools()));
  assert.ok(!failed(listed));
  const tools: Record<string, any> = Object.fromEntries(listed.tools.map((t: any) => [t.name, t.annotations]));
  assert.ok(tools.search_notes && tools.search_notes.readOnlyHint === true);
  assert.ok(tools.add_note);
  assert.ok(tools.add_note.readOnlyHint === false && tools.add_note.destructiveHint === false && tools.add_note.idempotentHint === false);
});

test("e5 resources give the count a note by id and an error for a missing one", async () => {
  const o: Record<string, any> = await session(async (c) => {
    const out: Record<string, any> = { templates: await attempt(c.listResourceTemplates()), resources: await attempt(c.listResources()) };
    out.zero = await read(c, "notes://count");
    await add(c, "One");
    out.one = await read(c, "notes://count");
    await add(c, "Two");
    out.two = await read(c, "notes://count");
    out.second = await read(c, "notes://note/2");
    out.missing = await read(c, "notes://note/3");
    out.zeroId = await read(c, "notes://note/0");
    out.word = await read(c, "notes://note/abc");
    return out;
  });
  assert.ok(!failed(o.resources));
  assert.deepEqual(o.resources.resources.map((r: any) => r.uri), ["notes://count"]);
  assert.ok(!failed(o.templates));
  assert.deepEqual(o.templates.resourceTemplates.map((t: any) => t.uriTemplate), ["notes://note/{id}"]);
  assert.deepEqual(["zero", "one", "two"].map((k) => (failed(o[k]) ? String(o[k]) : o[k].contents[0].text)), ["0 notes", "1 note", "2 notes"]);
  assert.ok(!failed(o.second) && o.second.contents[0].text === "Two\n\nbody");
  for (const key of ["missing", "zeroId", "word"]) assert.ok(failed(o[key]) && String(o[key].message).includes("No note"), key);
});

test("e6 the prompt lists the notes and defaults the tone", async () => {
  const o: Record<string, any> = await session(async (c) => {
    const out: Record<string, any> = { listed: await attempt(c.listPrompts()), empty: await attempt(c.getPrompt({ name: "review_notes", arguments: {} })) };
    await add(c, "Alpha");
    await add(c, "Beta");
    out.default = await attempt(c.getPrompt({ name: "review_notes", arguments: {} }));
    out.formal = await attempt(c.getPrompt({ name: "review_notes", arguments: { tone: "formal" } }));
    return out;
  });
  assert.ok(!failed(o.listed));
  assert.deepEqual(o.listed.prompts.map((p: any) => p.name), ["review_notes"]);
  const argument = o.listed.prompts[0].arguments[0];
  assert.ok(argument.name === "tone" && !argument.required);
  assert.ok(!failed(o.empty) && o.empty.messages[0].content.text === "There are no notes to review.");
  assert.ok(!failed(o.default) && o.default.messages.length === 1 && o.default.messages[0].role === "user");
  assert.equal(o.default.messages[0].content.text, "Review these notes in a brief tone:\n- Alpha\n- Beta");
  assert.ok(!failed(o.formal) && o.formal.messages[0].content.text.includes("in a formal tone"));
});

test("e7 ids are sequential and a failed call does not use one", async () => {
  const [first, bad, again, empty, fourth, one, count] = await session(async (c) => [
    await add(c, "  First  ", "a"), await add(c, "", "b"), await add(c, "First", "c"), await add(c, "Third", "   "), await add(c, "Fourth", "d"),
    await read(c, "notes://note/1"), await read(c, "notes://count"),
  ]);
  assert.ok(text(first) === "Saved note 1: First" && bad.isError === true);
  assert.ok(text(again) === "Saved note 2: First" && empty.isError === true);
  assert.equal(text(fourth), "Saved note 3: Fourth");
  assert.ok(!failed(one) && one.contents[0].text === "First\n\na");
  assert.ok(!failed(count) && count.contents[0].text === "3 notes");
});
