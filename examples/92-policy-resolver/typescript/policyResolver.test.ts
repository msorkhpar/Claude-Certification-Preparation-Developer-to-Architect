import { test } from "node:test";
import assert from "node:assert/strict";
import { allowedModel, effective } from "./policyResolver.ts";

test("a higher level wins and managed is the highest", () => {
  assert.equal(effective({ managed: { cleanupPeriodDays: 7 }, "command line": { cleanupPeriodDays: 14 }, project: { cleanupPeriodDays: 30 }, user: { cleanupPeriodDays: 60 } })[0].cleanupPeriodDays, 7);
  assert.equal(effective({ project: { cleanupPeriodDays: 30 }, user: { cleanupPeriodDays: 60 }, local: { cleanupPeriodDays: 20 } })[0].cleanupPeriodDays, 20);
});

test("lists merge across levels without duplicates until a lock stops it", () => {
  const open = { project: { "permissions.allow": ["a", "b"] }, user: { "permissions.allow": ["b", "c"] } };
  assert.deepEqual(effective(open)[0]["permissions.allow"], ["a", "b", "c"]);
  const [settings, notes] = effective({ managed: { allowManagedPermissionRulesOnly: true, "permissions.allow": ["m"] }, ...open });
  assert.deepEqual(settings["permissions.allow"], ["m"]);
  assert.deepEqual(notes, ["ignored permissions.allow from project: managed settings are the only source of permission rules", "ignored permissions.allow from user: managed settings are the only source of permission rules"]);
});

test("a key only an organisation can set is ignored anywhere else", () => {
  const [settings, notes] = effective({ user: { strictKnownMarketplaces: [] } });
  assert.ok(!("strictKnownMarketplaces" in settings));
  assert.deepEqual(notes, ["ignored strictKnownMarketplaces from user: a managed-only key"]);
  assert.deepEqual(effective({ managed: { strictKnownMarketplaces: ["one"] }, user: { strictKnownMarketplaces: ["two"] } })[0].strictKnownMarketplaces, ["one"]);
});

test("the lowest effort cap wins and a connector ban from any level stands", () => {
  assert.equal(effective({ managed: { maxEffortLevel: "high" }, user: { maxEffortLevel: "low" }, project: { maxEffortLevel: "max" } })[0].maxEffortLevel, "low");
  assert.equal(effective({ managed: { disableClaudeAiConnectors: false }, project: { disableClaudeAiConnectors: true } })[0].disableClaudeAiConnectors, true);
});

test("a managed model list refuses every other choice exactly", () => {
  const [settings] = effective({ managed: { availableModels: ["sonnet", "haiku"] }, user: { availableModels: ["opus"] } });
  assert.deepEqual(settings.availableModels, ["sonnet", "haiku"]);
  assert.equal(allowedModel("haiku", settings), "allowed");
  assert.equal(allowedModel("opus", settings), "refused (not in availableModels)");
  assert.equal(allowedModel("anything", {}), "allowed");
});
