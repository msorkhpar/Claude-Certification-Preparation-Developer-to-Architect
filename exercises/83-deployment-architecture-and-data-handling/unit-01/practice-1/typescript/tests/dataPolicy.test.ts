import { test } from "node:test";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

// SOLUTION_DIR selects starter, reference or a planted wrong solution.
const dir = resolve(process.env.SOLUTION_DIR ?? "starter");
const solution = await import(pathToFileURL(resolve(dir, "dataPolicy.ts")).href);

const GOOD = {
  platform: "api", zdr: true, hipaa_baa: false, model: "claude-sonnet-5-5", inference_geo: "us", region: null, tenancy: "workspace-per-tenant",
  pii_handling: "tokenise", audit: { store_prompts: false, retain_days: 365 },
};
const REQ = { residency: "us", phi: false, zdr_required: true, multi_tenant: true, audit_min_days: 180, audit_max_days: 400 };
const cfg = (over: Record<string, unknown> = {}) => ({ ...GOOD, ...over });
const req = (over: Record<string, unknown> = {}) => ({ ...REQ, ...over });
const check = (config: unknown, requirements: unknown): string[] => {
  const result = solution.checkDeployment(config, requirements);
  assert.ok(Array.isArray(result), "checkDeployment returned nothing");
  return result;
};

test("m1 a compliant deployment has no findings", () => {
  assert.deepEqual(check(GOOD, REQ), []);
});

test("e1 residency is pinned by the request or by the region the platform allows", () => {
  const us = req({ zdr_required: false });
  assert.deepEqual(check(cfg({ inference_geo: "global" }), us), ["residency-not-pinned"]);
  assert.deepEqual(check(cfg({ inference_geo: null }), us), ["residency-not-pinned"]);
  assert.deepEqual(check(cfg({ platform: "aws-platform" }), us), []);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: "us-east-1", inference_geo: null }), us), []);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: "eu-west-1" }), us), ["residency-region"]);
  assert.deepEqual(check(cfg({ platform: "vertex", region: "us-east5" }), us), []);
  const eu = req({ zdr_required: false, residency: "eu" });
  assert.deepEqual(check(cfg(), eu), ["residency-unavailable"]);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: "eu-west-1" }), eu), []);
  assert.deepEqual(check(cfg({ platform: "vertex", region: "europe-west4" }), eu), []);
  assert.deepEqual(check(cfg({ platform: "vertex", region: "eu" }), eu), []);
  assert.deepEqual(check(cfg({ platform: "vertex", region: "us-east5" }), eu), ["residency-region"]);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: null }), eu), ["residency-region"]);
});

test("e2 zero data retention is an arrangement of the providers own platforms and not of every model", () => {
  assert.deepEqual(check(cfg({ zdr: false }), REQ), ["zdr-missing"]);
  assert.deepEqual(check(cfg({ zdr: false }), req({ zdr_required: false })), []);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: "us-east-1", zdr: true }), REQ), ["zdr-not-anthropics"]);
  assert.deepEqual(check(cfg({ model: "claude-fable-5-1" }), REQ), ["model-needs-retention"]);
  assert.deepEqual(check(cfg({ model: "claude-fable-5-1", zdr: false }), REQ), ["model-needs-retention", "zdr-missing"]);
  assert.deepEqual(check(cfg({ model: "claude-fable-5-1" }), req({ zdr_required: false })), []);
});

test("e3 protected health information needs a covered platform an agreement and deidentified input", () => {
  const phi = req({ zdr_required: false, phi: true });
  assert.deepEqual(check(cfg(), phi), ["phi-no-baa"]);
  assert.deepEqual(check(cfg({ hipaa_baa: true }), phi), []);
  assert.deepEqual(check(cfg({ platform: "aws-platform", hipaa_baa: true }), phi), ["phi-platform-unsupported"]);
  assert.deepEqual(check(cfg({ platform: "bedrock", region: "us-east-1" }), phi), []);
  assert.deepEqual(check(cfg({ hipaa_baa: true, pii_handling: "none" }), phi), ["phi-not-deidentified"]);
  assert.deepEqual(check(cfg({ hipaa_baa: true, pii_handling: "redact" }), phi), []);
  const { pii_handling, ...withoutPii } = cfg({ hipaa_baa: true });
  assert.deepEqual(check(withoutPii, phi), ["phi-not-deidentified"]);
});

test("e4 a multi tenant service needs a workspace for each tenant", () => {
  assert.deepEqual(check(cfg({ tenancy: "shared" }), REQ), ["tenant-isolation"]);
  assert.deepEqual(check(cfg({ tenancy: "shared" }), req({ multi_tenant: false })), []);
});

test("e5 an audit log must not store prompts that hold sensitive data", () => {
  const stores = { store_prompts: true, retain_days: 365 };
  assert.deepEqual(check(cfg({ audit: stores }), REQ), []);
  assert.deepEqual(check(cfg({ audit: stores, pii_handling: "none" }), REQ), ["audit-stores-sensitive"]);
  assert.deepEqual(check(cfg({ audit: stores, hipaa_baa: true }), req({ phi: true })), ["audit-stores-sensitive"]);
  assert.deepEqual(check(cfg({ audit: { store_prompts: false, retain_days: 365 }, pii_handling: "none" }), REQ), []);
});

test("e6 audit retention stays between the minimum and the maximum", () => {
  const days = (n: number) => cfg({ audit: { store_prompts: false, retain_days: n } });
  assert.deepEqual(check(days(400), REQ), []);
  assert.deepEqual(check(days(180), REQ), []);
  assert.deepEqual(check(days(401), REQ), ["retention-too-long"]);
  assert.deepEqual(check(days(179), REQ), ["retention-too-short"]);
  assert.deepEqual(check(cfg({ audit: { store_prompts: false } }), REQ), []);
  assert.deepEqual(check(days(10_000), { residency: null }), []);
});

test("e7 purge only what is past the retention limit and not on hold", () => {
  const entries = [{ id: "old", date: "2025-01-01" }, { id: "edge", date: "2025-01-31" }, { id: "over", date: "2025-01-30" },
    { id: "held", date: "2020-01-01", hold: true }, { id: "new", date: "2026-01-30" }];
  const result = solution.retentionActions(entries, 365, "2026-01-31");
  assert.ok(result !== null && typeof result === "object", "retentionActions returned nothing");
  assert.deepEqual(result, { purge: ["old", "over"], keep: ["edge", "held", "new"] });
  assert.deepEqual(solution.retentionActions([], 30, "2026-01-31"), { purge: [], keep: [] });
});

test("e8 a request is served only by a deployment that keeps its data in the region", () => {
  const deployments = [{ name: "us-main", residency: "us" }, { name: "eu-main", residency: "eu" }, { name: "global-a", residency: "global" }, { name: "eu-backup", residency: "eu" }];
  const pick = (region: string, available: unknown[] = deployments) => {
    const result = solution.pickDeployment(region, available);
    assert.ok(result === null || typeof result === "string", "pickDeployment returned something that is not a name");
    return result;
  };
  assert.equal(pick("us"), "us-main");
  assert.equal(pick("eu"), "eu-backup");
  assert.equal(pick("other"), "global-a");
  assert.equal(pick("eu", [deployments[0], deployments[2]]), null);
  assert.equal(pick("other", deployments.slice(0, 2)), null);
  assert.equal(pick("us", []), null);
});
