import { test } from "node:test";
import assert from "node:assert/strict";
import { BROAD, NARROW, ROLES, reviewPolicy, reviewRole } from "./policyReview.ts";

test("the broad policy has a wildcard action and a wildcard resource", () => {
  assert.deepEqual(reviewPolicy(BROAD).map((f) => f.split(": ")[1].split(" ")[0]), ["action", "resource"]);
});

test("the narrow policy has no findings", () => {
  assert.deepEqual(reviewPolicy(NARROW), []);
});

test("a predefined role with deploy is flagged twice and the custom role passes", () => {
  assert.equal(reviewRole(ROLES.predefined).length, 2);
  assert.deepEqual(reviewRole(ROLES.custom), []);
});
