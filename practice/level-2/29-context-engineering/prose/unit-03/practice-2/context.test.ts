import { test } from "node:test";
import assert from "node:assert/strict";
import { pathToFileURL } from "node:url";
import { resolve } from "node:path";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { clearToolResults, compact, countTokens, footnotes, verifyCitations, window } = await import(pathToFileURL(resolve(dir, "context.ts")).href);

const user = (text: string) => ({ role: "user", content: text });
const said = (text: string) => ({ role: "assistant", content: [{ type: "text", text }] });
const call = (id: string, name: string, input: Record<string, unknown> = {}) => ({ role: "assistant", content: [{ type: "tool_use", id, name, input }] });
const result = (id: string, text: string, error = false) => ({ role: "user", content: [{ type: "tool_result", tool_use_id: id, content: text, ...(error ? { is_error: true } : {}) }] });

const resultBlocks = (messages: any[]) => (messages ?? []).flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b: any) => b.type === "tool_result");

/** Tool results without their call, and calls without their result. */
function orphans(messages: any[]): string[] {
  const calls = new Set((messages ?? []).flatMap((m) => (Array.isArray(m.content) ? m.content : [])).filter((b: any) => b.type === "tool_use").map((b: any) => b.id));
  const answered = new Set(resultBlocks(messages).map((b: any) => b.tool_use_id));
  return [...new Set([...calls, ...answered])].filter((id) => calls.has(id) !== answered.has(id)).sort();
}
const firstText = (m: any) => (typeof m.content === "string" ? m.content : m.content[0].text);
const clone = <T,>(x: T): T => JSON.parse(JSON.stringify(x));

const conversation = () => [user("find the invoice"), call("t1", "search", { q: "invoice" }), result("t1", "A".repeat(400)), call("t2", "read", { doc: 7 }), result("t2", "B".repeat(400), true),
  call("t3", "search", { q: "total" }), result("t3", "C".repeat(400)), call("t4", "calc", { expr: "1+1" }), result("t4", "D".repeat(40)), said("The total is 2.")];

const turnsConversation = () => [user("first question"), said("first answer " + "x".repeat(80)),
  user("second question"), call("a1", "search"), result("a1", "R".repeat(300)), said("second answer"),
  user("third question"), said("third answer " + "y".repeat(60)),
  user("fourth question"), call("a2", "search"), result("a2", "S".repeat(100)), said("fourth answer")];

function summariser(...texts: string[]) {
  const calls: any[][] = [];
  const fn = (messages: any[]) => {
    calls.push(clone(messages));
    return texts.shift() as string;
  };
  return { fn, calls };
}

test("m1 an over budget conversation becomes a summary and the newest turn", () => {
  const messages = turnsConversation();
  const budget = Math.floor(countTokens(messages) / 2);
  const s = summariser("The user asked three things.");
  const done = compact(messages, budget, s.fn, 1) ?? [];
  assert.equal(s.calls.length, 1);
  assert.deepEqual(s.calls[0], messages.slice(0, 8));
  assert.deepEqual(done.map((m: any) => m.role), ["user", "assistant", "user", "assistant"]);
  assert.deepEqual(done[0]?.content, [{ type: "text", text: "<summary>\nThe user asked three things.\n</summary>" }, { type: "text", text: "fourth question" }]);
  assert.deepEqual(done.slice(1), messages.slice(9));
  assert.ok(countTokens(done) <= budget);
});

test("e1 old tool results are cleared but their calls and flags stay", () => {
  const messages = conversation();
  const before = clone(messages);
  const cleared = clearToolResults(messages, 2) ?? [];
  assert.deepEqual(messages, before);
  assert.deepEqual(resultBlocks(cleared).map((b: any) => [b.tool_use_id, b.content === "[cleared]", b.is_error ?? false]), [["t1", true, false], ["t2", true, true], ["t3", false, false], ["t4", false, false]]);
  assert.deepEqual(cleared.filter((m: any) => m.role === "assistant"), messages.filter((m: any) => m.role === "assistant"));
  assert.deepEqual(resultBlocks(clearToolResults(messages, 0, [], "gone")).map((b: any) => b.content), ["gone", "gone", "gone", "gone"]);
  const kept = resultBlocks(clearToolResults(messages, 2, ["read"]));
  assert.deepEqual(kept.map((b: any) => [b.tool_use_id, b.content === "[cleared]"]), [["t1", true], ["t2", false], ["t3", false], ["t4", false]]);
});

