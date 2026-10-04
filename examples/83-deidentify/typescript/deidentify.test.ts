import { test } from "node:test";
import assert from "node:assert/strict";
import { auditEntry, restore, tokenise } from "./deidentify.ts";

test("the same value gets the same token and each kind is counted on its own", () => {
  const vault = new Map<string, string>();
  assert.equal(tokenise("a@x.io b@x.io a@x.io M-123456", vault), "<EMAIL_1> <EMAIL_2> <EMAIL_1> <MEMBER_1>");
  assert.equal(vault.size, 3);
});

test("restore puts every value back", () => {
  const vault = new Map<string, string>();
  const original = "write to a@x.io about M-123456";
  assert.equal(restore(tokenise(original, vault), vault), original);
});

test("text without identifiers is unchanged and the vault stays empty", () => {
  const vault = new Map<string, string>();
  assert.equal(tokenise("nothing to find", vault), "nothing to find");
  assert.equal(vault.size, 0);
});

test("a name is not found by these patterns", () => {
  assert.equal(tokenise("Jane Doe", new Map()), "Jane Doe");
});

test("the audit entry holds sizes and counts only", () => {
  const vault = new Map<string, string>();
  tokenise("a@x.io", vault);
  const entry = auditEntry("r1", "a@x.io", vault);
  assert.deepEqual(entry, { request_id: "r1", chars: 6, tokens_issued: 1, prompt_stored: false });
  assert.ok(!JSON.stringify(entry).includes("a@x.io"));
});
