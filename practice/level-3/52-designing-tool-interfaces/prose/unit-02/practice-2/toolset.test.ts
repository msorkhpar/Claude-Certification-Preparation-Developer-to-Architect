import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// The solution is the file beside this test.
const dir = resolve(import.meta.dirname);
const { effectiveHints, lintTool, lintToolSet, pageResults, parallelSafe } = await import(pathToFileURL(resolve(dir, "toolset.ts")).href);

const ORDER_TEXT = "Looks up one order by its id and returns its status, items and total in cents. Use when the customer gives an order id such as A-1042 "
  + "or asks where an order is. Do not use it to find a customer by name; use get_customer instead of this tool for that. It returns no payment details.";
const CUSTOMER_TEXT = "Finds one customer by email address and returns the customer id, name and plan. Use when the person gives an email or asks about their account. "
  + "Do not use it for orders; call lookup_order instead of this tool for those. It returns no payment details.";

type Obj = { [key: string]: any };

function tool(o: Obj = {}): Obj {
  const properties = o.properties ?? { order_id: { type: "string", description: "The order id, for example A-1042." } };
  const { name = "lookup_order", description = ORDER_TEXT, required = ["order_id"], properties: _p, ...more } = o;
  return { name, description, input_schema: { type: "object", properties, required }, ...more };
}

function rules(t: Obj): string[] {
  const found = lintTool(t);
  assert.ok(found !== null && found !== undefined, "lintTool returned nothing");
  return found;
}

function pairs(tools: Obj[], maxTools?: number): string[][] {
  const found = maxTools === undefined ? lintToolSet(tools) : lintToolSet(tools, maxTools);
  assert.ok(found !== null && found !== undefined, "lintToolSet returned nothing");
  return found;
}

test("m1 a well made tool lints clean and a poor one is named for every rule it breaks", () => {
  assert.deepEqual(rules(tool({ input_examples: [{ order_id: "A-1042" }], annotations: { readOnlyHint: true } })), []);
  const poor = { name: "helper", description: "Gets stuff.", input_schema: { type: "object", properties: { q: { type: "string" } }, required: ["q"] } };
  assert.deepEqual(rules(poor), ["no-boundary", "no-use-when", "param-undescribed", "short-description", "vague-name"]);
});

test("e1 names must match the pattern and a vague name is flagged", () => {
  for (const bad of ["get order", "get.order", "a".repeat(129), "order#1"]) assert.deepEqual(rules(tool({ name: bad })), ["bad-name"], bad);
  for (const good of ["a".repeat(128), "look-up-order", "run_report"]) assert.deepEqual(rules(tool({ name: good })), [], good);
  for (const vague of ["run", "Run", "helper", "query"]) assert.deepEqual(rules(tool({ name: vague })), ["vague-name"], vague);
});

test("e2 a description needs three sentences a when to use phrase and a boundary against the neighbour", () => {
  assert.deepEqual(rules(tool({ description: "Looks up one order by id. Use when the customer gives an order id, not for customers." })), ["short-description"]);
  assert.deepEqual(rules(tool({ description: "Looks up one order by id. It returns the status and total. Do not use it for customers." })), ["no-use-when"]);
  assert.deepEqual(rules(tool({ description: "Looks up one order by id. Use when the customer gives an order id. It returns the status and total." })), ["no-boundary"]);
  assert.deepEqual(rules(tool({ description: "Looks up one order by id. USE WHEN the customer gives an id! Prefer it instead of get_customer for orders." })), []);
  assert.deepEqual(rules(tool({ description: "" })), ["no-boundary", "no-use-when", "short-description"]);
});