test("e2 the window drops whole turns and never splits a tool call from its result", () => {
  const messages = turnsConversation();
  const total = countTokens(messages);
  for (let budget = total; budget > 0; budget -= 7) {
    const kept = window(messages, budget) ?? [];
    assert.deepEqual(orphans(kept), [], `budget ${budget}`);
    assert.ok(kept.length > 0 && ["first question", "second question", "third question", "fourth question"].includes(firstText(kept[0])), `budget ${budget}`);
  }
  const two = window(messages, countTokens(messages.slice(6)) + 1) ?? [];
  assert.deepEqual(two.filter((m: any) => m.role === "user" && typeof m.content === "string").map((m: any) => m.content), ["third question", "fourth question"]);
  assert.deepEqual(window(messages, total), messages);
  assert.deepEqual(window(messages, 1), messages.slice(8));
  assert.deepEqual(window(messages, countTokens([...messages.slice(0, 2), ...messages.slice(8)]) + 1, true), [...messages.slice(0, 2), ...messages.slice(8)]);
});

test("e3 a conversation within budget or with nothing older is left alone", () => {
  const messages = turnsConversation();
  const s = summariser("unused");
  assert.deepEqual(compact(messages, countTokens(messages), s.fn), messages);
  const single = [user("one long question " + "z".repeat(400)), said("answer")];
  assert.deepEqual(compact(single, 5, s.fn), single);
  assert.deepEqual(compact(messages, 10_000, s.fn, 2), messages);
  assert.deepEqual(s.calls, []);
});

test("e4 a second compaction folds the earlier summary into the new one", () => {
  const messages = turnsConversation();
  const s = summariser("SUMMARY ONE", "SUMMARY TWO");
  const first = compact(messages, 60, s.fn, 1) ?? [];
  const grown = [...first, said("noted"), user("fifth question " + "q".repeat(200)), said("fifth answer " + "w".repeat(200))];
  const second = compact(grown, 60, s.fn, 1) ?? [];
  assert.equal(s.calls.length, 2);
  const older = s.calls[1] ?? [];
  assert.ok(older.some((m: any) => Array.isArray(m.content) && m.content.some((b: any) => String(b.text ?? "").startsWith("<summary>\nSUMMARY ONE"))));
  const summaries = second.flatMap((m: any) => (Array.isArray(m.content) ? m.content : [])).filter((b: any) => b.type === "text" && b.text.startsWith("<summary>"));
  assert.deepEqual(summaries.map((b: any) => b.text), ["<summary>\nSUMMARY TWO\n</summary>"]);
});

const DOCS = [{ title: "Policy", text: "The grass is green. The sky is blue." }, { title: "Notes", text: "Water is essential for life." }];
const cite = (doc: number, start: number, end: number, cited: string) => ({ type: "char_location", cited_text: cited, document_index: doc, start_char_index: start, end_char_index: end });

test("e5 a citation that does not match its document is reported", () => {
  const good = { type: "text", text: "Grass is green.", citations: [cite(0, 0, 19, "The grass is green.")] };
  assert.deepEqual(verifyCitations([good, { type: "text", text: "No source." }], DOCS), []);
  const blocks = [{ type: "text", text: "x", citations: [cite(0, 0, 19, "The grass is green."), cite(0, 0, 19, "The grass is red."), cite(5, 0, 3, "The"), cite(0, 20, 99, "The sky is blue."),
    cite(1, 3, 3, ""), { type: "page_location", cited_text: "The", document_index: 0, start_page_number: 1, end_page_number: 2 }] }];
  assert.deepEqual((verifyCitations(blocks, DOCS) ?? []).map((p: any) => [p.block, p.citation, p.problem]), [[0, 1, "text_mismatch"], [0, 2, "unknown_document"], [0, 3, "bad_range"], [0, 4, "bad_range"], [0, 5, "unsupported_type"]]);
  assert.deepEqual(verifyCitations([{ type: "text", text: "x", citations: [cite(0, 20, 36, "The sky is blue.")] }], DOCS), []);
});

test("e6 footnotes number each distinct source once in order of appearance", () => {
  const blocks = [{ type: "text", text: "Grass is green. ", citations: [cite(0, 0, 19, "The grass is green.")] },
    { type: "text", text: "Water matters. ", citations: [cite(1, 0, 28, "Water is essential for life.")] },
    { type: "text", text: "Again, green.", citations: [cite(0, 0, 19, "The grass is green.")] },
    { type: "text", text: " No source here." }];
  assert.equal(footnotes(blocks, DOCS), 'Grass is green. [1]Water matters. [2]Again, green.[1] No source here.\n\nSources:\n[1] Policy: "The grass is green."\n[2] Notes: "Water is essential for life."');
  assert.equal(footnotes([{ type: "text", text: "Plain." }], DOCS), "Plain.");
});
