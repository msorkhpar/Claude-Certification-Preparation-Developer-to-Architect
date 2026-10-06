import { test } from "node:test";
import assert from "node:assert/strict";
import { recall, route, window } from "./assistantTurns.ts";

test("a signal of risk is routed to a person before anything else", () => {
  assert.equal(route("I might hurt myself, please get me a human"), "handoff:safety");
  assert.equal(route("this is an emergency", 5), "handoff:safety");
});

test("a request for a person is honoured and a stall needs two misses", () => {
  assert.equal(route("Can I speak to a person?"), "handoff:requested");
  assert.equal(route("what?", 1), "answer");
  assert.equal(route("what?", 2), "handoff:stalled");
});

test("the window keeps the newest turns that fit and all the pinned facts", () => {
  const turns = ["one two", "three four five", "six"];
  assert.deepEqual(window(turns, 4, ["fact"]), { kept: ["three four five", "six"], dropped: 1, facts: ["fact"] });
  assert.deepEqual(window(turns, 3, ["fact"]).kept, ["six"]);
  assert.equal(window(turns, 6, ["fact"]).dropped, 0);
});

test("memory is read per customer and old facts are marked to verify", () => {
  assert.deepEqual(recall("ada", "2026-10-04"), [["address", "12 Elm Road", "current"], ["plan", "Plus", "verify"]]);
  assert.deepEqual(recall("bob", "2026-10-04"), []);
  assert.equal(recall("ada", "2026-10-20")[0][2], "current");
  assert.equal(recall("ada", "2026-10-21")[0][2], "verify");
});