test("e3 parameters are described and required names exist and closed sets are enums and examples fit the schema", () => {
  assert.deepEqual(rules(tool({ properties: { order_id: { type: "string", description: "  " } } })), ["param-undescribed"]);
  assert.deepEqual(rules(tool({ required: ["order_id", "missing"] })), ["required-unknown"]);
  const status = { type: "string", description: "One of open, shipped or closed." };
  const orderId = { type: "string", description: "The order id." };
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, status } })), ["open-set"]);
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, status: { type: "string", description: "Either open or closed." } } })), ["open-set"]);
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, status: { ...status, enum: ["open", "shipped", "closed"] } } })), []);
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, reasoning: { type: "string", description: "Why." } } })), ["reasoning-param"]);
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, note: { type: "string", description: "Your step by step thinking." } } })), ["reasoning-param"]);
  assert.deepEqual(rules(tool({ properties: { order_id: orderId, note: { type: "string", description: "A short explanation of why the call is made." } } })), []);
  const props = { order_id: orderId, status: { type: "string", description: "The status.", enum: ["open", "closed"] }, limit: { type: "integer", description: "Page size." } };
  assert.deepEqual(rules(tool({ properties: props, input_examples: [{ order_id: "A", status: "open", limit: 5 }, { order_id: "B" }] })), []);
  for (const bad of [{ order_id: 5 }, {}, { order_id: "A", extra: 1 }, { order_id: "A", status: "lost" }, { order_id: "A", limit: true }, { order_id: "A", limit: "5" }]) {
    assert.deepEqual(rules(tool({ properties: props, input_examples: [bad] })), ["bad-example"], JSON.stringify(bad));
  }
});

test("e4 a list tool needs a limit and a cursor and a hint may not contradict the name", () => {
  const base = { query: { type: "string", description: "Search text." } };
  const limit = { type: "integer", description: "Page size." };
  const cursor = { type: "string", description: "Opaque cursor from the last page." };
  assert.deepEqual(rules(tool({ name: "search_orders", properties: base, required: [] })), ["list-unbounded"]);
  assert.deepEqual(rules(tool({ name: "search_orders", properties: { ...base, limit }, required: [] })), ["list-unbounded"]);
  assert.deepEqual(rules(tool({ name: "list_orders", properties: {}, required: [] })), ["list-unbounded"]);
  assert.deepEqual(rules(tool({ name: "find_customer", properties: { ...base, limit, cursor }, required: [] })), []);
  assert.deepEqual(rules(tool({ name: "get_order", properties: base, required: [] })), []);
  assert.deepEqual(rules(tool({ name: "delete_order", annotations: { readOnlyHint: true } })), ["hint-contradicts-name"]);
  assert.deepEqual(rules(tool({ name: "delete_order", annotations: { destructiveHint: false } })), ["hint-contradicts-name"]);
  assert.deepEqual(rules(tool({ name: "remove_item", annotations: { destructiveHint: false } })), ["hint-contradicts-name"]);
  assert.deepEqual(rules(tool({ name: "send_receipt", annotations: { readOnlyHint: true } })), ["hint-contradicts-name"]);
  for (const ok of [tool({ name: "delete_order", annotations: { destructiveHint: true } }), tool({ name: "get_order", annotations: { readOnlyHint: true } }), tool({ name: "create_order", annotations: { readOnlyHint: false } })]) {
    assert.deepEqual(rules(ok), []);
  }
});

test("e5 a set is graded for duplicate names overlapping descriptions and size", () => {
  const twin = tool({ name: "get_order_status" });
  assert.deepEqual(pairs([tool(), twin]), [["get_order_status", "overlap:lookup_order"], ["lookup_order", "overlap:get_order_status"]]);
  const customer = tool({ name: "get_customer", description: CUSTOMER_TEXT });
  assert.deepEqual(pairs([tool(), customer]), []);
  assert.deepEqual(pairs([tool(), tool({ description: CUSTOMER_TEXT })]), [["lookup_order", "duplicate-name"]]);
  const overlap = (a: string, b: string) => pairs([tool({ name: "a_tool", description: a }), tool({ name: "b_tool", description: b })]).filter((p) => p[1].startsWith("overlap:"));
  assert.deepEqual(overlap("alpha beta gamma delta", "alpha beta gamma epsilon"), [["a_tool", "overlap:b_tool"], ["b_tool", "overlap:a_tool"]]);
  assert.deepEqual(overlap("alpha beta gamma delta epsilon", "alpha beta gamma zeta eta"), []);
  assert.deepEqual(pairs([tool(), customer], 1), [["*", "too-many-tools"]]);
  assert.deepEqual(pairs([tool(), customer], 2), []);
});

function paged(...args: any[]) {
  const result = pageResults(...args);
  assert.ok(result !== null && result !== undefined, "pageResults returned nothing");
  return result;
}

test("e6 pages carry an opaque cursor and a clamped limit and a note", () => {
  const rows = Array.from({ length: 25 }, (_, i) => `row-${String(i).padStart(2, "0")}`);
  const first = paged(rows);
  assert.ok(first && first.truncated === false);
  assert.deepEqual(first.items, rows.slice(0, 10));
  assert.equal(first.note, "Showing 10 of 25 results; pass next_cursor to continue, or narrow the query with a filter.");
  const cursor = first.next_cursor;
  assert.ok(typeof cursor === "string" && !cursor.includes("10") && !cursor.includes("offset"));
  const second = paged(rows, cursor);
  assert.deepEqual(second.items, rows.slice(10, 20));
  assert.ok(second.next_cursor !== null);
  const last = paged(rows, second.next_cursor);
  assert.deepEqual(last.items, rows.slice(20));
  assert.ok(last.next_cursor === null && last.note === null);
  assert.equal(paged(Array.from({ length: 120 }, (_, i) => `r${String(i).padStart(3, "0")}`), null, 500).items.length, 50);
  assert.deepEqual(paged(rows, null, 3).items, rows.slice(0, 3));
  for (const badLimit of [0, -1, true, "5", 2.5]) assert.throws(() => paged(rows, null, badLimit), undefined, `limit ${String(badLimit)} was accepted`);
  for (const badCursor of ["not a cursor!", ""]) assert.throws(() => paged(rows, badCursor), undefined, `cursor ${badCursor} was accepted`);
});

test("e7 a page stops at the size cap and says so but always carries one item", () => {
  const ten = Array(5).fill("0123456789");
  const cut = paged(ten, null, 10, 25);
  assert.deepEqual(cut.items, ten.slice(0, 2));
  assert.ok(cut.truncated === true && cut.next_cursor !== null);
  assert.ok(String(cut.note).startsWith("Showing 2 of 5 results"));
  assert.equal(paged(ten, null, 10, 20).items.length, 2);
  const big = paged(["x".repeat(100), "y"], null, 10, 10);
  assert.deepEqual(big.items, ["x".repeat(100)]);
  assert.equal(big.truncated, true);
  const whole = paged(ten, null, 10, 1000);
  assert.deepEqual(whole.items, ten);
  assert.ok(whole.truncated === false && whole.next_cursor === null);
});

test("e8 a tools own hints are trusted only from a trusted server and the defaults apply otherwise", () => {
  const defaults = { readOnlyHint: false, destructiveHint: true, idempotentHint: false, openWorldHint: true };
  const reader = { name: "get_order", server: "orders", annotations: { readOnlyHint: true, idempotentHint: true } };
  assert.deepEqual(effectiveHints(reader, true), { readOnlyHint: true, destructiveHint: true, idempotentHint: true, openWorldHint: true });
  assert.deepEqual(effectiveHints(reader, false), defaults);
  assert.deepEqual(effectiveHints({ name: "x", annotations: { readOnlyHint: "yes" } }, true), defaults);
  assert.deepEqual(effectiveHints({ name: "x" }, true), defaults);
  const writer = { name: "update_order", server: "orders", annotations: { readOnlyHint: false } };
  const stranger = { name: "get_notes", server: "unknown", annotations: { readOnlyHint: true } };
  assert.deepEqual(parallelSafe([reader, writer, stranger], new Set(["orders"])), ["get_order"]);
  assert.deepEqual(parallelSafe([reader, writer, stranger], new Set(["orders", "unknown"])), ["get_order", "get_notes"]);
  assert.deepEqual(parallelSafe([reader], new Set()), []);
});
